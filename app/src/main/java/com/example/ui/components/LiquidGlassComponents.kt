package com.example.ui.components

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.example.ui.theme.LocalIsDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.GlassDarkBorderMid
import com.example.ui.theme.GlassDarkBorderTail
import com.example.ui.theme.GlassDarkHighlight
import com.example.ui.theme.GlassLightBorderMid
import com.example.ui.theme.GlassLightBorderTail
import com.example.ui.theme.GlassLightHighlight
import com.example.ui.theme.SplashGlassMintAccent
import com.example.ui.theme.SplashGlassPanelBottom
import com.example.ui.theme.SplashGlassPanelTop
import com.example.ui.theme.SplashGlassPillActiveTop
import com.example.ui.theme.SplashGlassSpecularEdge
import com.example.ui.theme.SplashGlassSpecularMid
import com.example.ui.theme.SplashGlassSpecularTop
import com.example.ui.theme.SplashGlassTealAccent
import com.example.ui.theme.SplashGlassTextPrimary
import com.example.ui.theme.SplashGlassTextSecondary
import com.example.ui.theme.SplashGlassWarmAccent

/**
 * Full-bleed atmospheric backdrop rendered behind the main app scaffold so
 * frosted iOS-style Liquid Glass cards, floating tab bars, and top headers
 * naturally refract subtle emerald, teal, and warm highland depth while
 * preserving crisp readability in both light and dark modes.
 */
@Composable
fun LiquidGlassAmbientBackdrop(
    modifier: Modifier = Modifier
) {
    val darkMode = LocalIsDarkTheme.current
    val baseColor = MaterialTheme.colorScheme.background
    val primaryTint = MaterialTheme.colorScheme.primary
    val tertiaryTint = MaterialTheme.colorScheme.tertiary
    val secondaryTint = MaterialTheme.colorScheme.secondary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseColor)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_dumingag_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = if (darkMode) 0.24f else 0.14f,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(radius = 28.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    } else {
                        Modifier
                    }
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = if (darkMode) {
                        listOf(
                            baseColor.copy(alpha = 0.72f),
                            baseColor.copy(alpha = 0.84f),
                            baseColor.copy(alpha = 0.90f)
                        )
                    } else {
                        listOf(
                            baseColor.copy(alpha = 0.76f),
                            baseColor.copy(alpha = 0.86f),
                            baseColor.copy(alpha = 0.90f)
                        )
                    }
                )
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryTint.copy(alpha = if (darkMode) 0.22f else 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.12f, size.height * 0.14f),
                    radius = size.width * 0.72f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tertiaryTint.copy(alpha = if (darkMode) 0.18f else 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.90f, size.height * 0.44f),
                    radius = size.width * 0.65f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        secondaryTint.copy(alpha = if (darkMode) 0.14f else 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.22f, size.height * 0.86f),
                    radius = size.width * 0.68f
                )
            )
        }
    }
}

/**
 * Reusable iOS-style Liquid Glass frosted surface container that automatically
 * adapts to light/dark themes with translucent vertical frosting, curved top
 * specular sheen, and diagonal crystalline rim border.
 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevation: Dp = 6.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val darkMode = LocalIsDarkTheme.current
    val shape = RoundedCornerShape(cornerRadius)
    val fillTop = MaterialTheme.colorScheme.surface.copy(alpha = if (darkMode) 0.82f else 0.88f)
    val fillBottom = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (darkMode) 0.74f else 0.78f)
    val borderBrush = Brush.linearGradient(
        colors = if (darkMode) {
            listOf(GlassDarkHighlight, GlassDarkBorderMid, GlassDarkBorderTail)
        } else {
            listOf(GlassLightHighlight, GlassLightBorderMid, GlassLightBorderTail)
        }
    )
    val topSheen = if (darkMode) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.56f)

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = if (darkMode) 0.34f else 0.12f),
                spotColor = Color.Black.copy(alpha = if (darkMode) 0.34f else 0.12f)
            )
            .clip(shape)
            .background(Brush.verticalGradient(colors = listOf(fillTop, fillBottom)))
            .drawBehind {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(topSheen, Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.32f
                    ),
                    cornerRadius = CornerRadius(cornerRadius.toPx(), cornerRadius.toPx())
                )
            }
            .border(width = 1.2.dp, brush = borderBrush, shape = shape)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurface
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

/**
 * Translucent iOS Liquid Glass card colors that softly refract the ambient
 * backdrop while preserving crisp readability in both light and dark modes.
 */
