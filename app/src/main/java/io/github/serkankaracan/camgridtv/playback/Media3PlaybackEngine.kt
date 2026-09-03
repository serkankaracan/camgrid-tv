package io.github.serkankaracan.camgridtv.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import io.github.serkankaracan.camgridtv.util.LocalNetworkRouteResolver
import java.util.Locale

@OptIn(markerClass = [UnstableApi::class])
class Media3PlaybackEngine(
    context: Context,
    private val localNetworkRouteResolver: LocalNetworkRouteResolver,
    private val rtspTimeoutMillis: Long = DEFAULT_RTSP_TIMEOUT_MILLIS,
) : PlaybackEngine {
    init {
        require(rtspTimeoutMillis > 0L) { "RTSP timeout must be positive" }
    }

    private val stateLock = Any()
    private val applicationHandler = Handler(Looper.getMainLooper())

    /** PlayerView/Compose adapters must access this value on the main thread. */
    @get:MainThread
    val player: ExoPlayer =
        ExoPlayer.Builder(context.applicationContext).setLooper(applicationHandler.looper).build()

    private var started = false
    private var released = false

    @Volatile private var eventListener: ((PlaybackEngineEvent) -> Unit)? = null
    private val playerListener =
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val event =
                    when (playbackState) {
                        Player.STATE_BUFFERING -> PlaybackEngineEvent.Connecting
                        Player.STATE_READY -> PlaybackEngineEvent.Live
                        Player.STATE_ENDED ->
                            PlaybackEngineEvent.Failed(PlaybackFailureReason.TRANSIENT)
                        else -> null
                    }
                event?.let { eventListener?.invoke(it) }
            }

            override fun onPlayerError(error: PlaybackException) {
                eventListener?.invoke(
                    PlaybackEngineEvent.Failed(Media3FailureClassifier.classify(error))
                )
            }
        }

    override fun start(
        uri: RtspUri,
        listener: (PlaybackEngineEvent) -> Unit,
    ) {
        synchronized(stateLock) {
            check(!started && !released) { "Playback engine cannot be started" }
            started = true
            eventListener = listener
        }

        runOnApplicationThread {
            if (isReleased()) return@runOnApplicationThread
            try {
                val socketFactory =
                    localNetworkRouteResolver.socketFactoryFor(uri.hostForNetworkBinding())
                if (socketFactory == null) {
                    eventListener?.invoke(
                        PlaybackEngineEvent.Failed(PlaybackFailureReason.NETWORK_UNAVAILABLE)
                    )
                    return@runOnApplicationThread
                }
                player.trackSelectionParameters =
                    player.trackSelectionParameters
                        .buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                        .build()
                player.addListener(playerListener)

                // Debug logging remains disabled because RTSP diagnostics may contain user-info.
                val mediaSource =
                    RtspMediaSource.Factory()
                        .setSocketFactory(socketFactory)
                        .setForceUseRtpTcp(true)
                        .setTimeoutMs(rtspTimeoutMillis)
                        .setDebugLoggingEnabled(false)
                        .createMediaSource(MediaItem.fromUri(uri.valueForPlayback()))
                player.setMediaSource(mediaSource)
                player.prepare()
                player.playWhenReady = true
            } catch (_: Throwable) {
                eventListener?.invoke(PlaybackEngineEvent.Failed(PlaybackFailureReason.FATAL))
            }
        }
    }

    override fun release() {
        val shouldRelease =
            synchronized(stateLock) {
                if (released) {
                    false
                } else {
                    released = true
                    eventListener = null
                    true
                }
            }
        if (!shouldRelease) return

        runOnApplicationThread {
            player.removeListener(playerListener)
            // release() already stops playback and frees media, decoder and surface resources.
            player.release()
        }
    }

    private fun isReleased(): Boolean = synchronized(stateLock) { released }

    private fun runOnApplicationThread(block: () -> Unit) {
        if (Looper.myLooper() == applicationHandler.looper) {
            block()
        } else {
            applicationHandler.post { block() }
        }
    }

    companion object {
        const val DEFAULT_RTSP_TIMEOUT_MILLIS = 15_000L
    }
}

@OptIn(markerClass = [UnstableApi::class])
class Media3PlaybackEngineFactory(
    context: Context,
    private val localNetworkRouteResolver: LocalNetworkRouteResolver,
    private val rtspTimeoutMillis: Long = Media3PlaybackEngine.DEFAULT_RTSP_TIMEOUT_MILLIS,
) : PlaybackEngineFactory {
    private val applicationContext = context.applicationContext

    override fun create(slotId: String): PlaybackEngine =
        Media3PlaybackEngine(
            context = applicationContext,
            localNetworkRouteResolver = localNetworkRouteResolver,
            rtspTimeoutMillis = rtspTimeoutMillis,
        )
}

/** Maps Media3 failures to a deliberately small, non-sensitive error vocabulary. */
@OptIn(markerClass = [UnstableApi::class])
internal object Media3FailureClassifier {
    fun classify(error: PlaybackException): PlaybackFailureReason = classify(error.errorCode, error)

    internal fun classify(
        errorCode: Int,
        error: Throwable,
    ): PlaybackFailureReason {
        if (isAuthenticationFailure(error)) return PlaybackFailureReason.AUTHENTICATION

        return when (errorCode) {
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
                PlaybackFailureReason.UNSUPPORTED_STREAM

            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
            PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ->
                PlaybackFailureReason.DECODER_RESOURCE_EXHAUSTED

            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            PlaybackException.ERROR_CODE_DECODING_FAILED -> PlaybackFailureReason.TRANSIENT

            else -> PlaybackFailureReason.FATAL
        }
    }

    private fun isAuthenticationFailure(error: Throwable): Boolean {
        var current: Throwable? = error
        repeat(MAX_CAUSE_DEPTH) {
            val normalizedMessage = current?.message?.lowercase(Locale.ROOT).orEmpty()
            if (
                AUTHENTICATION_TERMS.any(normalizedMessage::contains) ||
                    AUTHENTICATION_STATUS.containsMatchIn(normalizedMessage) ||
                    RTSP_METHOD_AUTHENTICATION_STATUS.containsMatchIn(normalizedMessage)
            ) {
                return true
            }
            current = current?.cause
        }
        return false
    }

    private const val MAX_CAUSE_DEPTH = 8
    private val AUTHENTICATION_TERMS = listOf("unauthorized", "authentication failed")
    private val AUTHENTICATION_STATUS =
        Regex(
            "\\b(?:status(?:\\s+code)?|response\\s+code|error\\s+code)\\s*[:=]?\\s*(?:401|403)\\b"
        )
    private val RTSP_METHOD_AUTHENTICATION_STATUS =
        Regex(
            "\\b(?:options|describe|setup|play|pause|teardown|get_parameter|set_parameter)\\s+(?:401|403)\\b"
        )
}
