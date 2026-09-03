package io.github.serkankaracan.camgridtv.playback

import androidx.media3.common.PlaybackException
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class Media3FailureClassifierTest {
    @Test
    fun `classifies 401 and 403 status in the cause chain as authentication failures`() {
        listOf(401, 403).forEach { status ->
            val error =
                IllegalStateException(
                    "RTSP playback failed",
                    IllegalStateException("RTSP response status $status"),
                )

            assertEquals(
                PlaybackFailureReason.AUTHENTICATION,
                Media3FailureClassifier.classify(
                    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                    error,
                ),
            )
        }
    }

    @Test
    fun `classifies Media3 RTSP method and status messages as authentication failures`() {
        listOf("DESCRIBE 401", "SETUP 403").forEach { message ->
            assertEquals(
                PlaybackFailureReason.AUTHENTICATION,
                Media3FailureClassifier.classify(
                    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                    IllegalStateException(message),
                ),
            )
        }
    }

    @Test
    fun `classifies nested authentication text independently of default locale`() {
        val originalLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            val error =
                IllegalStateException(
                    "RTSP playback failed",
                    IllegalStateException("AUTHENTICATION FAILED"),
                )

            assertEquals(
                PlaybackFailureReason.AUTHENTICATION,
                Media3FailureClassifier.classify(
                    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                    error,
                ),
            )
        } finally {
            Locale.setDefault(originalLocale)
        }
    }

    @Test
    fun `does not mistake unrelated numeric status for authentication`() {
        val error = IllegalStateException("RTSP response status 404")

        assertEquals(
            PlaybackFailureReason.TRANSIENT,
            Media3FailureClassifier.classify(
                PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                error,
            ),
        )
    }

    @Test
    fun `does not mistake a port or delay for an authentication status`() {
        listOf(
                "Connection to camera port 401 failed",
                "Retry scheduled after 403 ms",
            )
            .forEach { message ->
                assertEquals(
                    PlaybackFailureReason.TRANSIENT,
                    Media3FailureClassifier.classify(
                        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                        IllegalStateException(message),
                    ),
                )
            }
    }
}
