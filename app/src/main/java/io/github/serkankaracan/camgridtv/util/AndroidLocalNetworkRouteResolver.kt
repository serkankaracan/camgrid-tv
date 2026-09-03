package io.github.serkankaracan.camgridtv.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.RouteInfo
import android.os.Build
import java.net.Inet4Address
import javax.net.SocketFactory

/**
 * Selects only current, non-VPN Wi-Fi/Ethernet networks with an on-link route to the target.
 *
 * Nothing is cached: both Media3 engine creation and each socket creation re-read Android's current
 * Network/LinkProperties snapshot. A lost or changed Network therefore fails closed instead of
 * falling back to the process default route.
 */
class AndroidLocalNetworkRouteResolver(context: Context) : LocalNetworkRouteResolver {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE)
            as ConnectivityManager

    fun hasActiveRoute(canonicalHost: String): Boolean =
        currentNetworkSocketFactory(canonicalHost) != null

    override fun socketFactoryFor(canonicalHost: String): SocketFactory? {
        if (!hasActiveRoute(canonicalHost)) return null
        return RevalidatingLocalNetworkSocketFactory(
            canonicalHost = canonicalHost,
            currentDelegate = ::currentNetworkSocketFactory,
        )
    }

    // Android does not expose a callback-backed atomic snapshot for all eligible local transports.
    @Suppress("DEPRECATION")
    private fun currentNetworkSocketFactory(canonicalHost: String): SocketFactory? {
        return try {
            val target = parseCanonicalIpv4(canonicalHost)
            if (target == null || !isRfc1918(target)) {
                null
            } else {
                connectivityManager.allNetworks.firstNotNullOfOrNull { network ->
                    if (hasMatchingActiveRoute(network, canonicalHost)) {
                        network.socketFactory
                    } else {
                        null
                    }
                }
            }
        } catch (_: SecurityException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun hasMatchingActiveRoute(network: Network, canonicalHost: String): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)) return false
        if (
            !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                !capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        ) {
            return false
        }

        val linkProperties = connectivityManager.getLinkProperties(network) ?: return false
        val interfaceAddresses =
            linkProperties.linkAddresses.mapNotNull { linkAddress ->
                (linkAddress.address as? Inet4Address)?.address
            }
        if (interfaceAddresses.isEmpty()) return false

        return linkProperties.routes.any { route ->
            val destination = route.destination
            val destinationAddress =
                (destination.address as? Inet4Address)?.address ?: return@any false
            LocalIpv4RouteAdmission.allows(
                canonicalHost = canonicalHost,
                destinationAddress = destinationAddress,
                prefixLength = destination.prefixLength,
                hasGateway = route.hasGateway(),
                isUnicast =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        route.type == RouteInfo.RTN_UNICAST,
                interfaceAddresses = interfaceAddresses,
            )
        }
    }
}
