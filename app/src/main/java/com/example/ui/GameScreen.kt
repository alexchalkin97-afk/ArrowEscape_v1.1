package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.LevelState
import com.example.game.*
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Star
import com.yandex.mobile.ads.banner.BannerAdView
import com.yandex.mobile.ads.banner.BannerAdSize
import com.yandex.mobile.ads.banner.BannerAdEventListener
import android.util.Log
import com.yandex.mobile.ads.common.AdRequest
import com.yandex.mobile.ads.common.AdRequestConfiguration
import com.yandex.mobile.ads.common.AdError
import com.yandex.mobile.ads.common.ImpressionData
import com.yandex.mobile.ads.rewarded.Reward
import com.yandex.mobile.ads.interstitial.InterstitialAd
import com.yandex.mobile.ads.interstitial.InterstitialAdEventListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoadListener
import com.yandex.mobile.ads.interstitial.InterstitialAdLoader
import com.yandex.mobile.ads.rewarded.RewardedAd
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoadListener
import com.yandex.mobile.ads.rewarded.RewardedAdLoader
import com.yandex.mobile.ads.common.AdRequestError

@Composable
fun GameScreen(viewModel: GameViewModel, modifier: Modifier = Modifier) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeLevelIndex by viewModel.activeLevelIndex.collectAsStateWithLifecycle()
    val levelStates by viewModel.levelStates.collectAsStateWithLifecycle()

    val appBackgroundColor = when (viewModel.selectedBackground) {
        "bg_slate" -> Color(0xFF0F1113)
        "bg_nebula" -> Color(0xFF0D0E1C)
        "bg_digits" -> Color(0xFF070F07)
        "bg_grid" -> Color(0xFF0A0E1A)
        "bg_meadow" -> Color(0xFF1C1510)
        else -> Color(0xFF0F1113)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appBackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 58.dp) // Reserve a fixed bottom height for the floating banner so it doesn't shrink/overlap game elements
        ) {
            // Persistent Top Header Bar visible everywhere
            HeaderBar(viewModel = viewModel)

            val isDialogShowing = viewModel.showSettingsDialog || viewModel.showGiftsDialog

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(if (isDialogShowing) 12.dp else 0.dp)
                ) {
                    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
                        when (screen) {
                            ScreenType.MAIN_MENU -> {
                                MainMenuScreen(
                                    viewModel = viewModel,
                                    levelStates = levelStates,
                                    isEnglish = viewModel.isEnglish,
                                    onPlayClicked = {
                                        val activeLevel = levelStates.firstOrNull { !it.isCompleted && it.isUnlocked }?.levelId
                                            ?: levelStates.filter { it.isUnlocked }.maxOfOrNull { it.levelId }
                                            ?: 1
                                        viewModel.startPlayingLevel(activeLevel)
                                    },
                                    onLevelSelectClicked = {
                                        viewModel.showLevelSelect()
                                    }
                                )
                            }
                            ScreenType.LEVEL_SELECT -> {
                                LevelSelectScreen(
                                    levelStates = levelStates,
                                    activeLevelIndex = activeLevelIndex,
                                    onLevelSelected = { idx -> viewModel.startPlayingLevel(idx) },
                                    onUnlockAllLevels = { viewModel.unlockAllLevels() },
                                    onBackToMenu = { viewModel.exitToMainMenu() },
                                    isEnglish = viewModel.isEnglish
                                )
                            }
                            ScreenType.PLAYING -> {
                                PlayScreen(viewModel = viewModel)
                            }
                            ScreenType.BATTLE_PASS -> {
                                BattlePassScreen(viewModel = viewModel)
                            }
                            ScreenType.ACHIEVEMENTS -> {
                                AchievementsScreen(viewModel = viewModel)
                            }
                            ScreenType.DAILY_QUESTS -> {
                                DailyQuestsScreen(viewModel = viewModel)
                            }
                            ScreenType.WEEKLY_QUESTS -> {
                                WeeklyQuestsScreen(viewModel = viewModel)
                            }
                            ScreenType.SHOP -> {
                                ShopScreen(viewModel = viewModel)
                            }
                            ScreenType.REWARD_CENTER -> {
                                RewardCenterScreen(viewModel = viewModel)
                            }
                        }
                    }
                }

                // Custom Dialog Overlays
                if (viewModel.showSettingsDialog) {
                    SettingsDialog(viewModel = viewModel)
                }

                if (viewModel.showGiftsDialog) {
                    GiftsDialog(viewModel = viewModel)
                }

                if (viewModel.showQuestsDialog) {
                    QuestsDialog(viewModel = viewModel)
                }

                if (viewModel.showShopSoonDialog) {
                    ShopSoonDialog(viewModel = viewModel)
                }
            }
        }

        // Persistent bottom banner ad on all screens - floating as an overlay on top of the reserved area
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(58.dp),
            contentAlignment = Alignment.Center
        ) {
            YandexBannerAd(
                adUnitId = "R-M-19471149-4",
                paddingDp = 32, // Miniature size like in gifts dialog!
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                location = "game_bottom"
            )
        }

        // Interstitial Ad Trigger (after completing 5 levels)
        if (viewModel.triggerInterstitialAdShow) {
            InterstitialAdManager(
                adUnitId = "R-M-19471149-2",
                isEnglish = viewModel.isEnglish,
                purpose = "next_level_interstitial",
                onAdDismissedOrFailed = {
                    viewModel.proceedToNextLevelAfterAd()
                }
            )
        }

        // Skip Level Rewarded Ad
        if (viewModel.triggerRewardedAdSkip) {
            RewardedAdManager(
                adUnitId = "R-M-19471149-1",
                isEnglish = viewModel.isEnglish,
                purpose = "skip_level",
                onRewarded = {
                    viewModel.skipLevelForAd()
                    viewModel.triggerRewardedAdSkip = false
                },
                onAdFailedOrClosed = {
                    viewModel.triggerRewardedAdSkip = false
                }
            )
        }

        // Hint Rewarded Ad
        if (viewModel.triggerRewardedAdHint) {
            RewardedAdManager(
                adUnitId = "R-M-19471149-1",
                isEnglish = viewModel.isEnglish,
                purpose = "get_hint",
                onRewarded = {
                    viewModel.useHintForAd()
                    viewModel.triggerRewardedAdHint = false
                },
                onAdFailedOrClosed = {
                    viewModel.triggerRewardedAdHint = false
                }
            )
        }

        // Restore Heart Rewarded Ad
        if (viewModel.triggerRewardedAdRestoreHeart) {
            RewardedAdManager(
                adUnitId = "R-M-19471149-1",
                isEnglish = viewModel.isEnglish,
                purpose = "restore_heart",
                onRewarded = {
                    viewModel.restoreHeartForAd()
                    viewModel.triggerRewardedAdRestoreHeart = false
                },
                onAdFailedOrClosed = {
                    viewModel.triggerRewardedAdRestoreHeart = false
                }
            )
        }
    }
}

@Composable
fun LevelSelectScreen(
    levelStates: List<LevelState>,
    activeLevelIndex: Int,
    onLevelSelected: (Int) -> Unit,
    onUnlockAllLevels: () -> Unit,
    onBackToMenu: () -> Unit,
    isEnglish: Boolean = false
) {
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    // Scroll to current active level on first view
    LaunchedEffect(levelStates) {
        if (activeLevelIndex > 1) {
            val index = (activeLevelIndex - 1).coerceAtMost(levelStates.size - 1)
            if (index > 0) {
                gridState.scrollToItem(index)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Navigation Top Bar with beautiful alignment
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBackToMenu,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("select_level_back_to_menu")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Go Back to Menu",
                    tint = Color.White
                )
            }

            Text(
                text = if (isEnglish) "Level Select" else "Выбор уровней",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 48.dp) // Centered alignment compensation
            )
        }

        // Statistics Summary Card with premium glowing aesthetic
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16181A)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val completedCount = levelStates.count { it.isCompleted }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$completedCount / 1000",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676)
                    )
                    Text(text = if (isEnglish) "Cleared" else "Пройдено", fontSize = 12.sp, color = Color(0xFF8F9BB3))
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(Color(0xFF333A5E))
                )

                // Current Active Level index
                val currentPlayIdx = levelStates.firstOrNull { !it.isCompleted && it.isUnlocked }?.levelId ?: 1
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isEnglish) "Level $currentPlayIdx" else "Уровень $currentPlayIdx",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF42A5F5)
                    )
                    Text(text = if (isEnglish) "Current Progress" else "Текущий прогресс", fontSize = 12.sp, color = Color(0xFF8F9BB3))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Campaign Levels",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Unlock All",
                    fontSize = 13.sp,
                    color = Color(0xFFEF5350), // red accent for administrative actions
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable {
                            onUnlockAllLevels()
                        }
                        .padding(4.dp)
                        .testTag("unlock_all_button")
                )
                
                Text(
                    text = "|",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.2f)
                )

                Text(
                    text = "Scroll to Active",
                    fontSize = 13.sp,
                    color = Color(0xFF3B82F6),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                val activeIdx = levelStates.indexOfFirst { !it.isCompleted && it.isUnlocked }
                                if (activeIdx >= 0) {
                                    gridState.animateScrollToItem(activeIdx)
                                }
                            }
                        }
                        .padding(4.dp)
                )
            }
        }

        // Lazy grid displaying 1000 campaign levels
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 85.dp),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .testTag("levels_grid")
        ) {
            items(levelStates, key = { it.levelId }) { state ->
                LevelItemCard(state = state, onLevelSelected = onLevelSelected)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun LevelItemCard(state: LevelState, onLevelSelected: (Int) -> Unit) {
    // Style elements matching state with a highly polished unified minimalist approach
    val containerColor = when {
        state.isCompleted -> Color(0xFF1E293B)  // Premium Slate 800 - matches bottom bar buttons!
        state.isUnlocked -> Color(0xFF1E293B)   // Premium Slate 800
        else -> Color(0xFF0F172A).copy(alpha = 0.5f) // Dark glass slate
    }

    val strokeColor = when {
        state.isCompleted -> Color(0xFF10B981).copy(alpha = 0.7f) // Elegant subtle emerald
        state.isUnlocked -> Color(0xFF3B82F6).copy(alpha = 0.7f)  // Elegant subtle blue
        else -> Color(0xFF334155).copy(alpha = 0.25f)            // Faint divider gray
    }

    val labelColor = when {
        state.isCompleted -> Color.White.copy(alpha = 0.9f)
        state.isUnlocked -> Color.White
        else -> Color(0xFF475569) // Muted text
    }

    val clickModifier = if (state.isUnlocked) {
        Modifier.clickable { onLevelSelected(state.levelId) }
    } else {
        Modifier // Locked card can't be opened directly
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .then(clickModifier)
            .testTag("level_card_${state.levelId}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "${state.levelId}",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = labelColor
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (!state.isUnlocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(11.dp)
                )
            } else if (state.isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(11.dp)
                )
            } else {
                // For the current active level, show a tiny pulsing/active blue dot
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF3B82F6))
                )
            }
        }
    }
}

