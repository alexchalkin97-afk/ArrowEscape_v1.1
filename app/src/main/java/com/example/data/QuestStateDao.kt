package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestStateDao {
    @Query("SELECT * FROM quest_states ORDER BY id ASC")
    fun getAllQuests(): Flow<List<QuestState>>

    @Query("SELECT * FROM quest_states WHERE id = :id")
    suspend fun getQuest(id: String): QuestState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(quest: QuestState)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(quests: List<QuestState>)

    @Query("DELETE FROM quest_states")
    suspend fun deleteAllQuests()
}
