package io.github.serkankaracan.camgridtv.discovery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalConnectivityClassifierTest {
    @Test
    fun `reports simultaneous Wi-Fi and Ethernet local networks`() {
        val state =
            LocalConnectivityClassifier.classify(
                listOf(
                    observation(wifi = true),
                    observation(ethernet = true),
                )
            )

        assertTrue(state.isLocalNetworkAvailable)
        assertEquals(
            setOf(LocalNetworkTransport.WIFI, LocalNetworkTransport.ETHERNET),
            state.transports,
        )
        assertEquals(2, state.eligibleNetworkCount)
    }

    @Test
    fun `does not treat VPN cellular or an addressless transport as local availability`() {
        val state =
            LocalConnectivityClassifier.classify(
                listOf(
                    observation(wifi = true, notVpn = false),
                    observation(wifi = true, hasAddress = false),
                    observation(),
                )
            )

        assertFalse(state.isLocalNetworkAvailable)
        assertTrue(state.transports.isEmpty())
        assertEquals(0, state.eligibleNetworkCount)
    }

    @Test
    fun `an empty platform snapshot is safely offline`() {
        assertEquals(LocalConnectivityState(), LocalConnectivityClassifier.classify(emptyList()))
    }

    private fun observation(
        wifi: Boolean = false,
        ethernet: Boolean = false,
        notVpn: Boolean = true,
        hasAddress: Boolean = true,
    ) =
        LocalNetworkObservation(
            hasWifiTransport = wifi,
            hasEthernetTransport = ethernet,
            isNotVpn = notVpn,
            hasLinkAddress = hasAddress,
        )
}
