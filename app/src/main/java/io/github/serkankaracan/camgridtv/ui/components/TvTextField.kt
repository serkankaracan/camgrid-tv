package io.github.serkankaracan.camgridtv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreInterceptKeyBeforeSoftKeyboard
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.serkankaracan.camgridtv.R
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * A TV-first text field with separate browse and edit modes. D-pad focus only highlights the field.
 * OK enters edit mode and opens the keyboard; Back or the IME Done action returns to browse mode so
 * directional keys navigate between controls instead of moving a hidden text cursor.
 */
@Composable
fun TvTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    enabled: Boolean = true,
    maxLength: Int? = null,
) {
    var focused by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var consumingBackKey by remember { mutableStateOf(false) }
    val fieldState = rememberTextFieldState(initialText = value)
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val browseStateDescription = stringResource(R.string.text_field_browse_state)
    val editingStateDescription = stringResource(R.string.text_field_editing_state)
    val editActionLabel = stringResource(R.string.text_field_edit_action)
    val shape = RoundedCornerShape(10.dp)
    val borderColor =
        when {
            !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
            focused -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f)
        }
    val containerColor =
        if (focused) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val finishEditing = {
        editing = false
        keyboardController?.hide()
    }
    val fieldModifier =
        modifier
            .onPreInterceptKeyBeforeSoftKeyboard { event ->
                if (event.key == Key.Back && (editing || consumingBackKey)) {
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            consumingBackKey = true
                            finishEditing()
                        }
                        KeyEventType.KeyUp -> consumingBackKey = false
                    }
                    true
                } else {
                    false
                }
            }
            .onPreviewKeyEvent { event ->
                when {
                    !editing &&
                        event.type == KeyEventType.KeyDown &&
                        event.key.isTextFieldActivationKey() -> {
                        editing = true
                        true
                    }
                    !editing &&
                        event.type == KeyEventType.KeyDown &&
                        event.key.asFocusDirection() != null -> {
                        focusManager.moveFocus(checkNotNull(event.key.asFocusDirection()))
                        true
                    }
                    editing && event.type == KeyEventType.KeyDown && event.key == Key.Back -> {
                        finishEditing()
                        true
                    }
                    else -> false
                }
            }
            .onFocusChanged { focusState ->
                focused = focusState.isFocused
                if (!focusState.isFocused && editing) finishEditing()
            }
            .semantics {
                contentDescription = label
                stateDescription = if (editing) editingStateDescription else browseStateDescription
                onClick(label = editActionLabel) {
                    if (enabled) {
                        editing = true
                        true
                    } else {
                        false
                    }
                }
                if (password) password()
            }
            .background(containerColor, shape)
            .border(if (focused) 3.dp else 1.dp, borderColor, shape)
            .padding(horizontal = 16.dp, vertical = 11.dp)
    val keyboardOptions =
        KeyboardOptions(
            keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
            imeAction = ImeAction.Done,
            showKeyboardOnFocus = editing,
        )
    val inputTransformation = maxLength?.let(InputTransformation::maxLength)
    val decorator = TextFieldDecorator { innerTextField ->
        Column {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                fontSize = 13.sp,
            )
            innerTextField()
        }
    }

    LaunchedEffect(value) {
        if (fieldState.text.toString() != value) {
            fieldState.setTextAndPlaceCursorAtEnd(value)
        }
    }
    LaunchedEffect(fieldState) {
        snapshotFlow { fieldState.text.toString() }
            .distinctUntilChanged()
            .collect { text ->
                if (text != currentValue) currentOnValueChange(text)
            }
    }
    LaunchedEffect(editing, focused, enabled) {
        if (!enabled && editing) {
            finishEditing()
        } else if (editing && focused) {
            // Let readOnly/keyboardOptions recompose before asking Android to show the IME.
            withFrameNanos {}
            keyboardController?.show()
        }
    }

    BackHandler(enabled = editing && focused) { finishEditing() }

    if (password) {
        BasicSecureTextField(
            state = fieldState,
            modifier = fieldModifier,
            enabled = enabled,
            readOnly = !editing,
            inputTransformation = inputTransformation,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 19.sp),
            keyboardOptions = keyboardOptions,
            onKeyboardAction = { finishEditing() },
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textObfuscationMode = TextObfuscationMode.Hidden,
            decorator = decorator,
        )
    } else {
        BasicTextField(
            state = fieldState,
            modifier = fieldModifier,
            enabled = enabled,
            readOnly = !editing,
            inputTransformation = inputTransformation,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 19.sp),
            keyboardOptions = keyboardOptions,
            onKeyboardAction = { finishEditing() },
            lineLimits = TextFieldLineLimits.SingleLine,
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorator = decorator,
        )
    }
}

private fun Key.isTextFieldActivationKey(): Boolean =
    this == Key.DirectionCenter || this == Key.Enter || this == Key.NumPadEnter

private fun Key.asFocusDirection(): FocusDirection? =
    when (this) {
        Key.DirectionLeft -> FocusDirection.Left
        Key.DirectionRight -> FocusDirection.Right
        Key.DirectionUp -> FocusDirection.Up
        Key.DirectionDown -> FocusDirection.Down
        else -> null
    }
