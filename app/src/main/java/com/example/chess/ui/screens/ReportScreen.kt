package com.example.chess.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.AnalyzedMove
import com.example.chess.model.MoveClassification
import com.example.chess.model.PieceColor
import com.example.chess.pgn.PgnParser
import com.example.chess.ui.ChessUiState
import com.example.chess.ui.components.EvalGraph
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ReportScreen(
    state: ChessUiState,
    onStartReview: () -> Unit,
    onSaveGame: () -> Unit,
    onGoToMoveAndSwitchTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val report = state.reviewReport

    if (report == null) {
        // Empty state before review has run
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color(0x2200E676)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (state.isUkrainian) "Аналіз партії ще не проведено" else "Game review not conducted yet",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (state.isUkrainian)
                        "Запустіть Stockfish для розрахунку точності, класифікації ходів і виявлення помилок"
                    else
                        "Run Stockfish to calculate accuracy, classify moves, and detect tactical blunders",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onStartReview,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                    modifier = Modifier.testTag("btn_run_review_from_report")
                ) {
                    Text(
                        if (state.isUkrainian) "Запустити аналіз" else "Start Game Review",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // 1. Players & Accuracy Score Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (state.isUkrainian) "Підсумок точності" else "Accuracy Summary",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // White Player Accuracy
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = state.game.white,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${"%.1f".format(report.whiteAccuracy)}%",
                                color = EmeraldPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // VS Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("VS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Black Player Accuracy
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = state.game.black,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${"%.1f".format(report.blackAccuracy)}%",
                                color = EmeraldPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // 2. Evaluation Graph
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (state.isUkrainian) "Графік переваги" else "Evaluation Trend",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    EvalGraph(
                        evalHistory = report.evalHistory,
                        currentMoveIndex = state.currentMoveIndex,
                        onMoveSelected = onGoToMoveAndSwitchTab
                    )
                }
            }
        }

        // 3. Move Classification Grid
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (state.isUkrainian) "Класифікація ходів" else "Move Breakdown",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    for (classification in MoveClassification.values()) {
                        val whiteCount = report.whiteMoveStats[classification] ?: 0
                        val blackCount = report.blackMoveStats[classification] ?: 0

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // White count
                            Text(
                                text = whiteCount.toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.width(32.dp)
                            )

                            // Badge label center
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(classification.colorHex).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = classification.badge,
                                    color = Color(classification.colorHex),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = classification.getDisplayName(state.isUkrainian),
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Black count
                            Text(
                                text = blackCount.toString(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                modifier = Modifier.width(32.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Critical Mistakes & Blunders Section
        val criticalMoves = report.analyzedMoves.filter {
            it.classification == MoveClassification.BLUNDER || it.classification == MoveClassification.MISTAKE
        }

        if (criticalMoves.isNotEmpty()) {
            item {
                Text(
                    text = if (state.isUkrainian) "Критичні помилки та зевки (${criticalMoves.size})" else "Mistakes & Blunders (${criticalMoves.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5252),
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            items(criticalMoves) { analyzed ->
                val moveIndexInGame = (analyzed.moveNumber - 1) * 2 + (if (analyzed.color == PieceColor.WHITE) 0 else 1)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { onGoToMoveAndSwitchTab(moveIndexInGame + 1) },
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(analyzed.classification.colorHex))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${analyzed.classification.badge} ${analyzed.classification.getDisplayName(state.isUkrainian)}",
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${analyzed.moveNumber}. ${analyzed.playedMove.san}",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                if (analyzed.bestMove != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "(${if (state.isUkrainian) "Краще" else "Best"}: ${analyzed.bestMove.san})",
                                        color = EmeraldPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = analyzed.explanation,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Jump to Move",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // 5. Actions Footer (Save to DB, Copy PGN)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveGame,
                    modifier = Modifier.weight(1f).testTag("btn_save_game"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (state.isUkrainian) "Зберегти" else "Save Game", fontSize = 12.sp)
                }

                Button(
                    onClick = {
                        val exported = PgnParser.exportToPgn(state.game)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Chess PGN", exported))
                        Toast.makeText(
                            context,
                            if (state.isUkrainian) "PGN скопійовано в буфер" else "PGN copied to clipboard",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.weight(1f).testTag("btn_copy_pgn"),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (state.isUkrainian) "Експорт PGN" else "Export PGN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
