package io.github.serkankaracan.camgridtv.playback

import io.github.serkankaracan.camgridtv.util.Rfc1918LocalAddressPolicy
import java.net.URI
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RtspUriFactoryTest {
    private val factory = RtspUriFactory()

    @Test
    fun `builds secondary stream with encoded RFC 3986 user info`() {
        val uri =
            factory.create(
                username = "viewer name",
                password = "p@ss:/%# ş",
                host = "192.168.1.25",
                port = 8554,
                stream = RtspStream.SECONDARY,
            )

        val parsed = URI(uri.valueForPlayback())
        assertEquals("viewer%20name:p%40ss%3A%2F%25%23%20%C5%9F", parsed.rawUserInfo)
        assertEquals("192.168.1.25", parsed.host)
        assertEquals(8554, parsed.port)
        assertEquals("/stream2", parsed.path)
        assertEquals(RtspStream.SECONDARY, uri.stream)
    }

    @Test
    fun `preserves only unreserved user info bytes`() {
        val uri =
            factory.create(
                username = "AZaz09-._~",
                password = "safe-value_2~",
                host = "10.0.0.4",
                stream = RtspStream.PRIMARY,
            )

        assertEquals("AZaz09-._~:safe-value_2~", URI(uri.valueForPlayback()).rawUserInfo)
        assertEquals("/stream1", URI(uri.valueForPlayback()).path)
    }

    @Test
    fun `ordinary string representation never exposes credentials`() {
        val username = "private-user"
        val password = "private-password"
        val uri =
            factory.create(
                username = username,
                password = password,
                host = "172.16.5.9",
                stream = RtspStream.SECONDARY,
            )

        assertFalse(uri.toString().contains(username))
        assertFalse(uri.toString().contains(password))
        assertFalse(uri.redactedValue.contains(username))
        assertFalse(uri.redactedValue.contains(password))
        assertTrue(uri.redactedValue.contains("***"))
        assertTrue(uri.redactedValue.endsWith("/stream2"))
    }

    @Test
    fun `accepts every RFC 1918 range`() {
        listOf("10.255.255.255", "172.16.0.1", "172.31.255.254", "192.168.0.1").forEach { host ->
            val parsed =
                URI(
                    factory
                        .create("viewer", "password", host, stream = RtspStream.SECONDARY)
                        .valueForPlayback()
                )
            assertEquals(host, parsed.host)
        }
    }

    @Test
    fun `rejects public link local loopback hostname IPv6 and malformed hosts`() {
        val rejectedHosts =
            listOf(
                "8.8.8.8",
                "169.254.1.2",
                "127.0.0.1",
                "camera.local",
                "[fd00::1]",
                "192.168.001.1",
                "192.168.1",
                "192.168.1.256",
                " 192.168.1.2",
            )

        rejectedHosts.forEach { host ->
            val failure =
                assertThrows(IllegalArgumentException::class.java) {
                    factory.create("viewer", "password", host, stream = RtspStream.SECONDARY)
                }
            assertFalse(failure.message.orEmpty().contains(host))
        }
    }

    @Test
    fun `rejects invalid ports and credential controls`() {
        listOf(0, 65_536).forEach { port ->
            assertThrows(IllegalArgumentException::class.java) {
                factory.create(
                    username = "viewer",
                    password = "password",
                    host = "192.168.1.2",
                    port = port,
                    stream = RtspStream.SECONDARY,
                )
            }
        }

        assertThrows(IllegalArgumentException::class.java) {
            factory.create(
                username = "viewer\nname",
                password = "password",
                host = "192.168.1.2",
                stream = RtspStream.SECONDARY,
            )
        }
    }

    @Test
    fun `optional route gate rejects private address outside current network`() {
        val visited = mutableListOf<String>()
        val routedFactory =
            RtspUriFactory(
                Rfc1918LocalAddressPolicy { canonicalHost ->
                    visited += canonicalHost
                    canonicalHost == "192.168.50.7"
                }
            )

        routedFactory.create(
            "viewer",
            "password",
            "192.168.50.7",
            stream = RtspStream.SECONDARY,
        )
        assertThrows(IllegalArgumentException::class.java) {
            routedFactory.create(
                "viewer",
                "password",
                "192.168.60.7",
                stream = RtspStream.SECONDARY,
            )
        }
        assertEquals(listOf("192.168.50.7", "192.168.60.7"), visited)
    }
}
