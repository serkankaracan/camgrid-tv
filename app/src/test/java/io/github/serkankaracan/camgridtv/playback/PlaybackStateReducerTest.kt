package io.github.serkankaracan.camgridtv.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlaybackStateReducerTest {
    @Test
    fun `moves through connecting live retry and stop states`() {
        var state: PlaybackState = PlaybackState.Idle

        state = PlaybackStateReducer.reduce(state, PlaybackEvent.StartRequested)
        assertEquals(PlaybackState.Connecting, state)
        state = PlaybackStateReducer.reduce(state, PlaybackEvent.Ready)
        assertEquals(PlaybackState.Live, state)
        state = PlaybackStateReducer.reduce(state, PlaybackEvent.RetryScheduled(2, 2_000L))
        assertEquals(PlaybackState.Retrying(attempt = 2, nextDelayMillis = 2_000L), state)
        state = PlaybackStateReducer.reduce(state, PlaybackEvent.StopRequested)
        assertEquals(PlaybackState.Idle, state)
    }

    @Test
    fun `maps actionable failures to distinct states`() {
        assertEquals(
            PlaybackState.AuthenticationFailed,
            failureState(PlaybackFailureReason.AUTHENTICATION),
        )
        assertEquals(PlaybackState.Offline, failureState(PlaybackFailureReason.NETWORK_UNAVAILABLE))
        assertEquals(
            PlaybackState.UnsupportedStream,
            failureState(PlaybackFailureReason.UNSUPPORTED_STREAM),
        )
    }

    @Test
    fun `maps engine failures only to typed or fixed safe states`() {
        assertEquals(
            PlaybackState.DecoderResourceExhausted,
            failureState(PlaybackFailureReason.DECODER_RESOURCE_EXHAUSTED),
        )
        val transientFailure =
            failureState(PlaybackFailureReason.TRANSIENT) as PlaybackState.PlaybackFailed
        val fatalFailure = failureState(PlaybackFailureReason.FATAL) as PlaybackState.PlaybackFailed

        listOf(transientFailure, fatalFailure).forEach { state ->
            assertFalse(state.safeMessage.contains("rtsp", ignoreCase = true))
            assertFalse(state.safeMessage.contains("192.168"))
        }
        assertFalse(transientFailure.safeMessage == fatalFailure.safeMessage)
    }

    @Test
    fun `network loss always becomes offline`() {
        assertEquals(
            PlaybackState.Offline,
            PlaybackStateReducer.reduce(PlaybackState.Live, PlaybackEvent.NetworkLost),
        )
    }

    private fun failureState(reason: PlaybackFailureReason): PlaybackState =
        PlaybackStateReducer.reduce(
            PlaybackState.Connecting,
            PlaybackEvent.Failed(reason),
        )
}
