# Grandmaster Chess Review & Stockfish Analysis

A native, standalone Android chess analysis application powered by the Stockfish UCI engine. It delivers deep, local game reviews comparable to premium web platforms (such as Chess.com Game Review and Lichess Analysis) completely offline, without subscriptions, accounts, or cloud dependencies.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during initial clarification and are locked into the design:

- **Visual Theme & Board Style**: Modern dark theme with an **Emerald & Dark Slate** chessboard, subtle wood/accent highlights, clear rank/file coordinates, and smooth legal move markers.
- **Stockfish Analysis Profile**: **Deep Grandmaster Analysis** (depth 22+, high tactical precision) with customizable profiles (Fast, Balanced, Deep Grandmaster) for threads, hash size, and multi-PV lines.
- **Localization**: Full dual localization (**Ukrainian** and **English**) with automatic system locale detection and an in-app language switcher.
- **Zero Cloud & Zero Paywall**: All PGN parsing, game replay, UCI engine analysis, and game persistence are 100% on-device using local storage (Room/SQLite) and local engine execution.

---

## 1. Overview & Core Concept

- **What It Does**: Allows chess players to import games (PGN files from Chess.com, Lichess, or tournaments, or manual move input), replay moves on an interactive board, and run an automated full-game review. The review classifies every move (Brilliant, Best, Excellent, Good, Book, Inaccuracy, Mistake, Blunder), computes player accuracy percentages (0-100%), displays an interactive evaluation advantage graph, renders best-move arrows, and lets players explore alternative engine variations (MultiPV).
- **Target Audience**: Club players, tournament competitors, and chess enthusiasts seeking private, free, and unrestricted game analysis without monthly limits or server queues.
- **Key Value**: Delivers professional desktop-grade chess review directly on an Android device with instant offline responsiveness.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Quick Start / Library**:
   - The user opens the app to their **Saved Games Library** featuring preloaded iconic master games (e.g., Fischer's Game of the Century, Kasparov's Immortal, Opera Game) and their imported matches.
   - A floating action bar offers "Import PGN", "Paste PGN", or "Set Up Custom Position / New Game".
2. **Interactive Board & Replay**:
   - The top/center displays the high-fidelity chessboard with responsive piece dragging or tap-to-move, legal move highlights, last-move indicators, and king-in-check warnings.
   - On the left of the board, an animated **Evaluation Advantage Bar** visually represents the position evaluation (white vs. black advantage, or mate-in-N).
   - Below the board, a player bar shows player names, ratings, time control, and captured piece differences.
3. **Automated Game Review**:
   - Tapping "Run Game Review" starts asynchronous background analysis using Stockfish.
   - A smooth progress bar tracks analysis progress (e.g., Move 14/42, Depth 22, 1.4M nps).
   - Once completed, the **Game Review Summary Sheet** reveals:
     - Overall accuracy: White % vs. Black % (calculated via win probability delta).
     - Move breakdown grid: Brilliant (`!!`), Best (`★`), Excellent, Good, Inaccuracy (`?!`), Mistake (`?`), Blunder (`??`).
     - Interactive evaluation graph: scrolling or tapping points jumps immediately to the exact move and position.
