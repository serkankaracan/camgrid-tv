package io.github.serkankaracan.camgridtv

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.github.serkankaracan.camgridtv.ui.components.UiTestTags
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun activityHostsTheApplicationFlow() {
        composeRule.onNodeWithTag(UiTestTags.AppRoot).assertExists()
    }
}