@Composable
fun PlayScreen(viewModel: GameViewModel) {
    val levelIndex by viewModel.activeLevelIndex.collectAsStateWithLifecycle()
    val levelData = viewModel.levelData
    
    if (viewModel.isLoadingLevel || levelData == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF3B82F6),
                strokeWidth = 4.dp
            )
        }
        return
    }

    val arrows = viewModel.arrows
    val movesCount = viewModel.movesCount
    val isCleared = viewModel.isLevelCleared
    var showStatsDialog by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val cellSizeDp = 36.dp
    val cellSizePx = with(density) { cellSizeDp.toPx() }

    var boardViewportSize by remember { mutableStateOf(Size.Zero) }
    val densityVal = density.density

    // Center and Fit viewport camera on level start, restart, or window size changes
    LaunchedEffect(levelIndex, boardViewportSize, viewModel.restartCounter) {
        if (boardViewportSize.width > 0f && boardViewportSize.height > 0f) {
            viewModel.centerAndFitBoard(
                viewW = boardViewportSize.width,
                viewH = boardViewportSize.height,
                gridWidth = levelData.gridWidth,
                gridHeight = levelData.gridHeight,
                density = densityVal
            )
        }
    }

    // Dynamic win sequence delay: auto-reset camera and fade out dots before victory popup
    LaunchedEffect(viewModel.isWinningTransition) {
        if (viewModel.isWinningTransition) {
            if (boardViewportSize.width > 0f && boardViewportSize.height > 0f) {
                viewModel.centerAndFitBoard(
                    viewW = boardViewportSize.width,
                    viewH = boardViewportSize.height,
                    gridWidth = levelData.gridWidth,
                    gridHeight = levelData.gridHeight,
                    density = densityVal
                )
            }
            // Allow smooth fade out of dots
            kotlinx.coroutines.delay(1000)
            viewModel.finalizeWin()
        }
    }

    val isOverlayActive = isCleared || viewModel.isGameOver

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (isOverlayActive) 12.dp else 0.dp)
        ) {
            // Header Stats bar
            PlayScreenHeader(
                levelIndex = levelIndex,
                shapeName = levelData.shapeType.getLocalizedName(viewModel.isEnglish),
                shapeDesc = levelData.shapeType.getLocalizedDescription(viewModel.isEnglish),
                movesCount = movesCount,
                onBackClicked = { viewModel.exitToLevelSelect() },
                onRestartClicked = { viewModel.restartLevel() },
                isEnglish = viewModel.isEnglish,
                onStatsClicked = { showStatsDialog = true }
            )

            // Lives/Hearts indicator in place of zoom instructions banner
            LivesIndicator(lives = viewModel.lives, maxLives = viewModel.maxLives)

            // The absolute interactive game board viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Transparent)
                    .onSizeChanged { size ->
                        boardViewportSize = Size(size.width.toFloat(), size.height.toFloat())
                    }
            ) {
                InteractiveGameBoard(
                    arrows = arrows,
                    gridWidth = levelData.gridWidth,
                    gridHeight = levelData.gridHeight,
                    cellSizePx = cellSizePx,
                    scale = viewModel.cameraScale,
                    offset = viewModel.cameraOffset,
                    onScaleChanged = { viewModel.cameraScale = it },
                    onOffsetChanged = { viewModel.cameraOffset = it },
                    onCellTapped = { x, y ->
                        // Match world coordinate tapped cells
                        val col = kotlin.math.floor(x).toInt()
                        val row = kotlin.math.floor(y).toInt()
                        if (col in 0 until levelData.gridWidth && row in 0 until levelData.gridHeight) {
                            // Find which arrow occupies this cell
                            val hitCell = Point(col, row)
                            val hitArrow = viewModel.arrows.find { (it.state == ArrowState.IDLE || it.state == ArrowState.WIGGLING) && it.occupies(hitCell) }
                            if (hitArrow != null) {
                                viewModel.tapArrow(hitArrow.id)
                            }
                        }
                    },
                    onResetCamera = {
                        if (boardViewportSize.width > 0f && boardViewportSize.height > 0f) {
                            viewModel.centerAndFitBoard(
                                viewW = boardViewportSize.width,
                                viewH = boardViewportSize.height,
                                gridWidth = levelData.gridWidth,
                                gridHeight = levelData.gridHeight,
                                density = densityVal
                            )
                        }
                    },
                    showGuides = viewModel.showGuides,
                    highlightedHintArrowId = viewModel.highlightedHintArrowId,
                    isWinning = viewModel.isWinningTransition
                )

                // Absolute Floating Zoom Controls Overlay on top-right (Minimalist style)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            viewModel.cameraScale = (viewModel.cameraScale * 1.2f).coerceIn(0.5f, 4.0f)
                        },
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .size(40.dp)
                    ) {
                        Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
 
                    IconButton(
                        onClick = {
                            viewModel.cameraScale = (viewModel.cameraScale / 1.2f).coerceIn(0.5f, 4.0f)
                        },
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .size(40.dp)
                    ) {
                        Text("−", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }

                // Floating progress bar absolute layout at bottom-center
                val totalArrows = arrows.size
                val remainingArrows = arrows.count { it.state != ArrowState.EXITED }
                val clearedArrows = totalArrows - remainingArrows
                val progressRatio = if (totalArrows > 0) clearedArrows.toFloat() / totalArrows else 0f

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (viewModel.isEnglish) "$remainingArrows / $totalArrows REMAINING" else "ОСТАЛОСЬ: $remainingArrows / $totalArrows",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.4f),
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(192.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFF334155).copy(alpha = 0.5f)) // bg-slate-800
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fraction = progressRatio.coerceIn(0f, 1f))
                                .background(Color(0xFF3B82F6), RoundedCornerShape(2.dp)) // bg-blue-500
                        )
                    }
                }
            }

            // Bottom Buttons bar aligned on a single row with onResetCamera handler
            PlayScreenBottomBar(
                viewModel = viewModel,
                onResetCamera = {
                    if (boardViewportSize.width > 0f && boardViewportSize.height > 0f) {
                        viewModel.centerAndFitBoard(
                            viewW = boardViewportSize.width,
                            viewH = boardViewportSize.height,
                            gridWidth = levelData.gridWidth,
                            gridHeight = levelData.gridHeight,
                            density = densityVal
                        )
                    }
                }
            )
        }

        // Celebrate Clears dialog screen
        AnimatedVisibility(
            visible = isCleared,
            enter = fadeIn(animationSpec = tween(400)) + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            VictoryOverlay(
                levelIndex = levelIndex,
                isFlawless = viewModel.lives == viewModel.maxLives,
                livesRemaining = viewModel.lives,
                maxLives = viewModel.maxLives,
                onNextClicked = { viewModel.nextLevel() },
                onReplayClicked = { viewModel.restartLevel() },
                onBackClicked = { viewModel.exitToLevelSelect() },
                isEnglish = viewModel.isEnglish
            )
        }

        // GameOver Overlay screen when lives are fully depleted
        AnimatedVisibility(
            visible = viewModel.isGameOver,
            enter = fadeIn(animationSpec = tween(400)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            GameOverOverlay(
                levelIndex = levelIndex,
                viewModel = viewModel,
                onReplayClicked = { viewModel.restartLevel() },
                onBackClicked = { viewModel.exitToLevelSelect() },
                isEnglish = viewModel.isEnglish
            )
        }

        // Hint and Skip choice dialogs have been removed, triggering ads directly instead

        if (showStatsDialog) {
            AlertDialog(
                onDismissRequest = { showStatsDialog = false },
                title = {
                    Text(
                        text = if (viewModel.isEnglish) "Generation Stats" else "Статистика генерации",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = levelData.generationStats,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStatsDialog = false }) {
                        Text("OK", color = Color(0xFF3B82F6))
                    }
                },
                containerColor = Color(0xFF1E293B),
                titleContentColor = Color.White,
                textContentColor = Color(0xFFCBD5E1)
            )
        }
    }
}

@Composable
fun PlayScreenHeader(
    levelIndex: Int,
    shapeName: String,
    shapeDesc: String,
    movesCount: Int,
    onBackClicked: () -> Unit,
    onRestartClicked: () -> Unit,
    isEnglish: Boolean = false,
    onStatsClicked: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBackClicked,
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .size(40.dp)
                        .testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEnglish) "Level $levelIndex" else "Уровень $levelIndex",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onStatsClicked != null) {
                    IconButton(
                        onClick = onStatsClicked,
                        modifier = Modifier
                            .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                            .size(40.dp)
                            .testTag("stats_button_top")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Stats",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onRestartClicked,
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .size(40.dp)
                        .testTag("restart_button_top")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LivesIndicator(lives: Int, maxLives: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until maxLives) {
            val isActive = i < lives
            Icon(
                imageVector = if (isActive) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isActive) "Active Life" else "Empty Life",
                tint = if (isActive) Color(0xFFEF4444) else Color(0xFF4B5563),
                modifier = Modifier
                    .size(28.dp)
                    .padding(horizontal = 3.dp)
                    .graphicsLayer(
                        scaleX = if (isActive) 1.1f else 0.9f,
                        scaleY = if (isActive) 1.1f else 0.9f
                    )
            )
        }
    }
}

class ActiveTap(
    val id: Long,
    val position: Offset,
    val progress: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>
)

@Composable
fun InteractiveGameBoard(
    arrows: List<Arrow>,
    gridWidth: Int,
    gridHeight: Int,
    cellSizePx: Float,
    scale: Float,
    offset: Offset,
    onScaleChanged: (Float) -> Unit,
    onOffsetChanged: (Offset) -> Unit,
    onCellTapped: (Float, Float) -> Unit,
    onResetCamera: () -> Unit,
    showGuides: Boolean,
    highlightedHintArrowId: Int? = null,
    isWinning: Boolean = false
) {
    // Canvas dimensions helper
    var viewportSize by remember { mutableStateOf(Size.Zero) }

    val dotsAlpha by animateFloatAsState(
        targetValue = if (isWinning) 0f else 0.20f,
        animationSpec = tween(durationMillis = 800),
        label = "dotsAlpha"
    )

    val activeTaps = remember { mutableStateListOf<ActiveTap>() }
    val coroutineScope = rememberCoroutineScope()

    val currentScaleState = rememberUpdatedState(scale)
    val currentOffsetState = rememberUpdatedState(offset)
    val currentOnCellTappedState = rememberUpdatedState(onCellTapped)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1113)) // Blends completely with main page background color
            .pointerInput(gridWidth, gridHeight) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var totalPan = Offset.Zero
                    val touchSlop = viewConfiguration.touchSlop
                    var isPanning = false
                    var pointerId = down.id
                    
                    // Track local copies of offset and scale inside the gesture session
                    // to prevent high-frequency State update lag and resolve any jittering issues completely!
                    var localOffset = currentOffsetState.value
                    var localScale = currentScaleState.value
                    
                    while (true) {
                        val event = awaitPointerEvent()
                        val activeChanges = event.changes.filter { it.pressed }
                        
                        if (activeChanges.isEmpty()) {
                            // All fingers released
                            if (!isPanning) {
                                // Trigger tap selection in grid cell system
                                val screenTap = down.position
                                val worldX = (screenTap.x - localOffset.x) / (localScale * cellSizePx)
                                val worldY = (screenTap.y - localOffset.y) / (localScale * cellSizePx)
                                currentOnCellTappedState.value(worldX, worldY)

                                // Trigger a beautiful tap ripple
                                val tapId = System.currentTimeMillis()
                                val anim = androidx.compose.animation.core.Animatable(0f)
                                val activeTap = ActiveTap(tapId, screenTap, anim)
                                activeTaps.add(activeTap)
                                coroutineScope.launch {
                                    anim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = androidx.compose.animation.core.tween(durationMillis = 400)
                                    )
                                    activeTaps.remove(activeTap)
                                }
                            }
                            break
                        }
                        
                        if (activeChanges.size == 1) {
                            val change = activeChanges.firstOrNull { it.id == pointerId }
                            if (change != null) {
                                val dragAmount = change.position - change.previousPosition
                                totalPan += dragAmount
                                if (!isPanning && totalPan.getDistance() > touchSlop) {
                                    isPanning = true
                                }
                                if (isPanning) {
                                    localOffset += dragAmount
                                    onOffsetChanged(localOffset)
                                    change.consume()
                                }
                            } else {
                                pointerId = activeChanges.first().id
                            }
                        } else if (activeChanges.size >= 2) {
                            isPanning = true
                            val p1 = activeChanges[0]
                            val p2 = activeChanges[1]
                            
                            val currentDist = (p1.position - p2.position).getDistance()
                            val prevDist = (p1.previousPosition - p2.previousPosition).getDistance()
                            
                            val centroid = (p1.position + p2.position) / 2f
                            val prevCentroid = (p1.previousPosition + p2.previousPosition) / 2f
                            val panAmount = centroid - prevCentroid
                            
                            if (prevDist > 0f && currentDist > 0f) {
                                val zoomFactor = currentDist / prevDist
                                val oldScale = localScale
                                val newScale = (oldScale * zoomFactor).coerceIn(0.5f, 4.0f)
                                localScale = newScale
                                onScaleChanged(newScale)
                                
                                localOffset = centroid - (centroid - localOffset) * (newScale / oldScale) + panAmount
                                onOffsetChanged(localOffset)
                            } else {
                                localOffset += panAmount
                                onOffsetChanged(localOffset)
                            }
                            activeChanges.forEach { it.consume() }
                        }
                    }
                }
            }
            .testTag("game_board_canvas")
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            if (viewportSize != size) {
                viewportSize = size
            }

            // Save canvas transformation state
            drawContext.canvas.save()
            drawContext.canvas.translate(offset.x, offset.y)
            drawContext.canvas.scale(scale, scale)

            // 1. Draw Grid helper background lines inside board bounds
            drawGridAesthetic(gridWidth, gridHeight, cellSizePx)

            // 1.5. Draw elegant grid dots behind the arrows at their initial grid cells
            // When arrows exit, they leave behind these dots aligning precisely with the shape on the grid!
            val initialShapePoints = arrows.flatMap { it.path }.toSet()
            for (pt in initialShapePoints) {
                val cx = (pt.x + 0.5f) * cellSizePx
                val cy = (pt.y + 0.5f) * cellSizePx
                drawCircle(
                    color = Color.White.copy(alpha = dotsAlpha), // Fades out beautifully upon victory!
                    radius = cellSizePx * 0.04f,            // Perfectly sized to hide underneath strokeWidth of 0.11
                    center = Offset(cx, cy)
                )
            }

            // 2. Main curved arrows rendering
            for (arrow in arrows) {
                if (arrow.state != ArrowState.EXITED) {
                    val isHintHighlighted = arrow.id == highlightedHintArrowId
                    drawCurvedArrow(arrow, cellSizePx, arrows, gridWidth, gridHeight, showGuides, isHintHighlighted)
                }
            }

            // Restore canvas transformation state
            drawContext.canvas.restore()

            // 3. Draw active screen-space tap indicators
            activeTaps.forEach { tap ->
                val progress = tap.progress.value
                val radius = cellSizePx * 0.4f * (0.3f + 0.7f * progress)
                val alpha = (1f - progress) * 0.45f
                drawCircle(
                    color = Color(0xFFE2E8F0).copy(alpha = alpha), // Elegant minimalistic light gray/white tap ripple
                    radius = radius,
                    center = tap.position
                )
            }
        }
    }
}

