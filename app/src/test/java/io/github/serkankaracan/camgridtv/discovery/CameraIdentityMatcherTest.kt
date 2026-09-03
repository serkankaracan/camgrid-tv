package io.github.serkankaracan.camgridtv.discovery

import io.github.serkankaracan.camgridtv.model.CameraDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraIdentityMatcherTest {
    private val matcher = CameraIdentityMatcher()

    @Test
    fun `endpoint identity survives an IP address change`() {
        val stored = camera(host = "192.168.50.100")
        val moved = discovered(host = "192.168.50.110")

        val match = matcher.match(stored, moved)
        val updated = matcher.applyDiscovery(stored, moved)

        assertEquals(CameraIdentityMatchStrength.ENDPOINT_UUID, match.strength)
        assertEquals("192.168.50.110", updated.host)
        assertEquals("My camera", updated.displayName)
        assertTrue(updated.selected)
    }

    @Test
    fun `xaddr precedes host and port fallback`() {
        val stored =
            camera(host = "192.168.50.99")
                .copy(
                    endpointUuid = null,
                    onvifXAddr = "HTTP://192.168.50.100:2020/onvif/device_service",
                )

        assertEquals(
            CameraIdentityMatchStrength.ONVIF_XADDR,
            matcher.match(stored, discovered()).strength,
        )
    }

    @Test
    fun `different known endpoint identities never fall back to a reused address`() {
        val stored = camera(host = "192.168.50.100")
        val differentCamera =
            discovered(host = "192.168.50.100")
                .copy(endpointUuid = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee")

        assertEquals(
            CameraIdentityMatchStrength.NONE,
            matcher.match(stored, differentCamera).strength,
        )
    }

    private fun camera(host: String) =
        CameraDevice(
            id = "stored-camera",
            endpointUuid = UUID,
            onvifXAddr = "http://$host:2020/onvif/device_service",
            displayName = "My camera",
            discoveredName = "Old name",
            host = host,
            selected = true,
            selectionOrder = 0,
            lastSeenEpochMillis = 1L,
        )

    private fun discovered(host: String = "192.168.50.100") =
        DiscoveredOnvifDevice(
            id = "discovered-camera",
            endpointUuid = UUID,
            xAddrs = listOf("http://$host:2020/onvif/device_service"),
            scopes = emptyList(),
            types = emptyList(),
            host = host,
            onvifPort = 2020,
            discoveredName = "Fresh name",
            manufacturer = "Example",
            model = "Model 1",
            lastSeenEpochMillis = 2L,
        )

    private companion object {
        const val UUID = "11111111-2222-4333-8444-555555555555"
    }
}
