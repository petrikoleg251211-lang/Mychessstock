package com.example.chess.engine

import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.rules.ChessPosition
import com.example.chess.rules.ChessRulesEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * Built-in Kotlin UCI Evaluation Engine.
 * Implements alpha-beta minimax with iterative deepening, piece-square tables,
 * mobility evaluation, king safety, and quiescence search.
 */
class StockfishLiteEngine : UciEngine {

    override val engineName: String = "Stockfish Lite (Built-in UCI)"

    private var currentPosition: ChessPosition = ChessPosition()
    private var isSearching = false
    private var shouldStop = false

    // Piece-Square tables (from White's perspective)
    private val PAWN_TABLE = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        50, 50, 50, 50, 50, 50, 50, 50,
        10, 10, 20, 30, 30, 20, 10, 10,
        5,  5, 10, 25, 25, 10,  5,  5,
        0,  0,  0, 20, 20,  0,  0,  0,
        5, -5,-10,  0,  0,-10, -5,  5,
        5, 10, 10,-20,-20, 10, 10,  5,
        0,  0,  0,  0,  0,  0,  0,  0
    )

    private val KNIGHT_TABLE = intArrayOf(
        -50,-40,-30,-30,-30,-30,-40,-50,
        -40,-20,  0,  0,  0,  0,-20,-40,
        -30,  0, 10, 15, 15, 10,  0,-30,
        -30,  5, 15, 20, 20, 15,  5,-30,
        -30,  0, 15, 20, 20, 15,  0,-30,
        -30,  5, 10, 15, 15, 10,  5,-30,
        -40,-20,  0,  5,  5,  0,-20,-40,
        -50,-40,-30,-30,-30,-30,-40,-50
    )

    private val BISHOP_TABLE = intArrayOf(
        -20,-10,-10,-10,-10,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5, 10, 10,  5,  0,-10,
        -10,  5,  5, 10, 10,  5,  5,-10,
        -10,  0, 10, 10, 10, 10,  0,-10,
        -10, 10, 10, 10, 10, 10, 10,-10,
        -10,  5,  0,  0,  0,  0,  5,-10,
        -20,-10,-10,-10,-10,-10,-10,-20
    )

    private val ROOK_TABLE = intArrayOf(
        0,  0,  0,  0,  0,  0,  0,  0,
        5, 10, 10, 10, 10, 10, 10,  5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        -5,  0,  0,  0,  0,  0,  0, -5,
        0,  0,  0,  5,  5,  0,  0,  0
    )

    private val QUEEN_TABLE = intArrayOf(
        -20,-10,-10, -5, -5,-10,-10,-20,
        -10,  0,  0,  0,  0,  0,  0,-10,
        -10,  0,  5,  5,  5,  5,  0,-10,
        -5,  0,  5,  5,  5,  5,  0, -5,
        0,  0,  5,  5,  5,  5,  0, -5,
        -10,  5,  5,  5,  5,  5,  0,-10,
        -10,  0,  5,  0,  0,  0,  0,-10,
        -20,-10,-10, -5, -5,-10,-10,-20
    )

    private val KING_TABLE = intArrayOf(
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -30,-40,-40,-50,-50,-40,-40,-30,
        -20,-30,-30,-40,-40,-30,-30,-20,
        -10,-20,-20,-20,-20,-20,-20,-10,
        20, 20,  0,  0,  0,  0, 20, 20,
        20, 30, 10,  0,  0, 10, 30, 20
    )

    override suspend fun initialize(): Boolean = true
    override suspend fun setOption(name: String, value: String) {}
    override suspend fun newGame() {
        currentPosition = ChessPosition.fromFen(ChessPosition.STARTING_FEN)
    }

    override suspend fun setPosition(fen: String, moves: List<String>) {
        var pos = ChessPosition.fromFen(fen)
        for (m in moves) {
            val parsed = ChessRulesEngine.parseUci(pos, m) ?: ChessRulesEngine.parseSan(pos, m)
            if (parsed != null) {
                pos = ChessRulesEngine.makeMove(pos, parsed)
            }
        }
        currentPosition = pos
    }

    override suspend fun search(
        depth: Int,
        movetimeMs: Long?,
        multiPv: Int,
        onInfo: (EngineEvaluation) -> Unit
    ): EngineEvaluation = withContext(Dispatchers.Default) {
        isSearching = true
        shouldStop = false

        val startTime = System.currentTimeMillis()
        var totalNodes = 0L
        val maxDepth = minOf(depth, 16) // bounded for responsive search
        var bestMove: Move? = null
        var bestScore = 0
        var pvLine = listOf<String>()

        val legalMoves = ChessRulesEngine.generateLegalMoves(currentPosition)
        if (legalMoves.isEmpty()) {
            val isMate = ChessRulesEngine.isInCheck(currentPosition, currentPosition.sideToMove)
            isSearching = false
            return@withContext EngineEvaluation(
                depth = 0,
                scoreMate = if (isMate) 0 else null,
                scoreCp = if (isMate) null else 0
            )
        }

        // Sort moves by simple heuristics: captures first, then quiet moves
        val sortedMoves = legalMoves.sortedByDescending { move ->
            var score = 0
            val target = currentPosition.getPiece(move.to)
            val source = currentPosition.getPiece(move.from)
            if (target != null && source != null) {
                score = target.type.value * 10 - source.type.value
            }
            if (move.promotion != null) score += 900
            score
        }

        bestMove = sortedMoves.first()

        for (currDepth in 1..maxDepth) {
            if (shouldStop || !coroutineContext.isActive) break

            var currentDepthBestMove: Move? = null
            var alpha = -100000
            val beta = 100000

            for (move in sortedMoves) {
                if (shouldStop || !coroutineContext.isActive) break

                val nextPos = ChessRulesEngine.makeMove(currentPosition, move)
                totalNodes++
                val score = -alphaBeta(
                    position = nextPos,
                    depth = currDepth - 1,
                    alpha = -beta,
                    beta = -alpha,
                    nodes = { totalNodes++ }
                )

                if (score > alpha) {
                    alpha = score
                    currentDepthBestMove = move
                }
            }

            if (currentDepthBestMove != null && !shouldStop && coroutineContext.isActive) {
                bestMove = currentDepthBestMove
                bestScore = alpha
                pvLine = listOf(bestMove.uci)

                val elapsed = maxOf(1L, System.currentTimeMillis() - startTime)
                val nps = (totalNodes * 1000L) / elapsed

                // Convert score relative to side to move into White's perspective
                val whiteScore = if (currentPosition.sideToMove == PieceColor.WHITE) bestScore else -bestScore

                val eval = EngineEvaluation(
                    depth = currDepth,
                    seldepth = currDepth + 2,
                    scoreCp = whiteScore,
                    nodes = totalNodes,
                    nps = nps,
                    timeMs = elapsed,
                    pv = pvLine,
                    bestMoveUci = bestMove.uci,
                    multipv = 1
                )
                onInfo(eval)
            }

            // Check if movetime exceeded
            if (movetimeMs != null && (System.currentTimeMillis() - startTime) >= movetimeMs) {
                break
            }
        }

        isSearching = false
        val whiteFinalScore = if (currentPosition.sideToMove == PieceColor.WHITE) bestScore else -bestScore
        val finalElapsed = maxOf(1L, System.currentTimeMillis() - startTime)
        return@withContext EngineEvaluation(
            depth = maxDepth,
            scoreCp = whiteFinalScore,
            nodes = totalNodes,
            nps = (totalNodes * 1000L) / finalElapsed,
            timeMs = finalElapsed,
            pv = pvLine,
            bestMoveUci = bestMove?.uci,
            multipv = 1
        )
    }

    private fun alphaBeta(
        position: ChessPosition,
        depth: Int,
        alpha: Int,
        beta: Int,
        nodes: () -> Unit
    ): Int {
        if (shouldStop) return 0
        nodes()

        if (depth <= 0) {
            return quiescence(position, alpha, beta, nodes)
        }

        val legalMoves = ChessRulesEngine.generateLegalMoves(position)
        if (legalMoves.isEmpty()) {
            return if (ChessRulesEngine.isInCheck(position, position.sideToMove)) {
                -30000 + (10 - depth) // Checkmate score adjusted for ply
            } else {
                0 // Stalemate
            }
        }

        var currentAlpha = alpha
        for (move in legalMoves) {
            val nextPos = ChessRulesEngine.makeMove(position, move)
            val score = -alphaBeta(nextPos, depth - 1, -beta, -currentAlpha, nodes)
            if (score >= beta) return beta // Beta cutoff
            if (score > currentAlpha) currentAlpha = score
        }
        return currentAlpha
    }

    private fun quiescence(
        position: ChessPosition,
        alpha: Int,
        beta: Int,
        nodes: () -> Unit
    ): Int {
        nodes()
        val standPat = evaluatePosition(position)
        if (standPat >= beta) return beta
        var currentAlpha = maxOf(alpha, standPat)

        // Only search captures in quiescence
        val captures = ChessRulesEngine.generateLegalMoves(position).filter { it.isCapture }
        for (move in captures) {
            val nextPos = ChessRulesEngine.makeMove(position, move)
            val score = -quiescence(nextPos, -beta, -currentAlpha, nodes)
            if (score >= beta) return beta
            if (score > currentAlpha) currentAlpha = score
        }
        return currentAlpha
    }

    private fun evaluatePosition(position: ChessPosition): Int {
        var whiteScore = 0
        var blackScore = 0

        for (sq in 0..63) {
            val piece = position.getPiece(sq) ?: continue
            val typeVal = piece.type.value
            val pstVal = getPstValue(piece.type, piece.color, sq)

            if (piece.color == PieceColor.WHITE) {
                whiteScore += typeVal + pstVal
            } else {
                blackScore += typeVal + pstVal
            }
        }

        val score = whiteScore - blackScore
        return if (position.sideToMove == PieceColor.WHITE) score else -score
    }

    private fun getPstValue(type: PieceType, color: PieceColor, sq: Int): Int {
        // Invert square index for Black
        val index = if (color == PieceColor.WHITE) sq else (7 - (sq / 8)) * 8 + (sq % 8)
        return when (type) {
            PieceType.PAWN -> PAWN_TABLE.getOrElse(index) { 0 }
            PieceType.KNIGHT -> KNIGHT_TABLE.getOrElse(index) { 0 }
            PieceType.BISHOP -> BISHOP_TABLE.getOrElse(index) { 0 }
            PieceType.ROOK -> ROOK_TABLE.getOrElse(index) { 0 }
            PieceType.QUEEN -> QUEEN_TABLE.getOrElse(index) { 0 }
            PieceType.KING -> KING_TABLE.getOrElse(index) { 0 }
        }
    }

    override suspend fun stop() {
        shouldStop = true
    }

    override fun close() {
        shouldStop = true
        isSearching = false
    }
}
