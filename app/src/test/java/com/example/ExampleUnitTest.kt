package com.example

import com.example.chess.data.SampleGames
import com.example.chess.engine.EngineEvaluation
import com.example.chess.engine.MoveClassifier
import com.example.chess.engine.StockfishLiteEngine
import com.example.chess.engine.UciParser
import com.example.chess.model.Move
import com.example.chess.model.MoveClassification
import com.example.chess.model.PieceColor
import com.example.chess.model.PieceType
import com.example.chess.pgn.PgnParser
import com.example.chess.rules.ChessPosition
import com.example.chess.rules.ChessRulesEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testStartingPositionLegalMoves() {
        val startPos = ChessPosition.fromFen(ChessPosition.STARTING_FEN)
        val legalMoves = ChessRulesEngine.generateLegalMoves(startPos)
        // In chess, starting position has exactly 20 legal moves: 16 pawn moves (8 single, 8 double) + 4 knight moves
        assertEquals(20, legalMoves.size)
    }

    @Test
    fun testScholarsMate() {
        // 1. e4 e5 2. Qh5 Nc6 3. Bc4 Nf6 4. Qxf7#
        var pos = ChessPosition.fromFen(ChessPosition.STARTING_FEN)

        val moves = listOf("e4", "e5", "Qh5", "Nc6", "Bc4", "Nf6", "Qxf7#")
        for (san in moves) {
            val move = ChessRulesEngine.parseSan(pos, san)
            assertNotNull("Move $san should be parseable", move)
            pos = ChessRulesEngine.makeMove(pos, move!!)
        }

        assertTrue("Position after Qxf7# must be in check", ChessRulesEngine.isInCheck(pos, PieceColor.BLACK))
        assertTrue("Position after Qxf7# must be checkmate", ChessRulesEngine.isCheckmate(pos))
    }

    @Test
    fun testCastlingLegality() {
        // Setup position where White can castle Kingside: 1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5
        val pgn = "1. e4 e5 2. Nf3 Nc6 3. Bc4 Bc5"
        val game = PgnParser.parse(pgn)
        val pos = game.positions.last()

        val legal = ChessRulesEngine.generateLegalMoves(pos)
        val castlingMove = legal.firstOrNull { it.isCastling && it.to == 6 }
        assertNotNull("White should be able to castle kingside (O-O)", castlingMove)

        val nextPos = ChessRulesEngine.makeMove(pos, castlingMove!!)
        assertEquals(PieceType.KING, nextPos.getPiece(6)?.type)
        assertEquals(PieceType.ROOK, nextPos.getPiece(5)?.type)
        assertFalse(nextPos.castlingRights.whiteKingSide)
        assertFalse(nextPos.castlingRights.whiteQueenSide)
    }

    @Test
    fun testEnPassant() {
        // White: pawn on e5, Black plays d5 -> White can play exd6 e.p.
        var pos = ChessPosition.fromFen("rnbqkbnr/pppppppp/8/4P3/8/8/PPPP1PPP/RNBQKBNR b KQkq - 0 2")
        val blackMove = ChessRulesEngine.parseSan(pos, "d5")
        assertNotNull(blackMove)
        pos = ChessRulesEngine.makeMove(pos, blackMove!!)

        assertEquals(Move.algebraicToSquare("d6"), pos.enPassantSquare)

        val epMove = ChessRulesEngine.parseSan(pos, "exd6")
        assertNotNull("exd6 en passant must be legal", epMove)
        assertTrue(epMove!!.isEnPassant)

        pos = ChessRulesEngine.makeMove(pos, epMove)
        assertEquals(null, pos.getPiece(Move.algebraicToSquare("d5")))
        assertEquals(PieceType.PAWN, pos.getPiece(Move.algebraicToSquare("d6"))?.type)
    }

    @Test
    fun testPgnParserWithHeadersAndMoves() {
        val game = PgnParser.parse(SampleGames.FISCHER_GAME_OF_THE_CENTURY)
        assertEquals("Donald Byrne", game.white)
        assertEquals("Robert James Fischer", game.black)
        assertEquals("0-1", game.result)
        assertEquals("D92", game.eco)
        assertTrue("Byrne vs Fischer should have 82 half-moves (41 full moves)", game.moves.size >= 80)
    }

    @Test
    fun testUciParser() {
        val line = "info depth 18 seldepth 24 score cp 45 nodes 152000 nps 1200000 time 126 multipv 1 pv e2e4 c7c5 g1f3"
        val eval = UciParser.parseInfoLine(line)
        assertNotNull(eval)
        assertEquals(18, eval!!.depth)
        assertEquals(45, eval.scoreCp)
        assertEquals("e2e4", eval.bestMoveUci)
        assertEquals(152000L, eval.nodes)

        val bestLine = "bestmove e2e4 ponder e7e5"
        val best = UciParser.parseBestMove(bestLine)
        assertEquals("e2e4", best)
    }

    @Test
    fun testMoveClassifierWinProbabilityAndAccuracy() {
        // Equal evaluation gives ~50%
        val winProb0 = MoveClassifier.winProbability(0)
        assertEquals(50.0, winProb0, 0.5)

        // +400 cp advantage gives ~90.9% win probability
        val winProb400 = MoveClassifier.winProbability(400)
        assertTrue(winProb400 > 90.0)

        // Best move gives 100% accuracy
        val pos = ChessPosition.fromFen(ChessPosition.STARTING_FEN)
        val move = ChessRulesEngine.parseSan(pos, "e4")!!
        val nextPos = ChessRulesEngine.makeMove(pos, move)

        val classified = MoveClassifier.classifyMove(
            positionBefore = pos,
            positionAfter = nextPos,
            playedMove = move,
            bestMove = move,
            evalBeforeCp = 20,
            evalAfterCp = 25,
            isMateBefore = false,
            isMateAfter = false,
            mateInBefore = 0,
            mateInAfter = 0,
            moveNumber = 1,
            color = PieceColor.WHITE,
            sanMovesSoFar = listOf("e4"),
            isUkrainian = true
        )

        // Either BOOK or BEST in opening
        assertTrue(
            classified.classification == MoveClassification.BOOK ||
                    classified.classification == MoveClassification.BEST
        )
    }

    @Test
    fun testStockfishLiteEngineEvaluation() = runBlocking {
        val engine = StockfishLiteEngine()
        engine.initialize()
        engine.setPosition(ChessPosition.STARTING_FEN)

        val eval = engine.search(depth = 4, movetimeMs = 500L)
        assertNotNull(eval.bestMoveUci)
        assertTrue(eval.nodes > 0)
        assertTrue(eval.depth >= 1)
        engine.close()
    }

    @Test
    fun testRealStockfishBinaryUciHandshake() {
        val binaryFile = java.io.File("src/main/jniLibs/x86_64/libstockfish.so")
        if (!binaryFile.exists() || !binaryFile.canExecute()) {
            return // Skip if running on non-x86 host without binary
        }

        val pb = ProcessBuilder(binaryFile.absolutePath)
        pb.redirectErrorStream(true)
        val proc = pb.start()

        val reader = proc.inputStream.bufferedReader()
        val writer = proc.outputStream.bufferedWriter()

        writer.write("uci\n")
        writer.flush()

        var engineName: String? = null
        var uciOk = false
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            val l = line?.trim() ?: break
            if (l.startsWith("id name ")) {
                engineName = l.removePrefix("id name ")
            }
            if (l == "uciok") {
                uciOk = true
                break
            }
        }

        assertTrue("UCI handshake must output uciok", uciOk)
        assertNotNull("Must capture engine name", engineName)
        assertTrue("Must be Stockfish 19", engineName!!.contains("Stockfish 19"))

        // Test position and search
        writer.write("position startpos moves e2e4 e7e5\n")
        writer.write("go depth 6\n")
        writer.flush()

        var bestMoveFound: String? = null
        while (reader.readLine().also { line = it } != null) {
            val l = line?.trim() ?: break
            if (l.startsWith("bestmove ")) {
                bestMoveFound = l.split("\\s+".toRegex()).getOrNull(1)
                break
            }
        }

        assertNotNull("Stockfish must output a legal best move", bestMoveFound)

        writer.write("quit\n")
        writer.flush()
        proc.waitFor()
    }
}
