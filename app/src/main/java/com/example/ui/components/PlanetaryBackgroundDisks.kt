package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * Discos planetários sutis com transparência e física de inclinação por toda a tela.
 * Renderizados em segundo plano (atrás dos botões de controle e cards) apenas quando o cronômetro
 * está habilitado/ativo, com transição suave de opacidade.
 */
@Composable
fun PlanetaryBackgroundDisks(
    isEnabled: Boolean,
    deviceTilt: DeviceTilt,
    modifier: Modifier = Modifier
) {
    // Opacidade suave animada: visível com transparência elegante quando habilitado
    val planetaryAlpha by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.08f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "planetaryDisksAlpha"
    )

    // Rotação suave contínua dos sistemas de anéis
    val infiniteTransition = rememberInfiniteTransition(label = "planetaryRevolutions")

    val ringRotation1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 75000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation1"
    )

    val ringRotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 95000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation2"
    )

    // Pulso sutil de respiração do disco estelar
    val diskBreath by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "diskBreath"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        if (planetaryAlpha <= 0.01f) return@Canvas

        val w = size.width
        val h = size.height

        // Reação giroscópica sutil limitada
        val tiltX = deviceTilt.roll.coerceIn(-20f, 20f) * 1.8f
        val tiltY = deviceTilt.pitch.coerceIn(-20f, 20f) * 1.8f

        // ═══════════════════════════════════════════════════════════════════════
        // 1. DISCO PLANETÁRIO INFERIOR PRINCIPAL (Atrás dos Botões de Controle)
        // ═══════════════════════════════════════════════════════════════════════
        // Posicionado estrategicamente na parte inferior direita cobrindo a área atrás dos botões
        val planet1Center = Offset(w * 0.78f + tiltX, h * 0.88f + tiltY)
        val planet1Radius = w * 0.26f * diskBreath

        // Gradiente atmosférico do planeta gasoso (tons bronze/âmbar profundos com alta transparência)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF6B35).copy(alpha = 0.14f * planetaryAlpha),
                    Color(0xFF8D3B1B).copy(alpha = 0.08f * planetaryAlpha),
                    Color(0xFF26140E).copy(alpha = 0.04f * planetaryAlpha),
                    Color.Transparent
                ),
                center = planet1Center,
                radius = planet1Radius
            ),
            center = planet1Center,
            radius = planet1Radius
        )

        // Núcleo denso suave
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFF8555).copy(alpha = 0.12f * planetaryAlpha),
                    Color.Transparent
                ),
                center = planet1Center - Offset(planet1Radius * 0.2f, planet1Radius * 0.2f),
                radius = planet1Radius * 0.6f
            ),
            center = planet1Center - Offset(planet1Radius * 0.2f, planet1Radius * 0.2f),
            radius = planet1Radius * 0.6f
        )

        // Sistema de Anéis Planetários tipo Saturno (Inclinado em ~-26° no espaço 3D)
        val ring1Tilt = -26f + (deviceTilt.roll * 0.25f)
        withTransform({
            translate(planet1Center.x, planet1Center.y)
            rotate(ring1Tilt)
            scale(scaleX = 1f, scaleY = 0.32f, pivot = Offset.Zero)
        }) {
            // Anel A: Faixa interna brilhante translúcida
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFFFF8A50).copy(alpha = 0.16f * planetaryAlpha),
                        Color(0xFFFFA97A).copy(alpha = 0.22f * planetaryAlpha),
                        Color.Transparent
                    ),
                    center = Offset.Zero,
                    radius = planet1Radius * 1.6f
                ),
                center = Offset.Zero,
                radius = planet1Radius * 1.55f,
                style = Stroke(width = 18f)
            )

            // Divisão de Cassini & Anel B fino tracejado com rotação orbital
            drawCircle(
                color = Color(0xFFFFB288).copy(alpha = 0.25f * planetaryAlpha),
                center = Offset.Zero,
                radius = planet1Radius * 1.82f,
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(35f, 18f, 10f, 18f),
                        ringRotation1 * 1.5f
                    )
                )
            )

            // Anel C: Disco externo ultra sutil com poeira orbital ciano/âmbar
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFF6B35).copy(alpha = 0.18f * planetaryAlpha),
                        Color(0xFF38BDF8).copy(alpha = 0.14f * planetaryAlpha),
                        Color(0xFFFF6B35).copy(alpha = 0.05f * planetaryAlpha),
                        Color(0xFF38BDF8).copy(alpha = 0.18f * planetaryAlpha)
                    ),
                    center = Offset.Zero
                ),
                center = Offset.Zero,
                radius = planet1Radius * 2.25f,
                style = Stroke(
                    width = 1.8f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 25f), -ringRotation2)
                )
            )

            // Lua/Satélite orbital orbitando ao redor do anel
            val moon1AngleRad = Math.toRadians((ringRotation1 * 1.2).toDouble())
            val moon1Dist = planet1Radius * 2.25f
            val moon1X = moon1Dist * cos(moon1AngleRad).toFloat()
            val moon1Y = moon1Dist * sin(moon1AngleRad).toFloat()

            // Brilho da pequena lua orbital
            drawCircle(
                color = Color(0xFFFF9E73).copy(alpha = 0.5f * planetaryAlpha),
                center = Offset(moon1X, moon1Y),
                radius = 3f
            )
            drawCircle(
                color = Color(0xFFFF6B35).copy(alpha = 0.2f * planetaryAlpha),
                center = Offset(moon1X, moon1Y),
                radius = 7f
            )
        }

        // ═══════════════════════════════════════════════════════════════════════
        // 2. DISCO PLANETÁRIO SUPERIOR / ESQUERDO (Atrás do Relógio e Título)
        // ═══════════════════════════════════════════════════════════════════════
        val planet2Center = Offset(w * 0.15f - tiltX * 0.7f, h * 0.24f - tiltY * 0.7f)
        val planet2Radius = w * 0.14f

        // Corpo celeste etéreo ciano/celeste
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF38BDF8).copy(alpha = 0.10f * planetaryAlpha),
                    Color(0xFF0F2B48).copy(alpha = 0.05f * planetaryAlpha),
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
                color = Color(0xFF7DD3FC).copy(alpha = 0.18f * planetaryAlpha),
                center = Offset.Zero,
                radius = planet2Radius * 1.7f,
                style = Stroke(
                    width = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 20f), ringRotation2)
                )
            )

            drawCircle(
                color = Color(0xFF38BDF8).copy(alpha = 0.10f * planetaryAlpha),
                center = Offset.Zero,
                radius = planet2Radius * 2.1f,
                style = Stroke(width = 1f)
            )
        }

        // ═══════════════════════════════════════════════════════════════════════
        // 3. TRAJETÓRIA ORBITAL PANORÂMICA (Conectando a tela diagonalmente)
        // ═══════════════════════════════════════════════════════════════════════
        val sweepingCenter = Offset(w * 0.5f, h * 0.56f)
        val sweepingRadius = w * 0.85f

        withTransform({
            translate(sweepingCenter.x, sweepingCenter.y)
            rotate(-14f + (tiltX * 0.1f))
            scale(scaleX = 1f, scaleY = 0.68f, pivot = Offset.Zero)
        }) {
            // Grande elipse cósmica pontilhada
            drawCircle(
                color = Color(0xFFFF6B35).copy(alpha = 0.09f * planetaryAlpha),
                center = Offset.Zero,
                radius = sweepingRadius,
                style = Stroke(
                    width = 1.2f,
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(12f, 28f),
                        ringRotation1 * 0.6f
                    )
                )
            )

            // Ponto orbital de táquion correndo pelo arco
            val beaconAngleRad = Math.toRadians((ringRotation1 * 0.8).toDouble())
            val beaconX = sweepingRadius * cos(beaconAngleRad).toFloat()
            val beaconY = sweepingRadius * sin(beaconAngleRad).toFloat()

            drawCircle(
                color = Color(0xFFFFA97A).copy(alpha = 0.35f * planetaryAlpha),
                center = Offset(beaconX, beaconY),
                radius = 2.5f
            )
        }
    }
}
