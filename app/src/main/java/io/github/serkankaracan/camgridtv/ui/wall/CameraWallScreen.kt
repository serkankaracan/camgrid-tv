package io.github.serkankaracan.camgridtv.ui.wall

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import io.github.serkankaracan.camgridtv.R
import io.github.serkankaracan.camgridtv.playback.PlaybackState
import io.github.serkankaracan.camgridtv.ui.components.CamGridBackground
import io.github.serkankaracan.camgridtv.ui.components.CameraVideoSurface
import io.github.serkankaracan.camgridtv.ui.components.EmptyCameraVideoSurface
import io.github.serkankaracan.camgridtv.ui.components.PlaybackStatusOverlay
import io.github.serkankaracan.camgridtv.ui.components.StatusPill
import io.github.serkankaracan.camgridtv.ui.components.TvFocusableSurface
import io.github.serkankaracan.camgridtv.ui.components.UiTestTags
import io.github.serkankaracan.camgridtv.ui.theme.CamGridDimens
import io.github.serkankaracan.camgridtv.ui.theme.CamGridPalette
import kotlin.math.roundToInt

@Composable
fun CameraWallScreen(
    state: CameraWallUiState,
    onAction: (CameraWallUiAction) -> Unit,
    modifier: Modifier = Modifier,
    videoSurface: CameraVideoSurface = { _, surfaceModifier ->
        EmptyCameraVideoSurface(surfaceModifier)
    },
) {
    BackHandler { onAction(CameraWallUiAction.BackToCameraSetup) }
    val grid = GridLayoutCalculator.calculate(state.cameras.size)
    val initialFocusCameraId =
        state.restoreFocusCameraId?.takeIf { candidateId ->
            state.cameras.any { camera -> camera.id == candidateId }
        } ?: state.cameras.firstOrNull()?.id
    val liveCount = state.cameras.count { it.playbackState == PlaybackState.Live }
    val showRescan =
        state.cameras.any { camera ->
            camera.playbackState == PlaybackState.Offline ||
                camera.playbackState is PlaybackState.Retrying ||
                camera.playbackState is PlaybackState.PlaybackFailed
        }

    CamGridBackground(modifier = modifier.fillMaxSize().testTag(UiTestTags.WallScreen)) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .padding(
                        horizontal = CamGridDimens.SafeHorizontal,
                        vertical = CamGridDimens.SafeVertical,
                    )
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth().padding(start = 3.dp, end = 3.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.live_wall_title),
                        color = CamGridPalette.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(R.string.wall_back_hint),
                        color = CamGridPalette.TextMuted,
                        fontSize = 13.sp,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StatusPill(
                        text =
                            pluralStringResource(
                                R.plurals.live_camera_count,
                                liveCount,
                                liveCount,
                            ),
                        color =
                            if (liveCount > 0) {
                                CamGridPalette.Success
                            } else {
                                CamGridPalette.TextMuted
                            },
                    )
                    if (showRescan) {
                        Button(
                            onClick = { onAction(CameraWallUiAction.RescanCameras) },
                            modifier = Modifier.testTag(UiTestTags.WallRescanAction),
                        ) {
                            Text(stringResource(R.string.scan_again))
                        }
                    }
                }
            }

            CameraGrid(
                gridLayout = grid,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                state.cameras.forEach { camera ->
                    CameraWallTile(
                        camera = camera,
                        requestInitialFocus = camera.id == initialFocusCameraId,
                        onClick = { onAction(CameraWallUiAction.OpenFullscreen(camera.id)) },
                        videoSurface = videoSurface,
                        modifier = Modifier.padding(5.dp).testTag(UiTestTags.wallCamera(camera.id)),
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraWallTile(
    camera: WallCameraUiModel,
    requestInitialFocus: Boolean,
    onClick: () -> Unit,
    videoSurface: CameraVideoSurface,
    modifier: Modifier = Modifier,
) {
    TvFocusableSurface(
        onClick = onClick,
        requestInitialFocus = requestInitialFocus,
        contentPadding = PaddingValues(3.dp),
        shape = RoundedCornerShape(10.dp),
        scaleOnFocus = false,
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier.fillMaxSize()
                    .background(CamGridPalette.BackgroundBottom, RoundedCornerShape(7.dp))
        ) {
            videoSurface(camera.id, Modifier.fillMaxSize())
            Row(
                modifier =
                    Modifier.align(Alignment.TopStart)
                        .fillMaxWidth()
                        .background(CamGridPalette.Scrim)
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = camera.displayName,
                    modifier = Modifier.weight(1f),
                    color = CamGridPalette.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (camera.playbackState == PlaybackState.Live) {
                    Text(
                        text = stringResource(R.string.live),
                        modifier =
                            Modifier.padding(start = 8.dp)
                                .background(
                                    CamGridPalette.Success.copy(alpha = 0.18f),
                                    RoundedCornerShape(50),
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        color = CamGridPalette.Success,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )
                }
            }
            PlaybackStatusOverlay(
                cameraId = camera.id,
                state = camera.playbackState,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
            )
        }
    }
}

@Composable
private fun CameraGrid(
    gridLayout: GridLayout,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val height =
            if (constraints.hasBoundedHeight) constraints.maxHeight else constraints.minHeight
        val placeables = measurables.mapIndexed { index, measurable ->
            val placement = gridLayout.placements[index]
            measurable.measure(
                androidx.compose.ui.unit.Constraints.fixed(
                    width = (width * placement.widthFraction).roundToInt().coerceAtLeast(1),
                    height = (height * placement.heightFraction).roundToInt().coerceAtLeast(1),
                )
            )
        }
        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val placement = gridLayout.placements[index]
                placeable.placeRelative(
                    x = (width * placement.leftFraction).roundToInt(),
                    y = (height * placement.topFraction).roundToInt(),
                )
            }
        }
    }
}
