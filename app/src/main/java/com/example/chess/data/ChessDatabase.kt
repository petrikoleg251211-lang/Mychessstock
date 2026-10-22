package com.example.chess.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query("SELECT * FROM saved_games ORDER BY createdAt DESC")
    fun getAllGames(): Flow<List<GameEntity>>

    @Query("SELECT * FROM saved_games WHERE id = :id LIMIT 1")
    suspend fun getGameById(id: Long): GameEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity): Long

    @Update
    suspend fun updateGame(game: GameEntity)

    @Query("DELETE FROM saved_games WHERE id = :id")
    suspend fun deleteGame(id: Long)

    @Query("DELETE FROM saved_games")
    suspend fun clearAll()
}

@Database(entities = [GameEntity::class], version = 1, exportSchema = false)
abstract class ChessDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    companion object {
        @Volatile
        private var INSTANCE: ChessDatabase? = null

        fun getDatabase(context: Context): ChessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChessDatabase::class.java,
                    "chess_review_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class GameRepository(private val gameDao: GameDao) {
    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()

    suspend fun getGame(id: Long): GameEntity? = gameDao.getGameById(id)
    suspend fun saveGame(game: GameEntity): Long = gameDao.insertGame(game)
    suspend fun updateGame(game: GameEntity) = gameDao.updateGame(game)
    suspend fun deleteGame(id: Long) = gameDao.deleteGame(id)
}
