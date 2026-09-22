package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
import com.example.ui.theme.TempoSurface1
import com.example.utils.FormatUtils
import com.example.utils.rememberDeviceTilt
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Chrono-Quantum Holographic Reactor 3D.
 * - Rosto frontal 100% alinhado em repouso (0° base) para legibilidade perfeita.
 * - Reage em tempo real com física giroscópica suave à inclinação física do smartphone.
 * - Profundidade multi-plano com paralaxe reverso no núcleo.
 * - Múltiplos anéis orbitais concêntricos:
 *     1. Anel orbital externo com 4 fótons/satélites luminosos em revolução contínua.
 *     2. Anel de cristal chanfrado com glint especular que segue a inclinação física.
 *     3. Miras cardeais HUD técnicas (12h, 3h, 6h, 9h).
 *     4. Anel táquion com 48 dentes contra-rotativos.
 *     5. Arcos duplos de plasma neon vibrante com respiração cinética e cabeças de cometa.
 *     6. Ondas sonares pulsantes holográficas do centro.
 *     7. Núcleo quântico de obsidiana com radar cyber.
 */
@Composable
fun HolographicClock3D(
    durationText: String,
    accumulatedEarnings: Double,
    isPaused: Boolean,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 230.dp,
    effectiveRate: Double? = null,
    onClick: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val deviceTilt by rememberDeviceTilt()

    // Inclinação física suave do aparelho limitada para estabilidade
    val targetPitch = if (!isPaused) deviceTilt.pitch.coerceIn(-22f, 22f) else 0f
    val targetRoll = if (!isPaused) deviceTilt.roll.coerceIn(-22f, 22f) else 0f

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

    // Transições de Animações Contínuas
    val infiniteTransition = rememberInfiniteTransition(label = "chronoReactor")

    // Rotação primária no sentido horário dos arcos de plasma
    val primaryRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPaused) 0f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "primaryRotation"
    )

    // Contra-rotação dos marcadores táquion
    val counterRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = if (isPaused) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 24000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "counterRotation"
    )

    // Órbita rápida dos 4 satélites de energia
    val orbitalAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPaused) 0f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitalAngle"
    )

    // Pulso de respiração do plasma neon
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = if (isPaused) 0.6f else 0.8f,
        targetValue = if (isPaused) 0.6f else 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    // Ondas sonares pulsantes que nascem do núcleo
    val sonarWave1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPaused) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonarWave1"
    )

    val sonarWave2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPaused) 0f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, delayMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonarWave2"
    )

    Box(
        modifier = modifier
            .size(sizeDp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .graphicsLayer {
                rotationX = animatedPitch * 0.85f
                rotationY = animatedRoll * 0.85f
                cameraDistance = 16f * density.density
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height
            val center = Offset(canvasW / 2f, canvasH / 2f)
            val outerRadius = (canvasW / 2f) - 6.dp.toPx()

            // Vetor de inclinação para paralaxe e brilho especular físico
            val tiltMagnitude = sqrt(animatedRoll * animatedRoll + animatedPitch * animatedPitch)
            val tiltAngleDeg = Math.toDegrees(
                atan2(animatedPitch.toDouble(), animatedRoll.toDouble())
            ).toFloat()

            // Deslocamento de paralaxe 3D no núcleo escuro (profundidade simulada)
            val coreParallaxOffset = Offset(
                x = -animatedRoll * 0.30f,
                y = -animatedPitch * 0.30f
            )
            val deepCenter = center + coreParallaxOffset

            // ─── 1. BASE GRAFITE PROFUNDA COM PARALAXE ─────────────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF1E2630),
                        Color(0xFF11171E),
                        Color(0xFF07090C)
                    ),
                    center = deepCenter,
                    radius = outerRadius
                ),
                radius = outerRadius,
                center = center
            )

            // Micro-grade cibernética no fundo
            val gridSpacing = 24f
            val gridAlpha = 0.04f
            var xPos = center.x - outerRadius
            while (xPos <= center.x + outerRadius) {
                drawLine(
                    color = Color.White.copy(alpha = gridAlpha),
                    start = Offset(xPos, center.y - outerRadius),
                    end = Offset(xPos, center.y + outerRadius),
                    strokeWidth = 1f
                )
                xPos += gridSpacing
            }
            var yPos = center.y - outerRadius
            while (yPos <= center.y + outerRadius) {
                drawLine(
                    color = Color.White.copy(alpha = gridAlpha),
                    start = Offset(center.x - outerRadius, yPos),
                    end = Offset(center.x + outerRadius, yPos),
                    strokeWidth = 1f
                )
                yPos += gridSpacing
            }

            // ─── 2. ANEL ORBITAL EXTERNO COM 4 FÓTONS SATÉLITES ───────────────
            val orbitalRadius = outerRadius - 2.dp.toPx()
            val dashedStroke = Stroke(
                width = 1.2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f), phase = primaryRotation * 2f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = orbitalRadius,
                center = center,
                style = dashedStroke
            )

            // 4 Fótons orbitando a 90 graus um do outro
            val photonColors = listOf(
                TempoAccent,
                Color(0xFF38BDF8),
                Color(0xFFFBBF24),
                Color(0xFF34D399)
            )
            for (p in 0 until 4) {
                val photonAngleRad = Math.toRadians((orbitalAngle + (p * 90f)).toDouble())
                val px = center.x + (orbitalRadius * cos(photonAngleRad)).toFloat()
                val py = center.y + (orbitalRadius * sin(photonAngleRad)).toFloat()

                // Halo do fóton
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            photonColors[p].copy(alpha = 0.5f * pulseGlow),
                            Color.Transparent
                        ),
                        center = Offset(px, py),
                        radius = 12f
                    ),
                    radius = 12f,
                    center = Offset(px, py)
                )
                // Núcleo branco brillante do fóton
                drawCircle(
                    color = Color.White,
                    radius = 2.5f,
                    center = Offset(px, py)
                )
            }

            // ─── 3. MIRAS CARDEAIS HUD TÉCNICAS (12h, 3h, 6h, 9h) ─────────────
            val cardinalRadius = outerRadius - 8.dp.toPx()
            val cardinalAngles = floatArrayOf(270f, 0f, 90f, 180f) // Topo, Direita, Base, Esquerda
            cardinalAngles.forEach { ang ->
                val angRad = Math.toRadians(ang.toDouble())
                val cosA = cos(angRad).toFloat()
                val sinA = sin(angRad).toFloat()

                val pStart = Offset(
                    center.x + (cardinalRadius - 10f) * cosA,
                    center.y + (cardinalRadius - 10f) * sinA
                )
                val pEnd = Offset(
                    center.x + cardinalRadius * cosA,
                    center.y + cardinalRadius * sinA
                )
                drawLine(
                    color = TempoAccent.copy(alpha = 0.75f),
                    start = pStart,
                    end = pEnd,
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }

            // ─── 4. ANEL CHANFRADO COM REFLEXO ESPECULAR FÍSICO DO GIROSCÓPIO ───
            val crystalRadius = outerRadius - 12.dp.toPx()

            // 24 Facetas de corte do anel de cristal
            val numFacetas = 24
            for (i in 0 until numFacetas) {
                val segAngle = (i.toFloat() / numFacetas) * 360f
                val isGlint = (i % 4 == 0)
                drawArc(
                    color = if (isGlint) Color.White.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.08f),
                    startAngle = segAngle,
                    sweepAngle = 10f,
                    useCenter = false,
                    topLeft = Offset(center.x - crystalRadius, center.y - crystalRadius),
                    size = Size(crystalRadius * 2, crystalRadius * 2),
                    style = Stroke(width = 12f)
                )
            }

            // Reflexo Especular Dinâmico que se move ao inclinar o celular
            if (tiltMagnitude > 0.5f) {
                val flareIntensity = (tiltMagnitude / 22f).coerceIn(0.2f, 0.95f)
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = flareIntensity),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = tiltAngleDeg - 35f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(center.x - crystalRadius - 2f, center.y - crystalRadius - 2f),
                    size = Size((crystalRadius + 2f) * 2, (crystalRadius + 2f) * 2),
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }

            // ─── 5. ANEL TÁQUION COM 48 DENTES CONTRA-ROTATIVOS ────────────────
            val tachyonRadius = crystalRadius - 10.dp.toPx()
            val numTicks = 48
            for (i in 0 until numTicks) {
                val tickAngle = counterRotation + (i * (360f / numTicks))
                val tickRad = Math.toRadians(tickAngle.toDouble())
                val isMajor = (i % 4 == 0)
                val tickLen = if (isMajor) 7f else 3.5f
                val startDist = tachyonRadius - tickLen

                val p1 = Offset(
                    center.x + (startDist * cos(tickRad)).toFloat(),
                    center.y + (startDist * sin(tickRad)).toFloat()
                )
                val p2 = Offset(
                    center.x + (tachyonRadius * cos(tickRad)).toFloat(),
                    center.y + (tachyonRadius * sin(tickRad)).toFloat()
                )

                drawLine(
                    color = if (isMajor) TempoAccent.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.14f),
                    start = p1,
                    end = p2,
                    strokeWidth = if (isMajor) 1.8f else 1f,
                    cap = StrokeCap.Round
                )
            }

            // ─── 6. ONDAS SONARES HOLOGRÁFICAS EXPANSIVAS ─────────────────────
            if (!isPaused) {
                val maxWaveRadius = tachyonRadius - 4.dp.toPx()
                val minWaveRadius = maxWaveRadius * 0.35f

                // Onda 1
                val r1 = minWaveRadius + (maxWaveRadius - minWaveRadius) * sonarWave1
                val a1 = (1f - sonarWave1) * 0.28f * pulseGlow
                drawCircle(
                    color = TempoAccent.copy(alpha = a1),
                    radius = r1,
                    center = center,
                    style = Stroke(width = 1.5f)
                )

                // Onda 2
                val r2 = minWaveRadius + (maxWaveRadius - minWaveRadius) * sonarWave2
                val a2 = (1f - sonarWave2) * 0.28f * pulseGlow
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = a2),
                    radius = r2,
                    center = center,
                    style = Stroke(width = 1.2f)
                )
            }

            // ─── 7. ARCOS DUPLOS DE PLASMA NEON VIBRANTE ──────────────────────
            val plasmaRadius = tachyonRadius - 8.dp.toPx()
            val plasmaColor = if (isPaused) Color(0xFFFFA726) else TempoAccent
            val plasmaGlow = if (isPaused) Color(0xFFFFB74D) else Color(0xFFFF8A3D)

            // Arco Principal de 140°
            val startAngle1 = (primaryRotation - 70f) % 360f
            // Glow externo
            drawArc(
                color = plasmaGlow.copy(alpha = 0.40f * pulseGlow),
                startAngle = startAngle1,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - plasmaRadius, center.y - plasmaRadius),
                size = Size(plasmaRadius * 2, plasmaRadius * 2),
                style = Stroke(width = 12f, cap = StrokeCap.Round)
            )
            // Feixe central nítido
            drawArc(
                color = plasmaColor,
                startAngle = startAngle1,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(center.x - plasmaRadius, center.y - plasmaRadius),
                size = Size(plasmaRadius * 2, plasmaRadius * 2),
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )
            // Cabeça do cometa brilhante na ponta do arco principal
            val tipAngleRad = Math.toRadians((startAngle1 + 140f).toDouble())
            val tipX = center.x + (plasmaRadius * cos(tipAngleRad)).toFloat()
            val tipY = center.y + (plasmaRadius * sin(tipAngleRad)).toFloat()
            drawCircle(
                color = Color.White,
                radius = 3.5f,
                center = Offset(tipX, tipY)
            )
            drawCircle(
                color = plasmaColor.copy(alpha = 0.7f * pulseGlow),
                radius = 8f,
                center = Offset(tipX, tipY)
            )

            // Arco Secundário Oposto de 45°
            val startAngle2 = (primaryRotation + 140f) % 360f
            drawArc(
                color = plasmaGlow.copy(alpha = 0.30f * pulseGlow),
                startAngle = startAngle2,
                sweepAngle = 45f,
                useCenter = false,
                topLeft = Offset(center.x - plasmaRadius, center.y - plasmaRadius),
                size = Size(plasmaRadius * 2, plasmaRadius * 2),
                style = Stroke(width = 8f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFFFFA24C),
                startAngle = startAngle2,
                sweepAngle = 45f,
                useCenter = false,
                topLeft = Offset(center.x - plasmaRadius, center.y - plasmaRadius),
                size = Size(plasmaRadius * 2, plasmaRadius * 2),
                style = Stroke(width = 4.5f, cap = StrokeCap.Round)
            )

            // ─── 8. DISCO CENTRAL DE OBSIDIANA / NÚCLEO QUÂNTICO ──────────────
            val coreRadius = plasmaRadius - 12.dp.toPx()
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF131922),
                        Color(0xFF090D12),
                        Color(0xFF050709)
                    ),
                    center = deepCenter,
                    radius = coreRadius
                ),
                radius = coreRadius,
                center = center
            )

            // Círculos concêntricos de radar cibernético no núcleo
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = coreRadius * 0.75f,
                center = center,
                style = Stroke(width = 1f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = coreRadius * 0.50f,
                center = center,
                style = Stroke(width = 1f)
            )
            // Miras em cruz central sutis
            val crossSize = 14f
            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(center.x - crossSize, center.y),
                end = Offset(center.x + crossSize, center.y),
                strokeWidth = 1f
            )
            drawLine(
                color = Color.White.copy(alpha = 0.12f),
                start = Offset(center.x, center.y - crossSize),
                end = Offset(center.x, center.y + crossSize),
                strokeWidth = 1f
            )
        }

        // ─── MOSTRADOR CENTRAL DIGITAL E HUD FRONT ────────────────────────────
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            // Mini badge de status holográfico
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(if (isPaused) Color(0xFFFFA726) else TempoAccent)
                )
                Text(
                    text = if (isPaused) "PAUSADO" else "CHRONO LIVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = TempoMono,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = if (isPaused) Color(0xFFFFA726) else Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Horário Digital Grande de Alta Visibilidade Monospace
            Text(
                text = durationText,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = TempoMono,
                    fontSize = if (sizeDp < 200.dp) 24.sp else 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Badge de Ganhos Acumulados
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(
                        if (isPaused) Color(0xFF261D12) else Color(0xFF24150B)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                val displayText = if (effectiveRate != null && effectiveRate <= 0.0) {
                    "Sem taxa"
                } else {
                    FormatUtils.formatCurrency(accumulatedEarnings)
                }
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = TempoMono,
                        fontSize = if (sizeDp < 200.dp) 12.sp else 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isPaused) Color(0xFFFFA726) else TempoAccent
                )
            }
        }
    }
}
