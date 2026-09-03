package io.github.serkankaracan.camgridtv.ui.fullscreen

import io.github.serkankaracan.camgridtv.playback.PlaybackState

data class FullscreenUiState(
    val cameraId: String,
    val displayName: String,
    val playbackState: PlaybackState,
    val viewMode: FullscreenViewMode = FullscreenViewMode.SAFE,
)

sealed interface FullscreenUiAction {
    data object BackToWall : FullscreenUiAction

    data object PreviousViewMode : FullscreenUiAction

    data object NextViewMode : FullscreenUiAction
}

/** Aspect-preserving viewport choices for TVs that apply physical overscan. */
enum class FullscreenViewMode(
    val viewportFraction: Float,
    val cropsToFill: Boolean,
) {
    /** Keeps the complete source inside a centered 90% viewport. */
    SAFE(viewportFraction = 0.90f, cropsToFill = false),

    /** Fits the complete source into the full logical Android viewport. */
    FIT(viewportFraction = 1f, cropsToFill = false),

    /** Fills the logical viewport by cropping edges, never by stretching. */
    FILL(viewportFraction = 1f, cropsToFill = true);

    fun next(): FullscreenViewMode = entries[(ordinal + 1) % entries.size]

    fun previous(): FullscreenViewMode = entries[(ordinal - 1 + entries.size) % entries.size]
}
