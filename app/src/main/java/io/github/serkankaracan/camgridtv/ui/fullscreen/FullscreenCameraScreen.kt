package io.github.serkankaracan.camgridtv.ui.fullscreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(CamGridPalette.BackgroundBottom)
                .testTag(UiTestTags.FullscreenScreen)
    ) {
        videoSurface(state.cameraId, Modifier.fillMaxSize())

        Box(
            modifier =
                Modifier.align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(128.dp)
                    .background(
                        Brush.verticalGradient(listOf(CamGridPalette.Scrim, Color.Transparent))
                    )
        )
        Box(
            modifier =
                Modifier.align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color.Transparent, CamGridPalette.Scrim))
                    )
        )

        Column(
            modifier =
                Modifier.align(Alignment.TopStart)
                    .padding(
                        horizontal = CamGridDimens.SafeHorizontal,
                        vertical = CamGridDimens.SafeVertical,
                    )
        ) {
            Text(
                text = state.displayName,
                color = CamGridPalette.TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.fullscreen_back_hint),
                modifier = Modifier.padding(top = 3.dp),
                color = CamGridPalette.TextMuted,
                fontSize = 13.sp,
            )
        }
        Row(
            modifier =
                Modifier.align(Alignment.BottomStart)
                    .padding(
                        horizontal = CamGridDimens.SafeHorizontal,
                        vertical = CamGridDimens.SafeVertical,
                    ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaybackStatusOverlay(
                cameraId = state.cameraId,
                state = state.playbackState,
                announceChanges = true,
            )
        }
    }
}
