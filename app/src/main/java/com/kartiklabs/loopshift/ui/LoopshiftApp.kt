package com.kartiklabs.loopshift.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

private val Void = Color(0xFF050816)
private val Panel = Color(0xFF0A1024)
private val Cyan = Color(0xFF55F7FF)
private val Violet = Color(0xFF9B6BFF)
private val Pink = Color(0xFFFF4FA3)
private val Lime = Color(0xFFB9FF66)
private val Muted = Color(0xFF7180A5)

private data class Node(val sector: Int, val color: Color)
private enum class GameMode { RUSH, ZEN }

private const val SECTORS = 8
private const val RINGS = 4
private const val SECTOR_ANGLE = 360f / SECTORS

@Composable
fun LoopshiftApp() {
    var mode by remember { mutableStateOf<GameMode?>(null) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Cyan,
            secondary = Violet,
            tertiary = Pink,
            background = Void,
            surface = Panel,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(Modifier.fillMaxSize(), color = Void) {
            when (mode) {
                GameMode.RUSH -> LoopshiftGame(GameMode.RUSH) { mode = null }
                GameMode.ZEN -> LoopshiftGame(GameMode.ZEN) { mode = null }
                null -> StartScreen(
                    onRush = { mode = GameMode.RUSH },
                    onZen = { mode = GameMode.ZEN }
                )
            }
        }
    }
}

@Composable
private fun StartScreen(onRush: () -> Unit, onZen: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("loopshift", Context.MODE_PRIVATE) }
    val best = prefs.getInt("best", 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(.45f))
        Logo()
        Text("LOOPSHIFT", fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = 5.sp)
        Text("FIND THE PATH. HOLD THE FLOW.", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Panel),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("RUSH", fontWeight = FontWeight.Black, fontSize = 22.sp)
                        Text("Starts easy. Builds into chaos.", color = Muted, fontSize = 12.sp)
                    }
                    Text("BEST $best", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onRush,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("START RUSH", fontWeight = FontWeight.Black)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            onClick = onZen,
            colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = .82f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("ZEN", fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text("No timer. No lives. Just solve.", color = Muted, fontSize = 11.sp)
                }
                Text("PLAY", color = Lime, fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Difficulty now ramps from 2 → 3 → 4 rings", color = Muted, fontSize = 11.sp)
        Spacer(Modifier.weight(1f))
        Text("KARTIK LABS  •  v0.6", color = Muted, fontSize = 9.sp)
    }
}

