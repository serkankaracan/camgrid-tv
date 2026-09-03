package io.github.serkankaracan.camgridtv.discovery

import io.github.serkankaracan.camgridtv.model.CameraDevice

class CameraIdentityMatcher {
    fun match(camera: CameraDevice, discovered: DiscoveredOnvifDevice): CameraIdentityMatch {
        val storedEndpoint = DiscoveryAddressNormalizer.endpoint(camera.endpointUuid)
        val foundEndpoint = DiscoveryAddressNormalizer.endpoint(discovered.endpointUuid)
        if (storedEndpoint != null && foundEndpoint != null) {
            return if (storedEndpoint == foundEndpoint) {
                CameraIdentityMatch(CameraIdentityMatchStrength.ENDPOINT_UUID)
            } else {
                CameraIdentityMatch(CameraIdentityMatchStrength.NONE)
            }
        }

        val storedXAddr =
            DiscoveryAddressNormalizer.xAddrForHost(camera.onvifXAddr, camera.host)?.value
        if (
            storedXAddr != null &&
                DiscoveryAddressNormalizer.xAddrsForHost(
                        discovered.xAddrs,
                        discovered.host,
                    )
                    .any {
                        it.value == storedXAddr
                    }
        ) {
            return CameraIdentityMatch(CameraIdentityMatchStrength.ONVIF_XADDR)
        }

        if (
            camera.host.equals(discovered.host, ignoreCase = true) &&
                camera.onvifPort == discovered.onvifPort
        ) {
            return CameraIdentityMatch(CameraIdentityMatchStrength.HOST_AND_PORT)
        }
        return CameraIdentityMatch(CameraIdentityMatchStrength.NONE)
    }

    fun applyDiscovery(camera: CameraDevice, discovered: DiscoveredOnvifDevice): CameraDevice {
        require(match(camera, discovered).isMatch) {
            "Discovery result does not match camera identity"
        }
        return camera.copy(
            endpointUuid = discovered.endpointUuid ?: camera.endpointUuid,
            onvifXAddr =
                DiscoveryAddressNormalizer.xAddrsForHost(
                        discovered.xAddrs,
                        discovered.host,
                    )
                    .firstOrNull()
                    ?.value
                    ?: DiscoveryAddressNormalizer.xAddrForHost(
                            camera.onvifXAddr,
                            discovered.host,
                        )
                        ?.value,
            displayName = camera.displayNameAfterDiscovery(discovered.discoveredName),
            discoveredName = discovered.discoveredName,
            manufacturer = discovered.manufacturer,
            model = discovered.model,
            host = discovered.host,
            onvifPort = discovered.onvifPort,
            lastSeenEpochMillis = maxOf(camera.lastSeenEpochMillis, discovered.lastSeenEpochMillis),
        )
    }
}

data class CameraIdentityMatch(val strength: CameraIdentityMatchStrength) {
    val isMatch: Boolean = strength != CameraIdentityMatchStrength.NONE
}

enum class CameraIdentityMatchStrength {
    ENDPOINT_UUID,
    ONVIF_XADDR,
    HOST_AND_PORT,
    NONE,
}
