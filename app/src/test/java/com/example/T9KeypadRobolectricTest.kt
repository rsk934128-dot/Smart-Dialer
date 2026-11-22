package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.ui.components.T9KeypadInterface
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class T9KeypadRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun t9KeypadDisplaysAlphabetKeysAndTriggersDigitClicks() {
        var typed = ""

        composeTestRule.setContent {
            MyApplicationTheme {
                T9KeypadInterface(
                    inputQuery = typed,
                    matchCount = 0,
                    onDigitClick = { digit -> typed += digit },
                    onLongDigitClick = {},
                    onCallClick = {},
                    onBackspaceClick = {},
                    onClearAllClick = {},
                    onAddContactClick = {},
                    onToggleKeypad = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("t9_keypad_interface").assertIsDisplayed()

        // Verify digits 2..9 and their T9 alphabet mappings are displayed
        listOf(
            '2' to "A B C",
            '3' to "D E F",
            '4' to "G H I",
            '5' to "J K L",
            '6' to "M N O",
            '7' to "P Q R S",
            '8' to "T U V",
            '9' to "W X Y Z"
        ).forEach { (digit, letters) ->
            composeTestRule.onNodeWithTag("keypad_key_$digit").assertIsDisplayed()
            composeTestRule.onNodeWithText(letters).assertIsDisplayed()
        }

        // Tap digits 3, 2, 7 ("FAR")
        composeTestRule.onNodeWithTag("keypad_key_3").performClick()
        composeTestRule.onNodeWithTag("keypad_key_2").performClick()
        composeTestRule.onNodeWithTag("keypad_key_7").performClick()
        composeTestRule.waitForIdle()

        assertEquals("327", typed)
    }

    @Test
    fun t9KeypadShowsActiveMappingStripWhenQueryPresent() {
        composeTestRule.setContent {
            MyApplicationTheme {
                T9KeypadInterface(
                    inputQuery = "327",
                    matchCount = 2,
                    onDigitClick = {},
                    onLongDigitClick = {},
                    onCallClick = {},
                    onBackspaceClick = {},
                    onClearAllClick = {},
                    onAddContactClick = {},
                    onToggleKeypad = {}
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("t9_mapping_strip").assertIsDisplayed()
        composeTestRule.onNodeWithText("DEF").assertIsDisplayed()
        composeTestRule.onNodeWithText("ABC").assertIsDisplayed()
        composeTestRule.onNodeWithText("PQRS").assertIsDisplayed()
        composeTestRule.onNodeWithText("2 matches").assertIsDisplayed()
    }
}