private fun DrawScope.drawGridAesthetic(w: Int, h: Int, size: Float) {
    // Left completely blank to remove any bounding squares or outer borders, matching the reference screenshot exactly.
}

private fun isArrowBlocked(arrow: Arrow, activeArrows: List<Arrow>, w: Int, h: Int): Boolean {
    val exitDir = arrow.exitDirection
    val head = arrow.head
    
    var currX = head.x + exitDir.x
    var currY = head.y + exitDir.y
    
    while (currX in 0 until w && currY in 0 until h) {
        val checkPoint = Point(currX, currY)
        for (other in activeArrows) {
            if (other.id != arrow.id && other.occupies(checkPoint)) {
                return true
            }
        }
        currX += exitDir.x
        currY += exitDir.y
    }
    return false
}

private fun getDistanceToBlocker(arrow: Arrow, activeArrows: List<Arrow>, w: Int, h: Int): Float {
    val exitDir = arrow.exitDirection
    val head = arrow.head
    
    var currX = head.x + exitDir.x
    var currY = head.y + exitDir.y
    var step = 1
    
    while (currX in 0 until w && currY in 0 until h) {
        val checkPoint = Point(currX, currY)
        for (other in activeArrows) {
            if (other.id != arrow.id && other.occupies(checkPoint)) {
                return step.toFloat()
            }
        }
        currX += exitDir.x
        currY += exitDir.y
        step++
    }
    return 1f
}

// Custom trajectory point representing dynamically calculated positions of sliding curves
private data class TrajectoryPoint(val position: Offset, val direction: Offset)

private fun getTrajectoryAtDistance(
    d: Float,
    offsets: List<Offset>,
    lengths: List<Float>,
    cumLengths: List<Float>,
    exitDir: Offset
): TrajectoryPoint {
    val K = offsets.size
    if (K < 2) return TrajectoryPoint(offsets.firstOrNull() ?: Offset.Zero, exitDir)

    val totalLength = cumLengths.last()
    if (d < 0f) {
        // Extrapolate backwards from the first segment to handle negative slide positions beautifully
        val dir = if (lengths.isNotEmpty() && lengths[0] > 0f) {
            (offsets[1] - offsets[0]) / lengths[0]
        } else {
            exitDir
        }
        val pos = offsets.first() + dir * d
        return TrajectoryPoint(pos, dir)
    }
    if (d >= totalLength) {
        val extra = d - totalLength
        val pos = offsets.last() + exitDir * extra
        return TrajectoryPoint(pos, exitDir)
    }

    for (i in 0 until K - 1) {
        val startCum = cumLengths[i]
        val endCum = cumLengths[i + 1]
        if (d >= startCum && d < endCum) {
            val segLen = lengths[i]
            val factor = if (segLen > 0f) (d - startCum) / segLen else 0f
            val startPt = offsets[i]
            val endPt = offsets[i + 1]
            val pos = startPt + (endPt - startPt) * factor
            val dir = (endPt - startPt) / if (segLen > 0f) segLen else 1f
            return TrajectoryPoint(pos, dir)
        }
    }

    // Last segment clamped fallback
    val lastSegDir = (offsets[K - 1] - offsets[K - 2]).let {
        val len = sqrt(it.x * it.x + it.y * it.y)
        if (len > 0) it / len else Offset(0f, -1f)
    }
    return TrajectoryPoint(offsets.last(), lastSegDir)
}

private fun DrawScope.drawCurvedArrow(
    arrow: Arrow,
    cellSize: Float,
    arrows: List<Arrow>,
    gridWidth: Int,
    gridHeight: Int,
    showGuides: Boolean,
    isHintHighlighted: Boolean = false
) {
    if (arrow.path.size < 2) return

    val strokeWidth = cellSize * 0.11f      // Thin, crisp lines matching reference screenshot perfectly
    val arrowHeadWidth = cellSize * 0.36f   // Perfectly proportioned arrowhead width
    val arrowHeadLength = cellSize * 0.25f  // Perfectly proportioned arrowhead length

    // Render arrow in beautiful dynamic colors (Blue on exit selection, Red on collision, Green on solvable hint highlight, or crisp White!)
    val arrowColor = when {
        arrow.state == ArrowState.EXITING -> Color(0xFF3B82F6) // Electric Blue active feedback
        arrow.isCollided -> Color(0xFFEF4444)
        isHintHighlighted -> Color(0xFF10B981) // Beautiful emerald green hint color
        else -> Color.White
    }

    // 1. Map board coordinate path points to coordinate Offset centers
    val originalOffsets = arrow.path.map { p ->
        Offset((p.x + 0.5f) * cellSize, (p.y + 0.5f) * cellSize)
    }

    // 2. Measure segments exact pixel distances
    val K = originalOffsets.size
    val lengths = mutableListOf<Float>()
    val cumLengths = mutableListOf<Float>()
    cumLengths.add(0f)

    for (i in 0 until K - 1) {
        val dx = originalOffsets[i+1].x - originalOffsets[i].x
        val dy = originalOffsets[i+1].y - originalOffsets[i].y
        val d = sqrt(dx*dx + dy*dy)
        lengths.add(d)
        cumLengths.add(cumLengths.last() + d)
    }

    val totalLength = cumLengths.last()

    // 3. Compute exit direction normalized offset
    val lastDirX = arrow.exitDirection.x.toFloat()
    val lastDirY = arrow.exitDirection.y.toFloat()
    val rawExitDirLen = sqrt(lastDirX*lastDirX + lastDirY*lastDirY)
    val exitDir = if (rawExitDirLen > 0f) {
        Offset(lastDirX / rawExitDirLen, lastDirY / rawExitDirLen)
    } else {
        Offset(0f, -1f)
    }

    // 4. Determine slide or dynamic collision displacement shift distance
    val shiftDistance = when (arrow.state) {
        ArrowState.EXITING -> {
            // Animates outward + extra padding to completely clear screen bounds
            val extraTravel = cellSize * 8f
            arrow.slideAnimProgress * (totalLength + extraTravel)
        }
        ArrowState.WIGGLING -> {
            // Calculate a beautiful, physically solid collision bounce
            val activeArrows = arrows.filter { it.state != ArrowState.EXITED }
            val step = getDistanceToBlocker(arrow, activeArrows, gridWidth, gridHeight)
            val maxTravelCells = (step - 1.0f + 0.25f).coerceAtLeast(0.25f)
            
            // Peak at progress = 0.5, then smoothly retreat back to starting position at progress = 1.0
            val bounceFactor = Math.sin(arrow.slideAnimProgress * Math.PI).toFloat()
            bounceFactor * maxTravelCells * cellSize
        }
        else -> 0f
    }

    // 5. Build dynamic sub-path matching sliding portion
    val startD = shiftDistance
    val endD = shiftDistance + totalLength // Retains exact uniform width

    // Shaft goes all the way to endD so it merges flawlessly with the chevron arrowhead tip
    val endStrokeD = endD

    val startPointResult = getTrajectoryAtDistance(startD, originalOffsets, lengths, cumLengths, exitDir)
    val endPointResult = getTrajectoryAtDistance(endD, originalOffsets, lengths, cumLengths, exitDir)

    val pStart = startPointResult.position
    val pEnd = endPointResult.position

    // Collect original points inside sliding interval
    val drawPoints = mutableListOf<Offset>()
    drawPoints.add(pStart)

    for (i in 0 until K) {
        val vertexCumDist = cumLengths[i]
        if (vertexCumDist > startD && vertexCumDist < endStrokeD) {
            drawPoints.add(originalOffsets[i])
        }
    }
    drawPoints.add(pEnd)

    // Build Canvas path from offsets
    val arrowPath = Path().apply {
        moveTo(drawPoints.first().x, drawPoints.first().y)
        for (idx in 1 until drawPoints.size) {
            lineTo(drawPoints[idx].x, drawPoints[idx].y)
        }
    }

    // 6. Draw main solid stroke arrow shaft - using Butt cap and Miter join
    // for clean orthogonal, strictly sharp blocky shapes matching the screenshot 100%!
    drawPath(
        path = arrowPath,
        color = arrowColor,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Butt,
            join = StrokeJoin.Miter
        )
    )

    // 7. Draw the open chevron arrowhead at the exact sliding head tip
    val headTip = pEnd
    val headDir = endPointResult.direction

    // Rotation of arrowhead matching locally tangent vector
    val headAngleRad = Math.atan2(headDir.y.toDouble(), headDir.x.toDouble())
    val headAngleDeg = Math.toDegrees(headAngleRad).toFloat()

    drawContext.canvas.save()
    drawContext.canvas.translate(headTip.x, headTip.y)
    drawContext.canvas.rotate(headAngleDeg)

    // Beautiful sharp chevron arrowhead lines matching the reference screenshot exactly.
    val chevronPath = Path().apply {
        moveTo(-arrowHeadLength, -arrowHeadWidth * 0.5f)
        lineTo(0f, 0f)
        lineTo(-arrowHeadLength, arrowHeadWidth * 0.5f)
    }

    drawPath(
        path = chevronPath,
        color = arrowColor,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Butt,
            join = StrokeJoin.Miter
        )
    )

    drawContext.canvas.restore()

    if (showGuides) {
        val lastPoint = arrow.path.last()
        val gExitDir = arrow.exitDirection
        
        // Trace the path step-by-step to check for obstacles in other arrows
        var steps = 0
        var currX = lastPoint.x + gExitDir.x
        var currY = lastPoint.y + gExitDir.y
        
        while (true) {
            val withinGrid = currX in 0 until gridWidth && currY in 0 until gridHeight
            if (withinGrid) {
                val isOccupied = arrows.any { it.state != ArrowState.EXITED && it.occupies(Point(currX, currY)) }
                if (isOccupied) {
                    // If blocked, stop right at the obstacle cell to show the obstruction
                    steps++
                    break
                }
            } else {
                // Beyond grid bounds, continue drawing until it exits smartphone view boundaries completely (e.g. 150 cells max)
                if (steps >= 150) {
                    break
                }
            }
            steps++
            currX += gExitDir.x
            currY += gExitDir.y
        }
        
        val headCenter = Offset((lastPoint.x + 0.5f) * cellSize, (lastPoint.y + 0.5f) * cellSize)
        val exitTarget = headCenter + Offset(gExitDir.x * cellSize * steps, gExitDir.y * cellSize * steps)

        drawLine(
            color = Color(0xFF38BDF8).copy(alpha = 0.6f), // Classy single cyan guide line for all arrows
            start = headCenter,
            end = exitTarget,
            strokeWidth = cellSize * 0.05f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(cellSize * 0.2f, cellSize * 0.2f), 0f)
        )
    }
}

