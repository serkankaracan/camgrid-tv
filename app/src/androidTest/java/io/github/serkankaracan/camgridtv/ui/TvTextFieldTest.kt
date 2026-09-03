package io.github.serkankaracan.camgridtv.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.test.platform.app.InstrumentationRegistry
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import io.github.serkankaracan.camgridtv.R
import io.github.serkankaracan.camgridtv.ui.components.TvTextField
import io.github.serkankaracan.camgridtv.ui.theme.CamGridTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TvTextFieldTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun browseModeMovesBetweenFieldsInsteadOfTrappingDpadInTheCursor() {
        composeRule.setContent {
            CamGridTheme {
                Row(Modifier.fillMaxWidth()) {
                    TvTextField(
                        label = "Username",
                        value = "fixture-user",
                        onValueChange = {},
                        modifier = Modifier.weight(1f).testTag("username"),
                    )
                    TvTextField(
                        label = "Password",
                        value = listOf("masked", "fixture").joinToString("-"),
                        onValueChange = {},
                        password = true,
                        modifier = Modifier.weight(1f).testTag("password"),
                    )
                }
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val password = composeRule.onNodeWithTag("password")
        password.performSemanticsAction(SemanticsActions.RequestFocus)
        password.assertIsFocused()
        password.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(R.string.text_field_browse_state),
            )
        )

        password.performKeyInput { pressKey(Key.DirectionLeft) }

        composeRule.onNodeWithTag("username").assertIsFocused()
    }

    @Test
    fun okEntersEditingAndImeDoneRestoresDpadNavigation() {
        var value by mutableStateOf("")
        composeRule.setContent {
            CamGridTheme {
                Column {
                    TvTextField(
                        label = "Username",
                        value = value,
                        onValueChange = { value = it },
                        modifier = Modifier.testTag("field"),
                    )
                    Button(onClick = {}, modifier = Modifier.testTag("next")) { Text("Next") }
                }
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val field = composeRule.onNodeWithTag("field")
        field.performSemanticsAction(SemanticsActions.RequestFocus)
        field.performKeyInput { pressKey(Key.DirectionCenter) }
        field.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(R.string.text_field_editing_state),
            )
        )
        field.performTextInput("fixture-user")
        composeRule.waitUntil { value == "fixture-user" }

        field.performImeAction()
        field.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.StateDescription,
                context.getString(R.string.text_field_browse_state),
            )
        )
        field.performKeyInput { pressKey(Key.DirectionDown) }

        composeRule.onNodeWithTag("next").assertIsFocused()
        composeRule.runOnIdle { assertEquals("fixture-user", value) }
    }
}
