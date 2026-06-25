package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementStateDao {
    @Query("SELECT * FROM achievement_states ORDER BY id ASC")
    fun getAllAchievements(): Flow<List<AchievementState>>

    @Query("SELECT * FROM achievement_states WHERE id = :id")
    suspend fun getAchievement(id: String): AchievementState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(achievement: AchievementState)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(achievements: List<AchievementState>)

    @Query("DELETE FROM achievement_states")
    suspend fun deleteAllAchievements()
}
