package com.example.chess.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess.data.ChessDatabase
import com.example.chess.data.GameEntity
import com.example.chess.data.GameRepository
import com.example.chess.data.SampleGames
import com.example.chess.engine.EngineEvaluation
import com.example.chess.engine.EngineSettings
import com.example.chess.engine.GameReviewEngine
import com.example.chess.engine.ReviewProgress
import com.example.chess.engine.StockfishNativeEngine
import com.example.chess.model.GameReviewReport
import com.example.chess.model.Move
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.pgn.PgnGame
import com.example.chess.pgn.PgnParser
import com.example.chess.rules.ChessPosition
import com.example.chess.rules.ChessRulesEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

data class ChessUiState(
    val game: PgnGame,
    val currentMoveIndex: Int = 0,
    val selectedSquare: Int? = null,
    val legalMovesForSelected: List<Move> = emptyList(),
    val isFlipped: Boolean = false,
    val isUkrainian: Boolean = true,
    val pendingPromotion: Pair<Int, Int>? = null,
    val reviewReport: GameReviewReport? = null,
    val reviewProgress: ReviewProgress? = null,
    val isReviewing: Boolean = false,
    val engineSettings: EngineSettings = EngineSettings(),
    val liveEvaluation: EngineEvaluation? = null,
    val statusMessage: String? = null
) {
    val currentPosition: ChessPosition
        get() = if (currentMoveIndex in game.positions.indices) {
            game.positions[currentMoveIndex]
        } else {
            game.positions.lastOrNull() ?: ChessPosition()
        }

    val lastMove: Move?
        get() = if (currentMoveIndex > 0 && currentMoveIndex <= game.moves.size) {
            game.moves[currentMoveIndex - 1]
        } else null

    val currentAnalyzedMove = reviewReport?.analyzedMoves?.getOrNull(currentMoveIndex - 1)

    val bestMoveForCurrentPos: Move?
        get() {
            if (currentAnalyzedMove?.bestMove != null) {
                return currentAnalyzedMove.bestMove
            }
            return liveEvaluation?.bestMoveUci?.let {
                ChessRulesEngine.parseUci(currentPosition, it)
            }
        }
}

class ChessReviewViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository
    private val engine = StockfishNativeEngine(application)
    private val reviewEngine = GameReviewEngine(engine)
    private var reviewJob: Job? = null
    private var liveEvalJob: Job? = null

    private val _uiState = MutableStateFlow(
        ChessUiState(
            game = PgnParser.parse(SampleGames.FISCHER_GAME_OF_THE_CENTURY),
            isUkrainian = Locale.getDefault().language.startsWith("uk")
        )
    )
    val uiState: StateFlow<ChessUiState> = _uiState.asStateFlow()

    val savedGames: StateFlow<List<GameEntity>>

    init {
        val database = ChessDatabase.getDatabase(application)
        repository = GameRepository(database.gameDao())
        savedGames = repository.allGames.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            engine.initialize()
            // Seed initial sample games if database is empty
            launch {
                val existing = database.gameDao().getGameById(1)
                if (existing == null) {
                    val parsed1 = PgnParser.parse(SampleGames.FISCHER_GAME_OF_THE_CENTURY)
                    repository.saveGame(
                        GameEntity(
                            title = "Byrne vs Fischer (Game of the Century)",
                            whitePlayer = parsed1.white,
                            blackPlayer = parsed1.black,
                            whiteElo = parsed1.whiteElo,
                            blackElo = parsed1.blackElo,
                            result = parsed1.result,
                            date = parsed1.date,
                            event = parsed1.event,
                            eco = parsed1.eco,
                            pgnText = SampleGames.FISCHER_GAME_OF_THE_CENTURY,
                            initialFen = parsed1.initialFen,
                            moveCount = parsed1.moves.size
                        )
                    )
                    val parsed2 = PgnParser.parse(SampleGames.KASPAROV_IMMORTAL)
                    repository.saveGame(
                        GameEntity(
                            title = "Kasparov vs Topalov (Immortal)",
                            whitePlayer = parsed2.white,
                            blackPlayer = parsed2.black,
                            whiteElo = parsed2.whiteElo,
                            blackElo = parsed2.blackElo,
                            result = parsed2.result,
                            date = parsed2.date,
                            event = parsed2.event,
                            eco = parsed2.eco,
                            pgnText = SampleGames.KASPAROV_IMMORTAL,
                            initialFen = parsed2.initialFen,
                            moveCount = parsed2.moves.size
                        )
                    )
                }
            }
            updateLiveEvaluation()
        }
    }

    fun onSquareClicked(square: Int) {
        val state = _uiState.value
        if (state.isReviewing) return

        val selected = state.selectedSquare
        if (selected == null) {
            // Select piece
            val piece = state.currentPosition.getPiece(square)
            if (piece != null && piece.color == state.currentPosition.sideToMove) {
                val legal = ChessRulesEngine.generateLegalMovesForSquare(state.currentPosition, square)
                _uiState.value = state.copy(
                    selectedSquare = square,
                    legalMovesForSelected = legal
                )
            }
        } else {
            // Attempt to move to destination square
            val matchingMove = state.legalMovesForSelected.firstOrNull { it.to == square }
            if (matchingMove != null) {
                val piece = state.currentPosition.getPiece(selected)
                val isPawnPromotion = piece?.type == PieceType.PAWN &&
                        ((piece.color == PieceColor.WHITE && square / 8 == 7) ||
                                (piece.color == PieceColor.BLACK && square / 8 == 0))

                if (isPawnPromotion) {
                    _uiState.value = state.copy(
                        pendingPromotion = Pair(selected, square),
                        selectedSquare = null,
                        legalMovesForSelected = emptyList()
                    )
                } else {
                    executeMove(matchingMove)
                }
            } else {
                // If clicked another own piece, select that piece instead
                val piece = state.currentPosition.getPiece(square)
                if (piece != null && piece.color == state.currentPosition.sideToMove) {
                    val legal = ChessRulesEngine.generateLegalMovesForSquare(state.currentPosition, square)
                    _uiState.value = state.copy(
                        selectedSquare = square,
                        legalMovesForSelected = legal
                    )
                } else {
                    _uiState.value = state.copy(
                        selectedSquare = null,
                        legalMovesForSelected = emptyList()
                    )
                }
            }
        }
    }

    fun onPromotionSelected(promoType: PieceType) {
        val state = _uiState.value
        val pending = state.pendingPromotion ?: return
        val legal = ChessRulesEngine.generateLegalMovesForSquare(state.currentPosition, pending.first)
        val move = legal.firstOrNull { it.to == pending.second && it.promotion == promoType }
            ?: Move(pending.first, pending.second, promotion = promoType)

        _uiState.value = state.copy(pendingPromotion = null)
        executeMove(move)
    }

    fun dismissPromotion() {
        _uiState.value = _uiState.value.copy(pendingPromotion = null)
    }

    private fun executeMove(move: Move) {
        val state = _uiState.value
        val newPos = ChessRulesEngine.makeMove(state.currentPosition, move)
        val san = ChessRulesEngine.moveToSan(state.currentPosition, move)
        val completeMove = move.copy(san = san)

        // Truncate game after current move index if user makes an alternative move
        val truncatedMoves = state.game.moves.take(state.currentMoveIndex).toMutableList()
        truncatedMoves.add(completeMove)

        val truncatedPositions = state.game.positions.take(state.currentMoveIndex + 1).toMutableList()
        truncatedPositions.add(newPos)

        val updatedGame = state.game.copy(
            moves = truncatedMoves,
            positions = truncatedPositions
        )

        _uiState.value = state.copy(
            game = updatedGame,
            currentMoveIndex = truncatedMoves.size,
            selectedSquare = null,
            legalMovesForSelected = emptyList(),
            reviewReport = null // Reset review report since game tree branched
        )
        updateLiveEvaluation()
    }

    fun goToMove(index: Int) {
        val state = _uiState.value
        val safeIndex = index.coerceIn(0, state.game.moves.size)
        _uiState.value = state.copy(
            currentMoveIndex = safeIndex,
            selectedSquare = null,
            legalMovesForSelected = emptyList()
        )
        updateLiveEvaluation()
    }

    fun nextMove() = goToMove(_uiState.value.currentMoveIndex + 1)
    fun prevMove() = goToMove(_uiState.value.currentMoveIndex - 1)
    fun firstMove() = goToMove(0)
    fun lastMove() = goToMove(_uiState.value.game.moves.size)

    fun flipBoard() {
        _uiState.value = _uiState.value.copy(isFlipped = !_uiState.value.isFlipped)
    }

    fun toggleLanguage() {
        _uiState.value = _uiState.value.copy(isUkrainian = !_uiState.value.isUkrainian)
    }

    fun loadPgn(pgnText: String, title: String? = null) {
        try {
            val game = PgnParser.parse(pgnText)
            _uiState.value = _uiState.value.copy(
                game = game,
                currentMoveIndex = 0,
                selectedSquare = null,
                legalMovesForSelected = emptyList(),
                reviewReport = null,
                reviewProgress = null,
                statusMessage = if (_uiState.value.isUkrainian) "Партію успішно завантажено" else "Game loaded successfully"
            )
            updateLiveEvaluation()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                statusMessage = if (_uiState.value.isUkrainian) "Помилка читання PGN: ${e.message}" else "PGN import error: ${e.message}"
            )
        }
    }

    fun resetBoard() {
        loadPgn(
            """
[Event "New Game"]
[Site "Android Chess"]
[Date "${java.text.SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()).format(java.util.Date())}"]
[White "White"]
[Black "Black"]
[Result "*"]

*
            """.trimIndent()
        )
    }

    fun startReview() {
        val state = _uiState.value
        if (state.isReviewing || state.game.moves.isEmpty()) return

        reviewJob?.cancel()
        reviewJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isReviewing = true,
                statusMessage = if (_uiState.value.isUkrainian) "Аналіз партії розпочато..." else "Review in progress..."
            )

            try {
                val report = reviewEngine.reviewGame(
                    game = state.game,
                    depth = state.engineSettings.depth,
                    timePerMoveMs = state.engineSettings.timeLimitPerMoveMs,
                    isUkrainian = state.isUkrainian,
                    onProgress = { progress ->
                        _uiState.value = _uiState.value.copy(reviewProgress = progress)
                    }
                )

                _uiState.value = _uiState.value.copy(
                    reviewReport = report,
                    isReviewing = false,
                    statusMessage = if (_uiState.value.isUkrainian) "Аналіз завершено!" else "Review completed!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isReviewing = false,
                    statusMessage = "Analysis stopped: ${e.message}"
                )
            }
        }
    }

    fun stopReview() {
        reviewJob?.cancel()
        viewModelScope.launch {
            engine.stop()
            _uiState.value = _uiState.value.copy(
                isReviewing = false,
                reviewProgress = null,
                statusMessage = if (_uiState.value.isUkrainian) "Аналіз зупинено" else "Review paused"
            )
        }
    }

    fun saveCurrentGame() {
        viewModelScope.launch {
            val state = _uiState.value
            val pgn = PgnParser.exportToPgn(state.game)
            val title = "${state.game.white} vs ${state.game.black}"
            val entity = GameEntity(
                title = title,
                whitePlayer = state.game.white,
                blackPlayer = state.game.black,
                whiteElo = state.game.whiteElo,
                blackElo = state.game.blackElo,
                result = state.game.result,
                date = state.game.date,
                event = state.game.event,
                eco = state.game.eco,
                pgnText = pgn,
                initialFen = state.game.initialFen,
                moveCount = state.game.moves.size,
                whiteAccuracy = state.reviewReport?.whiteAccuracy,
                blackAccuracy = state.reviewReport?.blackAccuracy
            )
            repository.saveGame(entity)
            _uiState.value = state.copy(
                statusMessage = if (state.isUkrainian) "Партію збережено в бібліотеці" else "Game saved to library"
            )
        }
    }

    fun deleteGame(id: Long) {
        viewModelScope.launch {
            repository.deleteGame(id)
        }
    }

    fun updateSettings(newSettings: EngineSettings) {
        _uiState.value = _uiState.value.copy(engineSettings = newSettings)
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    private fun updateLiveEvaluation() {
        val state = _uiState.value
        if (state.isReviewing) return

        liveEvalJob?.cancel()
        liveEvalJob = viewModelScope.launch {
            try {
                engine.setPosition(state.currentPosition.toFen())
                val eval = engine.search(
                    depth = 12,
                    movetimeMs = 400L,
                    multiPv = state.engineSettings.multiPv
                ) { info ->
                    _uiState.value = _uiState.value.copy(liveEvaluation = info)
                }
                _uiState.value = _uiState.value.copy(liveEvaluation = eval)
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        reviewJob?.cancel()
        liveEvalJob?.cancel()
        engine.close()
    }
}
