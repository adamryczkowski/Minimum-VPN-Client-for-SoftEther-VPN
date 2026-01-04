package kittoku.mvc.splittunnel

import android.content.SharedPreferences

/**
 * Implementation of SplitTunnelSettings using SharedPreferences.
 */
class SplitTunnelSettingsImpl(
    private val sharedPreferences: SharedPreferences,
) : SplitTunnelSettings {
    companion object {
        private const val KEY_SPLIT_TUNNEL_ENABLED = "split_tunnel_enabled"
        private const val KEY_SPLIT_TUNNEL_MODE = "split_tunnel_mode"
        private const val KEY_SPLIT_TUNNEL_APPS = "split_tunnel_apps"
    }

    override fun isSplitTunnelEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_SPLIT_TUNNEL_ENABLED, false)
    }

    override fun setSplitTunnelEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_SPLIT_TUNNEL_ENABLED, enabled)
            .apply()
    }

    override fun getSplitTunnelMode(): SplitTunnelMode {
        val modeString = sharedPreferences.getString(KEY_SPLIT_TUNNEL_MODE, SplitTunnelMode.EXCLUDE.name)
        return try {
            SplitTunnelMode.valueOf(modeString ?: SplitTunnelMode.EXCLUDE.name)
        } catch (e: IllegalArgumentException) {
            SplitTunnelMode.EXCLUDE
        }
    }

    override fun setSplitTunnelMode(mode: SplitTunnelMode) {
        sharedPreferences.edit()
            .putString(KEY_SPLIT_TUNNEL_MODE, mode.name)
            .apply()
    }

    override fun getSelectedApps(): Set<String> {
        return sharedPreferences.getStringSet(KEY_SPLIT_TUNNEL_APPS, emptySet()) ?: emptySet()
    }

    override fun setSelectedApps(apps: Set<String>) {
        sharedPreferences.edit()
            .putStringSet(KEY_SPLIT_TUNNEL_APPS, apps)
            .apply()
    }

    override fun addSelectedApp(packageName: String) {
        val currentApps = getSelectedApps().toMutableSet()
        currentApps.add(packageName)
        setSelectedApps(currentApps)
    }

    override fun removeSelectedApp(packageName: String) {
        val currentApps = getSelectedApps().toMutableSet()
        currentApps.remove(packageName)
        setSelectedApps(currentApps)
    }

    override fun isAppSelected(packageName: String): Boolean {
        return getSelectedApps().contains(packageName)
    }

    override fun clearSelectedApps() {
        setSelectedApps(emptySet())
    }

    override fun getSelectedAppCount(): Int {
        return getSelectedApps().size
    }
}
