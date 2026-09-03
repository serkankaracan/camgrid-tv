package io.github.serkankaracan.camgridtv

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun primaryActionStartsFocusedAndRespondsToActivation() {
        val action = composeRule.onNodeWithTag("primary_scan_action")

        action.assertIsFocused()
        action.performClick()
        action.assertTextContains("…", substring = true)
    }
}
