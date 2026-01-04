package kittoku.mvc.splittunnel

/**
 * Interface for managing split tunneling settings.
 */
interface SplitTunnelSettings {
    /**
     * Check if split tunneling is enabled.
     *
     * @return true if split tunneling is enabled
     */
    fun isSplitTunnelEnabled(): Boolean

    /**
     * Enable or disable split tunneling.
     *
     * @param enabled true to enable, false to disable
     */
    fun setSplitTunnelEnabled(enabled: Boolean)

    /**
     * Get the current split tunnel mode.
     *
     * @return The current mode (EXCLUDE or INCLUDE)
     */
    fun getSplitTunnelMode(): SplitTunnelMode

    /**
     * Set the split tunnel mode.
     *
     * @param mode The mode to set
     */
    fun setSplitTunnelMode(mode: SplitTunnelMode)

    /**
     * Get the set of selected app package names.
     *
     * @return Set of package names
     */
    fun getSelectedApps(): Set<String>

    /**
     * Set the selected apps.
     *
     * @param apps Set of package names to select
     */
    fun setSelectedApps(apps: Set<String>)

    /**
     * Add an app to the selected apps.
     *
     * @param packageName The package name to add
     */
    fun addSelectedApp(packageName: String)

    /**
     * Remove an app from the selected apps.
     *
     * @param packageName The package name to remove
     */
    fun removeSelectedApp(packageName: String)

    /**
     * Check if an app is selected.
     *
     * @param packageName The package name to check
     * @return true if the app is selected
     */
    fun isAppSelected(packageName: String): Boolean

    /**
     * Clear all selected apps.
     */
    fun clearSelectedApps()

    /**
     * Get the count of selected apps.
     *
     * @return The number of selected apps
     */
    fun getSelectedAppCount(): Int
}
