package com.example.ui.screens

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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.ui.components.GoogleBrandIcon
import com.example.ui.theme.EmeraldPrimary
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AuthScreen(
    pendingVerificationEmail: String? = null,
    onAuthSuccess: () -> Unit,
    createAccount: suspend (username: String, email: String, password: String) -> Boolean = GoogleAuthHelper::createAccount,
    signIn: suspend (email: String, password: String) -> Boolean = GoogleAuthHelper::signIn,
    onCheckVerification: suspend () -> Boolean = GoogleAuthHelper::refreshEmailVerificationStatus,
    onResendVerification: suspend () -> Unit = GoogleAuthHelper::resendVerificationEmail,
    onSignOutPendingVerification: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var pendingMethod by remember { mutableStateOf<String?>(null) }
    val isLoading = pendingMethod != null
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    var registering by rememberSaveable { mutableStateOf(false) }
    var desiredUsername by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var localPendingVerificationEmail by rememberSaveable { mutableStateOf<String?>(null) }

    val activeVerificationEmail = pendingVerificationEmail?.takeIf { it.isNotBlank() }
        ?: localPendingVerificationEmail

    val infiniteTransition = rememberInfiniteTransition(label = "auth_liquid_glass")
    val causticPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auth_caustic_phase"
    )
    val backdropScale by infiniteTransition.animateFloat(
        initialValue = 1.04f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "auth_backdrop_scale"
    )
    val crestOrbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "auth_crest_orbit"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashScrimBottom)
    ) {
        // Full-bleed blurred highland photography so translucent iOS liquid glass refracts living depth
        Image(
            painter = painterResource(id = R.drawable.img_dumingag_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(backdropScale)
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(radius = 20.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    } else {
                        Modifier
                    }
                )
        )

        // Drifting caustic aurora orbs + deep emerald glass atmosphere
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
            val orb1 = Offset(
                x = size.width * (0.16f + 0.08f * cos(causticPhase)),
                y = size.height * (0.20f + 0.05f * sin(causticPhase))
            )
            val orb2 = Offset(
                x = size.width * (0.86f + 0.07f * sin(causticPhase * 0.85f)),
                y = size.height * (0.45f + 0.06f * cos(causticPhase * 0.9f))
            )
            val orb3 = Offset(
                x = size.width * (0.28f + 0.09f * cos(causticPhase * 1.1f + 1.2f)),
                y = size.height * (0.80f + 0.05f * sin(causticPhase * 0.8f))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SplashGlassMintAccent.copy(alpha = 0.36f), Color.Transparent),
                    center = orb1,
                    radius = size.width * 0.68f
                ),
                center = orb1,
                radius = size.width * 0.68f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SplashGlassTealAccent.copy(alpha = 0.28f), Color.Transparent),
                    center = orb2,
                    radius = size.width * 0.62f
                ),
                center = orb2,
                radius = size.width * 0.62f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SplashGlassWarmAccent.copy(alpha = 0.22f), Color.Transparent),
                    center = orb3,
                    radius = size.width * 0.65f
                ),
                center = orb3,
                radius = size.width * 0.65f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .displayCutoutPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Floating iOS Liquid Glass Brand Header with High-Contrast Smoked Backing
            val headerShape = RoundedCornerShape(24.dp)
            Row(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp)
                    .clip(headerShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                SplashScrimBottom.copy(alpha = 0.66f),
                                SplashGlassPanelBottom.copy(alpha = 0.78f)
                            )
                        )
                    )
                    .drawBehind {
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    SplashGlassSpecularTop.copy(alpha = 0.22f),
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.42f
                            ),
                            cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                        )
                    }
                    .border(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                SplashGlassSpecularTop.copy(alpha = 0.85f),
                                SplashGlassSpecularMid,
                                SplashGlassSpecularEdge
                            )
                        ),
                        shape = headerShape
                    )
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(60.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(SplashGlassPillActiveTop, SplashGlassPanelBottom)
                            )
                        )
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(SplashGlassSpecularTop, SplashGlassSpecularEdge)
                            ),
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        val pad = 3.dp.toPx()
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    SplashGlassSpecularTop,
                                    SplashGlassMintAccent,
                                    Color.Transparent
                                )
                            ),
                            startAngle = crestOrbitAngle,
                            sweepAngle = 95f,
                            useCenter = false,
                            topLeft = Offset(pad, pad),
                            size = androidx.compose.ui.geometry.Size(size.width - pad * 2, size.height - pad * 2),
                            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Image(
                        painter = painterResource(id = R.drawable.ic_dumingag_foreground),
                        contentDescription = "Dumingag Valley Crest",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, SplashGlassSpecularTop, CircleShape)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(SplashScrimBottom.copy(alpha = 0.72f))
                            .border(
                                width = 1.dp,
                                color = SplashGlassMintAccent.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(50)
                            )
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "ZAMBOANGA DEL SUR · MINDANAO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = SplashGlassMintAccent,
                            maxLines = 2
                        )
                    }
                    Text(
                        text = "Dumingag Travel Guide",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            lineHeight = 25.sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = SplashGlassTextPrimary,
                        maxLines = 2,
                        modifier = Modifier.testTag("auth_header_title")
                    )
                    Text(
                        text = "Offline maps, local spots & community reviews",
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = 18.sp
                        ),
                        fontWeight = FontWeight.Medium,
                        color = SplashGlassTextPrimary.copy(alpha = 0.90f),
                        maxLines = 2,
                        modifier = Modifier.testTag("auth_header_subtitle")
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (activeVerificationEmail != null) {
                    EmailVerificationCard(
                        email = activeVerificationEmail,
                        isLoading = isLoading,
                        pendingMethod = pendingMethod,
                        errorMessage = errorMessage,
                        infoMessage = infoMessage,
                        onCheckVerification = {
                            errorMessage = null
                            infoMessage = null
                            pendingMethod = "verify_check"
                            scope.launch {
                                try {
                                    val verified = onCheckVerification()
                                    if (verified) {
                                        localPendingVerificationEmail = null
                                        onAuthSuccess()
                                    } else {
                                        errorMessage = "Your email ($activeVerificationEmail) is not verified yet. Open the verification link sent to your inbox, then tap Continue."
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage ?: "Unable to verify email status right now."
                                } finally {
                                    pendingMethod = null
                                }
                            }
                        },
                        onResendVerification = {
                            errorMessage = null
                            infoMessage = null
                            pendingMethod = "verify_resend"
                            scope.launch {
                                try {
                                    onResendVerification()
                                    infoMessage = "Verification link sent to $activeVerificationEmail. Check your inbox or spam folder."
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    errorMessage = e.localizedMessage ?: "Could not resend verification email."
                                } finally {
                                    pendingMethod = null
                                }
                            }
                        },
                        onBackToSignIn = {
                            localPendingVerificationEmail = null
                            password = ""
                            errorMessage = null
                            infoMessage = null
                            registering = false
                            onSignOutPendingVerification()
                        }
                    )
                } else {
                    // High-contrast frosted glass input colors — luminous white text on translucent dark glass
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SplashGlassPillIdle,
                        unfocusedContainerColor = SplashGlassTrackBg,
                        disabledContainerColor = SplashGlassTrackBg.copy(alpha = 0.45f),
                        focusedTextColor = SplashGlassTextPrimary,
                        unfocusedTextColor = SplashGlassTextPrimary,
                        disabledTextColor = SplashGlassTextSecondary.copy(alpha = 0.7f),
                        cursorColor = SplashGlassMintAccent,
                        focusedLabelColor = SplashGlassMintAccent,
                        unfocusedLabelColor = SplashGlassTextSecondary,
                        focusedBorderColor = SplashGlassSpecularTop,
                        unfocusedBorderColor = SplashGlassSpecularMid,
                        focusedLeadingIconColor = SplashGlassMintAccent,
                        unfocusedLeadingIconColor = SplashGlassTextSecondary,
                        focusedTrailingIconColor = SplashGlassMintAccent,
                        unfocusedTrailingIconColor = SplashGlassTextSecondary
                    )

                    LiquidGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_primary_card")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // iOS Liquid Glass Segmented Control (Sign In / Create Account)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(SplashGlassTrackBg)
                                    .border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                SplashGlassSpecularTop.copy(alpha = 0.65f),
                                                SplashGlassSpecularEdge
                                            )
                                        ),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                                    .padding(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AuthModeTab(
                                        label = "Sign In",
                                        selected = !registering,
                                        enabled = !isLoading,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("auth_tab_sign_in"),
                                        onClick = {
                                            if (registering) {
                                                registering = false
                                                password = ""
                                                errorMessage = null
                                                infoMessage = null
                                            }
                                        }
                                    )
                                    AuthModeTab(
                                        label = "Create Account",
                                        selected = registering,
                                        enabled = !isLoading,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("auth_tab_create_account"),
                                        onClick = {
                                            if (!registering) {
                                                registering = true
                                                password = ""
                                                errorMessage = null
                                                infoMessage = null
                                            }
                                        }
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = if (registering) "Create Your Account" else "Welcome Back",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SplashGlassTextPrimary
                                )
                                Text(
                                    text = if (registering) {
                                        "Enter your username, email, and password. You must verify your email before logging in."
                                    } else {
                                        "Sign in with your verified email & password or use Google Sign-In."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SplashGlassTextSecondary
                                )
                            }

                            AnimatedVisibility(
                                visible = registering,
                                enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                                exit = fadeOut(tween(160)) + shrinkVertically(tween(180))
                            ) {
                                OutlinedTextField(
                                    value = desiredUsername,
                                    onValueChange = { desiredUsername = it.take(80) },
                                    label = { Text("Username") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Person, contentDescription = null)
                                    },
                                    singleLine = true,
                                    enabled = !isLoading,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = fieldColors,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("auth_username_input")
                                )
                            }

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                leadingIcon = {
                                    Icon(Icons.Default.Email, contentDescription = null)
                                },
                                singleLine = true,
                                enabled = !isLoading,
                                shape = RoundedCornerShape(16.dp),
                                colors = fieldColors,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Next
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_email_input")
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Password") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                },
                                trailingIcon = {
                                    IconButton(
                                        onClick = { passwordVisible = !passwordVisible },
                                        modifier = Modifier.testTag("auth_password_toggle")
                                    ) {
                                        Icon(
                                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                        )
                                    }
                                },
                                singleLine = true,
                                enabled = !isLoading,
                                shape = RoundedCornerShape(16.dp),
                                colors = fieldColors,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_password_input")
                            )

                            AnimatedVisibility(
                                visible = registering,
                                enter = fadeIn(tween(200)) + expandVertically(tween(220)),
                                exit = fadeOut(tween(160)) + shrinkVertically(tween(180))
                            ) {
                                Surface(
                                    color = SplashGlassWarmAccent.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, SplashGlassWarmAccent.copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = SplashGlassWarmAccent,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Email verification required before your first login.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = SplashGlassTextPrimary
                                        )
                                    }
                                }
                            }

                            if (errorMessage != null) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.94f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = errorMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            if (infoMessage != null) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = SplashGlassMintAccent.copy(alpha = 0.22f)
                                    ),
                                    border = BorderStroke(1.dp, SplashGlassMintAccent.copy(alpha = 0.55f)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = infoMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SplashGlassTextPrimary,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            val submitEnabled = !isLoading &&
                                email.isNotBlank() &&
                                password.isNotEmpty() &&
                                (!registering || desiredUsername.trim().length >= 2)

                            Button(
                                onClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    pendingMethod = "email"
                                    scope.launch {
                                        try {
                                            val cleanEmail = email.trim()
                                            val verified = if (registering) {
                                                createAccount(desiredUsername.trim(), cleanEmail, password)
                                            } else {
                                                signIn(cleanEmail, password)
                                            }
                                            if (verified) {
                                                localPendingVerificationEmail = null
                                                onAuthSuccess()
                                            } else {
                                                localPendingVerificationEmail = cleanEmail
                                                infoMessage = if (registering) {
                                                    "Account created! Verify the link sent to $cleanEmail to enter the app."
                                                } else {
                                                    "Please verify $cleanEmail before signing in."
                                                }
                                            }
                                        } catch (e: CancellationException) {
                                            throw e
                                        } catch (e: Exception) {
                                            errorMessage = e.localizedMessage ?: "Authentication failed. Please try again."
                                        } finally {
                                            pendingMethod = null
                                        }
                                    }
                                },
                                enabled = submitEnabled,
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SplashGlassThumbTop,
                                    contentColor = EmeraldPrimary,
                                    disabledContainerColor = SplashGlassPillActiveTop.copy(alpha = 0.26f),
                                    disabledContentColor = SplashGlassTextSecondary.copy(alpha = 0.78f)
                                ),
                                border = BorderStroke(
                                    width = 1.25.dp,
                                    color = if (submitEnabled) SplashGlassSpecularTop else SplashGlassSpecularEdge
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("auth_submit_button")
                            ) {
                                if (pendingMethod == "email") {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = EmeraldPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(if (registering) "Creating Account…" else "Signing In…")
                                } else {
                                    Text(
                                        text = if (registering) "Create & Verify Account" else "Sign In to Guide",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = SplashGlassSpecularEdge
                                )
                                Text(
                                    text = "OR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = SplashGlassTextSecondary
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = SplashGlassSpecularEdge
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    errorMessage = null
                                    infoMessage = null
                                    pendingMethod = "google"
                                    GoogleAuthHelper.onGoogleSignInClicked(
                                        context = context,
                                        credentialManager = credentialManager,
                                        onAuthSuccess = {
                                            pendingMethod = null
                                            localPendingVerificationEmail = null
                                            onAuthSuccess()
                                        },
                                        onAuthError = { msg ->
                                            pendingMethod = null
                                            errorMessage = msg
                                        },
                                        scope = scope,
                                        onAuthCancelled = {
                                            pendingMethod = null
                                        }
                                    )
                                },
                                enabled = !isLoading,
                                shape = RoundedCornerShape(18.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = SplashGlassPillIdle,
                                    contentColor = SplashGlassTextPrimary
                                ),
                                border = BorderStroke(
                                    width = 1.2.dp,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            SplashGlassSpecularTop.copy(alpha = 0.78f),
                                            SplashGlassSpecularEdge
                                        )
                                    )
                                ),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("google_sign_in_button")
                            ) {
                                if (pendingMethod == "google") {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = SplashGlassMintAccent,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    GoogleBrandIcon()
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (registering) "Sign up with Google" else "Continue with Google",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SplashGlassTextPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(
                                    onClick = {
                                        registering = !registering
                                        password = ""
                                        errorMessage = null
                                        infoMessage = null
                                    },
                                    enabled = !isLoading,
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = SplashGlassMintAccent
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 48.dp)
                                        .testTag("auth_mode_switch_link")
                                ) {
                                    Text(
                                        text = if (registering) {
                                            "Already have an account? Sign In"
                                        } else {
                                            "New traveler? Create Account"
                                        },
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "What's Inside Dumingag",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SplashGlassTextPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    FeatureHighlightRow(
                        icon = Icons.Default.Map,
                        title = "Interactive Map & Offline Routing",
                        subtitle = "Explore landmarks with turn-by-turn routes even without signal."
                    )
                    FeatureHighlightRow(
                        icon = Icons.Default.Translate,
                        title = "4 Languages for Travelers",
                        subtitle = "Switch instantly between English, Bisaya, Tagalog, and Español."
                    )
                    FeatureHighlightRow(
                        icon = Icons.Default.CloudOff,
                        title = "Offline Vault & Weather Advisories",
                        subtitle = "Cache guides, saved permits, transit fares, and emergency hotlines."
                    )
                    FeatureHighlightRow(
                        icon = Icons.Default.Shield,
                        title = "Community Reviews & Bookmarks",
                        subtitle = "Bookmark favorites and share ratings with fellow travelers."
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(30.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            SplashGlassSpecularTop,
            SplashGlassSpecularMid,
            SplashGlassSpecularEdge
        ),
        start = Offset.Zero,
        end = Offset(900f, 1200f)
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 20.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.40f),
                spotColor = Color.Black.copy(alpha = 0.40f)
            )
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
                // Curved top specular liquid sheen characteristic of iOS Liquid Glass
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SplashGlassSpecularTop.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        startY = 0f,
                        endY = size.height * 0.28f
                    ),
                    cornerRadius = CornerRadius(30.dp.toPx(), 30.dp.toPx())
                )
            }
            .border(width = 1.4.dp, brush = borderBrush, shape = shape)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun AuthModeTab(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val labelColor by animateColorAsState(
        targetValue = if (selected) SplashGlassTextPrimary else SplashGlassTextSecondary.copy(alpha = 0.85f),
        animationSpec = tween(durationMillis = 200),
        label = "auth_tab_fg"
    )
    val tabScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.98f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "auth_tab_scale"
    )
    val tabShape = RoundedCornerShape(14.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(tabScale)
            .heightIn(min = 48.dp)
            .clip(tabShape)
            .then(
                if (selected) {
                    Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    SplashGlassPillActiveTop,
                                    SplashGlassPillActiveBottom
                                )
                            )
                        )
                        .border(
                            width = 1.25.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    SplashGlassSpecularTop,
                                    SplashGlassSpecularMid
                                )
                            ),
                            shape = tabShape
                        )
                } else {
                    Modifier.background(Color.Transparent)
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun EmailVerificationCard(
    email: String,
    isLoading: Boolean,
    pendingMethod: String?,
    errorMessage: String?,
    infoMessage: String?,
    onCheckVerification: () -> Unit,
    onResendVerification: () -> Unit,
    onBackToSignIn: () -> Unit
) {
    LiquidGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("email_verification_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = SplashGlassMintAccent.copy(alpha = 0.20f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.2.dp, SplashGlassSpecularTop.copy(alpha = 0.65f)),
                modifier = Modifier.size(66.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = "Email verification required",
                        tint = SplashGlassMintAccent,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Text(
                text = "Verify Your Email",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = SplashGlassTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "We sent a verification link to $email. Open the link in your inbox before accessing your Dumingag account.",
                style = MaterialTheme.typography.bodyMedium,
                color = SplashGlassTextSecondary,
                textAlign = TextAlign.Center
            )

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.94f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.50f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (infoMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = SplashGlassMintAccent.copy(alpha = 0.22f)
                    ),
                    border = BorderStroke(1.dp, SplashGlassMintAccent.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = infoMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SplashGlassTextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = onCheckVerification,
                enabled = !isLoading,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SplashGlassThumbTop,
                    contentColor = EmeraldPrimary
                ),
                border = BorderStroke(1.2.dp, SplashGlassSpecularTop),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("check_verification_button")
            ) {
                if (pendingMethod == "verify_check") {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = EmeraldPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking…")
                } else {
                    Text("I've Verified — Continue", fontWeight = FontWeight.ExtraBold)
                }
            }

            OutlinedButton(
                onClick = onResendVerification,
                enabled = !isLoading,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SplashGlassPillIdle,
                    contentColor = SplashGlassTextPrimary
                ),
                border = BorderStroke(1.dp, SplashGlassSpecularMid),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("resend_verification_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (pendingMethod == "verify_resend") "Sending Link…" else "Resend Verification Email",
                    fontWeight = FontWeight.SemiBold
                )
            }

            TextButton(
                onClick = onBackToSignIn,
                enabled = !isLoading,
                colors = ButtonDefaults.textButtonColors(contentColor = SplashGlassMintAccent),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("back_to_sign_in_button")
            ) {
                Text("Back to Sign In", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    val cardShape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        SplashGlassPanelTop,
                        SplashGlassPanelBottom
                    )
                )
            )
            .border(
                width = 1.1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        SplashGlassSpecularTop.copy(alpha = 0.65f),
                        SplashGlassSpecularEdge
                    )
                ),
                shape = cardShape
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = SplashGlassMintAccent.copy(alpha = 0.18f),
            shape = RoundedCornerShape(13.dp),
            border = BorderStroke(1.dp, SplashGlassMintAccent.copy(alpha = 0.45f)),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = SplashGlassMintAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = SplashGlassTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SplashGlassTextSecondary
            )
        }
    }
}
