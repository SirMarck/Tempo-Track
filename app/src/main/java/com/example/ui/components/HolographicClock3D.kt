package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TempoAccent
import com.example.ui.theme.TempoMono
import com.example.utils.FormatUtils
import com.example.utils.rememberDeviceTilt
import kotlin.math.cos
import kotlin.math.sin

/**
 * Relógio Holográfico 3D com Giroscópio / Sensor de Movimento.
 * - Rosto frontal 100% alinhado e legível (base 0° de rotação).
 * - Reage de forma fluida e visível à inclinação física do smartphone.
 * - Anel toroidal chanfrado com glints de cristal e arcos de plasma neon.
 */
@Composable
fun HolographicClock3D(
    durationText: String,
    accumulatedEarnings: Double,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 230.dp,
    onClick: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val deviceTilt by rememberDeviceTilt()

    // Resposta viva à inclinação do celular (base 0 graus para máxima legibilidade)
    val targetPitch = if (!isPaused) deviceTilt.pitch else 0f
    val targetRoll = if (!isPaused) deviceTilt.roll else 0f

    val animatedPitch by animateFloatAsState(
        targetValue = targetPitch,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "clockPitch"
    )
    val animatedRoll by animateFloatAsState(
        targetValue = targetRoll,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "clockRoll"
    )

    // Respiração suave do arco neon quando em execução
    val infiniteTransition = rememberInfiniteTransition(label = "plasmaGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = if (isPaused) 0.6f else 0.85f,
        targetValue = if (isPaused) 0.6f else 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPaused) 0f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "plasmaRotation"
    )

    Box(
        modifier = modifier
            .size(sizeDp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .graphicsLayer {
                rotationX = animatedPitch
                rotationY = animatedRoll
                cameraDistance = 14f * density.density
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
            val radius = canvasWidth / 2f - 10f

            // 1. Base grafite profunda
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1B222B),
                        Color(0xFF10151B),
                        Color(0xFF090C0F)
                    ),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )

            // 2. 16 Facetas de cristal com chanfros luminosos
            val numSegments = 16
            for (i in 0 until numSegments) {
                val segAngle = (i.toFloat() / numSegments) * 360f
                val isGlint = (i % 3 == 0)
                drawArc(
                    color = if (isGlint) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f),
                    startAngle = segAngle,
                    sweepAngle = 18f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 14f)
                )
            }

            // Borda externa chanfrada com reflexo especular de vidro
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.12f),
                        Color.White.copy(alpha = 0.70f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(canvasWidth, canvasHeight)
                ),
                radius = radius + 6f,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // 3. Arcos de Plasma Neon Laranja Vibrante
            val plasmaColor = if (isPaused) Color(0xFFFFA726) else TempoAccent
            val plasmaGlow = if (isPaused) Color(0xFFFFB74D) else Color(0xFFFF9E58)

            // Arco Principal pulsante
            val startAngle1 = (rotationAngle - 70f) % 360f
            drawArc(
                color = plasmaGlow.copy(alpha = 0.38f * glowPulse),
                startAngle = startAngle1,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - radius + 8f, center.y - radius + 8f),
                size = Size((radius - 8f) * 2, (radius - 8f) * 2),
                style = Stroke(width = 12f, cap = StrokeCap.Round)
            )
            drawArc(
                color = plasmaColor,
                startAngle = startAngle1,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - radius + 8f, center.y - radius + 8f),
                size = Size((radius - 8f) * 2, (radius - 8f) * 2),
                style = Stroke(width = 6.5f, cap = StrokeCap.Round)
            )

            // Arco Secundário menor em oposição
            val startAngle2 = (rotationAngle + 140f) % 360f
            drawArc(
                color = plasmaGlow.copy(alpha = 0.28f * glowPulse),
                startAngle = startAngle2,
                sweepAngle = 40f,
                useCenter = false,
                topLeft = Offset(center.x - radius + 8f, center.y - radius + 8f),
                size = Size((radius - 8f) * 2, (radius - 8f) * 2),
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFFFFA24C),
                startAngle = startAngle2,
                sweepAngle = 40f,
                useCenter = false,
                topLeft = Offset(center.x - radius + 8f, center.y - radius + 8f),
                size = Size((radius - 8f) * 2, (radius - 8f) * 2),
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )

            // 4. Trilho com 60 ticks de precisão
            val tickRadius = radius - 22f
            for (i in 0 until 60) {
                val angleDeg = i * 6f
                val angleRad = Math.toRadians(angleDeg.toDouble() - 90.0)
                val isHourTick = i % 5 == 0
                val tickLen = if (isHourTick) 6f else 3.5f
                val startX = center.x + ((tickRadius - tickLen) * cos(angleRad)).toFloat()
                val startY = center.y + ((tickRadius - tickLen) * sin(angleRad)).toFloat()
                val endX = center.x + (tickRadius * cos(angleRad)).toFloat()
                val endY = center.y + (tickRadius * sin(angleRad)).toFloat()

                drawLine(
                    color = if (isHourTick) Color.White.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.12f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isHourTick) 1.8f else 1f,
                    cap = StrokeCap.Round
                )
            }

            // 5. Disco central de obsidiana
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F141A),
                        Color(0xFF07090C)
                    ),
                    center = center,
                    radius = radius - 26f
                ),
                radius = radius - 26f,
                center = center
            )
        }

        // Mostrador Central: Tempo Digital Monospace e Valor
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(
                text = durationText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = TempoMono,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = FormatUtils.formatCurrency(accumulatedEarnings),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = TempoMono,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isPaused) Color(0xFFFFA726) else TempoAccent
            )
        }
    }
}
