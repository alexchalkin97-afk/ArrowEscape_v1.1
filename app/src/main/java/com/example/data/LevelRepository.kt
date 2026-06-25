package com.example.data

import kotlinx.coroutines.flow.Flow

class LevelRepository(private val dao: LevelStateDao) {
    val allLevelStates: Flow<List<LevelState>> = dao.getAllLevelStates()

    suspend fun getLevelState(levelId: Int): LevelState? {
        return dao.getLevelState(levelId)
    }

    suspend fun saveLevelState(levelState: LevelState) {
        dao.insertOrUpdate(levelState)
    }

    suspend fun saveLevelStates(levelStates: List<LevelState>) {
        dao.insertOrUpdateAll(levelStates)
    }

    suspend fun unlockAllLevels() {
        dao.unlockAllLevels()
    }

    suspend fun completeAndUnlockNext(levelId: Int, moves: Int) {
        val current = dao.getLevelState(levelId)
        val bestMoves = if (current != null && current.bestMoves > 0) {
            val minMoves = minOf(current.bestMoves, moves)
            minMoves
        } else {
            moves
        }
        dao.insertOrUpdate(
            LevelState(
                levelId = levelId,
                isCompleted = true,
                bestMoves = bestMoves,
                isUnlocked = true
            )
        )
        dao.unlockNextLevel(levelId)
    }
}
