package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "level_states")
data class LevelState(
    @PrimaryKey val levelId: Int,
    val isCompleted: Boolean = false,
    val bestMoves: Int = 0,
    val isUnlocked: Boolean = false
)
