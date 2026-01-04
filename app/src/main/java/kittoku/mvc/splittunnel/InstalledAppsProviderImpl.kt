package kittoku.mvc.splittunnel

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/**
 * Implementation of InstalledAppsProvider that uses PackageManager
 * to retrieve information about installed applications.
 */
class InstalledAppsProviderImpl(
    private val packageManagerWrapper: PackageManagerWrapper,
) : InstalledAppsProvider {
    override fun getInstalledApps(includeSystemApps: Boolean): List<AppInfo> {
        val ownPackageName = packageManagerWrapper.getOwnPackageName()

        return packageManagerWrapper.getInstalledApplications()
            .asSequence()
            .filter { appInfo ->
                // Exclude own app
                appInfo.packageName != ownPackageName
            }
            .filter { appInfo ->
                // Filter system apps if requested
                if (includeSystemApps) {
                    true
                } else {
                    !isSystemApp(appInfo)
                }
            }
            .map { appInfo ->
                val label = packageManagerWrapper.getApplicationLabel(appInfo).toString()
                val appName =
                    if (label.isBlank()) {
                        appInfo.packageName
                    } else {
                        label
                    }
                AppInfo(
                    packageName = appInfo.packageName,
                    appName = appName,
                    isSystemApp = isSystemApp(appInfo),
                )
            }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }

    override fun getAppInfo(packageName: String): AppInfo? {
        return try {
            val appInfo = packageManagerWrapper.getApplicationInfo(packageName)
            val label = packageManagerWrapper.getApplicationLabel(appInfo).toString()
            val appName =
                if (label.isBlank()) {
                    packageName
                } else {
                    label
                }
            AppInfo(
                packageName = packageName,
                appName = appName,
                isSystemApp = isSystemApp(appInfo),
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    override fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManagerWrapper.getApplicationInfo(packageName)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun getAppIcon(packageName: String): Drawable? {
        return try {
            packageManagerWrapper.getApplicationIcon(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun isSystemApp(appInfo: ApplicationInfo): Boolean {
        return (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
    }
}
