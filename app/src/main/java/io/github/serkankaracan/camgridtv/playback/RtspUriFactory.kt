package io.github.serkankaracan.camgridtv.playback

import io.github.serkankaracan.camgridtv.util.LocalAddressPolicy
import io.github.serkankaracan.camgridtv.util.Rfc1918LocalAddressPolicy
import io.github.serkankaracan.camgridtv.util.Rfc3986UserInfoEncoder

enum class RtspStream(val pathSegment: String) {
    PRIMARY("stream1"),
    SECONDARY("stream2"),
}

/**
 * A credential-bearing value whose ordinary string representation is always redacted.
 *
 * Call [valueForPlayback] only at the final media-engine boundary. Never log or surface its result.
 * Equality intentionally includes credentials so a coordinator replaces a player after credentials
 * change.
 */
class RtspUri
internal constructor(
    private val playbackValue: String,
    private val routingHost: String,
    val redactedValue: String,
    val stream: RtspStream,
) {
    fun valueForPlayback(): String = playbackValue

    /** Used only to select a route-bound socket factory at the final playback boundary. */
    internal fun hostForNetworkBinding(): String = routingHost

    override fun toString(): String = redactedValue

    override fun equals(other: Any?): Boolean =
        this === other || (other is RtspUri && playbackValue == other.playbackValue)

    override fun hashCode(): Int = playbackValue.hashCode()
}

class RtspUriFactory(
    private val localAddressPolicy: LocalAddressPolicy = Rfc1918LocalAddressPolicy()
) {
    fun create(
        username: String,
        password: String,
        host: String,
        port: Int = DEFAULT_RTSP_PORT,
        stream: RtspStream,
    ): RtspUri {
        validateCredentialComponent(username, "RTSP username is invalid")
        validateCredentialComponent(password, "RTSP password is invalid")
        require(port in VALID_PORTS) { "RTSP port is invalid" }

        val canonicalHost = localAddressPolicy.requireAllowed(host)
        val encodedUsername = Rfc3986UserInfoEncoder.encode(username)
        val encodedPassword = Rfc3986UserInfoEncoder.encode(password)

        val playbackValue = buildString {
            append(SCHEME_PREFIX)
            append(encodedUsername)
            append(':')
            append(encodedPassword)
            append('@')
            append(canonicalHost)
            append(':')
            append(port)
            append('/')
            append(stream.pathSegment)
        }
        val redactedValue = buildString {
            append(SCHEME_PREFIX)
            append(REDACTED_COMPONENT)
            append(':')
            append(REDACTED_COMPONENT)
            append('@')
            append(canonicalHost)
            append(':')
            append(port)
            append('/')
            append(stream.pathSegment)
        }

        return RtspUri(
            playbackValue = playbackValue,
            routingHost = canonicalHost,
            redactedValue = redactedValue,
            stream = stream,
        )
    }

    private fun validateCredentialComponent(value: String, safeMessage: String) {
        require(value.isNotEmpty() && value.length <= MAX_CREDENTIAL_COMPONENT_LENGTH) {
            safeMessage
        }
        require(value.none(Char::isISOControl)) { safeMessage }
    }

    companion object {
        const val DEFAULT_RTSP_PORT = 554
        const val MAX_CREDENTIAL_COMPONENT_LENGTH = 1_024
        val VALID_PORTS: IntRange = 1..65_535

        private const val SCHEME_PREFIX = "rtsp://"
        private const val REDACTED_COMPONENT = "***"
    }
}
