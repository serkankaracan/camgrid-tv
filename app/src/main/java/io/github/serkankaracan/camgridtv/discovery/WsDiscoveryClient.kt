package io.github.serkankaracan.camgridtv.discovery

import kotlinx.coroutines.flow.Flow

interface WsDiscoveryClient {
    /** Cancellation of collection cancels the scan and releases its network resources. */
    fun discover(options: DiscoveryOptions = DiscoveryOptions()): Flow<DiscoveryEvent>
}
