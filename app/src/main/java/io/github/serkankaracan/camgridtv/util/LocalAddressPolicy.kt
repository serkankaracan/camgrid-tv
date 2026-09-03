package io.github.serkankaracan.camgridtv.util

/**
 * Validates a host without resolving it through DNS and returns its canonical representation.
 *
 * Implementations must throw a value-independent exception message. A rejected host can be
 * sensitive local-network information and must not be copied into diagnostics.
 */
fun interface LocalAddressPolicy {
    fun requireAllowed(host: String): String
}

/**
 * Restricts connections to canonical IPv4 literals in the RFC 1918 private address ranges.
 *
 * [currentNetworkMembership] is an optional second gate for an Android adapter backed by the active
 * network's routes. It must not perform DNS resolution. Keeping that check injectable makes the URI
 * factory deterministic and JVM-testable while allowing the app layer to reject a private address
 * that is not on the current local network.
 */
class Rfc1918LocalAddressPolicy(
    private val currentNetworkMembership: (canonicalHost: String) -> Boolean = { true }
) : LocalAddressPolicy {
    override fun requireAllowed(host: String): String {
        val address = parseCanonicalIpv4(host)
        require(address != null && isRfc1918(address)) { INVALID_HOST_MESSAGE }

        val canonicalHost = canonicalIpv4(address)
        require(currentNetworkMembership(canonicalHost)) { INVALID_HOST_MESSAGE }
        return canonicalHost
    }

    private companion object {
        const val INVALID_HOST_MESSAGE = "RTSP host must be a canonical local IPv4 address"
    }
}

internal fun parseCanonicalIpv4(host: String): ByteArray? {
    if (host.isEmpty() || host != host.trim()) return null
    val components = host.split('.', limit = IPV4_COMPONENT_COUNT + 1)
    if (components.size != IPV4_COMPONENT_COUNT) return null

    val address = ByteArray(IPV4_COMPONENT_COUNT)
    components.forEachIndexed { index, component ->
        if (
            component.isEmpty() ||
                component.length > MAX_IPV4_COMPONENT_LENGTH ||
                !component.all(Char::isDigit) ||
                (component.length > 1 && component.first() == '0')
        ) {
            return null
        }
        val value = component.toIntOrNull()?.takeIf { it in IPV4_OCTET_RANGE } ?: return null
        address[index] = value.toByte()
    }
    return address
}

internal fun isRfc1918(address: ByteArray): Boolean {
    if (address.size != IPV4_COMPONENT_COUNT) return false
    val first = address[0].toInt() and 0xff
    val second = address[1].toInt() and 0xff
    return first == 10 || (first == 172 && second in 16..31) || (first == 192 && second == 168)
}

internal fun canonicalIpv4(address: ByteArray): String =
    address.joinToString(separator = ".") { (it.toInt() and 0xff).toString() }

private const val IPV4_COMPONENT_COUNT = 4
private const val MAX_IPV4_COMPONENT_LENGTH = 3
private val IPV4_OCTET_RANGE = 0..255
