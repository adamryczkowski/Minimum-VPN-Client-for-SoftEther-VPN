package kittoku.mvc.autoconnect

/**
 * Represents the current network connectivity state.
 */
sealed class NetworkState {
    /**
     * Device is not connected to any network.
     */
    data object Disconnected : NetworkState()

    /**
     * Device is connected to a network.
     *
     * @property type The type of network connection
     * @property ssid The SSID of the WiFi network, if applicable and available
     */
    data class Connected(
        val type: NetworkType,
        val ssid: String? = null,
    ) : NetworkState()
}

/**
 * Types of network connections.
 */
enum class NetworkType {
    WIFI,
    CELLULAR,
    ETHERNET,
    UNKNOWN,
}
