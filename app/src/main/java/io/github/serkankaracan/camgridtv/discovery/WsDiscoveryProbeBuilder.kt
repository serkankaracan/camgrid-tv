package io.github.serkankaracan.camgridtv.discovery

import java.util.UUID

class WsDiscoveryProbeBuilder(private val messageIdFactory: () -> UUID = UUID::randomUUID) {
    fun build(version: WsDiscoveryVersion): ByteArray {
        val messageId = messageIdFactory()
        val xml =
            """<?xml version="1.0" encoding="UTF-8"?>
            |<s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope"
            | xmlns:a="${version.addressingNamespace}"
            | xmlns:d="${version.discoveryNamespace}"
            | xmlns:dn="http://www.onvif.org/ver10/network/wsdl">
            | <s:Header>
            |  <a:Action s:mustUnderstand="1">${version.probeAction}</a:Action>
            |  <a:MessageID>urn:uuid:$messageId</a:MessageID>
            |  <a:To s:mustUnderstand="1">${version.discoveryUrn}</a:To>
            | </s:Header>
            | <s:Body><d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types></d:Probe></s:Body>
            |</s:Envelope>
        """
                .trimMargin()
        return xml.toByteArray(Charsets.UTF_8)
    }
}

enum class WsDiscoveryVersion(
    val addressingNamespace: String,
    val discoveryNamespace: String,
    val discoveryUrn: String,
) {
    APRIL_2005(
        addressingNamespace = "http://schemas.xmlsoap.org/ws/2004/08/addressing",
        discoveryNamespace = "http://schemas.xmlsoap.org/ws/2005/04/discovery",
        discoveryUrn = "urn:schemas-xmlsoap-org:ws:2005:04:discovery",
    ),
    JANUARY_2009(
        addressingNamespace = "http://www.w3.org/2005/08/addressing",
        discoveryNamespace = "http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01",
        discoveryUrn = "urn:docs-oasis-open-org:ws-dd:ns:discovery:2009:01",
    );

    val probeAction: String = "$discoveryNamespace/Probe"
}
