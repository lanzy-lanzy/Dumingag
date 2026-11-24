package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.SplashGlassMintAccent
import com.example.ui.theme.SplashGlassPanelBottom
import com.example.ui.theme.SplashGlassPanelTop
import com.example.ui.theme.SplashGlassPillActiveTop
import com.example.ui.theme.SplashGlassPillIdle
import com.example.ui.theme.SplashGlassSpecularEdge
import com.example.ui.theme.SplashGlassSpecularMid
import com.example.ui.theme.SplashGlassSpecularTop
import com.example.ui.theme.SplashGlassTealAccent
import com.example.ui.theme.SplashGlassTextPrimary
import com.example.ui.theme.SplashGlassTextSecondary
import com.example.ui.theme.SplashGlassTrackBg
import com.example.ui.theme.SplashGlassWarmAccent
import com.example.ui.theme.SplashScrimBottom
import com.example.ui.theme.SplashScrimCenter
import com.example.ui.theme.SplashScrimTop
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Preliminary iOS-style Liquid Glass boot intro displayed briefly before the
 * interactive scenic splash screen. Automatically transitions once its fluid
 * entrance completes, or immediately if the user taps anywhere.
 */
@Composable
fun PreliminarySplashScreen(
    onFinished: () -> Unit,
    durationMillis: Int = 2250,
    modifier: Modifier = Modifier
) {
    var hasCompleted by remember { mutableStateOf(false) }
    val finishOnce = {
        if (!hasCompleted) {
            hasCompleted = true
            onFinished()
        }
    }

    val emblemScale = remember { Animatable(0.76f) }
    val contentAlpha = remember { Animatable(0.18f) }
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            emblemScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing)
        )
        finishOnce()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "preliminary_liquid_glass")
    val outerOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_orbit"
    )
    val innerOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_orbit"
    )
    val pulseWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquid_pulse_wave"
    )
    val auroraPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora_phase"
    )

    val progress = progressAnim.value
    val statusCopy = when {
        progress < 0.38f -> "Awakening Dumingag Highlands…"
        progress < 0.76f -> "Loading offline trails & glass map…"
        else -> "Welcome to Zamboanga del Sur"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashScrimBottom)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = finishOnce
            )
            .testTag("preliminary_splash_root")
    ) {
        // Blurred highland atmosphere so deep emerald & gold tones refract through the glass
        Image(
            painter = painterResource(id = R.drawable.img_dumingag_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(1.06f + progress * 0.06f)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(radius = 24.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    } else {
                        Modifier
                    }
                )
        )

        // Luminous caustic aurora + concentric liquid refraction waves
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        SplashScrimTop,
                        SplashScrimCenter,
                        SplashScrimBottom
                    )
                )
            )

            val orbA = Offset(
                x = size.width * (0.24f + 0.09f * cos(auroraPhase)),
                y = size.height * (0.28f + 0.05f * sin(auroraPhase))
            )
            val orbB = Offset(
                x = size.width * (0.78f + 0.08f * sin(auroraPhase * 0.9f)),
                y = size.height * (0.62f + 0.06f * cos(auroraPhase))
            )
            val orbC = Offset(
                x = size.width * (0.50f + 0.06f * cos(auroraPhase * 1.2f)),
                y = size.height * (0.46f + 0.04f * sin(auroraPhase * 1.1f))
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SplashGlassMintAccent.copy(alpha = 0.36f), Color.Transparent),
                    center = orbA,
                    radius = size.width * 0.70f
                ),
                center = orbA,
                radius = size.width * 0.70f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SplashGlassTealAccent.copy(alpha = 0.28f), Color.Transparent),
                    center = orbB,
                    radius = size.width * 0.64f
                ),
                center = orbB,
                radius = size.width * 0.64f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SplashGlassWarmAccent.copy(alpha = 0.20f), Color.Transparent),
                    center = orbC,
                    radius = size.width * 0.52f
                ),
                center = orbC,
                radius = size.width * 0.52f
            )

            // Dual expanding liquid-glass refraction ripples around screen center
            val center = Offset(size.width / 2f, size.height * 0.44f)
            val wave1Radius = size.minDimension * (0.20f + pulseWave * 0.36f)
            val wave1Alpha = (1f - pulseWave) * 0.38f
            drawCircle(
                color = SplashGlassSpecularTop.copy(alpha = wave1Alpha),
                radius = wave1Radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            val wave2Phase = (pulseWave + 0.5f) % 1f
            val wave2Radius = size.minDimension * (0.20f + wave2Phase * 0.36f)
            val wave2Alpha = (1f - wave2Phase) * 0.26f
            drawCircle(
                color = SplashGlassMintAccent.copy(alpha = wave2Alpha),
                radius = wave2Radius,
                center = center,
                style = Stroke(width = 1.4.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .padding(horizontal = 28.dp, vertical = 20.dp)
                .alpha(contentAlpha.value),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top subtle frosted municipal glass badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SplashGlassPillIdle, SplashGlassPanelBottom)
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(SplashGlassSpecularTop.copy(alpha = 0.72f), SplashGlassSpecularEdge)
                        ),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(SplashGlassMintAccent)
                    )
                    Text(
                        text = "MUNICIPALITY OF DUMINGAG",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        color = SplashGlassTextPrimary
                    )
                }
            }

            // Centerpiece Blooming Refractive Glass Crest & Brand Wordmark
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 420.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(164.dp)
                        .scale(emblemScale.value)
                        .testTag("preliminary_splash_crest")
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Outer frosted refractive lens
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    SplashGlassPillActiveTop,
                                    SplashGlassPanelTop,
                                    SplashGlassPanelBottom
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            )
                        )
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    SplashGlassSpecularTop,
                                    SplashGlassSpecularMid,
                                    SplashGlassSpecularEdge
                                ),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height)
                            ),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Outer orbiting specular liquid arc
                        val padOuter = 5.dp.toPx()
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    SplashGlassSpecularTop,
                                    SplashGlassMintAccent,
                                    Color.Transparent
                                )
                            ),
                            startAngle = outerOrbitAngle,
                            sweepAngle = 110f,
                            useCenter = false,
                            topLeft = Offset(padOuter, padOuter),
                            size = Size(size.width - padOuter * 2, size.height - padOuter * 2),
                            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // Counter-rotating inner teal sheen arc
                        val padInner = 13.dp.toPx()
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    SplashGlassTealAccent.copy(alpha = 0.85f),
                                    SplashGlassSpecularTop.copy(alpha = 0.65f),
                                    Color.Transparent
                                )
                            ),
                            startAngle = innerOrbitAngle,
                            sweepAngle = 80f,
                            useCenter = false,
                            topLeft = Offset(padInner, padInner),
                            size = Size(size.width - padInner * 2, size.height - padInner * 2),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Official Dumingag Crest inside inner luminous lens
                    Box(
                        modifier = Modifier
                            .size(118.dp)
                            .shadow(16.dp, CircleShape, clip = false)
                            .clip(CircleShape)
                            .border(
                                width = 2.2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(SplashGlassSpecularTop, SplashGlassSpecularEdge)
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_dumingag_foreground),
                            contentDescription = "Dumingag Crest",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Dumingag",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.8).sp
                    ),
                    color = SplashGlassTextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("preliminary_splash_title")
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Organic Capital · Zamboanga del Sur",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.4.sp,
                    color = SplashGlassTextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom Liquid-Glass Progress Capsule
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp)
                    .padding(bottom = 12.dp)
            ) {
                AnimatedContent(
                    targetState = statusCopy,
                    transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
                    label = "preliminary_status_copy"
                ) { copy ->
                    Text(
                        text = copy,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = SplashGlassTextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(SplashGlassTrackBg)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(SplashGlassSpecularTop.copy(alpha = 0.65f), SplashGlassSpecularEdge)
                            ),
                            shape = RoundedCornerShape(50)
                        )
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction = progress.coerceIn(0.08f, 1f))
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        SplashGlassMintAccent,
                                        SplashGlassSpecularTop,
                                        SplashGlassTealAccent
                                    )
                                )
                            )
                    )
                }

                Text(
                    text = "Tap anywhere to continue",
                    style = MaterialTheme.typography.labelSmall,
                    color = SplashGlassMintAccent.copy(alpha = 0.85f)
                )
            }
        }
    }
}
