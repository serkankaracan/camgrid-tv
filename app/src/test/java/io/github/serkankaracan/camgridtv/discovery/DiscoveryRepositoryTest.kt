package io.github.serkankaracan.camgridtv.discovery

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryRepositoryTest {
    @Test
    fun `emits incremental deduplicated snapshots`() = runTest {
        val first = probe("192.168.50.100")
        val moved = probe("192.168.50.110")
        val client =
            object : WsDiscoveryClient {
                override fun discover(options: DiscoveryOptions): Flow<DiscoveryEvent> =
                    flowOf(DiscoveryEvent.Match(first), DiscoveryEvent.Match(moved))
            }
        var time = 0L
        val snapshots =
            DefaultDiscoveryRepository(client, wallClockMillis = { ++time }).scan().toList()

        assertTrue(snapshots.first().isScanning)
        assertFalse(snapshots.last().isScanning)
        assertEquals(1, snapshots.last().devices.size)
        assertEquals("192.168.50.110", snapshots.last().devices.single().host)
    }

    private fun probe(host: String) =
        ProbeMatch(
            endpointAddress = "urn:uuid:11111111-2222-4333-8444-555555555555",
            xAddrs = listOf("http://$host:2020/onvif/device_service"),
            scopes = listOf("onvif://www.onvif.org/name/Camera"),
            types = listOf("dn:NetworkVideoTransmitter"),
            metadataVersion = 1,
            sourceHost = host,
        )
}
