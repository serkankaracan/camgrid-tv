package io.github.serkankaracan.camgridtv.util

import java.nio.charset.StandardCharsets

/** Percent-encodes one raw URI user-info component using UTF-8 and RFC 3986 rules. */
object Rfc3986UserInfoEncoder {
    private const val HEX = "0123456789ABCDEF"

    fun encode(rawValue: String): String = buildString {
        rawValue.toByteArray(StandardCharsets.UTF_8).forEach { byte ->
            val value = byte.toInt() and 0xFF
            if (isUnreserved(value)) {
                append(value.toChar())
            } else {
                append('%')
                append(HEX[value ushr 4])
                append(HEX[value and 0x0F])
            }
        }
    }

    private fun isUnreserved(value: Int): Boolean =
        value in 'a'.code..'z'.code ||
            value in 'A'.code..'Z'.code ||
            value in '0'.code..'9'.code ||
            value == '-'.code ||
            value == '.'.code ||
            value == '_'.code ||
            value == '~'.code
}
