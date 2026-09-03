package io.github.serkankaracan.camgridtv.discovery

import java.net.URI
import java.util.Locale
import java.util.UUID

internal object DiscoveryAddressNormalizer {
    fun endpoint(value: String?): String? {
        val candidate = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (candidate.length > 512) return null
        val uuidText =
            candidate
                .removePrefix("urn:uuid:")
                .removePrefix("URN:UUID:")
                .removeSurrounding("{", "}")
        return runCatching { UUID.fromString(uuidText).toString() }
            .getOrElse { candidate.lowercase(Locale.ROOT) }
    }

    fun xAddr(value: String?): NormalizedXAddr? {
        val candidate = value?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (candidate.length > 2_048) return null
        val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        if (scheme != "http" && scheme != "https") return null
        if (uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) return null
        val host = uri.host?.lowercase(Locale.ROOT) ?: return null
        val port =
            when {
                uri.port in 1..65535 -> uri.port
                uri.port != -1 -> return null
                scheme == "https" -> 443
                else -> 80
            }
        val rawPath = uri.rawPath?.ifBlank { "/" } ?: "/"
        val normalizedUri =
            runCatching {
                    URI(scheme, null, host, port, rawPath, null, null).normalize().toASCIIString()
                }
                .getOrNull() ?: return null
        return NormalizedXAddr(normalizedUri, host, port)
    }
}

internal data class NormalizedXAddr(
    val value: String,
    val host: String,
    val port: Int,
)
