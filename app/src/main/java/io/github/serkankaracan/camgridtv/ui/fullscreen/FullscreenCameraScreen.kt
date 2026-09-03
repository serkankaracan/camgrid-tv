package io.github.serkankaracan.camgridtv.ui.fullscreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.serkankaracan.camgridtv.ui.components.CameraVideoSurface
import io.github.serkankaracan.camgridtv.ui.components.EmptyCameraVideoSurface
import io.github.serkankaracan.camgridtv.ui.components.PlaybackStatusOverlay
import io.github.serkankaracan.camgridtv.ui.components.UiTestTags

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
    Surface(modifier = modifier.fillMaxSize().testTag(UiTestTags.FullscreenScreen)) {
        Box(modifier = Modifier.fillMaxSize()) {
            videoSurface(state.cameraId, Modifier.fillMaxSize())
            Text(
                text = state.displayName,
                modifier =
                    Modifier.align(Alignment.TopStart)
                        .padding(
                            horizontal = TV_SAFE_HORIZONTAL_PADDING,
                            vertical = TV_SAFE_VERTICAL_PADDING,
                        ),
                fontSize = 22.sp,
            )
            PlaybackStatusOverlay(
                cameraId = state.cameraId,
                state = state.playbackState,
                modifier =
                    Modifier.align(Alignment.BottomStart)
                        .padding(
                            horizontal = TV_SAFE_HORIZONTAL_PADDING,
                            vertical = TV_SAFE_VERTICAL_PADDING,
                        ),
            )
        }
    }
}

private val TV_SAFE_HORIZONTAL_PADDING = 48.dp
private val TV_SAFE_VERTICAL_PADDING = 27.dp
