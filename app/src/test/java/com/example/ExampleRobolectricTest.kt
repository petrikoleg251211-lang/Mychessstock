package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.chess.data.ChessDatabase
import com.example.chess.data.GameEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Stockfish Review", appName)
    }

    @Test
    fun `test Room database operations`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = ChessDatabase.getDatabase(context)
        val dao = db.gameDao()

        val game = GameEntity(
            title = "Test Game",
            whitePlayer = "Player1",
            blackPlayer = "Player2",
            result = "1-0",
            date = "2026.10.22",
            pgnText = "1. e4 e5 2. Nf3 Nc6 1-0",
            initialFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
            moveCount = 4
        )

        val id = dao.insertGame(game)
        assertNotNull(id)

        val retrieved = dao.getGameById(id)
        assertNotNull(retrieved)
        assertEquals("Player1", retrieved?.whitePlayer)
        assertEquals("1-0", retrieved?.result)
    }
}
