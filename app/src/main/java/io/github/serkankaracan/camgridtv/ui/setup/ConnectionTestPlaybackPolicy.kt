package io.github.serkankaracan.camgridtv.ui.setup

import io.github.serkankaracan.camgridtv.playback.PlaybackState

internal enum class ConnectionTestPlaybackAction {
    ContinueTesting,
    HoldPreview,
    ReleasePreview,
}

internal data class ConnectionTestPlaybackDecision(
    val connectionState: ConnectionTestUiState?,
    val action: ConnectionTestPlaybackAction,
)

/** Pure mapping from credential-free playback states to setup-preview ownership. */
internal object ConnectionTestPlaybackPolicy {
    fun duringTest(playbackState: PlaybackState?): ConnectionTestPlaybackDecision =
        when (playbackState) {
            null,
            PlaybackState.Idle ->
                ConnectionTestPlaybackDecision(
                    connectionState = null,
                    action = ConnectionTestPlaybackAction.ContinueTesting,
                )
            PlaybackState.Connecting,
            is PlaybackState.Retrying ->
                ConnectionTestPlaybackDecision(
                    connectionState = ConnectionTestUiState.Testing,
                    action = ConnectionTestPlaybackAction.ContinueTesting,
                )
            PlaybackState.Live ->
                ConnectionTestPlaybackDecision(
                    connectionState = ConnectionTestUiState.Connected,
                    action = ConnectionTestPlaybackAction.HoldPreview,
                )
            PlaybackState.AuthenticationFailed ->
                release(ConnectionTestUiState.AuthenticationFailed)
            PlaybackState.Offline -> release(ConnectionTestUiState.Offline)
            PlaybackState.UnsupportedStream,
            PlaybackState.DecoderResourceExhausted,
            is PlaybackState.PlaybackFailed -> release(ConnectionTestUiState.Failed)
        }

    fun whileHoldingPreview(playbackState: PlaybackState?): ConnectionTestPlaybackDecision? =
        when (playbackState) {
            PlaybackState.AuthenticationFailed ->
                release(ConnectionTestUiState.AuthenticationFailed)
            PlaybackState.Offline -> release(ConnectionTestUiState.Offline)
            is PlaybackState.Retrying,
            PlaybackState.UnsupportedStream,
            PlaybackState.DecoderResourceExhausted,
            is PlaybackState.PlaybackFailed -> release(ConnectionTestUiState.Failed)
            null,
            PlaybackState.Idle,
            PlaybackState.Connecting,
            PlaybackState.Live -> null
        }

    private fun release(state: ConnectionTestUiState) =
        ConnectionTestPlaybackDecision(
            connectionState = state,
            action = ConnectionTestPlaybackAction.ReleasePreview,
        )
}
