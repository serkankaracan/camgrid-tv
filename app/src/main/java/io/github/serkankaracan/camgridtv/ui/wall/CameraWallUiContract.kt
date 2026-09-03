package io.github.serkankaracan.camgridtv.ui.wall

import io.github.serkankaracan.camgridtv.playback.PlaybackState

data class WallCameraUiModel(
    val id: String,
    val displayName: String,
    val playbackState: PlaybackState = PlaybackState.Idle,
)

data class CameraWallUiState(
    val cameras: List<WallCameraUiModel>,
    val restoreFocusCameraId: String? = null,
)

sealed interface CameraWallUiAction {
    data class OpenFullscreen(val cameraId: String) : CameraWallUiAction

    data object RescanCameras : CameraWallUiAction

    data object BackToCameraSetup : CameraWallUiAction
}
