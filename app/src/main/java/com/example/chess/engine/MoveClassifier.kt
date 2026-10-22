package com.example.chess.engine

import com.example.chess.model.AnalyzedMove
import com.example.chess.model.Move
import com.example.chess.model.MoveClassification
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.rules.ChessPosition
import com.example.chess.rules.OpeningBook
import kotlin.math.exp
import kotlin.math.pow

object MoveClassifier {

    /**
     * Converts evaluation in centipawns to win probability percentage [0..100]
     * based on standard chess logistic model: P(cp) = 100 / (1 + 10^(-cp/400))
     */
    fun winProbability(evalCp: Int, isMate: Boolean = false, mateIn: Int = 0): Double {
        if (isMate) {
            return if (mateIn > 0) 100.0 else 0.0
        }
        val clampedCp = evalCp.coerceIn(-2000, 2000)
        return 100.0 / (1.0 + 10.0.pow(-clampedCp / 400.0))
    }

    /**
     * Classifies a single played move against the engine's suggested best move
     */
    fun classifyMove(
        positionBefore: ChessPosition,
        positionAfter: ChessPosition,
        playedMove: Move,
        bestMove: Move?,
        evalBeforeCp: Int,
        evalAfterCp: Int,
        isMateBefore: Boolean,
        isMateAfter: Boolean,
        mateInBefore: Int,
        mateInAfter: Int,
        moveNumber: Int,
        color: PieceColor,
        sanMovesSoFar: List<String>,
        isUkrainian: Boolean
    ): AnalyzedMove {
        val winProbBefore = winProbability(evalBeforeCp, isMateBefore, mateInBefore)
        val winProbAfter = winProbability(evalAfterCp, isMateAfter, mateInAfter)

        // Calculate win probability loss from the perspective of the moving side
        val playerWinProbBefore = if (color == PieceColor.WHITE) winProbBefore else (100.0 - winProbBefore)
        val playerWinProbAfter = if (color == PieceColor.WHITE) winProbAfter else (100.0 - winProbAfter)
        val winProbLoss = maxOf(0.0, playerWinProbBefore - playerWinProbAfter)

        val cpLoss = maxOf(0, if (color == PieceColor.WHITE) evalBeforeCp - evalAfterCp else evalAfterCp - evalBeforeCp)

        val isBestMatch = bestMove != null && (playedMove.uci == bestMove.uci || playedMove.san == bestMove.san)

        // Check if move was a piece sacrifice that kept advantage (Brilliant candidate)
        val movedPiece = positionBefore.getPiece(playedMove.from)
        val isSacrifice = movedPiece != null &&
                movedPiece.type != PieceType.PAWN &&
                playedMove.isCapture &&
                (positionBefore.getPiece(playedMove.to)?.type?.value ?: 0) < movedPiece.type.value &&
                playerWinProbAfter >= 75.0 &&
                winProbLoss <= 2.0

        val isBook = moveNumber <= 12 && OpeningBook.isBookMove(sanMovesSoFar)

        val classification: MoveClassification
        val explanation: String

        when {
            isSacrifice && isBestMatch -> {
                classification = MoveClassification.BRILLIANT
                explanation = if (isUkrainian)
                    "Жертовний тактичний хід із збереженням вирішальної переваги"
                else
                    "Brilliant tactical piece sacrifice maintaining decisive advantage"
            }
            isBook && winProbLoss < 8.0 -> {
                classification = MoveClassification.BOOK
                explanation = if (isUkrainian)
                    "Теоретичний хід із дебютної книги"
                else
                    "Standard theoretical opening book move"
            }
            isBestMatch || winProbLoss < 1.0 -> {
                classification = MoveClassification.BEST
                explanation = if (isUkrainian)
                    "Найкращий хід за розрахунком рушія"
                else
                    "The top engine choice in this position"
            }
            winProbLoss < 3.5 -> {
                classification = MoveClassification.EXCELLENT
                explanation = if (isUkrainian)
                    "Майже ідеальний хід (втрата ${"%.1f".format(winProbLoss)}% переваги)"
                else
                    "Nearly optimal move (loss of ${"%.1f".format(winProbLoss)}% win chance)"
            }
            winProbLoss < 8.0 -> {
                classification = MoveClassification.GOOD
                explanation = if (isUkrainian)
                    "Надійний і практичний хід"
                else
                    "Solid and practical choice"
            }
            winProbLoss < 16.0 -> {
                classification = MoveClassification.INACCURACY
                explanation = if (isUkrainian)
                    "Неточність: втрачено ${"%.1f".format(winProbLoss)}% очікуваної перемоги"
                else
                    "Inaccuracy: lost ${"%.1f".format(winProbLoss)}% win chance"
            }
            winProbLoss < 32.0 -> {
                classification = MoveClassification.MISTAKE
                explanation = if (isUkrainian)
                    "Помилка: втрата позиції або суттєвого темпу (${"%.1f".format(winProbLoss)}%)"
                else
                    "Mistake: significant loss of position or tempo (${"%.1f".format(winProbLoss)}%)"
            }
            else -> {
                classification = MoveClassification.BLUNDER
                explanation = if (isUkrainian)
                    "Грубий зевок: втрата вирішальної переваги чи матеріалу"
                else
                    "Blunder: major material or decisive advantage lost"
            }
        }

        return AnalyzedMove(
            moveNumber = moveNumber,
            color = color,
            playedMove = playedMove,
            bestMove = bestMove,
            evalBeforeCp = evalBeforeCp,
            evalAfterCp = evalAfterCp,
            isMateBefore = isMateBefore,
            isMateAfter = isMateAfter,
            mateMovesBefore = mateInBefore,
            mateMovesAfter = mateInAfter,
            cpLoss = cpLoss,
            winProbLoss = winProbLoss,
            classification = classification,
            explanation = explanation,
            fenAfter = positionAfter.toFen()
        )
    }

    /**
     * Calculates overall player accuracy (0% to 100%) from win probability losses
     */
    fun calculateAccuracy(analyzedMoves: List<AnalyzedMove>, color: PieceColor): Double {
        val playerMoves = analyzedMoves.filter { it.color == color }
        if (playerMoves.isEmpty()) return 100.0

        val accuracies = playerMoves.map { move ->
            // Move accuracy formula based on win probability loss
            val loss = move.winProbLoss
            val acc = 103.1668 * exp(-0.04354 * loss) - 3.1669
            acc.coerceIn(0.0, 100.0)
        }
        return accuracies.average()
    }
}