@Composable
fun BottomBarItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    activeHighlight: Boolean = false,
    testTag: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(enabled = enabled) { onClick() }
            .alpha(if (enabled) 1.0f else 0.4f)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (activeHighlight) Color(0xFFEAB308).copy(alpha = 0.2f) else Color(0xFF1E293B))
                .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = if (activeHighlight) Color(0xFFEAB308) else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = text,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = if (activeHighlight) Color(0xFFEAB308) else Color.White.copy(alpha = 0.6f),
            letterSpacing = 0.5.sp,
            maxLines = 1
        )
    }
}

@Composable
fun PlayScreenBottomBar(
    viewModel: GameViewModel,
    onResetCamera: () -> Unit
) {
    val isEnglish = viewModel.isEnglish
    val canUndo = viewModel.canUndo

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(86.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. UNDO / ОТМЕНИТЬ ХОД
        BottomBarItem(
            icon = Icons.Default.ArrowBack,
            text = if (isEnglish) "UNDO MOVE" else "ОТМЕНИТЬ ХОД",
            onClick = { viewModel.undo() },
            enabled = canUndo,
            testTag = "undo_button"
        )

        // 2. HINT / ПОДСКАЗКА
        BottomBarItem(
            icon = Icons.Default.Lightbulb,
            text = if (isEnglish) "HINT" else "ПОДСКАЗКА",
            onClick = { viewModel.requestHintForAd() },
            testTag = "hint_ad_button"
        )

        // 3. GUIDES / ПОКАЗАТЬ ПУТИ
        val guidesActive = viewModel.showGuides
        BottomBarItem(
            icon = if (guidesActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            text = if (isEnglish) "SHOW PATHS" else "ПОКАЗАТЬ ПУТИ",
            onClick = { viewModel.toggleGuides() },
            activeHighlight = guidesActive,
            testTag = "guides_button"
        )

        // 4. SKIP / ПРОПУСК
        BottomBarItem(
            icon = Icons.Default.SkipNext,
            text = if (isEnglish) "SKIP" else "ПРОПУСК",
            onClick = { viewModel.requestSkipLevelForAd() },
            testTag = "skip_level_ad_button"
        )

        // 5. CAMERA / КАМЕРА
        BottomBarItem(
            icon = Icons.Default.FilterCenterFocus,
            text = if (isEnglish) "CAMERA" else "КАМЕРА",
            onClick = onResetCamera,
            testTag = "reset_camera_button"
        )
    }
}

@Composable
fun VictoryOverlay(
    levelIndex: Int,
    isFlawless: Boolean,
    livesRemaining: Int,
    maxLives: Int,
    onNextClicked: () -> Unit,
    onReplayClicked: () -> Unit,
    onBackClicked: () -> Unit,
    isEnglish: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A0C16)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131524)),
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Success Trophy Header with beautiful glowing gold star
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFB300).copy(alpha = 0.15f), CircleShape)
                        .size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Success Star",
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isEnglish) "LEVEL CLEARED!" else "УРОВЕНЬ ПРОЙДЕН!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )

                Text(
                    text = if (isEnglish) "You successfully solved Level $levelIndex!" else "Вы успешно прошли Уровень $levelIndex!",
                    fontSize = 14.sp,
                    color = Color(0xFF8F9BB3),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Divider(color = Color(0xFF2E3555), modifier = Modifier.padding(vertical = 4.dp))

                // Beautiful custom star rating display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val starsCount = when {
                            isFlawless -> 3
                            livesRemaining >= maxLives - 2 -> 2
                            else -> 1
                        }
                        repeat(3) { index ->
                            val active = index < starsCount
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star",
                                tint = if (active) Color(0xFFFFB300) else Color(0xFF2E3555),
                                modifier = Modifier.size(if (index == 1) 36.dp else 28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = when {
                            isFlawless -> if (isEnglish) "PERFECT CLEAR!" else "ИДЕАЛЬНОЕ ПРОХОЖДЕНИЕ!"
                            livesRemaining >= maxLives - 2 -> if (isEnglish) "EXCELLENT!" else "ОТЛИЧНО!"
                            else -> if (isEnglish) "LEVEL COMPLETED!" else "УРОВЕНЬ ЗАВЕРШЕН!"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isFlawless -> Color(0xFF10B981)
                            livesRemaining >= maxLives - 2 -> Color(0xFF3B82F6)
                            else -> Color(0xFF8F9BB3)
                        }
                    )
                }

                // Beautiful, informative level reward card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16182C)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isEnglish) "STAGE REWARDS" else "НАГРАДА ЗА УРОВЕНЬ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8F9BB3),
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = "+15", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFFFBBF24))
                                Text(text = "🪙", fontSize = 16.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(text = "+2", fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF60A5FA))
                                Text(text = "💎", fontSize = 16.sp)
                            }
                        }
                    }
                }

                // Lives remaining summary
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Lives Left:" else "Осталось жизней:",
                        fontSize = 12.sp,
                        color = Color(0xFF8F9BB3)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(maxLives) { index ->
                            val hasLife = index < livesRemaining
                            Text(
                                text = if (hasLife) "❤️" else "🖤",
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions row
                Button(
                    onClick = onNextClicked,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                        contentColor = Color(0xFF10B981)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("next_after_win")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "NEXT LEVEL" else "СЛЕДУЮЩИЙ УРОВЕНЬ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onReplayClicked,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6).copy(alpha = 0.15f),
                            contentColor = Color(0xFF3B82F6)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEnglish) "Replay" else "Заново",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF3B82F6)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onBackClicked,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF64748B).copy(alpha = 0.15f),
                            contentColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Level Menu",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEnglish) "Menu" else "Меню",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameOverOverlay(
    levelIndex: Int,
    viewModel: GameViewModel,
    onReplayClicked: () -> Unit,
    onBackClicked: () -> Unit,
    isEnglish: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A0C16)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131524)),
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f), CircleShape)
                        .size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "No Lives Remaining",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isEnglish) "NO LIVES LEFT!" else "ЖИЗНИ ЗАКОНЧИЛИСЬ!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Text(
                    text = if (isEnglish) "You ran out of lives on Level $levelIndex. Don't give up!" else "У вас закончились жизни на Уровне $levelIndex. Не сдавайтесь!",
                    fontSize = 14.sp,
                    color = Color(0xFF8F9BB3),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // OPTION 1: Prominent Ad Button (visually preserved but updated to run ad for +1 heart)
                Button(
                    onClick = {
                        viewModel.requestRestoreHeartForAd()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD97706).copy(alpha = 0.15f),
                        contentColor = Color(0xFFD97706)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("restore_heart_with_ad")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Restore Heart with Ad",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "WATCH AD (+1 ❤️)" else "СМОТРЕТЬ РЕКЛАМУ (+1 ❤️)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // OPTION 2: Less noticeable Coins Button (restore 1 heart for 100 coins, max 2 times)
                val canAffordCoinsRestore = viewModel.coins >= 100 && viewModel.heartRestoresBoughtWithCoinsThisLevel < 2
                val remainingRestores = 2 - viewModel.heartRestoresBoughtWithCoinsThisLevel
                Button(
                    onClick = {
                        if (canAffordCoinsRestore) {
                            viewModel.buyOneHeartWithCoins()
                        }
                    },
                    enabled = canAffordCoinsRestore,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canAffordCoinsRestore) Color(0xFFEF4444).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                        contentColor = if (canAffordCoinsRestore) Color(0xFFEF4444) else Color(0xFF475569),
                        disabledContainerColor = Color.White.copy(alpha = 0.05f),
                        disabledContentColor = Color(0xFF475569)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("restore_heart_with_coins")
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Buy Heart",
                        tint = if (canAffordCoinsRestore) Color(0xFFEF4444) else Color(0xFF475569),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "BUY 1 ❤️ (100 🪙) ($remainingRestores left)" else "КУПИТЬ 1 ❤️ ЗА 100 🪙 (осталось: $remainingRestores)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (canAffordCoinsRestore) Color(0xFFEF4444) else Color(0xFF475569)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onReplayClicked,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                        contentColor = Color(0xFFEF4444)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("restart_after_game_over")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "TRY AGAIN" else "ПОПРОБОВАТЬ СНОВА",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onBackClicked,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        contentColor = Color(0xFF3B82F6)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Level Menu",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Level Select" else "Выбор уровня",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3B82F6)
                    )
                }
            }
        }
    }
}

@Composable
fun GameLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Background soft glowing aura
        drawCircle(
            color = Color.White.copy(alpha = 0.05f),
            radius = w * 0.45f
        )
        
        // Path 1: Loop-around curved line
        val path1 = Path().apply {
            moveTo(w * 0.25f, h * 0.75f)
            lineTo(w * 0.25f, h * 0.35f)
            quadraticTo(w * 0.25f, h * 0.20f, w * 0.45f, h * 0.20f)
            lineTo(w * 0.70f, h * 0.20f)
        }
        
        val strokeWidth = 5.dp.toPx()
        
        drawPath(
            path = path1,
            color = Color.White,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Chevron for Path 1 (pointing right)
        val head1 = Path().apply {
            val hLength = 12.dp.toPx()
            val hWidth = 14.dp.toPx()
            moveTo(w * 0.70f - hLength, h * 0.20f - hWidth * 0.5f)
            lineTo(w * 0.70f, h * 0.20f)
            lineTo(w * 0.70f - hLength, h * 0.20f + hWidth * 0.5f)
        }
        drawPath(
            path = head1,
            color = Color.White,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Path 2: Sharp S-bend
        val path2 = Path().apply {
            moveTo(w * 0.75f, h * 0.25f)
            lineTo(w * 0.75f, h * 0.65f)
            quadraticTo(w * 0.75f, h * 0.80f, w * 0.55f, h * 0.80f)
            lineTo(w * 0.30f, h * 0.80f)
        }
        drawPath(
            path = path2,
            color = Color.White,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        
        // Chevron for Path 2 (pointing left)
        val head2 = Path().apply {
            val hLength = 12.dp.toPx()
            val hWidth = 14.dp.toPx()
            moveTo(w * 0.30f + hLength, h * 0.80f - hWidth * 0.5f)
            lineTo(w * 0.30f, h * 0.80f)
            lineTo(w * 0.30f + hLength, h * 0.80f + hWidth * 0.5f)
        }
        drawPath(
            path = head2,
            color = Color.White,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun MainMenuScreen(
    viewModel: GameViewModel,
    levelStates: List<LevelState>,
    isEnglish: Boolean,
    onPlayClicked: () -> Unit,
    onLevelSelectClicked: () -> Unit
) {
    val currentPlayIdx = levelStates.firstOrNull { !it.isCompleted && it.isUnlocked }?.levelId ?: 1

    val hasDailyReward by viewModel.hasUnclaimedDailyReward.collectAsState()
    val hasWeeklyReward by viewModel.hasUnclaimedWeeklyReward.collectAsState()
    val hasAchReward by viewModel.hasUnclaimedAchievementReward.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper block: Logo and title
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp)
        ) {
            GameLogo(
                modifier = Modifier
                    .size(120.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Arrow Escape",
                fontSize = 38.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isEnglish) "Logical puzzle of untangling arrows" else "Логическая головоломка с распутыванием стрелок",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8F9BB3),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Middle block: Bold action center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp)
        ) {
            // "ИГРАТЬ" (PLAY) button starting the last unlocked level immediately
            Button(
                onClick = onPlayClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("menu_play_button"),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = when (viewModel.selectedTheme) {
                        "classic" -> Color(0xFF2563EB)
                        "emerald" -> Color(0xFF059669)
                        "golden" -> Color(0xFFD97706)
                        "volcanic" -> Color(0xFFDC2626)
                        "retro" -> Color(0xFF16A34A)
                        "vaporwave" -> Color(0xFFD946EF)
                        else -> Color(0xFF2563EB)
                    },
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Icon",
                        modifier = Modifier
                            .size(28.dp)
                            .padding(end = 8.dp),
                        tint = Color.White
                    )
                    Text(
                        text = if (isEnglish) "PLAY" else "ИГРАТЬ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Level selection button
            Button(
                onClick = onLevelSelectClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("menu_level_select_button"),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155), contentColor = Color.White)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Levels Icon",
                        modifier = Modifier
                            .size(20.dp)
                            .padding(end = 8.dp),
                        tint = Color.White
                    )
                    Text(
                        text = if (isEnglish) "Select Level" else "Выбор уровней",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Lower block: Prominent Battle Pass banner and Row layout displaying auxiliary buttons as pairs on 2 rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Prominent BATTLE PASS banner card placed exactly above the lower buttons for compact spacing
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)), // Deep cosmic indigo
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clickable { viewModel.navigateTo(ScreenType.BATTLE_PASS) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF4338CA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Battle Pass",
                                tint = Color(0xFFFBBF24), // Golden star
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "BATTLE PASS" else "БОЕВОЙ ПРОПУСК",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = if (isEnglish) "Season Level ${viewModel.seasonLevel}" else "Сезонный уровень ${viewModel.seasonLevel}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC7D2FE)
                            )
                        }
                    }
                    
                    // Small progress chip on the right
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF312E81), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${viewModel.seasonXp}/100 XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF818CF8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp)) // Tiny spacer for neat layout separation

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuSubCard(
                    title = if (isEnglish) "Daily Quests" else "Ежедневные задания",
                    icon = Icons.Default.DateRange,
                    showBadge = hasDailyReward,
                    onClick = {
                        viewModel.navigateTo(ScreenType.DAILY_QUESTS)
                    },
                    modifier = Modifier.weight(1f)
                )
                MenuSubCard(
                    title = if (isEnglish) "Store" else "Магазин",
                    icon = Icons.Default.ShoppingCart,
                    showBadge = false,
                    onClick = {
                        viewModel.navigateTo(ScreenType.SHOP)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuSubCard(
                    title = if (isEnglish) "Weekly Missions" else "Еженедельные задания",
                    icon = Icons.Default.Star,
                    showBadge = hasWeeklyReward,
                    onClick = {
                        viewModel.navigateTo(ScreenType.WEEKLY_QUESTS)
                    },
                    modifier = Modifier.weight(1f)
                )
                MenuSubCard(
                    title = if (isEnglish) "Achievements" else "Достижения",
                    icon = Icons.Default.Favorite,
                    showBadge = hasAchReward,
                    onClick = {
                        viewModel.navigateTo(ScreenType.ACHIEVEMENTS)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MenuSubCard(
    title: String,
    icon: ImageVector,
    showBadge: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16181A)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    lineHeight = 13.sp,
                    maxLines = 2
                )
            }
        }

        if (showBadge) {
            // Elegant glowing gold/red badge nestled inside the top right corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
                    .size(10.dp)
                    .background(Color(0xFFEF4444), CircleShape)
            )
        }
    }
}

