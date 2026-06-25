package com.example.game

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.LevelRepository
import com.example.data.LevelState
import com.example.data.QuestState
import com.example.data.AchievementState
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenType {
    MAIN_MENU,
    LEVEL_SELECT,
    PLAYING,
    BATTLE_PASS,     // Боевой пропуск
    ACHIEVEMENTS,    // Достижения
    DAILY_QUESTS,    // Ежедневные квесты
    WEEKLY_QUESTS,   // Еженедельные квесты
    SHOP,            // Магазин косметики
    REWARD_CENTER    // Центр наград (бывшие Подарки)
}

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = LevelRepository(db.levelStateDao())
    private val sp = application.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)

    // All levels states loaded reactively from database
    val levelStates: StateFlow<List<LevelState>> = repository.allLevelStates
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Navigation and screen management StateFlow
    private val _currentScreen = MutableStateFlow(ScreenType.MAIN_MENU)
    val currentScreen: StateFlow<ScreenType> = _currentScreen.asStateFlow()

    // Current screen flow: true for game screen, false for level select dashboard
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _activeLevelIndex = MutableStateFlow(1)
    val activeLevelIndex: StateFlow<Int> = _activeLevelIndex.asStateFlow()

    // Interactive level state
    var levelData by mutableStateOf<LevelData?>(null)
        private set

    var arrows by mutableStateOf<List<Arrow>>(emptyList())
        private set

    var movesCount by mutableStateOf(0)
        private set

    var isLevelCleared by mutableStateOf(false)
        private set

    var isWinningTransition by mutableStateOf(false)
        private set

    var isLoadingLevel by mutableStateOf(false)
        private set

    // Navigation helper methods
    fun navigateTo(screen: ScreenType) {
        _currentScreen.value = screen
    }

    fun navigateBack() {
        _currentScreen.value = ScreenType.MAIN_MENU
    }

    // Economy mechanics
    var coins by mutableStateOf(150)
        private set
    var diamonds by mutableStateOf(15)
        private set

    fun addCoins(amount: Int) {
        coins += amount
        saveResourceState()
        viewModelScope.launch { syncCoinsAchievement() }
    }

    fun addDiamonds(amount: Int) {
        diamonds += amount
        saveResourceState()
    }

    // Battle Pass properties
    var seasonXp by mutableStateOf(0)
        private set
    var seasonLevel by mutableStateOf(1)
        private set
    var hasPremiumPass by mutableStateOf(false)
        private set
    var claimedSeasonRewards by mutableStateOf<Set<Int>>(emptySet())
        private set
    var claimedSeasonPremiumRewards by mutableStateOf<Set<Int>>(emptySet())
        private set

    fun addSeasonXp(amount: Int) {
        seasonXp += amount
        while (seasonXp >= 100 && seasonLevel < 10) {
            seasonXp -= 100
            seasonLevel += 1
        }
        saveBattlePassState()
    }

    fun buyPremiumPass(): Boolean {
        if (diamonds >= 25 && !hasPremiumPass) {
            diamonds -= 25
            hasPremiumPass = true
            saveResourceState()
            saveBattlePassState()
            return true
        }
        return false
    }

    fun getFreeRewardForLevel(level: Int): Pair<String, Int> {
        return when (level) {
            1 -> Pair("coins", 50)
            2 -> Pair("diamonds", 2)
            3 -> Pair("coins", 100)
            4 -> Pair("diamonds", 5)
            5 -> Pair("coins", 150)
            6 -> Pair("diamonds", 8)
            7 -> Pair("coins", 200)
            8 -> Pair("diamonds", 10)
            9 -> Pair("coins", 300)
            10 -> Pair("diamonds", 20)
            else -> Pair("coins", 0)
        }
    }

    fun getPremiumRewardForLevel(level: Int): Pair<String, Int> {
        return when (level) {
            1 -> Pair("diamonds", 10)
            2 -> Pair("coins", 200)
            3 -> Pair("diamonds", 15)
            4 -> Pair("coins", 300)
            5 -> Pair("diamonds", 20)
            6 -> Pair("coins", 400)
            7 -> Pair("diamonds", 25)
            8 -> Pair("coins", 500)
            9 -> Pair("diamonds", 30)
            10 -> Pair("cosmetic_skin", 1) // prestige golden skin
            else -> Pair("coins", 0)
        }
    }

    fun claimFreeReward(level: Int): Boolean {
        if (seasonLevel >= level && !claimedSeasonRewards.contains(level)) {
            val reward = getFreeRewardForLevel(level)
            when (reward.first) {
                "coins" -> addCoins(reward.second)
                "diamonds" -> addDiamonds(reward.second)
            }
            claimedSeasonRewards = claimedSeasonRewards + level
            saveBattlePassState()
            return true
        }
        return false
    }

    fun claimPremiumReward(level: Int): Boolean {
        if (hasPremiumPass && seasonLevel >= level && !claimedSeasonPremiumRewards.contains(level)) {
            val reward = getPremiumRewardForLevel(level)
            when (reward.first) {
                "coins" -> addCoins(reward.second)
                "diamonds" -> addDiamonds(reward.second)
                "cosmetic_skin" -> {
                    unlockSkin("golden")
                }
            }
            claimedSeasonPremiumRewards = claimedSeasonPremiumRewards + level
            saveBattlePassState()
            return true
        }
        return false
    }

    // Cosmetics properties
    var unlockedSkins by mutableStateOf(setOf("default"))
        private set
    var selectedSkin by mutableStateOf("default")
        private set

    var unlockedThemes by mutableStateOf(setOf("classic"))
        private set
    var selectedTheme by mutableStateOf("classic")
        private set

    var unlockedBackgrounds by mutableStateOf(setOf("bg_slate"))
        private set
    var selectedBackground by mutableStateOf("bg_slate")
        private set

    var unlockedUiStyles by mutableStateOf(setOf("ui_classic"))
        private set
    var selectedUiStyle by mutableStateOf("ui_classic")
        private set

    var unlockedSounds by mutableStateOf(setOf("classic"))
        private set
    var selectedSound by mutableStateOf("classic")
        private set

    var unlockedParticles by mutableStateOf(setOf("classic"))
        private set
    var selectedParticle by mutableStateOf("classic")
        private set

    // Procedural Level and Daily Streak States
    var isProceduralLevel by mutableStateOf(false)
        private set
    var arenaDifficulty by mutableStateOf("easy")
        private set

    var dailyStreak by mutableStateOf(1)
        private set
    var lastDailyStreakClaimDate by mutableStateOf("")
        private set

    fun selectSound(soundId: String) {
        if (unlockedSounds.contains(soundId)) {
            selectedSound = soundId
            saveCosmeticsState()
        }
    }

    fun selectParticle(particleId: String) {
        if (unlockedParticles.contains(particleId)) {
            selectedParticle = particleId
            saveCosmeticsState()
        }
    }

    fun restoreDefaultStyle() {
        selectedTheme = "classic"
        selectedSkin = "default"
        selectedSound = "classic"
        selectedParticle = "classic"
        selectedBackground = "bg_slate"
        selectedUiStyle = "ui_classic"
        saveCosmeticsState()
    }

    fun startProceduralArena(difficulty: String, levelIdx: Int) {
        isProceduralLevel = true
        arenaDifficulty = difficulty
        startPlayingLevel(levelIdx)
    }

    fun checkAndResetProceduralState() {
        isProceduralLevel = false
    }

    fun claimDailyStreak() {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        if (lastDailyStreakClaimDate == todayStr) return

        // Calculate reward based on streak day
        val multiplier = dailyStreak
        addCoins(25 * multiplier)
        addDiamonds(1 * multiplier)

        lastDailyStreakClaimDate = todayStr
        dailyStreak = if (dailyStreak >= 7) 1 else dailyStreak + 1

        sp.edit()
            .putInt("daily_streak", dailyStreak)
            .putString("last_daily_streak_claim_date", lastDailyStreakClaimDate)
            .apply()
    }

    fun unlockSkin(skinId: String) {
        unlockedSkins = unlockedSkins + skinId
        saveCosmeticsState()
        syncUnlockAchievements()
    }

    fun selectSkin(skinId: String) {
        if (unlockedSkins.contains(skinId)) {
            selectedSkin = skinId
            saveCosmeticsState()
        }
    }

    fun unlockTheme(themeId: String) {
        unlockedThemes = unlockedThemes + themeId
        saveCosmeticsState()
        syncUnlockAchievements()
    }

    fun selectTheme(themeId: String) {
        if (unlockedThemes.contains(themeId)) {
            selectedTheme = themeId
            saveCosmeticsState()
        }
    }

    fun unlockBackground(bgId: String) {
        unlockedBackgrounds = unlockedBackgrounds + bgId
        saveCosmeticsState()
        syncUnlockAchievements()
    }

    fun selectBackground(bgId: String) {
        if (unlockedBackgrounds.contains(bgId)) {
            selectedBackground = bgId
            saveCosmeticsState()
        }
    }

    fun unlockUiStyle(styleId: String) {
        unlockedUiStyles = unlockedUiStyles + styleId
        saveCosmeticsState()
        syncUnlockAchievements()
    }

    fun selectUiStyle(styleId: String) {
        if (unlockedUiStyles.contains(styleId)) {
            selectedUiStyle = styleId
            saveCosmeticsState()
        }
    }

    fun unlockSound(soundId: String) {
        unlockedSounds = unlockedSounds + soundId
        saveCosmeticsState()
        syncUnlockAchievements()
    }

    fun unlockParticle(particleId: String) {
        unlockedParticles = unlockedParticles + particleId
        saveCosmeticsState()
        syncUnlockAchievements()
    }

    // Purchase shop item using resources
    fun purchaseShopItem(itemId: String, cost: Int, currency: String, type: String, value: String): Boolean {
        if (currency == "coins") {
            if (coins >= cost) {
                coins -= cost
                saveResourceState()
                
                when (type) {
                    "skin" -> unlockSkin(value)
                    "theme" -> unlockTheme(value)
                    "background" -> unlockBackground(value)
                    "uistyle" -> unlockUiStyle(value)
                    "sound" -> unlockSound(value)
                    "particle" -> unlockParticle(value)
                }
                
                trackCoinsSpent(cost)
                GameAnalytics.trackCoinsSpent(cost, "shop_purchase_$itemId")
                return true
            }
        } else if (currency == "gems" || currency == "diamonds") {
            if (diamonds >= cost) {
                diamonds -= cost
                saveResourceState()
                
                when (type) {
                    "skin" -> unlockSkin(value)
                    "theme" -> unlockTheme(value)
                    "background" -> unlockBackground(value)
                    "uistyle" -> unlockUiStyle(value)
                    "sound" -> unlockSound(value)
                    "particle" -> unlockParticle(value)
                }
                return true
            }
        }
        return false
    }

    // Saving helpers
    fun saveResourceState() {
        sp.edit()
            .putInt("coins", coins)
            .putInt("diamonds", diamonds)
            .apply()
    }

    fun saveBattlePassState() {
        sp.edit()
            .putInt("season_xp", seasonXp)
            .putInt("season_level", seasonLevel)
            .putBoolean("has_premium_pass", hasPremiumPass)
            .putString("claimed_season_rewards", claimedSeasonRewards.joinToString(","))
            .putString("putString", claimedSeasonPremiumRewards.joinToString(","))
            .apply()
    }

    fun saveCosmeticsState() {
        sp.edit()
            .putString("selected_skin", selectedSkin)
            .putString("unlocked_skins", unlockedSkins.joinToString(","))
            .putString("selected_theme", selectedTheme)
            .putString("unlocked_themes", unlockedThemes.joinToString(","))
            .putString("selected_background", selectedBackground)
            .putString("unlocked_backgrounds", unlockedBackgrounds.joinToString(","))
            .putString("selected_ui_style", selectedUiStyle)
            .putString("unlocked_ui_styles", unlockedUiStyles.joinToString(","))
            .putString("selected_sound", selectedSound)
            .putString("unlocked_sounds", unlockedSounds.joinToString(","))
            .putString("selected_particle", selectedParticle)
            .putString("unlocked_particles", unlockedParticles.joinToString(","))
            .apply()
    }

    // Sound adjustments
    var isMuted by mutableStateOf(false)
    var volume by mutableStateOf(0.8f)

    // Language settings: false = RU (default), true = EN
    var isEnglish by mutableStateOf(false)

    // Dialog state controllers
    var showSettingsDialog by mutableStateOf(false)
    var showGiftsDialog by mutableStateOf(false)
    var showQuestsDialog by mutableStateOf(false)
    var activeQuestTab by mutableStateOf(0) // 0 = Daily, 1 = Weekly, 2 = Achievements
    var showShopSoonDialog by mutableStateOf(false)
    var showHintChoiceDialog by mutableStateOf(false)
    var showSkipChoiceDialog by mutableStateOf(false)

    // Notification badge indicator state flows
    val hasUnclaimedDailyReward = MutableStateFlow(false)
    val hasUnclaimedWeeklyReward = MutableStateFlow(false)
    val hasUnclaimedAchievementReward = MutableStateFlow(false)

    // Quests & Achievements StateFlows
    val questStates: StateFlow<List<QuestState>> = db.questStateDao().getAllQuests()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val achievementStates: StateFlow<List<AchievementState>> = db.achievementStateDao().getAllAchievements()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Purchase mechanics utilizing coins
    fun buyHint(): Boolean {
        if (coins >= 20 && !isLevelCleared && !isGameOver) {
            coins -= 20
            trackCoinsSpent(20)
            GameAnalytics.trackCoinsSpent(20, "buy_hint")
            useHintForAd("coins") // highlights hint
            return true
        }
        return false
    }

    fun buySkip(): Boolean {
        if (coins >= 50 && !isLevelCleared) {
            coins -= 50
            trackCoinsSpent(50)
            GameAnalytics.trackCoinsSpent(50, "buy_skip")
            skipLevelForAd()
            return true
        }
        return false
    }

    fun buyLivesRestore(): Boolean {
        if (coins >= 30 && isGameOver) {
            coins -= 30
            trackCoinsSpent(30)
            GameAnalytics.trackCoinsSpent(30, "restore_lives")
            lives = maxLives
            isGameOver = false
            return true
        }
        return false
    }

    var heartRestoresBoughtWithCoinsThisLevel by mutableStateOf(0)

    fun buyOneHeartWithCoins(): Boolean {
        if (coins >= 100 && heartRestoresBoughtWithCoinsThisLevel < 2 && isGameOver) {
            coins -= 100
            trackCoinsSpent(100)
            GameAnalytics.trackCoinsSpent(100, "restore_one_heart_coins")
            heartRestoresBoughtWithCoinsThisLevel++
            lives = 1
            isGameOver = false
            return true
        }
        return false
    }

    // Yandex Ads trigger states and counters
    var levelsCompletedThisSession by mutableStateOf(0)
    var triggerInterstitialAdShow by mutableStateOf(false)
    var triggerRewardedAdSkip by mutableStateOf(false)
    var triggerRewardedAdHint by mutableStateOf(false)
    var triggerRewardedAdRestoreHeart by mutableStateOf(false)

    fun requestSkipLevelForAd() {
        triggerRewardedAdSkip = true
    }

    fun requestHintForAd() {
        triggerRewardedAdHint = true
    }

    fun requestRestoreHeartForAd() {
        triggerRewardedAdRestoreHeart = true
    }

    fun restoreHeartForAd() {
        lives = 1
        isGameOver = false
    }

    // Reset all game data (coins, diamonds, open levels)
    fun resetAllGameProgress() {
        coins = 150
        diamonds = 15
        isEnglish = false
        isMuted = false
        volume = 0.8f
        _activeLevelIndex.value = 1
        viewModelScope.launch {
            // Lock all levels besides 1 in a single batch insert for top performance
            val levels = mutableListOf<LevelState>()
            levels.add(LevelState(1, isCompleted = false, isUnlocked = true))
            for (i in 2..1000) {
                levels.add(LevelState(i, isCompleted = false, isUnlocked = false))
            }
            repository.saveLevelStates(levels)
            loadLevel(1)
            exitToMainMenu()
        }
    }

    // Watching / claiming reward helper
    fun claimGiftRewardVideo() {
        coins += 100
        diamonds += 5
    }

    // Lives/Hearts mechanics
    var lives by mutableStateOf(5)
        private set
    var maxLives by mutableStateOf(5)
        private set
    var isGameOver by mutableStateOf(false)
        private set

    // Hints/Guides showing
    var showGuides by mutableStateOf(false)
        private set

    var highlightedHintArrowId by mutableStateOf<Int?>(null)
        private set

    fun toggleGuides() {
        showGuides = !showGuides
    }

    // Reactive state for Undo mechanism in Jetpack Compose
    var canUndo by mutableStateOf(false)
        private set

    // Counter to trigger re-centering camera when level restarts
    var restartCounter by mutableStateOf(0)
        private set

    // History for beautiful, infinite undos
    private val undoHistory = mutableListOf<List<Arrow>>()

    // Interactive Camera State (Zoom/Pan persistent)
    var cameraScale by mutableStateOf(1.0f)
    var cameraOffset by mutableStateOf(Offset.Zero)

    fun centerAndFitBoard(
        viewW: Float,
        viewH: Float,
        gridWidth: Int,
        gridHeight: Int,
        density: Float
    ) {
        if (viewW <= 0f || viewH <= 0f || arrows.isEmpty()) return
        
        val allPoints = arrows.flatMap { it.path }
        val minX = allPoints.minOfOrNull { it.x } ?: 0
        val maxX = allPoints.maxOfOrNull { it.x } ?: (gridWidth - 1)
        val minY = allPoints.minOfOrNull { it.y } ?: 0
        val maxY = allPoints.maxOfOrNull { it.y } ?: (gridHeight - 1)
        
        val figureWCells = maxX - minX + 1
        val figureHCells = maxY - minY + 1
        
        val cellSizePx = 36f * density // 36.dp in pixels
        
        // Add a padding of 1.25 cells around the figure for a spacious, beautifully centered presentation
        val paddingCells = 1.25f
        val figureWidthPx = (figureWCells + paddingCells * 2) * cellSizePx
        val figureHeightPx = (figureHCells + paddingCells * 2) * cellSizePx
        
        val scaleX = viewW / figureWidthPx
        val scaleY = viewH / figureHeightPx
        val idealScale = minOf(scaleX, scaleY).coerceIn(0.4f, 3.5f)
        
        val centerXCells = (minX + maxX + 1) / 2.0f
        val centerYCells = (minY + maxY + 1) / 2.0f
        
        val centerXNoScalePx = centerXCells * cellSizePx
        val centerYNoScalePx = centerYCells * cellSizePx
        
        val idealOffset = Offset(
            viewW / 2f - centerXNoScalePx * idealScale,
            viewH / 2f - centerYNoScalePx * idealScale
        )
        
        cameraScale = idealScale
        cameraOffset = idealOffset
    }

    init {
        // Load coins, diamonds, and settings from SharedPreferences
        coins = sp.getInt("coins", 150)
        diamonds = sp.getInt("diamonds", 15)
        isEnglish = sp.getBoolean("is_english", false)
        isMuted = sp.getBoolean("is_muted", false)
        volume = sp.getFloat("volume", 0.8f)

        // Load Battle Pass State
        seasonXp = sp.getInt("season_xp", 0)
        seasonLevel = sp.getInt("season_level", 1)
        hasPremiumPass = sp.getBoolean("has_premium_pass", false)
        claimedSeasonRewards = sp.getString("claimed_season_rewards", "")
            ?.split(",")?.filter { it.isNotEmpty() }?.map { it.toInt() }?.toSet() ?: emptySet()
        claimedSeasonPremiumRewards = sp.getString("claimed_season_premium_rewards", "")
            ?.split(",")?.filter { it.isNotEmpty() }?.map { it.toInt() }?.toSet() ?: emptySet()

        // Load Cosmetics State
        selectedSkin = sp.getString("selected_skin", "default") ?: "default"
        unlockedSkins = sp.getString("unlocked_skins", "default")
            ?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: setOf("default")

        selectedTheme = sp.getString("selected_theme", "classic") ?: "classic"
        unlockedThemes = sp.getString("unlocked_themes", "classic")
            ?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: setOf("classic")

        selectedBackground = sp.getString("selected_background", "bg_slate") ?: "bg_slate"
        unlockedBackgrounds = sp.getString("unlocked_backgrounds", "bg_slate")
            ?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: setOf("bg_slate")

        selectedUiStyle = sp.getString("selected_ui_style", "ui_classic") ?: "ui_classic"
        unlockedUiStyles = sp.getString("unlocked_ui_styles", "ui_classic")
            ?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: setOf("ui_classic")

        selectedSound = sp.getString("selected_sound", "classic") ?: "classic"
        unlockedSounds = sp.getString("unlocked_sounds", "classic")
            ?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: setOf("classic")

        selectedParticle = sp.getString("selected_particle", "classic") ?: "classic"
        unlockedParticles = sp.getString("unlocked_particles", "classic")
            ?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: setOf("classic")

        // Load Streak State
        dailyStreak = sp.getInt("daily_streak", 1)
        lastDailyStreakClaimDate = sp.getString("last_daily_streak_claim_date", "") ?: ""

        // Pre-populate levels database when empty
        viewModelScope.launch {
            val count = db.levelStateDao().getLevelState(1)
            if (count == null) {
                // Unlock level 1 by default and save in a batch insert for top performance
                val levels = mutableListOf<LevelState>()
                levels.add(LevelState(1, isCompleted = false, isUnlocked = true))
                for (i in 2..1000) {
                    levels.add(LevelState(i, isCompleted = false, isUnlocked = false))
                }
                repository.saveLevelStates(levels)
            }
        }

        // Pre-populate quests and achievements database with 1-to-1 Old-ArrowEsc content
        viewModelScope.launch {
            val dbVersion = sp.getInt("db_repopulation_version_v2", 0)
            if (dbVersion < 1) {
                // Clear old quests and achievements
                db.questStateDao().deleteAllQuests()
                db.achievementStateDao().deleteAllAchievements()
                
                val defaultQuests = listOf(
                    // EASY
                    QuestState("task_complete_5", "DAILY", "Complete 5 levels in normal mode", "Пройти 5 обычных уровней", 5, 0, 50, 1),
                    QuestState("task_earn_5_gems", "DAILY", "Earn 5 gems today", "Заработать 5 алмазов за день", 5, 0, 50, 1),
                    QuestState("task_use_combo_1", "DAILY", "Perform 1 combo chain", "Выполнить комбо-эффект", 1, 0, 40, 1),

                    // MEDIUM
                    QuestState("task_complete_10", "DAILY", "Complete 10 levels in normal mode", "Пройти 10 обычных уровней", 10, 0, 100, 2),
                    QuestState("task_no_heart_loss_5", "DAILY", "Win 5 levels without losing any hearts", "Пройти 5 уровней без потери сердечек", 5, 0, 120, 3),
                    QuestState("task_use_combo_5", "DAILY", "Perform 5 combo chains", "Выполнить 5 комбо-цепочек", 5, 0, 100, 2),
                    QuestState("task_earn_10_gems", "DAILY", "Earn 10 gems today", "Заработать 10 алмазов за день", 10, 0, 80, 2),

                    // HARD
                    QuestState("task_complete_15", "DAILY", "Complete 15 levels in normal mode", "Пройти 15 обычных уровней", 15, 0, 200, 5),
                    QuestState("task_perfect_10", "DAILY", "Finish 10 levels perfectly (0 mistakes)", "Завершить 10 уровней идеально", 10, 0, 240, 8),
                    QuestState("task_make_combo_10_arrow", "DAILY", "Perform a 10-arrow combo chain", "Выполнить комбо на 10+ стрелок", 10, 0, 200, 6),
                    QuestState("task_complete_hard_5", "DAILY", "Complete 5 Hard/Expert levels", "Пройти 5 сложных уровней (Hard/Expert)", 5, 0, 220, 6),
                    QuestState("task_earn_20_gems", "DAILY", "Earn 20 gems today", "Заработать 20 алмазов за день", 20, 0, 300, 5),

                    // VERY HARD
                    QuestState("task_expert_perfect_3", "DAILY", "Finish 3 Expert levels perfectly (0 mistakes)", "Завершить 3 экспертных уровня идеально", 3, 0, 300, 10),
                    QuestState("task_combo_chain_x5", "DAILY", "Execute a 5-arrow combo chain 5 times", "Выполнить комбо-эффекты на 5+ стрелок 5 раз", 5, 0, 240, 12),
                    QuestState("task_complete_25_levels", "DAILY", "Complete 25 total levels of any difficulty today", "Пройти 25 любых уровней за день", 25, 0, 400, 15),

                    // WEEKLY
                    QuestState("weekly_levels_100", "WEEKLY", "Endurance Master: Complete 100 puzzle levels", "Мастер выносливости: Пройти 100 уровней", 100, 0, 1000, 40),
                    QuestState("weekly_gems_50", "WEEKLY", "Diamond Collector: Earn 50 premium Gems", "Алмазный магнат: Собрать 50 алмазов за неделю", 50, 0, 600, 25),
                    QuestState("weekly_combos_50", "WEEKLY", "Combo Champion: Perform 50 combos of size x3 or greater", "Король комбо-цепей: Выполнить 50 комбо x3+", 50, 0, 400, 15),
                    QuestState("weekly_hard_20", "WEEKLY", "Apex Conqueror: Solve 20 Hard or Expert Arena challenges", "Покоритель вершин: Пройти 20 сложных уровней", 20, 0, 1500, 60)
                )
                db.questStateDao().insertOrUpdateAll(defaultQuests)

                val defaultAchievements = listOf(
                    AchievementState("ach_level_100", "Centenary Escape: Complete 100 total puzzle levels", "Век лабиринтов: Завершить 100 уровней", 100, 0, 300, 15),
                    AchievementState("ach_level_500", "Pentaclassical Runner: Complete 500 total puzzle levels", "Пятизвездочный беглец: Завершить 500 уровней", 500, 0, 1000, 75),
                    AchievementState("ach_level_1000", "Primal Maze Deity: Complete 1,000 total levels", "Истинное Божество лабиринта: Завершить 1000 уровней", 1000, 0, 2000, 180),
                    AchievementState("ach_level_5000", "Absolute Legend: Complete 5,000 total levels", "Абсолютная Легенда Времени: Завершить 5000 уровней", 5000, 0, 10000, 1000),
                    AchievementState("ach_gems_100", "Spark Of Fortune: Earn 100 total lifetime premium gems", "Искра Сверхприбыли: Собрать суммарно 100 алмазов", 100, 0, 400, 20),
                    AchievementState("ach_gems_1000", "Treasurer Of Infinity: Earn 1,000 total lifetime premium gems", "Алмазный Казначей: Собрать суммарно 1000 алмазов", 1000, 0, 2400, 150),
                    AchievementState("ach_all_themes", "Sovereign Architect: Unlock all 20 premium themes", "Архитектор Эпох: Разблокировать все 20 тем", 20, 0, 1600, 50),
                    AchievementState("ach_all_sounds", "Soundscape Maestro: Unlock all 15 audio packs", "Маэстро Звукового Пейзажа: Разблокировать все 15 звуков", 15, 0, 1200, 40),
                    AchievementState("ach_all_collections", "Unrivaled Museum Master: Unlock all 90 items", "Легендарный Хранитель Музея: Разблокировать все 90 предметов", 90, 0, 20000, 500)
                )
                db.achievementStateDao().insertOrUpdateAll(defaultAchievements)
                
                sp.edit().putInt("db_repopulation_version_v2", 1).apply()
            }
        }

        // Observe and update notification badges
        viewModelScope.launch {
            db.questStateDao().getAllQuests().collect { quests ->
                hasUnclaimedDailyReward.value = quests.any { it.type == "DAILY" && it.isCompleted && !it.isClaimed }
                hasUnclaimedWeeklyReward.value = quests.any { it.type == "WEEKLY" && it.isCompleted && !it.isClaimed }
            }
        }
        viewModelScope.launch {
            db.achievementStateDao().getAllAchievements().collect { achs ->
                hasUnclaimedAchievementReward.value = achs.any { it.isCompleted && !it.isClaimed }
            }
        }

        // Handle auto-reset for daily and weekly quests based on Calendar
        viewModelScope.launch {
            val sp = getApplication<Application>().getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
            val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val currentWeek = java.util.Calendar.getInstance().get(java.util.Calendar.WEEK_OF_YEAR)

            val savedDay = sp.getInt("last_day", -1)
            val savedYear = sp.getInt("last_year", -1)
            val savedWeek = sp.getInt("last_week", -1)

            if (savedDay != -1 && (currentDay != savedDay || currentYear != savedYear)) {
                resetDailyQuests()
            }
            if (savedWeek != -1 && (currentWeek != savedWeek || currentYear != savedYear)) {
                resetWeeklyQuests()
            }

            sp.edit()
                .putInt("last_day", currentDay)
                .putInt("last_year", currentYear)
                .putInt("last_week", currentWeek)
                .apply()
        }
    }

    fun startPlayingLevel(levelIndex: Int) {
        val index = levelIndex.coerceIn(1, 1000)
        _activeLevelIndex.value = index
        loadLevel(index)
        _currentScreen.value = ScreenType.PLAYING
        _isPlaying.value = true
        GameAnalytics.trackLevelStart(index)
    }

    fun exitToLevelSelect() {
        _currentScreen.value = ScreenType.LEVEL_SELECT
        _isPlaying.value = false
    }

    fun showLevelSelect() {
        _currentScreen.value = ScreenType.LEVEL_SELECT
        _isPlaying.value = false
    }

    fun exitToMainMenu() {
        _currentScreen.value = ScreenType.MAIN_MENU
        _isPlaying.value = false
    }

    fun unlockAllLevels() {
        viewModelScope.launch {
            repository.unlockAllLevels()
        }
    }

    fun loadLevel(levelIndex: Int) {
        _activeLevelIndex.value = levelIndex
        isLoadingLevel = true
        
        // Clear previous level state instantly to avoid layout flash
        levelData = null
        arrows = emptyList()
        movesCount = 0
        isLevelCleared = false
        isWinningTransition = false
        undoHistory.clear()
        canUndo = false
        
        viewModelScope.launch {
            // Execute heavy procedural generation asynchronously on the CPU dispatcher
            val data = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
                LevelGenerator.generateLevel(levelIndex)
            }
            
            levelData = data
            arrows = data.arrows
            restartCounter++
            
            // Reset camera when changing levels
            cameraScale = 1.0f
            cameraOffset = Offset.Zero

            // Setup hearts based on level difficulty
            maxLives = when {
                levelIndex <= 10 -> 5
                levelIndex <= 40 -> 4
                levelIndex <= 100 -> 3
                else -> 2
            }
            lives = maxLives
            heartRestoresBoughtWithCoinsThisLevel = 0
            isGameOver = false
            showGuides = false
            highlightedHintArrowId = null
            
            isLoadingLevel = false
        }
    }

    fun nextLevel() {
        if (_activeLevelIndex.value < 1000) {
            if (levelsCompletedThisSession > 0 && levelsCompletedThisSession % 5 == 0) {
                triggerInterstitialAdShow = true
            } else {
                startPlayingLevel(_activeLevelIndex.value + 1)
            }
        }
    }

    fun proceedToNextLevelAfterAd() {
        triggerInterstitialAdShow = false
        if (_activeLevelIndex.value < 1000) {
            startPlayingLevel(_activeLevelIndex.value + 1)
        }
    }

    fun previousLevel() {
        if (_activeLevelIndex.value > 1) {
            startPlayingLevel(_activeLevelIndex.value - 1)
        }
    }

    fun restartLevel() {
        loadLevel(_activeLevelIndex.value)
        trackLevelRestarted()
        GameAnalytics.trackLevelRestart(_activeLevelIndex.value)
    }

    fun undo() {
        if (undoHistory.isNotEmpty() && !isLevelCleared) {
            val previousState = undoHistory.removeAt(undoHistory.size - 1)
            arrows = previousState
            movesCount = (movesCount - 1).coerceAtLeast(0)
            canUndo = undoHistory.isNotEmpty()
            highlightedHintArrowId = null
        }
    }

    fun canUndo(): Boolean {
        return canUndo && !isLevelCleared
    }

    fun tapArrow(arrowId: Int) {
        if (isLevelCleared || isGameOver) return
        val arrow = arrows.find { it.id == arrowId } ?: return
        if (arrow.state != ArrowState.IDLE) return

        highlightedHintArrowId = null // Turn off green highlight on tap

        val activeArrows = arrows.filter { it.state == ArrowState.IDLE || it.state == ArrowState.WIGGLING }
        val w = levelData?.gridWidth ?: 10
        val h = levelData?.gridHeight ?: 10
        val isBlocked = isArrowBlockedStatic(arrow, activeArrows, w, h)

        if (isBlocked) {
            // Mark the arrow as collided (turns RED on screen)
            setArrowCollided(arrowId, true)

            // Deduct a heart!
            if (lives > 0 && !isLevelCleared && !isGameOver) {
                lives--
                if (lives == 0) {
                    isGameOver = true
                }
            }

            // Crash and return animation: progress goes smoothly from 0 to 1
            viewModelScope.launch {
                var time = 0L
                val duration = 300L // Perfectly snappy 300ms crash-and-return sequence
                while (time < duration) {
                    val progress = (time.toFloat() / duration).coerceIn(0f, 1f)
                    updateArrowAnim(arrowId, ArrowState.WIGGLING, progress)
                    delay(16)
                    time += 16
                }
                updateArrowAnim(arrowId, ArrowState.IDLE, 0f)
                setArrowCollided(arrowId, false)
            }
        } else {
            // Save state for undo before modifying!
            saveToUndoHistory()

            // Smooth exit sliding coroutine
            viewModelScope.launch {
                movesCount++
                // Transition instantly to EXITING and recheck collisions so paths of other arrows are unblocked immediately!
                updateArrowAnim(arrowId, ArrowState.EXITING, 0f)
                recheckCollisions()

                var time = 0L
                val duration = 650L // Slower and smoother arrow slide-off
                while (time < duration) {
                    val progress = (time.toFloat() / duration).coerceIn(0f, 1f)
                    updateArrowAnim(arrowId, ArrowState.EXITING, progress)
                    delay(16)
                    time += 16
                }
                updateArrowAnim(arrowId, ArrowState.EXITED, 1.0f)
                recheckCollisions()
                checkWinCondition()
            }
        }
    }

    private fun setArrowCollided(arrowId: Int, isCollided: Boolean) {
        arrows = arrows.map {
            if (it.id == arrowId) {
                it.copy(isCollided = isCollided)
            } else {
                it
            }
        }
    }

    private fun recheckCollisions() {
        val activeArrows = arrows.filter { it.state == ArrowState.IDLE || it.state == ArrowState.WIGGLING }
        val w = levelData?.gridWidth ?: 10
        val h = levelData?.gridHeight ?: 10

        arrows = arrows.map { arrow ->
            if (arrow.isCollided) {
                val stillBlocked = isArrowBlockedStatic(arrow, activeArrows, w, h)
                arrow.copy(isCollided = stillBlocked)
            } else {
                arrow
            }
        }
    }

    private fun saveToUndoHistory() {
        // Deep copy of arrows state to avoid pointer reference mutations
        val snapshot = arrows.map { it.copy() }
        undoHistory.add(snapshot)
        canUndo = true
    }

    private fun updateArrowAnim(arrowId: Int, newState: ArrowState, progress: Float) {
        arrows = arrows.map {
            if (it.id == arrowId) {
                it.copy(state = newState, slideAnimProgress = progress)
            } else {
                it
            }
        }
    }

    private suspend fun checkWinCondition() {
        val remaining = arrows.count { it.state != ArrowState.EXITED }
        if (remaining == 0) {
            isWinningTransition = true
            levelsCompletedThisSession++
            // Reward player for completing the level
            addCoins(15)
            addDiamonds(2)
            // Persist completion and unlock the next level
            repository.completeAndUnlockNext(_activeLevelIndex.value, movesCount)
            
            // Track quests & achievements!
            val flawless = (lives == maxLives)
            trackLevelCompleted(noMistakes = flawless)
            GameAnalytics.trackLevelWin(_activeLevelIndex.value, movesCount, flawless)
        }
    }

    fun finalizeWin() {
        isWinningTransition = false
        isLevelCleared = true
    }

    fun skipLevelForAd() {
        isLevelCleared = true
        addCoins(15)
        addDiamonds(2)
        viewModelScope.launch {
            repository.completeAndUnlockNext(_activeLevelIndex.value, movesCount)
            trackLevelCompleted(noMistakes = false)
            GameAnalytics.trackLevelSkip(_activeLevelIndex.value)
        }
    }

    fun useHintForAd(type: String = "ad") {
        val activeArrows = arrows.filter { it.state == ArrowState.IDLE || it.state == ArrowState.WIGGLING }
        val w = levelData?.gridWidth ?: 10
        val h = levelData?.gridHeight ?: 10
        
        // Find first arrow that is not blocked
        val solvableArrow = activeArrows.firstOrNull { arrow ->
            !isArrowBlockedStatic(arrow, activeArrows, w, h)
        }
        
        if (solvableArrow != null) {
            highlightedHintArrowId = solvableArrow.id
            trackHintUsed()
            GameAnalytics.trackHintUsed(_activeLevelIndex.value, type)
        }
    }

    // Static blocker checker running within the active level context
    private fun isArrowBlockedStatic(arrow: Arrow, activeArrows: List<Arrow>, w: Int, h: Int): Boolean {
        val exitDir = arrow.exitDirection
        val head = arrow.head
        
        var currX = head.x + exitDir.x
        var currY = head.y + exitDir.y
        
        while (currX in 0 until w && currY in 0 until h) {
            val checkPoint = Point(currX, currY)
            for (other in activeArrows) {
                if (other.occupies(checkPoint)) {
                    return true
                }
            }
            currX += exitDir.x
            currY += exitDir.y
        }
        return false
    }

    // QUEST & ACHIEVEMENT PROGRESS TRACKERS
    private suspend fun resetDailyQuests() {
        db.questStateDao().deleteAllQuests()
        val defaultQuests = listOf(
            // EASY
            QuestState("task_complete_5", "DAILY", "Complete 5 levels in normal mode", "Пройти 5 обычных уровней", 5, 0, 50, 1),
            QuestState("task_earn_5_gems", "DAILY", "Earn 5 gems today", "Заработать 5 алмазов за день", 5, 0, 50, 1),
            QuestState("task_use_combo_1", "DAILY", "Perform 1 combo chain", "Выполнить комбо-эффект", 1, 0, 40, 1),

            // MEDIUM
            QuestState("task_complete_10", "DAILY", "Complete 10 levels in normal mode", "Пройти 10 обычных уровней", 10, 0, 100, 2),
            QuestState("task_no_heart_loss_5", "DAILY", "Win 5 levels without losing any hearts", "Пройти 5 уровней без потери сердечек", 5, 0, 120, 3),
            QuestState("task_use_combo_5", "DAILY", "Perform 5 combo chains", "Выполнить 5 комбо-цепочек", 5, 0, 100, 2),
            QuestState("task_earn_10_gems", "DAILY", "Earn 10 gems today", "Заработать 10 алмазов за день", 10, 0, 80, 2),

            // HARD
            QuestState("task_complete_15", "DAILY", "Complete 15 levels in normal mode", "Пройти 15 обычных уровней", 15, 0, 200, 5),
            QuestState("task_perfect_10", "DAILY", "Finish 10 levels perfectly (0 mistakes)", "Завершить 10 уровней идеально", 10, 0, 240, 8),
            QuestState("task_make_combo_10_arrow", "DAILY", "Perform a 10-arrow combo chain", "Выполнить комбо на 10+ стрелок", 10, 0, 200, 6),
            QuestState("task_complete_hard_5", "DAILY", "Complete 5 Hard/Expert levels", "Пройти 5 сложных уровней (Hard/Expert)", 5, 0, 220, 6),
            QuestState("task_earn_20_gems", "DAILY", "Earn 20 gems today", "Заработать 20 алмазов за день", 20, 0, 300, 5),

            // VERY HARD
            QuestState("task_expert_perfect_3", "DAILY", "Finish 3 Expert levels perfectly (0 mistakes)", "Завершить 3 экспертных уровня идеально", 3, 0, 300, 10),
            QuestState("task_combo_chain_x5", "DAILY", "Execute a 5-arrow combo chain 5 times", "Выполнить комбо-эффекты на 5+ стрелок 5 раз", 5, 0, 240, 12),
            QuestState("task_complete_25_levels", "DAILY", "Complete 25 total levels of any difficulty today", "Пройти 25 любых уровней за день", 25, 0, 400, 15),

            // WEEKLY
            QuestState("weekly_levels_100", "WEEKLY", "Endurance Master: Complete 100 puzzle levels", "Мастер выносливости: Пройти 100 уровней", 100, 0, 1000, 40),
            QuestState("weekly_gems_50", "WEEKLY", "Diamond Collector: Earn 50 premium Gems", "Алмазный магнат: Собрать 50 алмазов за неделю", 50, 0, 600, 25),
            QuestState("weekly_combos_50", "WEEKLY", "Combo Champion: Perform 50 combos of size x3 or greater", "Король комбо-цепей: Выполнить 50 комбо x3+", 50, 0, 400, 15),
            QuestState("weekly_hard_20", "WEEKLY", "Apex Conqueror: Solve 20 Hard or Expert Arena challenges", "Покоритель вершин: Пройти 20 сложных уровней", 20, 0, 1500, 60)
        )
        db.questStateDao().insertOrUpdateAll(defaultQuests)
    }

    private suspend fun resetWeeklyQuests() {
        val quests = db.questStateDao().getAllQuests().stateIn(viewModelScope).value
        val updated = quests.map {
            if (it.type == "WEEKLY") {
                it.copy(current = 0, isCompleted = false, isClaimed = false)
            } else {
                it
            }
        }
        if (updated.isNotEmpty()) {
            db.questStateDao().insertOrUpdateAll(updated)
        }
    }

    fun trackLevelCompleted(noMistakes: Boolean) {
        viewModelScope.launch {
            updateQuestProgressByType("task_complete_5", 1)
            updateQuestProgressByType("task_complete_10", 1)
            updateQuestProgressByType("task_complete_15", 1)
            updateQuestProgressByType("task_complete_25_levels", 1)
            updateQuestProgressByType("weekly_levels_100", 1)
            if (noMistakes) {
                updateQuestProgressByType("task_perfect_10", 1)
            }

            updateAchievementProgressByType("ach_level_100", 1)
            updateAchievementProgressByType("ach_level_500", 1)
            updateAchievementProgressByType("ach_level_1000", 1)
            updateAchievementProgressByType("ach_level_5000", 1)
            
            syncCoinsAchievement()
            syncUnlockAchievements()
        }
    }

    fun trackCoinsSpent(amount: Int) {
        viewModelScope.launch {
            // Coins spent trackers can be added here if needed
            syncUnlockAchievements()
        }
    }

    fun trackHintUsed() {
        viewModelScope.launch {
            // Hint used trackers can be added here if needed
        }
    }

    fun trackLevelRestarted() {
        viewModelScope.launch {
            // Restart level trackers can be added here if needed
        }
    }

    fun syncUnlockAchievements() {
        viewModelScope.launch {
            val totalThemes = unlockedThemes.size
            val themesAch = db.achievementStateDao().getAchievement("ach_all_themes")
            if (themesAch != null && !themesAch.isCompleted) {
                db.achievementStateDao().insertOrUpdate(
                    themesAch.copy(
                        current = totalThemes.coerceAtMost(themesAch.target),
                        isCompleted = totalThemes >= themesAch.target
                    )
                )
            }

            val totalSounds = unlockedSounds.size
            val soundsAch = db.achievementStateDao().getAchievement("ach_all_sounds")
            if (soundsAch != null && !soundsAch.isCompleted) {
                db.achievementStateDao().insertOrUpdate(
                    soundsAch.copy(
                        current = totalSounds.coerceAtMost(soundsAch.target),
                        isCompleted = totalSounds >= soundsAch.target
                    )
                )
            }

            val totalItems = unlockedSkins.size + unlockedThemes.size + unlockedBackgrounds.size + unlockedUiStyles.size + unlockedSounds.size + unlockedParticles.size
            val itemsAch = db.achievementStateDao().getAchievement("ach_all_collections")
            if (itemsAch != null && !itemsAch.isCompleted) {
                db.achievementStateDao().insertOrUpdate(
                    itemsAch.copy(
                        current = totalItems.coerceAtMost(itemsAch.target),
                        isCompleted = totalItems >= itemsAch.target
                    )
                )
            }
        }
    }

    private suspend fun updateQuestProgressByType(id: String, amount: Int) {
        val quest = db.questStateDao().getQuest(id) ?: return
        if (quest.isCompleted) return
        val newProgress = (quest.current + amount).coerceAtMost(quest.target)
        val isCompletedNow = newProgress >= quest.target
        db.questStateDao().insertOrUpdate(
            quest.copy(
                current = newProgress,
                isCompleted = isCompletedNow
            )
        )
    }

    private suspend fun updateAchievementProgressByType(id: String, amount: Int) {
        val ach = db.achievementStateDao().getAchievement(id) ?: return
        if (ach.isCompleted) return
        val newProgress = (ach.current + amount).coerceAtMost(ach.target)
        val isCompletedNow = newProgress >= ach.target
        db.achievementStateDao().insertOrUpdate(
            ach.copy(
                current = newProgress,
                isCompleted = isCompletedNow
            )
        )
    }

    private suspend fun syncCoinsAchievement() {
        val ach = db.achievementStateDao().getAchievement("ach_total_coins_1000") ?: return
        if (ach.isCompleted) return
        val currentCoins = coins
        val isCompletedNow = currentCoins >= ach.target
        db.achievementStateDao().insertOrUpdate(
            ach.copy(
                current = currentCoins.coerceAtMost(ach.target),
                isCompleted = isCompletedNow
            )
        )
    }

    fun claimQuestReward(questId: String) {
        viewModelScope.launch {
            val quest = db.questStateDao().getQuest(questId) ?: return@launch
            if (quest.isCompleted && !quest.isClaimed) {
                db.questStateDao().insertOrUpdate(quest.copy(isClaimed = true))
                addCoins(quest.rewardCoins)
                addDiamonds(quest.rewardDiamonds)
                val xpAward = if (quest.type == "DAILY") 15 else 40
                addSeasonXp(xpAward)
                syncCoinsAchievement()
            }
        }
    }

    fun claimAchievementReward(achId: String) {
        viewModelScope.launch {
            val ach = db.achievementStateDao().getAchievement(achId) ?: return@launch
            if (ach.isCompleted && !ach.isClaimed) {
                db.achievementStateDao().insertOrUpdate(ach.copy(isClaimed = true))
                addCoins(ach.rewardCoins)
                addDiamonds(ach.rewardDiamonds)
                addSeasonXp(30)
                syncCoinsAchievement()
            }
        }
    }
}
