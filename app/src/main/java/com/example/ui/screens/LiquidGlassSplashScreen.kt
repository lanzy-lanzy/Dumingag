package com.example.ui.screens

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.SplashGlassMintAccent
import com.example.ui.theme.SplashGlassPanelBottom
import com.example.ui.theme.SplashGlassPanelTop
import com.example.ui.theme.SplashGlassPillActiveBottom
import com.example.ui.theme.SplashGlassPillActiveTop
import com.example.ui.theme.SplashGlassPillIdle
import com.example.ui.theme.SplashGlassSpecularEdge
import com.example.ui.theme.SplashGlassSpecularMid
import com.example.ui.theme.SplashGlassSpecularTop
import com.example.ui.theme.SplashGlassTealAccent
import com.example.ui.theme.SplashGlassTextPrimary
import com.example.ui.theme.SplashGlassTextSecondary
import com.example.ui.theme.SplashGlassThumbBottom
import com.example.ui.theme.SplashGlassThumbTop
import com.example.ui.theme.SplashGlassTrackBg
import com.example.ui.theme.SplashGlassWarmAccent
import com.example.ui.theme.SplashScrimBottom
import com.example.ui.theme.SplashScrimCenter
import com.example.ui.theme.SplashScrimTop
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal data class SplashScene(
    val id: String,
    val chipLabel: String,
    val title: String,
    val subtitle: String,
    val statBadge: String,
    val icon: ImageVector,
    val accentColor: Color,
    @DrawableRes val imageRes: Int
)

internal val DumingagSplashScenes = listOf(
    SplashScene(
        id = "highlands",
        chipLabel = "Highlands",
        title = "Misty Dumingag Highlands",
        subtitle = "Lush mountain valleys, crisp morning breezes, and panoramic trails across Zamboanga del Sur.",
        statBadge = "8.16° N · Organic Valley",
        icon = Icons.Default.Landscape,
        accentColor = SplashGlassMintAccent,
        imageRes = R.drawable.img_dumingag_hero
    ),
    SplashScene(
        id = "waterfalls",
        chipLabel = "Waterfalls",
        title = "Crystal Cascades & Springs",
        subtitle = "Explore Danlugan Falls and hidden highland pools with live distance & travel-hour mapping.",
        statBadge = "Danlugan Falls · Nature",
        icon = Icons.Default.WaterDrop,
        accentColor = SplashGlassTealAccent,
        imageRes = R.drawable.img_waterfall_falls
    ),
    SplashScene(
        id = "ecofarms",
        chipLabel = "Eco-Farms",
        title = "Organic Capital Heritage",
        subtitle = "Visit Paglinawan Eco-Farm and sustainable terraces celebrated across Mindanao.",
        statBadge = "Paglinawan · Eco-Tourism",
        icon = Icons.Default.Eco,
        accentColor = SplashGlassMintAccent,
        imageRes = R.drawable.img_organic_village
    ),
    SplashScene(
        id = "heritage",
        chipLabel = "Heritage",
        title = "Subanen Roots & Town Plaza",
        subtitle = "Discover cultural landmarks, local habal-habal routes, and community travel guides offline.",
        statBadge = "Poblacion · Culture",
        icon = Icons.Default.AccountBalance,
        accentColor = SplashGlassWarmAccent,
        imageRes = R.drawable.img_heritage_plaza
    )
)

