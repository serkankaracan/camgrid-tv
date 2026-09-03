package io.github.serkankaracan.camgridtv.playback

sealed interface PlaybackState {
    data object Idle : PlaybackState

    data object Connecting : PlaybackState

    data object Live : PlaybackState

    data class Retrying(
        val attempt: Int,
        val nextDelayMillis: Long,
    ) : PlaybackState {
        init {
            require(attempt > 0) { "Retry attempt must be positive" }
            require(nextDelayMillis >= 0L) { "Retry delay must not be negative" }
        }
    }

    data object AuthenticationFailed : PlaybackState

    data object Offline : PlaybackState

    data object UnsupportedStream : PlaybackState

    data object DecoderResourceExhausted : PlaybackState

    /** [safeMessage] is selected from local constants; engine exceptions never reach this state. */
    @ConsistentCopyVisibility
    data class PlaybackFailed internal constructor(val safeMessage: String) : PlaybackState
}

enum class PlaybackFailureReason {
    AUTHENTICATION,
    NETWORK_UNAVAILABLE,
    UNSUPPORTED_STREAM,
    DECODER_RESOURCE_EXHAUSTED,
    TRANSIENT,
    FATAL,
}

sealed interface PlaybackEvent {
    data object StartRequested : PlaybackEvent

    data object Ready : PlaybackEvent

    data class RetryScheduled(
        val attempt: Int,
        val delayMillis: Long,
    ) : PlaybackEvent

    data class Failed(val reason: PlaybackFailureReason) : PlaybackEvent

    data object NetworkLost : PlaybackEvent

    data object StopRequested : PlaybackEvent
}

/** Pure state transition function shared by the coordinator and JVM tests. */
object PlaybackStateReducer {
    fun reduce(
        current: PlaybackState,
        event: PlaybackEvent,
    ): PlaybackState =
        when (event) {
            PlaybackEvent.StartRequested -> PlaybackState.Connecting
            PlaybackEvent.Ready -> PlaybackState.Live
            is PlaybackEvent.RetryScheduled ->
                PlaybackState.Retrying(
                    attempt = event.attempt,
                    nextDelayMillis = event.delayMillis,
                )
            is PlaybackEvent.Failed -> failureState(event.reason)
            PlaybackEvent.NetworkLost -> PlaybackState.Offline
            PlaybackEvent.StopRequested -> PlaybackState.Idle
        }

    private fun failureState(reason: PlaybackFailureReason): PlaybackState =
        when (reason) {
            PlaybackFailureReason.AUTHENTICATION -> PlaybackState.AuthenticationFailed
            PlaybackFailureReason.NETWORK_UNAVAILABLE -> PlaybackState.Offline
            PlaybackFailureReason.UNSUPPORTED_STREAM -> PlaybackState.UnsupportedStream
            PlaybackFailureReason.DECODER_RESOURCE_EXHAUSTED ->
                PlaybackState.DecoderResourceExhausted
            PlaybackFailureReason.TRANSIENT ->
                PlaybackState.PlaybackFailed(TRANSIENT_FAILURE_MESSAGE)
            PlaybackFailureReason.FATAL -> PlaybackState.PlaybackFailed(FATAL_FAILURE_MESSAGE)
        }

    private const val TRANSIENT_FAILURE_MESSAGE = "The camera stream was interrupted."
    private const val FATAL_FAILURE_MESSAGE = "The camera stream could not be played."
}
