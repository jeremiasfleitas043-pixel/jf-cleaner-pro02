package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberMint
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TealEnergy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.FormatUtils
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class SphereState {
    IDLE,
    SCANNING,
    CLEANING,
    CLEANED
}

private data class EnergyParticle(
    val baseTheta: Float,
    val basePhi: Float,
    val radiusScale: Float,
    val speedFactor: Float,
    val sizeDp: Float,
    val phaseOffset: Float
)

@Composable
fun EnergySphereHero(
    displayBytes: Long,
    isCalculated: Boolean,
    sphereState: SphereState,
    onClick: (() -> Unit)? = null,
    size: Dp = 264.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sphere_energy")

    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 9000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "sphere_phase"
    )

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sphere_pulse"
    )

    val energyIntensity by animateFloatAsState(
        targetValue = when (sphereState) {
            SphereState.IDLE -> 1.0f
            SphereState.SCANNING -> 1.55f
            SphereState.CLEANING -> 1.85f
            SphereState.CLEANED -> 0.85f
        },
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 200f),
        label = "energy_intensity"
    )

    val particles = remember {
        List(44) { idx ->
            val f = idx.toFloat()
            EnergyParticle(
                baseTheta = ((f * 137.508f) % 360f) * (PI.toFloat() / 180f),
                basePhi = ((f * 73.13f) % 180f) * (PI.toFloat() / 180f),
                radiusScale = 0.86f + ((idx % 7) * 0.055f),
                speedFactor = 0.6f + ((idx % 5) * 0.25f),
                sizeDp = 1.3f + ((idx % 4) * 0.65f),
                phaseOffset = f * 0.42f
            )
        }
    }

    val ringPath = remember { Path() }
    val sizeParts = remember(displayBytes) { FormatUtils.splitSizeParts(displayBytes) }

    val clickableModifier = if (onClick != null) {
        Modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .size(size)
            .testTag("energy_sphere_hero")
            .semantics {
                contentDescription = if (isCalculated) {
                    "Optimizable space ${sizeParts.value} ${sizeParts.unit}"
                } else {
                    "Energy sphere scanner"
                }
            }
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxRadius = (this.size.minDimension / 2f) * 0.74f * pulse

            // Ambient radial volumetric green aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NeonEmerald.copy(alpha = 0.22f * energyIntensity.coerceAtMost(1.4f)),
                        EmeraldPrimary.copy(alpha = 0.12f),
                        EmeraldDeep.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = this.size.minDimension * 0.50f
                ),
                center = center,
                radius = this.size.minDimension * 0.50f
            )

            // Inner dark core contrast so center text stays razor sharp
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF03140E).copy(alpha = 0.72f),
                        Color(0xFF020C08).copy(alpha = 0.45f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 0.85f
                ),
                center = center,
                radius = maxRadius * 0.85f
            )

            val activePhase = phase * (if (sphereState == SphereState.CLEANING || sphereState == SphereState.SCANNING) 2f else 1f)
            val waveAmp = 0.14f * energyIntensity
            val rotY = activePhase * 0.5f
            val rotX = 0.42f + 0.16f * sin(activePhase)

            val cosRotY = cos(rotY)
            val sinRotY = sin(rotY)
            val cosRotX = cos(rotX)
            val sinRotX = sin(rotX)

            // 1. Latitude organic wave contour ribbons
            val ribbonCount = 24
            val segments = 56
            val strokePx = 1.05.dp.toPx()

            for (rIdx in 0 until ribbonCount) {
                val v = (rIdx + 0.5f) / ribbonCount.toFloat()
                val basePhi = v * PI.toFloat()
                val sinBasePhi = sin(basePhi)
                val cosBasePhi = cos(basePhi)

                ringPath.reset()
                var avgZ = 0f

                for (sIdx in 0..segments) {
                    val theta = (sIdx.toFloat() / segments.toFloat()) * (2f * PI.toFloat())
                    val harmonic =
                        0.58f * sin(3f * theta + activePhase + basePhi * 2.2f) *
                            cos(2f * basePhi - activePhase * 0.8f) +
                        0.42f * cos(4f * basePhi + activePhase * 1.4f - 2f * theta)

                    val r = maxRadius * (1f + waveAmp * harmonic)

                    val x0 = r * sinBasePhi * cos(theta)
                    val y0 = r * cosBasePhi
                    val z0 = r * sinBasePhi * sin(theta)

                    // Rotate around Y then X
                    val x1 = x0 * cosRotY + z0 * sinRotY
                    val z1 = -x0 * sinRotY + z0 * cosRotY
                    val y2 = y0 * cosRotX - z1 * sinRotX
                    val z2 = y0 * sinRotX + z1 * cosRotX

                    avgZ += z2
                    val sx = center.x + x1
                    val sy = center.y + y2

                    if (sIdx == 0) ringPath.moveTo(sx, sy) else ringPath.lineTo(sx, sy)
                }

                val normZ = ((avgZ / (segments + 1)) / maxRadius).coerceIn(-1f, 1f)
                val depthT = (normZ + 1f) * 0.5f
                val ribbonColor = lerp(
                    EmeraldDeep.copy(alpha = 0.20f),
                    if (rIdx % 3 == 0) CyberMint.copy(alpha = 0.72f) else NeonEmerald.copy(alpha = 0.56f),
                    depthT
                )

                drawPath(
                    path = ringPath,
                    color = ribbonColor,
                    style = Stroke(
                        width = strokePx * (0.75f + 0.5f * depthT),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            // 2. Longitudinal meridian swirl lines for 3D lattice depth
            val meridianCount = 12
            val meridianSegs = 40
            for (mIdx in 0 until meridianCount) {
                val baseTheta = (mIdx.toFloat() / meridianCount.toFloat()) * (2f * PI.toFloat())
                ringPath.reset()
                var avgZ = 0f

                for (sIdx in 0..meridianSegs) {
                    val phi = (sIdx.toFloat() / meridianSegs.toFloat()) * PI.toFloat()
                    val theta = baseTheta + 0.25f * sin(2f * phi + activePhase)
                    val harmonic =
                        0.55f * sin(3f * theta + activePhase + phi * 2f) *
                            cos(2f * phi - activePhase * 0.8f) +
                        0.45f * cos(4f * phi + activePhase * 1.4f - 2f * theta)

                    val r = maxRadius * (1f + waveAmp * harmonic)
                    val sinPhi = sin(phi)
                    val x0 = r * sinPhi * cos(theta)
                    val y0 = r * cos(phi)
                    val z0 = r * sinPhi * sin(theta)

                    val x1 = x0 * cosRotY + z0 * sinRotY
                    val z1 = -x0 * sinRotY + z0 * cosRotY
                    val y2 = y0 * cosRotX - z1 * sinRotX
                    val z2 = y0 * sinRotX + z1 * cosRotX

                    avgZ += z2
                    val sx = center.x + x1
                    val sy = center.y + y2
                    if (sIdx == 0) ringPath.moveTo(sx, sy) else ringPath.lineTo(sx, sy)
                }

                val normZ = ((avgZ / (meridianSegs + 1)) / maxRadius).coerceIn(-1f, 1f)
                val depthT = (normZ + 1f) * 0.5f
                drawPath(
                    path = ringPath,
                    color = lerp(
                        EmeraldDeep.copy(alpha = 0.12f),
                        TealEnergy.copy(alpha = 0.42f),
                        depthT
                    ),
                    style = Stroke(width = 0.85.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // 3. Luminous orbiting energy particles
            for (p in particles) {
                val theta = p.baseTheta + activePhase * p.speedFactor
                val phi = p.basePhi + 0.22f * sin(activePhase + p.phaseOffset)
                val r = maxRadius * p.radiusScale * (1f + 0.08f * sin(activePhase * 1.6f + p.phaseOffset))

                val sinPhi = sin(phi)
                val x0 = r * sinPhi * cos(theta)
                val y0 = r * cos(phi)
                val z0 = r * sinPhi * sin(theta)

                val x1 = x0 * cosRotY + z0 * sinRotY
                val z1 = -x0 * sinRotY + z0 * cosRotY
                val y2 = y0 * cosRotX - z1 * sinRotX
                val z2 = y0 * sinRotX + z1 * cosRotX

                val depthT = ((z2 / maxRadius) + 1f) * 0.5f
                val alpha = (0.18f + 0.78f * depthT.coerceIn(0f, 1f))
                val pos = Offset(center.x + x1, center.y + y2)
                val dotRadius = p.sizeDp.dp.toPx() * (0.7f + 0.5f * depthT.coerceIn(0f, 1f))

                // Soft glow halo
                drawCircle(
                    color = NeonEmerald.copy(alpha = alpha * 0.28f),
                    radius = dotRadius * 2.8f,
                    center = pos
                )
                // Bright core particle
                drawCircle(
                    color = lerp(NeonEmerald, Color.White, 0.45f).copy(alpha = alpha),
                    radius = dotRadius,
                    center = pos
                )
            }
        }

        // Center Typography Overlay
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when {
                sphereState == SphereState.SCANNING && !isCalculated -> {
                    Text(
                        text = "SCAN",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "ANALYZING...",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberMint,
                        letterSpacing = 1.5.sp
                    )
                }
                isCalculated -> {
                    Text(
                        text = sizeParts.value,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = sizeParts.unit,
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberMint,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp
                    )
                }
                else -> {
                    Text(
                        text = "--",
                        style = MaterialTheme.typography.displayLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "TAP TO SCAN",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary,
                        letterSpacing = 1.4.sp
                    )
                }
            }
        }
    }
}