@Composable
fun HeaderBar(viewModel: GameViewModel) {
    val isEnglish = viewModel.isEnglish
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left block: Coins and Diamonds in soft glassmorphic pills
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Coins
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text = "🪙", fontSize = 14.sp)
                Text(
                    text = "${viewModel.coins}",
                    color = Color(0xFFFBBF24), // Consistent gold color
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Diamonds
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(text = "💎", fontSize = 14.sp)
                Text(
                    text = "${viewModel.diamonds}",
                    color = Color(0xFF60A5FA), // Consistent sky blue color
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        // Right block: Mute, Red Gifts & Rewards Center, gear settings icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Sound toggle button (transparent)
            IconButton(
                onClick = { viewModel.isMuted = !viewModel.isMuted },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (viewModel.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = "Toggle Mute",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Red Gift/Rewards Center Button (translucent modern red accent)
            Button(
                onClick = { viewModel.navigateTo(ScreenType.REWARD_CENTER) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                    contentColor = Color(0xFFEF4444)
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CardGiftcard,
                    contentDescription = "Gifts",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isEnglish) "GIFTS" else "ПОДАРКИ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFEF4444)
                )
            }

            // Settings icon (transparent)
            IconButton(
                onClick = { viewModel.showSettingsDialog = true },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsDialog(viewModel: GameViewModel) {
    val isEnglish = viewModel.isEnglish
    var showConfirmReset by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A0C16))
            .clickable { viewModel.showSettingsDialog = false },
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131524)),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) {}, // prevent click-through
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "SETTINGS" else "НАСТРОЙКИ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = { viewModel.showSettingsDialog = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFF334155).copy(alpha = 0.3f)))
                Spacer(modifier = Modifier.height(12.dp))

                if (!showConfirmReset) {
                    // Mute check
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (viewModel.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Sound Mute",
                                tint = Color(0xFF38BDF8)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isEnglish) "Mute Sound" else "Без звука",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = viewModel.isMuted,
                            onCheckedChange = { viewModel.isMuted = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF3B82F6),
                                checkedTrackColor = Color(0xFF1E293B),
                                uncheckedThumbColor = Color(0xFF94A3B8),
                                uncheckedTrackColor = Color(0xFF0F172A),
                                uncheckedBorderColor = Color(0xFF334155)
                            )
                        )
                    }

                    // Volume slider row
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (isEnglish) "Volume" else "Громкость",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${(viewModel.volume * 100).toInt()}%",
                                color = Color(0xFF38BDF8),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = viewModel.volume,
                            onValueChange = {
                                viewModel.volume = it
                                if (it > 0f) {
                                    viewModel.isMuted = false
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF38BDF8),
                                inactiveTrackColor = Color(0xFF1E293B)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Language Selector Container with Slide Tab Style
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Language" else "Язык",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B))
                                .padding(2.dp)
                        ) {
                            // Russian Tab
                            val isRuSel = !viewModel.isEnglish
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isRuSel) Color(0xFF3B82F6) else Color.Transparent)
                                    .clickable { viewModel.isEnglish = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Русский",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRuSel) Color.White else Color.White.copy(alpha = 0.5f)
                                )
                            }
                            // English Tab
                            val isEnSel = viewModel.isEnglish
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isEnSel) Color(0xFF3B82F6) else Color.Transparent)
                                    .clickable { viewModel.isEnglish = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "English",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEnSel) Color.White else Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Reset Progress Button
                    Button(
                        onClick = { showConfirmReset = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.1f), contentColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Reset Progress", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEnglish) "RESET PROGRESS" else "СБРОСИТЬ ПРОГРЕСС",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Confirmation Section
                    Text(
                        text = if (isEnglish) "Are you sure you want to reset all your progress?" else "Вы уверены, что хотите сбросить весь прогресс?",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showConfirmReset = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Text(if (isEnglish) "Cancel" else "Отмена", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.resetAllGameProgress()
                                showConfirmReset = false
                                viewModel.showSettingsDialog = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.weight(1f).height(42.dp)
                        ) {
                            Text(if (isEnglish) "Reset" else "Сбросить", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GiftsDialog(viewModel: GameViewModel) {
    val isEnglish = viewModel.isEnglish
    var claimedSuccessfully by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A0C16))
            .clickable { viewModel.showGiftsDialog = false },
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131524)),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) {},
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "GIFTS & REWARDS" else "ЦЕНТР НАГРАД",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFEF4444),
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = { viewModel.showGiftsDialog = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Divider(color = Color(0xFF4A1A1A), modifier = Modifier.padding(vertical = 10.dp))

                // Яндекс Ads Real Banner Ad Placement (R-M-19471149-4)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111416)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .align(Alignment.Start)
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "AD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                            Text(
                                text = "Yandex Partner Network",
                                fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Real Yandex Banner Ad View
                        YandexBannerAd(
                            adUnitId = "R-M-19471149-4",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                var showRewardedAdLoader by remember { mutableStateOf(false) }

                if (showRewardedAdLoader) {
                    RewardedAdManager(
                        adUnitId = "R-M-19471149-1",
                        isEnglish = isEnglish,
                        purpose = "chest_reward",
                        onRewarded = {
                            viewModel.claimGiftRewardVideo()
                            claimedSuccessfully = true
                            showRewardedAdLoader = false
                        },
                        onAdFailedOrClosed = {
                            showRewardedAdLoader = false
                        }
                    )
                }

                if (!claimedSuccessfully) {
                    Text(
                        text = if (isEnglish) "Watch a short video clip to claim free coins & diamonds!" else "Посмотрите короткий ролик, чтобы забрать монеты и алмазы!",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Button(
                        onClick = {
                            showRewardedAdLoader = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.PlayCircleFilled, contentDescription = "Play Video")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isEnglish) "WATCH VIDEO (+50🪙 +5💎)" else "СМОТРЕТЬ ВИДЕО (+50🪙 +5💎)",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Claimed successfully",
                        tint = Color(0xFF00E676),
                        modifier = Modifier
                            .size(54.dp)
                            .padding(vertical = 8.dp)
                    )

                    Text(
                        text = if (isEnglish) "Congratulations! Reward credited!" else "Награда успешно зачислена в ваш кошелек!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Button(
                        onClick = { viewModel.showGiftsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = if (isEnglish) "Close" else "Закрыть", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun YandexBannerAd(
    adUnitId: String,
    modifier: Modifier = Modifier,
    paddingDp: Int = 0,
    location: String = "game_bottom"
) {
    val context = LocalContext.current
    var adLoadedSuccessfully by remember { mutableStateOf(false) }
    var isFailedToLoad by remember { mutableStateOf(false) }

    // Compute dynamic adaptive height based on screen width minus container padding
    val screenWidth = context.resources.configuration.screenWidthDp
    val widthDp = remember(screenWidth, paddingDp) { (screenWidth - paddingDp).coerceAtLeast(320) }
    val adSize = remember(widthDp) { BannerAdSize.stickySize(context, widthDp) }
    val adHeightDp = remember(adSize) {
        val heightInPx = adSize.getHeightInPixels(context)
        val density = context.resources.displayMetrics.density
        if (density > 0) (heightInPx / density).toInt() else 50
    }

    val boxModifier = if (adLoadedSuccessfully) {
        modifier
            .fillMaxWidth()
            .height(adHeightDp.dp)
            .background(Color(0xFF16181A))
            .clip(RoundedCornerShape(8.dp))
    } else {
        modifier
            .fillMaxWidth()
            .height(if (isFailedToLoad) 0.dp else adHeightDp.dp)
    }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        if (!isFailedToLoad) {
            key(adUnitId) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        Log.d("YandexAds_Banner", "BannerAdView created for adUnitId: $adUnitId")
                        BannerAdView(ctx).apply {
                            setAdUnitId(adUnitId)
                            setAdSize(adSize)
                            setBannerAdEventListener(object : BannerAdEventListener {
                                override fun onAdLoaded() {
                                    Log.d("YandexAds_Banner", "onAdLoaded triggered successfully for adUnitId: $adUnitId")
                                    adLoadedSuccessfully = true
                                    isFailedToLoad = false
                                    com.example.game.GameAnalytics.trackAdShowed("banner", location)
                                }

                                override fun onAdFailedToLoad(error: AdRequestError) {
                                    val errMsg = "${error.description} (code: ${error.code})"
                                    Log.e("YandexAds_Banner", "onAdFailedToLoad triggered for adUnitId: $adUnitId. Error: $errMsg")
                                    isFailedToLoad = true
                                    adLoadedSuccessfully = false
                                }

                                override fun onAdClicked() {
                                    Log.d("YandexAds_Banner", "onAdClicked triggered for adUnitId: $adUnitId")
                                    com.example.game.GameAnalytics.trackAdClicked("banner", location)
                                }

                                override fun onLeftApplication() {
                                    Log.d("YandexAds_Banner", "onLeftApplication triggered for adUnitId: $adUnitId")
                                }

                                override fun onReturnedToApplication() {
                                    Log.d("YandexAds_Banner", "onReturnedToApplication triggered for adUnitId: $adUnitId")
                                }

                                override fun onImpression(impressionData: ImpressionData?) {
                                    Log.d("YandexAds_Banner", "onImpression triggered for adUnitId: $adUnitId. RawData: ${impressionData?.rawData}")
                                }
                            })
                            Log.d("YandexAds_Banner", "Calling loadAd() for adUnitId: $adUnitId")
                            loadAd(AdRequest.Builder().build())
                        }
                    },
                    update = { /* No-op */ }
                )
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

@Composable
fun InterstitialAdManager(
    adUnitId: String,
    isEnglish: Boolean,
    purpose: String,
    onAdDismissedOrFailed: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1113).copy(alpha = 0.9f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = Color(0xFF3B82F6), strokeWidth = 4.dp)
            Text(
                text = if (isEnglish) "Loading advertisement..." else "Загрузка рекламы...",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    LaunchedEffect(adUnitId) {
        if (activity == null) {
            onAdDismissedOrFailed()
            return@LaunchedEffect
        }

        try {
            val interstitialAdLoader = InterstitialAdLoader(context)
            interstitialAdLoader.setAdLoadListener(object : InterstitialAdLoadListener {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d("YandexAds_Interstitial", "onAdLoaded triggered successfully for adUnitId: $adUnitId")
                    interstitialAd.setAdEventListener(object : InterstitialAdEventListener {
                        override fun onAdShown() {
                            Log.d("YandexAds_Interstitial", "onAdShown triggered for adUnitId: $adUnitId")
                            com.example.game.GameAnalytics.trackAdShowed("interstitial", purpose)
                        }
                        override fun onAdFailedToShow(adError: AdError) {
                            Log.e("YandexAds_Interstitial", "onAdFailedToShow triggered for adUnitId: $adUnitId. Error: ${adError.description}")
                            onAdDismissedOrFailed()
                        }
                        override fun onAdDismissed() {
                            Log.d("YandexAds_Interstitial", "onAdDismissed triggered for adUnitId: $adUnitId")
                            onAdDismissedOrFailed()
                        }
                        override fun onAdClicked() {
                            Log.d("YandexAds_Interstitial", "onAdClicked triggered for adUnitId: $adUnitId")
                            com.example.game.GameAnalytics.trackAdClicked("interstitial", purpose)
                        }
                        override fun onAdImpression(impressionData: ImpressionData?) {
                            Log.d("YandexAds_Interstitial", "onAdImpression triggered for adUnitId: $adUnitId. RawData: ${impressionData?.rawData}")
                        }
                    })
                    interstitialAd.show(activity)
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    Log.e("YandexAds_Interstitial", "onAdFailedToLoad triggered for adUnitId: $adUnitId. Error: ${error.description} (code: ${error.code})")
                    onAdDismissedOrFailed()
                }
            })

            val adRequestConfiguration = AdRequestConfiguration.Builder(adUnitId).build()
            interstitialAdLoader.loadAd(adRequestConfiguration)
        } catch (e: Exception) {
            onAdDismissedOrFailed()
        }
    }
}

@Composable
fun RewardedAdManager(
    adUnitId: String,
    isEnglish: Boolean,
    purpose: String,
    onRewarded: () -> Unit,
    onAdFailedOrClosed: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1113).copy(alpha = 0.95f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(color = Color(0xFFFBBF24), strokeWidth = 4.dp)
            Text(
                text = if (isEnglish) "Loading rewarded video..." else "Загрузка видео с наградой...",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    LaunchedEffect(adUnitId) {
        if (activity == null) {
            onAdFailedOrClosed()
            return@LaunchedEffect
        }

        try {
            val rewardedAdLoader = RewardedAdLoader(context)
            rewardedAdLoader.setAdLoadListener(object : RewardedAdLoadListener {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    Log.d("YandexAds_Rewarded", "onAdLoaded triggered successfully for adUnitId: $adUnitId")
                    var rewarded = false
                    rewardedAd.setAdEventListener(object : RewardedAdEventListener {
                        override fun onAdShown() {
                            Log.d("YandexAds_Rewarded", "onAdShown triggered for adUnitId: $adUnitId")
                            com.example.game.GameAnalytics.trackAdShowed("rewarded", purpose)
                        }
                        override fun onAdFailedToShow(adError: AdError) {
                            Log.e("YandexAds_Rewarded", "onAdFailedToShow triggered for adUnitId: $adUnitId. Error: ${adError.description}")
                            onAdFailedOrClosed()
                        }
                        override fun onAdDismissed() {
                            Log.d("YandexAds_Rewarded", "onAdDismissed triggered for adUnitId: $adUnitId. Reward granted: $rewarded")
                            if (rewarded) {
                                onRewarded()
                            } else {
                                onAdFailedOrClosed()
                            }
                        }
                        override fun onAdClicked() {
                            Log.d("YandexAds_Rewarded", "onAdClicked triggered for adUnitId: $adUnitId")
                            com.example.game.GameAnalytics.trackAdClicked("rewarded", purpose)
                        }
                        override fun onAdImpression(impressionData: ImpressionData?) {
                            Log.d("YandexAds_Rewarded", "onAdImpression triggered for adUnitId: $adUnitId. RawData: ${impressionData?.rawData}")
                        }
                        override fun onRewarded(reward: Reward) {
                            Log.d("YandexAds_Rewarded", "onRewarded triggered for adUnitId: $adUnitId. Reward amount: ${reward.amount}, type: ${reward.type}")
                            rewarded = true
                        }
                    })
                    rewardedAd.show(activity)
                }

                override fun onAdFailedToLoad(error: AdRequestError) {
                    Log.e("YandexAds_Rewarded", "onAdFailedToLoad triggered for adUnitId: $adUnitId. Error: ${error.description} (code: ${error.code})")
                    onAdFailedOrClosed()
                }
            })

            val adRequestConfiguration = AdRequestConfiguration.Builder(adUnitId).build()
            rewardedAdLoader.loadAd(adRequestConfiguration)
        } catch (e: Exception) {
            onAdFailedOrClosed()
        }
    }
}

@Composable
fun QuestsDialog(viewModel: GameViewModel) {
    val isEnglish = viewModel.isEnglish
    val activeTab = viewModel.activeQuestTab
    val questList by viewModel.questStates.collectAsState()
    val achievementList by viewModel.achievementStates.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A0C16))
            .clickable { viewModel.showQuestsDialog = false },
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131524)),
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clickable(enabled = false) {},
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "MISSIONS & REWARDS" else "ЗАДАНИЯ И НАГРАДЫ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF3B82F6),
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = { viewModel.showQuestsDialog = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Selectors (Daily, Weekly, Achievements)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E223A), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf(
                        if (isEnglish) "Daily" else "Дневные",
                        if (isEnglish) "Weekly" else "Недели",
                        if (isEnglish) "Achievements" else "Достижения"
                    )
                    tabs.forEachIndexed { index, title ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (activeTab == index) Color(0xFF3B82F6) else Color.Transparent)
                                .clickable { viewModel.activeQuestTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == index) Color.White else Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // List Items Container
                Box(modifier = Modifier.weight(1f)) {
                    if (activeTab == 0 || activeTab == 1) {
                        // Filter Quests
                        val typeFilter = if (activeTab == 0) "DAILY" else "WEEKLY"
                        val filteredQuests = questList.filter { it.type == typeFilter }

                        if (filteredQuests.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isEnglish) "No quests available" else "Нет активных заданий",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(filteredQuests) { quest ->
                                    QuestRowItem(
                                        title = if (isEnglish) quest.titleEn else quest.titleRu,
                                        current = quest.current,
                                        target = quest.target,
                                        rewardCoins = quest.rewardCoins,
                                        rewardDiamonds = quest.rewardDiamonds,
                                        isCompleted = quest.isCompleted,
                                        isClaimed = quest.isClaimed,
                                        onClaimClicked = { viewModel.claimQuestReward(quest.id) },
                                        isEnglish = isEnglish
                                    )
                                }
                            }
                        }
                    } else {
                        // Achievements tab
                        if (achievementList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (isEnglish) "No achievements available" else "Нет достижений",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(achievementList) { ach ->
                                    QuestRowItem(
                                        title = if (isEnglish) ach.titleEn else ach.titleRu,
                                        current = ach.current,
                                        target = ach.target,
                                        rewardCoins = ach.rewardCoins,
                                        rewardDiamonds = ach.rewardDiamonds,
                                        isCompleted = ach.isCompleted,
                                        isClaimed = ach.isClaimed,
                                        onClaimClicked = { viewModel.claimAchievementReward(ach.id) },
                                        isEnglish = isEnglish
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuestRowItem(
    title: String,
    current: Int,
    target: Int,
    rewardCoins: Int,
    rewardDiamonds: Int,
    isCompleted: Boolean,
    isClaimed: Boolean,
    onClaimClicked: () -> Unit,
    isEnglish: Boolean
) {
    val progressRatio = (current.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val opacity = if (isClaimed) 0.5f else 1.0f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1E32)),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(opacity)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = if (isClaimed) {
                            if (isEnglish) "Claimed" else "Получено"
                        } else {
                            "$current / $target"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) Color(0xFF10B981) else Color.Gray
                    )
                }

                // Reward values
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    if (rewardCoins > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "+$rewardCoins", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFFBBF24))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "🪙", fontSize = 12.sp)
                        }
                    }
                    if (rewardDiamonds > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "+$rewardDiamonds", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF60A5FA))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "💎", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Progress Bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressRatio)
                            .background(
                                if (isClaimed) Color.Gray 
                                else if (isCompleted) Color(0xFF10B981) 
                                else Color(0xFF2563EB)
                            )
                    )
                }

                // Action Button (Claim, Claimed icon, or Locked status)
                if (isClaimed) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Claimed",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isCompleted) {
                    Button(
                        onClick = onClaimClicked,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                            contentColor = Color(0xFF10B981)
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = "Claim Reward",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isEnglish) "CLAIM" else "ЗАБРАТЬ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10B981)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "In Progress",
                        tint = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShopSoonDialog(viewModel: GameViewModel) {
    val isEnglish = viewModel.isEnglish
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A0C16))
            .clickable { viewModel.showShopSoonDialog = false },
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131524)),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(0.9f)
                .clickable(enabled = false) {},
            elevation = CardDefaults.cardElevation(defaultElevation = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "SHOP / BOOSTER LAB" else "МАГАЗИН / ЛАБОРАТОРИЯ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFD97706),
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = { viewModel.showShopSoonDialog = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.LightGray)
                    }
                }

                Divider(color = Color(0xFF2E3338), modifier = Modifier.padding(vertical = 12.dp))

                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Store",
                    tint = Color(0xFFD97706).copy(alpha = 0.15f),
                    modifier = Modifier.size(96.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .background(Color(0xFFD97706).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = if (isEnglish) "COMING SOON" else "СКОРО ОТКРЫТИЕ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFBBF24),
                        letterSpacing = 2.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isEnglish) {
                        "Our engineering team is refining the Booster Store! Get ready for special arrow skins, coin double-packs, and power-up bundles."
                    } else {
                        "Наша инженерная группа настраивает Бустер-Магазин! Готовьтесь к особым скинам для стрелочек, двойным пакам монет и наборам усилений."
                    },
                    fontSize = 13.sp,
                    color = Color(0xFF8F9BB3),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().alpha(0.35f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("Starter pack", "Medium Chest", "Diamond Vault").forEach { pkg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1E32)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = pkg, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = "🪙 100", fontSize = 10.sp, color = Color(0xFFFBBF24))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.showShopSoonDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD97706).copy(alpha = 0.15f),
                        contentColor = Color(0xFFD97706)
                    ),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirm",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "OK, I'LL WAIT!" else "ОК, БУДУ ЖДАТЬ!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                }
            }
        }
    }
}

