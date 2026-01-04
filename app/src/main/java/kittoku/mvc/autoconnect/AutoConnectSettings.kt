package kittoku.mvc.autoconnect

import android.content.SharedPreferences

/**
 * Manages auto-connect preferences for the VPN client.
 *
 * This class handles settings related to:
 * - Connect on boot
 * - Auto-reconnect on network change
 * - Trusted networks (networks where VPN should not connect)
 * - Default profile for auto-connect
 */
class AutoConnectSettings(
    private val sharedPreferences: SharedPreferences,
) {
    companion object {
        const val KEY_CONNECT_ON_BOOT = "auto_connect_on_boot"
        const val KEY_DEFAULT_PROFILE_ID = "auto_connect_default_profile_id"
        const val KEY_AUTO_RECONNECT = "auto_connect_auto_reconnect"
        const val KEY_RECONNECT_AFTER_UPDATE = "auto_connect_reconnect_after_update"
        const val KEY_TRUSTED_NETWORKS = "auto_connect_trusted_networks"
        const val KEY_TRUSTED_NETWORKS_ENABLED = "auto_connect_trusted_networks_enabled"
    }

    /**
     * Returns whether connect-on-boot is enabled.
     */
    fun isConnectOnBootEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_CONNECT_ON_BOOT, false)

    /**
     * Sets whether connect-on-boot is enabled.
     */
    fun setConnectOnBootEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_CONNECT_ON_BOOT, enabled)
            .apply()
    }

    /**
     * Returns the default profile ID for auto-connect, or null if not set.
     */
    fun getDefaultProfileId(): Long? {
        val id = sharedPreferences.getLong(KEY_DEFAULT_PROFILE_ID, -1L)
        return if (id == -1L) null else id
    }

    /**
     * Sets the default profile ID for auto-connect.
     * Pass null to clear the default profile.
     */
    fun setDefaultProfileId(profileId: Long?) {
        sharedPreferences.edit()
            .putLong(KEY_DEFAULT_PROFILE_ID, profileId ?: -1L)
            .apply()
    }

    /**
     * Returns whether auto-reconnect on network change is enabled.
     */
    fun isAutoReconnectEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_AUTO_RECONNECT, false)

    /**
     * Sets whether auto-reconnect on network change is enabled.
     */
    fun setAutoReconnectEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_AUTO_RECONNECT, enabled)
            .apply()
    }

    /**
     * Returns whether reconnect after app update is enabled.
     */
    fun isReconnectAfterUpdateEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_RECONNECT_AFTER_UPDATE, false)

    /**
     * Sets whether reconnect after app update is enabled.
     */
    fun setReconnectAfterUpdateEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_RECONNECT_AFTER_UPDATE, enabled)
            .apply()
    }

    /**
     * Returns the set of trusted network SSIDs.
     * VPN will not auto-connect when connected to a trusted network.
     */
    fun getTrustedNetworks(): Set<String> =
        sharedPreferences.getStringSet(KEY_TRUSTED_NETWORKS, emptySet()) ?: emptySet()

    /**
     * Sets the trusted network SSIDs.
     */
    fun setTrustedNetworks(networks: Set<String>) {
        sharedPreferences.edit()
            .putStringSet(KEY_TRUSTED_NETWORKS, networks)
            .apply()
    }

    /**
     * Adds a network to the trusted networks list.
     */
    fun addTrustedNetwork(ssid: String) {
        val networks = getTrustedNetworks().toMutableSet()
        networks.add(ssid)
        setTrustedNetworks(networks)
    }

    /**
     * Removes a network from the trusted networks list.
     */
    fun removeTrustedNetwork(ssid: String) {
        val networks = getTrustedNetworks().toMutableSet()
        networks.remove(ssid)
        setTrustedNetworks(networks)
    }

    /**
     * Returns whether the given network SSID is in the trusted networks list.
     */
    fun isNetworkTrusted(ssid: String): Boolean =
        getTrustedNetworks().contains(ssid)

    /**
     * Returns whether trusted networks feature is enabled.
     */
    fun isTrustedNetworksEnabled(): Boolean =
        sharedPreferences.getBoolean(KEY_TRUSTED_NETWORKS_ENABLED, false)

    /**
     * Sets whether trusted networks feature is enabled.
     */
    fun setTrustedNetworksEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_TRUSTED_NETWORKS_ENABLED, enabled)
            .apply()
    }
}
