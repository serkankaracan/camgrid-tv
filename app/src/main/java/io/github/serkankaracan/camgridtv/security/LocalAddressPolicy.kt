package io.github.serkankaracan.camgridtv.security

import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress

/**
 * Rejects hostnames and public addresses without performing DNS. Runtime callers should use the
 * route-aware overload so a private address is accepted only when it is on an active local route.
 */
class LocalAddressPolicy {
    fun isPotentiallyLocalLiteral(host: String): Boolean {
        val address = parseLiteral(host) ?: return false
        val bytes = address.address
        return when (address) {
            is Inet4Address -> {
                val first = bytes[0].unsigned()
                val second = bytes[1].unsigned()
                first == 10 ||
                    (first == 172 && second in 16..31) ||
                    (first == 192 && second == 168) ||
                    (first == 169 && second == 254)
            }
            is Inet6Address -> {
                val first = bytes[0].unsigned()
                val second = bytes[1].unsigned()
                (first and 0xFE) == 0xFC || (first == 0xFE && (second and 0xC0) == 0x80)
            }
            else -> false
        }
    }

    fun isAllowed(host: String, connectedRoutes: Collection<CidrBlock>): Boolean =
        isPotentiallyLocalLiteral(host) && connectedRoutes.any { it.contains(host) }

    internal fun parseLiteral(host: String): InetAddress? {
        val unwrapped = host.trim().removeSurrounding("[", "]")
        val withoutZone = unwrapped.substringBefore('%')
        if (withoutZone.isEmpty()) return null
        return if (withoutZone.contains(':')) {
            if (!IPV6_LITERAL.matches(withoutZone)) return null
            runCatching { InetAddress.getByName(withoutZone) }
                .getOrNull()
                ?.takeIf { it is Inet6Address }
        } else {
            parseIpv4(withoutZone)?.let { InetAddress.getByAddress(it) }
        }
    }

    private fun parseIpv4(value: String): ByteArray? {
        val parts = value.split('.')
        if (parts.size != 4) return null
        val octets = parts.map { part ->
            if (part.isEmpty() || part.length > 3 || !part.all(Char::isDigit)) return null
            if (part.length > 1 && part.startsWith('0')) return null
            part.toIntOrNull()?.takeIf { it in 0..255 } ?: return null
        }
        return ByteArray(4) { octets[it].toByte() }
    }

    private companion object {
        val IPV6_LITERAL = Regex("[0-9a-fA-F:.]+")
    }
}

class CidrBlock
private constructor(
    private val networkBytes: ByteArray,
    private val prefixLength: Int,
) {
    fun contains(host: String): Boolean {
        val candidate = LocalAddressPolicy().parseLiteral(host)?.address ?: return false
        if (candidate.size != networkBytes.size) return false
        val wholeBytes = prefixLength / 8
        val remainingBits = prefixLength % 8
        for (index in 0 until wholeBytes) {
            if (candidate[index] != networkBytes[index]) return false
        }
        if (remainingBits == 0) return true
        val mask = (0xFF shl (8 - remainingBits)) and 0xFF
        return (candidate[wholeBytes].unsigned() and mask) ==
            (networkBytes[wholeBytes].unsigned() and mask)
    }

    companion object {
        fun parse(value: String): CidrBlock {
            val addressPart = value.substringBefore('/')
            val address =
                LocalAddressPolicy().parseLiteral(addressPart)
                    ?: throw IllegalArgumentException("CIDR address is not a literal IP address")
            val maximumPrefix = address.address.size * 8
            val prefix =
                value
                    .substringAfter('/', missingDelimiterValue = maximumPrefix.toString())
                    .toIntOrNull() ?: throw IllegalArgumentException("CIDR prefix is invalid")
            require(prefix in 0..maximumPrefix) { "CIDR prefix is invalid" }
            val masked = address.address.copyOf()
            val wholeBytes = prefix / 8
            val remainingBits = prefix % 8
            if (remainingBits != 0 && wholeBytes < masked.size) {
                val mask = (0xFF shl (8 - remainingBits)) and 0xFF
                masked[wholeBytes] = (masked[wholeBytes].unsigned() and mask).toByte()
            }
            for (index in (wholeBytes + if (remainingBits == 0) 0 else 1) until masked.size) {
                masked[index] = 0
            }
            return CidrBlock(masked, prefix)
        }
    }
}

private fun Byte.unsigned(): Int = toInt() and 0xFF