4. **Move Exploration & MultiPV**:
   - Stepping through moves updates the engine suggestion panel showing best move arrows on the board, the evaluation score (+1.45, -0.80, or #M3), and top alternative lines.
   - Users can try out "What If" alternative moves directly on the board and let Stockfish evaluate them on the fly.

### Visual Identity & Theme

- **Background & Surfaces**: Deep charcoal (`#121417`), dark slate card surfaces (`#1B1F24`), elevated borders (`#2A3038`).
- **Board Colors**:
  - Dark squares: Rich Forest Slate Emerald (`#4B7355` / `#3B5E43`).
  - Light squares: Crisp Ivory Slate (`#EAEED4` / `#DFE3C8`).
  - Move Highlights: Amber translucent tint (`#FFEB3B40`) for source/target squares.
  - In-Check King: Ruby red radial gradient (`#E5393566`).
  - Best Move Arrow: Neon Emerald Green (`#00E676`) with drop shadow.
- **Move Badge Colors**:
  - Brilliant `!!`: Cyan / Teal (`#00E5FF`).
  - Best `★`: Vibrant Emerald (`#00C853`).
  - Excellent: Bright Green (`#64DD17`).
  - Good: Sage Green (`#81C784`).
  - Book: Terracotta / Brown (`#A1887F`).
  - Inaccuracy `?!`: Amber Yellow (`#FFD600`).
  - Mistake `?`: Orange (`#FF9100`).
  - Blunder `??`: Vivid Crimson (`#D50000`).
- **Typography**: Clean, monospace-friendly tabular numbers for evaluation clocks and move notation, paired with modern high-legibility display headers.

---

## 3. Key Product Decisions & Trade-Offs

- **Stockfish Engine Architecture & Fail-Safe Integration**:
  - *Approach*: Implement a robust dual-layer UCI engine pipeline. The app includes a native UCI executable connector (`StockfishUciProcess`) designed for Android ARM64/x86_64 binaries, coupled with an embedded Kotlin-native UCI bitboard engine (`StockfishLiteEngine`).
  - *Why*: This ensures 100% reliable execution in all environments (Robolectric JVM tests, older Android emulators, and physical devices), while delivering full UCI communication (`uci`, `isready`, `position`, `go depth`, `stop`, `bestmove`, `info score cp / mate`).
- **Chess Rules Engine (Bitboard Representation)**:
  - *Approach*: Built-in pure Kotlin complete chess logic covering full FEN parsing, SAN move generation, legal move validation, castling rights (O-O, O-O-O), en passant, pawn promotions, and draw conditions (threefold repetition, 50-move rule, insufficient material).
  - *Why*: Self-contained, dependency-free, zero risk of outdated unmaintained third-party SDKs, fully testable with local unit tests.
- **Win Probability & Accuracy Formula**:
  - *Approach*: Rather than raw centipawn differences (which distort in endgames), we compute win probability using standard logistic scaling:
    $$\text{WinProb} = \frac{100}{1 + 10^{-(\text{centipawns} / 400)}}$$
    Accuracy for each move is derived from the decrease in win probability, providing fair, balanced percentages for both sides.
- **Local Persistence via Room**:
  - *Approach*: Games, move records, evaluations, and analysis summaries are stored in a local SQLite database via Room with Kotlin Coroutines Flow.
  - *Why*: Instant loading, zero network requests, offline persistence, and automatic caching of previously analyzed positions.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│ ┌───────────────────┐  ┌────────────────────┐  ┌─────────────────────┐ │
│ │  ChessBoardView   │  │  GameReviewScreen  │  │   GameLibraryView   │ │
│ │ (Canvas, Arrows,  │  │ (Eval Bar, Chart,  │  │ (Saved Games, PGN   │ │
│ │  Drag & Tap)      │  │  Move Badges)      │  │  Import, Samples)   │ │
│ └─────────▲─────────┘  └─────────▲──────────┘  └──────────▲──────────┘ │
└───────────┼──────────────────────┼────────────────────────┼────────────┘
            │                      │                        │
┌───────────┴──────────────────────┴────────────────────────┴────────────┐
│                        ChessReviewViewModel                            │
│  - Active Game State & Move History StateFlow                          │
│  - Analysis Progress & Live Engine MultiPV StateFlow                   │
│  - Move Classification & Accuracy Calculation Engine                   │
└───────────────────────────▲────────────────────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────────────────────┐
│                      Domain & Engine Services                          │
│ ┌───────────────────────────────┐   ┌────────────────────────────────┐ │
│ │      ChessRulesEngine         │   │       UciEngineService         │ │
│ │ (Bitboard / Move Validation,  │   │ (Stockfish UCI Process Bridge, │ │
│ │  FEN, SAN, PGN Parser/Export) │   │  Command I/O, MultiPV parsing) │ │
│ └───────────────────────────────┘   └────────────────────────────────┘ │
└───────────────────────────▲────────────────────────────────────────────┘
                            │
┌───────────────────────────┴────────────────────────────────────────────┐
│                       Data Layer (Room DB)                             │
│ ┌───────────────────────────────┐   ┌────────────────────────────────┐ │
│ │         GameDao / Repo        │   │        AnalysisCacheDao        │ │
│ │ (GameEntity, PGN text, Date,  │   │ (Position FEN -> BestMove,     │ │
│ │  White/Black, Result, ECO)    │   │  Evaluation, MultiPV, Depth)   │ │
│ └───────────────────────────────┘   └────────────────────────────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State

- **`ChessPosition`**: 64-square board array, active turn (White/Black), castling rights, en passant target square, halfmove clock, fullmove number.
- **`ChessMove`**: From square, to square, promotion piece, SAN notation (e.g. `Nf3`, `exd5`, `O-O`, `Qh7#`), captured piece, flags.
- **`PositionEvaluation`**: Score in centipawns or mate-in-moves (`#3`, `#-2`), depth reached, principal variation (PV line), best move.
- **`AnalyzedMove`**: Played move, best move suggested by engine, evaluation before move, evaluation after move, centipawn loss, win probability loss, move classification (`BRILLIANT`, `BEST`, `EXCELLENT`, `GOOD`, `BOOK`, `INACCURACY`, `MISTAKE`, `BLUNDER`), brief explanation.
- **`GameReviewReport`**: White accuracy %, Black accuracy %, move classification tallies for both sides, advantage shift chart data points.
- **`GameEntity`**: Room database record holding PGN metadata, full move history, and cached review JSON.

### Interactive Component & State Mapping

- **Square Selection / Drag**: Interacting with a piece calculates legal destinations via `ChessRulesEngine.getLegalMoves(square)`. Destination squares render subtle selection rings. Dropping or clicking valid square triggers move commit, audio/haptic feedback, and updates board state.
- **Pawn Promotion Modal**: When a pawn reaches the 8th or 1st rank, an elegant M3 dialog allows selecting Queen, Rook, Bishop, or Knight.
- **Move List Navigation**: User can scrub through moves via bottom controller (`<<`, `<`, `>`, `>>`), tap any move in the SAN list, or click on points in the evaluation chart.
- **Review Run / Cancel**: "Analyze" button launches a Kotlin Coroutine job on `Dispatchers.Default` analyzing each move sequentially through `UciEngineService`. User can pause, resume, or cancel at any time with immediate UI reactivity.
