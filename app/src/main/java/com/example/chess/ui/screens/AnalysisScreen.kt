package com.example.chess.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.MoveClassification
import com.example.chess.model.PieceColor
import com.example.chess.ui.ChessUiState
import com.example.chess.ui.components.ChessBoardView
import com.example.chess.ui.components.EvalGraph
import com.example.chess.ui.components.EvaluationBar
import com.example.chess.ui.components.PromotionDialog
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AnalysisScreen(
    state: ChessUiState,
    onSquareClicked: (Int) -> Unit,
    onPromotionSelected: (com.example.chess.model.PieceType) -> Unit,
    onDismissPromotion: () -> Unit,
    onStartReview: () -> Unit,
    onStopReview: () -> Unit,
    onFirstMove: () -> Unit,
    onPrevMove: () -> Unit,
    onNextMove: () -> Unit,
    onLastMove: () -> Unit,
    onGoToMove: (Int) -> Unit,
    onFlipBoard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll move list as current move changes
    LaunchedEffect(state.currentMoveIndex) {
        val targetRow = (state.currentMoveIndex / 2).coerceAtLeast(0)
        listState.animateScrollToItem(targetRow)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        // Top Player Bar (Black if normal, White if flipped)
        val topPlayer = if (!state.isFlipped) state.game.black else state.game.white
        val topElo = if (!state.isFlipped) state.game.blackElo else state.game.whiteElo
        PlayerHeader(
            name = topPlayer,
            elo = topElo,
            color = if (!state.isFlipped) PieceColor.BLACK else PieceColor.WHITE,
            isCurrentTurn = state.currentPosition.sideToMove == (if (!state.isFlipped) PieceColor.BLACK else PieceColor.WHITE)
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Center Chessboard + Evaluation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EvaluationBar(
                evaluation = state.liveEvaluation,
                isFlipped = state.isFlipped,
                modifier = Modifier.padding(end = 6.dp)
            )

            ChessBoardView(
                position = state.currentPosition,
                selectedSquare = state.selectedSquare,
                legalMovesForSelected = state.legalMovesForSelected,
                lastMove = state.lastMove,
                bestMove = state.bestMoveForCurrentPos,
                isFlipped = state.isFlipped,
                modifier = Modifier.weight(1f),
                onSquareClicked = onSquareClicked
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom Player Bar
        val bottomPlayer = if (!state.isFlipped) state.game.white else state.game.black
        val bottomElo = if (!state.isFlipped) state.game.whiteElo else state.game.blackElo
        PlayerHeader(
            name = bottomPlayer,
            elo = bottomElo,
            color = if (!state.isFlipped) PieceColor.WHITE else PieceColor.BLACK,
            isCurrentTurn = state.currentPosition.sideToMove == (if (!state.isFlipped) PieceColor.WHITE else PieceColor.BLACK)
        )

        // Review Progress Bar if running
        AnimatedVisibility(visible = state.isReviewing && state.reviewProgress != null) {
            val progress = state.reviewProgress
            if (progress != null) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (state.isUkrainian)
                                "Аналіз: хід ${progress.currentMoveIndex}/${progress.totalMoves} (${progress.currentSan})"
                            else
                                "Analyzing: move ${progress.currentMoveIndex}/${progress.totalMoves} (${progress.currentSan})",
                            color = EmeraldPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "d=${progress.currentDepth} | ${progress.nps / 1000}k nps",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    val fraction = if (progress.totalMoves > 0) progress.currentMoveIndex.toFloat() / progress.totalMoves else 0f
                    LinearProgressIndicator(
                        progress = { fraction },
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        color = EmeraldPrimary,
                        trackColor = SlateBorder
                    )
                }
            }
        }

        // Current Move Explanation Card
        val analyzedMove = state.currentAnalyzedMove
        if (analyzedMove != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(analyzedMove.classification.colorHex))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${analyzedMove.classification.badge} ${analyzedMove.classification.getDisplayName(state.isUkrainian)}",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = analyzedMove.explanation,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }

        // Advantage Trend Graph
        val evalHistory = state.reviewReport?.evalHistory
        if (evalHistory != null && evalHistory.size > 1) {
            EvalGraph(
                evalHistory = evalHistory,
                currentMoveIndex = state.currentMoveIndex,
                onMoveSelected = onGoToMove,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        // Replay Navigation Controller
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                IconButton(onClick = onFirstMove, modifier = Modifier.testTag("btn_first")) {
                    Icon(Icons.Default.FirstPage, contentDescription = "First Move", tint = TextPrimary)
                }
                IconButton(onClick = onPrevMove, modifier = Modifier.testTag("btn_prev")) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Move", tint = TextPrimary)
                }
                IconButton(onClick = onNextMove, modifier = Modifier.testTag("btn_next")) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Move", tint = TextPrimary)
                }
                IconButton(onClick = onLastMove, modifier = Modifier.testTag("btn_last")) {
                    Icon(Icons.Default.LastPage, contentDescription = "Last Move", tint = TextPrimary)
                }
                IconButton(onClick = onFlipBoard, modifier = Modifier.testTag("btn_flip")) {
                    Icon(Icons.Default.SwapVert, contentDescription = "Flip Board", tint = TextSecondary)
                }
            }

            // Review action button
            if (state.isReviewing) {
                OutlinedButton(
                    onClick = onStopReview,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252)),
                    modifier = Modifier.testTag("btn_stop_review")
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (state.isUkrainian) "Зупинити" else "Stop", fontSize = 12.sp)
                }
            } else {
                Button(
                    onClick = onStartReview,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
                    modifier = Modifier.testTag("btn_start_review")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (state.reviewReport != null)
                            (if (state.isUkrainian) "Повторити" else "Re-Review")
                        else
                            (if (state.isUkrainian) "Аналіз" else "Review"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Two-column Move List
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 2.dp, bottom = 4.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            val moves = state.game.moves
            val rowCount = (moves.size + 1) / 2

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
            ) {
                items(rowCount) { rowIndex ->
                    val moveNum = rowIndex + 1
                    val whiteIdx = rowIndex * 2
                    val blackIdx = whiteIdx + 1

                    val whiteMove = moves.getOrNull(whiteIdx)
                    val blackMove = moves.getOrNull(blackIdx)

                    val whiteAnalyzed = state.reviewReport?.analyzedMoves?.getOrNull(whiteIdx)
                    val blackAnalyzed = state.reviewReport?.analyzedMoves?.getOrNull(blackIdx)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Move number
                        Text(
                            text = "$moveNum.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(28.dp)
                        )

                        // White Move
                        if (whiteMove != null) {
                            MovePill(
                                san = whiteMove.san,
                                classification = whiteAnalyzed?.classification,
                                isSelected = state.currentMoveIndex == whiteIdx + 1,
                                onClick = { onGoToMove(whiteIdx + 1) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Black Move
                        if (blackMove != null) {
                            MovePill(
                                san = blackMove.san,
                                classification = blackAnalyzed?.classification,
                                isSelected = state.currentMoveIndex == blackIdx + 1,
                                onClick = { onGoToMove(blackIdx + 1) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Pawn Promotion Dialog
    if (state.pendingPromotion != null) {
        PromotionDialog(
            color = state.currentPosition.sideToMove,
            isUkrainian = state.isUkrainian,
            onPieceSelected = onPromotionSelected,
            onDismiss = onDismissPromotion
        )
    }
}

@Composable
private fun PlayerHeader(
    name: String,
    elo: String?,
    color: PieceColor,
    isCurrentTurn: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (color == PieceColor.WHITE) Color.White else Color.Black)
                    .border(1.dp, SlateBorder, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isCurrentTurn) FontWeight.Bold else FontWeight.Normal
            )
            if (!elo.isNullOrEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "($elo)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        if (isCurrentTurn) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x3300E676))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Turn",
                    color = EmeraldPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MovePill(
    san: String,
    classification: MoveClassification?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFF233B2B) else SlateSurfaceVariant)
            .border(
                1.dp,
                if (isSelected) EmeraldPrimary else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = san,
            color = if (isSelected) EmeraldPrimary else TextPrimary,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )

        if (classification != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(classification.colorHex))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = classification.badge,
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
