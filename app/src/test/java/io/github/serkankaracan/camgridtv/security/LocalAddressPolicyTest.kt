package io.github.serkankaracan.camgridtv.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAddressPolicyTest {
    private val policy = LocalAddressPolicy()

    @Test
    fun `allows RFC1918 IPv4 and local IPv6 literals`() {
        assertTrue(policy.isPotentiallyLocalLiteral("10.20.30.40"))
        assertTrue(policy.isPotentiallyLocalLiteral("172.20.30.40"))
        assertTrue(policy.isPotentiallyLocalLiteral("192.168.50.100"))
        assertTrue(policy.isPotentiallyLocalLiteral("169.254.30.40"))
        assertTrue(policy.isPotentiallyLocalLiteral("fd12:3456::10"))
        assertTrue(policy.isPotentiallyLocalLiteral("fe80::10"))
    }

    @Test
    fun `rejects public addresses hostnames loopback multicast and ambiguous IPv4`() {
        assertFalse(policy.isPotentiallyLocalLiteral("198.51.100.20"))
        assertFalse(policy.isPotentiallyLocalLiteral("camera.example"))
        assertFalse(policy.isPotentiallyLocalLiteral("127.0.0.1"))
        assertFalse(policy.isPotentiallyLocalLiteral("239.1.2.3"))
        assertFalse(policy.isPotentiallyLocalLiteral("192.168.050.100"))
    }

    @Test
    fun `requires an address to match an active route`() {
        val routes = listOf(CidrBlock.parse("192.168.50.0/24"))

        assertTrue(policy.isAllowed("192.168.50.100", routes))
        assertFalse(policy.isAllowed("192.168.51.100", routes))
        assertFalse(policy.isAllowed("192.168.50.100", emptyList()))
    }

    @Test
    fun `supports IPv6 route matching`() {
        val routes = listOf(CidrBlock.parse("fd12:3456::/64"))

        assertTrue(policy.isAllowed("[fd12:3456::20]", routes))
        assertFalse(policy.isAllowed("fd12:9876::20", routes))
    }
}
