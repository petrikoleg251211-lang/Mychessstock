package com.example.chess.rules

import com.example.chess.model.Move
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType

object ChessRulesEngine {

    private val KNIGHT_OFFSETS = intArrayOf(-17, -15, -10, -6, 6, 10, 15, 17)
    private val BISHOP_DIRS = arrayOf(Pair(1, 1), Pair(1, -1), Pair(-1, 1), Pair(-1, -1))
    private val ROOK_DIRS = arrayOf(Pair(1, 0), Pair(-1, 0), Pair(0, 1), Pair(0, -1))
    private val KING_OFFSETS = intArrayOf(-9, -8, -7, -1, 1, 7, 8, 9)

    fun generateLegalMoves(position: ChessPosition): List<Move> {
        val pseudoMoves = generatePseudoLegalMoves(position)
        val legalMoves = ArrayList<Move>(pseudoMoves.size)
        val side = position.sideToMove

        for (m in pseudoMoves) {
            val nextPos = makeMove(position, m)
            if (!isInCheck(nextPos, side)) {
                val san = moveToSan(position, m)
                legalMoves.add(m.copy(san = san))
            }
        }
        return legalMoves
    }

    fun generateLegalMovesForSquare(position: ChessPosition, square: Int): List<Move> {
        return generateLegalMoves(position).filter { it.from == square }
    }

    fun makeMove(position: ChessPosition, move: Move): ChessPosition {
        val newPos = position.copy()
        val piece = newPos.getPiece(move.from) ?: return newPos
        val color = piece.color
        val opponent = color.opposite()

        newPos.setPiece(move.from, null)

        var isCapture = false
        val targetPiece = newPos.getPiece(move.to)
        if (targetPiece != null) {
            isCapture = true
        }

        // Handle En Passant
        if (piece.type == PieceType.PAWN && move.to == position.enPassantSquare) {
            val capSq = if (color == PieceColor.WHITE) move.to - 8 else move.to + 8
            newPos.setPiece(capSq, null)
            isCapture = true
        }

        // Handle Castling King & Rook movement
        if (piece.type == PieceType.KING) {
            if (color == PieceColor.WHITE) {
                if (move.from == 4 && move.to == 6) { // O-O
                    newPos.setPiece(7, null)
                    newPos.setPiece(5, Piece(PieceType.ROOK, PieceColor.WHITE))
                } else if (move.from == 4 && move.to == 2) { // O-O-O
                    newPos.setPiece(0, null)
                    newPos.setPiece(3, Piece(PieceType.ROOK, PieceColor.WHITE))
                }
                newPos.castlingRights = newPos.castlingRights.copy(whiteKingSide = false, whiteQueenSide = false)
            } else {
                if (move.from == 60 && move.to == 62) { // O-O
                    newPos.setPiece(63, null)
                    newPos.setPiece(61, Piece(PieceType.ROOK, PieceColor.BLACK))
                } else if (move.from == 60 && move.to == 58) { // O-O-O
                    newPos.setPiece(56, null)
                    newPos.setPiece(59, Piece(PieceType.ROOK, PieceColor.BLACK))
                }
                newPos.castlingRights = newPos.castlingRights.copy(blackKingSide = false, blackQueenSide = false)
            }
        }

        // Place piece or promoted piece
        if (move.promotion != null) {
            newPos.setPiece(move.to, Piece(move.promotion, color))
        } else {
            newPos.setPiece(move.to, piece)
        }

        // Update Castling rights if rook moved or captured
        val cr = newPos.castlingRights
        if (move.from == 0 || move.to == 0) newPos.castlingRights = cr.copy(whiteQueenSide = false)
        if (move.from == 7 || move.to == 7) newPos.castlingRights = cr.copy(whiteKingSide = false)
        if (move.from == 56 || move.to == 56) newPos.castlingRights = newPos.castlingRights.copy(blackQueenSide = false)
        if (move.from == 63 || move.to == 63) newPos.castlingRights = newPos.castlingRights.copy(blackKingSide = false)

        // En passant square update
        if (piece.type == PieceType.PAWN && Math.abs(move.to - move.from) == 16) {
            newPos.enPassantSquare = (move.from + move.to) / 2
        } else {
            newPos.enPassantSquare = null
        }

        // Clock updates
        if (piece.type == PieceType.PAWN || isCapture) {
            newPos.halfmoveClock = 0
        } else {
            newPos.halfmoveClock++
        }

        if (color == PieceColor.BLACK) {
            newPos.fullmoveNumber++
        }
        newPos.sideToMove = opponent

        return newPos
    }

