package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quest_states")
data class QuestState(
    @PrimaryKey val id: String,
    val type: String, // "DAILY" or "WEEKLY"
    val titleEn: String,
    val titleRu: String,
    val target: Int,
    val current: Int,
    val rewardCoins: Int,
    val rewardDiamonds: Int,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false
)
