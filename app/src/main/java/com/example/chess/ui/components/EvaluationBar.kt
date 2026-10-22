package com.example.chess.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.engine.EngineEvaluation
import kotlin.math.abs

@Composable
fun EvaluationBar(
    evaluation: EngineEvaluation?,
    isFlipped: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Calculate White proportion between 0.0 (total Black advantage) and 1.0 (total White advantage)
    val scoreCp = evaluation?.scoreCp ?: 0
    val scoreMate = evaluation?.scoreMate

    val whiteWinRatio = when {
        scoreMate != null -> if (scoreMate > 0) 1.0f else 0.0f
        else -> {
            val clamped = scoreCp.coerceIn(-1000, 1000)
            // Logistic conversion centered at 0.5
            (0.5f + (clamped / 2000f)).coerceIn(0.05f, 0.95f)
        }
    }

    // If flipped (playing as Black), invert bar
    val topProportion = if (!isFlipped) 1.0f - whiteWinRatio else whiteWinRatio
    val animatedTopWeight by animateFloatAsState(
        targetValue = topProportion,
        animationSpec = tween(durationMillis = 350),
        label = "evalBarAnim"
    )

    val labelText = when {
        scoreMate != null -> if (scoreMate > 0) "M$scoreMate" else "-M${abs(scoreMate)}"
        else -> {
            val pawns = scoreCp / 100.0
            if (pawns > 0) "+%.1f".format(pawns) else "%.1f".format(pawns)
        }
    }

    Box(
        modifier = modifier
            .width(26.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF212529))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Black portion (top)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(animatedTopWeight.coerceIn(0.01f, 0.99f))
                    .background(Color(0xFF1E2022))
            )
            // White portion (bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight((1f - animatedTopWeight).coerceIn(0.01f, 0.99f))
                    .background(Color(0xFFF8F9FA))
            )
        }

        // Numeric eval text pill
        val isWhiteAdvantage = scoreCp >= 0
        Box(
            modifier = Modifier
                .align(if (isWhiteAdvantage) Alignment.BottomCenter else Alignment.TopCenter)
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = labelText,
                color = if (isWhiteAdvantage) Color(0xFF1E2022) else Color(0xFFF8F9FA),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
