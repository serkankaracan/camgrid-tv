package io.github.serkankaracan.camgridtv.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import io.github.serkankaracan.camgridtv.ui.theme.CamGridPalette

@Composable
fun TvFocusableSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean? = null,
    enabled: Boolean = true,
    requestInitialFocus: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    shape: Shape = RoundedCornerShape(12.dp),
    scaleOnFocus: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val borderColor =
        when {
            focused -> CamGridPalette.Primary
            selected == true -> CamGridPalette.Selection.copy(alpha = 0.82f)
            else -> CamGridPalette.Outline.copy(alpha = 0.62f)
        }
    val scale by
        animateFloatAsState(
            targetValue = if (focused && scaleOnFocus) 1.018f else 1f,
            animationSpec = tween(durationMillis = 120),
            label = "tv-focus-scale",
        )
    val containerColor =
        when {
            focused -> CamGridPalette.FocusedSurface
            else -> MaterialTheme.colorScheme.surface
        }
    val visualTransform =
        if (scaleOnFocus || !enabled) {
            Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            }
        } else {
            Modifier
        }

    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) focusRequester.requestFocus()
    }

    Box(
        modifier =
            modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused }
                .then(visualTransform)
                .semantics {
                    role = Role.Button
                    selected?.let { this.selected = it }
                }
                .clickable(enabled = enabled, onClick = onClick)
                .background(containerColor, shape)
                .border(
                    BorderStroke(if (focused) 3.dp else 1.dp, borderColor),
                    shape,
                )
                .padding(contentPadding),
        content = content,
    )
}
