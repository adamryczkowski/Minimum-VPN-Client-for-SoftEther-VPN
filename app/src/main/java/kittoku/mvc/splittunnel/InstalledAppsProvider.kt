package kittoku.mvc.splittunnel

import android.graphics.drawable.Drawable

/**
 * Interface for providing information about installed applications.
 * Used for split tunneling app selection.
 */
interface InstalledAppsProvider {
    /**
     * Get list of all installed applications.
     *
     * @param includeSystemApps Whether to include system apps in the list
     * @return List of AppInfo for installed apps, sorted alphabetically by name
     */
    fun getInstalledApps(includeSystemApps: Boolean = false): List<AppInfo>

    /**
     * Get information about a specific app by package name.
     *
     * @param packageName The package name to look up
     * @return AppInfo if found, null if the app is not installed
     */
    fun getAppInfo(packageName: String): AppInfo?

    /**
     * Check if an app is installed.
     *
     * @param packageName The package name to check
     * @return true if the app is installed, false otherwise
     */
    fun isAppInstalled(packageName: String): Boolean

    /**
     * Get the icon for an app.
     *
     * @param packageName The package name to get the icon for
     * @return The app icon drawable, or null if not found
     */
    fun getAppIcon(packageName: String): Drawable?
}
