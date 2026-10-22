package com.example.chess.engine

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.zip.ZipFile

/**
 * Native Stockfish Engine Bridge.
 * Launches and communicates with the official Stockfish 19 binary (arm64-v8a / x86_64) via standard UCI protocol.
 * Employs mutex-guarded I/O, robust process health monitoring, and explicit engine status reporting.
 */
class StockfishNativeEngine(private val context: Context? = null) : UciEngine {

    companion object {
        private const val TAG = "StockfishNativeEngine"
        private const val TIMEOUT_UCI_INIT_MS = 6000L
        private const val TIMEOUT_READY_MS = 4000L
    }

    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private val commandMutex = Mutex()

    private var fallbackEngine: StockfishLiteEngine? = null

    private var _engineName: String = "Stockfish 19"
    override val engineName: String
        get() = if (isRealStockfish) _engineName else fallbackEngine?.engineName ?: "Stockfish Lite"

    private var _isRealStockfish: Boolean = false
    override val isRealStockfish: Boolean
        get() = _isRealStockfish

    private var _engineDetails: String = "Not initialized"
    override val engineDetails: String
        get() = if (_isRealStockfish) _engineDetails else fallbackEngine?.engineDetails ?: "Built-in fallback"

    private var _lastError: String? = null
    override val lastError: String?
        get() = _lastError

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        commandMutex.withLock {
            _lastError = null

            val binaryFile = findOrExtractBinary()
            if (binaryFile == null) {
                _lastError = "Could not locate or extract Stockfish binary for architecture: ${Build.SUPPORTED_ABIS.joinToString()}"
                Log.e(TAG, _lastError!!)
                setupFallback()
                return@withContext false
            }

            try {
                if (!binaryFile.canExecute()) {
                    binaryFile.setExecutable(true, false)
                }

                val pb = ProcessBuilder(binaryFile.absolutePath)
                pb.redirectErrorStream(true)
                context?.let { pb.directory(it.filesDir) }

                val proc = pb.start()
                process = proc
                reader = BufferedReader(InputStreamReader(proc.inputStream))
                writer = BufferedWriter(OutputStreamWriter(proc.outputStream))

                // Perform UCI handshake
                sendCommandInternal("uci")
                var uciOk = false
                var versionCaptured = false

                val initSuccess = withTimeoutOrNull(TIMEOUT_UCI_INIT_MS) {
                    var line: String?
                    while (reader?.readLine().also { line = it } != null) {
                        val l = line?.trim() ?: continue
                        Log.d(TAG, "UCI >> $l")

                        if (l.startsWith("id name ")) {
                            _engineName = l.removePrefix("id name ").trim()
                            versionCaptured = true
                        } else if (l == "uciok") {
                            uciOk = true
                            break
                        }
                    }
                    uciOk
                } ?: false

                if (!initSuccess) {
                    _lastError = "Stockfish UCI handshake timed out or failed to reply 'uciok'"
                    Log.e(TAG, _lastError!!)
                    closeProcess()
                    setupFallback()
                    return@withContext false
                }

                // Verify isready
                sendCommandInternal("isready")
                val readySuccess = withTimeoutOrNull(TIMEOUT_READY_MS) {
                    var line: String?
                    while (reader?.readLine().also { line = it } != null) {
                        val l = line?.trim() ?: continue
                        if (l == "readyok") return@withTimeoutOrNull true
                    }
                    false
                } ?: false

                if (!readySuccess) {
                    _lastError = "Stockfish failed to respond 'readyok'"
                    closeProcess()
                    setupFallback()
                    return@withContext false
                }

                _isRealStockfish = true
                _engineDetails = "$_engineName (Official Native ARM64/NNUE)"
                Log.i(TAG, "Successfully initialized real native Stockfish: $_engineName")
                return@withContext true

            } catch (e: Exception) {
                _lastError = "Process execution error: ${e.message}"
                Log.e(TAG, _lastError, e)
                closeProcess()
                setupFallback()
                return@withContext false
            }
        }
    }

    private fun setupFallback() {
        _isRealStockfish = false
        if (fallbackEngine == null) {
            fallbackEngine = StockfishLiteEngine()
        }
    }

    private fun findOrExtractBinary(): File? {
        if (context == null) return null

        // 1. Primary path: Android extracted native library dir
        val nativeDir = File(context.applicationInfo.nativeLibraryDir)
        val primaryLib = File(nativeDir, "libstockfish.so")
        if (primaryLib.exists() && primaryLib.canExecute()) {
            return primaryLib
        }

        // 2. Secondary path: Application files directory
        val filesDirLib = File(context.filesDir, "libstockfish.so")
        if (filesDirLib.exists() && filesDirLib.canExecute()) {
            return filesDirLib
        }

        // 3. Fallback: Extract from APK if not unpacked by installer
        val extracted = extractBinaryFromApk()
        if (extracted != null && extracted.exists() && extracted.canExecute()) {
            return extracted
        }

        // Check if primaryLib exists even without canExecute returning true
        if (primaryLib.exists()) {
            primaryLib.setExecutable(true, false)
            if (primaryLib.canExecute()) return primaryLib
        }

        return null
    }

    private fun extractBinaryFromApk(): File? {
        if (context == null) return null
        return try {
            val apkFile = File(context.applicationInfo.sourceDir)
            if (!apkFile.exists()) return null

            val targetFile = File(context.filesDir, "stockfish_native")
            ZipFile(apkFile).use { zip ->
                val preferredAbis = Build.SUPPORTED_ABIS
                for (abi in preferredAbis) {
                    val entryPath = "lib/$abi/libstockfish.so"
                    val entry = zip.getEntry(entryPath)
                    if (entry != null) {
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(targetFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        targetFile.setExecutable(true, false)
                        Log.i(TAG, "Extracted $entryPath from APK to ${targetFile.absolutePath}")
                        return targetFile
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Failed extracting binary from APK", e)
            null
        }
    }

    private fun sendCommandInternal(cmd: String) {
        val w = writer ?: throw IllegalStateException("Writer not open")
        w.write(cmd + "\n")
        w.flush()
    }

    override suspend fun setOption(name: String, value: String) {
        commandMutex.withLock {
            withContext(Dispatchers.IO) {
                if (_isRealStockfish) {
                    try {
                        sendCommandInternal("setoption name $name value $value")
                        sendCommandInternal("isready")
                        var line: String?
                        while (reader?.readLine().also { line = it } != null) {
                            if (line?.trim() == "readyok") break
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting option $name", e)
                    }
                } else {
                    fallbackEngine?.setOption(name, value)
                }
            }
        }
    }

    override suspend fun newGame() {
        commandMutex.withLock {
            withContext(Dispatchers.IO) {
                if (_isRealStockfish) {
                    try {
                        sendCommandInternal("ucinewgame")
                        sendCommandInternal("isready")
                        var line: String?
                        while (reader?.readLine().also { line = it } != null) {
                            if (line?.trim() == "readyok") break
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error starting new game", e)
                    }
                } else {
                    fallbackEngine?.newGame()
                }
            }
        }
    }

    override suspend fun setPosition(fen: String, moves: List<String>) {
        commandMutex.withLock {
            withContext(Dispatchers.IO) {
                if (_isRealStockfish) {
                    try {
                        val movesStr = if (moves.isNotEmpty()) " moves ${moves.joinToString(" ")}" else ""
                        sendCommandInternal("position fen $fen$movesStr")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting position", e)
                    }
                } else {
                    fallbackEngine?.setPosition(fen, moves)
                }
            }
        }
    }

    override suspend fun search(
        depth: Int,
        movetimeMs: Long?,
        multiPv: Int,
        onInfo: (EngineEvaluation) -> Unit
    ): EngineEvaluation = commandMutex.withLock {
        withContext(Dispatchers.IO) {
            if (!_isRealStockfish) {
                return@withContext fallbackEngine?.search(depth, movetimeMs, multiPv, onInfo)
                    ?: EngineEvaluation()
            }

            try {
                // Configure MultiPV if requested
                sendCommandInternal("setoption name MultiPV value $multiPv")

                // Issue search command
                val goCmd = when {
                    movetimeMs != null -> "go movetime $movetimeMs"
                    else -> "go depth $depth"
                }
                sendCommandInternal(goCmd)

                var latestEval = EngineEvaluation()
                var line: String?

                while (reader?.readLine().also { line = it } != null) {
                    val l = line?.trim() ?: break
                    if (l.startsWith("info ")) {
                        val eval = UciParser.parseInfoLine(l)
                        if (eval != null) {
                            latestEval = eval
                            onInfo(eval)
                        }
                    } else if (l.startsWith("bestmove ")) {
                        val best = UciParser.parseBestMove(l)
                        latestEval = latestEval.copy(bestMoveUci = best)
                        break
                    }
                }
                return@withContext latestEval

            } catch (e: Exception) {
                Log.e(TAG, "Error during search execution", e)
                return@withContext EngineEvaluation()
            }
        }
    }

    override suspend fun stop() {
        commandMutex.withLock {
            withContext(Dispatchers.IO) {
                if (_isRealStockfish) {
                    try {
                        sendCommandInternal("stop")
                        // Consume until bestmove
                        var line: String?
                        while (reader?.readLine().also { line = it } != null) {
                            if (line?.startsWith("bestmove") == true) break
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending stop command", e)
                    }
                } else {
                    fallbackEngine?.stop()
                }
            }
        }
    }

    private fun closeProcess() {
        try {
            if (writer != null) {
                try { sendCommandInternal("quit") } catch (_: Exception) {}
            }
            writer?.close()
            reader?.close()
            process?.destroy()
        } catch (_: Exception) {}
        writer = null
        reader = null
        process = null
        _isRealStockfish = false
    }

    override fun close() {
        closeProcess()
        fallbackEngine?.close()
    }
}
