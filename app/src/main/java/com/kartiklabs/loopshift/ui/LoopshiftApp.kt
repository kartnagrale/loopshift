package com.kartiklabs.loopshift.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
private const val SECTORS = 8
private const val RINGS = 4

@Composable
fun LoopshiftApp() {
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
            LoopshiftGame()
        }
    }
}

@Composable
private fun LoopshiftGame() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("loopshift", Context.MODE_PRIVATE) }

    var gateOffsets by remember { mutableStateOf(List(RINGS) { Random.nextInt(SECTORS) }) }
    var node by remember { mutableStateOf(randomNode()) }
    var ringIndex by remember { mutableIntStateOf(RINGS - 1) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var paused by remember { mutableStateOf(false) }
    var gameOver by remember { mutableStateOf(false) }
    var best by remember { mutableIntStateOf(prefs.getInt("best", 0)) }
    var tickMs by remember { mutableLongStateOf(1050L) }

    fun haptic(strong: Boolean = false) {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    if (strong) 45L else 18L,
                    if (strong) 150 else 70
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(if (strong) 45L else 18L)
        }
    }

    fun resetGame() {
        gateOffsets = List(RINGS) { Random.nextInt(SECTORS) }
        node = randomNode()
        ringIndex = RINGS - 1
        score = 0
        combo = 0
        lives = 3
        tickMs = 1050L
        paused = false
        gameOver = false
    }

    LaunchedEffect(paused, gameOver, node, ringIndex, gateOffsets) {
        if (paused || gameOver) return@LaunchedEffect
        delay(tickMs)
        val gate = gateOffsets[ringIndex]
        if (gate == node.sector) {
            if (ringIndex == 0) {
                combo += 1
                val flowBonus = if (combo >= 5) combo * 2 else combo
                score += 100 + flowBonus * 10
                if (score > best) {
                    best = score
                    prefs.edit().putInt("best", best).apply()
                }
                tickMs = max(430L, 1050L - (score / 500) * 55L)
                haptic(combo >= 5)
                node = randomNode()
                ringIndex = RINGS - 1
            } else {
                ringIndex -= 1
                haptic(false)
            }
        } else {
            lives -= 1
            combo = 0
            haptic(true)
            if (lives <= 0) {
                gameOver = true
            } else {
                node = randomNode()
                ringIndex = RINGS - 1
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Header(score = score, best = best, lives = lives, combo = combo, paused = paused) {
            paused = !paused
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            GameBoard(
                gateOffsets = gateOffsets,
                node = node,
                ringIndex = ringIndex,
                combo = combo,
                enabled = !paused && !gameOver,
                onRingTap = { tappedRing ->
                    gateOffsets = gateOffsets.toMutableList().also {
                        it[tappedRing] = (it[tappedRing] + 1) % SECTORS
                    }
                    haptic(false)
                }
            )

            if (paused && !gameOver) {
                OverlayCard("PAUSED", "Tap resume to hold the flow") { paused = false }
            }

            if (gameOver) {
                OverlayCard(
                    title = "FLOW BROKEN",
                    subtitle = "Score $score  •  Best $best",
                    button = "SHIFT AGAIN",
                    onAction = { resetGame() }
                )
            }
        }

        AnimatedVisibility(visible = combo >= 5 && !gameOver) {
            Text(
                text = "FLOW ×$combo",
                color = Lime,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                letterSpacing = 3.sp
            )
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tap a ring to rotate its gate • Align the glowing node before the pulse",
            color = Muted,
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Header(
    score: Int,
    best: Int,
    lives: Int,
    combo: Int,
    paused: Boolean,
    onPause: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "LOOPSHIFT",
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                letterSpacing = 3.sp
            )
            Text("FIND THE PATH. HOLD THE FLOW.", color = Muted, fontSize = 9.sp, letterSpacing = 1.6.sp)
        }
        FilledTonalButton(
            onClick = onPause,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(if (paused) "RESUME" else "PAUSE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }

    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("SCORE", score.toString(), Modifier.weight(1f))
        StatCard("BEST", best.toString(), Modifier.weight(1f))
        StatCard("FLOW", "×$combo", Modifier.weight(1f))
        StatCard("LIVES", "●".repeat(lives), Modifier.weight(1f), valueColor = Pink)
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier, valueColor: Color = Color.White) {
    Column(
        modifier
            .background(Panel, RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = Muted, fontSize = 8.sp, letterSpacing = 1.2.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}

@Composable
private fun GameBoard(
    gateOffsets: List<Int>,
    node: Node,
    ringIndex: Int,
    combo: Int,
    enabled: Boolean,
    onRingTap: (Int) -> Unit
) {
    val glow by animateFloatAsState(
        targetValue = if (combo >= 5) 1f else .58f,
        label = "glow"
    )

    Canvas(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { tap ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val distance = (tap - center).getDistance()
                    val maxRadius = size.minDimension * .45f
                    val spacing = maxRadius / RINGS
                    val ring = (distance / spacing).toInt()
                    if (ring in 0 until RINGS) {
                        onRingTap(ring)
                    }
                }
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension * .45f
        val ringSpacing = maxRadius / RINGS
        val stroke = size.minDimension * .018f
        val segmentSweep = 360f / SECTORS
        val gapSweep = 24f

        drawCircle(
            brush = Brush.radialGradient(
                listOf(Cyan.copy(alpha = .10f * glow), Color.Transparent),
                center = center,
                radius = maxRadius
            ),
            radius = maxRadius
        )

        repeat(RINGS) { i ->
            val radius = ringSpacing * (i + 1)
            val gateSector = gateOffsets[i]
            val rect = Size(radius * 2f, radius * 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)

            drawCircle(
                color = Color(0xFF1A2443),
                radius = radius,
                center = center,
                style = Stroke(width = stroke)
            )

            for (sector in 0 until SECTORS) {
                if (sector == gateSector) continue
                drawArc(
                    color = if (i == ringIndex) Cyan.copy(alpha = .95f) else Violet.copy(alpha = .58f),
                    startAngle = sector * segmentSweep - 90f + gapSweep / 2f,
                    sweepAngle = segmentSweep - gapSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = rect,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }

            val gateAngle = Math.toRadians((gateSector * segmentSweep - 90f).toDouble())
            val gatePoint = Offset(
                center.x + cos(gateAngle).toFloat() * radius,
                center.y + sin(gateAngle).toFloat() * radius
            )
            drawCircle(Cyan.copy(alpha = .22f), stroke * 1.5f, gatePoint)
            drawCircle(Cyan, stroke * .55f, gatePoint)
        }

        val activeRadius = ringSpacing * (ringIndex + 1)
        val angle = Math.toRadians((node.sector * segmentSweep - 90f).toDouble())
        val nodePoint = Offset(
            center.x + cos(angle).toFloat() * activeRadius,
            center.y + sin(angle).toFloat() * activeRadius
        )
        drawCircle(node.color.copy(alpha = .18f), stroke * 2.6f, nodePoint)
        drawCircle(node.color.copy(alpha = .48f), stroke * 1.65f, nodePoint)
        drawCircle(node.color, stroke * .78f, nodePoint)

        drawCircle(Color(0xFF101A33), ringSpacing * .48f, center)
        drawCircle(
            brush = Brush.radialGradient(
                listOf(node.color.copy(alpha = .85f), node.color.copy(alpha = .08f)),
                center = center,
                radius = ringSpacing * .42f
            ),
            radius = ringSpacing * .42f,
            center = center
        )
        drawCircle(Color.White.copy(alpha = .9f), stroke * .36f, center)
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
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .padding(34.dp)
            .fillMaxWidth()
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = Muted, textAlign = TextAlign.Center, fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                Text(button, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            }
        }
    }
}

private fun randomNode(): Node {
    val colors = listOf(Cyan, Pink, Lime, Violet)
    return Node(
        sector = Random.nextInt(SECTORS),
        color = colors.random()
    )
}
