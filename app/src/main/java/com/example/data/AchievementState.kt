package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievement_states")
data class AchievementState(
    @PrimaryKey val id: String,
    val titleEn: String,
    val titleRu: String,
    val target: Int,
    val current: Int,
    val rewardCoins: Int,
    val rewardDiamonds: Int,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false
)