// ==========================================
// FULL-SCREEN SEPARATE GAME MODULES (PAGES)
// ==========================================

@Composable
fun BattlePassScreen(viewModel: GameViewModel) {
    val isEnglish = viewModel.isEnglish
    val level = viewModel.seasonLevel
    val xp = viewModel.seasonXp
    val premium = viewModel.hasPremiumPass

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = if (isEnglish) "SEASON BATTLE PASS" else "БОЕВОЙ ПРОПУСК СЕЗОНА",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Level & XP Progress Card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEnglish) "Season Level $level" else "Сезонный уровень $level",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isEnglish) "Clear quests to earn Season XP" else "Проходите квесты для сезонного опыта",
                            fontSize = 12.sp,
                            color = Color(0xFFC7D2FE)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF312E81), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$xp / 100 XP",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF818CF8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // XP Progress Bar
                LinearProgressIndicator(
                    progress = { (xp.toFloat() / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = Color(0xFF818CF8),
                    trackColor = Color(0xFF312E81)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Premium Golden Pass Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1B10)),
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(text = "👑", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isEnglish) "GOLDEN PREMIUM PASS" else "ЗОЛОТОЙ ПРЕМИУМ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFBBF24),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isEnglish) "Unlock exclusive golden cosmetic rewards" else "Разблокируйте золотые особые награды",
                            fontSize = 10.sp,
                            color = Color(0xFFFDE68A)
                        )
                    }
                }
                if (premium) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFBBF24).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "ACTIVE" else "АКТИВЕН",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFBBF24)
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.buyPremiumPass() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFBBF24).copy(alpha = 0.15f),
                            contentColor = Color(0xFFFBBF24)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "💎 25",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFBBF24)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Level Tiers List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(10) { idx ->
                val tier = idx + 1
                val isUnlocked = level >= tier
                val isFreeClaimed = viewModel.claimedSeasonRewards.contains(tier)
                val isPremClaimed = viewModel.claimedSeasonPremiumRewards.contains(tier)

                val freeReward = viewModel.getFreeRewardForLevel(tier)
                val premReward = viewModel.getPremiumRewardForLevel(tier)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131524)),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Card Header: Tier number
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(if (isUnlocked) Color(0xFF312E81) else Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "TIER $tier" else "УРОВЕНЬ $tier",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isUnlocked) Color(0xFF818CF8) else Color.Gray,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Two Columns
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Column: Free
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color.White.copy(alpha = 0.02f), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "FREE" else "БЕСПЛАТНО",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray,
                                    letterSpacing = 0.5.sp
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(text = if (freeReward.first == "coins") "🪙" else "💎", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${freeReward.second}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }

                                if (isFreeClaimed) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                            .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isEnglish) "CLAIMED ✓" else "ПОЛУЧЕНО ✓",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                } else if (isUnlocked) {
                                    Button(
                                        onClick = { viewModel.claimFreeReward(tier) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                                            contentColor = Color(0xFF10B981)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(28.dp)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "CLAIM" else "ЗАБРАТЬ",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF10B981)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = Color.Gray.copy(alpha = 0.6f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            // Right Column: Premium
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0xFF2D1B10).copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "PREMIUM" else "ПРЕМИУМ",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFBBF24),
                                    letterSpacing = 0.5.sp
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = when (premReward.first) {
                                            "coins" -> "🪙"
                                            "diamonds" -> "💎"
                                            else -> "🌟"
                                        },
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (premReward.first == "cosmetic_skin") {
                                            if (isEnglish) "Gold Skin" else "Скин Золото"
                                        } else {
                                            "${premReward.second}"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFBBF24)
                                    )
                                }

                                if (isPremClaimed) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                            .background(Color(0xFFFBBF24).copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isEnglish) "CLAIMED ✓" else "ПОЛУЧЕНО ✓",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFBBF24)
                                        )
                                    }
                                } else if (isUnlocked && premium) {
                                    Button(
                                        onClick = { viewModel.claimPremiumReward(tier) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFBBF24).copy(alpha = 0.15f),
                                            contentColor = Color(0xFFFBBF24)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(28.dp)
                                    ) {
                                        Text(
                                            text = if (isEnglish) "CLAIM" else "ЗАБРАТЬ",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFBBF24)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(28.dp)
                                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Locked",
                                            tint = Color(0xFFFBBF24).copy(alpha = 0.4f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementsScreen(viewModel: GameViewModel) {
    val isEnglish = viewModel.isEnglish
    val achievementList by viewModel.achievementStates.collectAsState()
    
    val totalUnlocks = viewModel.unlockedSkins.size + viewModel.unlockedThemes.size + viewModel.unlockedBackgrounds.size + viewModel.unlockedUiStyles.size + viewModel.unlockedSounds.size + viewModel.unlockedParticles.size
    val completedCount = achievementList.count { it.isCompleted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Navigation Header - Flat slate bg
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { viewModel.navigateBack() },
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (isEnglish) "ACHIEVEMENTS" else "ДОСТИЖЕНИЯ",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bento Stats Block
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Completed card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2238)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isEnglish) "COMPLETED" else "ВЫПОЛНЕНО",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$completedCount / ${achievementList.size}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF10B981)
                    )
                }
            }

            // Collection unlocks card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2238)),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isEnglish) "COLLECTION" else "КОЛЛЕКЦИЯ",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$totalUnlocks / 90",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF60A5FA)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Achievements List
        if (achievementList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isEnglish) "No achievements available" else "Нет достижений",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(achievementList) { ach ->
                    QuestRowItem(
                        title = if (isEnglish) ach.titleEn else ach.titleRu,
                        current = ach.current,
                        target = ach.target,
                        rewardCoins = ach.rewardCoins,
                        rewardDiamonds = ach.rewardDiamonds,
                        isCompleted = ach.isCompleted,
                        isClaimed = ach.isClaimed,
                        onClaimClicked = { viewModel.claimAchievementReward(ach.id) },
                        isEnglish = isEnglish
                    )
                }
            }
        }
    }
}