@Composable
fun liquidGlassCardColors(
    baseColor: Color = MaterialTheme.colorScheme.surface,
    lightAlpha: Float = 0.86f,
    darkAlpha: Float = 0.84f
) = androidx.compose.material3.CardDefaults.cardColors(
    containerColor = baseColor.copy(alpha = if (LocalIsDarkTheme.current) darkAlpha else lightAlpha),
    contentColor = contentColorFor(baseColor).takeOrElse { MaterialTheme.colorScheme.onSurface }
)

/**
 * Diagonal crystalline specular rim border matching iOS Liquid Glass panels.
 */
@Composable
fun liquidGlassBorder(width: Dp = 1.15.dp): androidx.compose.foundation.BorderStroke {
    val darkMode = LocalIsDarkTheme.current
    return androidx.compose.foundation.BorderStroke(
        width = width,
        brush = Brush.linearGradient(
            colors = if (darkMode) {
                listOf(GlassDarkHighlight, GlassDarkBorderMid, GlassDarkBorderTail)
            } else {
                listOf(GlassLightHighlight, GlassLightBorderMid, GlassLightBorderTail)
            }
        )
    )
}

/**
 * Refractive iOS Liquid Glass circular loader spinner with dual counter-rotating
 * luminous arcs and a breathing specular inner orb.
 */
@Composable
fun LiquidGlassSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    accentColor: Color = SplashGlassMintAccent
) {
    val infiniteTransition = rememberInfiniteTransition(label = "liquid_glass_spinner")
    val outerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinner_outer_angle"
    )
    val innerAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1850, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinner_inner_angle"
    )
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spinner_core_pulse"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .testTag("liquid_glass_spinner")
    ) {
        // Frosted crystalline glass ring housing
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(
                    SplashGlassPillActiveTop,
                    SplashGlassPanelTop,
                    SplashGlassPanelBottom
                ),
                start = Offset.Zero,
                end = Offset(this.size.width, this.size.height)
            )
        )
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(
                    SplashGlassSpecularTop.copy(alpha = 0.85f),
                    SplashGlassSpecularMid,
                    SplashGlassSpecularEdge
                )
            ),
            style = Stroke(width = 1.6.dp.toPx())
        )

        // Outer luminous sweeping specular arc
        val outerPad = 5.dp.toPx()
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    SplashGlassSpecularTop,
                    accentColor,
                    Color.Transparent
                )
            ),
            startAngle = outerAngle,
            sweepAngle = 125f,
            useCenter = false,
            topLeft = Offset(outerPad, outerPad),
            size = Size(this.size.width - outerPad * 2, this.size.height - outerPad * 2),
            style = Stroke(width = 3.8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Inner counter-rotating teal refraction arc
        val innerPad = 13.dp.toPx()
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color.Transparent,
                    SplashGlassTealAccent,
                    SplashGlassSpecularTop.copy(alpha = 0.75f),
                    Color.Transparent
                )
            ),
            startAngle = innerAngle,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = Offset(innerPad, innerPad),
            size = Size(this.size.width - innerPad * 2, this.size.height - innerPad * 2),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Breathing specular liquid droplet core
        val coreRadius = (this.size.minDimension * 0.14f) * corePulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    SplashGlassSpecularTop,
                    accentColor.copy(alpha = 0.75f),
                    Color.Transparent
                ),
                center = center,
                radius = coreRadius * 1.9f
            ),
            radius = coreRadius * 1.9f,
            center = center
        )
        drawCircle(
            color = SplashGlassSpecularTop.copy(alpha = 0.90f),
            radius = coreRadius * 0.58f,
            center = center
        )
    }
}

/**
 * Full-screen iOS Liquid Glass sign-out loader overlay blocking accidental taps
 * while smoothly displaying the refractive spinner and session status.
 */
