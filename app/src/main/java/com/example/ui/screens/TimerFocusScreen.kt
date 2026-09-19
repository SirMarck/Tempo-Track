package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.utils.DeviceTilt
import com.example.utils.FormatUtils
import com.example.utils.rememberDeviceTilt
import com.example.viewmodel.TimeTrackerViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tela 3 - Timer Ativo Foco 3D
 * Replicação Fiel ao Pôster Oficial (media_1789841983545.png)
 * Disco toroidal isométrico com facetas de cristal, arcos de plasma neon laranja,
 * cartões de métricas duplos, botão de bronze e controles de alta fidelidade.
 */
@Composable
fun TimerFocusScreen(
    viewModel: TimeTrackerViewModel,
    onNavigateBack: () -> Unit
) {
    val activeSession by viewModel.activeSession.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val activities by viewModel.activities.collectAsState()

    if (activeSession == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(TempoBgBase),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(TempoSpacing.space4)
            ) {
                Text(
                    text = "Nenhum timer em andamento",
                    style = MaterialTheme.typography.titleMedium,
                    color = TempoTextMuted
                )
                TempoSecondaryAction(text = "Voltar para o Início", onClick = onNavigateBack)
            }
        }
        return
    }

    val session = activeSession!!
    val client = remember(session.clientId, clients) {
        clients.find { it.id == session.clientId }
    }
    val project = remember(session.projectId, projects) {
        projects.find { it.id == session.projectId }
    }
    val activity = remember(session.activityId, activities) {
        activities.find { it.id == session.activityId }
    }

    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(session.isPaused, session.id) {
        if (!session.isPaused) {
            while (true) {
                delay(1000)
                currentTime = System.currentTimeMillis()
            }
        }
    }

    val durationMillis = session.calculateDurationMillis(currentTime)
    val durationFormatted = FormatUtils.formatDuration(durationMillis)

    val effectiveRate = remember(session, project, client) {
        if (session.appliedRate > 0.0) session.appliedRate
        else FormatUtils.resolveEffectiveRate(project, client, 170.0)
    }
    val currentEarnings = if (session.billable) {
        (durationMillis.toDouble() / (1000.0 * 3600.0)) * effectiveRate
    } else 0.0

    val sessionStartTimeFormatted = remember(session.startTime) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(session.startTime))
    }

    // Sensor de movimento suave
    val deviceTilt by rememberDeviceTilt()
    val targetPitch = if (!session.isPaused) 54f + deviceTilt.pitch else 40f
    val targetRoll = if (!session.isPaused) -38f + deviceTilt.roll else -20f

    val animatedPitch by animateFloatAsState(
        targetValue = targetPitch,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "animatedPitch"
    )
    val animatedRoll by animateFloatAsState(
        targetValue = targetRoll,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "animatedRoll"
    )

    // Respiração suave do arco neon
    val infiniteTransition = rememberInfiniteTransition(label = "plasmaGlow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF13171D),
                        Color(0xFF0A0D10),
                        Color(0xFF06080A)
                    ),
                    center = Offset(500f, 400f),
                    radius = 900f
                )
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // ─── Top Header: Back Arrow, Centered Status, More Icon ─────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161B22))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = TempoTextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Centered Status Pill with glowing orange dot
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF171B21))
                    .tempoMaterialHighlight(CircleShape)
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF6B35))
                )
                Text(
                    text = if (session.isPaused) "Pausado" else "Em andamento",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF6B35)
                )
            }

            IconButton(
                onClick = { /* Menu opções */ },
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF161B22))
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Mais opções",
                    tint = TempoTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ─── Centro: Título, Relógio Isométrico 3D e Métricas ───────────────
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Título do Cliente e Projeto (matching media_1789841983545.png)
            Text(
                text = client?.name ?: "Domsul",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                ),
                color = TempoTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = project?.name ?: "API de autenticação",
                style = MaterialTheme.typography.bodyMedium,
                color = TempoTextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Adjust,
                    contentDescription = null,
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = activity?.name ?: "Desenvolvimento",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFFF6B35)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ─── Estágio 3D do Relógio Toroidal com Inclinação Isométrica ───
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .graphicsLayer {
                        rotationX = animatedPitch
                        rotationZ = animatedRoll
                        rotationY = 8f
                        cameraDistance = 16f * density.density
                    },
                contentAlignment = Alignment.Center
            ) {
                // Canvas 3D: Anel Toroidal de Cristal Facetado + Plasma Neon Laranja
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val center = Offset(canvasWidth / 2f, canvasHeight / 2f)
                    val radius = canvasWidth / 2f - 12f

                    // 1. Corpo base do anel grafite translúcido
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E2630),
                                Color(0xFF12171E),
                                Color(0xFF090C0F)
                            ),
                            center = center,
                            radius = radius
                        ),
                        radius = radius,
                        center = center
                    )

                    // 2. Facetas de cristal (16 blocos chanfrados com glints brancos)
                    val numSegments = 16
                    for (i in 0 until numSegments) {
                        val segAngle = (i.toFloat() / numSegments) * 360f
                        val isEven = i % 2 == 0
                        drawArc(
                            color = if (isEven) Color.White.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.08f),
                            startAngle = segAngle,
                            sweepAngle = 16f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = 16f)
                        )
                    }

                    // Borda externa de vidro chanfrado com glint especular
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.9f),
                                Color.White.copy(alpha = 0.1f),
                                Color.White.copy(alpha = 0.8f)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(canvasWidth, canvasHeight)
                        ),
                        radius = radius + 8f,
                        center = center,
                        style = Stroke(width = 2.5f)
                    )

                    // 3. Arcos de Plasma Neon Laranja Intenso (matching media_1789841983545.png)
                    val plasmaColor = Color(0xFFFF6B35)
                    val plasmaGlow = Color(0xFFFF9E58)

                    // Arco Principal (lado direito superior-inferior: ~140 graus)
                    drawArc(
                        color = plasmaGlow.copy(alpha = 0.35f * glowPulse),
                        startAngle = -70f,
                        sweepAngle = 145f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius + 10f, center.y - radius + 10f),
                        size = Size((radius - 10f) * 2, (radius - 10f) * 2),
                        style = Stroke(width = 14f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = plasmaColor,
                        startAngle = -70f,
                        sweepAngle = 145f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius + 10f, center.y - radius + 10f),
                        size = Size((radius - 10f) * 2, (radius - 10f) * 2),
                        style = Stroke(width = 7.5f, cap = StrokeCap.Round)
                    )

                    // Arco Secundário (topo esquerdo: ~35 graus)
                    drawArc(
                        color = plasmaGlow.copy(alpha = 0.3f * glowPulse),
                        startAngle = 145f,
                        sweepAngle = 35f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius + 10f, center.y - radius + 10f),
                        size = Size((radius - 10f) * 2, (radius - 10f) * 2),
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color(0xFFFFA24C),
                        startAngle = 145f,
                        sweepAngle = 35f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius + 10f, center.y - radius + 10f),
                        size = Size((radius - 10f) * 2, (radius - 10f) * 2),
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )

                    // 4. Trilho interno com ticks discretos
                    val tickRadius = radius - 26f
                    for (i in 0 until 60) {
                        val angleDeg = i * 6f
                        val angleRad = Math.toRadians(angleDeg.toDouble() - 90.0)
                        val isHourTick = i % 5 == 0
                        val tickLen = if (isHourTick) 7f else 4f
                        val startX = center.x + ((tickRadius - tickLen) * cos(angleRad)).toFloat()
                        val startY = center.y + ((tickRadius - tickLen) * sin(angleRad)).toFloat()
                        val endX = center.x + (tickRadius * cos(angleRad)).toFloat()
                        val endY = center.y + (tickRadius * sin(angleRad)).toFloat()

                        drawLine(
                            color = if (isHourTick) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.12f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (isHourTick) 2f else 1f,
                            cap = StrokeCap.Round
                        )
                    }

                    // 5. Disco central rebaixado (obsidian sunken plate)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF0E1217),
                                Color(0xFF07090C)
                            ),
                            center = center,
                            radius = radius - 30f
                        ),
                        radius = radius - 30f,
                        center = center
                    )
                }

                // Conteúdo Central: Dígitos Monospace Legíveis com Contrarrotação
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer {
                        rotationZ = -animatedRoll
                        rotationX = -animatedPitch
                    }
                ) {
                    Text(
                        text = durationFormatted,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = TempoMono,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.8).sp
                        ),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = FormatUtils.formatCurrency(currentEarnings),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = TempoMono,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFFFF8A50)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Dual Metric Cards Below Ring (Valor/hora & Início) ──────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Card 1: Valor/hora
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF171B21))
                        .tempoMaterialHighlight(RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column {
                        Text(
                            text = "Valor/hora",
                            fontSize = 11.sp,
                            color = TempoTextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = FormatUtils.formatCurrency(effectiveRate),
                            fontSize = 15.sp,
                            fontFamily = TempoMono,
                            fontWeight = FontWeight.Bold,
                            color = TempoTextPrimary
                        )
                    }
                }

                // Card 2: Início
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF171B21))
                        .tempoMaterialHighlight(RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Início",
                            fontSize = 11.sp,
                            color = TempoTextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = sessionStartTimeFormatted,
                            fontSize = 15.sp,
                            fontFamily = TempoMono,
                            fontWeight = FontWeight.Bold,
                            color = TempoTextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ─── Dots Indicator (• • •) ─────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(TempoTextPrimary)
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                )
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.3f))
                )
            }
        }

        // ─── Bottom Controls Bar: Pausar (Moon), Bronze Button, Finalizar ───
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Botão Sleep/Pausar à esquerda
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier
                    .clickable {
                        if (session.isPaused) viewModel.resumeActiveSession()
                        else viewModel.pauseActiveSession()
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF191D24))
                        .tempoMaterialHighlight(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bedtime,
                        contentDescription = "Pausar",
                        tint = TempoTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = "Pausar",
                    fontSize = 10.sp,
                    color = TempoTextMuted,
                    fontWeight = FontWeight.Medium
                )
            }

            // 2. Botão Circular Metálico de Bronze / Cobre (Pausa Principal)
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF42291E),
                                Color(0xFF22150E),
                                Color(0xFF140D09)
                            ),
                            center = Offset(22f, 22f),
                            radius = 45f
                        )
                    )
                    .tempoMaterialHighlight(CircleShape)
                    .clickable {
                        if (session.isPaused) viewModel.resumeActiveSession()
                        else viewModel.pauseActiveSession()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (session.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (session.isPaused) "Retomar" else "Pausar",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 3. Botão Laranja Vibrante "Finalizar"
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFF6B35),
                                Color(0xFFE45A25)
                            )
                        )
                    )
                    .clickable {
                        viewModel.stopActiveSession()
                        onNavigateBack()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Finalizar",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