@Composable
fun DailyQuestsScreen(viewModel: GameViewModel) {
    val isEnglish = viewModel.isEnglish
    val questList by viewModel.questStates.collectAsState()
    val dailyQuests = questList.filter { it.type == "DAILY" }

    val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
    val isStreakClaimedToday = viewModel.lastDailyStreakClaimDate == todayStr

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isEnglish) "DAILY QUESTS" else "ЕЖЕДНЕВНЫЕ ЗАДАНИЯ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // 7 Days Daily Streak Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131524)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) "7-DAY LOGIN STREAK" else "7-ДНЕВНЫЙ СТРИК ВХОДА",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B82F6)
                            )
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Day ${viewModel.dailyStreak}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Yellow
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Row of 7 days
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (day in 1..7) {
                                val isPast = if (isStreakClaimedToday) {
                                    if (viewModel.dailyStreak == 1) true else day < viewModel.dailyStreak
                                } else {
                                    day < viewModel.dailyStreak
                                }
                                val isCurrent = if (isStreakClaimedToday) {
                                    false
                                } else {
                                    day == viewModel.dailyStreak
                                }
                                val dayColor = when {
                                    isPast -> Color(0xFF10B981)
                                    isCurrent -> Color(0xFF3B82F6)
                                    else -> Color(0xFF334155)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(42.dp)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(dayColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(text = "D$day", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isPast) "✓" else if (day == 7) "👑" else "🪙",
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Claim Button spanning the full width of the block
                        Button(
                            onClick = { if (!isStreakClaimedToday) viewModel.claimDailyStreak() },
                            enabled = !isStreakClaimedToday,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                                contentColor = Color(0xFF10B981),
                                disabledContainerColor = Color.White.copy(alpha = 0.05f),
                                disabledContentColor = Color.White.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isStreakClaimedToday) Icons.Default.Check else Icons.Default.CardGiftcard,
                                contentDescription = "Daily Reward Icon",
                                tint = if (isStreakClaimedToday) Color.Gray else Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isStreakClaimedToday) {
                                    if (isEnglish) "CLAIMED TODAY" else "УЖЕ ПОЛУЧЕНО"
                                } else {
                                    if (isEnglish) "CLAIM DAILY REWARD" else "ПОЛУЧИТЬ НАГРАДУ"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isStreakClaimedToday) Color.Gray else Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                Text(
                    text = if (isEnglish) "ACTIVE DAILY TASKS" else "АКТИВНЫЕ ЗАДАНИЯ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.LightGray,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            if (dailyQuests.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isEnglish) "No quests available" else "Нет активных заданий",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(dailyQuests) { quest ->
                    QuestRowItem(
                        title = if (isEnglish) quest.titleEn else quest.titleRu,
                        current = quest.current,
                        target = quest.target,
                        rewardCoins = quest.rewardCoins,
                        rewardDiamonds = quest.rewardDiamonds,
                        isCompleted = quest.isCompleted,
                        isClaimed = quest.isClaimed,
                        onClaimClicked = { viewModel.claimQuestReward(quest.id) },
                        isEnglish = isEnglish
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyQuestsScreen(viewModel: GameViewModel) {
    val isEnglish = viewModel.isEnglish
    val questList by viewModel.questStates.collectAsState()
    val weeklyQuests = questList.filter { it.type == "WEEKLY" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = if (isEnglish) "WEEKLY MISSIONS" else "ЕЖЕНЕДЕЛЬНЫЕ ЗАДАНИЯ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weekly Milestones Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131524)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (isEnglish) "WEEKLY BATTLE PASS MILESTONES" else "ВЕХИ ЕЖЕНЕДЕЛЬНОГО ПРОПУСКА",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF60A5FA)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val completedWeeklyCount = weeklyQuests.count { it.isCompleted }
                val progressPercent = if (weeklyQuests.isEmpty()) 0f else (completedWeeklyCount.toFloat() / weeklyQuests.size.toFloat())
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (isEnglish) "Tasks Completed" else "Заданий выполнено", fontSize = 12.sp, color = Color.LightGray)
                    Text(text = "$completedWeeklyCount / ${weeklyQuests.size}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E293B))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressPercent)
                            .fillMaxHeight()
                            .background(Color(0xFF3B82F6))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (weeklyQuests.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isEnglish) "No missions available" else "Нет активных миссий",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(weeklyQuests) { quest ->
                    QuestRowItem(
                        title = if (isEnglish) quest.titleEn else quest.titleRu,
                        current = quest.current,
                        target = quest.target,
                        rewardCoins = quest.rewardCoins,
                        rewardDiamonds = quest.rewardDiamonds,
                        isCompleted = quest.isCompleted,
                        isClaimed = quest.isClaimed,
                        onClaimClicked = { viewModel.claimQuestReward(quest.id) },
                        isEnglish = isEnglish
                    )
                }
            }
        }
    }
}

@Composable
fun ShopScreen(viewModel: GameViewModel) {
    val isEnglish = viewModel.isEnglish
    var activeTab by remember { mutableStateOf(0) } // 0 = Skins, 1 = Themes, 2 = UI Styles, 3 = Sounds, 4 = Particles

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Navigation Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier
                    .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = if (isEnglish) "COSMETIC SHOP" else "МАГАЗИН",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Restore defaults card - Prominent Red Button
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDC2626)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.restoreDefaultStyle() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isEnglish) "♻️ RESTORE DEFAULT THEMES & STYLES" else "♻️ СБРОСИТЬ ВСЕ СТИЛИ ПО УМОЛЧАНИЮ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            // 5-Category Tabs selector
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF131524), RoundedCornerShape(14.dp))
                        .padding(6.dp)
                ) {
                    val row1 = listOf(
                        if (isEnglish) "Skins" else "Скины",
                        if (isEnglish) "Themes" else "Темы",
                        if (isEnglish) "UI" else "Стили"
                    )
                    val row2 = listOf(
                        if (isEnglish) "Sounds" else "Звуки",
                        if (isEnglish) "Effects" else "Эффекты"
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        row1.forEachIndexed { idx, title ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (activeTab == idx) Color(0xFF3B82F6) else Color.Transparent)
                                    .clickable { activeTab = idx }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (activeTab == idx) Color.White else Color.Gray)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        row2.forEachIndexed { idx, title ->
                            val actualIdx = idx + 3
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (activeTab == actualIdx) Color(0xFF3B82F6) else Color.Transparent)
                                    .clickable { activeTab = actualIdx }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (activeTab == actualIdx) Color.White else Color.Gray)
                            }
                        }
                    }
                }
            }

            // Shop Items container
            item {
                val cosmeticList = when (activeTab) {
                    0 -> listOf(
                        ShopCosmeticItem("skin_default", if (isEnglish) "Aero Default" else "Аэро Стандарт", 0, "coins", "skin", "default", "🚀"),
                        ShopCosmeticItem("skin_wood", if (isEnglish) "Warm Oak" else "Теплое Дерево", 100, "coins", "skin", "wood", "🪵"),
                        ShopCosmeticItem("skin_plasma", if (isEnglish) "Hot Plasma" else "Горячая Плазма", 200, "coins", "skin", "plasma", "🔥"),
                        ShopCosmeticItem("skin_cyber", if (isEnglish) "Cyber Vector" else "Кибер Вектор", 300, "coins", "skin", "cyber", "⚡"),
                        ShopCosmeticItem("skin_metal", if (isEnglish) "Polished Steel" else "Полированная Сталь", 150, "coins", "skin", "metal", "🔩"),
                        ShopCosmeticItem("skin_titanium", if (isEnglish) "Titanium Alloy" else "Титановый Сплав", 250, "coins", "skin", "titanium", "🛡️"),
                        ShopCosmeticItem("skin_golden", if (isEnglish) "Prestige Gold" else "Золотой Престиж", 25, "diamonds", "skin", "golden", "🌟"),
                        ShopCosmeticItem("skin_crystal", if (isEnglish) "Shattered Crystal" else "Осколочный Кристалл", 35, "diamonds", "skin", "crystal", "💎"),
                        ShopCosmeticItem("skin_prestige_diamond", if (isEnglish) "Prestige Diamond" else "Престижный Алмаз", 50, "diamonds", "skin", "prestige_diamond", "✨")
                    )
                    1 -> listOf(
                        ShopCosmeticItem("theme_classic", if (isEnglish) "Classic Slate" else "Классический Грифель", 0, "coins", "theme", "classic", "🎨"),
                        ShopCosmeticItem("theme_neon", if (isEnglish) "Neon Glow" else "Неоновое Свечение", 150, "coins", "theme", "neon", "🔮"),
                        ShopCosmeticItem("theme_luxury", if (isEnglish) "Golden Luxury" else "Золотой Люкс", 250, "coins", "theme", "luxury", "👑"),
                        ShopCosmeticItem("theme_ice", if (isEnglish) "Chilled Ice" else "Холодный Лед", 350, "coins", "theme", "ice", "❄️"),
                        ShopCosmeticItem("theme_lava", if (isEnglish) "Volcanic Lava" else "Вулканическая Лава", 450, "coins", "theme", "lava", "🌋"),
                        ShopCosmeticItem("theme_galaxy", if (isEnglish) "Cosmic Galaxy" else "Космическая Галактика", 550, "coins", "theme", "galaxy", "🌌"),
                        ShopCosmeticItem("theme_cyberpunk", if (isEnglish) "Cyberpunk Neon" else "Киберпанк", 650, "coins", "theme", "cyberpunk", "🤖"),
                        ShopCosmeticItem("theme_forest", if (isEnglish) "Emerald Forest" else "Изумрудный Лес", 750, "coins", "theme", "forest", "🌲"),
                        ShopCosmeticItem("theme_steampunk", if (isEnglish) "Steampunk Brass" else "Стимпанк Латунь", 850, "coins", "theme", "steampunk", "⚙️"),
                        ShopCosmeticItem("theme_terminal", if (isEnglish) "Retro Terminal" else "Ретро Терминал", 950, "coins", "theme", "terminal", "📟"),
                        ShopCosmeticItem("theme_royal", if (isEnglish) "Royal Purple" else "Королевский Пурпур", 1050, "coins", "theme", "royal", "🍷")
                    )
                    2 -> listOf(
                        ShopCosmeticItem("ui_classic", if (isEnglish) "Classic Border" else "Классический закругленный", 0, "coins", "uistyle", "ui_classic", "📱"),
                        ShopCosmeticItem("ui_sharp", if (isEnglish) "Neo Sharp" else "Строгий Острый", 150, "coins", "uistyle", "ui_sharp", "📐"),
                        ShopCosmeticItem("ui_neumorph", if (isEnglish) "Neumorphic Soft" else "Мягкий Неоморфизм", 250, "coins", "uistyle", "ui_neumorph", "☁️"),
                        ShopCosmeticItem("ui_chrome", if (isEnglish) "Future Chrome" else "Жидкий Хром", 350, "coins", "uistyle", "ui_chrome", "💿"),
                        ShopCosmeticItem("ui_golden_dust", if (isEnglish) "Golden Sparkle" else "Золотая Пыль", 450, "coins", "uistyle", "ui_golden_dust", "✨"),
                        ShopCosmeticItem("ui_retro_invader", if (isEnglish) "Retro Invader" else "Ретро Захватчик", 15, "diamonds", "uistyle", "ui_retro_invader", "👾"),
                        ShopCosmeticItem("ui_bio_slime", if (isEnglish) "Bio Slime" else "Биологическая Слизь", 20, "diamonds", "uistyle", "ui_bio_slime", "🧪"),
                        ShopCosmeticItem("ui_brass_punk", if (isEnglish) "Steampunk Punk" else "Медный Стимпанк", 25, "diamonds", "uistyle", "ui_brass_punk", "🪙"),
                        ShopCosmeticItem("ui_magma_melt", if (isEnglish) "Magma Melt" else "Плавленая Магма", 30, "diamonds", "uistyle", "ui_magma_melt", "🔥"),
                        ShopCosmeticItem("ui_minimal_slate", if (isEnglish) "Minimal Slate" else "Минимал Грифель", 35, "diamonds", "uistyle", "ui_minimal_slate", "♟️")
                    )
                    3 -> listOf(
                        ShopCosmeticItem("sound_classic", if (isEnglish) "Standard Tap" else "Стандартный Щелчок", 0, "coins", "sound", "classic", "Tap 🎵"),
                        ShopCosmeticItem("sound_synth", if (isEnglish) "Analog Synth" else "Аналоговый Синтезатор", 100, "coins", "sound", "synth", "Synth 🎹"),
                        ShopCosmeticItem("sound_retro", if (isEnglish) "8-Bit Retro" else "Ретро 8-Бит", 200, "coins", "sound", "retro", "Snd 👾"),
                        ShopCosmeticItem("sound_arcade", if (isEnglish) "Arcade Blip" else "Аркадный Блип", 300, "coins", "sound", "arcade", "Arc 🕹️"),
                        ShopCosmeticItem("sound_orchestral", if (isEnglish) "Violin Chord" else "Скрипичный Аккорд", 25, "diamonds", "sound", "orchestral", "Vln 🎻"),
                        ShopCosmeticItem("sound_nature", if (isEnglish) "Wind Chime" else "Музыка Ветра", 35, "diamonds", "sound", "nature", "Chm 🍃")
                    )
                    else -> listOf(
                        ShopCosmeticItem("particle_classic", if (isEnglish) "Default Particle" else "Обычные Частицы", 0, "coins", "particle", "classic", "Prt ✨"),
                        ShopCosmeticItem("particle_stars", if (isEnglish) "Sparkly Stars" else "Сияющие Звезды", 150, "coins", "particle", "stars", "Star ⭐"),
                        ShopCosmeticItem("particle_fire", if (isEnglish) "Neon Trails" else "Неоновые Хвосты", 250, "coins", "particle", "fire", "Fire 💫"),
                        ShopCosmeticItem("particle_ice_crystal", if (isEnglish) "Chilled Ice" else "Ледяные Осколки", 350, "coins", "particle", "ice_crystal", "Ice ❄️"),
                        ShopCosmeticItem("particle_magic_dust", if (isEnglish) "Pixie Sparkles" else "Волшебная Пыль", 20, "diamonds", "particle", "magic_dust", "Dst 🧚"),
                        ShopCosmeticItem("particle_rainbow", if (isEnglish) "Prismatic Glow" else "Радужный След", 30, "diamonds", "particle", "rainbow", "Rnb 🌈")
                    )
                }
                ShopItemsGrid(cosmeticList, viewModel, isEnglish)
            }
        }
    }
}

