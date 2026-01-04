package kittoku.mvc.autoconnect

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build

/**
 * Implementation of [WifiSsidProvider] that retrieves the current WiFi SSID
 * using the appropriate Android API based on the SDK version.
 *
 * Note: Requires ACCESS_FINE_LOCATION permission on Android 10+ and
 * location services must be enabled.
 */
class WifiSsidProviderImpl(
    private val context: Context,
) : WifiSsidProvider {
    private val wifiManager: WifiManager by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @SuppressLint("MissingPermission")
    override fun getCurrentSsid(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+): Use ConnectivityManager
            getSsidFromConnectivityManager()
        } else {
            // Android 11 and below: Use WifiManager
            getSsidFromWifiManager()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getSsidFromConnectivityManager(): String? {
        val network = connectivityManager.activeNetwork ?: return null
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return null

        if (!capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return null
        }

        // On Android 12+, we need to get WifiInfo from NetworkCapabilities
        val wifiInfo =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                capabilities.transportInfo as? WifiInfo
            } else {
                null
            }

        return wifiInfo?.ssid?.removeSurroundingQuotes()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun getSsidFromWifiManager(): String? {
        val wifiInfo = wifiManager.connectionInfo ?: return null
        val ssid = wifiInfo.ssid ?: return null
        return ssid.removeSurroundingQuotes()
    }

    private fun String.removeSurroundingQuotes(): String {
        return if (startsWith("\"") && endsWith("\"") && length >= 2) {
            substring(1, length - 1)
        } else {
            this
        }
    }
}
