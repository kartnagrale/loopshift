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
                null -> StartScreen(onRush = { mode = GameMode.RUSH }, onZen = { mode = GameMode.ZEN })
            }
        }
    }
}

@Composable
private fun StartScreen(onRush: () -> Unit, onZen: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("loopshift", Context.MODE_PRIVATE) }
    val best = remember { prefs.getInt("best", 0) }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(.45f))
        Logo()
        Spacer(Modifier.height(6.dp))
        Text("LOOPSHIFT", fontWeight = FontWeight.Black, fontSize = 34.sp, letterSpacing = 5.sp)
        Text("FIND THE PATH. HOLD THE FLOW.", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(24.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("RUSH", fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 2.sp)
                        Text("Route the pulse. Build FLOW. Survive the speed.", color = Muted, fontSize = 12.sp)
                    }
                    Text("BEST $best", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = onRush, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(16.dp)) {
                    Text("START RUSH", fontWeight = FontWeight.Black, letterSpacing = 1.5.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(onClick = onZen, colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = .82f)), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ZEN", fontWeight = FontWeight.Black, fontSize = 17.sp, letterSpacing = 1.5.sp)
                    Text("No lives. No game over. Route at your pace.", color = Muted, fontSize = 11.sp)
                }
                Text("PLAY", color = Lime, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.2.sp)
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().background(Panel.copy(alpha = .72f), RoundedCornerShape(16.dp)).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("DAILY SHIFT", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text("SOON", color = Muted, fontSize = 8.sp, letterSpacing = 1.sp)
        }

        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = .65f)), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("HOW TO PLAY", color = Cyan, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.4.sp)
                Text("1  Tap a ring to rotate its glowing gate.", color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
                Text("2  Align the gate with the incoming energy node.", color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
                Text("3  Clear all four rings to score and build FLOW.", color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.weight(1f))
        Text("KARTIK LABS  •  v0.5", color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
    }
}

@Composable
private fun Logo() {
    val infinite = rememberInfiniteTransition(label = "logo")
    val spin by infinite.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "spin")
    Canvas(Modifier.size(138.dp)) {
        val stroke = size.minDimension * .045f
        listOf(.42f, .31f, .20f).forEachIndexed { index, factor ->
            val r = size.minDimension * factor
            drawArc(
                color = listOf(Cyan, Violet, Pink)[index],
                startAngle = -78f + index * 22f + if (index == 0) spin else -spin * .4f,
                sweepAngle = 292f,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = Size(r * 2f, r * 2f),
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
    val isZen = mode == GameMode.ZEN

    var ringSteps by remember { mutableStateOf(List(RINGS) { Random.nextInt(SECTORS) }) }
    var node by remember { mutableStateOf(randomNode()) }
    var ringIndex by remember { mutableIntStateOf(RINGS - 1) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var paused by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var best by remember { mutableIntStateOf(prefs.getInt("best", 0)) }
    var tickMs by remember { mutableLongStateOf(if (isZen) 1800L else 1050L) }
    var reverseRing by remember { mutableIntStateOf(-1) }
    var feedback by remember { mutableIntStateOf(0) }
    var showTutorial by remember { mutableStateOf(!isZen && !prefs.getBoolean("tutorial_seen", false)) }
    var tutorialPage by remember { mutableIntStateOf(0) }
    val pulse = remember { Animatable(1f) }

    fun sectorOf(step: Int) = ((step % SECTORS) + SECTORS) % SECTORS

    fun haptic(strong: Boolean = false) {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(if (strong) 45L else 16L, if (strong) 150 else 65))
        } else {
            @Suppress("DEPRECATION") vibrator.vibrate(if (strong) 45L else 16L)
        }
    }

    fun rollHazard(): Int = if (!isZen && score >= 800 && Random.nextFloat() < .30f) Random.nextInt(RINGS) else -1

    fun resetGame() {
        ringSteps = List(RINGS) { Random.nextInt(SECTORS) }
        node = randomNode()
        ringIndex = RINGS - 1
        score = 0
        combo = 0
        lives = 3
        tickMs = if (isZen) 1800L else 1050L
        reverseRing = -1
        feedback = 0
        paused = false
        gameOver = false
    }

    LaunchedEffect(feedback) {
        if (feedback != 0) {
            delay(190)
            feedback = 0
        }
    }

    LaunchedEffect(paused, gameOver, node, ringIndex, showTutorial) {
        if (paused || gameOver || showTutorial) return@LaunchedEffect
        pulse.snapTo(1f)
        pulse.animateTo(0f, tween(tickMs.toInt(), easing = LinearEasing))

        if (sectorOf(ringSteps[ringIndex]) == node.sector) {
            feedback = 1
            if (ringIndex == 0) {
                combo += 1
                val flowBonus = if (combo >= 5) combo * 2 else combo
                score += 100 + flowBonus * 10
                if (!isZen && score > best) {
                    best = score
                    prefs.edit().putInt("best", best).apply()
                }
                if (!isZen) tickMs = max(430L, 1050L - (score / 500) * 55L)
                haptic(combo >= 5)
                node = randomNode()
                ringIndex = RINGS - 1
                reverseRing = rollHazard()
            } else {
                ringIndex -= 1
                haptic(false)
            }
        } else if (isZen) {
            feedback = -1
            combo = 0
            haptic(false)
        } else {
            feedback = -1
            lives -= 1
            combo = 0
            haptic(true)
            if (lives <= 0) gameOver = true else {
                node = randomNode()
                ringIndex = RINGS - 1
                reverseRing = rollHazard()
            }
        }
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GameHeader(mode, score, best, lives, combo, paused, onExit) { paused = !paused }
        Spacer(Modifier.height(10.dp))
        if (reverseRing >= 0) Text("REVERSE RING ACTIVE", color = Pink, fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)

        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            GameBoard(
                ringSteps = ringSteps,
                node = node,
                ringIndex = ringIndex,
                combo = combo,
                pulseProgress = pulse.value,
                reverseRing = reverseRing,
                feedback = feedback,
                enabled = !paused && !gameOver && !showTutorial,
                onRingTap = { tapped ->
                    ringSteps = ringSteps.toMutableList().also { it[tapped] += if (tapped == reverseRing) -1 else 1 }
                    haptic(false)
                }
            )

            if (showTutorial) {
                TutorialCard(tutorialPage,
                    onNext = {
                        if (tutorialPage < 2) tutorialPage++ else {
                            showTutorial = false
                            prefs.edit().putBoolean("tutorial_seen", true).apply()
                        }
                    },
                    onSkip = {
                        showTutorial = false
                        prefs.edit().putBoolean("tutorial_seen", true).apply()
                    }
                )
            } else if (paused && !gameOver) {
                OverlayCard("PAUSED", if (isZen) "Your puzzle is waiting" else "Tap resume to hold the flow") { paused = false }
            } else if (gameOver) {
                OverlayCard("FLOW BROKEN", "Score $score  •  Best $best", "SHIFT AGAIN") { resetGame() }
            }
        }

        AnimatedVisibility(combo >= 5 && !gameOver) {
            Text("FLOW ×$combo", color = Lime, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 3.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            when {
                isZen -> "ZEN • Wrong alignments do not cost lives • Route at your pace"
                reverseRing >= 0 -> "Pink ring rotates backwards • Align the node before the pulse expires"
                else -> "Tap a ring to rotate its gate • Align the node before the outer pulse expires"
            },
            color = Muted, textAlign = TextAlign.Center, fontSize = 12.sp, lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun GameHeader(mode: GameMode, score: Int, best: Int, lives: Int, combo: Int, paused: Boolean, onExit: () -> Unit, onPause: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("LOOPSHIFT", fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 3.sp)
            Text(if (mode == GameMode.ZEN) "ZEN MODE" else "FIND THE PATH. HOLD THE FLOW.", color = if (mode == GameMode.ZEN) Lime else Muted, fontSize = 9.sp, letterSpacing = 1.6.sp)
        }
        TextButton(onClick = onExit) { Text("HOME", color = Muted, fontSize = 10.sp) }
        FilledTonalButton(onClick = onPause, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
            Text(if (paused) "RESUME" else "PAUSE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("SCORE", score.toString(), Modifier.weight(1f))
        if (mode == GameMode.RUSH) StatCard("BEST", best.toString(), Modifier.weight(1f))
        StatCard("FLOW", "×$combo", Modifier.weight(1f))
        StatCard(if (mode == GameMode.ZEN) "MODE" else "LIVES", if (mode == GameMode.ZEN) "∞" else "●".repeat(lives), Modifier.weight(1f), if (mode == GameMode.ZEN) Lime else Pink)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier, valueColor: Color = Color.White) {
    Column(modifier.background(Panel, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, fontSize = 8.sp, letterSpacing = 1.2.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun GameBoard(
    ringSteps: List<Int>,
    node: Node,
    ringIndex: Int,
    combo: Int,
    pulseProgress: Float,
    reverseRing: Int,
    feedback: Int,
    enabled: Boolean,
    onRingTap: (Int) -> Unit
) {
    val glow by animateFloatAsState(if (combo >= 5) 1f else .58f, label = "glow")
    val feedbackAlpha by animateFloatAsState(if (feedback == 0) 0f else .16f, tween(90), label = "feedback")
    val a0 by animateFloatAsState(ringSteps[0] * SECTOR_ANGLE, spring(stiffness = Spring.StiffnessMediumLow), label = "r0")
    val a1 by animateFloatAsState(ringSteps[1] * SECTOR_ANGLE, spring(stiffness = Spring.StiffnessMediumLow), label = "r1")
    val a2 by animateFloatAsState(ringSteps[2] * SECTOR_ANGLE, spring(stiffness = Spring.StiffnessMediumLow), label = "r2")
    val a3 by animateFloatAsState(ringSteps[3] * SECTOR_ANGLE, spring(stiffness = Spring.StiffnessMediumLow), label = "r3")
    val angles = listOf(a0, a1, a2, a3)

    Canvas(
        Modifier.aspectRatio(1f).fillMaxWidth().pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures { tap ->
                val c = Offset(size.width / 2f, size.height / 2f)
                val distance = (tap - c).getDistance()
                val maxRadius = minOf(size.width, size.height).toFloat() * .45f
                val ring = (distance / (maxRadius / RINGS)).toInt()
                if (ring in 0 until RINGS) onRingTap(ring)
            }
        }
    ) {
        val c = center
        val maxRadius = size.minDimension * .45f
        val spacing = maxRadius / RINGS
        val stroke = size.minDimension * .018f
        val gapSweep = 24f

        drawCircle(if (feedback > 0) Lime.copy(feedbackAlpha) else Pink.copy(feedbackAlpha), maxRadius * 1.13f, c)

        val timerR = maxRadius * 1.07f
        val timerTop = Offset(c.x - timerR, c.y - timerR)
        drawArc(Color(0xFF17213F), -90f, 360f, false, timerTop, Size(timerR * 2, timerR * 2), style = Stroke(stroke * .45f, cap = StrokeCap.Round))
        drawArc(if (pulseProgress < .28f) Pink else Cyan, -90f, 360f * pulseProgress, false, timerTop, Size(timerR * 2, timerR * 2), style = Stroke(stroke * .65f, cap = StrokeCap.Round))
        drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha = .10f * glow), Color.Transparent), c, maxRadius), maxRadius, c)

        repeat(RINGS) { i ->
            val r = spacing * (i + 1)
            val rect = Size(r * 2, r * 2)
            val top = Offset(c.x - r, c.y - r)
            val ringColor = when { i == reverseRing -> Pink; i == ringIndex -> Cyan; else -> Violet }
            drawCircle(Color(0xFF1A2443), r, c, style = Stroke(stroke))

            for (sector in 0 until SECTORS) {
                if (sector == 0) continue
                drawArc(
                    ringColor.copy(alpha = if (i == ringIndex || i == reverseRing) .95f else .58f),
                    angles[i] + sector * SECTOR_ANGLE - 90f + gapSweep / 2f,
                    SECTOR_ANGLE - gapSweep,
                    false,
                    top,
                    rect,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }

            val gateAngle = Math.toRadians((angles[i] - 90f).toDouble())
            val gatePoint = Offset(c.x + cos(gateAngle).toFloat() * r, c.y + sin(gateAngle).toFloat() * r)
            drawCircle(ringColor.copy(alpha = .22f), stroke * 1.5f, gatePoint)
            drawCircle(ringColor, stroke * .55f, gatePoint)
        }

        val activeR = spacing * (ringIndex + 1)
        val nodeAngle = Math.toRadians((node.sector * SECTOR_ANGLE - 90f).toDouble())
        val p = Offset(c.x + cos(nodeAngle).toFloat() * activeR, c.y + sin(nodeAngle).toFloat() * activeR)
        drawCircle(node.color.copy(alpha = .18f), stroke * 2.6f, p)
        drawCircle(node.color.copy(alpha = .48f), stroke * 1.65f, p)
        drawCircle(node.color, stroke * .78f, p)

        drawCircle(Color(0xFF101A33), spacing * .48f, c)
        drawCircle(Brush.radialGradient(listOf(node.color.copy(alpha = .85f), node.color.copy(alpha = .08f)), c, spacing * .42f), spacing * .42f, c)
        drawCircle(Color.White.copy(alpha = .9f), stroke * .36f, c)
    }
}

@Composable
private fun TutorialCard(page: Int, onNext: () -> Unit, onSkip: () -> Unit) {
    val title = when (page) { 0 -> "ROUTE THE PULSE"; 1 -> "BEAT THE TIMER"; else -> "WATCH FOR PINK" }
    val text = when (page) {
        0 -> "Tap a ring to rotate its gap until the glowing gate lines up with the incoming energy node."
        1 -> "The outer countdown drains every step. Clear all four rings before it reaches zero to score."
        else -> "Later in a run, a pink ring may appear. Pink rings rotate in the opposite direction."
    }
    Card(colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = .98f)), shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(26.dp).fillMaxWidth()) {
        Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${page + 1} / 3", color = Muted, fontSize = 10.sp, letterSpacing = 1.4.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(10.dp))
            Text(text, color = Color.White.copy(alpha = .86f), textAlign = TextAlign.Center, fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(if (page == 2) "START" else "NEXT", fontWeight = FontWeight.Black) }
            TextButton(onClick = onSkip) { Text("SKIP", color = Muted) }
        }
    }
}

@Composable
private fun OverlayCard(title: String, subtitle: String, button: String = "RESUME", onAction: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel.copy(alpha = .96f)), shape = RoundedCornerShape(24.dp), modifier = Modifier.padding(34.dp).fillMaxWidth()) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Muted, textAlign = TextAlign.Center, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(button, fontWeight = FontWeight.Black, letterSpacing = 1.sp) }
        }
    }
}

private fun randomNode(): Node {
    val colors = listOf(Cyan, Pink, Lime, Violet)
    return Node(Random.nextInt(SECTORS), colors.random())
}
