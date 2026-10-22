package com.example.chess.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.rules.ChessPosition
import com.example.chess.rules.ChessRulesEngine
import com.example.ui.theme.BoardArrow
import com.example.ui.theme.BoardCheck
import com.example.ui.theme.BoardDarkSquare
import com.example.ui.theme.BoardHighlight
import com.example.ui.theme.BoardLightSquare
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ChessBoardView(
    position: ChessPosition,
    selectedSquare: Int?,
    legalMovesForSelected: List<Move>,
    lastMove: Move?,
    bestMove: Move?,
    isFlipped: Boolean = false,
    modifier: Modifier = Modifier,
    onSquareClicked: (Int) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isFlipped) {
                    detectTapGestures { offset ->
                        val squareSize = size.width / 8f
                        val file = (offset.x / squareSize).toInt().coerceIn(0, 7)
                        val rankFromTop = (offset.y / squareSize).toInt().coerceIn(0, 7)

                        val rank = if (!isFlipped) 7 - rankFromTop else rankFromTop
                        val actualFile = if (!isFlipped) file else 7 - file
                        val square = rank * 8 + actualFile
                        onSquareClicked(square)
                    }
                }
        ) {
            val squareSize = size.width / 8f
            val isCheck = ChessRulesEngine.isInCheck(position, position.sideToMove)
            val kingSquare = if (isCheck) ChessRulesEngine.findKing(position, position.sideToMove) else -1

            // 1. Draw 64 squares
            for (rankIdx in 0..7) {
                for (fileIdx in 0..7) {
                    val actualRank = if (!isFlipped) 7 - rankIdx else rankIdx
                    val actualFile = if (!isFlipped) fileIdx else 7 - fileIdx
                    val square = actualRank * 8 + actualFile

                    val isLight = (actualRank + actualFile) % 2 != 0
                    val baseColor = if (isLight) BoardLightSquare else BoardDarkSquare
                    val topLeft = Offset(fileIdx * squareSize, rankIdx * squareSize)

                    drawRect(
                        color = baseColor,
                        topLeft = topLeft,
                        size = Size(squareSize, squareSize)
                    )

                    // Draw Last Move highlight
                    if (lastMove != null && (square == lastMove.from || square == lastMove.to)) {
                        drawRect(
                            color = BoardHighlight,
                            topLeft = topLeft,
                            size = Size(squareSize, squareSize)
                        )
                    }

                    // Draw Selected Square highlight
                    if (selectedSquare == square) {
                        drawRect(
                            color = Color(0x6600E676),
                            topLeft = topLeft,
                            size = Size(squareSize, squareSize)
                        )
                    }

                    // Draw King in check highlight
                    if (square == kingSquare) {
                        drawRect(
                            color = BoardCheck,
                            topLeft = topLeft,
                            size = Size(squareSize, squareSize)
                        )
                    }
                }
            }

            // 2. Draw Coordinates on borders
            drawCoordinates(squareSize, isFlipped)

            // 3. Draw Pieces
            for (sq in 0..63) {
                val piece = position.getPiece(sq) ?: continue
                val rank = sq / 8
                val file = sq % 8
                val drawRank = if (!isFlipped) 7 - rank else rank
                val drawFile = if (!isFlipped) file else 7 - file
                val topLeft = Offset(drawFile * squareSize, drawRank * squareSize)

                ChessPieceRenderer.drawPiece(this, piece, topLeft, squareSize)
            }

            // 4. Draw Legal Move indicators
            for (move in legalMovesForSelected) {
                val rank = move.to / 8
                val file = move.to % 8
                val drawRank = if (!isFlipped) 7 - rank else rank
                val drawFile = if (!isFlipped) file else 7 - file
                val center = Offset(drawFile * squareSize + squareSize / 2f, drawRank * squareSize + squareSize / 2f)

                val targetPiece = position.getPiece(move.to)
                if (targetPiece != null || move.isEnPassant) {
                    // Capture target: hollow ring
                    drawCircle(
                        color = Color(0xAA00E676),
                        radius = squareSize * 0.42f,
                        center = center,
                        style = Stroke(width = squareSize * 0.09f)
                    )
                } else {
                    // Quiet move target: small filled dot
                    drawCircle(
                        color = Color(0x8800E676),
                        radius = squareSize * 0.16f,
                        center = center
                    )
                }
            }

            // 5. Draw Engine Best Move Arrow
            if (bestMove != null) {
                drawMoveArrow(bestMove, squareSize, isFlipped, BoardArrow)
            }
        }
    }
}

private fun DrawScope.drawCoordinates(squareSize: Float, isFlipped: Boolean) {
    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            textSize = squareSize * 0.22f
            isAntiAlias = true
            color = android.graphics.Color.parseColor("#80FFFFFF")
            typeface = android.graphics.Typeface.MONOSPACE
        }

        // Files: a to h (bottom edge)
        for (f in 0..7) {
            val fileChar = if (!isFlipped) ('a'.code + f).toChar() else ('h'.code - f).toChar()
            canvas.nativeCanvas.drawText(
                fileChar.toString(),
                f * squareSize + squareSize * 0.08f,
                size.height - squareSize * 0.08f,
                paint
            )
        }

        // Ranks: 1 to 8 (left edge)
        for (r in 0..7) {
            val rankChar = if (!isFlipped) ('8'.code - r).toChar() else ('1'.code + r).toChar()
            canvas.nativeCanvas.drawText(
                rankChar.toString(),
                squareSize * 0.08f,
                r * squareSize + squareSize * 0.26f,
                paint
            )
        }
    }
}

private fun DrawScope.drawMoveArrow(
    move: Move,
    squareSize: Float,
    isFlipped: Boolean,
    color: Color
) {
    val fromRank = move.from / 8
    val fromFile = move.from % 8
    val toRank = move.to / 8
    val toFile = move.to % 8

    val drawFromRank = if (!isFlipped) 7 - fromRank else fromRank
    val drawFromFile = if (!isFlipped) fromFile else 7 - fromFile
    val drawToRank = if (!isFlipped) 7 - toRank else toRank
    val drawToFile = if (!isFlipped) toFile else 7 - toFile

    val start = Offset(drawFromFile * squareSize + squareSize / 2f, drawFromRank * squareSize + squareSize / 2f)
    val end = Offset(drawToFile * squareSize + squareSize / 2f, drawToRank * squareSize + squareSize / 2f)

    val angle = atan2(end.y - start.y, end.x - start.x)
    val headLength = squareSize * 0.35f
    val headAngle = 0.5f

    // Shorten line slightly so arrow sits cleanly
    val shortenedEnd = Offset(
        end.x - cos(angle) * (squareSize * 0.15f),
        end.y - sin(angle) * (squareSize * 0.15f)
    )

    // Draw main arrow shaft
    drawLine(
        color = color.copy(alpha = 0.85f),
        start = start,
        end = shortenedEnd,
        strokeWidth = squareSize * 0.16f
    )

    // Draw arrow head triangle
    val path = Path().apply {
        moveTo(end.x, end.y)
        lineTo(
            end.x - headLength * cos(angle - headAngle),
            end.y - headLength * sin(angle - headAngle)
        )
        lineTo(
            end.x - headLength * cos(angle + headAngle),
            end.y - headLength * sin(angle + headAngle)
        )
        close()
    }
    drawPath(path, color = color)
}
