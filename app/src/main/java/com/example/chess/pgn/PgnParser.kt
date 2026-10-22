package com.example.chess.pgn

import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.rules.ChessPosition
import com.example.chess.rules.ChessRulesEngine

data class PgnGame(
    val headers: Map<String, String>,
    val moves: List<Move>,
    val initialFen: String = ChessPosition.STARTING_FEN,
    val positions: List<ChessPosition> = emptyList(),
    val comments: Map<Int, String> = emptyMap()
) {
    val white: String get() = headers["White"] ?: "White"
    val black: String get() = headers["Black"] ?: "Black"
    val whiteElo: String? get() = headers["WhiteElo"]
    val blackElo: String? get() = headers["BlackElo"]
    val event: String get() = headers["Event"] ?: "Casual Game"
    val site: String get() = headers["Site"] ?: "Chess App"
    val date: String get() = headers["Date"] ?: "????"
    val result: String get() = headers["Result"] ?: "*"
    val eco: String? get() = headers["ECO"]
}

object PgnParser {

    fun parse(pgnText: String): PgnGame {
        val headers = mutableMapOf<String, String>()
        val moveTokens = mutableListOf<String>()
        val comments = mutableMapOf<Int, String>()

        val lines = pgnText.lines()
        var parsingHeaders = true
        val moveTextBuilder = StringBuilder()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                if (parsingHeaders && headers.isNotEmpty()) {
                    parsingHeaders = false
                }
                continue
            }

            if (parsingHeaders && trimmed.startsWith("[") && trimmed.endsWith("]")) {
                val tagContent = trimmed.substring(1, trimmed.length - 1)
                val spaceIdx = tagContent.indexOf(' ')
                if (spaceIdx > 0) {
                    val key = tagContent.substring(0, spaceIdx).trim()
                    val value = tagContent.substring(spaceIdx + 1).replace("\"", "").trim()
                    headers[key] = value
                }
            } else {
                parsingHeaders = false
                moveTextBuilder.append(" ").append(trimmed)
            }
        }

        val rawMoves = moveTextBuilder.toString()
        val cleanedTokens = tokenizeMoves(rawMoves)

        val initialFen = headers["FEN"] ?: ChessPosition.STARTING_FEN
        var currentPosition = ChessPosition.fromFen(initialFen)
        val verifiedMoves = mutableListOf<Move>()
        val positions = mutableListOf<ChessPosition>()
        positions.add(currentPosition)

        for (token in cleanedTokens) {
            if (token == "1-0" || token == "0-1" || token == "1/2-1/2" || token == "*") {
                if (!headers.containsKey("Result")) {
                    headers["Result"] = token
                }
                break
            }

            val parsedMove = ChessRulesEngine.parseSan(currentPosition, token)
            if (parsedMove != null) {
                verifiedMoves.add(parsedMove)
                currentPosition = ChessRulesEngine.makeMove(currentPosition, parsedMove)
                positions.add(currentPosition)
            }
        }

        return PgnGame(
            headers = headers,
            moves = verifiedMoves,
            initialFen = initialFen,
            positions = positions,
            comments = comments
        )
    }

    private fun tokenizeMoves(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inComment = false
        var inVariation = 0

        for (ch in text) {
            when (ch) {
                '{' -> inComment = true
                '}' -> {
                    inComment = false
                    continue
                }
                '(' -> {
                    inVariation++
                    continue
                }
                ')' -> {
                    if (inVariation > 0) inVariation--
                    continue
                }
            }

            if (inComment || inVariation > 0) continue

            if (ch.isWhitespace()) {
                if (sb.isNotEmpty()) {
                    addTokenIfValid(sb.toString(), tokens)
                    sb.clear()
                }
            } else {
                sb.append(ch)
            }
        }
        if (sb.isNotEmpty()) {
            addTokenIfValid(sb.toString(), tokens)
        }

        return tokens
    }

    private fun addTokenIfValid(token: String, target: MutableList<String>) {
        var t = token.trim()
        if (t.isEmpty()) return
        // Strip move numbering like 1. or 12...
        if (t.matches("\\d+\\.+".toRegex())) return
        if (t.matches("\\d+\\..+".toRegex())) {
            t = t.substringAfter(".").trim()
            if (t.isEmpty()) return
        }
        if (t.startsWith("$")) return // NAG
        target.add(t)
    }

    fun exportToPgn(game: PgnGame, moveEvaluations: Map<Int, String> = emptyMap()): String {
        val sb = StringBuilder()
        sb.append("[Event \"${game.event}\"]\n")
        sb.append("[Site \"${game.site}\"]\n")
        sb.append("[Date \"${game.date}\"]\n")
        sb.append("[White \"${game.white}\"]\n")
        sb.append("[Black \"${game.black}\"]\n")
        sb.append("[Result \"${game.result}\"]\n")
        game.whiteElo?.let { sb.append("[WhiteElo \"$it\"]\n") }
        game.blackElo?.let { sb.append("[BlackElo \"$it\"]\n") }
        game.eco?.let { sb.append("[ECO \"$it\"]\n") }
        if (game.initialFen != ChessPosition.STARTING_FEN) {
            sb.append("[SetUp \"1\"]\n")
            sb.append("[FEN \"${game.initialFen}\"]\n")
        }
        sb.append("\n")

        var moveNum = 1
        for (i in game.moves.indices) {
            val move = game.moves[i]
            if (i % 2 == 0) {
                sb.append("$moveNum. ")
            }
            sb.append(move.san).append(" ")
            val eval = moveEvaluations[i]
            if (eval != null) {
                sb.append("{ [%eval $eval] } ")
            }
            if (i % 2 == 1) {
                moveNum++
            }
        }
        sb.append(game.result).append("\n")
        return sb.toString()
    }
}
