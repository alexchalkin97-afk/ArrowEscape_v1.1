package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelStateDao {
    @Query("SELECT * FROM level_states ORDER BY levelId ASC")
    fun getAllLevelStates(): Flow<List<LevelState>>

    @Query("SELECT * FROM level_states WHERE levelId = :levelId")
    suspend fun getLevelState(levelId: Int): LevelState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(levelState: LevelState)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(levelStates: List<LevelState>)

    @Query("UPDATE level_states SET isUnlocked = 1")
    suspend fun unlockAllLevels()

    @Transaction
    suspend fun unlockNextLevel(completedLevelId: Int) {
        val current = getLevelState(completedLevelId)
        insertOrUpdate(current?.copy(isCompleted = true) ?: LevelState(completedLevelId, isCompleted = true, isUnlocked = true))
        
        val nextLevelId = completedLevelId + 1
        if (nextLevelId <= 1000) {
            val next = getLevelState(nextLevelId)
            if (next == null || !next.isUnlocked) {
                insertOrUpdate(next?.copy(isUnlocked = true) ?: LevelState(nextLevelId, isCompleted = false, isUnlocked = true))
            }
        }
    }
}
