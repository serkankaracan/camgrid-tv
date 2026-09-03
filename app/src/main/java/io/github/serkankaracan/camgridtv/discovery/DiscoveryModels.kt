package io.github.serkankaracan.camgridtv.discovery

data class ProbeMatch(
    val endpointAddress: String?,
    val xAddrs: List<String>,
    val scopes: List<String>,
    val types: List<String>,
    val metadataVersion: Long?,
    val sourceHost: String,
)

data class DiscoveredOnvifDevice(
    val id: String,
    val endpointUuid: String?,
    val xAddrs: List<String>,
    val scopes: List<String>,
    val types: List<String>,
    val host: String,
    val onvifPort: Int,
    val discoveredName: String,
    val manufacturer: String?,
    val model: String?,
    val lastSeenEpochMillis: Long,
)

data class DiscoverySnapshot(
    val devices: List<DiscoveredOnvifDevice> = emptyList(),
    val isScanning: Boolean,
    val issue: DiscoveryIssue? = null,
)

enum class DiscoveryIssue {
    NO_ACTIVE_LOCAL_NETWORK,
    PERMISSION_REQUIRED,
    TRANSPORT_UNAVAILABLE,
    NETWORK_LOST,
}

data class DiscoveryOptions(
    val attempts: Int = 3,
    val totalTimeoutMillis: Long = 10_000L,
    val receivePollMillis: Int = 250,
) {
    init {
        require(attempts in 1..5) { "Discovery attempt count is invalid" }
        require(totalTimeoutMillis in 1_000L..30_000L) { "Discovery timeout is invalid" }
        require(receivePollMillis in 50..1_000) { "Discovery receive poll is invalid" }
    }
}

sealed interface DiscoveryEvent {
    data class Match(val probeMatch: ProbeMatch) : DiscoveryEvent

    data class Issue(val issue: DiscoveryIssue) : DiscoveryEvent
}
