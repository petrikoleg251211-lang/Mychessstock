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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.example.chess.engine.EngineSettings
import com.example.chess.ui.ChessUiState
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    state: ChessUiState,
    onSettingsChanged: (EngineSettings) -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val settings = state.engineSettings
    val isUkrainian = state.isUkrainian

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Text(
            text = if (isUkrainian) "Налаштування Stockfish" else "Stockfish Settings",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Active Engine Status Card (MANDATORY REQUIREMENT)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (state.isRealStockfishActive) Color(0xFF14241B) else Color(0xFF2E2218)
            ),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (state.isRealStockfishActive) EmeraldPrimary else Color(0xFFFF9100)
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (state.isRealStockfishActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (state.isRealStockfishActive) EmeraldPrimary else Color(0xFFFF9100),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (state.isRealStockfishActive)
                                "Stockfish 19 (Official NNUE Engine)"
                            else
                                "Stockfish Lite (Fallback Mode)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (state.isRealStockfishActive)
                                "Native ARM64-v8a • NNUE Evaluator Active"
                            else
                                "Built-in Kotlin Minimax Evaluator",
                            color = if (state.isRealStockfishActive) EmeraldPrimary else Color(0xFFFF9100),
                            fontSize = 12.sp
                        )
                    }
                }

                if (state.engineWarning != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.engineWarning,
                        color = Color(0xFFFFCC80),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Profile Presets Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isUkrainian) "Профіль аналізу" else "Analysis Profile",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))

                val profiles = listOf(
                    Triple(
                        if (isUkrainian) "Швидкий (Глибина 14 ply, 600 мс)" else "Fast (Depth 14 ply, 600ms)",
                        14,
                        600L
                    ),
                    Triple(
                        if (isUkrainian) "Збалансований (Глибина 18 ply, 1.2 с)" else "Balanced (Depth 18 ply, 1.2s)",
                        18,
                        1200L
                    ),
                    Triple(
                        if (isUkrainian) "Гросмейстер S25 Ultra (Глибина 24+ ply, 2 с)" else "Grandmaster S25 Ultra (Depth 24+ ply, 2s)",
                        24,
                        2000L
                    )
                )

                for ((label, depth, timeMs) in profiles) {
                    val isSelected = settings.depth == depth
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF233B2B) else SlateSurfaceVariant)
                            .border(
                                1.dp,
                                if (isSelected) EmeraldPrimary else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                onSettingsChanged(
                                    settings.copy(
                                        depth = depth,
                                        timeLimitPerMoveMs = timeMs,
                                        profileName = label
                                    )
                                )
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) EmeraldPrimary else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                        if (isSelected) {
                            Text(
                                text = "Active",
                                color = EmeraldPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Samsung Galaxy S25 Ultra Optimization & Hardware Options
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isUkrainian) "Параметри процесора та пам'яті" else "CPU & Memory Configuration",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Target Depth Slider (up to 30 ply)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isUkrainian) "Цільова глибина (ply)" else "Target Depth (ply)",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${settings.depth} ply",
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
                Slider(
                    value = settings.depth.toFloat(),
                    onValueChange = { onSettingsChanged(settings.copy(depth = it.toInt())) },
                    valueRange = 10f..30f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary,
                        inactiveTrackColor = SlateBorder
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Threads Selector (1, 2, 4, 8)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isUkrainian) "Потоки CPU (Threads)" else "CPU Threads",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (isUkrainian) "Для S25 Ultra рекомендовано 4–6" else "4–6 recommended for S25 Ultra",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (th in listOf(1, 2, 4, 8)) {
                            val active = settings.threads == th
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (active) EmeraldPrimary else SlateSurfaceVariant)
                                    .clickable { onSettingsChanged(settings.copy(threads = th)) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$th",
                                    color = if (active) Color.Black else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hash Size Selector (32, 64, 128, 256 MB)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isUkrainian) "Розмір хешу (Hash)" else "Transposition Hash",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = if (isUkrainian) "128–256 MB для 12-16 GB RAM" else "128–256 MB for 12-16 GB RAM",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (mb in listOf(32, 64, 128, 256)) {
                            val active = settings.hashSizeMb == mb
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (active) EmeraldPrimary else SlateSurfaceVariant)
                                    .clickable { onSettingsChanged(settings.copy(hashSizeMb = mb)) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${mb}M",
                                    color = if (active) Color.Black else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // MultiPV Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUkrainian) "Альтернативні лінії (MultiPV)" else "MultiPV Variations",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (pv in listOf(1, 2, 3)) {
                            val active = settings.multiPv == pv
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (active) EmeraldPrimary else SlateSurfaceVariant)
                                    .clickable { onSettingsChanged(settings.copy(multiPv = pv)) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$pv",
                                    color = if (active) Color.Black else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Language & Localization Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isUkrainian) "Мова інтерфейсу" else "UI Language",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isUkrainian) "Українська (UK)" else "English (EN)",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = isUkrainian,
                    onCheckedChange = { onToggleLanguage() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldPrimary,
                        checkedTrackColor = Color(0xFF233B2B),
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = SlateBorder
                    ),
                    modifier = Modifier.testTag("switch_language")
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Engine & License Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isUkrainian) "Ліцензія та вихідний код" else "License & Open Source",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Stockfish поширюється під ліцензією GNU General Public License v3 (GPLv3).\nОфіційний репозиторій: github.com/official-stockfish/Stockfish\n100% Локальний аналіз • Повна автономність",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
