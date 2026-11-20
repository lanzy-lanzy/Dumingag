package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.ui.components.GoogleBrandIcon
import com.example.ui.theme.GlassBackdropBottom
import com.example.ui.theme.GlassBackdropDeepTop
import com.example.ui.theme.GlassBackdropMid
import com.example.ui.theme.GlassDarkBorderMid
import com.example.ui.theme.GlassDarkBorderTail
import com.example.ui.theme.GlassDarkHighlight
import com.example.ui.theme.GlassHeroScrimBottom
import com.example.ui.theme.GlassHeroScrimTop
import com.example.ui.theme.GlassLightBorderMid
import com.example.ui.theme.GlassLightBorderTail
import com.example.ui.theme.GlassLightHighlight
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

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

    val ambientPrimary = MaterialTheme.colorScheme.primary
    val ambientTertiary = MaterialTheme.colorScheme.tertiary
    val ambientSecondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Full-bleed blurred highland backdrop so frosted liquid-glass surfaces catch luminous depth
        Image(
            painter = painterResource(id = R.drawable.img_dumingag_hero),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(radius = 22.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    } else {
                        Modifier
                    }
                )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            GlassBackdropDeepTop,
                            GlassBackdropMid,
                            GlassBackdropBottom
                        )
                    )
                )
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ambientPrimary.copy(alpha = 0.42f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.14f, size.height * 0.24f),
                            radius = size.width * 0.72f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ambientSecondary.copy(alpha = 0.26f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.88f, size.height * 0.44f),
                            radius = size.width * 0.65f
                        )
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                ambientTertiary.copy(alpha = 0.34f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.26f, size.height * 0.78f),
                            radius = size.width * 0.75f
                        )
                    )
                }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Crisp hero header window over the blurred atmospheric canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_dumingag_hero),
                    contentDescription = "Dumingag Valley Rice Terraces and Highlands",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    GlassHeroScrimTop,
                                    GlassHeroScrimBottom
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.92f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = "ZAMBOANGA DEL SUR • MINDANAO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = "Dumingag Travel Guide",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Explore local spots, offline maps & community reviews",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
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
                    val darkMode = isSystemInDarkTheme()
                    val fieldColors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (darkMode) 0.90f else 0.96f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (darkMode) 0.78f else 0.88f),
                        disabledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.60f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.85f),
                        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                        unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                            // Frosted glass segmented Sign In / Create Account pill bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = if (darkMode) 0.82f else 0.88f
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (darkMode) GlassDarkBorderMid else GlassLightHighlight.copy(alpha = 0.75f),
                                        shape = RoundedCornerShape(16.dp)
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (registering) {
                                        "Enter your username, email, and password. You must verify your email before logging in."
                                    } else {
                                        "Sign in with your verified email & password or use Google Sign-In."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    shape = RoundedCornerShape(15.dp),
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
                                shape = RoundedCornerShape(15.dp),
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
                                shape = RoundedCornerShape(15.dp),
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
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.88f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
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
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "Email verification required before your first login.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }

                            if (errorMessage != null) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = errorMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }

                            if (infoMessage != null) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = infoMessage.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
                                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("auth_submit_button")
                            ) {
                                if (pendingMethod == "email") {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(if (registering) "Creating Account…" else "Signing In…")
                                } else {
                                    Text(
                                        text = if (registering) "Create & Verify Account" else "Sign In to Guide",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
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
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                                )
                                Text(
                                    text = "OR",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
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
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (darkMode) 0.90f else 0.96f),
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("google_sign_in_button")
                            ) {
                                if (pendingMethod == "google") {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    GoogleBrandIcon()
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = if (registering) "Sign up with Google" else "Continue with Google",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
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
                                        contentColor = MaterialTheme.colorScheme.primary
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
                        color = Color.White.copy(alpha = 0.94f),
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
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val darkMode = isSystemInDarkTheme()
    val glassFillTop = MaterialTheme.colorScheme.surface.copy(alpha = if (darkMode) 0.88f else 0.91f)
    val glassFillBottom = if (darkMode) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
    }
    val borderBrush = Brush.linearGradient(
        colors = if (darkMode) {
            listOf(GlassDarkHighlight, GlassDarkBorderMid, GlassDarkBorderTail)
        } else {
            listOf(GlassLightHighlight, GlassLightBorderMid, GlassLightBorderTail)
        },
        start = Offset.Zero,
        end = Offset(900f, 1200f)
    )
    val sheenColor = if (darkMode) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.48f)

    Box(
        modifier = modifier
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.30f),
                spotColor = Color.Black.copy(alpha = 0.30f)
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(glassFillTop, glassFillBottom)
                )
            )
            .drawBehind {
                // Soft top specular highlight band characteristic of curved liquid glass
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(sheenColor, Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.24f
                    )
                )
            }
            .border(width = 1.25.dp, brush = borderBrush, shape = shape)
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
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "auth_tab_bg"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 200),
        label = "auth_tab_fg"
    )
    val tabShape = RoundedCornerShape(12.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(
                if (selected) {
                    Modifier.shadow(elevation = 4.dp, shape = tabShape)
                } else {
                    Modifier
                }
            )
            .clip(tabShape)
            .background(containerColor)
            .then(
                if (selected) {
                    Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.28f),
                        shape = tabShape
                    )
                } else {
                    Modifier
                }
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
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
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = "Email verification required",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Text(
                text = "Verify Your Email",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Text(
                text = "We sent a verification link to $email. Open the link in your inbox before accessing your Dumingag account.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (infoMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.30f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = infoMessage,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = onCheckVerification,
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("check_verification_button")
            ) {
                if (pendingMethod == "verify_check") {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking…")
                } else {
                    Text("I've Verified — Continue", fontWeight = FontWeight.Bold)
                }
            }

            OutlinedButton(
                onClick = onResendVerification,
                enabled = !isLoading,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                    contentColor = MaterialTheme.colorScheme.primary
                ),
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
    val darkMode = isSystemInDarkTheme()
    val cardShape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                MaterialTheme.colorScheme.surface.copy(
                    alpha = if (darkMode) 0.84f else 0.90f
                )
            )
            .border(
                width = 1.dp,
                color = if (darkMode) GlassDarkBorderMid else GlassLightHighlight.copy(alpha = 0.72f),
                shape = cardShape
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(42.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