@Composable
private fun Logo() {
    val transition = rememberInfiniteTransition(label = "logo")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "spin"
    )

    Canvas(Modifier.size(138.dp)) {
        val stroke = size.minDimension * .045f
        listOf(.42f, .31f, .20f).forEachIndexed { index, factor ->
            val radius = size.minDimension * factor
            drawArc(
                color = listOf(Cyan, Violet, Pink)[index],
                startAngle = -78f + index * 22f + if (index == 0) spin else -spin * .4f,
                sweepAngle = 292f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        drawCircle(Color.White, stroke * .65f, center)
    }
}

@Composable
private fun LoopshiftGame(mode: GameMode, onExit: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("loopshift", Context.MODE_PRIVATE) }
    val zen = mode == GameMode.ZEN

    var steps by remember { mutableStateOf(List(RINGS) { Random.nextInt(SECTORS) }) }
    var node by remember { mutableStateOf(randomNode()) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var paused by remember { mutableStateOf(false) }
    var over by remember { mutableStateOf(false) }
    var best by remember { mutableIntStateOf(prefs.getInt("best", 0)) }
    var reverse by remember { mutableIntStateOf(-1) }
    var feedback by remember { mutableIntStateOf(0) }
    var tutorial by remember { mutableStateOf(!zen && !prefs.getBoolean("tutorial_seen", false)) }
    var page by remember { mutableIntStateOf(0) }

    val activeRings = if (zen) 4 else when {
        score < 500 -> 2
        score < 1500 -> 3
        else -> 4
    }

    var ringIndex by remember { mutableIntStateOf(activeRings - 1) }

    val tickMs = when {
        zen -> Long.MAX_VALUE
        score < 500 -> 3200L
        score < 1500 -> 2700L
        score < 3000 -> 2200L
        else -> max(1100L, 2200L - ((score - 3000) / 500) * 100L)
    }

    val pulse = remember { Animatable(1f) }

    fun sector(step: Int) = ((step % SECTORS) + SECTORS) % SECTORS

    fun haptic(strong: Boolean = false) {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    if (strong) 40L else 14L,
                    if (strong) 140 else 60
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (strong) 40L else 14L)
        }
    }

    fun hazard(): Int =
        if (!zen && score >= 3000 && Random.nextFloat() < .22f) Random.nextInt(activeRings) else -1

    fun reset() {
        node = randomNode()
        steps = List(RINGS) { Random.nextInt(SECTORS) }
        score = 0
        combo = 0
        lives = 3
        ringIndex = 1
        reverse = -1
        over = false
        paused = false
    }

    LaunchedEffect(feedback) {
        if (feedback != 0) {
            delay(180)
            feedback = 0
        }
    }

    LaunchedEffect(paused, over, node, ringIndex, tutorial, activeRings) {
        if (paused || over || tutorial) return@LaunchedEffect

        if (zen) {
            pulse.snapTo(1f)
            return@LaunchedEffect
        }

        pulse.snapTo(1f)
        pulse.animateTo(0f, tween(tickMs.toInt(), easing = LinearEasing))

        if (sector(steps[ringIndex]) == node.sector) {
            feedback = 1
            if (ringIndex == 0) {
                combo++
                score += 100 + if (combo >= 5) combo * 20 else combo * 10

                if (score > best) {
                    best = score
                    prefs.edit().putInt("best", best).apply()
                }

                haptic(combo >= 5)
                node = randomNode()
                ringIndex = when {
                    score < 500 -> 1
                    score < 1500 -> 2
                    else -> 3
                }
                reverse = hazard()
            } else {
                ringIndex--
                haptic()
            }
        } else {
            feedback = -1
            lives--
            combo = 0
            haptic(true)
            if (lives <= 0) {
                over = true
            } else {
                node = randomNode()
                ringIndex = activeRings - 1
                reverse = hazard()
            }
        }
    }

    LaunchedEffect(activeRings) {
        if (ringIndex >= activeRings) ringIndex = activeRings - 1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header(mode, score, best, lives, combo, paused, onExit) { paused = !paused }
        Spacer(Modifier.height(8.dp))

        Text(
            text = if (zen) "NO TIMER" else "$activeRings RINGS  •  ${tickMs / 1000f}s",
            color = if (zen) Lime else Cyan,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )

        if (reverse >= 0) {
            Text("REVERSE RING ACTIVE", color = Pink, fontSize = 10.sp, fontWeight = FontWeight.Black)
        }

        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.Center
        ) {
            GameBoard(
                steps = steps,
                node = node,
                ringIndex = ringIndex,
                activeRings = activeRings,
                combo = combo,
                pulse = if (zen) 1f else pulse.value,
                reverse = reverse,
                feedback = feedback,
                enabled = !paused && !over && !tutorial,
                onTap = { tapped ->
                    if (tapped < activeRings) {
                        steps = steps.toMutableList().also {
                            it[tapped] += if (tapped == reverse) -1 else 1
                        }
                        haptic()
                    }
                }
            )

            if (tutorial) {
                TutorialCard(
                    page = page,
                    onNext = {
                        if (page < 2) page++ else {
                            tutorial = false
                            prefs.edit().putBoolean("tutorial_seen", true).apply()
                        }
                    },
                    onSkip = {
                        tutorial = false
                        prefs.edit().putBoolean("tutorial_seen", true).apply()
                    }
                )
            } else if (paused && !over) {
                OverlayCard("PAUSED", "Take your time") { paused = false }
            } else if (over) {
                OverlayCard("FLOW BROKEN", "Score $score  •  Best $best", "SHIFT AGAIN") { reset() }
            }
        }

        AnimatedVisibility(combo >= 5 && !over) {
            Text("FLOW ×$combo", color = Lime, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }

        Text(
            text = if (zen) {
                "ZEN • Tap until each gate aligns. There is no countdown."
            } else {
                when (activeRings) {
                    2 -> "WARM UP • Only 2 rings. Learn the rhythm."
                    3 -> "NICE • Third ring unlocked."
                    else -> "FULL SHIFT • All four rings active."
                }
            },
            color = Muted,
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Header(
    mode: GameMode,
    score: Int,
    best: Int,
    lives: Int,
    combo: Int,
    paused: Boolean,
    onExit: () -> Unit,
    onPause: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("LOOPSHIFT", fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 3.sp)
            Text(if (mode == GameMode.ZEN) "ZEN MODE" else "RUSH MODE", color = Muted, fontSize = 9.sp)
        }
        TextButton(onClick = onExit) { Text("HOME", color = Muted) }
        FilledTonalButton(onClick = onPause) {
            Text(if (paused) "RESUME" else "PAUSE", fontSize = 10.sp)
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Stat("SCORE", "$score", Modifier.weight(1f))
        if (mode == GameMode.RUSH) Stat("BEST", "$best", Modifier.weight(1f))
        Stat("FLOW", "×$combo", Modifier.weight(1f))
        Stat(
            if (mode == GameMode.ZEN) "MODE" else "LIVES",
            if (mode == GameMode.ZEN) "∞" else "●".repeat(lives),
            Modifier.weight(1f),
            if (mode == GameMode.ZEN) Lime else Pink
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier, color: Color = Color.White) {
    Column(
        modifier = modifier
            .background(Panel, RoundedCornerShape(14.dp))
            .padding(9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Muted, fontSize = 8.sp)
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun GameBoard(
    steps: List<Int>,
    node: Node,
    ringIndex: Int,
    activeRings: Int,
    combo: Int,
    pulse: Float,
    reverse: Int,
    feedback: Int,
    enabled: Boolean,
    onTap: (Int) -> Unit
) {
    val animated = steps.mapIndexed { index, step ->
        animateFloatAsState(
            targetValue = step.toFloat(),
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "ring$index"
        ).value
    }

    Canvas(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures { tap ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val distance = (tap - center).getDistance()
                        val maxRadius = minOf(size.width, size.height) * .45f
                        val ring = (distance / (maxRadius / RINGS)).toInt()
                        if (ring in 0 until activeRings) onTap(ring)
                    }
                }
            }
    ) {
        val center = center
        val maxRadius = size.minDimension * .45f
        val spacing = maxRadius / RINGS
        val stroke = size.minDimension * .018f
        val sweep = 360f / SECTORS

        if (feedback != 0) {
            drawCircle(
                color = (if (feedback > 0) Lime else Pink).copy(alpha = .14f),
                radius = maxRadius * 1.13f,
                center = center
            )
        }

        if (pulse < 1f) {
            val timerRadius = maxRadius * 1.07f
            drawArc(
                color = if (pulse < .28f) Pink else Cyan,
                startAngle = -90f,
                sweepAngle = 360f * pulse,
                useCenter = false,
                topLeft = Offset(center.x - timerRadius, center.y - timerRadius),
                size = Size(timerRadius * 2f, timerRadius * 2f),
                style = Stroke(stroke * .65f, cap = StrokeCap.Round)
            )
        }

        repeat(RINGS) { i ->
            val radius = spacing * (i + 1)
            val active = i < activeRings
            val ringColor = when {
                i == reverse -> Pink
                i == ringIndex -> Cyan
                active -> Violet
                else -> Muted.copy(alpha = .18f)
            }

            drawCircle(Color(0xFF1A2443), radius, center, style = Stroke(stroke))

            if (active) {
                val gateAngle = animated[i] * SECTOR_ANGLE
                for (sector in 0 until SECTORS) {
                    val centerAngle = sector * sweep + gateAngle
                    drawArc(
                        color = ringColor.copy(alpha = if (i == ringIndex || i == reverse) .95f else .58f),
                        startAngle = centerAngle - 90f + 12f,
                        sweepAngle = sweep - 24f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f),
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }

                val gateRadians = Math.toRadians((gateAngle - 90f).toDouble())
                drawCircle(
                    color = ringColor,
                    radius = stroke * .6f,
                    center = Offset(
                        center.x + cos(gateRadians).toFloat() * radius,
                        center.y + sin(gateRadians).toFloat() * radius
                    )
                )
            }
        }

        val activeRadius = spacing * (ringIndex + 1)
        val nodeRadians = Math.toRadians((node.sector * sweep - 90f).toDouble())
        val nodePoint = Offset(
            center.x + cos(nodeRadians).toFloat() * activeRadius,
            center.y + sin(nodeRadians).toFloat() * activeRadius
        )
        drawCircle(node.color.copy(alpha = .25f), stroke * 2.5f, nodePoint)
        drawCircle(node.color, stroke * .8f, nodePoint)
        drawCircle(Color(0xFF101A33), spacing * .48f, center)
        drawCircle(Color.White, stroke * .35f, center)
    }
}

@Composable
private fun TutorialCard(page: Int, onNext: () -> Unit, onSkip: () -> Unit) {
    val titles = listOf("ROUTE THE PULSE", "TAKE YOUR TIME", "DIFFICULTY GROWS")
    val texts = listOf(
        "Tap a ring until its glowing gate lines up with the energy node.",
        "Rush now starts with over 3 seconds and only two rings.",
        "More rings and reverse hazards unlock only after you build confidence."
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = .98f)),
        modifier = Modifier.padding(26.dp).fillMaxWidth()
    ) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${page + 1} / 3", color = Muted)
            Text(titles[page], fontWeight = FontWeight.Black, fontSize = 21.sp)
            Spacer(Modifier.height(10.dp))
            Text(texts[page], color = Color.White.copy(alpha = .86f), textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                Text(if (page == 2) "START" else "NEXT")
            }
            TextButton(onClick = onSkip) { Text("SKIP", color = Muted) }
        }
    }
}

@Composable
private fun OverlayCard(
    title: String,
    subtitle: String,
    button: String = "RESUME",
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = .96f)),
        modifier = Modifier.padding(34.dp).fillMaxWidth()
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 24.sp)
            Text(subtitle, color = Muted)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                Text(button, fontWeight = FontWeight.Black)
            }
        }
    }
}

private fun randomNode(): Node =
    Node(Random.nextInt(SECTORS), listOf(Cyan, Pink, Lime, Violet).random())
