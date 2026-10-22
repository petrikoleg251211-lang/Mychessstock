package com.example.chess.engine

import com.example.chess.model.AnalyzedMove
import com.example.chess.model.GameReviewReport
import com.example.chess.model.MoveClassification
import com.example.chess.model.PieceColor
import com.example.chess.pgn.PgnGame
import com.example.chess.rules.ChessPosition
import com.example.chess.rules.ChessRulesEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class ReviewProgress(
    val currentMoveIndex: Int,
    val totalMoves: Int,
    val currentSan: String,
    val currentDepth: Int,
    val currentScore: String,
    val nps: Long,
    val isRunning: Boolean
)

class GameReviewEngine(private val engine: UciEngine) {

    suspend fun reviewGame(
        game: PgnGame,
        depth: Int = 18,
        timePerMoveMs: Long = 1000L,
        isUkrainian: Boolean = true,
        onProgress: (ReviewProgress) -> Unit = {}
    ): GameReviewReport = withContext(Dispatchers.Default) {
        val moves = game.moves
        val totalMoves = moves.size
        val analyzedMoves = mutableListOf<AnalyzedMove>()
        val evalHistory = mutableListOf<Float>()

        var currentPos = ChessPosition.fromFen(game.initialFen)
        engine.newGame()

        // 1. Initial position evaluation
        engine.setPosition(currentPos.toFen())
        val initialEval = engine.search(depth, timePerMoveMs)
        var prevEvalCp = initialEval.numericCpValue
        var prevIsMate = initialEval.scoreMate != null
        var prevMateIn = initialEval.scoreMate ?: 0
        evalHistory.add((prevEvalCp / 100f).coerceIn(-15f, 15f))

        val sanMovesSoFar = mutableListOf<String>()

        for (index in moves.indices) {
            if (!coroutineContext.isActive) break

            val playedMove = moves[index]
            val color = currentPos.sideToMove
            val moveNumber = (index / 2) + 1
            sanMovesSoFar.add(playedMove.san)

            // Find best move for current position before move
            engine.setPosition(currentPos.toFen())
            val positionEval = engine.search(depth, timePerMoveMs) { info ->
                onProgress(
                    ReviewProgress(
                        currentMoveIndex = index + 1,
                        totalMoves = totalMoves,
                        currentSan = playedMove.san,
                        currentDepth = info.depth,
                        currentScore = info.formattedScore,
                        nps = info.nps,
                        isRunning = true
                    )
                )
            }

            val bestMoveUci = positionEval.bestMoveUci
            val bestMove = bestMoveUci?.let { ChessRulesEngine.parseUci(currentPos, it) }

            // Execute played move
            val nextPos = ChessRulesEngine.makeMove(currentPos, playedMove)

            // Evaluate resulting position
            engine.setPosition(nextPos.toFen())
            val nextEval = engine.search(depth, timePerMoveMs)
            val currentEvalCp = nextEval.numericCpValue
            val currentIsMate = nextEval.scoreMate != null
            val currentMateIn = nextEval.scoreMate ?: 0

            val analyzed = MoveClassifier.classifyMove(
                positionBefore = currentPos,
                positionAfter = nextPos,
                playedMove = playedMove,
                bestMove = bestMove,
                evalBeforeCp = prevEvalCp,
                evalAfterCp = currentEvalCp,
                isMateBefore = prevIsMate,
                isMateAfter = currentIsMate,
                mateInBefore = prevMateIn,
                mateInAfter = currentMateIn,
                moveNumber = moveNumber,
                color = color,
                sanMovesSoFar = sanMovesSoFar,
                isUkrainian = isUkrainian
            )

            analyzedMoves.add(analyzed)
            evalHistory.add((currentEvalCp / 100f).coerceIn(-15f, 15f))

            prevEvalCp = currentEvalCp
            prevIsMate = currentIsMate
            prevMateIn = currentMateIn
            currentPos = nextPos
        }

        val whiteAccuracy = MoveClassifier.calculateAccuracy(analyzedMoves, PieceColor.WHITE)
        val blackAccuracy = MoveClassifier.calculateAccuracy(analyzedMoves, PieceColor.BLACK)

        val whiteStats = MoveClassification.values().associateWith { classification ->
            analyzedMoves.count { it.color == PieceColor.WHITE && it.classification == classification }
        }
        val blackStats = MoveClassification.values().associateWith { classification ->
            analyzedMoves.count { it.color == PieceColor.BLACK && it.classification == classification }
        }

        onProgress(
            ReviewProgress(
                currentMoveIndex = totalMoves,
                totalMoves = totalMoves,
                currentSan = "Done",
                currentDepth = depth,
                currentScore = "Done",
                nps = 0L,
                isRunning = false
            )
        )

        return@withContext GameReviewReport(
            whiteAccuracy = whiteAccuracy,
            blackAccuracy = blackAccuracy,
            whiteMoveStats = whiteStats,
            blackMoveStats = blackStats,
            analyzedMoves = analyzedMoves,
            evalHistory = evalHistory
        )
    }
}
