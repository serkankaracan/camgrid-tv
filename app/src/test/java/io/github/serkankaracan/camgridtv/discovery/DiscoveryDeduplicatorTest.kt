package io.github.serkankaracan.camgridtv.discovery

import org.junit.Assert.assertEquals
import org.junit.Test

class DiscoveryDeduplicatorTest {
    private val deduplicator = DiscoveryDeduplicator()

    @Test
    fun `same endpoint uuid updates changed address`() {
        val old =
            device(
                id = "old-id",
                uuid = UUID,
                host = "192.168.50.100",
                xAddr = "http://192.168.50.100:2020/onvif/device_service",
                seen = 1L,
            )
        val moved =
            device(
                id = "new-id",
                uuid = UUID,
                host = "192.168.50.110",
                xAddr = "http://192.168.50.110:2020/onvif/device_service",
                seen = 2L,
            )

        val result = deduplicator.deduplicate(listOf(old, moved)).single()

        assertEquals("old-id", result.id)
        assertEquals("192.168.50.110", result.host)
        assertEquals(listOf(moved.xAddrs.single()), result.xAddrs)
        assertEquals(2L, result.lastSeenEpochMillis)
    }

    @Test
    fun `newest source host cannot inherit a stale xaddr from an older observation`() {
        val old =
            device(
                id = "old-id",
                uuid = UUID,
                host = "192.168.50.100",
                xAddr = "http://192.168.50.100:2020/onvif/device_service",
                seen = 1L,
            )
        val movedWithoutAddress =
            old.copy(
                id = "new-id",
                host = "192.168.50.110",
                xAddrs = emptyList(),
                lastSeenEpochMillis = 2L,
            )

        val result = deduplicator.deduplicate(listOf(old, movedWithoutAddress)).single()

        assertEquals("192.168.50.110", result.host)
        assertEquals(emptyList<String>(), result.xAddrs)
    }

    @Test
    fun `repeated packet remains one device`() {
        val repeated =
            device(
                id = "one",
                uuid = UUID,
                host = "192.168.50.100",
                xAddr = "http://192.168.50.100:2020/onvif/device_service",
                seen = 1L,
            )

        assertEquals(1, deduplicator.deduplicate(listOf(repeated, repeated.copy())).size)
    }

    @Test
    fun `distinct endpoint uuids remain separate when address matches`() {
        val first =
            device(
                id = "one",
                uuid = UUID,
                host = "192.168.50.100",
                xAddr = "http://192.168.50.100:2020/onvif/device_service",
                seen = 1L,
            )
        val second =
            device(
                id = "two",
                uuid = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
                host = first.host,
                xAddr = first.xAddrs.single(),
                seen = 2L,
            )

        val result = deduplicator.deduplicate(listOf(first, second))

        assertEquals(listOf("one", "two"), result.map(DiscoveredOnvifDevice::id))
    }

    @Test
    fun `uuid-less observation cannot bridge distinct endpoint uuids`() {
        val first =
            device(
                id = "one",
                uuid = UUID,
                host = "192.168.50.100",
                xAddr = "http://192.168.50.100:2020/onvif/device_service",
                seen = 1L,
            )
        val second =
            device(
                id = "two",
                uuid = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
                host = first.host,
                xAddr = first.xAddrs.single(),
                seen = 2L,
            )
        val addressOnly =
            device(
                id = "address-only",
                uuid = null,
                host = first.host,
                xAddr = first.xAddrs.single(),
                seen = 3L,
            )

        listOf(
                listOf(first, second, addressOnly),
                listOf(addressOnly, first, second),
                listOf(first, addressOnly, second),
            )
            .forEach { observations ->
                val result = deduplicator.deduplicate(observations)

                assertEquals(2, result.size)
                assertEquals(
                    setOf(UUID, second.endpointUuid),
                    result.mapTo(mutableSetOf(), DiscoveredOnvifDevice::endpointUuid),
                )
            }
    }

    @Test
    fun `host and ONVIF port are the final fallback identity`() {
        val first = device("one", null, "192.168.50.100", "http://192.168.50.100:2020/a", 1L)
        val second = device("two", null, "192.168.50.100", "http://192.168.50.100:2020/b", 2L)

        assertEquals(1, deduplicator.deduplicate(listOf(first, second)).size)
    }

    private fun device(
        id: String,
        uuid: String?,
        host: String,
        xAddr: String,
        seen: Long,
    ) =
        DiscoveredOnvifDevice(
            id = id,
            endpointUuid = uuid,
            xAddrs = listOf(xAddr),
            scopes = emptyList(),
            types = listOf("dn:NetworkVideoTransmitter"),
            host = host,
            onvifPort = 2020,
            discoveredName = "Camera",
            manufacturer = null,
            model = null,
            lastSeenEpochMillis = seen,
        )

    private companion object {
        const val UUID = "11111111-2222-4333-8444-555555555555"
    }
}
