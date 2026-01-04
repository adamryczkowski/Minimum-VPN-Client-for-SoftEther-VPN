package kittoku.mvc.splittunnel

/**
 * Data class representing information about an installed application.
 *
 * @property packageName The unique package name of the application (e.g., "com.example.app")
 * @property appName The user-visible name of the application
 * @property isSystemApp Whether this is a system application
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
)