    fun isInCheck(position: ChessPosition, color: PieceColor): Boolean {
        val kingSq = findKing(position, color)
        if (kingSq == -1) return false
        return isSquareAttacked(position, kingSq, color.opposite())
    }

    fun isCheckmate(position: ChessPosition): Boolean {
        return isInCheck(position, position.sideToMove) && generateLegalMoves(position).isEmpty()
    }

    fun isStalemate(position: ChessPosition): Boolean {
        return !isInCheck(position, position.sideToMove) && generateLegalMoves(position).isEmpty()
    }

    fun isInsufficientMaterial(position: ChessPosition): Boolean {
        val pieces = position.board.filterNotNull()
        if (pieces.size <= 2) return true // Only Kings
        if (pieces.size == 3) {
            val nonKing = pieces.firstOrNull { it.type != PieceType.KING }
            if (nonKing?.type == PieceType.KNIGHT || nonKing?.type == PieceType.BISHOP) {
                return true
            }
        }
        return false
    }

    fun findKing(position: ChessPosition, color: PieceColor): Int {
        for (i in 0..63) {
            val p = position.board[i]
            if (p != null && p.type == PieceType.KING && p.color == color) {
                return i
            }
        }
        return -1
    }

    fun isSquareAttacked(position: ChessPosition, square: Int, byColor: PieceColor): Boolean {
        val rank = square / 8
        val file = square % 8

        // Pawns
        val pawnDir = if (byColor == PieceColor.WHITE) -1 else 1
        val pRank = rank + pawnDir
        if (pRank in 0..7) {
            if (file > 0) {
                val p = position.getPiece(pRank * 8 + (file - 1))
                if (p != null && p.type == PieceType.PAWN && p.color == byColor) return true
            }
            if (file < 7) {
                val p = position.getPiece(pRank * 8 + (file + 1))
                if (p != null && p.type == PieceType.PAWN && p.color == byColor) return true
            }
        }

        // Knights
        for (offset in KNIGHT_OFFSETS) {
            val target = square + offset
            if (target in 0..63) {
                val tRank = target / 8
                val tFile = target % 8
                if (Math.abs(tRank - rank) in 1..2 && Math.abs(tFile - file) in 1..2) {
                    val p = position.getPiece(target)
                    if (p != null && p.type == PieceType.KNIGHT && p.color == byColor) return true
                }
            }
        }

        // Bishops / Queens
        for (d in BISHOP_DIRS) {
            var r = rank + d.first
            var f = file + d.second
            while (r in 0..7 && f in 0..7) {
                val p = position.getPiece(r * 8 + f)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += d.first
                f += d.second
            }
        }

        // Rooks / Queens
        for (d in ROOK_DIRS) {
            var r = rank + d.first
            var f = file + d.second
            while (r in 0..7 && f in 0..7) {
                val p = position.getPiece(r * 8 + f)
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += d.first
                f += d.second
            }
        }

        // King
        for (offset in KING_OFFSETS) {
            val target = square + offset
            if (target in 0..63) {
                val tRank = target / 8
                val tFile = target % 8
                if (Math.abs(tRank - rank) <= 1 && Math.abs(tFile - file) <= 1) {
                    val p = position.getPiece(target)
                    if (p != null && p.type == PieceType.KING && p.color == byColor) return true
                }
            }
        }

        return false
    }

