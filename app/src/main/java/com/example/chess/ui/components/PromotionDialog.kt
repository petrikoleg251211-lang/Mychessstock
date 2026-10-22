package com.example.chess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionDialog(
    color: PieceColor,
    isUkrainian: Boolean,
    onPieceSelected: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SlateSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isUkrainian) "Оберіть фігуру перетворення" else "Choose Promotion Piece",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val pieces = listOf(
                        PieceType.QUEEN to if (color == PieceColor.WHITE) "♕" else "♛",
                        PieceType.ROOK to if (color == PieceColor.WHITE) "♖" else "♜",
                        PieceType.BISHOP to if (color == PieceColor.WHITE) "♗" else "♝",
                        PieceType.KNIGHT to if (color == PieceColor.WHITE) "♘" else "♞"
                    )

                    for ((type, glyph) in pieces) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF26332A))
                                .clickable { onPieceSelected(type) }
                                .testTag("promotion_${type.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = glyph,
                                fontSize = 32.sp,
                                color = if (color == PieceColor.WHITE) Color.White else Color(0xFFE0E0E0)
                            )
                        }
                    }
                }
            }
        }
    }
}