@Composable
fun LiquidGlassSignOutOverlay(
    visible: Boolean,
    title: String = "Signing Out…",
    subtitle: String = "Securing your Dumingag travel session",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.94f, animationSpec = tween(220)),
        exit = fadeOut(tween(220)) + scaleOut(targetScale = 1.04f, animationSpec = tween(220)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.54f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {}
                )
                .padding(24.dp)
                .testTag("sign_out_liquid_glass_loader")
        ) {
            val shape = RoundedCornerShape(30.dp)
            Box(
                modifier = Modifier
                    .widthIn(min = 240.dp, max = 320.dp)
                    .shadow(24.dp, shape, ambientColor = Color.Black.copy(alpha = 0.45f))
                    .clip(shape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SplashGlassPanelTop,
                                SplashGlassPanelBottom
                            )
                        )
                    )
                    .drawBehind {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    SplashGlassSpecularTop.copy(alpha = 0.24f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.36f
                            ),
                            cornerRadius = CornerRadius(30.dp.toPx(), 30.dp.toPx())
                        )
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(SplashGlassWarmAccent.copy(alpha = 0.14f), Color.Transparent),
                                center = Offset(size.width * 0.85f, size.height * 0.20f),
                                radius = size.width * 0.55f
                            )
                        )
                    }
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                SplashGlassSpecularTop,
                                SplashGlassSpecularMid,
                                SplashGlassSpecularEdge
                            )
                        ),
                        shape = shape
                    )
                    .padding(horizontal = 28.dp, vertical = 26.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LiquidGlassSpinner(size = 72.dp)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SplashGlassTextPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("sign_out_loader_title")
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = SplashGlassTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

data class LiquidGlassNavTab(
    val route: String,
    val icon: ImageVector,
    val label: String
)

/**
 * Floating iOS 26-inspired Liquid Glass capsule bottom navigation bar with
 * specular rim, frosted translucent housing, and animated luminous active lens.
 */
@Composable
fun LiquidGlassFloatingNavBar(
    tabs: List<LiquidGlassNavTab>,
    selectedRoute: String,
    onSelectRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val darkMode = LocalIsDarkTheme.current
    val dockShape = RoundedCornerShape(30.dp)
    val dockFillTop = MaterialTheme.colorScheme.surface.copy(alpha = if (darkMode) 0.86f else 0.90f)
    val dockFillBottom = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (darkMode) 0.80f else 0.84f)
    val dockBorder = Brush.linearGradient(
        colors = if (darkMode) {
            listOf(GlassDarkHighlight, GlassDarkBorderMid, GlassDarkBorderTail)
        } else {
            listOf(GlassLightHighlight, GlassLightBorderMid, GlassLightBorderTail)
        }
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .shadow(
                    elevation = 14.dp,
                    shape = dockShape,
                    ambientColor = Color.Black.copy(alpha = if (darkMode) 0.42f else 0.16f),
                    spotColor = Color.Black.copy(alpha = if (darkMode) 0.42f else 0.16f)
                )
                .clip(dockShape)
                .background(Brush.verticalGradient(colors = listOf(dockFillTop, dockFillBottom)))
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (darkMode) 0.12f else 0.62f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = size.height * 0.45f
                        ),
                        cornerRadius = CornerRadius(30.dp.toPx(), 30.dp.toPx())
                    )
                }
                .border(width = 1.25.dp, brush = dockBorder, shape = dockShape)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val selected = tab.route == selectedRoute
                val pillScale by animateFloatAsState(
                    targetValue = if (selected) 1f else 0.95f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "nav_pill_scale_${tab.route}"
                )
                val iconTint by animateColorAsState(
                    targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200),
                    label = "nav_icon_tint_${tab.route}"
                )
                val itemShape = RoundedCornerShape(22.dp)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .scale(pillScale)
                        .heightIn(min = 52.dp)
                        .clip(itemShape)
                        .then(
                            if (selected) {
                                Modifier
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.tertiary
                                            )
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.42f),
                                        shape = itemShape
                                    )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onSelectRoute(tab.route) }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .testTag("nav_${tab.route}")
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.label,
                            tint = iconTint,
                            modifier = Modifier.size(21.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = iconTint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
