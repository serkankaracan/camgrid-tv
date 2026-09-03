package io.github.serkankaracan.camgridtv.playback

import kotlin.math.roundToLong
import kotlin.random.Random

sealed interface RetryDecision {
    data class RetryAfter(
        val attempt: Int,
        val delayMillis: Long,
    ) : RetryDecision

    data object WaitForConnectivity : RetryDecision

    data object DoNotRetry : RetryDecision
}

/** Supplies a bounded signed offset, which makes jitter deterministic in unit tests. */
fun interface RetryJitter {
    fun offsetMillis(maxAbsoluteOffsetMillis: Long): Long
}

object RandomRetryJitter : RetryJitter {
    override fun offsetMillis(maxAbsoluteOffsetMillis: Long): Long {
        if (maxAbsoluteOffsetMillis == 0L) return 0L
        return Random.nextLong(
            from = -maxAbsoluteOffsetMillis,
            until = maxAbsoluteOffsetMillis + 1L,
        )
    }
}

/**
 * Bounded exponential retry for transient failures.
 *
 * Attempts one through five use 1, 2, 4, 8 and 15 seconds before jitter. Later attempts continue in
 * a controlled 15-30 second window. Authentication, unsupported-stream and decoder-resource
 * failures require user/device intervention and are never retried automatically. Tests may inject a
 * finite [maxAttempts], while production keeps retrying transient camera outages until the screen
 * is left or the lifecycle/connectivity boundary cancels the work.
 */
class RetryPolicy(
    private val jitter: RetryJitter = RandomRetryJitter,
    private val maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
) {
    init {
        require(maxAttempts >= 0) { "Maximum retry attempts must not be negative" }
    }

    fun decisionFor(
        reason: PlaybackFailureReason,
        attempt: Int,
    ): RetryDecision {
        require(attempt > 0) { "Retry attempt must be positive" }

        return when (reason) {
            PlaybackFailureReason.NETWORK_UNAVAILABLE -> RetryDecision.WaitForConnectivity
            PlaybackFailureReason.TRANSIENT -> transientDecision(attempt)
            PlaybackFailureReason.AUTHENTICATION,
            PlaybackFailureReason.UNSUPPORTED_STREAM,
            PlaybackFailureReason.DECODER_RESOURCE_EXHAUSTED,
            PlaybackFailureReason.FATAL -> RetryDecision.DoNotRetry
        }
    }

    private fun transientDecision(attempt: Int): RetryDecision {
        if (attempt > maxAttempts) return RetryDecision.DoNotRetry

        val baseDelay = BASE_DELAYS_MILLIS.getOrElse(attempt - 1) { STEADY_STATE_MIDPOINT_MILLIS }
        val maximumJitter =
            if (attempt <= BASE_DELAYS_MILLIS.size) {
                (baseDelay * INITIAL_JITTER_FRACTION).roundToLong()
            } else {
                STEADY_STATE_JITTER_MILLIS
            }
        val rawOffset = jitter.offsetMillis(maximumJitter)
        require(rawOffset in -maximumJitter..maximumJitter) {
            "Retry jitter returned an out-of-range offset"
        }
        val delay = (baseDelay + rawOffset).coerceAtLeast(0L)
        return RetryDecision.RetryAfter(attempt = attempt, delayMillis = delay)
    }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = Int.MAX_VALUE

        private val BASE_DELAYS_MILLIS = longArrayOf(1_000L, 2_000L, 4_000L, 8_000L, 15_000L)
        private const val INITIAL_JITTER_FRACTION = 0.1
        private const val STEADY_STATE_MIDPOINT_MILLIS = 22_500L
        private const val STEADY_STATE_JITTER_MILLIS = 7_500L
    }
}
