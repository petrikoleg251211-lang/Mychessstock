package com.example.chess.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.data.SampleGames
import com.example.chess.pgn.PgnParser
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ImportScreen(
    isUkrainian: Boolean,
    onGameImported: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pgnInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().use { it.readText() }
                    pgnInput = text
                    errorMessage = null
                }
            } catch (e: Exception) {
                errorMessage = if (isUkrainian) "Помилка відкриття файлу: ${e.message}" else "File read error: ${e.message}"
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = if (isUkrainian) "Імпорт шахової партії (PGN)" else "Import Chess Game (PGN)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (isUkrainian)
                "Підтримуються PGN файли з Chess.com, Lichess та турнірних баз"
            else
                "Supports standard PGN from Chess.com, Lichess, and tournament archives",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row: Open File + Insert Demo
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = { filePicker.launch("*/*") },
                modifier = Modifier.weight(1f).testTag("btn_pick_file"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isUkrainian) "Вибрати файл" else "Pick File", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = {
                    pgnInput = SampleGames.OPERA_GAME
                    errorMessage = null
                },
                modifier = Modifier.weight(1f).testTag("btn_paste_sample"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Text(if (isUkrainian) "Вставити приклад" else "Sample PGN", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // PGN Text Input Field
        OutlinedTextField(
            value = pgnInput,
            onValueChange = {
                pgnInput = it
                errorMessage = null
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("pgn_text_field"),
            placeholder = {
                Text(
                    text = if (isUkrainian)
                        "[Event \"Game\"]\n1. e4 e5 2. Nf3 Nc6 3. Bc4 ...\nВставте сюди текст PGN..."
                    else
                        "[Event \"Game\"]\n1. e4 e5 2. Nf3 Nc6 3. Bc4 ...\nPaste PGN text here...",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SlateSurface,
                unfocusedContainerColor = SlateSurface,
                focusedBorderColor = EmeraldPrimary,
                unfocusedBorderColor = SlateBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = Color(0xFFFF5252),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Import & Analyze Button
        Button(
            onClick = {
                if (pgnInput.isBlank()) {
                    errorMessage = if (isUkrainian) "Будь ласка, вставте PGN текст" else "Please paste PGN text"
                    return@Button
                }
                try {
                    val parsed = PgnParser.parse(pgnInput)
                    if (parsed.moves.isEmpty()) {
                        errorMessage = if (isUkrainian) "У PGN не знайдено валідних ходів" else "No valid moves found in PGN"
                        return@Button
                    }
                    onGameImported(pgnInput)
                } catch (e: Exception) {
                    errorMessage = if (isUkrainian) "Помилка розбору: ${e.message}" else "Parse error: ${e.message}"
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("btn_import_submit"),
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.Black),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isUkrainian) "Завантажити та відкрити" else "Load and Review",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
