package io.github.serkankaracan.camgridtv.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RetryPolicyTest {
    @Test
    fun `uses documented backoff sequence with deterministic jitter`() {
        val policy = RetryPolicy(jitter = RetryJitter { 0L })

        val delays =
            (1..6).map { attempt ->
                val decision =
                    policy.decisionFor(PlaybackFailureReason.TRANSIENT, attempt)
                        as RetryDecision.RetryAfter
                assertEquals(attempt, decision.attempt)
                decision.delayMillis
            }

        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 15_000L, 22_500L), delays)
    }

    @Test
    fun `bounds steady state delays between fifteen and thirty seconds`() {
        val shortest = RetryPolicy(jitter = RetryJitter { -it })
        val longest = RetryPolicy(jitter = RetryJitter { it })

        assertEquals(
            15_000L,
            (shortest.decisionFor(PlaybackFailureReason.TRANSIENT, 6) as RetryDecision.RetryAfter)
                .delayMillis,
        )
        assertEquals(
            30_000L,
            (longest.decisionFor(PlaybackFailureReason.TRANSIENT, 6) as RetryDecision.RetryAfter)
                .delayMillis,
        )
    }

    @Test
    fun `production default continues with bounded steady state retry`() {
        val policy = RetryPolicy(jitter = RetryJitter { 0L })

        val decision =
            policy.decisionFor(PlaybackFailureReason.TRANSIENT, 10_000) as RetryDecision.RetryAfter

        assertEquals(10_000, decision.attempt)
        assertEquals(22_500L, decision.delayMillis)
    }

    @Test
    fun `does not retry failures that require intervention`() {
        val policy = RetryPolicy(jitter = RetryJitter { 0L })

        listOf(
                PlaybackFailureReason.AUTHENTICATION,
                PlaybackFailureReason.UNSUPPORTED_STREAM,
                PlaybackFailureReason.DECODER_RESOURCE_EXHAUSTED,
                PlaybackFailureReason.FATAL,
            )
            .forEach { reason ->
                assertEquals(RetryDecision.DoNotRetry, policy.decisionFor(reason, 1))
            }
        assertEquals(
            RetryDecision.WaitForConnectivity,
            policy.decisionFor(PlaybackFailureReason.NETWORK_UNAVAILABLE, 1),
        )
    }

    @Test
    fun `stops after configured maximum attempts`() {
        val policy = RetryPolicy(jitter = RetryJitter { 0L }, maxAttempts = 2)

        assertEquals(
            RetryDecision.DoNotRetry,
            policy.decisionFor(PlaybackFailureReason.TRANSIENT, 3),
        )
    }

    @Test
    fun `rejects invalid attempt and out of contract jitter`() {
        val policy = RetryPolicy(jitter = RetryJitter { it + 1L })

        assertThrows(IllegalArgumentException::class.java) {
            policy.decisionFor(PlaybackFailureReason.TRANSIENT, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            policy.decisionFor(PlaybackFailureReason.TRANSIENT, 1)
        }
    }
}
