package io.github.serkankaracan.camgridtv.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

interface ConnectivityMonitor {
    /** A cold flow; each collector owns and unregisters its Android network callback. */
    fun observe(): Flow<LocalConnectivityState>
}

class AndroidConnectivityMonitor(context: Context) : ConnectivityMonitor {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

    override fun observe(): Flow<LocalConnectivityState> =
        callbackFlow {
                var callbackRegistered = false

                fun publishSnapshot() {
                    val state =
                        try {
                            readCurrentState()
                        } catch (_: SecurityException) {
                            LocalConnectivityState(
                                monitorIssue = ConnectivityMonitorIssue.PERMISSION_DENIED
                            )
                        } catch (_: RuntimeException) {
                            LocalConnectivityState(
                                monitorIssue = ConnectivityMonitorIssue.MONITOR_UNAVAILABLE
                            )
                        }
                    trySend(state)
                }

                val callback =
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onAvailable(network: Network) = publishSnapshot()

                        override fun onLost(network: Network) = publishSnapshot()

                        override fun onCapabilitiesChanged(
                            network: Network,
                            networkCapabilities: NetworkCapabilities,
                        ) = publishSnapshot()

                        override fun onLinkPropertiesChanged(
                            network: Network,
                            linkProperties: LinkProperties,
                        ) = publishSnapshot()
                    }

                val request =
                    NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                        .build()
                try {
                    connectivityManager.registerNetworkCallback(request, callback)
                    callbackRegistered = true
                    publishSnapshot()
                } catch (_: SecurityException) {
                    trySend(
                        LocalConnectivityState(
                            monitorIssue = ConnectivityMonitorIssue.PERMISSION_DENIED
                        )
                    )
                    close()
                } catch (_: RuntimeException) {
                    trySend(
                        LocalConnectivityState(
                            monitorIssue = ConnectivityMonitorIssue.MONITOR_UNAVAILABLE
                        )
                    )
                    close()
                }

                awaitClose {
                    if (callbackRegistered) {
                        runCatching { connectivityManager.unregisterNetworkCallback(callback) }
                    }
                }
            }
            .distinctUntilChanged()
            .conflate()

    // The callback handles future changes; this deprecated snapshot seeds its initial state.
    @Suppress("DEPRECATION")
    private fun readCurrentState(): LocalConnectivityState {
        val observations =
            connectivityManager.allNetworks.mapNotNull { network ->
                val capabilities =
                    connectivityManager.getNetworkCapabilities(network) ?: return@mapNotNull null
                val linkProperties = connectivityManager.getLinkProperties(network)
                LocalNetworkObservation(
                    hasWifiTransport =
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                    hasEthernetTransport =
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET),
                    isNotVpn =
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN),
                    hasLinkAddress = linkProperties?.linkAddresses?.isNotEmpty() == true,
                )
            }
        return LocalConnectivityClassifier.classify(observations)
    }
}

object LocalConnectivityClassifier {
    fun classify(observations: Iterable<LocalNetworkObservation>): LocalConnectivityState {
        val eligible = observations.filter { observation ->
            observation.isNotVpn &&
                observation.hasLinkAddress &&
                (observation.hasWifiTransport || observation.hasEthernetTransport)
        }
        val transports = buildSet {
            eligible.forEach { observation ->
                if (observation.hasWifiTransport) add(LocalNetworkTransport.WIFI)
                if (observation.hasEthernetTransport) add(LocalNetworkTransport.ETHERNET)
            }
        }
        return LocalConnectivityState(
            transports = transports,
            eligibleNetworkCount = eligible.size,
        )
    }
}

data class LocalNetworkObservation(
    val hasWifiTransport: Boolean,
    val hasEthernetTransport: Boolean,
    val isNotVpn: Boolean,
    val hasLinkAddress: Boolean,
)

data class LocalConnectivityState(
    val transports: Set<LocalNetworkTransport> = emptySet(),
    val eligibleNetworkCount: Int = 0,
    val monitorIssue: ConnectivityMonitorIssue? = null,
) {
    init {
        require(eligibleNetworkCount >= 0) { "Eligible network count is invalid" }
    }

    val isLocalNetworkAvailable: Boolean = eligibleNetworkCount > 0 && transports.isNotEmpty()
}

enum class LocalNetworkTransport {
    WIFI,
    ETHERNET,
}

enum class ConnectivityMonitorIssue {
    PERMISSION_DENIED,
    MONITOR_UNAVAILABLE,
}
