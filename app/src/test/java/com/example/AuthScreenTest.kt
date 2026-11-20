package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.DumingagTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w400dp-h800dp")
class AuthScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun failedLoginCanSwitchToRegistrationRequireUsernameAndVerifyEmail() {
        val email = "traveler@example.com"
        var successes = 0
        var registrations = 0
        var verificationChecks = 0

        compose.setContent {
            DumingagTheme {
                AuthScreen(
                    onAuthSuccess = { successes++ },
                    signIn = { address, password ->
                        assertEquals(email, address)
                        assertEquals("old password", password)
                        error("Sign-in failed")
                    },
                    createAccount = { username, address, password ->
                        assertEquals("JuanTraveler", username)
                        assertEquals(email, address)
                        assertEquals("new password", password)
                        registrations++
                        false // Requires email verification before entering
                    },
                    onCheckVerification = {
                        verificationChecks++
                        true
                    }
                )
            }
        }

        compose.onNodeWithTag("auth_submit_button").assertIsNotEnabled()
        compose.onNodeWithTag("auth_email_input").performScrollTo().performTextInput(email)
        compose.onNodeWithTag("auth_password_input").performScrollTo().performTextInput("old password")
        compose.onNodeWithTag("auth_submit_button").performScrollTo().performClick()
        compose.onNodeWithText("Sign-in failed").assertExists()
        compose.runOnIdle { assertEquals(0, successes) }

        compose.onNodeWithTag("auth_tab_create_account").performScrollTo().performClick()
        compose.onNodeWithText("Username").assertExists()
        compose.onNodeWithText("Desired Username").assertDoesNotExist()
        compose.onNodeWithTag("auth_submit_button").assertIsNotEnabled()
        compose.onNodeWithTag("auth_username_input").performScrollTo().performTextInput("JuanTraveler")
        compose.onNodeWithTag("auth_submit_button").assertIsNotEnabled()
        compose.onNodeWithTag("auth_password_input").performScrollTo().performTextInput("new password")
        compose.onNodeWithTag("auth_submit_button").performScrollTo().performClick()

        compose.onNodeWithTag("email_verification_card").assertIsDisplayed()
        compose.onNodeWithText("Verify Your Email").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, registrations)
            assertEquals(0, successes)
        }

        compose.onNodeWithTag("check_verification_button").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, verificationChecks)
            assertEquals(1, successes)
        }
    }

    @Test
    fun unverifiedSignInBlocksEntryAndAllowsResend() {
        var resends = 0
        var successes = 0

        compose.setContent {
            DumingagTheme {
                AuthScreen(
                    onAuthSuccess = { successes++ },
                    signIn = { _, _ -> false },
                    onResendVerification = { resends++ },
                    onCheckVerification = { false }
                )
            }
        }

        compose.onNodeWithTag("auth_email_input").performScrollTo().performTextInput("pending@example.com")
        compose.onNodeWithTag("auth_password_input").performScrollTo().performTextInput("secret123")
        compose.onNodeWithTag("auth_submit_button").performScrollTo().performClick()

        compose.onNodeWithTag("email_verification_card").assertIsDisplayed()
        compose.onNodeWithTag("resend_verification_button").performScrollTo().performClick()
        compose.onNodeWithTag("check_verification_button").performScrollTo().performClick()

        compose.runOnIdle {
            assertEquals(1, resends)
            assertEquals(0, successes)
        }
    }
}
