package kittoku.mvc.autoconnect

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors network connectivity changes using ConnectivityManager.
 *
 * This class provides:
 * - A StateFlow of the current network state
 * - Methods to check connectivity status
 * - Network type detection (WiFi, Cellular, Ethernet)
 */
class NetworkMonitor(
    private val connectivityManager: ConnectivityManager,
) {
    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Disconnected)

    /**
     * StateFlow of the current network state.
     */
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var currentNetwork: Network? = null
    private var currentCapabilities: NetworkCapabilities? = null

    /**
     * Starts monitoring network connectivity changes.
     * Call [stopMonitoring] when monitoring is no longer needed.
     */
    fun startMonitoring() {
        if (networkCallback != null) return

        val callback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    currentNetwork = network
                    updateNetworkState()
                }

                override fun onLost(network: Network) {
                    if (currentNetwork == network) {
                        currentNetwork = null
                        currentCapabilities = null
                        _networkState.value = NetworkState.Disconnected
                    }
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    if (currentNetwork == network) {
                        currentCapabilities = networkCapabilities
                        updateNetworkState()
                    }
                }
            }

        networkCallback = callback
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    /**
     * Stops monitoring network connectivity changes.
     */
    fun stopMonitoring() {
        networkCallback?.let { callback ->
            connectivityManager.unregisterNetworkCallback(callback)
            networkCallback = null
        }
    }

    /**
     * Returns whether the device is currently connected to a network.
     */
    fun isConnected(): Boolean = _networkState.value is NetworkState.Connected

    /**
     * Returns the current network type, or null if not connected.
     */
    fun getCurrentNetworkType(): NetworkType? {
        val state = _networkState.value
        return if (state is NetworkState.Connected) state.type else null
    }

    private fun updateNetworkState() {
        val network = currentNetwork
        if (network == null) {
            _networkState.value = NetworkState.Disconnected
            return
        }

        val capabilities = currentCapabilities
        val networkType =
            when {
                capabilities == null -> NetworkType.UNKNOWN
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                else -> NetworkType.UNKNOWN
            }

        _networkState.value = NetworkState.Connected(type = networkType)
    }
}
