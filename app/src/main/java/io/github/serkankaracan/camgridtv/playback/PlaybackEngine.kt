package io.github.serkankaracan.camgridtv.playback

/** The only events an engine may expose; raw exceptions and endpoint details stay internal. */
sealed interface PlaybackEngineEvent {
    data object Connecting : PlaybackEngineEvent

    data object Live : PlaybackEngineEvent

    data class Failed(val reason: PlaybackFailureReason) : PlaybackEngineEvent
}

interface PlaybackEngine {
    /** A coordinator starts each engine exactly once. */
    fun start(
        uri: RtspUri,
        listener: (PlaybackEngineEvent) -> Unit,
    )

    /** Must be idempotent so lifecycle and error paths can safely converge. */
    fun release()
}

fun interface PlaybackEngineFactory {
    fun create(slotId: String): PlaybackEngine
}

data class PlaybackRequest(
    val slotId: String,
    val uri: RtspUri,
    val fallbackUri: RtspUri? = null,
) {
    init {
        require(slotId.isNotBlank() && slotId.length <= MAX_SLOT_ID_LENGTH) {
            "Playback slot identifier is invalid"
        }
        require(
            fallbackUri == null ||
                (uri.stream == RtspStream.PRIMARY &&
                    fallbackUri.stream == RtspStream.SECONDARY &&
                    uri.hostForNetworkBinding() == fallbackUri.hostForNetworkBinding())
        ) {
            "Playback fallback must be a secondary stream for the same camera"
        }
    }

    private companion object {
        const val MAX_SLOT_ID_LENGTH = 256
    }
}

fun interface RetryDelay {
    suspend fun await(delayMillis: Long)
}
