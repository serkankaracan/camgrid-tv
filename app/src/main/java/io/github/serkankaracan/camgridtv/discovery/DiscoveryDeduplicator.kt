package io.github.serkankaracan.camgridtv.discovery

class DiscoveryDeduplicator {
    fun addOrUpdate(
        current: List<DiscoveredOnvifDevice>,
        incoming: DiscoveredOnvifDevice,
    ): List<DiscoveredOnvifDevice> {
        val matches = current.withIndex().filter { (_, device) -> sameDevice(device, incoming) }
        if (matches.isEmpty()) return (current + incoming).sortedBy(DiscoveredOnvifDevice::id)

        val incomingEndpoint = DiscoveryAddressNormalizer.endpoint(incoming.endpointUuid)
        val matchedEndpoints =
            matches
                .mapNotNull { (_, device) ->
                    DiscoveryAddressNormalizer.endpoint(device.endpointUuid)
                }
                .distinct()
        if (incomingEndpoint == null && matchedEndpoints.size > 1) {
            // An address-only packet cannot identify which authoritative UUID it belongs to.
            // Ignoring the ambiguous observation preserves both devices instead of bridging them.
            return current.sortedBy(DiscoveredOnvifDevice::id)
        }

        val primaryIndex = matches.first().index
        var merged = incoming
        matches.forEach { (_, existing) -> merged = merge(existing, merged) }
        val duplicateIndexes = matches.mapTo(mutableSetOf()) { it.index }
        return buildList {
                current.forEachIndexed { index, device ->
                    when {
                        index == primaryIndex -> add(merged)
                        index !in duplicateIndexes -> add(device)
                    }
                }
            }
            .sortedBy(DiscoveredOnvifDevice::id)
    }

    fun deduplicate(devices: Iterable<DiscoveredOnvifDevice>): List<DiscoveredOnvifDevice> =
        devices.fold(emptyList(), ::addOrUpdate)

    fun sameDevice(first: DiscoveredOnvifDevice, second: DiscoveredOnvifDevice): Boolean {
        val firstEndpoint = DiscoveryAddressNormalizer.endpoint(first.endpointUuid)
        val secondEndpoint = DiscoveryAddressNormalizer.endpoint(second.endpointUuid)
        if (firstEndpoint != null && secondEndpoint != null) {
            return firstEndpoint == secondEndpoint
        }

        val firstAddresses =
            DiscoveryAddressNormalizer.xAddrsForHost(first.xAddrs, first.host)
                .mapTo(mutableSetOf(), NormalizedXAddr::value)
        if (
            DiscoveryAddressNormalizer.xAddrsForHost(second.xAddrs, second.host).any {
                it.value in firstAddresses
            }
        ) {
            return true
        }
        return first.host.equals(second.host, ignoreCase = true) &&
            first.onvifPort == second.onvifPort
    }

    private fun merge(
        existing: DiscoveredOnvifDevice,
        incoming: DiscoveredOnvifDevice,
    ): DiscoveredOnvifDevice {
        val newest =
            if (incoming.lastSeenEpochMillis >= existing.lastSeenEpochMillis) incoming else existing
        val oldest = if (newest === incoming) existing else incoming
        return newest.copy(
            id = existing.id,
            endpointUuid = newest.endpointUuid ?: oldest.endpointUuid,
            xAddrs =
                (newest.xAddrs + oldest.xAddrs)
                    .mapNotNull {
                        DiscoveryAddressNormalizer.xAddrForHost(it, newest.host)
                    }
                    .distinctBy(NormalizedXAddr::value)
                    .map(NormalizedXAddr::value),
            scopes = (newest.scopes + oldest.scopes).distinct(),
            types = (newest.types + oldest.types).distinct(),
            manufacturer = newest.manufacturer ?: oldest.manufacturer,
            model = newest.model ?: oldest.model,
            lastSeenEpochMillis = maxOf(existing.lastSeenEpochMillis, incoming.lastSeenEpochMillis),
        )
    }
}
