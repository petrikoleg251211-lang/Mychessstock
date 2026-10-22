package com.example.chess.engine

interface UciEngine {
    val engineName: String
    suspend fun initialize(): Boolean
    suspend fun setOption(name: String, value: String)
    suspend fun newGame()
    suspend fun setPosition(fen: String, moves: List<String> = emptyList())
    suspend fun search(
        depth: Int,
        movetimeMs: Long? = null,
        multiPv: Int = 1,
        onInfo: (EngineEvaluation) -> Unit = {}
    ): EngineEvaluation
    suspend fun stop()
    fun close()
}

object UciParser {

    fun parseInfoLine(line: String): EngineEvaluation? {
        if (!line.startsWith("info ")) return null

        val tokens = line.split("\\s+".toRegex())
        var depth = 0
        var seldepth = 0
        var scoreCp: Int? = null
        var scoreMate: Int? = null
        var nodes = 0L
        var nps = 0L
        var timeMs = 0L
        var multipv = 1
        val pv = mutableListOf<String>()

        var i = 1
        while (i < tokens.size) {
            when (tokens[i]) {
                "depth" -> {
                    if (i + 1 < tokens.size) depth = tokens[i + 1].toIntOrNull() ?: depth
                    i += 2
                }
                "seldepth" -> {
                    if (i + 1 < tokens.size) seldepth = tokens[i + 1].toIntOrNull() ?: seldepth
                    i += 2
                }
                "score" -> {
                    if (i + 2 < tokens.size) {
                        when (tokens[i + 1]) {
                            "cp" -> scoreCp = tokens[i + 2].toIntOrNull()
                            "mate" -> scoreMate = tokens[i + 2].toIntOrNull()
                        }
                    }
                    i += 3
                }
                "nodes" -> {
                    if (i + 1 < tokens.size) nodes = tokens[i + 1].toLongOrNull() ?: nodes
                    i += 2
                }
                "nps" -> {
                    if (i + 1 < tokens.size) nps = tokens[i + 1].toLongOrNull() ?: nps
                    i += 2
                }
                "time" -> {
                    if (i + 1 < tokens.size) timeMs = tokens[i + 1].toLongOrNull() ?: timeMs
                    i += 2
                }
                "multipv" -> {
                    if (i + 1 < tokens.size) multipv = tokens[i + 1].toIntOrNull() ?: multipv
                    i += 2
                }
                "pv" -> {
                    i++
                    while (i < tokens.size && !tokens[i].startsWith("string")) {
                        pv.add(tokens[i])
                        i++
                    }
                }
                else -> i++
            }
        }

        return EngineEvaluation(
            depth = depth,
            seldepth = seldepth,
            scoreCp = scoreCp,
            scoreMate = scoreMate,
            nodes = nodes,
            nps = nps,
            timeMs = timeMs,
            pv = pv,
            bestMoveUci = pv.firstOrNull(),
            multipv = multipv
        )
    }

    fun parseBestMove(line: String): String? {
        if (!line.startsWith("bestmove")) return null
        val parts = line.split("\\s+".toRegex())
        return if (parts.size >= 2) parts[1] else null
    }
}
