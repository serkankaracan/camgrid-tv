package io.github.serkankaracan.camgridtv.ui.fullscreen

import io.github.serkankaracan.camgridtv.playback.PlaybackState

data class FullscreenUiState(
    val cameraId: String,
    val displayName: String,
    val playbackState: PlaybackState,
)

sealed interface FullscreenUiAction {
    data object BackToWall : FullscreenUiAction
}
