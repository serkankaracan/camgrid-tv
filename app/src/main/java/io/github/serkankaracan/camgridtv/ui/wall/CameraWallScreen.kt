package io.github.serkankaracan.camgridtv.ui.wall

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.serkankaracan.camgridtv.ui.components.CameraVideoSurface
import io.github.serkankaracan.camgridtv.ui.components.EmptyCameraVideoSurface
import io.github.serkankaracan.camgridtv.ui.components.PlaybackStatusOverlay
import io.github.serkankaracan.camgridtv.ui.components.TvFocusableSurface
import io.github.serkankaracan.camgridtv.ui.components.UiTestTags
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
    Surface(modifier = modifier.fillMaxSize().testTag(UiTestTags.WallScreen)) {
        val grid = GridLayoutCalculator.calculate(state.cameras.size)
        val initialFocusCameraId =
            state.restoreFocusCameraId?.takeIf { candidateId ->
                state.cameras.any { camera -> camera.id == candidateId }
            } ?: state.cameras.firstOrNull()?.id
        CameraGrid(gridLayout = grid, modifier = Modifier.fillMaxSize()) {
            state.cameras.forEach { camera ->
                CameraWallTile(
                    camera = camera,
                    requestInitialFocus = camera.id == initialFocusCameraId,
                    onClick = {
                        onAction(CameraWallUiAction.OpenFullscreen(camera.id))
                    },
                    videoSurface = videoSurface,
                    modifier = Modifier.padding(6.dp).testTag(UiTestTags.wallCamera(camera.id)),
                )
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
        modifier = modifier,
    ) {
        videoSurface(camera.id, Modifier.fillMaxSize())
        Text(
            text = camera.displayName,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            fontSize = 17.sp,
        )
        PlaybackStatusOverlay(
            cameraId = camera.id,
            state = camera.playbackState,
            modifier = Modifier.align(Alignment.BottomStart),
        )
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
