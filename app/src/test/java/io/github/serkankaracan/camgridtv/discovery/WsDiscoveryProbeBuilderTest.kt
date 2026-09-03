package io.github.serkankaracan.camgridtv.discovery

import java.util.UUID
import org.junit.Assert.assertTrue
import org.junit.Test

class WsDiscoveryProbeBuilderTest {
    private val fixedId = UUID.fromString("12345678-1234-4234-8234-123456789abc")
    private val builder = WsDiscoveryProbeBuilder { fixedId }

    @Test
    fun `builds a 2005 ONVIF network video transmitter probe`() {
        val probe = builder.build(WsDiscoveryVersion.APRIL_2005).toString(Charsets.UTF_8)

        assertTrue(probe.contains("http://schemas.xmlsoap.org/ws/2005/04/discovery"))
        assertTrue(probe.contains("urn:uuid:$fixedId"))
        assertTrue(probe.contains("dn:NetworkVideoTransmitter"))
    }

    @Test
    fun `builds a 2009 probe with its matching action and destination`() {
        val probe = builder.build(WsDiscoveryVersion.JANUARY_2009).toString(Charsets.UTF_8)

        assertTrue(probe.contains("http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01/Probe"))
        assertTrue(probe.contains("urn:docs-oasis-open-org:ws-dd:ns:discovery:2009:01"))
    }
}