    private fun generatePseudoLegalMoves(position: ChessPosition): List<Move> {
        val moves = ArrayList<Move>(60)
        val color = position.sideToMove

        for (sq in 0..63) {
            val piece = position.getPiece(sq) ?: continue
            if (piece.color != color) continue

            when (piece.type) {
                PieceType.PAWN -> generatePawnMoves(position, sq, piece, moves)
                PieceType.KNIGHT -> generateKnightMoves(position, sq, piece, moves)
                PieceType.BISHOP -> generateSliderMoves(position, sq, piece, BISHOP_DIRS, moves)
                PieceType.ROOK -> generateSliderMoves(position, sq, piece, ROOK_DIRS, moves)
                PieceType.QUEEN -> {
                    generateSliderMoves(position, sq, piece, BISHOP_DIRS, moves)
                    generateSliderMoves(position, sq, piece, ROOK_DIRS, moves)
                }
                PieceType.KING -> generateKingMoves(position, sq, piece, moves)
            }
        }
        return moves
    }

    private fun generatePawnMoves(position: ChessPosition, sq: Int, piece: Piece, moves: MutableList<Move>) {
        val rank = sq / 8
        val file = sq % 8
        val isWhite = piece.color == PieceColor.WHITE
        val forward = if (isWhite) 8 else -8
        val startRank = if (isWhite) 1 else 6
        val promoRank = if (isWhite) 7 else 0

        // Single push
        val oneStep = sq + forward
        if (oneStep in 0..63 && position.getPiece(oneStep) == null) {
            if (oneStep / 8 == promoRank) {
                for (promo in arrayOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                    moves.add(Move(sq, oneStep, promotion = promo))
                }
            } else {
                moves.add(Move(sq, oneStep))
                // Double push
                if (rank == startRank) {
                    val twoStep = sq + forward * 2
                    if (position.getPiece(twoStep) == null) {
                        moves.add(Move(sq, twoStep))
                    }
                }
            }
        }

        // Captures
        for (df in intArrayOf(-1, 1)) {
            val targetFile = file + df
            if (targetFile in 0..7) {
                val targetSq = sq + forward + df
                if (targetSq in 0..63) {
                    val targetPiece = position.getPiece(targetSq)
                    val isEnPassant = targetSq == position.enPassantSquare
                    if ((targetPiece != null && targetPiece.color != piece.color) || isEnPassant) {
                        if (targetSq / 8 == promoRank) {
                            for (promo in arrayOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)) {
                                moves.add(Move(sq, targetSq, promotion = promo, isCapture = true, isEnPassant = isEnPassant))
                            }
                        } else {
                            moves.add(Move(sq, targetSq, isCapture = true, isEnPassant = isEnPassant))
                        }
                    }
                }
            }
        }
    }

    private fun generateKnightMoves(position: ChessPosition, sq: Int, piece: Piece, moves: MutableList<Move>) {
        val rank = sq / 8
        val file = sq % 8
        for (offset in KNIGHT_OFFSETS) {
            val target = sq + offset
            if (target in 0..63) {
                val tRank = target / 8
                val tFile = target % 8
                if (Math.abs(tRank - rank) in 1..2 && Math.abs(tFile - file) in 1..2) {
                    val targetPiece = position.getPiece(target)
                    if (targetPiece == null || targetPiece.color != piece.color) {
                        moves.add(Move(sq, target, isCapture = targetPiece != null))
                    }
                }
            }
        }
    }

    private fun generateSliderMoves(
        position: ChessPosition,
        sq: Int,
        piece: Piece,
        directions: Array<Pair<Int, Int>>,
        moves: MutableList<Move>
    ) {
        val rank = sq / 8
        val file = sq % 8
        for (d in directions) {
            var r = rank + d.first
            var f = file + d.second
            while (r in 0..7 && f in 0..7) {
                val targetSq = r * 8 + f
                val targetPiece = position.getPiece(targetSq)
                if (targetPiece == null) {
                    moves.add(Move(sq, targetSq))
                } else {
                    if (targetPiece.color != piece.color) {
                        moves.add(Move(sq, targetSq, isCapture = true))
                    }
                    break
                }
                r += d.first
                f += d.second
            }
        }
    }

    private fun generateKingMoves(position: ChessPosition, sq: Int, piece: Piece, moves: MutableList<Move>) {
        val rank = sq / 8
        val file = sq % 8
        for (offset in KING_OFFSETS) {
            val target = sq + offset
            if (target in 0..63) {
                val tRank = target / 8
                val tFile = target % 8
                if (Math.abs(tRank - rank) <= 1 && Math.abs(tFile - file) <= 1) {
                    val targetPiece = position.getPiece(target)
                    if (targetPiece == null || targetPiece.color != piece.color) {
                        moves.add(Move(sq, target, isCapture = targetPiece != null))
                    }
                }
            }
        }

        // Castling
        val cr = position.castlingRights
        val opp = piece.color.opposite()
        if (piece.color == PieceColor.WHITE && sq == 4 && !isInCheck(position, PieceColor.WHITE)) {
            // White King-side (e1-g1)
            if (cr.whiteKingSide && position.getPiece(5) == null && position.getPiece(6) == null) {
                if (!isSquareAttacked(position, 5, opp) && !isSquareAttacked(position, 6, opp)) {
                    moves.add(Move(4, 6, isCastling = true))
                }
            }
            // White Queen-side (e1-c1)
            if (cr.whiteQueenSide && position.getPiece(1) == null && position.getPiece(2) == null && position.getPiece(3) == null) {
                if (!isSquareAttacked(position, 3, opp) && !isSquareAttacked(position, 2, opp)) {
                    moves.add(Move(4, 2, isCastling = true))
                }
            }
        } else if (piece.color == PieceColor.BLACK && sq == 60 && !isInCheck(position, PieceColor.BLACK)) {
            // Black King-side (e8-g8)
            if (cr.blackKingSide && position.getPiece(61) == null && position.getPiece(62) == null) {
                if (!isSquareAttacked(position, 61, opp) && !isSquareAttacked(position, 62, opp)) {
                    moves.add(Move(60, 62, isCastling = true))
                }
            }
            // Black Queen-side (e8-c8)
            if (cr.blackQueenSide && position.getPiece(57) == null && position.getPiece(58) == null && position.getPiece(59) == null) {
                if (!isSquareAttacked(position, 59, opp) && !isSquareAttacked(position, 58, opp)) {
                    moves.add(Move(60, 58, isCastling = true))
                }
            }
        }
    }

    fun moveToSan(position: ChessPosition, move: Move): String {
        val piece = position.getPiece(move.from) ?: return ""
        val targetSquareStr = Move.squareToAlgebraic(move.to)

        if (move.isCastling || (piece.type == PieceType.KING && Math.abs(move.to - move.from) == 2)) {
            val castlingStr = if (move.to > move.from) "O-O" else "O-O-O"
            val nextPos = makeMove(position, move)
            return when {
                isCheckmate(nextPos) -> "$castlingStr#"
                isInCheck(nextPos, nextPos.sideToMove) -> "$castlingStr+"
                else -> castlingStr
            }
        }

        val sb = StringBuilder()
        if (piece.type == PieceType.PAWN) {
            if (move.isCapture || (move.from % 8 != move.to % 8)) {
                sb.append(('a'.code + (move.from % 8)).toChar())
                sb.append('x')
            }
            sb.append(targetSquareStr)
            if (move.promotion != null) {
                sb.append('=').append(move.promotion.symbol.uppercaseChar())
            }
        } else {
            sb.append(piece.type.symbol.uppercaseChar())
            // Disambiguation
            val pseudoMoves = generatePseudoLegalMoves(position)
            val others = pseudoMoves.filter {
                it.from != move.from && it.to == move.to &&
                        position.getPiece(it.from)?.type == piece.type &&
                        !isInCheck(makeMove(position, it), position.sideToMove)
            }
            if (others.isNotEmpty()) {
                val sameFile = others.any { (it.from % 8) == (move.from % 8) }
                val sameRank = others.any { (it.from / 8) == (move.from / 8) }
                if (!sameFile) {
                    sb.append(('a'.code + (move.from % 8)).toChar())
                } else if (!sameRank) {
                    sb.append(('1'.code + (move.from / 8)).toChar())
                } else {
                    sb.append(('a'.code + (move.from % 8)).toChar())
                    sb.append(('1'.code + (move.from / 8)).toChar())
                }
            }
            if (move.isCapture || position.getPiece(move.to) != null) {
                sb.append('x')
            }
            sb.append(targetSquareStr)
        }

        val nextPos = makeMove(position, move)
        if (isCheckmate(nextPos)) {
            sb.append('#')
        } else if (isInCheck(nextPos, nextPos.sideToMove)) {
            sb.append('+')
        }
        return sb.toString()
    }

    fun parseUci(position: ChessPosition, uci: String): Move? {
        if (uci.length < 4) return null
        val from = Move.algebraicToSquare(uci.substring(0, 2))
        val to = Move.algebraicToSquare(uci.substring(2, 4))
        if (from == -1 || to == -1) return null

        val promoChar = if (uci.length >= 5) uci[4] else null
        val promo = promoChar?.let { PieceType.fromChar(it) }

        val legal = generateLegalMoves(position)
        return legal.firstOrNull { it.from == from && it.to == to && (promo == null || it.promotion == promo) }
    }

    fun parseSan(position: ChessPosition, rawSan: String): Move? {
        val clean = rawSan.replace("+", "").replace("#", "").replace("!", "").replace("?", "").trim()
        val legal = generateLegalMoves(position)

        // Try exact match first
        val exact = legal.firstOrNull {
            it.san.replace("+", "").replace("#", "") == clean
        }
        if (exact != null) return exact

        // Castling
        if (clean == "O-O" || clean == "0-0") {
            return legal.firstOrNull { it.isCastling && it.to > it.from }
        }
        if (clean == "O-O-O" || clean == "0-0-0") {
            return legal.firstOrNull { it.isCastling && it.to < it.from }
        }

        // Pawn moves & piece moves
        for (m in legal) {
            val p = position.getPiece(m.from) ?: continue
            val targetStr = Move.squareToAlgebraic(m.to)
            if (!clean.endsWith(targetStr) && !clean.contains(targetStr)) continue

            if (p.type == PieceType.PAWN) {
                if (clean.length == 2 && clean == targetStr) return m
                if (clean.contains("x") && clean.startsWith(('a'.code + (m.from % 8)).toChar().toString())) {
                    if (clean.contains("=")) {
                        val pr = PieceType.fromChar(clean.substringAfter("=").firstOrNull() ?: 'q')
                        if (m.promotion == pr) return m
                    } else if (m.promotion == null) {
                        return m
                    }
                }
            } else if (clean.startsWith(p.type.symbol.uppercaseChar().toString())) {
                val disambig = clean.substring(1).substringBefore("x").substringBefore(targetStr)
                if (disambig.isEmpty()) return m
                if (disambig.length == 1) {
                    val c = disambig[0]
                    if (c in 'a'..'h' && (m.from % 8) == (c - 'a')) return m
                    if (c in '1'..'8' && (m.from / 8) == (c - '1')) return m
                } else if (disambig.length == 2) {
                    val sq = Move.algebraicToSquare(disambig)
                    if (m.from == sq) return m
                }
            }
        }

        return null
    }
}
