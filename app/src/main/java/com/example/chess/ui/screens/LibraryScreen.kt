package com.example.chess.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.data.GameEntity
import com.example.chess.data.SampleGames
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LibraryScreen(
    savedGames: List<GameEntity>,
    isUkrainian: Boolean,
    onLoadGame: (String, String) -> Unit,
    onDeleteGame: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        // Section: Classic Grandmaster Games
        item {
            Text(
                text = if (isUkrainian) "Історичні шедеври" else "Classic Masterpieces",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = EmeraldPrimary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            SampleGameCard(
                title = "Byrne vs Fischer (1956)",
                subtitle = "The Game of the Century (17... Be6!!)",
                result = "0-1",
                moves = "41 moves",
                onClick = { onLoadGame(SampleGames.FISCHER_GAME_OF_THE_CENTURY, "Byrne vs Fischer") }
            )
            Spacer(modifier = Modifier.height(6.dp))
            SampleGameCard(
                title = "Kasparov vs Topalov (1999)",
                subtitle = "Kasparov's Immortal (24. Rxd4!!)",
                result = "1-0",
                moves = "44 moves",
                onClick = { onLoadGame(SampleGames.KASPAROV_IMMORTAL, "Kasparov vs Topalov") }
            )
            Spacer(modifier = Modifier.height(6.dp))
            SampleGameCard(
                title = "Morphy vs Allies (1858)",
                subtitle = "The Opera Game (Tactical King Hunt)",
                result = "1-0",
                moves = "17 moves",
                onClick = { onLoadGame(SampleGames.OPERA_GAME, "Opera Game") }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Section: Saved Database Games
        item {
            Text(
                text = if (isUkrainian) "Збережені партії (${savedGames.size})" else "Saved Games (${savedGames.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (savedGames.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isUkrainian) "Ще немає збережених партій" else "No saved games yet",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(savedGames, key = { it.id }) { game ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onLoadGame(game.pgnText, game.title) }
                        .testTag("game_item_${game.id}"),
                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${game.whitePlayer} vs ${game.blackPlayer}",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${game.date} • ${game.result} • ${game.moveCount} moves",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )
                                if (game.whiteAccuracy != null && game.blackAccuracy != null) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${"%.0f".format(game.whiteAccuracy)}% / ${"%.0f".format(game.blackAccuracy)}%",
                                        color = EmeraldPrimary,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onDeleteGame(game.id) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Game",
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = { onLoadGame(game.pgnText, game.title) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Open Game",
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleGameCard(
    title: String,
    subtitle: String,
    result: String,
    moves: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x2200E676)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    text = "$subtitle • $result • $moves",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
