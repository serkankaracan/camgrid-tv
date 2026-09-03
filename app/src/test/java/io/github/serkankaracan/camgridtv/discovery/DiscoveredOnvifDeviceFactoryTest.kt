package io.github.serkankaracan.camgridtv.discovery

import io.github.serkankaracan.camgridtv.model.CameraDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveredOnvifDeviceFactoryTest {
    private val factory = DiscoveredOnvifDeviceFactory("ONVIF camera")

    @Test
    fun `injected localized generic name is used when scopes omit a name and model`() {
        val localizedFactory = DiscoveredOnvifDeviceFactory("ONVIF kamera")
        val device =
            checkNotNull(
                localizedFactory.create(
                    match(sourceHost = SOURCE_HOST, xAddrs = emptyList()),
                    nowEpochMillis = 0L,
                )
            )

        assertEquals("ONVIF kamera · $SOURCE_HOST", device.discoveredName)
    }

    @Test
    fun `generic name provider observes a runtime locale change`() {
        var localizedName = "ONVIF camera"
        val localizedFactory =
            DiscoveredOnvifDeviceFactory(genericCameraNameProvider = { localizedName })

        val english =
            checkNotNull(
                localizedFactory.create(
                    match(sourceHost = SOURCE_HOST, xAddrs = emptyList()),
                    nowEpochMillis = 0L,
                )
            )
        localizedName = "ONVIF kamera"
        val turkish =
            checkNotNull(
                localizedFactory.create(
                    match(sourceHost = SOURCE_HOST, xAddrs = emptyList()),
                    nowEpochMillis = 1L,
                )
            )

        assertEquals("ONVIF camera · $SOURCE_HOST", english.discoveredName)
        assertEquals("ONVIF kamera · $SOURCE_HOST", turkish.discoveredName)
    }

    @Test
    fun `mismatched private xaddr cannot redirect a response from the source host`() {
        val device =
            checkNotNull(
                factory.create(
                    match(
                        sourceHost = SOURCE_HOST,
                        xAddrs = listOf("http://192.168.50.200:8899/malicious/device_service"),
                    ),
                    nowEpochMillis = 1L,
                )
            )

        assertEquals(SOURCE_HOST, device.host)
        assertEquals(CameraDevice.DEFAULT_ONVIF_PORT, device.onvifPort)
        assertTrue(device.xAddrs.isEmpty())
    }

    @Test
    fun `multiple xaddrs retain only source matching ports and paths`() {
        val device =
            checkNotNull(
                factory.create(
                    match(
                        sourceHost = SOURCE_HOST,
                        xAddrs =
                            listOf(
                                "http://192.168.50.200:8899/malicious/device_service",
                                "http://$SOURCE_HOST:8080/custom/onvif/device_service",
                                "https://$SOURCE_HOST/alternate/device_service",
                            ),
                    ),
                    nowEpochMillis = 2L,
                )
            )

        assertEquals(SOURCE_HOST, device.host)
        assertEquals(8080, device.onvifPort)
        assertEquals(
            listOf(
                "http://$SOURCE_HOST:8080/custom/onvif/device_service",
                "https://$SOURCE_HOST:443/alternate/device_service",
            ),
            device.xAddrs,
        )
    }

    @Test
    fun `equivalent ipv6 source and xaddr literals preserve xaddr metadata`() {
        val device =
            checkNotNull(
                factory.create(
                    match(
                        sourceHost = "fd00:0:0:0:0:0:0:1",
                        xAddrs = listOf("http://[fd00::1]:8081/onvif/device_service"),
                    ),
                    nowEpochMillis = 3L,
                )
            )

        assertEquals("fd00:0:0:0:0:0:0:1", device.host)
        assertEquals(8081, device.onvifPort)
        assertEquals(1, device.xAddrs.size)
        assertTrue(device.xAddrs.single().endsWith(":8081/onvif/device_service"))
    }

    private fun match(
        sourceHost: String,
        xAddrs: List<String>,
    ) =
        ProbeMatch(
            endpointAddress = null,
            xAddrs = xAddrs,
            scopes = emptyList(),
            types = emptyList(),
            metadataVersion = null,
            sourceHost = sourceHost,
        )

    private companion object {
        const val SOURCE_HOST = "192.168.50.100"
    }
}
