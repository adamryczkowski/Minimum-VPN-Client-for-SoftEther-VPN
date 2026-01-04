package kittoku.mvc.splittunnel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build

/**
 * Implementation of PackageManagerWrapper that delegates to Android's PackageManager.
 */
class PackageManagerWrapperImpl(
    private val context: Context,
) : PackageManagerWrapper {
    private val packageManager: PackageManager
        get() = context.packageManager

    override fun getInstalledApplications(): List<ApplicationInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getInstalledApplications(
                PackageManager.ApplicationInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstalledApplications(0)
        }
    }

    override fun getApplicationLabel(appInfo: ApplicationInfo): CharSequence {
        return packageManager.getApplicationLabel(appInfo)
    }

    override fun getApplicationInfo(packageName: String): ApplicationInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
    }

    override fun getApplicationIcon(packageName: String): Drawable {
        return packageManager.getApplicationIcon(packageName)
    }

    override fun getOwnPackageName(): String {
        return context.packageName
    }
}
