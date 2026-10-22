package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chess.rules.OpeningBook
import com.example.chess.ui.ChessReviewViewModel
import com.example.chess.ui.screens.AnalysisScreen
import com.example.chess.ui.screens.ImportScreen
import com.example.chess.ui.screens.LibraryScreen
import com.example.chess.ui.screens.ReportScreen
import com.example.chess.ui.screens.SettingsScreen
import com.example.ui.theme.ChessReviewTheme
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.SlateBackground
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChessReviewTheme {
                ChessApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessApp(viewModel: ChessReviewViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedGames by viewModel.savedGames.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle back button when not on main board
    if (selectedTab != 0) {
        BackHandler {
            selectedTab = 0
        }
    }

    // Show status messages in Snackbar
    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    // Identify current opening from moves played so far
    val sanMoves = uiState.game.moves.take(uiState.currentMoveIndex).map { it.san }
    val opening = OpeningBook.identifyOpening(sanMoves)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = SlateBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Stockfish Review",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (opening != null) {
                            Text(
                                text = if (uiState.isUkrainian) "${opening.eco}: ${opening.nameUa}" else "${opening.eco}: ${opening.nameEn}",
                                fontSize = 11.sp,
                                color = EmeraldPrimary,
                                maxLines = 1
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.flipBoard() },
                        modifier = Modifier.testTag("appbar_flip")
                    ) {
                        Icon(
                            Icons.Default.SwapVert,
                            contentDescription = "Flip Board",
                            tint = TextSecondary
                        )
                    }
                },
                actions = {
                    // Language Switch Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF202C24))
                            .clickable { viewModel.toggleLanguage() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("appbar_lang")
                    ) {
                        Text(
                            text = if (uiState.isUkrainian) "UA" else "EN",
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = { viewModel.resetBoard() },
                        modifier = Modifier.testTag("appbar_new_game")
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "New Game",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = SlateSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SlateSurface,
                tonalElevation = 4.dp
            ) {
                val tabs = listOf(
                    Triple(
                        if (uiState.isUkrainian) "Дошка" else "Board",
                        Icons.Default.SportsEsports,
                        0
                    ),
                    Triple(
                        if (uiState.isUkrainian) "Звіт" else "Report",
                        Icons.Default.Assessment,
                        1
                    ),
                    Triple(
                        if (uiState.isUkrainian) "Імпорт" else "Import",
                        Icons.Default.FileDownload,
                        2
                    ),
                    Triple(
                        if (uiState.isUkrainian) "Партії" else "Library",
                        Icons.Default.Folder,
                        3
                    ),
                    Triple(
                        if (uiState.isUkrainian) "Опції" else "Settings",
                        Icons.Default.Settings,
                        4
                    )
                )

                for ((label, icon, index) in tabs) {
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 11.sp, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = EmeraldPrimary,
                            indicatorColor = EmeraldPrimary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        ),
                        modifier = Modifier.testTag("nav_tab_$index")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> AnalysisScreen(
                    state = uiState,
                    onSquareClicked = { viewModel.onSquareClicked(it) },
                    onPromotionSelected = { viewModel.onPromotionSelected(it) },
                    onDismissPromotion = { viewModel.dismissPromotion() },
                    onStartReview = { viewModel.startReview() },
                    onStopReview = { viewModel.stopReview() },
                    onFirstMove = { viewModel.firstMove() },
                    onPrevMove = { viewModel.prevMove() },
                    onNextMove = { viewModel.nextMove() },
                    onLastMove = { viewModel.lastMove() },
                    onGoToMove = { viewModel.goToMove(it) },
                    onFlipBoard = { viewModel.flipBoard() }
                )
                1 -> ReportScreen(
                    state = uiState,
                    onStartReview = {
                        selectedTab = 0
                        viewModel.startReview()
                    },
                    onSaveGame = { viewModel.saveCurrentGame() },
                    onGoToMoveAndSwitchTab = { moveIndex ->
                        viewModel.goToMove(moveIndex)
                        selectedTab = 0
                    }
                )
                2 -> ImportScreen(
                    isUkrainian = uiState.isUkrainian,
                    onGameImported = { pgn ->
                        viewModel.loadPgn(pgn)
                        selectedTab = 0
                    }
                )
                3 -> LibraryScreen(
                    savedGames = savedGames,
                    isUkrainian = uiState.isUkrainian,
                    onLoadGame = { pgn, title ->
                        viewModel.loadPgn(pgn, title)
                        selectedTab = 0
                    },
                    onDeleteGame = { viewModel.deleteGame(it) }
                )
                4 -> SettingsScreen(
                    settings = uiState.engineSettings,
                    isUkrainian = uiState.isUkrainian,
                    onSettingsChanged = { viewModel.updateSettings(it) },
                    onToggleLanguage = { viewModel.toggleLanguage() }
                )
            }
        }
    }
}
