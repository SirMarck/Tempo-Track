package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.utils.DeviceTilt
import kotlin.math.cos
import kotlin.math.sin

/**
 * Discos planetários e efeitos cósmicos expandidos com transparência e física de inclinação.
 * Renderizados em segundo plano por toda a tela (atrás de todos os botões, cartões e listas)
 * assim que o cronômetro é iniciado.
 */
@Composable
fun PlanetaryBackgroundDisks(
    isEnabled: Boolean,
    deviceTilt: DeviceTilt,
    modifier: Modifier = Modifier
) {
    // Opacidade suave animada: visível com transparência elegante quando habilitado
    val planetaryAlpha by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.0f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "planetaryDisksAlpha"
    )

    // Rotações orbitais contínuas em períodos diferenciados
    val infiniteTransition = rememberInfiniteTransition(label = "planetaryRevolutions")

    val ringRotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 70000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation1"
    )

    val ringRotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 90000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation2"
    )

    val ringRotation3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation3"
    )

    // Pulso sutil de respiração do campo estelar e das nebulosas
    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathPhase"
    )

    // Cintilação suave das estrelas de fundo
    val twinklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "twinklePhase"
    )

    // Coordenadas relativas normalizadas (x, y, tamanho, fase) para as micro-estrelas de fundo
    val stars = remember {
        listOf(
            Triple(0.08f, 0.12f, 1.8f),
            Triple(0.22f, 0.08f, 1.4f),
            Triple(0.85f, 0.07f, 2.0f),
            Triple(0.92f, 0.20f, 1.5f),
            Triple(0.05f, 0.45f, 1.6f),
            Triple(0.94f, 0.55f, 2.2f),
            Triple(0.12f, 0.72f, 1.4f),
            Triple(0.88f, 0.76f, 1.7f),
            Triple(0.28f, 0.92f, 2.0f),
            Triple(0.45f, 0.96f, 1.5f),
            Triple(0.70f, 0.94f, 1.8f),
            Triple(0.40f, 0.18f, 1.3f),
            Triple(0.62f, 0.35f, 1.6f),
            Triple(0.35f, 0.65f, 1.4f),
            Triple(0.75f, 0.60f, 1.9f),
            Triple(0.18f, 0.38f, 1.5f)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        if (planetaryAlpha <= 0.005f) return@Canvas

        val w = size.width
        val h = size.height

        // Reação giroscópica sutil limitada por camadas (paralaxe 3D)
        val tiltX = deviceTilt.roll.coerceIn(-20f, 20f) * 1.6f
        val tiltY = deviceTilt.pitch.coerceIn(-20f, 20f) * 1.6f

        // ═══════════════════════════════════════════════════════════════════════
        // 0. NEBULOSAS CÓSMICAS SUAVES (Camada mais profunda de ambiente)
        // ═══════════════════════════════════════════════════════════════════════
        // Nebulosa 1: Âmbar/Bronze na região sul/centro
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF6B35).copy(alpha = 0.06f * planetaryAlpha * breathPhase),
                    Color(0xFF8A3B18).copy(alpha = 0.03f * planetaryAlpha),
                    Color.Transparent
                ),
                center = Offset(w * 0.65f + tiltX * 0.5f, h * 0.75f + tiltY * 0.5f),
                radius = w * 0.75f
            ),
            center = Offset(w * 0.65f + tiltX * 0.5f, h * 0.75f + tiltY * 0.5f),
            radius = w * 0.75f
        )

        // Nebulosa 2: Ciano/Safira na região norte/noroeste
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF38BDF8).copy(alpha = 0.05f * planetaryAlpha * breathPhase),
                    Color(0xFF0C243B).copy(alpha = 0.02f * planetaryAlpha),
                    Color.Transparent
                ),
                center = Offset(w * 0.25f - tiltX * 0.5f, h * 0.30f - tiltY * 0.5f),
                radius = w * 0.65f
            ),
            center = Offset(w * 0.25f - tiltX * 0.5f, h * 0.30f - tiltY * 0.5f),
            radius = w * 0.65f
        )

        // ═══════════════════════════════════════════════════════════════════════
        // 1. CONSTELAÇÃO DE MICRO-ESTRELAS CINTILANTES
        // ═══════════════════════════════════════════════════════════════════════
        stars.forEachIndexed { i, star ->
            val starX = w * star.first + tiltX * 0.4f
            val starY = h * star.second + tiltY * 0.4f
            val pulse = (sin(twinklePhase + i * 0.7f) + 1f) / 2f
            val starAlpha = (0.20f + pulse * 0.45f) * planetaryAlpha
            val starColor = if (i % 3 == 0) Color(0xFFFFB088) else Color(0xFFBAE6FD)

            drawCircle(
                color = starColor.copy(alpha = starAlpha),
                center = Offset(starX, starY),
                radius = star.third * (0.8f + pulse * 0.4f)
            )
        }

        // ═══════════════════════════════════════════════════════════════════════
        // 2. DISCO PLANETÁRIO INFERIOR (Atrás dos Botões Inferiores)
        // ═══════════════════════════════════════════════════════════════════════
        val planet1Center = Offset(w * 0.80f + tiltX, h * 0.88f + tiltY)
        val planet1Radius = w * 0.27f * breathPhase

        // Atmosfera planetária gasosa
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF6B35).copy(alpha = 0.16f * planetaryAlpha),
                    Color(0xFF9E421A).copy(alpha = 0.09f * planetaryAlpha),
                    Color(0xFF26140E).copy(alpha = 0.04f * planetaryAlpha),
                    Color.Transparent
                ),
                center = planet1Center,
                radius = planet1Radius
            ),
            center = planet1Center,
            radius = planet1Radius
        )

        // Núcleo com gradiente esférico
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF9466).copy(alpha = 0.14f * planetaryAlpha),
                    Color.Transparent
                ),
                center = planet1Center - Offset(planet1Radius * 0.22f, planet1Radius * 0.22f),
                radius = planet1Radius * 0.65f
            ),
            center = planet1Center - Offset(planet1Radius * 0.22f, planet1Radius * 0.22f),
            radius = planet1Radius * 0.65f
        )

        // Sistema de Anéis Planetários Triplos estilo Saturno
        val ring1Tilt = -26f + (deviceTilt.roll * 0.25f)
        withTransform({
            translate(planet1Center.x, planet1Center.y)
            rotate(ring1Tilt)
            scale(scaleX = 1f, scaleY = 0.30f, pivot = Offset.Zero)
        }) {
            // Anel A: Faixa interna luminosa
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFF8A50).copy(alpha = 0.18f * planetaryAlpha),
                        Color(0xFFFFAC7E).copy(alpha = 0.25f * planetaryAlpha),
                        Color.Transparent
                    ),
                    center = Offset.Zero,
                    radius = planet1Radius * 1.6f
                ),
                center = Offset.Zero,
                radius = planet1Radius * 1.55f,
                style = Stroke(width = 20f)
            )

            // Divisão de Cassini e Anel B fino tracejado
            drawCircle(
                color = Color(0xFFFFBCA0).copy(alpha = 0.30f * planetaryAlpha),
                center = Offset.Zero,
                radius = planet1Radius * 1.84f,
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(36f, 18f, 12f, 18f),
                        ringRotation1 * 1.6f
                    )
                )
            )

            // Anel C: Disco externo com poeira orbital suave
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFF6B35).copy(alpha = 0.20f * planetaryAlpha),
                        Color(0xFF38BDF8).copy(alpha = 0.16f * planetaryAlpha),
                        Color(0xFFFF6B35).copy(alpha = 0.08f * planetaryAlpha),
                        Color(0xFF38BDF8).copy(alpha = 0.20f * planetaryAlpha)
                    ),
                    center = Offset.Zero
                ),
                center = Offset.Zero,
                radius = planet1Radius * 2.30f,
                style = Stroke(
                    width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(22f, 26f), -ringRotation2)
                )
            )

            // Lua 1: Maior, orbitando no anel C
            val moon1AngleRad = Math.toRadians((ringRotation1 * 1.2).toDouble())
            val moon1Dist = planet1Radius * 2.30f
            val moon1X = moon1Dist * cos(moon1AngleRad).toFloat()
            val moon1Y = moon1Dist * sin(moon1AngleRad).toFloat()
            drawCircle(
                color = Color(0xFFFFBA94).copy(alpha = 0.65f * planetaryAlpha),
                center = Offset(moon1X, moon1Y),
                radius = 3.5f
            )
            drawCircle(
                color = Color(0xFFFF6B35).copy(alpha = 0.25f * planetaryAlpha),
                center = Offset(moon1X, moon1Y),
                radius = 8f
            )

            // Lua 2: Menor, em contra-órbita interna
            val moon2AngleRad = Math.toRadians((-ringRotation2 * 0.9).toDouble())
            val moon2Dist = planet1Radius * 1.45f
            val moon2X = moon2Dist * cos(moon2AngleRad).toFloat()
            val moon2Y = moon2Dist * sin(moon2AngleRad).toFloat()
            drawCircle(
                color = Color(0xFFBAE6FD).copy(alpha = 0.50f * planetaryAlpha),
                center = Offset(moon2X, moon2Y),
                radius = 2.2f
            )
        }

        // ═══════════════════════════════════════════════════════════════════════
        // 3. DISCO PLANETÁRIO SUPERIOR / ESQUERDO
        // ═══════════════════════════════════════════════════════════════════════
        val planet2Center = Offset(w * 0.16f - tiltX * 0.7f, h * 0.22f - tiltY * 0.7f)
        val planet2Radius = w * 0.15f

        // Corpo celeste etéreo ciano/celeste
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF38BDF8).copy(alpha = 0.12f * planetaryAlpha),
                    Color(0xFF0F2B48).copy(alpha = 0.06f * planetaryAlpha),
                    Color.Transparent
                ),
                center = planet2Center,
                radius = planet2Radius
            ),
            center = planet2Center,
            radius = planet2Radius
        )

        // Anel celeste Kepleriano inclinado em ~35°
        withTransform({
            translate(planet2Center.x, planet2Center.y)
            rotate(35f - (deviceTilt.pitch * 0.2f))
            scale(scaleX = 1f, scaleY = 0.28f, pivot = Offset.Zero)
        }) {
            drawCircle(
                color = Color(0xFF7DD3FC).copy(alpha = 0.22f * planetaryAlpha),
                center = Offset.Zero,
                radius = planet2Radius * 1.7f,
                style = Stroke(
                    width = 1.6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 22f), ringRotation2)
                )
            )

            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.12f * planetaryAlpha),
                center = Offset.Zero,
                radius = planet2Radius * 2.15f,
                style = Stroke(width = 1f)
            )
        }

        // ═══════════════════════════════════════════════════════════════════════
        // 4. TERCEIRO DISCO PLANETÁRIO / LUA MENOR (Quadrante Médio Direito)
        // ═══════════════════════════════════════════════════════════════════════
        val planet3Center = Offset(w * 0.90f + tiltX * 0.8f, h * 0.44f + tiltY * 0.8f)
        val planet3Radius = w * 0.08f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFA78BFA).copy(alpha = 0.10f * planetaryAlpha),
                    Color(0xFF2E1065).copy(alpha = 0.04f * planetaryAlpha),
                    Color.Transparent
                ),
                center = planet3Center,
                radius = planet3Radius
            ),
            center = planet3Center,
            radius = planet3Radius
        )

        withTransform({
            translate(planet3Center.x, planet3Center.y)
            rotate(-18f + (deviceTilt.roll * 0.2f))
            scale(scaleX = 1f, scaleY = 0.25f, pivot = Offset.Zero)
        }) {
            drawCircle(
                color = Color(0xFFC4B5FD).copy(alpha = 0.18f * planetaryAlpha),
                center = Offset.Zero,
                radius = planet3Radius * 1.9f,
                style = Stroke(
                    width = 1.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 16f), ringRotation3)
                )
            )
        }

        // ═══════════════════════════════════════════════════════════════════════
        // 5. TRAJETÓRIAS ORBITAIS PANORÂMICAS CRUZADAS (Cruzando toda a tela)
        // ═══════════════════════════════════════════════════════════════════════
        // Arco 1: Diagonal Noroeste -> Sudeste
        val sweepingCenter1 = Offset(w * 0.50f, h * 0.54f)
        val sweepingRadius1 = w * 0.88f

        withTransform({
            translate(sweepingCenter1.x, sweepingCenter1.y)
            rotate(-15f + (tiltX * 0.1f))
            scale(scaleX = 1f, scaleY = 0.65f, pivot = Offset.Zero)
        }) {
            drawCircle(
                color = Color(0xFFFF6B35).copy(alpha = 0.10f * planetaryAlpha),
                center = Offset.Zero,
                radius = sweepingRadius1,
                style = Stroke(
                    width = 1.3f,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(14f, 30f),
                        ringRotation1 * 0.6f
                    )
                )
            )

            // Ponto de táquion correndo pelo arco
            val beaconAngle1 = Math.toRadians((ringRotation1 * 0.8).toDouble())
            val beacon1X = sweepingRadius1 * cos(beaconAngle1).toFloat()
            val beacon1Y = sweepingRadius1 * sin(beaconAngle1).toFloat()
            drawCircle(
                color = Color(0xFFFFB28D).copy(alpha = 0.40f * planetaryAlpha),
                center = Offset(beacon1X, beacon1Y),
                radius = 2.8f
            )
        }

        // Arco 2: Diagonal Sudoeste -> Nordeste (Contra-órbita)
        val sweepingCenter2 = Offset(w * 0.48f, h * 0.48f)
        val sweepingRadius2 = w * 0.78f

        withTransform({
            translate(sweepingCenter2.x, sweepingCenter2.y)
            rotate(28f - (tiltY * 0.1f))
            scale(scaleX = 1f, scaleY = 0.55f, pivot = Offset.Zero)
        }) {
            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.08f * planetaryAlpha),
                center = Offset.Zero,
                radius = sweepingRadius2,
                style = Stroke(
                    width = 1.0f,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(10f, 26f),
                        -ringRotation2 * 0.5f
                    )
                )
            )

            val beaconAngle2 = Math.toRadians((-ringRotation2 * 0.7).toDouble())
            val beacon2X = sweepingRadius2 * cos(beaconAngle2).toFloat()
            val beacon2Y = sweepingRadius2 * sin(beaconAngle2).toFloat()
            drawCircle(
                color = Color(0xFFBAE6FD).copy(alpha = 0.35f * planetaryAlpha),
                center = Offset(beacon2X, beacon2Y),
                radius = 2.5f
            )
        }
    }
}
