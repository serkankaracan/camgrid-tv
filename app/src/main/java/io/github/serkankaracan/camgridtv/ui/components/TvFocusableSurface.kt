package io.github.serkankaracan.camgridtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme

@Composable
fun TvFocusableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    requestInitialFocus: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val shape = RoundedCornerShape(12.dp)
    val borderColor =
        when {
            focused -> MaterialTheme.colorScheme.primary
            selected -> MaterialTheme.colorScheme.secondary
            else -> Color.Transparent
        }

    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) focusRequester.requestFocus()
    }

    Box(
        modifier =
            modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .semantics {
                    role = Role.Button
                    this.selected = selected
                }
                .clickable(enabled = enabled, onClick = onClick)
                .background(MaterialTheme.colorScheme.surface, shape)
                .border(BorderStroke(if (focused) 4.dp else 2.dp, borderColor), shape)
                .padding(12.dp),
        content = content,
    )
}
