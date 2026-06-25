package com.example.game

import androidx.compose.ui.graphics.Color
import java.util.Random

object LevelGenerator {
    private val COLORS = listOf(
        Color(0xFF3B82F6), // Electric Azure Blue
        Color(0xFF06B6D4), // Cyan Glow
        Color(0xFF34D399), // Emerald Neon
        Color(0xFFFBBF24), // Vibrant Amber gold
        Color(0xFFF87171), // Warm Coral Red
        Color(0xFF818CF8), // Lavender Blue Indigo
        Color(0xFFCBD5E1)  // Minimalistic Light Slate
    )

    private val levelCache = java.util.concurrent.ConcurrentHashMap<Int, LevelData>()

    fun generateLevel(levelIndex: Int): LevelData {
        val cached = levelCache[levelIndex]
        if (cached != null) {
            return cached
        }

        val baseSeed = levelIndex.toLong() * 987654L
        val shapes = ShapeType.values()
        val shapeType = shapes[(levelIndex - 1) % shapes.size]

        // Grid size matching the user's request: escalates rapidly with leveling up to level 100 for maximum arrow counts and rich detail!
        val gridSize = when {
            levelIndex <= 5 -> 10     // 10x10 for perfect early learning curves!
            levelIndex <= 15 -> 12    // 12x12
            levelIndex <= 30 -> 14    // 14x14
            levelIndex <= 50 -> 16    // 16x16
            levelIndex <= 75 -> 18    // 18x18
            levelIndex <= 100 -> 20   // 20x20
            else -> {
                // Keep complexity high with 20x20, 22x22, or 24x24 grids!
                val ranSize = Random(baseSeed).nextInt(3)
                20 + ranSize * 2
            }
        }
        val w = gridSize
        val h = gridSize

        // Compile the active shape mask cells using high-quality SVG path rasterizer
        val shapeCells = mutableSetOf<Point>()
        val svgPath = SvgShapeRegistry.paths[shapeType]
        val rasterStatsInfo = if (svgPath != null) {
            getSvgRasterStats(svgPath, w, h).details
        } else {
            "Non-SVG procedural shape"
        }

        if (svgPath != null) {
            shapeCells.addAll(getSilhouetteFromSvg(svgPath, w, h))
        } else {
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val p = Point(x, y)
                    if (isPointInShape(p, shapeType, w, h)) {
                        shapeCells.add(p)
                    }
                }
            }
        }

        val originalSizeBeforeFallback = shapeCells.size
        var defensiveFallbackUsed = false

        // Defensive fallback: If shape mask is too small or empty, fill the central area
        if (shapeCells.size < 8) {
            defensiveFallbackUsed = true
            val cx = w / 2
            val cy = h / 2
            val rSize = (gridSize / 3).coerceAtLeast(2)
            for (y in (cy - rSize)..(cy + rSize)) {
                for (x in (cx - rSize)..(cx + rSize)) {
                    if (x in 0 until w && y in 0 until h) {
                        shapeCells.add(Point(x, y))
                    }
                }
            }
        }

        var bestArrows = emptyList<Arrow>()
        var maxDifficulty = -1.0f

        var seedOffset = 0L
        var attempts = 0
        var totalBacktrackCount = 0
        var strategyUsed = "None"
        while (bestArrows.isEmpty() && attempts < 30) {
            val ran = Random(baseSeed + seedOffset)
            val lengthScale = when {
                attempts < 5 -> 1.0f
                attempts < 10 -> 0.7f
                attempts < 20 -> 0.4f
                else -> 0.2f
            }

            for (partitionTrial in 0 until 40) {
                val trialRan = Random(baseSeed + seedOffset + partitionTrial * 2026L)
                val unvisited = shapeCells.toMutableSet()
                val paths = mutableListOf<MutableList<Point>>()

                while (unvisited.isNotEmpty()) {
                    // Heuristic: Start from a node with the fewest remaining unvisited neighbors (Warnsdorff's heuristic)
                    val start = unvisited.minByOrNull { pt ->
                        getNeighbors(pt, w, h).count { it in unvisited }
                    } ?: break

                    val path = mutableListOf<Point>()
                    path.add(start)
                    unvisited.remove(start)

                    // 1. Roll the target length for this path from a varied probability distribution
                    val roll = trialRan.nextFloat()
                    val targetLength = when {
                        roll < 0.32f -> 12 + trialRan.nextInt(11) // Extra Long: 12..22
                        roll < 0.70f -> 6 + trialRan.nextInt(6)   // Medium: 6..11
                        else -> 3 + trialRan.nextInt(3)           // Short: 3..5
                    }.let { (it * lengthScale).toInt().coerceAtLeast(3) }

                    var current = start
                    while (path.size < targetLength) {
                        val nextCandidates = getNeighbors(current, w, h).filter { it in unvisited }
                        if (nextCandidates.isEmpty()) break

                        val nextPT = if (path.size >= 2) {
                            val prevDir = current - path[path.size - 2]
                            val straightCandidates = nextCandidates.filter { (it - current) == prevDir }
                            
                            // 75% chance to continue straight, maintaining clean linear corridors
                            if (straightCandidates.isNotEmpty() && trialRan.nextFloat() < 0.75f) {
                                straightCandidates.first()
                            } else {
                                // Otherwise, choose a turn, preferring nodes that don't block/trap
                                val otherCandidates = nextCandidates.filter { (it - current) != prevDir }
                                if (otherCandidates.isNotEmpty()) {
                                    otherCandidates.minByOrNull { pt ->
                                        getNeighbors(pt, w, h).count { it in unvisited }
                                    }!!
                                } else {
                                    nextCandidates.minByOrNull { pt ->
                                        getNeighbors(pt, w, h).count { it in unvisited }
                                    }!!
                                }
                            }
                        } else {
                            // First step: pick neighbor preferring outer boundaries of shape first
                            nextCandidates.minByOrNull { pt ->
                                getNeighbors(pt, w, h).count { it in unvisited }
                            }!!
                        }

                        path.add(nextPT)
                        unvisited.remove(nextPT)
                        current = nextPT
                    }

                    paths.add(path)
                }

                // --- Robust Split & Merge Stage for Singletons ---
                val validPaths = paths.filter { it.size >= 2 }.map { it.toMutableList() }.toMutableList()
                val singletons = paths.filter { it.size == 1 }.map { it.first() }.toMutableList()

                var mergeFailed = false
                for (singleton in singletons) {
                    var resolved = false
                    val neighbors = getNeighbors(singleton, w, h).filter { it in shapeCells }.shuffled(trialRan)

                    // 1. Try appending/prepending to an existing short path (if length < 6)
                    for (nb in neighbors) {
                        val adjPath = validPaths.find { (it.first() == nb || it.last() == nb) && it.size < 6 }
                        if (adjPath != null) {
                            if (adjPath.first() == nb) {
                                adjPath.add(0, singleton)
                            } else {
                                adjPath.add(singleton)
                            }
                            resolved = true
                            break
                        }
                    }

                    // 2. Try stealing/splitting an endpoint or middle node from a larger path
                    if (!resolved) {
                        for (nb in neighbors) {
                            val ownerPath = validPaths.find { it.contains(nb) } ?: continue
                            if (ownerPath.size >= 4) {
                                if (ownerPath.first() == nb) {
                                    ownerPath.removeAt(0)
                                    validPaths.add(mutableListOf(singleton, nb))
                                    resolved = true
                                    break
                                } else if (ownerPath.last() == nb) {
                                    ownerPath.removeAt(ownerPath.size - 1)
                                    validPaths.add(mutableListOf(singleton, nb))
                                    resolved = true
                                    break
                                } else {
                                    // Split owner path into two subpaths at nb
                                    val idx = ownerPath.indexOf(nb)
                                    val part1 = ownerPath.subList(0, idx).toMutableList()
                                    val part2 = ownerPath.subList(idx + 1, ownerPath.size).toMutableList()
                                    if (part1.size >= 2 && part2.size >= 2) {
                                        validPaths.remove(ownerPath)
                                        validPaths.add(part1)
                                        validPaths.add(part2)
                                        validPaths.add(mutableListOf(singleton, nb))
                                        resolved = true
                                        break
                                    }
                                }
                            }
                        }
                    }

                    // 3. Last fallback: Simply append or prepend to an adjacent path endpoint
                    if (!resolved) {
                        for (nb in neighbors) {
                            val ownerPath = validPaths.find { (it.first() == nb || it.last() == nb) && it.size < 12 }
                            if (ownerPath != null) {
                                if (ownerPath.first() == nb) {
                                    ownerPath.add(0, singleton)
                                } else {
                                    ownerPath.add(singleton)
                                }
                                resolved = true
                                break
                            }
                        }
                    }

                    if (!resolved) {
                        // Instead of rejecting the layout, we simply ignore this isolated cell.
                        // This prevents empty levels and maintains highly detailed, solvable grids.
                        resolved = true
                    }
                }

                if (mergeFailed) {
                    continue // Try another partition trial
                }

                // Backtracking-based Direction Assignment with incremental pruning for perfect solvability
                var hasUnusablePath = false
                for (path in validPaths) {
                    if (isPathSelfBlocking(path, w, h) && isPathSelfBlocking(path.asReversed(), w, h)) {
                        hasUnusablePath = true
                        break
                    }
                }
                if (hasUnusablePath) {
                    continue // Skip this partition trial immediately!
                }

                backtrackCount = 0
                val trialArrows = findValidOrientations(validPaths, 0, mutableListOf(), trialRan, w, h, maxSteps = 1500)
                totalBacktrackCount += backtrackCount
                if (trialArrows != null) {
                    bestArrows = trialArrows
                    strategyUsed = "Main Backtracking Solver"
                    break
                }
            }
            seedOffset += 12345L
            attempts++
        }

        var finalArrows = bestArrows
        var fallbackAttemptCount = 0

        // GUARANTEED FALLBACK: If everything failed, generate basic straight arrows of length 2 and ensure solvability
        if (finalArrows.isEmpty()) {
            strategyUsed = "Fallback 1 (Length-2 pairs)"
            var fallbackAttempt = 0
            while (finalArrows.isEmpty() && fallbackAttempt < 30) {
                val trialRan = Random(baseSeed + 99999L + fallbackAttempt)
                val paths = mutableListOf<MutableList<Point>>()
                
                // Shuffle unvisited to avoid strictly left-to-right top-to-bottom horizontal pairings!
                val shuffledCells = shapeCells.shuffled(trialRan).toMutableSet()
                while (shuffledCells.isNotEmpty()) {
                    val start = shuffledCells.first()
                    shuffledCells.remove(start)
                    val neighbors = getNeighbors(start, w, h).filter { it in shuffledCells }
                    if (neighbors.isNotEmpty()) {
                        val next = neighbors.shuffled(trialRan).first()
                        shuffledCells.remove(next)
                        paths.add(mutableListOf(start, next))
                    }
                }
                
                backtrackCount = 0
                val trialArrows = findValidOrientations(paths, 0, mutableListOf(), trialRan, w, h, maxSteps = 3000)
                totalBacktrackCount += backtrackCount
                if (trialArrows != null) {
                    finalArrows = trialArrows
                    break
                }
                fallbackAttempt++
            }
            fallbackAttemptCount = fallbackAttempt
        }

        // ABSOLUTE FALLBACK: simple list with zero opposing arrows and mixed directions
        if (finalArrows.isEmpty()) {
            strategyUsed = "Fallback 2 (Absolute Fallback)"
            val ran = Random(baseSeed + 9999)
            val paths = mutableListOf<MutableList<Point>>()
            val shuffledCells = shapeCells.shuffled(ran).toMutableSet()
            while (shuffledCells.isNotEmpty()) {
                val start = shuffledCells.first()
                shuffledCells.remove(start)
                val neighbors = getNeighbors(start, w, h).filter { it in shuffledCells }
                if (neighbors.isNotEmpty()) {
                    val next = neighbors.shuffled(ran).first()
                    shuffledCells.remove(next)
                    paths.add(mutableListOf(start, next))
                }
            }
            
            backtrackCount = 0
            val trialArrows = findValidOrientations(paths, 0, mutableListOf(), ran, w, h, maxSteps = 5000)
            totalBacktrackCount += backtrackCount
            if (trialArrows != null) {
                finalArrows = trialArrows
            } else {
                // If backtracking fails, orient them all to exit Left or Up to guarantee ZERO opposing arrows!
                val fallbackList = mutableListOf<Arrow>()
                var idCounter = 1
                for (path in paths) {
                    val color = COLORS[ran.nextInt(COLORS.size)]
                    // Determine orientation to only exit LEFT (dx <= 0) or UP (dy <= 0)
                    val p1 = path[0]
                    val p2 = path[1]
                    val dx = p2.x - p1.x
                    val dy = p2.y - p1.y
                    
                    val orientedPath = if (dx > 0 || dy > 0) {
                        listOf(p2, p1) // Reverse so it goes LEFT or UP
                    } else {
                        listOf(p1, p2)
                    }
                    fallbackList.add(Arrow(idCounter++, orientedPath, color))
                }
                finalArrows = fallbackList
                strategyUsed = "Fallback 2 (Absolute - Forced Left/Up)"
            }
        }

        // Normalize indices for correct state transitions
        val finalCleanedArrows = finalArrows.mapIndexed { idx, arrow ->
            arrow.copy(id = idx + 1, state = ArrowState.IDLE, slideAnimProgress = 0f)
        }

        val avgArrowLength = if (finalCleanedArrows.isNotEmpty()) {
            finalCleanedArrows.map { it.path.size }.average()
        } else {
            0.0
        }
        val avgLengthFormatted = ((avgArrowLength * 100).toInt() / 100.0).toString()
        val fallbackTriggered = (strategyUsed != "Main Backtracking Solver")

        val statsString = """
            [Level $levelIndex Generation Stats]
            - Grid Size: ${w}x${h}
            - Shape Type: $shapeType
            - Svg Rasterization: $rasterStatsInfo
            - Active Cells: ${shapeCells.size} (Original: $originalSizeBeforeFallback)
            - Defensive fallback used: $defensiveFallbackUsed
            - Solver Strategy: $strategyUsed
            - Solver Attempts: $attempts
            - Fallback Attempts: $fallbackAttemptCount
            - Backtrack Steps: $totalBacktrackCount
            - Arrows Count: ${finalCleanedArrows.size}
            - Avg Arrow Length: $avgLengthFormatted
            - Fallback Triggered: $fallbackTriggered
        """.trimIndent()

        android.util.Log.i("LevelGenerator", statsString)

        val generated = LevelData(
            levelIndex = levelIndex,
            shapeType = shapeType,
            gridWidth = w,
            gridHeight = h,
            arrows = finalCleanedArrows,
            generationStats = statsString
        )
        levelCache[levelIndex] = generated
        return generated
    }

    private fun isPointInShape(p: Point, shapeType: ShapeType, w: Int, h: Int): Boolean {
        val cx = (w - 1) / 2.0f
        val cy = (h - 1) / 2.0f
        val dx = p.x - cx
        val dy = p.y - cy
        val r = sdist(dx, dy)
        val maxR = minOf(w, h) / 2.0f

        return when (shapeType) {
            ShapeType.CIRCLE -> {
                r <= maxR - 0.2f
            }
            ShapeType.SQUARE -> {
                Math.abs(dx) <= maxR - 0.5f && Math.abs(dy) <= maxR - 0.5f
            }
            ShapeType.DIAMOND -> {
                Math.abs(dx) + Math.abs(dy) <= maxR * 1.05f
            }
            ShapeType.CROSS -> {
                val barHalfWidth = if (w <= 12) 1.5f else 1.9f
                Math.abs(dx) <= barHalfWidth || Math.abs(dy) <= barHalfWidth
            }
            ShapeType.HEART -> {
                // Highly refined algebraic Heart shape with a prominent top neck/notch and rounded lobes!
                val nx = (p.x - cx) / (w / 2.0f) * 1.3f
                val ny = -(p.y - cy) / (h / 2.0f) * 1.3f + 0.22f
                val term = nx * nx + ny * ny - 0.85f
                val classicalHeart = (term * term * term - nx * nx * ny * ny * ny) <= 0.0f
                
                // Deep cut notch at top-center to ensure it never looks like a simple inverted triangle!
                val isNotch = Math.abs(p.x - cx) < 0.9f && p.y <= cy - 1.2f
                classicalHeart && !isNotch
            }
            ShapeType.STAR -> {
                val angle = Math.atan2(dy.toDouble(), dx.toDouble())
                val starR = maxR * (0.45f + 0.55f * Math.abs(Math.cos(2 * angle)).toFloat() * 0.6f)
                r <= starR
            }
            ShapeType.FLOWER -> {
                val angle = Math.atan2(dy.toDouble(), dx.toDouble())
                val petals = Math.abs(Math.sin(3 * angle)).toFloat()
                val flowerR = maxR * (0.45f + 0.55f * petals)
                r <= flowerR
            }
            ShapeType.SPIRAL -> {
                val outer = r >= maxR * 0.65f && r <= maxR - 0.2f
                val inner = r <= maxR * 0.35f
                outer || inner
            }
            ShapeType.DOUBLE_SPIRAL -> {
                val core = r <= maxR * 0.22f
                val mid = r >= maxR * 0.45f && r <= maxR * 0.62f
                val out = r >= maxR * 0.82f && r <= maxR - 0.1f
                core || mid || out
            }
            ShapeType.INFINITY -> {
                val offset = maxR * 0.4f
                val leftDx = dx + offset
                val rightDx = dx - offset
                val dL = sdist(leftDx, dy)
                val dR = sdist(rightDx, dy)
                val circleR = maxR * 0.45f
                dL <= circleR || dR <= circleR
            }
            ShapeType.WHEEL -> {
                val tire = r >= maxR * 0.7f && r <= maxR - 0.2f
                val hub = r <= maxR * 0.32f
                val spokes = Math.abs(dx) <= 1.1f || Math.abs(dy) <= 1.1f
                tire || hub || (spokes && r < maxR)
            }
            ShapeType.LABYRINTH -> {
                val distEdgeX = minOf(p.x, w - 1 - p.x)
                val distEdgeY = minOf(p.y, h - 1 - p.y)
                val dist = minOf(distEdgeX, distEdgeY)
                dist % 2 == 0
            }
            ShapeType.SNAKE -> {
                // Highly detailed Chess Knight profile with a separate pedestal stand!
                val nx = (p.x - cx) / (w / 2.0f)
                val ny = (p.y - cy) / (h / 2.0f)
                
                if (ny >= 0.70f && ny <= 0.88f) {
                    // Pedestal Stand
                    Math.abs(nx) <= 0.82f
                } else if (ny > 0.52f && ny < 0.70f) {
                    // Gap
                    false
                } else {
                    // Knight body
                    when {
                        ny < -0.8f -> false
                        // Ears
                        ny >= -0.8f && ny < -0.52f -> {
                            val inEar1 = nx >= 0.02f && nx <= 0.16f && ny >= -0.8f
                            val inEar2 = nx >= 0.22f && nx <= 0.36f && ny >= -0.72f
                            inEar1 || inEar2
                        }
                        // Head & Snout
                        ny >= -0.52f && ny < -0.22f -> {
                            val rightLimit = 0.45f
                            // Protruding snout on left
                            val leftLimit = if (ny < -0.38f) -0.7f else -0.45f
                            nx in leftLimit..rightLimit
                        }
                        // Chest & Neck
                        ny >= -0.22f && ny < 0.22f -> {
                            // Back of neck on the right
                            val rightLimit = 0.45f + 0.15f * (ny + 0.22f)
                            // Curved chest bulge on the left (curving to the left, then back right)
                            val leftLimit = -0.55f + 0.6f * (ny - 0.0f) * (ny - 0.0f)
                            nx in leftLimit..rightLimit
                        }
                        // Base of the Knight
                        ny >= 0.22f && ny <= 0.52f -> {
                            // Flaring bottom base
                            val rightLimit = 0.45f + 0.4f * (ny - 0.22f)
                            val leftLimit = -0.55f - 0.2f * (ny - 0.22f)
                            nx in leftLimit..rightLimit
                        }
                        else -> false
                    }
                }
            }
            ShapeType.TREE -> {
                if (p.y >= h - 2) {
                    Math.abs(dx) <= maxR * 0.25f
                } else {
                    val remH = h - 2
                    val tier = p.y * 3 / remH
                    val tierY = p.y % (remH / 3 + 1)
                    val maxSpread = when (tier) {
                        0 -> maxR * 0.4f
                        1 -> maxR * 0.7f
                        else -> maxR * 0.95f
                    }
                    val currentWidth = maxSpread * (tierY + 1) / (remH / 3 + 1)
                    Math.abs(dx) <= currentWidth
                }
            }
            ShapeType.SYMMETRICAL -> {
                val inCircle = r <= maxR - 0.2f
                val inSquare = Math.abs(dx) <= maxR * 0.6f && Math.abs(dy) <= maxR * 0.6f
                inCircle && inSquare
            }
            ShapeType.SUN -> {
                // Beautiful sun with circular core and separate triangular rays radiating outwards!
                val isCore = r <= maxR * 0.42f
                val isRay = if (r in (maxR * 0.62f)..(maxR * 0.98f)) {
                    val angleRad = Math.atan2(dy.toDouble(), dx.toDouble())
                    val angleDeg = Math.toDegrees(angleRad).let { if (it < 0) it + 360.0 else it }
                    val nearestRayAngle = Math.round(angleDeg / 45.0) * 45.0
                    val angleDiff = Math.abs(angleDeg - nearestRayAngle).let { Math.min(it, 360.0 - it) }
                    val tolerance = 15.0 * (1.0 - (r - maxR * 0.62f) / (maxR * 0.36f))
                    angleDiff <= tolerance
                } else false
                isCore || isRay
            }
            ShapeType.CRESCENT_MOON -> {
                // Crescent silhouette subtracting shifted inner circular mask from outer circular mask
                val outer = r <= maxR * 0.9f
                val cutoutDx = dx - maxR * 0.35f
                val cutoutDy = dy + maxR * 0.15f
                val cutoutR = Math.sqrt((cutoutDx * cutoutDx + cutoutDy * cutoutDy).toDouble())
                outer && (cutoutR > maxR * 0.75f)
            }
            ShapeType.CROWN -> {
                // Detailed royal crown with spikes and thick horizontal base
                val ny = (p.y - cy) / (h / 2.0f)
                val nx = (p.x - cx) / (w / 2.0f)
                
                if (ny > 0.5f) {
                    Math.abs(nx) <= 0.85f
                } else if (ny in -0.7f..0.5f) {
                    val inLeftPeak = Math.abs(nx + 0.6f) <= 0.25f && ny >= 1.8f * Math.abs(nx + 0.6f) - 0.5f
                    val inCenterPeak = Math.abs(nx) <= 0.25f && ny >= 1.8f * Math.abs(nx) - 0.72f
                    val inRightPeak = Math.abs(nx - 0.6f) <= 0.25f && ny >= 1.8f * Math.abs(nx - 0.6f) - 0.5f
                    
                    (inLeftPeak || inCenterPeak || inRightPeak) && Math.abs(nx) <= 0.82f
                } else {
                    false
                }
            }
            ShapeType.BUTTERFLY -> {
                // Symmetrical butterfly with elegant winged curves
                val nx = Math.abs((p.x - cx) / (w / 2.0f))
                val ny = (p.y - cy) / (h / 2.0f)
                val isBody = nx <= 0.12f && ny in -0.85f..0.85f
                val isWing = if (nx > 0.12f && nx <= 0.95f && ny in -0.8f..0.8f) {
                    val factor = if (ny < 0) {
                        0.85f - 0.3f * Math.abs(ny + 0.4f)
                    } else {
                        0.7f - 0.5f * Math.abs(ny - 0.4f)
                    }
                    nx <= factor
                } else false
                isBody || isWing
            }
            ShapeType.KEY -> {
                // Beautiful key shape: hollow circular handle, thin shaft, and notched teeth
                val nx = (p.x - cx) / (w / 2.0f)
                val ny = (p.y - cy) / (h / 2.0f)
                
                val handleCx = -0.6f
                val handleDist = Math.sqrt(((nx - handleCx) * (nx - handleCx) + ny * ny).toDouble()).toFloat()
                val isHandle = handleDist >= 0.15f && handleDist <= 0.38f
                
                val isShaft = nx >= -0.3f && nx <= 0.7f && Math.abs(ny) <= 0.08f
                
                val isTeeth = nx in 0.4f..0.7f && ny >= 0.0f && ny <= 0.45f && ((p.x % 2 == 0))
                
                isHandle || isShaft || isTeeth
            }
            ShapeType.ARCHIPELAGO -> {
                // Scattered set of isolated round islands
                val island1 = sdist(dx - maxR * 0.4f, dy - maxR * 0.4f) <= maxR * 0.25f
                val island2 = sdist(dx + maxR * 0.4f, dy - maxR * 0.4f) <= maxR * 0.25f
                val island3 = sdist(dx - maxR * 0.4f, dy + maxR * 0.4f) <= maxR * 0.25f
                val island4 = sdist(dx + maxR * 0.4f, dy + maxR * 0.4f) <= maxR * 0.25f
                island1 || island2 || island3 || island4
            }
            ShapeType.CONCENTRIC_RINGS -> {
                // Multiple concentric circular rings separated by empty channels
                val ring1 = r in (maxR * 0.25f)..(maxR * 0.4f)
                val ring2 = r in (maxR * 0.65f)..(maxR * 0.85f)
                ring1 || ring2
            }
            ShapeType.CONCENTRIC_SQUARES -> {
                // Nested square boundaries separated by empty channels
                val adx = Math.abs(dx)
                val ady = Math.abs(dy)
                val maxCoord = maxOf(adx, ady)
                val innerSquare = maxCoord >= maxR * 0.2f && maxCoord <= maxR * 0.4f
                val outerSquare = maxCoord >= maxR * 0.65f && maxCoord <= maxR * 0.85f
                innerSquare || outerSquare
            }
            ShapeType.GRID_LOCK -> {
                // A network of horizontal and vertical bars with hollow cells
                val barX = p.x % 4 == 1
                val barY = p.y % 4 == 1
                (barX || barY) && (Math.abs(dx) <= maxR * 0.85f && Math.abs(dy) <= maxR * 0.85f)
            }
            ShapeType.SATURN -> {
                // A central planet with a completely detached outer planetary ring
                val planet = r <= maxR * 0.45f
                val ring = r >= maxR * 0.7f && r <= maxR * 0.88f && Math.abs(dy - dx * 0.3f) <= maxR * 0.15f
                planet || ring
            }
            ShapeType.ATOM -> {
                // A central nucleus surrounded by separate orbiting electron paths
                val nucleus = r <= maxR * 0.25f
                val ellipse1 = Math.abs(dx * dx + 4 * dy * dy - maxR * maxR * 0.5f) <= maxR * 0.15f
                val ellipse2 = Math.abs(4 * dx * dx + dy * dy - maxR * maxR * 0.5f) <= maxR * 0.15f
                nucleus || ellipse1 || ellipse2
            }
            ShapeType.YIN_YANG -> {
                // Two swirling halves with isolated contrasting dots inside them
                if (r > maxR * 0.85f) false
                else {
                    val dot1 = sdist(dx, dy - maxR * 0.4f) <= maxR * 0.12f
                    val dot2 = sdist(dx, dy + maxR * 0.4f) <= maxR * 0.12f
                    val upperSemicircle = dy < 0 && dx > 0
                    val lowerSemicircle = dy > 0 && dx < 0
                    val swirl1 = sdist(dx, dy - maxR * 0.4f) <= maxR * 0.4f && dx >= 0
                    val swirl2 = sdist(dx, dy + maxR * 0.4f) <= maxR * 0.4f && dx <= 0
                    ((swirl1 || swirl2 || upperSemicircle || lowerSemicircle) && !dot1) || dot2
                }
            }
            ShapeType.PAW_PRINT -> {
                // A main pad with several separate small toe pads above it
                val mainPad = sdist(dx, dy + maxR * 0.15f) <= maxR * 0.38f && dy + maxR * 0.15f >= -maxR * 0.15f
                val toe1 = sdist(dx - maxR * 0.38f, dy - maxR * 0.35f) <= maxR * 0.18f
                val toe2 = sdist(dx - maxR * 0.13f, dy - maxR * 0.52f) <= maxR * 0.18f
                val toe3 = sdist(dx + maxR * 0.13f, dy - maxR * 0.52f) <= maxR * 0.18f
                val toe4 = sdist(dx + maxR * 0.38f, dy - maxR * 0.35f) <= maxR * 0.18f
                mainPad || toe1 || toe2 || toe3 || toe4
            }
            ShapeType.CLOUD_AND_RAIN -> {
                // A big fluffy cloud with separate raindrop dashes falling below it
                val inCloud = (sdist(dx, dy - maxR * 0.15f) <= maxR * 0.32f) ||
                        (sdist(dx - maxR * 0.3f, dy - maxR * 0.05f) <= maxR * 0.25f) ||
                        (sdist(dx + maxR * 0.3f, dy - maxR * 0.05f) <= maxR * 0.25f) ||
                        (Math.abs(dx) <= maxR * 0.45f && dy >= -maxR * 0.15f && dy <= maxR * 0.15f)
                val isRain = dy > maxR * 0.42f && dy <= maxR * 0.85f && Math.abs(dx) <= maxR * 0.5f && (p.x % 3 == 0) && (p.y % 2 == 0)
                inCloud || isRain
            }
            ShapeType.SNOWFLAKE -> {
                // A central star with separate ice crystals floating symmetrically around it
                val centralStar = r <= maxR * 0.3f || (Math.abs(dx) <= 1.2f && r <= maxR * 0.6f) || (Math.abs(dy) <= 1.2f && r <= maxR * 0.6f)
                val crystal1 = sdist(dx - maxR * 0.7f, dy) <= maxR * 0.15f
                val crystal2 = sdist(dx + maxR * 0.7f, dy) <= maxR * 0.15f
                val crystal3 = sdist(dx, dy - maxR * 0.7f) <= maxR * 0.15f
                val crystal4 = sdist(dx, dy + maxR * 0.7f) <= maxR * 0.15f
                centralStar || crystal1 || crystal2 || crystal3 || crystal4
            }
            ShapeType.CONSTELLATION -> {
                // A pattern of separate star-islands connected by linear paths
                val starA = sdist(dx - maxR * 0.5f, dy - maxR * 0.5f) <= maxR * 0.18f
                val starB = sdist(dx + maxR * 0.5f, dy - maxR * 0.3f) <= maxR * 0.18f
                val starC = sdist(dx - maxR * 0.2f, dy + maxR * 0.4f) <= maxR * 0.18f
                val starD = sdist(dx + maxR * 0.3f, dy + maxR * 0.5f) <= maxR * 0.18f
                starA || starB || starC || starD
            }
            ShapeType.DONUT_HOLES -> {
                // A regular grid of nine small separated circle islands
                val step = maxR * 0.55f
                var match = false
                for (ix in -1..1) {
                    for (iy in -1..1) {
                        if (sdist(dx - ix * step, dy - iy * step) <= maxR * 0.18f) {
                            match = true
                        }
                    }
                }
                match
            }
            ShapeType.GALAXY -> {
                // A central cosmic core with spiral arms made of detached star islets
                val coreGalaxy = r <= maxR * 0.28f
                val angleGal = Math.atan2(dy.toDouble(), dx.toDouble())
                val spiralDist1 = Math.abs(r - maxR * 0.5f * (1f + angleGal.toFloat() / 3f)) <= maxR * 0.12f
                val spiralDist2 = Math.abs(r - maxR * 0.5f * (1f + (angleGal.toFloat() + Math.PI.toFloat()) / 3f)) <= maxR * 0.12f
                val onSpirals = (spiralDist1 || spiralDist2) && r >= maxR * 0.35f && r <= maxR * 0.9f && (p.x + p.y) % 2 == 0
                coreGalaxy || onSpirals
            }
            ShapeType.SOUND_WAVE -> {
                // A series of parallel vertical bars of varying heights, completely detached
                val isBarColumn = (p.x % 3 == 0) && (Math.abs(dx) <= maxR * 0.85f)
                if (isBarColumn) {
                    val colIndex = (p.x - cx).toInt()
                    val maxAllowedY = maxR * 0.85f * Math.abs(Math.cos(colIndex * 0.5)).toFloat()
                    Math.abs(dy) <= maxAllowedY
                } else false
            }
            ShapeType.EUREKA_BULB -> {
                // A lightbulb with separate glowing rays radiating around it
                val glass = sdist(dx, dy - maxR * 0.15f) <= maxR * 0.38f
                val base = Math.abs(dx) <= maxR * 0.22f && dy >= maxR * 0.12f && dy <= maxR * 0.45f
                val filament = Math.abs(dx) <= 1.0f && dy >= -maxR * 0.05f && dy <= maxR * 0.15f
                val rayAngle = Math.atan2(dy.toDouble(), dx.toDouble())
                val isRayLight = r >= maxR * 0.55f && r <= maxR * 0.85f && Math.abs(Math.sin(6 * rayAngle)) >= 0.92f && (p.x + p.y) % 2 == 0
                glass || base || filament || isRayLight
            }
            ShapeType.FACE -> {
                // Two separate eyes, a nose bar, and a mouth curve
                val eyeLeft = sdist(dx + maxR * 0.35f, dy + maxR * 0.3f) <= maxR * 0.15f
                val eyeRight = sdist(dx - maxR * 0.35f, dy + maxR * 0.3f) <= maxR * 0.15f
                val nose = Math.abs(dx) <= 1.0f && dy >= -maxR * 0.1f && dy <= maxR * 0.15f
                val mouth = dy >= maxR * 0.32f && dy <= maxR * 0.52f && Math.abs(dx) <= maxR * 0.45f && dy >= maxR * 0.32f + Math.abs(dx) * 0.4f
                eyeLeft || eyeRight || nose || mouth
            }
            ShapeType.ALIEN_INVADER -> {
                // Symmetrical retro space invader mapped to normalized coordinates so it scales perfectly with grid size!
                val nx = (p.x - cx) / (w / 2.0f)
                val ny = (p.y - (cy - 0.2f)) / (h / 2.0f) // centered shift
                
                // 11x8 detailed pixel representation of the retro space invader
                val alienGrid = listOf(
                    "00100000100",
                    "00010001000",
                    "00111111100",
                    "01101110110",
                    "11111111111",
                    "10111111101",
                    "10100000101",
                    "00011011000"
                )
                
                // Map nx from -0.8 to 0.8 and ny from -0.65 to 0.65
                val col = ((nx + 0.8f) / 1.6f * 11).toInt()
                val row = ((ny + 0.65f) / 1.3f * 8).toInt()
                
                if (row in 0..7 && col in 0..10) {
                    alienGrid[row][col] == '1'
                } else {
                    // Symmetrical floaters on side that scale perfectly with maxR
                    sdist(dx - maxR * 0.75f, dy - maxR * 0.5f) <= maxR * 0.16f ||
                            sdist(dx + maxR * 0.75f, dy - maxR * 0.5f) <= maxR * 0.16f
                }
            }
            ShapeType.FISH_BONES -> {
                // A central spine with separate ribs on the sides
                val spine = Math.abs(dx) <= 1.0f && Math.abs(dy) <= maxR * 0.85f
                val rib = (p.y % 3 == 0) && Math.abs(dx) <= maxR * 0.65f && Math.abs(dy) <= maxR * 0.75f
                spine || rib
            }
            ShapeType.CASTLE -> {
                // A central keep with separate tower islands on the left and right
                val keep = Math.abs(dx) <= maxR * 0.35f && dy >= -maxR * 0.4f && dy <= maxR * 0.8f
                val towerLeft = Math.abs(dx + maxR * 0.65f) <= maxR * 0.18f && dy >= -maxR * 0.1f && dy <= maxR * 0.8f
                val towerRight = Math.abs(dx - maxR * 0.65f) <= maxR * 0.18f && dy >= -maxR * 0.1f && dy <= maxR * 0.8f
                keep || towerLeft || towerRight
            }
            ShapeType.TRAIN -> {
                // A train cabin with separate wheel circles underneath
                val bodyTrain = (dx >= -maxR * 0.7f && dx <= maxR * 0.5f && dy >= -maxR * 0.4f && dy <= maxR * 0.3f) ||
                        (dx >= maxR * 0.1f && dx <= maxR * 0.5f && dy >= -maxR * 0.7f && dy <= -maxR * 0.4f)
                val wheelL = sdist(dx + maxR * 0.35f, dy - maxR * 0.55f) <= maxR * 0.22f && sdist(dx + maxR * 0.35f, dy - maxR * 0.55f) >= maxR * 0.08f
                val wheelR = sdist(dx - maxR * 0.25f, dy - maxR * 0.55f) <= maxR * 0.22f && sdist(dx - maxR * 0.25f, dy - maxR * 0.55f) >= maxR * 0.08f
                bodyTrain || wheelL || wheelR
            }
            ShapeType.TARGET -> {
                // A central bullseye dot and separate outer concentric rings
                val bullseye = r <= maxR * 0.18f
                val midRing = r >= maxR * 0.42f && r <= maxR * 0.58f
                val outRing = r >= maxR * 0.78f && r <= maxR * 0.92f
                bullseye || midRing || outRing
            }
            ShapeType.HOURGLASS -> {
                // Top and bottom triangular chambers with empty space between them
                val inHourglass = Math.abs(dx) <= (Math.abs(dy) * 0.85f) && Math.abs(dy) <= maxR * 0.85f
                val isNeckSpace = Math.abs(dy) < maxR * 0.18f
                inHourglass && !isNeckSpace
            }
            ShapeType.CHEVRONS -> {
                // A series of parallel V-shaped arrows pointing upwards, completely separate
                val yOffset = dy + Math.abs(dx) * 0.6f
                val chevronNum = ((yOffset + maxR) / (maxR * 0.45f)).toInt()
                val rem = (yOffset + maxR) % (maxR * 0.45f)
                Math.abs(dx) <= maxR * 0.8f && chevronNum in 1..3 && rem <= maxR * 0.15f
            }
            ShapeType.LADDER -> {
                // Two vertical rails with separate horizontal rungs
                val railL = Math.abs(dx + maxR * 0.45f) <= 1.1f && Math.abs(dy) <= maxR * 0.85f
                val railR = Math.abs(dx - maxR * 0.45f) <= 1.1f && Math.abs(dy) <= maxR * 0.85f
                val rung = (p.y % 3 == 0) && dx >= -maxR * 0.45f && dx <= maxR * 0.45f && Math.abs(dy) <= maxR * 0.75f
                railL || railR || rung
            }
            ShapeType.STAIRS -> {
                // A stepped pattern rising diagonally
                val stepSum = p.x + p.y
                val stepDiff = p.x - p.y
                Math.abs(stepDiff) <= 1 && stepSum in (w / 2)..(w * 3 / 2)
            }
            ShapeType.STORM_CLOUD -> {
                // A fluffy cloud with a separate lightning bolt underneath
                val mainCloud = sdist(dx, dy - maxR * 0.15f) <= maxR * 0.35f ||
                        sdist(dx - maxR * 0.3f, dy) <= maxR * 0.25f ||
                        sdist(dx + maxR * 0.3f, dy) <= maxR * 0.25f
                val lBolt = dy > maxR * 0.3f && dy <= maxR * 0.8f && Math.abs(dx + (dy - maxR * 0.55f) * 0.5f) <= 1.0f
                mainCloud || lBolt
            }
            ShapeType.CAT_FACE -> {
                // A head with separate whiskers and separate triangular ears
                val headCat = sdist(dx, dy + maxR * 0.1f) <= maxR * 0.42f
                val earL = dx <= -maxR * 0.2f && dy <= -maxR * 0.1f && Math.abs(dx + maxR * 0.35f) + Math.abs(dy + maxR * 0.5f) <= maxR * 0.2f
                val earR = dx >= maxR * 0.2f && dy <= -maxR * 0.1f && Math.abs(dx - maxR * 0.35f) + Math.abs(dy + maxR * 0.5f) <= maxR * 0.2f
                val whiskerL = dy >= maxR * 0.1f && dy <= maxR * 0.25f && dx <= -maxR * 0.4f && dx >= -maxR * 0.85f && (p.y % 2 == 0)
                val whiskerR = dy >= maxR * 0.1f && dy <= maxR * 0.25f && dx >= maxR * 0.4f && dx <= maxR * 0.85f && (p.y % 2 == 0)
                headCat || earL || earR || whiskerL || whiskerR
            }
            ShapeType.DNA_STRAND -> {
                // Two intertwining helical strands with separate rungs between them
                val strand1 = Math.abs(dx - maxR * 0.45f * Math.sin(dy * 0.6 + Math.PI / 2).toFloat()) <= 1.1f && Math.abs(dy) <= maxR * 0.85f
                val strand2 = Math.abs(dx - maxR * 0.45f * Math.sin(dy * 0.6 - Math.PI / 2).toFloat()) <= 1.1f && Math.abs(dy) <= maxR * 0.85f
                val rungsDna = (p.y % 4 == 0) && Math.abs(dx) <= maxR * 0.45f && Math.abs(dy) <= maxR * 0.8f
                strand1 || strand2 || rungsDna
            }
            ShapeType.COMPASS -> {
                // A central pointer arrow and four separate cardinal direction indicators
                val needle = (Math.abs(dx) <= 1.0f && Math.abs(dy) <= maxR * 0.52f) ||
                        (Math.abs(dy) <= 1.0f && Math.abs(dx) <= maxR * 0.52f)
                val north = sdist(dx, dy + maxR * 0.78f) <= maxR * 0.15f
                val south = sdist(dx, dy - maxR * 0.78f) <= maxR * 0.15f
                val east = sdist(dx - maxR * 0.78f, dy) <= maxR * 0.15f
                val west = sdist(dx + maxR * 0.78f, dy) <= maxR * 0.15f
                needle || north || south || east || west
            }
            ShapeType.SHIELD_SWORD -> {
                // A shield shape with a separate sword passing behind it
                val inShield = Math.abs(dx) <= maxR * 0.55f && dy >= -maxR * 0.4f && dy <= maxR * 0.55f - Math.abs(dx) * 0.4f
                val inSword = Math.abs(dx + dy) <= 1.1f && sdist(dx, dy) <= maxR * 0.95f
                inShield || inSword
            }
            ShapeType.GEARS -> {
                // Two interlocking gear wheels with separate central axle holes
                val gear1Dist = sdist(dx + maxR * 0.35f, dy + maxR * 0.35f)
                val gear1 = gear1Dist <= maxR * 0.42f && gear1Dist >= maxR * 0.15f && (gear1Dist <= maxR * 0.32f || Math.abs(Math.sin(8 * Math.atan2(dy + maxR * 0.35, dx + maxR * 0.35))) >= 0.7f)
                val gear2Dist = sdist(dx - maxR * 0.35f, dy - maxR * 0.35f)
                val gear2 = gear2Dist <= maxR * 0.42f && gear2Dist >= maxR * 0.15f && (gear2Dist <= maxR * 0.32f || Math.abs(Math.sin(8 * Math.atan2(dy - maxR * 0.35, dx - maxR * 0.35) + 0.4)) >= 0.7f)
                gear1 || gear2
            }
            ShapeType.ANCHOR -> {
                // A classic anchor with a separate ring at the top
                val shaftAnchor = Math.abs(dx) <= 1.1f && dy >= -maxR * 0.52f && dy <= maxR * 0.45f
                val ringAnchor = sdist(dx, dy + maxR * 0.65f) <= maxR * 0.22f && sdist(dx, dy + maxR * 0.65f) >= maxR * 0.08f
                val hook = r >= maxR * 0.45f && r <= maxR * 0.65f && dy >= 0
                shaftAnchor || ringAnchor || hook
            }
            ShapeType.WIFI_SIGNAL -> {
                // A small central dot with separate curved signal arcs expanding upwards
                val wifiDot = r <= maxR * 0.18f && dy >= -maxR * 0.15f
                val arc1 = r >= maxR * 0.38f && r <= maxR * 0.52f && dy <= 0 && Math.abs(dx) <= -dy * 1.5f
                val arc2 = r >= maxR * 0.72f && r <= maxR * 0.88f && dy <= 0 && Math.abs(dx) <= -dy * 1.5f
                wifiDot || arc1 || arc2
            }
            ShapeType.EIFFEL_TOWER -> {
                // A tall tower with horizontal arch bases
                val towerBody = Math.abs(dx) <= maxR * 0.45f * (1.0f - (dy + maxR) / (2.0f * maxR))
                val archEiffel = dy >= maxR * 0.45f && Math.abs(dx) <= maxR * 0.32f
                towerBody && !archEiffel
            }
            ShapeType.PANDA_FACE -> {
                // A face circle with separate black eye patches and ears
                val mainPanda = sdist(dx, dy) <= maxR * 0.52f
                val pandaEarL = sdist(dx + maxR * 0.45f, dy + maxR * 0.45f) <= maxR * 0.22f
                val pandaEarR = sdist(dx - maxR * 0.45f, dy + maxR * 0.45f) <= maxR * 0.22f
                val pandaEyeL = sdist(dx + maxR * 0.18f, dy + maxR * 0.1f) <= maxR * 0.12f
                val pandaEyeR = sdist(dx - maxR * 0.18f, dy + maxR * 0.1f) <= maxR * 0.12f
                mainPanda || pandaEarL || pandaEarR || pandaEyeL || pandaEyeR
            }
            ShapeType.SWAN -> {
                // An elegant swimming swan with a long neck
                val swanBody = dx >= -maxR * 0.55f && dx <= maxR * 0.25f && dy >= 0 && dy <= maxR * 0.45f && sdist(dx + maxR * 0.15f, dy) <= maxR * 0.45f
                val swanNeck = Math.abs(dx - maxR * 0.35f + (dy + maxR * 0.2f) * 0.4f) <= 1.1f && dy >= -maxR * 0.75f && dy <= maxR * 0.25f
                swanBody || swanNeck
            }
            ShapeType.UMBRELLA -> {
                // A curved canopy with separate falling raindrops
                val canopy = sdist(dx, dy + maxR * 0.15f) <= maxR * 0.55f && dy <= -maxR * 0.1f
                val handleUmbrella = Math.abs(dx) <= 1.1f && dy >= -maxR * 0.1f && dy <= maxR * 0.65f
                val rainUmbrella = dy >= maxR * 0.15f && dy <= maxR * 0.85f && Math.abs(dx) >= maxR * 0.4f && (p.x % 3 == 0) && (p.y % 2 == 0)
                canopy || handleUmbrella || rainUmbrella
            }
            ShapeType.SAILBOAT -> {
                // A boat hull with separate triangular sails above it
                val boatHull = dy >= maxR * 0.22f && dy <= maxR * 0.55f && dx >= -maxR * 0.75f && dx <= maxR * 0.75f && dy >= maxR * 0.22f + Math.abs(dx) * 0.35f
                val mast = Math.abs(dx) <= 1.1f && dy >= -maxR * 0.75f && dy < maxR * 0.22f
                val sailL = dx <= -1.0f && dy >= -maxR * 0.65f && dy < maxR * 0.15f && dx >= -maxR * 0.55f && dy >= -maxR * 0.65f + Math.abs(dx) * 1.1f
                val sailR = dx >= 1.0f && dy >= -maxR * 0.65f && dy < maxR * 0.15f && dx <= maxR * 0.55f && dy >= -maxR * 0.65f + Math.abs(dx) * 1.1f
                boatHull || mast || sailL || sailR
            }
            ShapeType.SPIDER_WEB -> {
                // Concentric polygons connected by radiating spokes
                val inSpokes = Math.abs(dx) <= 1.0f || Math.abs(dy) <= 1.0f || Math.abs(dx - dy) <= 1.0f || Math.abs(dx + dy) <= 1.0f
                val webRing1 = Math.abs(r - maxR * 0.32f) <= 1.0f
                val webRing2 = Math.abs(r - maxR * 0.62f) <= 1.0f
                val webRing3 = Math.abs(r - maxR * 0.88f) <= 1.0f
                (inSpokes && r <= maxR) || webRing1 || webRing2 || webRing3
            }
            ShapeType.PYRAMID -> {
                // Horizontal stepped tiers separated by narrow spaces
                val pyramidH = (dy + maxR) / (2f * maxR)
                val tierIndex = (pyramidH * 4).toInt()
                val remPyramid = (pyramidH * 4) % 1.0f
                val maxWidthTier = maxR * (tierIndex + 1) * 0.22f
                Math.abs(dx) <= maxWidthTier && tierIndex in 0..3 && remPyramid <= 0.82f
            }
            ShapeType.FLAME -> {
                // A fire contour with separate embers floating above it
                val mainFlame = r <= maxR * 0.55f && dy >= -maxR * 0.15f && Math.abs(dx) <= maxR * 0.45f * (1.0f - dy / (maxR * 0.75f))
                val ember1 = sdist(dx, dy + maxR * 0.55f) <= maxR * 0.15f
                val ember2 = sdist(dx - maxR * 0.22f, dy + maxR * 0.75f) <= maxR * 0.11f
                mainFlame || ember1 || ember2
            }
            ShapeType.WAVE -> {
                // A series of separate undulating sine wave segments
                val waveY1 = Math.abs(dy + maxR * 0.35f - maxR * 0.22f * Math.sin(dx * 0.45).toFloat()) <= 1.1f
                val waveY2 = Math.abs(dy - maxR * 0.35f - maxR * 0.22f * Math.sin(dx * 0.45).toFloat()) <= 1.1f
                (waveY1 || waveY2) && Math.abs(dx) <= maxR * 0.88f
            }
            ShapeType.BIRD -> {
                // Symmetrical wings separated from the beak and head
                val headBird = sdist(dx, dy + maxR * 0.25f) <= maxR * 0.18f
                val wingL = dx <= -maxR * 0.1f && sdist(dx + maxR * 0.42f, dy - maxR * 0.15f) <= maxR * 0.38f && dy <= maxR * 0.15f
                val wingR = dx >= maxR * 0.1f && sdist(dx - maxR * 0.42f, dy - maxR * 0.15f) <= maxR * 0.38f && dy <= maxR * 0.15f
                headBird || wingL || wingR
            }
            ShapeType.FOOTPRINT -> {
                // A sole with separate toe circles
                val sole = sdist(dx, dy + maxR * 0.18f) <= maxR * 0.38f && Math.abs(dx) <= maxR * 0.28f * (1.1f - dy / maxR)
                val fToe1 = sdist(dx - maxR * 0.28f, dy - maxR * 0.42f) <= maxR * 0.11f
                val fToe2 = sdist(dx - maxR * 0.1f, dy - maxR * 0.52f) <= maxR * 0.11f
                val fToe3 = sdist(dx + maxR * 0.1f, dy - maxR * 0.52f) <= maxR * 0.11f
                val fToe4 = sdist(dx + maxR * 0.28f, dy - maxR * 0.42f) <= maxR * 0.11f
                sole || fToe1 || fToe2 || fToe3 || fToe4
            }
            ShapeType.DICE -> {
                // A square with separate dot islands representing the face numbers
                val diceFrame = Math.abs(dx) <= maxR * 0.72f && Math.abs(dy) <= maxR * 0.72f && (Math.abs(dx) >= maxR * 0.6f || Math.abs(dy) >= maxR * 0.6f)
                val dotCenter = r <= maxR * 0.15f
                val dotTL = sdist(dx + maxR * 0.38f, dy + maxR * 0.38f) <= maxR * 0.15f
                val dotBR = sdist(dx - maxR * 0.38f, dy - maxR * 0.38f) <= maxR * 0.15f
                diceFrame || dotCenter || dotTL || dotBR
            }
            ShapeType.PIZZA -> {
                // A circular crust with separate pepperoni circles inside
                val crust = r >= maxR * 0.72f && r <= maxR * 0.85f
                val pep1 = sdist(dx - maxR * 0.28f, dy - maxR * 0.28f) <= maxR * 0.15f
                val pep2 = sdist(dx + maxR * 0.28f, dy + maxR * 0.18f) <= maxR * 0.15f
                val pep3 = sdist(dx - maxR * 0.15f, dy + maxR * 0.38f) <= maxR * 0.15f
                crust || pep1 || pep2 || pep3
            }
            ShapeType.CACTUS -> {
                // A central trunk with separate arm segments
                val trunkCactus = Math.abs(dx) <= maxR * 0.18f && Math.abs(dy) <= maxR * 0.85f
                val armL = dx <= -maxR * 0.15f && dx >= -maxR * 0.65f && dy >= -maxR * 0.25f && dy <= -maxR * 0.1f
                val armLUp = Math.abs(dx + maxR * 0.55f) <= maxR * 0.12f && dy >= -maxR * 0.55f && dy <= -maxR * 0.1f
                val armR = dx >= maxR * 0.15f && dx <= maxR * 0.65f && dy >= maxR * 0.1f && dy <= maxR * 0.25f
                val armRUp = Math.abs(dx - maxR * 0.55f) <= maxR * 0.12f && dy >= -maxR * 0.15f && dy <= maxR * 0.25f
                trunkCactus || armL || armLUp || armR || armRUp
            }
            ShapeType.BALLOON -> {
                // A round balloon with a separate basket below
                val balloonBody = sdist(dx, dy + maxR * 0.15f) <= maxR * 0.45f
                val basket = Math.abs(dx) <= maxR * 0.18f && dy >= maxR * 0.52f && dy <= maxR * 0.78f
                balloonBody || basket
            }
            ShapeType.PENGUIN -> {
                // A penguin body with separate flippers and feet
                val penBody = Math.abs(dx) <= maxR * 0.4f && Math.abs(dy) <= maxR * 0.7f && sdist(dx, dy) <= maxR * 0.68f
                val flipperL = dx <= -maxR * 0.35f && dy >= -maxR * 0.1f && dy <= maxR * 0.35f
                val flipperR = dx >= maxR * 0.35f && dy >= -maxR * 0.1f && dy <= maxR * 0.35f
                penBody || flipperL || flipperR
            }
            ShapeType.COSMIC_RING -> {
                // A planet with separate tilted rings around it
                val planetCosmic = r <= maxR * 0.45f
                val ringCosmic = r >= maxR * 0.62f && r <= maxR * 0.88f && Math.abs(dy + dx * 0.45f) <= maxR * 0.12f
                planetCosmic || ringCosmic
            }
            ShapeType.SMILEY -> {
                // A friendly circle with separate eyes and a smiling mouth
                val headSmiley = r >= maxR * 0.72f && r <= maxR * 0.85f
                val eyeLSmiley = sdist(dx + maxR * 0.25f, dy + maxR * 0.25f) <= maxR * 0.11f
                val eyeRSmiley = sdist(dx - maxR * 0.25f, dy + maxR * 0.25f) <= maxR * 0.11f
                val mouthSmiley = dy >= -maxR * 0.22f && dy <= -maxR * 0.1f && Math.abs(dx) <= maxR * 0.35f
                headSmiley || eyeLSmiley || eyeRSmiley || mouthSmiley
            }
            ShapeType.HEARTBEAT -> {
                // An ECG line with separate peaks
                val isOnEcg = if (Math.abs(dy) <= 1.0f) {
                    Math.abs(dx) >= maxR * 0.65f || Math.abs(dx) <= maxR * 0.12f
                } else {
                    Math.abs(dx - maxR * 0.38f) <= 1.0f || Math.abs(dx + maxR * 0.38f) <= 1.0f
                }
                isOnEcg && Math.abs(dx) <= maxR * 0.88f && Math.abs(dy) <= maxR * 0.82f
            }
            ShapeType.COMRADE_STAR -> {
                // A standard star with a separate nested inner star
                val angleCStar = Math.atan2(dy.toDouble(), dx.toDouble())
                val starOuterLimit = maxR * (0.42f + 0.58f * Math.abs(Math.cos(2 * angleCStar)).toFloat() * 0.6f)
                val inStarOuter = r <= starOuterLimit
                val starInnerLimit = maxR * (0.15f + 0.25f * Math.abs(Math.cos(2 * angleCStar)).toFloat() * 0.6f)
                val inStarInner = r <= starInnerLimit
                inStarOuter && (r >= maxR * 0.35f || inStarInner)
            }
            ShapeType.MAZE -> {
                // A nested rectangular spiral
                val adxMaze = Math.abs(p.x - cx).toInt()
                val adyMaze = Math.abs(p.y - cy).toInt()
                val maxMaze = maxOf(adxMaze, adyMaze)
                maxMaze % 2 == 0 && maxMaze <= (w / 2)
            }
            ShapeType.STETHOSCOPE -> {
                // A looping tube with a separate chestpiece
                val stethoscopeRing = r >= maxR * 0.45f && r <= maxR * 0.62f && dy >= -maxR * 0.15f
                val stethoscopeLeft = Math.abs(dx + maxR * 0.35f) <= 1.1f && dy <= -maxR * 0.15f
                val stethoscopeRight = Math.abs(dx - maxR * 0.35f) <= 1.1f && dy <= -maxR * 0.15f
                val chestPiece = sdist(dx, dy + maxR * 0.72f) <= maxR * 0.18f
                stethoscopeRing || stethoscopeLeft || stethoscopeRight || chestPiece
            }
            ShapeType.PALM_TREE -> {
                // A trunk with separate leafy branches
                val trunkPalm = Math.abs(dx + dy * 0.15f) <= 1.1f && dy >= -maxR * 0.45f && dy <= maxR * 0.85f
                val branchL = dy <= -maxR * 0.35f && dx <= 0 && Math.abs(dy + maxR * 0.65f + dx * 0.65f) <= maxR * 0.12f
                val branchR = dy <= -maxR * 0.35f && dx >= 0 && Math.abs(dy + maxR * 0.65f - dx * 0.65f) <= maxR * 0.12f
                trunkPalm || branchL || branchR
            }
            ShapeType.DIAMOND_RING -> {
                // A circular band with a separate diamond jewel on top
                val ringBand = sdist(dx, dy - maxR * 0.15f) <= maxR * 0.52f && sdist(dx, dy - maxR * 0.15f) >= maxR * 0.32f
                val diamondJewel = dy <= -maxR * 0.42f && Math.abs(dx) <= maxR * 0.22f && Math.abs(dx) + Math.abs(dy + maxR * 0.65f) <= maxR * 0.32f
                ringBand || diamondJewel
            }
            ShapeType.TURTLE -> {
                // A shell with a separate head, legs, and tail
                val turtleShell = sdist(dx, dy + maxR * 0.05f) <= maxR * 0.45f
                val turtleHead = sdist(dx, dy - maxR * 0.6f) <= maxR * 0.15f
                val turtleLegFL = sdist(dx + maxR * 0.4f, dy - maxR * 0.35f) <= maxR * 0.12f
                val turtleLegFR = sdist(dx - maxR * 0.4f, dy - maxR * 0.35f) <= maxR * 0.12f
                val turtleLegBL = sdist(dx + maxR * 0.35f, dy + maxR * 0.42f) <= maxR * 0.12f
                val turtleLegBR = sdist(dx - maxR * 0.35f, dy + maxR * 0.42f) <= maxR * 0.12f
                turtleShell || turtleHead || turtleLegFL || turtleLegFR || turtleLegBL || turtleLegBR
            }
            ShapeType.DRAGONFLY -> {
                // A slender body with four separate wings
                val dragonflyBody = Math.abs(dx) <= 1.0f && Math.abs(dy) <= maxR * 0.85f
                val dfWing1 = dy <= -maxR * 0.15f && dy >= -maxR * 0.3f && Math.abs(dx) <= maxR * 0.85f
                val dfWing2 = dy >= maxR * 0.1f && dy <= maxR * 0.25f && Math.abs(dx) <= maxR * 0.72f
                dragonflyBody || dfWing1 || dfWing2
            }
            ShapeType.CHESS_BOARD -> {
                // An alternating checkerboard pattern of separate squares
                val blockX = (p.x / 3)
                val blockY = (p.y / 3)
                (blockX + blockY) % 2 == 0 && Math.abs(dx) <= maxR * 0.85f && Math.abs(dy) <= maxR * 0.85f
            }
            ShapeType.ROCKET -> {
                // A slender spaceship with separate fire plume at the bottom
                val rocketBody = Math.abs(dx) <= maxR * 0.22f && dy >= -maxR * 0.55f && dy <= maxR * 0.45f
                val rocketNose = dy <= -maxR * 0.55f && Math.abs(dx) <= maxR * 0.22f * (1.0f - (-dy - maxR * 0.55f) / (maxR * 0.35f))
                val rocketPlume = dy >= maxR * 0.55f && dy <= maxR * 0.85f && Math.abs(dx) <= maxR * 0.12f && (p.y % 2 == 0)
                rocketBody || rocketNose || rocketPlume
            }
            ShapeType.MUSHROOM -> {
                // A cap with separate dots and a stem
                val mCap = sdist(dx, dy + maxR * 0.1f) <= maxR * 0.52f && dy <= -maxR * 0.1f
                val mStem = Math.abs(dx) <= maxR * 0.18f && dy >= -maxR * 0.1f && dy <= maxR * 0.65f
                val mDot1 = sdist(dx - maxR * 0.22f, dy - maxR * 0.22f) <= maxR * 0.11f
                val mDot2 = sdist(dx + maxR * 0.22f, dy - maxR * 0.22f) <= maxR * 0.11f
                (mCap && !mDot1 && !mDot2) || mStem
            }
            ShapeType.HELICOPTER -> {
                // A cabin with separate top and tail rotors
                val heliCabin = sdist(dx + maxR * 0.15f, dy) <= maxR * 0.38f
                val heliTail = dx >= -maxR * 0.15f && dx <= maxR * 0.72f && Math.abs(dy + maxR * 0.1f) <= 1.1f
                val topRotor = dy <= -maxR * 0.45f && dy >= -maxR * 0.55f && dx >= -maxR * 0.65f && dx <= maxR * 0.65f
                val tailRotor = dx >= maxR * 0.68f && dx <= maxR * 0.78f && Math.abs(dy + maxR * 0.1f) <= maxR * 0.28f
                heliCabin || heliTail || topRotor || tailRotor
            }
            ShapeType.BICYCLE -> {
                // Two separate wheel circles and a frame
                val wL = sdist(dx + maxR * 0.45f, dy - maxR * 0.35f) <= maxR * 0.25f && sdist(dx + maxR * 0.45f, dy - maxR * 0.35f) >= maxR * 0.11f
                val wR = sdist(dx - maxR * 0.45f, dy - maxR * 0.35f) <= maxR * 0.25f && sdist(dx - maxR * 0.45f, dy - maxR * 0.35f) >= maxR * 0.11f
                val fStrut = Math.abs((dx - maxR * 0.15f) + (dy + maxR * 0.15f) * 0.6f) <= 1.1f && dy >= -maxR * 0.65f && dy <= maxR * 0.35f
                wL || wR || fStrut
            }
            ShapeType.CRAB -> {
                // A body with separate claws and legs
                val crabBody = sdist(dx, dy + maxR * 0.1f) <= maxR * 0.35f
                val clawL = sdist(dx + maxR * 0.45f, dy - maxR * 0.22f) <= maxR * 0.15f
                val clawR = sdist(dx - maxR * 0.45f, dy - maxR * 0.22f) <= maxR * 0.15f
                val legL = dy >= maxR * 0.22f && dy <= maxR * 0.55f && dx <= -maxR * 0.22f && dx >= -maxR * 0.65f && (p.x % 3 == 0)
                val legR = dy >= maxR * 0.22f && dy <= maxR * 0.55f && dx >= maxR * 0.22f && dx <= maxR * 0.65f && (p.x % 3 == 0)
                crabBody || clawL || clawR || legL || legR
            }
            ShapeType.PUMPKIN -> {
                // A ribbed pumpkin with separate eyes and mouth
                val pumpkinOutline = r <= maxR * 0.55f
                val pEyeL = sdist(dx + maxR * 0.18f, dy + maxR * 0.1f) <= maxR * 0.11f
                val pEyeR = sdist(dx - maxR * 0.18f, dy + maxR * 0.1f) <= maxR * 0.11f
                val pMouth = dy >= -maxR * 0.25f && dy <= -maxR * 0.1f && Math.abs(dx) <= maxR * 0.32f
                pumpkinOutline && !pEyeL && !pEyeR && !pMouth
            }
            ShapeType.TEDDY_BEAR -> {
                // Head with separate ears and snout
                val tHead = sdist(dx, dy + maxR * 0.1f) <= maxR * 0.45f
                val tEarL = sdist(dx + maxR * 0.38f, dy + maxR * 0.45f) <= maxR * 0.18f
                val tEarR = sdist(dx - maxR * 0.38f, dy + maxR * 0.45f) <= maxR * 0.18f
                val tSnout = sdist(dx, dy - maxR * 0.1f) <= maxR * 0.15f
                tHead || tEarL || tEarR || tSnout
            }
            ShapeType.OWL -> {
                // A body with separate large eyes
                val owlBody = sdist(dx, dy + maxR * 0.1f) <= maxR * 0.48f && Math.abs(dx) <= maxR * 0.38f
                val owlEyeL = sdist(dx + maxR * 0.18f, dy + maxR * 0.32f) <= maxR * 0.15f
                val owlEyeR = sdist(dx - maxR * 0.18f, dy + maxR * 0.32f) <= maxR * 0.15f
                (owlBody || owlEyeL || owlEyeR) && !(owlEyeL && sdist(dx + maxR * 0.18f, dy + maxR * 0.32f) <= maxR * 0.05f) && !(owlEyeR && sdist(dx - maxR * 0.18f, dy + maxR * 0.32f) <= maxR * 0.05f)
            }
            ShapeType.TEAPOT -> {
                // A body with separate handle and spout
                val tBody = sdist(dx + maxR * 0.05f, dy + maxR * 0.1f) <= maxR * 0.42f
                val tSpout = dx >= maxR * 0.3f && dx <= maxR * 0.72f && dy >= -maxR * 0.15f && dy <= maxR * 0.15f && dy >= -maxR * 0.15f + (dx - maxR * 0.3f) * 0.4f
                val tHandle = sdist(dx + maxR * 0.42f, dy + maxR * 0.1f) <= maxR * 0.25f && sdist(dx + maxR * 0.42f, dy + maxR * 0.1f) >= maxR * 0.11f && dx <= -maxR * 0.32f
                tBody || tSpout || tHandle
            }
            ShapeType.GUITAR -> {
                // A body, neck, and separate tuning pegs
                val gBody = sdist(dx, dy - maxR * 0.28f) <= maxR * 0.38f || sdist(dx, dy - maxR * 0.1f) <= maxR * 0.28f
                val gNeck = Math.abs(dx) <= 1.1f && dy >= -maxR * 0.75f && dy <= -maxR * 0.1f
                val pegL = sdist(dx + maxR * 0.15f, dy + maxR * 0.8f) <= maxR * 0.08f
                val pegR = sdist(dx - maxR * 0.15f, dy + maxR * 0.8f) <= maxR * 0.08f
                gBody || gNeck || pegL || pegR
            }
            ShapeType.CANDLE -> {
                // A candle pillar with a separate flame tip
                val candleBody = Math.abs(dx) <= maxR * 0.18f && dy >= -maxR * 0.35f && dy <= maxR * 0.75f
                val candleFlame = sdist(dx, dy + maxR * 0.55f) <= maxR * 0.15f && Math.abs(dx) <= maxR * 0.12f * (1.1f - (-dy - maxR * 0.55f) / (maxR * 0.2f))
                candleBody || candleFlame
            }
            ShapeType.ICE_CREAM -> {
                // A cone with separate scoop circles on top
                val cone = dy >= -maxR * 0.1f && dy <= maxR * 0.82f && Math.abs(dx) <= maxR * 0.45f * (1.0f - (dy + maxR * 0.1f) / (maxR * 0.92f))
                val scoop1 = sdist(dx, dy + maxR * 0.25f) <= maxR * 0.28f
                val scoop2 = sdist(dx - maxR * 0.18f, dy + maxR * 0.1f) <= maxR * 0.22f
                val scoop3 = sdist(dx + maxR * 0.18f, dy + maxR * 0.1f) <= maxR * 0.22f
                cone || scoop1 || scoop2 || scoop3
            }
            ShapeType.CUPCAKE -> {
                // A base with a separate cherry on top
                val cakeBase = Math.abs(dx) <= maxR * 0.42f && dy >= maxR * 0.1f && dy <= maxR * 0.65f && Math.abs(dx) <= maxR * 0.32f + dy * 0.15f
                val frosting = sdist(dx, dy - maxR * 0.1f) <= maxR * 0.35f && dy <= maxR * 0.1f
                val cherry = sdist(dx, dy - maxR * 0.52f) <= maxR * 0.11f
                cakeBase || frosting || cherry
            }
            ShapeType.CLOCK -> {
                // A circle with separate hand arrows
                val clockRim = r >= maxR * 0.68f && r <= maxR * 0.82f
                val hrHand = Math.abs(dx) <= 1.0f && dy >= -maxR * 0.15f && dy <= maxR * 0.42f
                val minHand = Math.abs(dy + dx * 0.5f) <= 1.0f && dx >= -maxR * 0.52f && dx <= maxR * 0.15f
                clockRim || hrHand || minHand
            }
            ShapeType.RAINBOW -> {
                // Parallel curved arches
                val radIndex = ((r - maxR * 0.35f) / (maxR * 0.15f)).toInt()
                val remRainbow = (r - maxR * 0.35f) % (maxR * 0.15f)
                dy <= 0 && radIndex in 0..3 && remRainbow <= maxR * 0.08f && Math.abs(dx) <= maxR * 0.85f
            }
            ShapeType.SATELLITE -> {
                // A central body with separate solar panels
                val satelliteBody = sdist(dx, dy) <= maxR * 0.32f
                val sPanelL = dx <= -maxR * 0.45f && dx >= -maxR * 0.85f && Math.abs(dy) <= maxR * 0.18f
                val sPanelR = dx >= maxR * 0.45f && dx <= maxR * 0.85f && Math.abs(dy) <= maxR * 0.18f
                satelliteBody || sPanelL || sPanelR
            }
            ShapeType.SWORD -> {
                // A blade and guard with separate jewel
                val sBlade = Math.abs(dx - dy) <= 1.0f && sdist(dx, dy) <= maxR * 0.68f && dx >= -maxR * 0.42f && dy >= -maxR * 0.42f
                val guard = Math.abs(dx + dy) <= 1.0f && sdist(dx, dy) <= maxR * 0.38f
                val jewel = sdist(dx + maxR * 0.42f, dy + maxR * 0.42f) <= maxR * 0.11f
                sBlade || guard || jewel
            }
            ShapeType.KITE -> {
                // A diamond shape with separate bow ties on a string
                val kiteHead = Math.abs(dx) + Math.abs(dy + maxR * 0.22f) <= maxR * 0.45f
                val kiteString = Math.abs(dx - (dy - maxR * 0.22f) * 0.3f) <= 0.8f && dy >= -maxR * 0.1f && dy <= maxR * 0.72f
                val kiteBow = Math.abs(dy - maxR * 0.45f) <= maxR * 0.08f && Math.abs(dx - maxR * 0.08f) <= maxR * 0.18f
                kiteHead || kiteString || kiteBow
            }
            ShapeType.TREASURE_CHEST -> {
                // A chest with separate lock and gold coins
                val chestBody = Math.abs(dx) <= maxR * 0.65f && dy >= -maxR * 0.15f && dy <= maxR * 0.65f
                val chestCap = Math.abs(dx) <= maxR * 0.65f && dy >= -maxR * 0.55f && dy <= -maxR * 0.22f
                val chestLock = sdist(dx, dy + maxR * 0.05f) <= maxR * 0.12f
                (chestBody || chestCap) && !chestLock
            }
            ShapeType.SCISSORS -> {
                // Two crossed blades with separate finger loops
                val bldL = Math.abs((dx - maxR * 0.1f) + (dy - maxR * 0.1f)) <= 1.1f && dy >= -maxR * 0.55f && dy <= maxR * 0.35f
                val bldR = Math.abs((dx + maxR * 0.1f) - (dy - maxR * 0.1f)) <= 1.1f && dy >= -maxR * 0.55f && dy <= maxR * 0.35f
                val loopL = sdist(dx + maxR * 0.35f, dy + maxR * 0.52f) <= maxR * 0.22f && sdist(dx + maxR * 0.35f, dy + maxR * 0.52f) >= maxR * 0.08f
                val loopR = sdist(dx - maxR * 0.35f, dy + maxR * 0.52f) <= maxR * 0.22f && sdist(dx - maxR * 0.35f, dy + maxR * 0.52f) >= maxR * 0.08f
                bldL || bldR || loopL || loopR
            }
        }
    }

    private fun sdist(dx: Float, dy: Float): Float = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()

    private fun getNeighbors(p: Point, w: Int, h: Int): List<Point> {
        val list = mutableListOf<Point>()
        if (p.x > 0) list.add(Point(p.x - 1, p.y))
        if (p.x < w - 1) list.add(Point(p.x + 1, p.y))
        if (p.y > 0) list.add(Point(p.x, p.y - 1))
        if (p.y < h - 1) list.add(Point(p.x, p.y + 1))
        return list
    }

    private fun evaluateDifficulty(arrows: List<Arrow>, w: Int, h: Int): Float {
        // Build efficient 2D occupation board for blazingly fast checks
        val grid = Array(h) { IntArray(w) { 0 } }
        for (arrow in arrows) {
            for (p in arrow.path) {
                grid[p.y][p.x] = arrow.id
            }
        }

        val activeSet = arrows.toMutableSet()
        var totalAvailableMoves = 0
        var simulationSteps = 0

        while (activeSet.isNotEmpty()) {
            val freeArrows = activeSet.filter { !isArrowBlockedStatic(it, grid, w, h) }
            if (freeArrows.isEmpty()) return 0.0f // UNSOLVABLE layout!

            totalAvailableMoves += freeArrows.size
            simulationSteps++

            // Walk simulated clearing: pop one move to progress
            val toClear = freeArrows.first()
            for (p in toClear.path) {
                grid[p.y][p.x] = 0
            }
            activeSet.remove(toClear)
        }

        if (simulationSteps == 0) return 0.0f
        val avgMoves = totalAvailableMoves.toFloat() / simulationSteps
        // Higher score indicates tighter, more linear sequential clearing (more challenging and rewarding to solve!)
        return 100.0f / (avgMoves + 0.1f)
    }

    private fun isArrowBlockedStatic(arrow: Arrow, grid: Array<IntArray>, w: Int, h: Int): Boolean {
        val exitDir = arrow.exitDirection
        val head = arrow.head

        var currX = head.x + exitDir.x
        var currY = head.y + exitDir.y

        while (currX in 0 until w && currY in 0 until h) {
            val occupantId = grid[currY][currX]
            if (occupantId != 0) {
                return true
            }
            currX += exitDir.x
            currY += exitDir.y
        }
        return false
    }

    private fun hasOpposingArrows(arrows: List<Arrow>): Boolean {
        for (i in arrows.indices) {
            val a = arrows[i]
            val headA = a.head
            val dirA = a.exitDirection
            for (j in i + 1 until arrows.size) {
                val b = arrows[j]
                val headB = b.head
                val dirB = b.exitDirection
                
                if (dirA.x == -dirB.x && dirA.y == -dirB.y) {
                    if (dirA.x != 0) { // Horizontal exits
                        if (headA.y == headB.y) {
                            val facing = if (dirA.x > 0) headA.x < headB.x else headA.x > headB.x
                            if (facing) return true
                        }
                    } else if (dirA.y != 0) { // Vertical exits
                        if (headA.x == headB.x) {
                            val facing = if (dirA.y > 0) headA.y < headB.y else headA.y > headB.y
                            if (facing) return true
                        }
                    }
                }
            }
        }
        return false
    }

    private fun hasDiagonalOrJumpingSteps(arrows: List<Arrow>): Boolean {
        for (arrow in arrows) {
            val path = arrow.path
            for (i in 0 until path.size - 1) {
                val p1 = path[i]
                val p2 = path[i + 1]
                val manhattanDist = Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y)
                if (manhattanDist != 1) {
                    return true
                }
            }
        }
        return false
    }

    private var backtrackCount = 0

    private fun getOutwardScore(path: List<Point>, w: Int, h: Int): Float {
        if (path.size < 2) return 0f
        val head = path.last()
        val neck = path[path.size - 2]
        val dx = (head.x - neck.x).coerceIn(-1, 1)
        val dy = (head.y - neck.y).coerceIn(-1, 1)
        
        val cx = (w - 1) / 2f
        val cy = (h - 1) / 2f
        
        val vx = head.x - cx
        val vy = head.y - cy
        
        return dx * vx + dy * vy
    }

    private fun findValidOrientations(
        paths: List<List<Point>>,
        index: Int,
        currentArrows: MutableList<Arrow>,
        ran: Random,
        w: Int,
        h: Int,
        maxSteps: Int = 1000
    ): List<Arrow>? {
        if (backtrackCount > maxSteps) return null
        backtrackCount++

        if (index == paths.size) {
            val score = evaluateDifficulty(currentArrows, w, h)
            if (score > 0.0f) {
                return currentArrows.toList()
            }
            return null
        }

        val path = paths[index]
        val color = COLORS[ran.nextInt(COLORS.size)]

        val scoreNormal = getOutwardScore(path, w, h)
        val scoreReversed = getOutwardScore(path.asReversed(), w, h)
        
        // Try the outward-pointing direction first! This yields beautiful layouts and solves instantly.
        val orientations = if (scoreNormal >= scoreReversed) {
            listOf(path, path.asReversed())
        } else {
            listOf(path.asReversed(), path)
        }

        for (orientedPath in orientations) {
            val arrow = Arrow(index + 1, orientedPath, color)
            if (hasOpposingWithAny(arrow, currentArrows)) {
                continue
            }

            currentArrows.add(arrow)
            val result = findValidOrientations(paths, index + 1, currentArrows, ran, w, h, maxSteps)
            if (result != null) {
                return result
            }
            currentArrows.removeAt(currentArrows.size - 1)
        }

        return null
    }

    private fun hasOpposingWithAny(newArrow: Arrow, existing: List<Arrow>): Boolean {
        val headA = newArrow.head
        val dirA = newArrow.exitDirection
        
        for (b in existing) {
            val headB = b.head
            val dirB = b.exitDirection
            
            if (dirA.x == -dirB.x && dirA.y == -dirB.y) {
                if (dirA.x != 0) { // Horizontal exits
                    if (headA.y == headB.y) {
                        val facing = if (dirA.x > 0) headA.x < headB.x else headA.x > headB.x
                        if (facing) return true
                    }
                } else if (dirA.y != 0) { // Vertical exits
                    if (headA.x == headB.x) {
                        val facing = if (dirA.y > 0) headA.y < headB.y else headA.y > headB.y
                        if (facing) return true
                    }
                }
            }
        }
        return false
    }

    private fun isPathSelfBlocking(path: List<Point>, w: Int, h: Int): Boolean {
        if (path.size < 2) return false
        val head = path.last()
        val neck = path[path.size - 2]
        val dx = (head.x - neck.x).coerceIn(-1, 1)
        val dy = (head.y - neck.y).coerceIn(-1, 1)
        
        var cx = head.x + dx
        var cy = head.y + dy
        while (cx in 0 until w && cy in 0 until h) {
            if (path.any { it.x == cx && it.y == cy }) {
                return true
            }
            cx += dx
            cy += dy
        }
        return false
    }

    private fun getSilhouetteFromSvg(pathData: String, w: Int, h: Int): Set<Point> {
        val points = mutableSetOf<Point>()
        try {
            val path = androidx.core.graphics.PathParser.createPathFromPathData(pathData) ?: return emptySet()
            val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            
            val bounds = android.graphics.RectF()
            path.computeBounds(bounds, true)
            
            if (bounds.width() > 0 && bounds.height() > 0) {
                // Keep some padding so the shape fits beautifully within the grid boundaries
                val paddingX = if (w <= 12) 0.5f else 1.0f
                val paddingY = if (h <= 12) 0.5f else 1.0f
                
                val scaleX = (w - 2 * paddingX) / bounds.width()
                val scaleY = (h - 2 * paddingY) / bounds.height()
                val scale = minOf(scaleX, scaleY)
                
                val matrix = android.graphics.Matrix()
                matrix.postTranslate(-bounds.centerX(), -bounds.centerY())
                matrix.postScale(scale, scale)
                matrix.postTranslate(w / 2f, h / 2f)
                path.transform(matrix)
            }
            
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                style = android.graphics.Paint.Style.FILL
                isAntiAlias = false
            }
            canvas.drawPath(path, paint)
            
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val pixel = bitmap.getPixel(x, y)
                    if ((pixel ushr 24) > 128) {
                        points.add(Point(x, y))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return points
    }

    data class SvgRasterStats(
        val fillCount: Int,
        val fillAndStrokeCount: Int,
        val strokeCount: Int,
        val details: String
    )

    private fun getSvgRasterStats(pathData: String, w: Int, h: Int): SvgRasterStats {
        try {
            val path = androidx.core.graphics.PathParser.createPathFromPathData(pathData) 
                ?: return SvgRasterStats(0, 0, 0, "Invalid/empty path data")
            
            val bounds = android.graphics.RectF()
            path.computeBounds(bounds, true)
            
            if (bounds.width() > 0 && bounds.height() > 0) {
                val paddingX = if (w <= 12) 0.5f else 1.0f
                val paddingY = if (h <= 12) 0.5f else 1.0f
                
                val scaleX = (w - 2 * paddingX) / bounds.width()
                val scaleY = (h - 2 * paddingY) / bounds.height()
                val scale = minOf(scaleX, scaleY)
                
                val matrix = android.graphics.Matrix()
                matrix.postTranslate(-bounds.centerX(), -bounds.centerY())
                matrix.postScale(scale, scale)
                matrix.postTranslate(w / 2f, h / 2f)
                path.transform(matrix)
            }
            
            fun countCellsForPaint(configBlock: android.graphics.Paint.() -> Unit): Int {
                val bitmap = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(bitmap)
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    isAntiAlias = false
                    configBlock()
                }
                canvas.drawPath(path, paint)
                var count = 0
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        if ((bitmap.getPixel(x, y) ushr 24) > 128) {
                            count++
                        }
                    }
                }
                return count
            }
            
            val fillCount = countCellsForPaint {
                style = android.graphics.Paint.Style.FILL
            }
            
            val fillAndStrokeCount = countCellsForPaint {
                style = android.graphics.Paint.Style.FILL_AND_STROKE
                strokeWidth = 1.0f
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }
            
            val strokeCount = countCellsForPaint {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 1.0f
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeJoin = android.graphics.Paint.Join.ROUND
            }
            
            val details = "FILL: $fillCount, FILL_AND_STROKE: $fillAndStrokeCount, STROKE: $strokeCount"
            return SvgRasterStats(fillCount, fillAndStrokeCount, strokeCount, details)
        } catch (e: Exception) {
            return SvgRasterStats(0, 0, 0, "Error rendering: ${e.localizedMessage}")
        }
    }
}
