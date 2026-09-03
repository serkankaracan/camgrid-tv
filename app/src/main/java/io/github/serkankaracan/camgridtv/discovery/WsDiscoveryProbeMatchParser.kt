package io.github.serkankaracan.camgridtv.discovery

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import java.util.Locale
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import org.xml.sax.helpers.DefaultHandler

class WsDiscoveryProbeMatchParser(private val maximumPacketBytes: Int = MAXIMUM_UDP_PAYLOAD_BYTES) {
    fun parse(
        packet: ByteArray,
        sourceHost: String,
        length: Int = packet.size,
    ): WsDiscoveryParseResult {
        if (length !in 1..packet.size || length > maximumPacketBytes) {
            return WsDiscoveryParseResult.Rejected(WsDiscoveryRejection.PACKET_SIZE)
        }
        if (containsForbiddenDeclaration(packet, length)) {
            return WsDiscoveryParseResult.Rejected(WsDiscoveryRejection.UNSAFE_XML)
        }
        return try {
            val handler = ProbeMatchHandler(sourceHost)
            val parser = secureParserFactory().newSAXParser()
            runCatching { parser.setProperty(ACCESS_EXTERNAL_DTD_PROPERTY, "") }
            runCatching { parser.setProperty(ACCESS_EXTERNAL_SCHEMA_PROPERTY, "") }
            parser.xmlReader.entityResolver = { _, _ ->
                throw SAXException("External entities are disabled")
            }
            parser.parse(InputSource(ByteArrayInputStream(packet, 0, length)), handler)
            WsDiscoveryParseResult.Success(handler.matches.toList())
        } catch (_: FieldLimitException) {
            WsDiscoveryParseResult.Rejected(WsDiscoveryRejection.FIELD_SIZE)
        } catch (_: Exception) {
            WsDiscoveryParseResult.Rejected(WsDiscoveryRejection.MALFORMED_XML)
        }
    }

    private fun secureParserFactory(): SAXParserFactory =
        SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
            isValidating = false
            setFeatureSafely("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeatureSafely("http://xml.org/sax/features/external-general-entities", false)
            setFeatureSafely("http://xml.org/sax/features/external-parameter-entities", false)
            setFeatureSafely(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false,
            )
            runCatching { isXIncludeAware = false }
        }

    private fun SAXParserFactory.setFeatureSafely(feature: String, enabled: Boolean) {
        runCatching { setFeature(feature, enabled) }
    }

    private fun containsForbiddenDeclaration(packet: ByteArray, length: Int): Boolean {
        val text = String(packet, 0, length, StandardCharsets.ISO_8859_1).uppercase(Locale.ROOT)
        return "<!DOCTYPE" in text || "<!ENTITY" in text
    }

    private class ProbeMatchHandler(private val sourceHost: String) : DefaultHandler() {
        val matches = mutableListOf<ProbeMatch>()
        private val elements = ArrayDeque<Element>()
        private var current: MutableProbeMatch? = null

        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String?,
            attributes: Attributes?,
        ) {
            val name = localName?.takeIf(String::isNotBlank) ?: qName.orEmpty().substringAfter(':')
            elements.addLast(Element(name))
            if (name == "ProbeMatch") {
                if (current != null || matches.size >= MAX_MATCHES_PER_PACKET) {
                    throw FieldLimitException()
                }
                current = MutableProbeMatch()
            }
        }

        override fun characters(characters: CharArray, start: Int, length: Int) {
            val element = elements.peekLast() ?: return
            if (element.text.length + length > MAX_ELEMENT_CHARACTERS) throw FieldLimitException()
            element.text.append(characters, start, length)
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            val element = elements.removeLast()
            val parentName = elements.peekLast()?.name
            val value = element.text.toString().trim()
            current?.let { match ->
                when {
                    element.name == "Address" && parentName == "EndpointReference" -> {
                        match.endpointAddress = sanitize(value, MAX_ENDPOINT_CHARACTERS)
                    }
                    element.name == "XAddrs" -> match.xAddrs += splitValues(value, MAX_XADDRS)
                    element.name == "Scopes" -> match.scopes += splitValues(value, MAX_SCOPES)
                    element.name == "Types" -> match.types += splitValues(value, MAX_TYPES)
                    element.name == "MetadataVersion" ->
                        match.metadataVersion = value.toLongOrNull()
                }
            }
            if (element.name == "ProbeMatch") {
                val match = checkNotNull(current)
                matches +=
                    ProbeMatch(
                        endpointAddress = match.endpointAddress,
                        xAddrs = match.xAddrs.distinct(),
                        scopes = match.scopes.distinct(),
                        types = match.types.distinct(),
                        metadataVersion = match.metadataVersion,
                        sourceHost = sourceHost,
                    )
                current = null
            }
        }

        private fun splitValues(value: String, maximum: Int): List<String> {
            if (value.isBlank()) return emptyList()
            val values = value.split(WHITESPACE).filter(String::isNotBlank)
            if (values.size > maximum) throw FieldLimitException()
            return values.map { sanitize(it, MAX_VALUE_CHARACTERS) }
        }

        private fun sanitize(value: String, maximum: Int): String {
            if (value.length > maximum) throw FieldLimitException()
            return buildString(value.length) {
                    value.forEach { character ->
                        when {
                            character == '\t' || character == '\n' || character == '\r' ->
                                append(' ')
                            !character.isISOControl() -> append(character)
                        }
                    }
                }
                .replace(REPEATED_SPACES, " ")
                .trim()
        }
    }

    private data class Element(val name: String, val text: StringBuilder = StringBuilder())

    private data class MutableProbeMatch(
        var endpointAddress: String? = null,
        val xAddrs: MutableList<String> = mutableListOf(),
        val scopes: MutableList<String> = mutableListOf(),
        val types: MutableList<String> = mutableListOf(),
        var metadataVersion: Long? = null,
    )

    private class FieldLimitException : SAXException()

    private companion object {
        const val MAXIMUM_UDP_PAYLOAD_BYTES = 65_507
        const val MAX_MATCHES_PER_PACKET = 64
        const val MAX_ELEMENT_CHARACTERS = 16_384
        const val MAX_ENDPOINT_CHARACTERS = 512
        const val MAX_VALUE_CHARACTERS = 2_048
        const val MAX_XADDRS = 32
        const val MAX_SCOPES = 256
        const val MAX_TYPES = 64
        const val ACCESS_EXTERNAL_DTD_PROPERTY =
            "http://javax.xml.XMLConstants/property/accessExternalDTD"
        const val ACCESS_EXTERNAL_SCHEMA_PROPERTY =
            "http://javax.xml.XMLConstants/property/accessExternalSchema"
        val WHITESPACE = Regex("\\s+")
        val REPEATED_SPACES = Regex(" +")
    }
}

sealed interface WsDiscoveryParseResult {
    data class Success(val matches: List<ProbeMatch>) : WsDiscoveryParseResult

    data class Rejected(val reason: WsDiscoveryRejection) : WsDiscoveryParseResult
}

enum class WsDiscoveryRejection {
    PACKET_SIZE,
    FIELD_SIZE,
    UNSAFE_XML,
    MALFORMED_XML,
}
