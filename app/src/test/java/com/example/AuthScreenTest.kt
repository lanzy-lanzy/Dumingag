package com.example

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.ui.screens.AuthScreen
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
    fun failedLoginCanSwitchToRegistrationAndSubmitAgain() {
        val email = "traveler@example.com"
        var successes = 0
        var registrations = 0
        compose.setContent {
            AuthScreen(
                onAuthSuccess = { successes++ },
                signIn = { address, password ->
                    assertEquals(email, address)
                    assertEquals("old password", password)
                    error("Sign-in failed")
                },
                createAccount = { address, password ->
                    assertEquals(email, address)
                    assertEquals("new password", password)
                    registrations++
                }
            )
        }
        compose.onNodeWithText("Sign In").assertIsNotEnabled()
        compose.onNodeWithText("Email").performScrollTo().performTextInput(email)
        compose.onNodeWithText("Password").performScrollTo().performTextInput("old password")
        compose.onNodeWithText("Sign In").performScrollTo().performClick()
        compose.onNodeWithText("Sign-in failed").assertExists()
        compose.runOnIdle { assertEquals(0, successes) }
        compose.onNodeWithText("New here? Create Account").performScrollTo().performClick()
        compose.onNodeWithText("Create Account").assertIsNotEnabled()
        compose.onNodeWithText("Password").performScrollTo().performTextInput("new password")
        compose.onNodeWithText("Create Account").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, successes)
            assertEquals(1, registrations)
        }
    }
}
