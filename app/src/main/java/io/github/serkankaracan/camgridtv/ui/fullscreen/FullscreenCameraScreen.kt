package io.github.serkankaracan.camgridtv.ui.fullscreen

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import io.github.serkankaracan.camgridtv.R
import io.github.serkankaracan.camgridtv.ui.components.CameraVideoSurface
import io.github.serkankaracan.camgridtv.ui.components.EmptyCameraVideoSurface
import io.github.serkankaracan.camgridtv.ui.components.PlaybackStatusOverlay
import io.github.serkankaracan.camgridtv.ui.components.UiTestTags
import io.github.serkankaracan.camgridtv.ui.theme.CamGridDimens
import io.github.serkankaracan.camgridtv.ui.theme.CamGridPalette

@Composable
fun FullscreenCameraScreen(
    state: FullscreenUiState,
    onAction: (FullscreenUiAction) -> Unit,
    modifier: Modifier = Modifier,
    videoSurface: CameraVideoSurface = { _, surfaceModifier ->
        EmptyCameraVideoSurface(surfaceModifier)
    },
) {
    BackHandler { onAction(FullscreenUiAction.BackToWall) }
    val viewModeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(state.cameraId) { viewModeFocusRequester.requestFocus() }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(CamGridPalette.BackgroundBottom)
                .testTag(UiTestTags.FullscreenScreen)
    ) {
        videoSurface(
            state.cameraId,
            Modifier.align(Alignment.Center).fillMaxSize(state.viewMode.viewportFraction),
        )

        Column(
            modifier =
                Modifier.align(Alignment.TopEnd)
                    .padding(
                        horizontal = CamGridDimens.SafeHorizontal,
                        vertical = CamGridDimens.SafeVertical,
                    )
                    .widthIn(min = 220.dp, max = 360.dp)
                    .background(CamGridPalette.Scrim, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .testTag(UiTestTags.FullscreenMetadata),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = state.displayName,
                modifier = Modifier.fillMaxWidth(),
                color = CamGridPalette.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            PlaybackStatusOverlay(
                cameraId = state.cameraId,
                state = state.playbackState,
                modifier = Modifier.padding(top = 8.dp),
                announceChanges = true,
            )
            Button(
                onClick = { onAction(FullscreenUiAction.NextViewMode) },
                modifier =
                    Modifier.padding(top = 12.dp)
                        .focusRequester(viewModeFocusRequester)
                        .onPreviewKeyEvent { event ->
                            when (event.key) {
                                Key.DirectionLeft -> {
                                    if (event.type == KeyEventType.KeyDown) {
                                        onAction(FullscreenUiAction.PreviousViewMode)
                                    }
                                    true
                                }
                                Key.DirectionRight -> {
                                    if (event.type == KeyEventType.KeyDown) {
                                        onAction(FullscreenUiAction.NextViewMode)
                                    }
                                    true
                                }
                                else -> false
                            }
                        }
                        .testTag(UiTestTags.FullscreenViewModeAction),
            ) {
                Text(stringResource(state.viewMode.labelRes()))
            }
        }
    }
}

@StringRes
private fun FullscreenViewMode.labelRes(): Int =
    when (this) {
        FullscreenViewMode.SAFE -> R.string.fullscreen_view_mode_safe
        FullscreenViewMode.FIT -> R.string.fullscreen_view_mode_fit
        FullscreenViewMode.FILL -> R.string.fullscreen_view_mode_fill
    }
