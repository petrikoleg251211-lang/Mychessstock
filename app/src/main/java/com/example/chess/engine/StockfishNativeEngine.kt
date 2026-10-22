package com.example.chess.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * Connects to a native Stockfish executable process via UCI protocol standard I/O.
 * Falls back transparently to StockfishLiteEngine if the native binary is absent on the host device.
 */
class StockfishNativeEngine(private val context: Context? = null) : UciEngine {

    private var process: Process? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var fallbackEngine: StockfishLiteEngine? = null
    private var isNativeActive = false

    override val engineName: String
        get() = if (isNativeActive) "Stockfish (Native UCI)" else fallbackEngine?.engineName ?: "Stockfish Lite"

    override suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        // Try finding native Stockfish executable
        val executable = findStockfishBinary()
        if (executable != null && executable.exists() && executable.canExecute()) {
            try {
                val pb = ProcessBuilder(executable.absolutePath)
                pb.redirectErrorStream(true)
                val proc = pb.start()
                process = proc
                reader = BufferedReader(InputStreamReader(proc.inputStream))
                writer = BufferedWriter(OutputStreamWriter(proc.outputStream))

                sendCommand("uci")
                var line: String?
                while (reader?.readLine().also { line = it } != null) {
                    if (line == "uciok") {
                        isNativeActive = true
                        break
                    }
                }
                if (isNativeActive) return@withContext true
            } catch (_: Exception) {
                closeProcess()
            }
        }

        // Fallback to embedded pure-Kotlin UCI engine
        isNativeActive = false
        fallbackEngine = StockfishLiteEngine()
        fallbackEngine?.initialize() ?: true
    }

    private fun findStockfishBinary(): File? {
        if (context == null) return null
        val libDir = context.applicationInfo.nativeLibraryDir
        val libFile = File(libDir, "libstockfish.so")
        if (libFile.exists()) return libFile

        val filesDirFile = File(context.filesDir, "stockfish")
        if (filesDirFile.exists()) return filesDirFile

        return null
    }

    private fun sendCommand(cmd: String) {
        writer?.write(cmd + "\n")
        writer?.flush()
    }

    override suspend fun setOption(name: String, value: String) {
        withContext(Dispatchers.IO) {
            if (isNativeActive) {
                sendCommand("setoption name $name value $value")
            } else {
                fallbackEngine?.setOption(name, value)
            }
        }
    }

    override suspend fun newGame() {
        withContext(Dispatchers.IO) {
            if (isNativeActive) {
                sendCommand("ucinewgame")
                sendCommand("isready")
                var line: String?
                while (reader?.readLine().also { line = it } != null) {
                    if (line == "readyok") break
                }
            } else {
                fallbackEngine?.newGame()
            }
        }
    }

    override suspend fun setPosition(fen: String, moves: List<String>) {
        withContext(Dispatchers.IO) {
            if (isNativeActive) {
                val movesStr = if (moves.isNotEmpty()) " moves ${moves.joinToString(" ")}" else ""
                sendCommand("position fen $fen$movesStr")
            } else {
                fallbackEngine?.setPosition(fen, moves)
            }
        }
    }

    override suspend fun search(
        depth: Int,
        movetimeMs: Long?,
        multiPv: Int,
        onInfo: (EngineEvaluation) -> Unit
    ): EngineEvaluation = withContext(Dispatchers.IO) {
        if (!isNativeActive) {
            return@withContext fallbackEngine?.search(depth, movetimeMs, multiPv, onInfo)
                ?: EngineEvaluation()
        }

        if (multiPv > 1) {
            sendCommand("setoption name MultiPV value $multiPv")
        }

        val goCmd = when {
            movetimeMs != null -> "go movetime $movetimeMs"
            else -> "go depth $depth"
        }
        sendCommand(goCmd)

        var latestEval = EngineEvaluation()
        var line: String?
        while (reader?.readLine().also { line = it } != null) {
            val l = line ?: break
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
        latestEval
    }

    override suspend fun stop() {
        withContext(Dispatchers.IO) {
            if (isNativeActive) {
                sendCommand("stop")
            } else {
                fallbackEngine?.stop()
            }
        }
    }

    private fun closeProcess() {
        try {
            sendCommand("quit")
            writer?.close()
            reader?.close()
            process?.destroy()
        } catch (_: Exception) {}
        writer = null
        reader = null
        process = null
    }

    override fun close() {
        closeProcess()
        fallbackEngine?.close()
    }
}
