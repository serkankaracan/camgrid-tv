package io.github.serkankaracan.camgridtv.discovery

import io.github.serkankaracan.camgridtv.model.CameraConfiguration
import io.github.serkankaracan.camgridtv.model.CameraDevice

/**
 * Merges ephemeral discovery identities into persisted camera records.
 *
 * A camera may first be seen without an endpoint UUID and receive one on a later response. The
 * persisted id, user name, credential reference and selection order must survive that transition.
 */
class CameraDiscoveryMergePolicy(
    private val identityMatcher: CameraIdentityMatcher = CameraIdentityMatcher()
) {
    fun merge(
        current: CameraConfiguration,
        discoveredDevices: List<DiscoveredOnvifDevice>,
        selectedByDiscoveryId: Map<String, Boolean>,
    ): CameraConfiguration {
        val cameras = current.cameras.toMutableList()
        val claimedPersistedIds = mutableSetOf<String>()
        var nextSelectionOrder =
            (current.cameras.mapNotNull(CameraDevice::selectionOrder).maxOrNull() ?: -1) + 1

        discoveredDevices.distinctBy(DiscoveredOnvifDevice::id).forEach { discovered ->
            val matchIndex = matchingIndex(cameras, discovered, claimedPersistedIds)
            val previous = matchIndex?.let(cameras::get)
            val selected =
                selectedByDiscoveryId[discovered.id]
                    ?: previous?.let { selectedByDiscoveryId[it.id] }
                    ?: previous?.selected
                    ?: false
            val selectionOrder =
                when {
                    !selected -> null
                    previous?.selected == true -> previous.selectionOrder
                    else -> nextSelectionOrder++
                }
            val merged = mergeCamera(previous, discovered, selected, selectionOrder)

            if (matchIndex == null) {
                if (cameras.none { it.id == merged.id }) cameras += merged
            } else {
                cameras[matchIndex] = merged
                claimedPersistedIds += merged.id
            }
        }

        return current.copy(cameras = cameras)
    }

    fun matchingCamera(
        cameras: List<CameraDevice>,
        discovered: DiscoveredOnvifDevice,
    ): CameraDevice? = matchingIndex(cameras, discovered, emptySet())?.let(cameras::get)

    private fun matchingIndex(
        cameras: List<CameraDevice>,
        discovered: DiscoveredOnvifDevice,
        excludedIds: Set<String>,
    ): Int? {
        val exactIndex =
            cameras
                .indexOfFirst {
                    it.id == discovered.id &&
                        it.id !in excludedIds &&
                        !hasEndpointConflict(it, discovered)
                }
                .takeIf { it >= 0 }
        if (exactIndex != null) return exactIndex

        return cameras.indices
            .asSequence()
            .filter { cameras[it].id !in excludedIds }
            .map { index -> index to identityMatcher.match(cameras[index], discovered) }
            .filter { (_, match) -> match.isMatch }
            .minByOrNull { (_, match) -> match.strength.ordinal }
            ?.first
    }

    private fun hasEndpointConflict(
        camera: CameraDevice,
        discovered: DiscoveredOnvifDevice,
    ): Boolean {
        val storedEndpoint = DiscoveryAddressNormalizer.endpoint(camera.endpointUuid)
        val discoveredEndpoint = DiscoveryAddressNormalizer.endpoint(discovered.endpointUuid)
        return storedEndpoint != null &&
            discoveredEndpoint != null &&
            storedEndpoint != discoveredEndpoint
    }

    private fun mergeCamera(
        previous: CameraDevice?,
        discovered: DiscoveredOnvifDevice,
        selected: Boolean,
        selectionOrder: Int?,
    ): CameraDevice {
        val discoveredXAddr =
            DiscoveryAddressNormalizer.xAddrsForHost(discovered.xAddrs, discovered.host)
                .firstOrNull()
                ?.value
        val safeXAddr =
            discoveredXAddr
                ?: previous?.onvifXAddr?.let {
                    DiscoveryAddressNormalizer.xAddrForHost(it, discovered.host)?.value
                }
        return if (previous == null) {
            CameraDevice(
                id = discovered.id,
                endpointUuid = discovered.endpointUuid,
                onvifXAddr = safeXAddr,
                displayName = discovered.discoveredName,
                discoveredName = discovered.discoveredName,
                manufacturer = discovered.manufacturer,
                model = discovered.model,
                host = discovered.host,
                onvifPort = discovered.onvifPort,
                rtspPort = CameraDevice.DEFAULT_RTSP_PORT,
                credentialProfileId = null,
                selected = selected,
                selectionOrder = selectionOrder,
                lastSeenEpochMillis = discovered.lastSeenEpochMillis,
            )
        } else {
            previous.copy(
                endpointUuid = discovered.endpointUuid ?: previous.endpointUuid,
                onvifXAddr = safeXAddr,
                displayName = previous.displayNameAfterDiscovery(discovered.discoveredName),
                discoveredName = discovered.discoveredName,
                manufacturer = discovered.manufacturer ?: previous.manufacturer,
                model = discovered.model ?: previous.model,
                host = discovered.host,
                onvifPort = discovered.onvifPort,
                selected = selected,
                selectionOrder = selectionOrder,
                lastSeenEpochMillis =
                    maxOf(previous.lastSeenEpochMillis, discovered.lastSeenEpochMillis),
            )
        }
    }
}

internal fun CameraDevice.displayNameAfterDiscovery(discoveredName: String): String =
    if (this.discoveredName != null && displayName == this.discoveredName) {
        discoveredName
    } else {
        displayName
    }
