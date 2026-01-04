package kittoku.mvc.splittunnel

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

/**
 * Interface wrapping PackageManager for testability.
 * This allows mocking PackageManager operations in unit tests.
 */
interface PackageManagerWrapper {
    /**
     * Get list of all installed applications.
     *
     * @return List of ApplicationInfo for all installed apps
     */
    fun getInstalledApplications(): List<ApplicationInfo>

    /**
     * Get the application label (display name) for an app.
     *
     * @param appInfo The ApplicationInfo to get the label for
     * @return The user-visible label for the application
     */
    fun getApplicationLabel(appInfo: ApplicationInfo): CharSequence

    /**
     * Get ApplicationInfo for a specific package.
     *
     * @param packageName The package name to look up
     * @return The ApplicationInfo for the package
     * @throws android.content.pm.PackageManager.NameNotFoundException if package not found
     */
    fun getApplicationInfo(packageName: String): ApplicationInfo

    /**
     * Get the application icon for a package.
     *
     * @param packageName The package name to get the icon for
     * @return The application icon drawable
     * @throws android.content.pm.PackageManager.NameNotFoundException if package not found
     */
    fun getApplicationIcon(packageName: String): Drawable

    /**
     * Get the package name of this application.
     *
     * @return The package name of the current app
     */
    fun getOwnPackageName(): String
}