@Composable
fun LiquidGlassSplashScreen(
    onEnterApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var activeSceneIndex by rememberSaveable { mutableIntStateOf(0) }
    val activeScene = DumingagSplashScenes[activeSceneIndex.coerceIn(0, DumingagSplashScenes.lastIndex)]

    var hasEntered by remember { mutableStateOf(false) }
    var crestTapCount by remember { mutableIntStateOf(0) }
    var touchLightTarget by remember { mutableStateOf(Offset(0.5f, 0.34f)) }
    val rippleProgress = remember { Animatable(1f) }
    var rippleCenterFraction by remember { mutableStateOf(Offset(0.5f, 0.34f)) }

    val triggerFinish = {
        if (!hasEntered) {
            hasEntered = true
            onEnterApp()
        }
    }

    val triggerRippleAt: (Offset) -> Unit = { fraction ->
        touchLightTarget = fraction
        rippleCenterFraction = fraction
        scope.launch {
            rippleProgress.snapTo(0f)
            rippleProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "liquid_glass_splash")
    val kenBurnsScale by infiniteTransition.animateFloat(
        initialValue = 1.04f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ken_burns_scale"
    )
    val causticPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "caustic_phase"
    )
    val rimOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rim_orbit_angle"
    )
    val chevronShimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chevron_shimmer"
    )

    val animatedTouchLight by animateOffsetAsState(
        targetValue = touchLightTarget,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "touch_light_spring"
    )

    var crestPressed by remember { mutableStateOf(false) }
    val crestScale by animateFloatAsState(
        targetValue = if (crestPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "crest_bounce"
    )
    LaunchedEffect(crestTapCount) {
        if (crestTapCount > 0) {
            crestPressed = true
            kotlinx.coroutines.delay(130)
            crestPressed = false
        }
    }

    val activeAccentColor by animateColorAsState(
        targetValue = activeScene.accentColor,
        animationSpec = tween(durationMillis = 480),
        label = "scene_accent"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SplashScrimBottom)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (size.width > 0 && size.height > 0) {
                        val fx = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        val fy = (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                        triggerRippleAt(Offset(fx, fy))
                    }
                }
            }
            .testTag("liquid_glass_splash_root")
    ) {
        // 1. Living Scenic Backdrop with Ken Burns Motion & Subtle Peripheral Depth
        Crossfade(
            targetState = activeScene,
            animationSpec = tween(durationMillis = 550),
            label = "splash_scene_crossfade",
            modifier = Modifier.fillMaxSize()
        ) { scene ->
            Image(
                painter = painterResource(id = scene.imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(kenBurnsScale)
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Modifier.blur(radius = 4.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        } else {
                            Modifier
                        }
                    )
            )
        }

        // 2. Atmospheric Scrim + Floating Liquid-Glass Caustic Orbs & Touch Ripple
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

            // Orbiting ambient emerald/teal/warm refraction blobs
            val orb1 = Offset(
                x = size.width * (0.20f + 0.08f * cos(causticPhase)),
                y = size.height * (0.24f + 0.05f * sin(causticPhase))
            )
            val orb2 = Offset(
                x = size.width * (0.82f + 0.07f * sin(causticPhase * 0.8f)),
                y = size.height * (0.46f + 0.06f * cos(causticPhase * 0.9f))
            )
            val orb3 = Offset(
                x = size.width * (0.36f + 0.09f * cos(causticPhase * 1.1f + 1.4f)),
                y = size.height * (0.76f + 0.04f * sin(causticPhase * 0.7f))
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(activeAccentColor.copy(alpha = 0.34f), Color.Transparent),
                    center = orb1,
                    radius = size.width * 0.65f
                ),
                center = orb1,
                radius = size.width * 0.65f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SplashGlassTealAccent.copy(alpha = 0.24f), Color.Transparent),
                    center = orb2,
                    radius = size.width * 0.58f
                ),
                center = orb2,
                radius = size.width * 0.58f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SplashGlassWarmAccent.copy(alpha = 0.18f), Color.Transparent),
                    center = orb3,
                    radius = size.width * 0.62f
                ),
                center = orb3,
                radius = size.width * 0.62f
            )

            // Interactive touch specular glow
            val lightCenter = Offset(
                x = size.width * animatedTouchLight.x,
                y = size.height * animatedTouchLight.y
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SplashGlassSpecularTop.copy(alpha = 0.22f),
                        activeAccentColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = lightCenter,
                    radius = size.width * 0.42f
                ),
                center = lightCenter,
                radius = size.width * 0.42f
            )

            // Liquid ripple ring emitted when the user taps anywhere on the glass
            val rp = rippleProgress.value
            if (rp < 0.99f) {
                val center = Offset(
                    x = size.width * rippleCenterFraction.x,
                    y = size.height * rippleCenterFraction.y
                )
                val radius = size.minDimension * (0.06f + rp * 0.52f)
                val alpha = (1f - rp) * 0.55f
                drawCircle(
                    color = SplashGlassSpecularTop.copy(alpha = alpha),
                    radius = radius,
                    center = center,
                    style = Stroke(width = (3.5.dp.toPx() * (1f - rp * 0.5f)))
                )
                drawCircle(
                    color = activeAccentColor.copy(alpha = alpha * 0.65f),
                    radius = radius * 0.72f,
                    center = center,
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }
        }

        // 3. Foreground Liquid Glass Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Row: Live Location Glass Capsule + Skip Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiquidGlassCapsule(
                    modifier = Modifier.heightIn(min = 40.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(activeAccentColor)
                        )
                        Text(
                            text = "DUMINGAG · ZDS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.1.sp,
                            color = SplashGlassTextPrimary,
                            maxLines = 1
                        )
                    }
                }

                LiquidGlassCapsule(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { triggerFinish() }
                        .testTag("splash_skip_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Skip",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SplashGlassTextPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Skip splash screen",
                            tint = SplashGlassMintAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Centerpiece: Interactive Refractive Liquid-Glass Crest Medallion & Brand Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(134.dp)
                        .scale(crestScale)
                        .clip(CircleShape)
                        .clickable {
                            crestTapCount++
                            activeSceneIndex = (activeSceneIndex + 1) % DumingagSplashScenes.size
                            triggerRippleAt(Offset(0.5f, 0.34f))
                        }
                        .semantics { contentDescription = "Dumingag emblem, tap to switch scenic preview" }
                        .testTag("splash_crest_medallion")
                ) {
                    // Outer frosted liquid lens ring + orbiting specular highlight arc
                    Canvas(modifier = Modifier.fillMaxSize()) {
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
                        val pad = 4.dp.toPx()
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    SplashGlassSpecularTop.copy(alpha = 0.92f),
                                    activeAccentColor.copy(alpha = 0.85f),
                                    Color.Transparent
                                )
                            ),
                            startAngle = rimOrbitAngle,
                            sweepAngle = 95f,
                            useCenter = false,
                            topLeft = Offset(pad, pad),
                            size = Size(size.width - pad * 2, size.height - pad * 2),
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Official Dumingag Crest inside inner crystalline lens
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .shadow(12.dp, CircleShape, clip = false)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(SplashGlassSpecularTop, SplashGlassSpecularEdge)
                                ),
                                shape = CircleShape
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_dumingag_foreground),
                            contentDescription = "Official Municipality of Dumingag crest",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Dumingag",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = SplashGlassTextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Interactive Highland Travel Guide",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = SplashGlassTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tap the glass scenes or crest to preview",
                    style = MaterialTheme.typography.labelSmall,
                    color = SplashGlassMintAccent,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bottom Dock: Interactive Liquid Glass Scene Switcher + Showcase Card + Slide-to-Explore
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive 4-Scene Liquid Glass Pill Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DumingagSplashScenes.forEachIndexed { idx, scene ->
                        val isSelected = idx == activeSceneIndex
                        val pillBorderColor by animateColorAsState(
                            targetValue = if (isSelected) SplashGlassSpecularTop else SplashGlassSpecularEdge,
                            animationSpec = tween(250),
                            label = "pill_border_$idx"
                        )
                        val pillScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.03f else 0.98f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "pill_scale_$idx"
                        )

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .scale(pillScale)
                                .heightIn(min = 54.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    brush = if (isSelected) {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                SplashGlassPillActiveTop,
                                                SplashGlassPillActiveBottom
                                            )
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            colors = listOf(
                                                SplashGlassPillIdle,
                                                SplashGlassPanelBottom
                                            )
                                        )
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = pillBorderColor,
                                    shape = RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    activeSceneIndex = idx
                                    triggerRippleAt(Offset((idx + 0.5f) / 4f, 0.68f))
                                }
                                .padding(horizontal = 4.dp, vertical = 8.dp)
                                .testTag("splash_scene_pill_${scene.id}")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = scene.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) SplashGlassTextPrimary else scene.accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = scene.chipLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = SplashGlassTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Main iOS Liquid Glass Card with Specular Refraction Rim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 16.dp, shape = RoundedCornerShape(30.dp), clip = false)
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    SplashGlassPanelTop,
                                    SplashGlassPanelBottom
                                )
                            )
                        )
                        .drawBehind {
                            // Subtle top inner liquid sheen highlight like iOS glass
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        SplashGlassSpecularTop.copy(alpha = 0.20f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = size.height * 0.42f
                                ),
                                cornerRadius = CornerRadius(30.dp.toPx(), 30.dp.toPx())
                            )
                        }
                        .border(
                            width = 1.4.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    SplashGlassSpecularTop,
                                    SplashGlassSpecularMid,
                                    SplashGlassSpecularEdge
                                )
                            ),
                            shape = RoundedCornerShape(30.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = activeScene,
                            transitionSpec = {
                                (fadeIn(tween(260)) + slideInVertically { it / 6 })
                                    .togetherWith(fadeOut(tween(180)) + slideOutVertically { -it / 6 })
                            },
                            label = "splash_scene_copy"
                        ) { scene ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = scene.accentColor.copy(alpha = 0.20f),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, scene.accentColor.copy(alpha = 0.55f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = scene.accentColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = scene.statBadge,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SplashGlassTextPrimary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "${activeSceneIndex + 1} / ${DumingagSplashScenes.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SplashGlassTextSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Text(
                                    text = scene.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = SplashGlassTextPrimary,
                                    modifier = Modifier.testTag("splash_scene_title")
                                )
                                Text(
                                    text = scene.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SplashGlassTextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Interactive iOS-Style Slide-or-Tap Liquid Glass Action Track
                        LiquidGlassExploreSlider(
                            accentColor = activeAccentColor,
                            shimmerPhase = chevronShimmer,
                            onComplete = triggerFinish
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassCapsule(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        SplashGlassPillIdle,
                        SplashGlassPanelBottom
                    )
                )
            )
            .border(
                width = 1.1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        SplashGlassSpecularTop.copy(alpha = 0.72f),
                        SplashGlassSpecularEdge
                    )
                ),
                shape = RoundedCornerShape(50)
            )
    ) {
        content()
    }
}

