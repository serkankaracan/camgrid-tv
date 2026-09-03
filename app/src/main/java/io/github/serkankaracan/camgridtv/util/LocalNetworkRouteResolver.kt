package io.github.serkankaracan.camgridtv.util

import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import javax.net.SocketFactory

/** Resolves a fresh socket factory only when [canonicalHost] is on an active local route. */
fun interface LocalNetworkRouteResolver {
    fun socketFactoryFor(canonicalHost: String): SocketFactory?
}

/**
 * Pure route admission shared by the Android adapter and JVM tests.
 *
 * A route is local only when it is an on-link IPv4 CIDR (no gateway), has an interface IPv4 address
 * in that CIDR and contains the canonical RFC 1918 target. A default route is never enough.
 */
internal object LocalIpv4RouteAdmission {
    fun allows(
        canonicalHost: String,
        destinationAddress: ByteArray,
        prefixLength: Int,
        hasGateway: Boolean,
        isUnicast: Boolean,
        interfaceAddresses: Iterable<ByteArray>,
    ): Boolean {
        val target = parseCanonicalIpv4(canonicalHost) ?: return false
        if (
            !isRfc1918(target) ||
                hasGateway ||
                !isUnicast ||
                prefixLength !in MIN_PREFIX_LENGTH..MAX_PREFIX_LENGTH
        ) {
            return false
        }
        val subnet = Ipv4Subnet.create(destinationAddress, prefixLength) ?: return false
        return subnet.contains(target) &&
            interfaceAddresses.any { address -> isRfc1918(address) && subnet.contains(address) }
    }

    private const val MIN_PREFIX_LENGTH = 1
    private const val MAX_PREFIX_LENGTH = 32
}

internal class Ipv4Subnet
private constructor(
    private val network: Int,
    private val mask: Int,
) {
    fun contains(address: ByteArray): Boolean =
        address.size == IPV4_BYTE_COUNT && (address.toIpv4Int() and mask) == network

    companion object {
        fun create(address: ByteArray, prefixLength: Int): Ipv4Subnet? {
            if (address.size != IPV4_BYTE_COUNT || prefixLength !in 0..IPV4_BIT_COUNT) return null
            val mask = if (prefixLength == 0) 0 else -1 shl (IPV4_BIT_COUNT - prefixLength)
            return Ipv4Subnet(network = address.toIpv4Int() and mask, mask = mask)
        }

        private const val IPV4_BYTE_COUNT = 4
        private const val IPV4_BIT_COUNT = 32
    }
}

/**
 * Re-resolves the Android [SocketFactory] for every socket creation. This deliberately avoids
 * retaining a factory for a Network whose routes may have changed between URI admission and Media3
 * opening its RTSP control connection.
 */
internal class RevalidatingLocalNetworkSocketFactory(
    canonicalHost: String,
    private val currentDelegate: (canonicalHost: String) -> SocketFactory?,
) : SocketFactory() {
    private val expectedAddress =
        requireNotNull(parseCanonicalIpv4(canonicalHost)) { INVALID_TARGET_MESSAGE }
    private val expectedHost = canonicalIpv4(expectedAddress)
    private val targetAddress = InetAddress.getByAddress(expectedAddress)

    // Media3 uses createSocket(String, int). Refuse unaddressed sockets so another caller cannot
    // obtain a bound socket and later connect it to a destination that bypasses the host pin.
    override fun createSocket(): Socket = throw SocketException(INVALID_TARGET_MESSAGE)

    override fun createSocket(host: String, port: Int): Socket {
        requireExpectedHost(host)
        return requireCurrentDelegate().createSocket(targetAddress, port)
    }

    override fun createSocket(
        host: String,
        port: Int,
        localHost: InetAddress,
        localPort: Int,
    ): Socket {
        requireExpectedHost(host)
        return requireCurrentDelegate().createSocket(targetAddress, port, localHost, localPort)
    }

    override fun createSocket(host: InetAddress, port: Int): Socket {
        requireExpectedHost(host)
        return requireCurrentDelegate().createSocket(targetAddress, port)
    }

    override fun createSocket(
        address: InetAddress,
        port: Int,
        localAddress: InetAddress,
        localPort: Int,
    ): Socket {
        requireExpectedHost(address)
        return requireCurrentDelegate().createSocket(targetAddress, port, localAddress, localPort)
    }

    private fun requireCurrentDelegate(): SocketFactory =
        currentDelegate(expectedHost) ?: throw SocketException(NO_ROUTE_MESSAGE)

    private fun requireExpectedHost(host: String) {
        val candidate = parseCanonicalIpv4(host)
        if (candidate == null || !candidate.contentEquals(expectedAddress)) {
            throw SocketException(INVALID_TARGET_MESSAGE)
        }
    }

    private fun requireExpectedHost(host: InetAddress) {
        if (!host.address.contentEquals(expectedAddress)) {
            throw SocketException(INVALID_TARGET_MESSAGE)
        }
    }

    private companion object {
        const val NO_ROUTE_MESSAGE = "No active local network route is available"
        const val INVALID_TARGET_MESSAGE = "RTSP socket target is invalid"
    }
}

private fun ByteArray.toIpv4Int(): Int =
    fold(0) { result, byte -> (result shl Byte.SIZE_BITS) or (byte.toInt() and 0xff) }
