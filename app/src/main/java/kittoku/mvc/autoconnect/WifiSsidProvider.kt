package kittoku.mvc.autoconnect

/**
 * Interface for providing the current WiFi SSID.
 * This abstraction allows for easier testing and handles
 * the complexity of getting SSID across different Android versions.
 */
interface WifiSsidProvider {
    /**
     * Returns the current WiFi SSID, or null if not connected to WiFi
     * or if the SSID cannot be determined.
     *
     * Note: Getting the SSID requires ACCESS_FINE_LOCATION permission
     * on Android 10+ and the location must be enabled.
     */
    fun getCurrentSsid(): String?
}
