package com.example.chess.model

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val symbol: Char, val value: Int) {
    PAWN('p', 100),
    KNIGHT('n', 320),
    BISHOP('b', 330),
    ROOK('r', 500),
    QUEEN('q', 900),
    KING('k', 20000);

    companion object {
        fun fromChar(c: Char): PieceType? {
            return when (c.lowercaseChar()) {
                'p' -> PAWN
                'n' -> KNIGHT
                'b' -> BISHOP
                'r' -> ROOK
                'q' -> QUEEN
                'k' -> KING
                else -> null
            }
        }
    }
}

data class Piece(val type: PieceType, val color: PieceColor) {
    val fenChar: Char
        get() = if (color == PieceColor.WHITE) type.symbol.uppercaseChar() else type.symbol
}

data class CastlingRights(
    val whiteKingSide: Boolean = true,
    val whiteQueenSide: Boolean = true,
    val blackKingSide: Boolean = true,
    val blackQueenSide: Boolean = true
) {
    fun toFen(): String {
        val sb = StringBuilder()
        if (whiteKingSide) sb.append('K')
        if (whiteQueenSide) sb.append('Q')
        if (blackKingSide) sb.append('k')
        if (blackQueenSide) sb.append('q')
        return if (sb.isEmpty()) "-" else sb.toString()
    }

    companion object {
        fun fromFen(fen: String): CastlingRights {
            return CastlingRights(
                whiteKingSide = fen.contains('K'),
                whiteQueenSide = fen.contains('Q'),
                blackKingSide = fen.contains('k'),
                blackQueenSide = fen.contains('q')
            )
        }
    }
}

data class Move(
    val from: Int,
    val to: Int,
    val promotion: PieceType? = null,
    val isCapture: Boolean = false,
    val isEnPassant: Boolean = false,
    val isCastling: Boolean = false,
    val san: String = ""
) {
    val uci: String
        get() {
            val fromStr = squareToAlgebraic(from)
            val toStr = squareToAlgebraic(to)
            val promoStr = promotion?.symbol?.lowercaseChar()?.toString() ?: ""
            return "$fromStr$toStr$promoStr"
        }

    companion object {
        fun squareToAlgebraic(square: Int): String {
            if (square !in 0..63) return "-"
            val file = square % 8
            val rank = square / 8
            val fileChar = ('a'.code + file).toChar()
            val rankChar = ('1'.code + rank).toChar()
            return "$fileChar$rankChar"
        }

        fun algebraicToSquare(alg: String): Int {
            if (alg.length < 2) return -1
            val file = alg[0] - 'a'
            val rank = alg[1] - '1'
            if (file !in 0..7 || rank !in 0..7) return -1
            return rank * 8 + file
        }
    }
}

enum class MoveClassification(val badge: String, val colorHex: Long) {
    BRILLIANT("!!", 0xFF00E5FF),
    BEST("★", 0xFF00C853),
    EXCELLENT("✓", 0xFF64DD17),
    GOOD("•", 0xFF81C784),
    BOOK("📖", 0xFFA1887F),
    INACCURACY("?!", 0xFFFFD600),
    MISTAKE("?", 0xFFFF9100),
    BLUNDER("??", 0xFFD50000);

    fun getDisplayName(isUkrainian: Boolean): String {
        return if (isUkrainian) {
            when (this) {
                BRILLIANT -> "Геніальний"
                BEST -> "Найкращий"
                EXCELLENT -> "Відмінний"
                GOOD -> "Хороший"
                BOOK -> "Теорія"
                INACCURACY -> "Неточність"
                MISTAKE -> "Помилка"
                BLUNDER -> "Зевок"
            }
        } else {
            when (this) {
                BRILLIANT -> "Brilliant"
                BEST -> "Best"
                EXCELLENT -> "Excellent"
                GOOD -> "Good"
                BOOK -> "Book"
                INACCURACY -> "Inaccuracy"
                MISTAKE -> "Mistake"
                BLUNDER -> "Blunder"
            }
        }
    }
}

data class AnalyzedMove(
    val moveNumber: Int,
    val color: PieceColor,
    val playedMove: Move,
    val bestMove: Move?,
    val evalBeforeCp: Int,
    val evalAfterCp: Int,
    val isMateBefore: Boolean = false,
    val isMateAfter: Boolean = false,
    val mateMovesBefore: Int = 0,
    val mateMovesAfter: Int = 0,
    val cpLoss: Int,
    val winProbLoss: Double,
    val classification: MoveClassification,
    val explanation: String = "",
    val fenAfter: String = ""
)

data class GameReviewReport(
    val whiteAccuracy: Double,
    val blackAccuracy: Double,
    val whiteMoveStats: Map<MoveClassification, Int>,
    val blackMoveStats: Map<MoveClassification, Int>,
    val analyzedMoves: List<AnalyzedMove>,
    val evalHistory: List<Float>
)
