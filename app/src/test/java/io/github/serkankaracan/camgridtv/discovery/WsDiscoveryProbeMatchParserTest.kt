package io.github.serkankaracan.camgridtv.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WsDiscoveryProbeMatchParserTest {
    private val parser = WsDiscoveryProbeMatchParser()

    @Test
    fun `parses endpoint scopes xaddr types and metadata`() {
        val result =
            parser.parse(fixture("single-probe-match-2005.xml"), SOURCE_HOST)
                as WsDiscoveryParseResult.Success
        val match = result.matches.single()

        assertEquals("urn:uuid:11111111-2222-4333-8444-555555555555", match.endpointAddress)
        assertEquals(3, match.scopes.size)
        assertEquals(1, match.xAddrs.size)
        assertEquals(listOf("dn:NetworkVideoTransmitter"), match.types)
        assertEquals(7L, match.metadataVersion)
        assertEquals(SOURCE_HOST, match.sourceHost)
    }

    @Test
    fun `parses namespace variations multiple matches and xaddrs`() {
        val result =
            parser.parse(fixture("multiple-probe-match-2009.xml"), SOURCE_HOST)
                as WsDiscoveryParseResult.Success

        assertEquals(2, result.matches.size)
        assertEquals(2, result.matches.first().xAddrs.size)
        assertTrue(result.matches.last().scopes.any { "C510W" in it })
    }

    @Test
    fun `rejects malformed xml without throwing`() {
        val result = parser.parse(fixture("malformed.xml"), SOURCE_HOST)

        assertEquals(
            WsDiscoveryParseResult.Rejected(WsDiscoveryRejection.MALFORMED_XML),
            result,
        )
    }

    @Test
    fun `rejects doctype and external entities before parsing`() {
        val xml = """<!DOCTYPE x [<!ENTITY read SYSTEM "file:///not-read">]><ProbeMatches/>"""

        assertEquals(
            WsDiscoveryParseResult.Rejected(WsDiscoveryRejection.UNSAFE_XML),
            parser.parse(xml.toByteArray(), SOURCE_HOST),
        )
    }

    @Test
    fun `long discovered name is bounded for display`() {
        val name = "A".repeat(300)
        val xml =
            """<Envelope><ProbeMatches><ProbeMatch>
            |<EndpointReference><Address>urn:uuid:cccccccc-dddd-4eee-8fff-000000000000</Address></EndpointReference>
            |<Scopes>onvif://www.onvif.org/name/$name</Scopes>
            |<XAddrs>http://$SOURCE_HOST:2020/onvif/device_service</XAddrs>
            |</ProbeMatch></ProbeMatches></Envelope>
        """
                .trimMargin()
        val match =
            (parser.parse(xml.toByteArray(), SOURCE_HOST) as WsDiscoveryParseResult.Success)
                .matches
                .single()
        val device = checkNotNull(DiscoveredOnvifDeviceFactory("ONVIF camera").create(match, 1L))

        assertEquals(120, device.discoveredName.length)
    }

    private fun fixture(name: String): ByteArray =
        checkNotNull(javaClass.getResourceAsStream("/discovery/$name")).use { it.readBytes() }

    private companion object {
        const val SOURCE_HOST = "192.168.50.100"
    }
}
