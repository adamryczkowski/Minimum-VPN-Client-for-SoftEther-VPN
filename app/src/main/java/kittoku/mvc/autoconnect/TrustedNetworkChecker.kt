package kittoku.mvc.autoconnect

/**
 * Checks if the current network is trusted and determines
 * whether VPN should connect based on trusted network settings.
 *
 * When trusted networks feature is enabled, VPN will not auto-connect
 * when the device is connected to a trusted WiFi network (e.g., home WiFi).
 */
class TrustedNetworkChecker(
    private val autoConnectSettings: AutoConnectSettings,
    private val wifiSsidProvider: WifiSsidProvider,
    private val networkMonitor: NetworkMonitor,
) {
    companion object {
        private const val UNKNOWN_SSID = "<unknown ssid>"
    }

    /**
     * Determines if VPN should connect based on trusted network settings.
     *
     * @return true if VPN should connect, false if current network is trusted
     */
    fun shouldConnectVpn(): Boolean {
        // If trusted networks feature is disabled, always allow connection
        if (!autoConnectSettings.isTrustedNetworksEnabled()) {
            return true
        }

        // If not connected to WiFi, allow connection
        val networkType = networkMonitor.getCurrentNetworkType()
        if (networkType != NetworkType.WIFI) {
            return true
        }

        // Get current SSID
        val ssid = wifiSsidProvider.getCurrentSsid()

        // If SSID is null or unknown, allow connection (safer default)
        if (ssid == null || ssid == UNKNOWN_SSID) {
            return true
        }

        // Check if current network is trusted
        return !autoConnectSettings.isNetworkTrusted(ssid)
    }

    /**
     * Checks if the current network is in the trusted networks list.
     *
     * @return true if current WiFi network is trusted, false otherwise
     */
    fun isCurrentNetworkTrusted(): Boolean {
        // Must be connected to WiFi
        val networkType = networkMonitor.getCurrentNetworkType()
        if (networkType != NetworkType.WIFI) {
            return false
        }

        // Get current SSID
        val ssid = wifiSsidProvider.getCurrentSsid() ?: return false

        // Check if in trusted list
        return autoConnectSettings.isNetworkTrusted(ssid)
    }
}
