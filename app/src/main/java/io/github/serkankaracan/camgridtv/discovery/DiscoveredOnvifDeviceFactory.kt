package io.github.serkankaracan.camgridtv.discovery

import io.github.serkankaracan.camgridtv.model.CameraDevice
import io.github.serkankaracan.camgridtv.security.LocalAddressPolicy
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

class DiscoveredOnvifDeviceFactory(
    private val genericCameraNameProvider: () -> String,
    private val localAddressPolicy: LocalAddressPolicy = LocalAddressPolicy(),
) {
    constructor(
        genericCameraName: String,
        localAddressPolicy: LocalAddressPolicy = LocalAddressPolicy(),
    ) : this({ genericCameraName }, localAddressPolicy)

    fun create(match: ProbeMatch, nowEpochMillis: Long): DiscoveredOnvifDevice? {
        require(nowEpochMillis >= 0L) { "Discovery time is invalid" }
        val genericCameraName = currentGenericCameraName()
        val sourceHost = match.sourceHost.trim().removeSurrounding("[", "]")
        if (!localAddressPolicy.isPotentiallyLocalLiteral(sourceHost)) return null

        val xAddrs =
            match.xAddrs
                .asSequence()
                .mapNotNull(DiscoveryAddressNormalizer::xAddr)
                .filter { localAddressPolicy.isPotentiallyLocalLiteral(it.host) }
                .filter { DiscoveryAddressNormalizer.sameLiteralAddress(it.host, sourceHost) }
                .distinctBy(NormalizedXAddr::value)
                .toList()
        val preferredAddress = xAddrs.firstOrNull()
        val host = sourceHost
        val onvifPort = preferredAddress?.port ?: CameraDevice.DEFAULT_ONVIF_PORT
        val endpointUuid = DiscoveryAddressNormalizer.endpoint(match.endpointAddress)
        val scopeMetadata = extractScopeMetadata(match.scopes)
        val discoveredName =
            scopeMetadata.name
                ?: scopeMetadata.model
                ?: "$genericCameraName · ${sanitizeForDisplay(host, 64, genericCameraName)}"
        val identitySeed =
            endpointUuid ?: preferredAddress?.value ?: "${host.lowercase(Locale.ROOT)}:$onvifPort"
        return DiscoveredOnvifDevice(
            id = stableId(identitySeed),
            endpointUuid = endpointUuid,
            xAddrs = xAddrs.map(NormalizedXAddr::value),
            scopes = match.scopes.map { sanitizeForStorage(it, 2_048) }.distinct(),
            types = match.types.map { sanitizeForStorage(it, 2_048) }.distinct(),
            host = host,
            onvifPort = onvifPort,
            discoveredName =
                sanitizeForDisplay(
                    discoveredName,
                    CameraDevice.MAX_DISPLAY_NAME_LENGTH,
                    genericCameraName,
                ),
            manufacturer =
                scopeMetadata.manufacturer?.let {
                    sanitizeForDisplay(it, 120, genericCameraName)
                },
            model = scopeMetadata.model?.let { sanitizeForDisplay(it, 120, genericCameraName) },
            lastSeenEpochMillis = nowEpochMillis,
        )
    }

    private fun extractScopeMetadata(scopes: List<String>): ScopeMetadata {
        var name: String? = null
        var manufacturer: String? = null
        var model: String? = null
        scopes.forEach { scope ->
            val lower = scope.lowercase(Locale.ROOT)
            when {
                "/name/" in lower && name == null -> name = scopeValue(scope, "/name/")
                "/hardware/" in lower && model == null -> model = scopeValue(scope, "/hardware/")
                "/model/" in lower && model == null -> model = scopeValue(scope, "/model/")
                "/manufacturer/" in lower && manufacturer == null -> {
                    manufacturer = scopeValue(scope, "/manufacturer/")
                }
            }
        }
        if (manufacturer == null) {
            val joined = scopes.joinToString(" ").lowercase(Locale.ROOT)
            manufacturer =
                when {
                    "tp-link" in joined || "tplink" in joined -> "TP-Link"
                    "tapo" in joined -> "Tapo"
                    else -> null
                }
        }
        return ScopeMetadata(name = name, manufacturer = manufacturer, model = model)
    }

    private fun scopeValue(scope: String, marker: String): String? {
        val index = scope.lowercase(Locale.ROOT).indexOf(marker)
        if (index < 0) return null
        val encoded = scope.substring(index + marker.length).substringBefore('/')
        if (encoded.isBlank()) return null
        return runCatching {
                URLDecoder.decode(encoded.replace("+", "%2B"), StandardCharsets.UTF_8.name())
                    .replace('_', ' ')
            }
            .getOrNull()
            ?.takeIf(String::isNotBlank)
    }

    private fun currentGenericCameraName(): String =
        genericCameraNameProvider().trim().take(CameraDevice.MAX_DISPLAY_NAME_LENGTH).also {
            require(it.isNotEmpty()) { "Generic camera name must not be blank" }
        }

    private fun sanitizeForDisplay(
        value: String,
        maximum: Int,
        fallback: String,
    ): String =
        sanitizeForStorage(value, maximum).replace(WHITESPACE, " ").trim().ifBlank {
            fallback
        }

    private fun sanitizeForStorage(value: String, maximum: Int): String =
        buildString(minOf(value.length, maximum)) {
            value.take(maximum).forEach { character ->
                if (!character.isISOControl() || character == '\t') append(character)
            }
        }

    private fun stableId(seed: String): String {
        val digest =
            MessageDigest.getInstance("SHA-256").digest(seed.toByteArray(StandardCharsets.UTF_8))
        return "onvif-" + digest.take(16).joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    private data class ScopeMetadata(
        val name: String?,
        val manufacturer: String?,
        val model: String?,
    )

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}