@Composable
private fun LiquidGlassExploreSlider(
    accentColor: Color,
    shimmerPhase: Float,
    onComplete: () -> Unit
) {
    val density = LocalDensity.current
    val thumbSizeDp = 52.dp
    val trackPaddingDp = 4.dp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(50))
            .background(SplashGlassTrackBg)
            .border(
                width = 1.25.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        SplashGlassSpecularTop.copy(alpha = 0.80f),
                        accentColor.copy(alpha = 0.50f),
                        SplashGlassSpecularEdge
                    )
                ),
                shape = RoundedCornerShape(50)
            )
            .clickable { onComplete() }
            .testTag("splash_explore_button")
    ) {
        val thumbSizePx = with(density) { thumbSizeDp.toPx() }
        val trackPadPx = with(density) { trackPaddingDp.toPx() }
        val maxDragPx = (constraints.maxWidth.toFloat() - thumbSizePx - trackPadPx * 2f).coerceAtLeast(1f)

        var rawDragPx by remember { mutableFloatStateOf(0f) }
        var isDragging by remember { mutableStateOf(false) }

        val animatedDragPx by animateFloatAsState(
            targetValue = rawDragPx,
            animationSpec = if (isDragging) {
                spring(stiffness = Spring.StiffnessHigh)
            } else {
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            },
            label = "explore_slider_drag"
        )
        val progress = (animatedDragPx / maxDragPx).coerceIn(0f, 1f)
        val thumbWidthDp by animateDpAsState(
            targetValue = if (isDragging) 58.dp else thumbSizeDp,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
            label = "thumb_stretch"
        )

        // Luminous progress fill trailing behind the sliding glass orb
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val fillWidth = (trackPadPx + animatedDragPx + thumbSizePx).coerceIn(thumbSizePx, size.width)
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.42f),
                                SplashGlassSpecularMid.copy(alpha = 0.34f)
                            )
                        ),
                        size = Size(fillWidth, size.height),
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                    )
                }
        )

        // Center Call-to-Action Text + Animated Shimmering Chevrons
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 42.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (progress > 0.45f) "Release to Explore" else "Slide or Tap to Explore",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = SplashGlassTextPrimary.copy(alpha = (1f - progress * 0.4f).coerceIn(0.55f, 1f)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                for (i in 0..2) {
                    val wave = ((shimmerPhase * 3f - i).mod(3f))
                    val alpha = (1f - (wave / 2.2f)).coerceIn(0.28f, 1f)
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = accentColor.copy(alpha = alpha),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Draggable Crystalline Liquid Glass Thumb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((trackPadPx + animatedDragPx).roundToInt(), 0) }
                .size(width = thumbWidthDp, height = thumbSizeDp)
                .shadow(elevation = 8.dp, shape = CircleShape, clip = false)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            SplashGlassThumbTop,
                            SplashGlassThumbBottom
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = SplashGlassSpecularTop,
                    shape = CircleShape
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        isDragging = true
                        rawDragPx = (rawDragPx + delta).coerceIn(0f, maxDragPx)
                    },
                    onDragStopped = {
                        isDragging = false
                        if (rawDragPx / maxDragPx >= 0.68f) {
                            rawDragPx = maxDragPx
                            onComplete()
                        } else {
                            rawDragPx = 0f
                        }
                    }
                )
                .testTag("splash_explore_slider")
        ) {
            Icon(
                imageVector = if (progress > 0.55f) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Explore,
                contentDescription = "Slide to explore Dumingag",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
