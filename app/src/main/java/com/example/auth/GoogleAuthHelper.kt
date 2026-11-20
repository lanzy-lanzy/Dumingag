package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import android.content.ContextWrapper
import kotlinx.coroutines.CancellationException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object GoogleAuthHelper {
    private const val TAG = "Auth"

    private fun friendlyAuthError(e: FirebaseException): IllegalStateException {
        val code = (e as? FirebaseAuthException)?.errorCode.orEmpty()
        val raw = e.localizedMessage.orEmpty()
        val message = when {
            code == "ERROR_OPERATION_NOT_ALLOWED" ||
                raw.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ||
                raw.contains("sign-in provider is disabled", ignoreCase = true) ->
                "Email/Password sign-up is disabled on Firebase project \"named-trilogy-466605-u4\". Enable Authentication → Sign-in method → Email/Password in Firebase Console, or tap Google Sign-In below."
            code == "ERROR_EMAIL_ALREADY_IN_USE" ->
                "An account with this email already exists. Switch to Sign In or use Google."
            code == "ERROR_WEAK_PASSWORD" ->
                "Password must be at least 6 characters long."
            code == "ERROR_INVALID_EMAIL" ->
                "Please enter a valid email address."
            code in setOf("ERROR_INVALID_CREDENTIAL", "ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND") ->
                "Incorrect email or password. Please check your details and try again."
            code == "ERROR_TOO_MANY_REQUESTS" ->
                "Too many attempts right now. Please wait a moment and try again."
            else -> raw.ifBlank { "Authentication failed. Please try again." }
        }
        return IllegalStateException(message, e)
    }

    suspend fun createAccount(username: String, email: String, password: String): Boolean {
        val cleanUsername = username.trim().take(80)
        val cleanEmail = email.trim()
        require(cleanUsername.length >= 2) { "Please enter a username (at least 2 characters)." }
        require(cleanEmail.isNotEmpty()) { "Please enter your email address." }
        require(password.length >= 6) { "Password must be at least 6 characters long." }
        return try {
            val result = Firebase.auth.createUserWithEmailAndPassword(cleanEmail, password).await()
            val user = result.user ?: throw IllegalStateException("Unable to create account. Please try again.")
            try {
                val profileUpdate = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = cleanUsername
                }
                user.updateProfile(profileUpdate).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Profile displayName update skipped: ${e.message}")
            }
            user.sendEmailVerification().await()
            user.isEmailVerified
        } catch (e: FirebaseException) {
            throw friendlyAuthError(e)
        }
    }

    suspend fun signIn(email: String, password: String): Boolean {
        val cleanEmail = email.trim()
        require(cleanEmail.isNotEmpty()) { "Please enter your email address." }
        require(password.isNotEmpty()) { "Please enter your password." }
        return try {
            val result = Firebase.auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val user = result.user ?: Firebase.auth.currentUser
            if (user != null && !user.isEmailVerified) {
                user.reload().await()
            }
            val refreshed = Firebase.auth.currentUser
            if (refreshed?.isEmailVerified == true) {
                refreshed.getIdToken(true).await()
                true
            } else {
                false
            }
        } catch (e: FirebaseException) {
            throw friendlyAuthError(e)
        }
    }

    suspend fun resendVerificationEmail() {
        val user = Firebase.auth.currentUser
            ?: throw IllegalStateException("Please sign in first to resend the verification email.")
        try {
            user.sendEmailVerification().await()
        } catch (e: FirebaseException) {
            throw friendlyAuthError(e)
        }
    }

    suspend fun refreshEmailVerificationStatus(): Boolean {
        val user = Firebase.auth.currentUser ?: return false
        return try {
            user.reload().await()
            val refreshed = Firebase.auth.currentUser ?: return false
            if (refreshed.isEmailVerified) {
                refreshed.getIdToken(true).await()
                true
            } else {
                false
            }
        } catch (e: FirebaseException) {
            throw friendlyAuthError(e)
        }
    }

    private tailrec fun Context.activity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.activity()
        else -> null
    }

    fun onGoogleSignInClicked(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onAuthError: (String) -> Unit,
        scope: CoroutineScope,
        onAuthCancelled: () -> Unit = {}
    ) {
        val resourceId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        val clientId = if (resourceId != 0) context.getString(resourceId) else ""
        if (clientId.isBlank()) {
            onAuthError("Google Sign-In is missing its web client ID.")
            return
        }
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
        scope.launch {
            try {
                val activity = context.activity()
                if (activity == null) {
                    onAuthError("Activity context required for Google Sign-In")
                    return@launch
                }
                val cred = credentialManager.getCredential(activity, request).credential
                if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                    Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                    onAuthSuccess()
                } else {
                    onAuthError("Unexpected credential type")
                }
            } catch (e: GetCredentialCancellationException) {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                if (e.message.orEmpty().contains("reauth failed", ignoreCase = true)) {
                    onAuthError("Google could not verify the selected account. Try again or choose another Google account.")
                } else {
                    onAuthCancelled()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Sign in failed")
            }
        }
    }

    fun signOut(
        credentialManager: CredentialManager,
        scope: CoroutineScope,
        onSignOutComplete: () -> Unit = {}
    ) {
        scope.launch {
            withContext(NonCancellable) {
                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to clear credential state: ${e.message}")
                } finally {
                    Firebase.auth.signOut()
                    onSignOutComplete()
                }
            }
        }
    }
}