data class ShopCosmeticItem(
    val id: String,
    val name: String,
    val cost: Int,
    val currency: String, // coins / diamonds / gems
    val type: String, // skin / theme / background / uistyle / sound / particle
    val value: String,
    val icon: String
)

@Composable
fun ShopItemsGrid(items: List<ShopCosmeticItem>, viewModel: GameViewModel, isEnglish: Boolean) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { item ->
            val isUnlocked = when (item.type) {
                "skin" -> viewModel.unlockedSkins.contains(item.value)
                "theme" -> viewModel.unlockedThemes.contains(item.value)
                "background" -> viewModel.unlockedBackgrounds.contains(item.value)
                "uistyle" -> viewModel.unlockedUiStyles.contains(item.value)
                "sound" -> viewModel.unlockedSounds.contains(item.value)
                "particle" -> viewModel.unlockedParticles.contains(item.value)
                else -> false
            }

            val isSelected = when (item.type) {
                "skin" -> viewModel.selectedSkin == item.value
                "theme" -> viewModel.selectedTheme == item.value
                "background" -> viewModel.selectedBackground == item.value
                "uistyle" -> viewModel.selectedUiStyle == item.value
                "sound" -> viewModel.selectedSound == item.value
                "particle" -> viewModel.selectedParticle == item.value
                else -> false
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131524)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = if (item.icon.length > 2) item.icon.substring(0, 2) else item.icon, fontSize = 20.sp)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(text = item.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isUnlocked) {
                                    if (isEnglish) "Unlocked" else "Разблокировано"
                                } else {
                                    "${if (item.currency == "coins") "🪙" else "💎"} ${item.cost}"
                                },
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isEnglish) "ACTIVE" else "АКТИВЕН",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    } else if (isUnlocked) {
                        Button(
                            onClick = {
                                when (item.type) {
                                    "skin" -> viewModel.selectSkin(item.value)
                                    "theme" -> viewModel.selectTheme(item.value)
                                    "background" -> viewModel.selectBackground(item.value)
                                    "uistyle" -> viewModel.selectUiStyle(item.value)
                                    "sound" -> viewModel.selectSound(item.value)
                                    "particle" -> viewModel.selectParticle(item.value)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF3B82F6).copy(alpha = 0.15f),
                                contentColor = Color(0xFF3B82F6)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Use",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEnglish) "USE" else "НАДЕТЬ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF3B82F6)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.purchaseShopItem(item.id, item.cost, item.currency, item.type, item.value)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD97706).copy(alpha = 0.15f),
                                contentColor = Color(0xFFD97706)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Buy",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isEnglish) "BUY" else "КУПИТЬ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFD97706)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RewardCenterScreen(viewModel: GameViewModel) {
    val isEnglish = viewModel.isEnglish
    var claimedSuccessfully by remember { mutableStateOf(false) }
    var showRewardedAdLoader by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = { viewModel.navigateBack() },
                    modifier = Modifier
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (isEnglish) "GIFTS & REWARDS" else "ЦЕНТР ПОДАРКОВ И НАГРАД",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                text = if (isEnglish) {
                    "Welcome to the Gifts & Rewards Center! Here you can watch short sponsor ads to instantly earn gold coins, which can be spent in the shop on awesome new skins, themes, and game styles!"
                } else {
                    "Добро пожаловать в центр подарков и наград! Здесь вы можете смотреть короткую спонсорскую рекламу и мгновенно получать золотые монеты, которые можно потратить в магазине на новые скины, темы и стили оформления игры!"
                },
                fontSize = 13.sp,
                color = Color(0xFF8F9BB3),
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Real Yandex Banner Ad View with paddingDp = 32! Will NEVER cut off!
            YandexBannerAd(
                adUnitId = "R-M-19471149-4",
                paddingDp = 32,
                location = "reward_center_inline",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            // Card 2: Rewarded Action Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131524)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Rewarded ad pill tag
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .align(Alignment.Start)
                    ) {
                        Text(
                            text = if (isEnglish) "SPONSOR AD (YANDEX)" else "СПОНСОРСКАЯ РЕКЛАМА (ЯНДЕКС)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (claimedSuccessfully) {
                        Text(text = "🎁", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (isEnglish) "REWARD CLAIMED SUCCESS!" else "ПОДАРОК УСПЕШНО ПОЛУЧЕН!",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF10B981),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isEnglish) "Added 🪙 100 to your wallet." else "В кошелек добавлено 🪙 100.",
                            fontSize = 13.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { claimedSuccessfully = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                                contentColor = Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp).fillMaxWidth(0.7f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Get More",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEnglish) "GET MORE" else "ПОЛУЧИТЬ ЕЩЕ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (isEnglish) "🪙 WATCH AD FOR COINS" else "🪙 СМОТРЕТЬ РЕКЛАМУ ЗА МОНЕТЫ",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFBBF24)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isEnglish) {
                                    "Watch a short non-skippable sponsor video and instantly get 100 gold coins added to your game balance!"
                                } else {
                                    "Посмотрите короткий спонсорский ролик без возможности пропуска и мгновенно получите 100 золотых монет на свой игровой баланс!"
                                },
                                fontSize = 13.sp,
                                color = Color(0xFF8F9BB3),
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Red Button with White Text explicitly
                        Button(
                            onClick = { showRewardedAdLoader = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                                contentColor = Color(0xFFEF4444)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp).fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Watch ad",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isEnglish) "WATCH AD FOR COINS" else "СМОТРЕТЬ РЕКЛАМУ ЗА МОНЕТЫ",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Exit Button matching Yandex screenshot (large purple button)
            Button(
                onClick = { viewModel.navigateBack() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4F46E5).copy(alpha = 0.15f),
                    contentColor = Color(0xFF818CF8)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = "Exit",
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEnglish) "EXIT" else "ВЫХОД",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF818CF8)
                )
            }
        }

        // Full Screen Rewarded Ad Loader Overlay!
        if (showRewardedAdLoader) {
            RewardedAdManager(
                adUnitId = "R-M-19471149-1",
                isEnglish = isEnglish,
                purpose = "chest_reward",
                onRewarded = {
                    viewModel.claimGiftRewardVideo()
                    claimedSuccessfully = true
                    showRewardedAdLoader = false
                },
                onAdFailedOrClosed = {
                    showRewardedAdLoader = false
                }
            )
        }
    }
}
