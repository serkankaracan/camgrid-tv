package io.github.serkankaracan.camgridtv.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class AndroidMulticastWsDiscoveryClient(
    context: Context,
    private val probeBuilder: WsDiscoveryProbeBuilder = WsDiscoveryProbeBuilder(),
    private val parser: WsDiscoveryProbeMatchParser = WsDiscoveryProbeMatchParser(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val monotonicNanos: () -> Long = System::nanoTime,
) : WsDiscoveryClient {
    private val applicationContext = context.applicationContext
    private val connectivityManager =
        applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    override fun discover(options: DiscoveryOptions): Flow<DiscoveryEvent> = channelFlow {
        launch(ioDispatcher) {
            val targets = activeLocalNetworks()
            if (targets.isEmpty()) {
                send(DiscoveryEvent.Issue(DiscoveryIssue.NO_ACTIVE_LOCAL_NETWORK))
                return@launch
            }

            val multicastLock =
                if (targets.any(NetworkTarget::isWifi)) {
                    wifiManager?.createMulticastLock(MULTICAST_LOCK_TAG)?.apply {
                        setReferenceCounted(false)
                    }
                } else {
                    null
                }
            try {
                multicastLock?.acquire()
                coroutineScope {
                    targets
                        .map { target ->
                            launch {
                                try {
                                    scanNetwork(target.network, options) { event -> send(event) }
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: SecurityException) {
                                    send(DiscoveryEvent.Issue(DiscoveryIssue.PERMISSION_REQUIRED))
                                } catch (_: Exception) {
                                    send(DiscoveryEvent.Issue(DiscoveryIssue.NETWORK_LOST))
                                }
                            }
                        }
                        .joinAll()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: SecurityException) {
                send(DiscoveryEvent.Issue(DiscoveryIssue.PERMISSION_REQUIRED))
            } catch (_: Exception) {
                send(DiscoveryEvent.Issue(DiscoveryIssue.TRANSPORT_UNAVAILABLE))
            } finally {
                if (multicastLock?.isHeld == true) multicastLock.release()
            }
        }
    }

    private suspend fun scanNetwork(
        network: Network,
        options: DiscoveryOptions,
        emit: suspend (DiscoveryEvent) -> Unit,
    ) {
        val deadline = monotonicNanos() + TimeUnit.MILLISECONDS.toNanos(options.totalTimeoutMillis)
        val multicastAddress = InetAddress.getByName(MULTICAST_HOST)
        DatagramSocket(null).use { socket ->
            socket.reuseAddress = true
            network.bindSocket(socket)
            socket.bind(InetSocketAddress(0))
            socket.soTimeout = options.receivePollMillis

            repeat(options.attempts) { attemptIndex ->
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                WsDiscoveryVersion.entries.forEach { version ->
                    val probe = probeBuilder.build(version)
                    socket.send(DatagramPacket(probe, probe.size, multicastAddress, MULTICAST_PORT))
                }

                val now = monotonicNanos()
                val attemptsRemaining = options.attempts - attemptIndex
                val receiveDeadline =
                    minOf(deadline, now + ((deadline - now).coerceAtLeast(0L) / attemptsRemaining))
                while (monotonicNanos() < receiveDeadline) {
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    val buffer = ByteArray(MAXIMUM_UDP_PAYLOAD_BYTES)
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        socket.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        continue
                    }
                    val sourceHost = packet.address.hostAddress ?: continue
                    when (val result = parser.parse(packet.data, sourceHost, packet.length)) {
                        is WsDiscoveryParseResult.Success -> {
                            result.matches.forEach { match -> emit(DiscoveryEvent.Match(match)) }
                        }
                        is WsDiscoveryParseResult.Rejected -> Unit
                    }
                }
            }
        }
    }

    // A point-in-time scan needs every eligible transport; the platform has no snapshot
    // replacement for getAllNetworks, while callbacks are used for ongoing monitoring.
    @Suppress("DEPRECATION")
    private fun activeLocalNetworks(): List<NetworkTarget> =
        connectivityManager.allNetworks.mapNotNull { network ->
            val capabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return@mapNotNull null
            if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN))
                return@mapNotNull null
            val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val isEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            if (!isWifi && !isEthernet) return@mapNotNull null
            NetworkTarget(network, isWifi)
        }

    private data class NetworkTarget(val network: Network, val isWifi: Boolean)

    private companion object {
        const val MULTICAST_HOST = "239.255.255.250"
        const val MULTICAST_PORT = 3702
        const val MAXIMUM_UDP_PAYLOAD_BYTES = 65_507
        const val MULTICAST_LOCK_TAG = "CamGridTV:WsDiscovery"
    }
}
