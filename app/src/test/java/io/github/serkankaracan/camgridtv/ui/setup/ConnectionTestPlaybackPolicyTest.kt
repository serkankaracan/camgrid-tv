package io.github.serkankaracan.camgridtv.ui.setup

import io.github.serkankaracan.camgridtv.playback.PlaybackEvent
import io.github.serkankaracan.camgridtv.playback.PlaybackFailureReason
import io.github.serkankaracan.camgridtv.playback.PlaybackState
import io.github.serkankaracan.camgridtv.playback.PlaybackStateReducer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionTestPlaybackPolicyTest {
    @Test
    fun `live result keeps the single camera preview for visual verification`() {
        val decision = ConnectionTestPlaybackPolicy.duringTest(PlaybackState.Live)

        assertEquals(ConnectionTestUiState.Connected, decision.connectionState)
        assertEquals(ConnectionTestPlaybackAction.HoldPreview, decision.action)
    }

    @Test
    fun `connecting and retrying continue within the bounded test`() {
        listOf(
                PlaybackState.Connecting,
                PlaybackState.Retrying(attempt = 1, nextDelayMillis = 10),
            )
            .forEach { playbackState ->
                val decision = ConnectionTestPlaybackPolicy.duringTest(playbackState)

                assertEquals(ConnectionTestUiState.Testing, decision.connectionState)
                assertEquals(ConnectionTestPlaybackAction.ContinueTesting, decision.action)
            }
    }

    @Test
    fun `every terminal playback failure releases preview with a safe setup result`() {
        val failures =
            listOf(
                PlaybackState.AuthenticationFailed to ConnectionTestUiState.AuthenticationFailed,
                PlaybackState.Offline to ConnectionTestUiState.Offline,
                PlaybackState.UnsupportedStream to ConnectionTestUiState.Failed,
                PlaybackState.DecoderResourceExhausted to ConnectionTestUiState.Failed,
                PlaybackStateReducer.reduce(
                    PlaybackState.Connecting,
                    PlaybackEvent.Failed(PlaybackFailureReason.FATAL),
                ) to ConnectionTestUiState.Failed,
            )

        failures.forEach { (playbackState, expectedState) ->
            val decision = ConnectionTestPlaybackPolicy.duringTest(playbackState)

            assertEquals(expectedState, decision.connectionState)
            assertEquals(ConnectionTestPlaybackAction.ReleasePreview, decision.action)
        }
    }

    @Test
    fun `failure after live releases held preview while healthy states do nothing`() {
        val failure =
            ConnectionTestPlaybackPolicy.whileHoldingPreview(
                PlaybackState.Retrying(attempt = 1, nextDelayMillis = 10)
            )

        assertEquals(ConnectionTestUiState.Failed, failure?.connectionState)
        assertEquals(ConnectionTestPlaybackAction.ReleasePreview, failure?.action)
        assertNull(ConnectionTestPlaybackPolicy.whileHoldingPreview(PlaybackState.Live))
    }
}
