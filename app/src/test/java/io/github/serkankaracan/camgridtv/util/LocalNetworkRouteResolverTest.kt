package io.github.serkankaracan.camgridtv.util

import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import javax.net.SocketFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkRouteResolverTest {
    @Test
    fun `accepts canonical private target on direct active interface subnet`() {
        assertTrue(
            LocalIpv4RouteAdmission.allows(
                canonicalHost = "192.168.50.44",
                destinationAddress = ipv4(192, 168, 50, 0),
                prefixLength = 24,
                hasGateway = false,
                isUnicast = true,
                interfaceAddresses = listOf(ipv4(192, 168, 50, 10)),
            )
        )
    }

    @Test
    fun `rejects route outside target subnet or without an address on that subnet`() {
        assertFalse(
            LocalIpv4RouteAdmission.allows(
                canonicalHost = "192.168.51.44",
                destinationAddress = ipv4(192, 168, 50, 0),
                prefixLength = 24,
                hasGateway = false,
                isUnicast = true,
                interfaceAddresses = listOf(ipv4(192, 168, 50, 10)),
            )
        )
        assertFalse(
            LocalIpv4RouteAdmission.allows(
                canonicalHost = "192.168.50.44",
                destinationAddress = ipv4(192, 168, 50, 0),
                prefixLength = 24,
                hasGateway = false,
                isUnicast = true,
                interfaceAddresses = listOf(ipv4(10, 20, 30, 10)),
            )
        )
    }

    @Test
    fun `rejects gateway default non-private and non-canonical targets`() {
        fun admitted(
            host: String,
            prefix: Int = 24,
            hasGateway: Boolean = false,
            isUnicast: Boolean = true,
        ): Boolean =
            LocalIpv4RouteAdmission.allows(
                canonicalHost = host,
                destinationAddress = ipv4(192, 168, 50, 0),
                prefixLength = prefix,
                hasGateway = hasGateway,
                isUnicast = isUnicast,
                interfaceAddresses = listOf(ipv4(192, 168, 50, 10)),
            )

        assertFalse(admitted("192.168.50.44", hasGateway = true))
        assertFalse(admitted("192.168.50.44", isUnicast = false))
        assertFalse(admitted("192.168.50.44", prefix = 0))
        assertFalse(admitted("203.0.113.44"))
        assertFalse(admitted("192.168.050.44"))
        assertFalse(admitted("camera.invalid"))
    }

    @Test
    fun `socket factory revalidates the current route for each socket`() {
        val firstDelegate = RecordingSocketFactory()
        val secondDelegate = RecordingSocketFactory()
        val delegates = ArrayDeque(listOf(firstDelegate, secondDelegate))
        val visited = mutableListOf<String>()
        val factory =
            RevalidatingLocalNetworkSocketFactory("10.20.30.40") { host ->
                visited += host
                delegates.removeFirstOrNull()
            }

        factory.createSocket("10.20.30.40", 554).use { socket ->
            assertSame(firstDelegate.lastSocket, socket)
        }
        factory.createSocket(InetAddress.getByAddress(ipv4(10, 20, 30, 40)), 8554).use { socket ->
            assertSame(secondDelegate.lastSocket, socket)
        }

        assertEquals(listOf("10.20.30.40", "10.20.30.40"), visited)
        assertEquals(ipv4(10, 20, 30, 40).toList(), firstDelegate.lastAddress?.address?.toList())
    }

    @Test
    fun `socket factory fails closed when route disappears or target changes`() {
        var resolutions = 0
        val factory =
            RevalidatingLocalNetworkSocketFactory("172.20.30.40") {
                resolutions += 1
                null
            }

        val missingRoute =
            assertThrows(SocketException::class.java) {
                factory.createSocket("172.20.30.40", 554)
            }
        assertFalse(missingRoute.message.orEmpty().contains("172.20.30.40"))
        assertEquals(1, resolutions)

        val changedTarget =
            assertThrows(SocketException::class.java) {
                factory.createSocket("172.20.30.41", 554)
            }
        assertFalse(changedTarget.message.orEmpty().contains("172.20.30.41"))
        assertEquals(1, resolutions)

        assertThrows(SocketException::class.java) { factory.createSocket() }
        assertEquals(1, resolutions)
    }

    private class RecordingSocketFactory : SocketFactory() {
        var lastSocket: Socket? = null
            private set

        var lastAddress: InetAddress? = null
            private set

        override fun createSocket(): Socket = record(null)

        override fun createSocket(host: String, port: Int): Socket =
            throw AssertionError("String host overload must not be used")

        override fun createSocket(
            host: String,
            port: Int,
            localHost: InetAddress,
            localPort: Int,
        ): Socket = throw AssertionError("String host overload must not be used")

        override fun createSocket(host: InetAddress, port: Int): Socket = record(host)

        override fun createSocket(
            address: InetAddress,
            port: Int,
            localAddress: InetAddress,
            localPort: Int,
        ): Socket = record(address)

        private fun record(address: InetAddress?): Socket =
            Socket().also {
                lastAddress = address
                lastSocket = it
            }
    }

    private fun ipv4(first: Int, second: Int, third: Int, fourth: Int): ByteArray =
        byteArrayOf(first.toByte(), second.toByte(), third.toByte(), fourth.toByte())
}
