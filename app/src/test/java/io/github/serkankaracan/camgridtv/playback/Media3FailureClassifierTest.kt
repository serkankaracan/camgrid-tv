package io.github.serkankaracan.camgridtv.playback

import androidx.media3.common.PlaybackException
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class Media3FailureClassifierTest {
    @Test
    fun `classifies decoder initialization query and reclaimed resource failures as exhausted`() {
        listOf(
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED,
                PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED,
            )
            .forEach { errorCode ->
                assertEquals(
                    PlaybackFailureReason.DECODER_RESOURCE_EXHAUSTED,
                    Media3FailureClassifier.classify(errorCode, decoderError()),
                )
            }
    }

    @Test
    fun `classifies decoder capability failures as unsupported streams`() {
        listOf(
                PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
            )
            .forEach { errorCode ->
                assertEquals(
                    PlaybackFailureReason.UNSUPPORTED_STREAM,
                    Media3FailureClassifier.classify(errorCode, decoderError()),
                )
            }
    }

    @Test
    fun `classifies generic decoding failure as transient`() {
        assertEquals(
            PlaybackFailureReason.TRANSIENT,
            Media3FailureClassifier.classify(
                PlaybackException.ERROR_CODE_DECODING_FAILED,
                decoderError(),
            ),
        )
    }

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

    private fun decoderError(): Throwable = IllegalStateException("Decoder failure")
}
