package io.github.serkankaracan.camgridtv.discovery

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface DiscoveryRepository {
    fun scan(options: DiscoveryOptions = DiscoveryOptions()): Flow<DiscoverySnapshot>
}

class DefaultDiscoveryRepository(
    private val client: WsDiscoveryClient,
    private val deviceFactory: DiscoveredOnvifDeviceFactory,
    private val deduplicator: DiscoveryDeduplicator = DiscoveryDeduplicator(),
    private val wallClockMillis: () -> Long = System::currentTimeMillis,
) : DiscoveryRepository {
    override fun scan(options: DiscoveryOptions): Flow<DiscoverySnapshot> = flow {
        var devices = emptyList<DiscoveredOnvifDevice>()
        var issue: DiscoveryIssue? = null
        emit(DiscoverySnapshot(devices = devices, isScanning = true))
        client.discover(options).collect { event ->
            when (event) {
                is DiscoveryEvent.Match -> {
                    val device = deviceFactory.create(event.probeMatch, wallClockMillis())
                    if (device != null) {
                        devices = deduplicator.addOrUpdate(devices, device)
                        emit(DiscoverySnapshot(devices = devices, isScanning = true, issue = issue))
                    }
                }
                is DiscoveryEvent.Issue -> {
                    issue = event.issue
                    emit(DiscoverySnapshot(devices = devices, isScanning = true, issue = issue))
                }
            }
        }
        emit(DiscoverySnapshot(devices = devices, isScanning = false, issue = issue))
    }
}
