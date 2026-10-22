package com.example.chess.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val whitePlayer: String,
    val blackPlayer: String,
    val whiteElo: String? = null,
    val blackElo: String? = null,
    val result: String,
    val date: String,
    val event: String? = null,
    val eco: String? = null,
    val pgnText: String,
    val initialFen: String,
    val moveCount: Int,
    val whiteAccuracy: Double? = null,
    val blackAccuracy: Double? = null,
    val reviewReportJson: String? = null,
    val analyzedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
