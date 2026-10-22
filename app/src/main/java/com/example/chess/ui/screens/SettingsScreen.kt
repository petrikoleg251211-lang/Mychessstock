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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: EngineSettings,
    isUkrainian: Boolean,
    onSettingsChanged: (EngineSettings) -> Unit,
    onToggleLanguage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

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

        // Profile Presets Card
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
                        if (isUkrainian) "Швидкий (Глибина 14)" else "Fast (Depth 14)",
                        14,
                        600L
                    ),
                    Triple(
                        if (isUkrainian) "Збалансований (Глибина 18)" else "Balanced (Depth 18)",
                        18,
                        1000L
                    ),
                    Triple(
                        if (isUkrainian) "Гросмейстер (Глибина 22+)" else "Grandmaster (Depth 22+)",
                        22,
                        1600L
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

        // Fine Tuning Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isUkrainian) "Параметри обчислень" else "Computation Engine Options",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Depth Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isUkrainian) "Максимальна глибина" else "Target Search Depth",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${settings.depth}",
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    )
                }
                Slider(
                    value = settings.depth.toFloat(),
                    onValueChange = { onSettingsChanged(settings.copy(depth = it.toInt())) },
                    valueRange = 10f..26f,
                    steps = 7,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary,
                        inactiveTrackColor = SlateBorder
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Threads Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUkrainian) "Кількість потоків (Threads)" else "CPU Threads",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (th in listOf(1, 2, 4)) {
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

                Spacer(modifier = Modifier.height(12.dp))

                // Hash Size Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUkrainian) "Розмір хешу (Hash MB)" else "Hash Table Size (MB)",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (mb in listOf(32, 64, 128)) {
                            val active = settings.hashSizeMb == mb
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (active) EmeraldPrimary else SlateSurfaceVariant)
                                    .clickable { onSettingsChanged(settings.copy(hashSizeMb = mb)) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$mb",
                                    color = if (active) Color.Black else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // MultiPV Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUkrainian) "Лінії MultiPV (Варіанти)" else "MultiPV Alternate Lines",
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

        // Language & Localization Card
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

        // Engine & Legal Information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, SlateBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isUkrainian) "Про рушій і ліцензію" else "About Engine & License",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Stockfish UCI Engine • Open Source (GPLv3)\n100% Offline • Без реклами та платних підписок",
                    color = TextMuted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
