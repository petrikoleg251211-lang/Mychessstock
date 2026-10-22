package com.example.chess.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BoardArrow
import com.example.ui.theme.SlateBorder

@Composable
fun EvalGraph(
    evalHistory: List<Float>, // in pawns, e.g. +1.5, -2.0, clamped between -10 and +10
    currentMoveIndex: Int,
    onMoveSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .pointerInput(evalHistory.size) {
                detectTapGestures { offset ->
                    if (evalHistory.isNotEmpty()) {
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val targetIndex = (fraction * (evalHistory.size - 1)).toInt()
                        onMoveSelected(targetIndex)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val midY = height / 2f

        // Draw baseline (0.0 evaluation)
        drawLine(
            color = SlateBorder,
            start = Offset(0f, midY),
            end = Offset(width, midY),
            strokeWidth = 1.5f
        )

        if (evalHistory.size < 2) return@Canvas

        val stepX = width / (evalHistory.size - 1)
        val maxScale = 8.0f // +/- 8 pawns fills the graph

        val path = Path()
        val fillPath = Path()
        fillPath.moveTo(0f, midY)

        for (i in evalHistory.indices) {
            val eval = evalHistory[i].coerceIn(-maxScale, maxScale)
            val x = i * stepX
            // +eval goes UP (y decreases)
            val y = midY - (eval / maxScale) * (height / 2f)

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        fillPath.lineTo(width, midY)
        fillPath.close()

        // Soft gradient fill
        drawPath(
            path = fillPath,
            color = Color(0x3300E676)
        )

        // Curve line
        drawPath(
            path = path,
            color = BoardArrow,
            style = Stroke(width = 2.5f)
        )

        // Current move scrubber cursor
        if (currentMoveIndex in evalHistory.indices) {
            val currentX = currentMoveIndex * stepX
            val currentEval = evalHistory[currentMoveIndex].coerceIn(-maxScale, maxScale)
            val currentY = midY - (currentEval / maxScale) * (height / 2f)

            drawLine(
                color = Color.White,
                start = Offset(currentX, 0f),
                end = Offset(currentX, height),
                strokeWidth = 2f
            )

            drawCircle(
                color = BoardArrow,
                radius = 5f,
                center = Offset(currentX, currentY)
            )
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = Offset(currentX, currentY)
            )
        }
    }
}
