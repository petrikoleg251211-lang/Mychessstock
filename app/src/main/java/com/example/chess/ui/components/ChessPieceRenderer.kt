package com.example.chess.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.example.chess.model.Piece
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType

object ChessPieceRenderer {

    // Unicode symbols for chess pieces
    fun getSymbol(piece: Piece): String {
        return when (piece.color) {
            PieceColor.WHITE -> when (piece.type) {
                PieceType.KING -> "♔"
                PieceType.QUEEN -> "♕"
                PieceType.ROOK -> "♖"
                PieceType.BISHOP -> "♗"
                PieceType.KNIGHT -> "♘"
                PieceType.PAWN -> "♙"
            }
            PieceColor.BLACK -> when (piece.type) {
                PieceType.KING -> "♚"
                PieceType.QUEEN -> "♛"
                PieceType.ROOK -> "♜"
                PieceType.BISHOP -> "♝"
                PieceType.KNIGHT -> "♞"
                PieceType.PAWN -> "♟"
            }
        }
    }

    fun drawPiece(
        drawScope: DrawScope,
        piece: Piece,
        topLeft: Offset,
        squareSize: Float
    ) {
        val symbol = getSymbol(piece)
        val isWhite = piece.color == PieceColor.WHITE

        drawScope.drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            val textSize = squareSize * 0.78f

            val paint = android.graphics.Paint().apply {
                this.textSize = textSize
                this.textAlign = android.graphics.Paint.Align.CENTER
                this.isAntiAlias = true
                this.typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val centerX = topLeft.x + squareSize / 2f
            val centerY = topLeft.y + squareSize / 2f - ((paint.descent() + paint.ascent()) / 2f)

            // Draw shadow/stroke
            paint.style = android.graphics.Paint.Style.STROKE
            paint.strokeWidth = squareSize * 0.05f
            paint.color = if (isWhite) android.graphics.Color.parseColor("#1A1A1A") else android.graphics.Color.parseColor("#E0E0E0")
            nativeCanvas.drawText(symbol, centerX, centerY, paint)

            // Draw fill
            paint.style = android.graphics.Paint.Style.FILL
            paint.color = if (isWhite) android.graphics.Color.WHITE else android.graphics.Color.parseColor("#151515")
            nativeCanvas.drawText(symbol, centerX, centerY, paint)
        }
    }
}
