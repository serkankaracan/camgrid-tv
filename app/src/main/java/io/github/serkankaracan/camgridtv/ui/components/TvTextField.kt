package io.github.serkankaracan.camgridtv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

@Composable
fun TvTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    enabled: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    val borderColor =
        if (focused) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged { focused = it.isFocused }
                .semantics { contentDescription = label }
                .background(MaterialTheme.colorScheme.surface, shape)
                .border(2.dp, borderColor, shape)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        enabled = enabled,
        singleLine = true,
        textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions =
            KeyboardOptions(
                keyboardType = if (password) KeyboardType.Password else KeyboardType.Text
            ),
        visualTransformation =
            if (password) PasswordVisualTransformation() else VisualTransformation.None,
        decorationBox = { innerTextField ->
            Column {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                )
                innerTextField()
            }
        },
    )
}
