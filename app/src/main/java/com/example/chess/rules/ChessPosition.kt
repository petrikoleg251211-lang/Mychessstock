package com.example.chess.rules

import com.example.chess.model.CastlingRights
import com.example.chess.model.Move
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType

data class ChessPosition(
    val board: Array<Piece?> = Array(64) { null },
    var sideToMove: PieceColor = PieceColor.WHITE,
    var castlingRights: CastlingRights = CastlingRights(),
    var enPassantSquare: Int? = null,
    var halfmoveClock: Int = 0,
    var fullmoveNumber: Int = 1
) {
    fun copy(): ChessPosition {
        return ChessPosition(
            board = board.copyOf(),
            sideToMove = sideToMove,
            castlingRights = castlingRights.copy(),
            enPassantSquare = enPassantSquare,
            halfmoveClock = halfmoveClock,
            fullmoveNumber = fullmoveNumber
        )
    }

    fun getPiece(square: Int): Piece? = if (square in 0..63) board[square] else null
    fun setPiece(square: Int, piece: Piece?) {
        if (square in 0..63) board[square] = piece
    }

    fun toFen(): String {
        val sb = StringBuilder()
        for (rank in 7 downTo 0) {
            var empty = 0
            for (file in 0..7) {
                val p = board[rank * 8 + file]
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    sb.append(p.fenChar)
                }
            }
            if (empty > 0) sb.append(empty)
            if (rank > 0) sb.append('/')
        }
        sb.append(if (sideToMove == PieceColor.WHITE) " w " else " b ")
        sb.append(castlingRights.toFen()).append(' ')
        sb.append(if (enPassantSquare != null) Move.squareToAlgebraic(enPassantSquare!!) else "-")
        sb.append(" ").append(halfmoveClock).append(" ").append(fullmoveNumber)
        return sb.toString()
    }

    companion object {
        const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun fromFen(fen: String): ChessPosition {
            val parts = fen.trim().split("\\s+".toRegex())
            val position = ChessPosition()
            if (parts.isEmpty()) return position

            val ranks = parts[0].split('/')
            if (ranks.size == 8) {
                for (rankIdx in 0..7) {
                    val rank = 7 - rankIdx
                    var file = 0
                    for (c in ranks[rankIdx]) {
                        if (c.isDigit()) {
                            file += c.digitToInt()
                        } else {
                            val color = if (c.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                            val type = PieceType.fromChar(c)
                            if (type != null && file in 0..7) {
                                position.board[rank * 8 + file] = Piece(type, color)
                            }
                            file++
                        }
                    }
                }
            }

            if (parts.size > 1) {
                position.sideToMove = if (parts[1] == "b") PieceColor.BLACK else PieceColor.WHITE
            }
            if (parts.size > 2) {
                position.castlingRights = CastlingRights.fromFen(parts[2])
            }
            if (parts.size > 3 && parts[3] != "-") {
                position.enPassantSquare = Move.algebraicToSquare(parts[3]).takeIf { it in 0..63 }
            }
            if (parts.size > 4) {
                position.halfmoveClock = parts[4].toIntOrNull() ?: 0
            }
            if (parts.size > 5) {
                position.fullmoveNumber = parts[5].toIntOrNull() ?: 1
            }

            return position
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChessPosition) return false
        return board.contentEquals(other.board) &&
                sideToMove == other.sideToMove &&
                castlingRights == other.castlingRights &&
                enPassantSquare == other.enPassantSquare
    }

    override fun hashCode(): Int {
        var result = board.contentHashCode()
        result = 31 * result + sideToMove.hashCode()
        result = 31 * result + castlingRights.hashCode()
        result = 31 * result + (enPassantSquare ?: 0)
        return result
    }
}
