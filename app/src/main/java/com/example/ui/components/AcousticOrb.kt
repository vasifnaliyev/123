package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class AcousticOrbMode {
    IDLE,
    SPEAKING,
    LISTENING,
    THINKING
}

@Composable
fun AcousticOrb(
    mode: AcousticOrbMode,
    audioLevel: Float,
    modifier: Modifier = Modifier,
    orbSize: Dp = 176.dp
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryTint = when (mode) {
        AcousticOrbMode.SPEAKING -> colorScheme.primary
        AcousticOrbMode.LISTENING -> colorScheme.secondary
        AcousticOrbMode.THINKING -> colorScheme.tertiary
        AcousticOrbMode.IDLE -> colorScheme.primary.copy(alpha = 0.72f)
    }
    val secondaryTint = when (mode) {
        AcousticOrbMode.SPEAKING -> colorScheme.tertiary
        AcousticOrbMode.LISTENING -> colorScheme.primary
        AcousticOrbMode.THINKING -> colorScheme.primary
        AcousticOrbMode.IDLE -> colorScheme.secondary.copy(alpha = 0.55f)
    }

    val transition = rememberInfiniteTransition(label = "acoustic_orb_transition")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (mode) {
                    AcousticOrbMode.SPEAKING -> 1400
                    AcousticOrbMode.LISTENING -> 1200
                    AcousticOrbMode.THINKING -> 1800
                    AcousticOrbMode.IDLE -> 3400
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_phase"
    )

    val breath by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_breath"
    )

    Box(
        modifier = modifier.size(orbSize),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f
            val dynamicBoost = when (mode) {
                AcousticOrbMode.LISTENING -> 0.14f * audioLevel
                AcousticOrbMode.SPEAKING -> 0.10f * ((sin(phase * 2f) + 1f) / 2f)
                AcousticOrbMode.THINKING -> 0.06f
                AcousticOrbMode.IDLE -> 0.02f
            }

            // Outer atmospheric glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryTint.copy(alpha = 0.30f),
                        secondaryTint.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius
                ),
                radius = maxRadius,
                center = center
            )

            // Concentric acoustic ripple rings
            val ringCount = 3
            for (index in 0 until ringCount) {
                val ringFactor = 0.56f + index * 0.13f + dynamicBoost
                val waveOffset = sin(phase + index * 1.1f) * 0.035f
                val radius = maxRadius * (ringFactor + waveOffset).coerceAtMost(0.95f)
                val alpha = (0.45f - index * 0.12f).coerceAtLeast(0.08f)
                drawCircle(
                    color = primaryTint.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = (2.2f - index * 0.4f).dp.toPx())
                )
            }

            // Radial equalizer bars when speaking or listening
            if (mode == AcousticOrbMode.SPEAKING || mode == AcousticOrbMode.LISTENING) {
                val barCount = 28
                val innerR = maxRadius * 0.48f
                for (i in 0 until barCount) {
                    val angle = (2.0 * PI * i / barCount) + phase * 0.35
                    val harmonic = (sin(angle * 3.0 + phase * 2.0).toFloat() + 1f) / 2f
                    val amp = if (mode == AcousticOrbMode.LISTENING) {
                        (0.18f + 0.82f * audioLevel) * harmonic
                    } else {
                        0.32f + 0.68f * harmonic
                    }
                    val outerR = innerR + maxRadius * (0.08f + 0.24f * amp)
                    val start = Offset(
                        x = center.x + (cos(angle) * innerR).toFloat(),
                        y = center.y + (sin(angle) * innerR).toFloat()
                    )
                    val end = Offset(
                        x = center.x + (cos(angle) * outerR).toFloat(),
                        y = center.y + (sin(angle) * outerR).toFloat()
                    )
                    drawLine(
                        color = if (i % 2 == 0) primaryTint else secondaryTint,
                        start = start,
                        end = end,
                        strokeWidth = 2.6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Core glowing medallion
            val coreRadius = maxRadius * 0.40f * breath
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(primaryTint, secondaryTint),
                    start = Offset(center.x - coreRadius, center.y - coreRadius),
                    end = Offset(center.x + coreRadius, center.y + coreRadius)
                ),
                radius = coreRadius,
                center = center
            )
        }

        val centerIcon = when (mode) {
            AcousticOrbMode.SPEAKING -> Icons.Rounded.GraphicEq
            AcousticOrbMode.LISTENING -> Icons.Rounded.Mic
            AcousticOrbMode.THINKING -> Icons.Rounded.AutoAwesome
            AcousticOrbMode.IDLE -> Icons.Rounded.PhoneInTalk
        }
        Icon(
            imageVector = centerIcon,
            contentDescription = null,
            tint = colorScheme.onPrimary,
            modifier = Modifier.size(38.dp)
        )
    }
}
