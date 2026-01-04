package kittoku.mvc.splittunnel

/**
 * Implementation of SplitTunnelApplicator.
 */
class SplitTunnelApplicatorImpl(
    private val settings: SplitTunnelSettings,
    private val installedAppsProvider: InstalledAppsProvider,
) : SplitTunnelApplicator {
    override fun getAppsToAllow(): List<String> {
        if (!settings.isSplitTunnelEnabled()) {
            return emptyList()
        }

        if (settings.getSplitTunnelMode() != SplitTunnelMode.INCLUDE) {
            return emptyList()
        }

        return settings.getSelectedApps()
            .filter { installedAppsProvider.isAppInstalled(it) }
            .toList()
    }

    override fun getAppsToDisallow(): List<String> {
        if (!settings.isSplitTunnelEnabled()) {
            return emptyList()
        }

        if (settings.getSplitTunnelMode() != SplitTunnelMode.EXCLUDE) {
            return emptyList()
        }

        return settings.getSelectedApps()
            .filter { installedAppsProvider.isAppInstalled(it) }
            .toList()
    }

    override fun isSplitTunnelActive(): Boolean {
        return settings.isSplitTunnelEnabled() && settings.getSelectedApps().isNotEmpty()
    }

    override fun getSplitTunnelSummary(): String {
        if (!settings.isSplitTunnelEnabled()) {
            return "Split tunneling disabled"
        }

        val count = settings.getSelectedAppCount()

        if (count == 0) {
            return "No apps selected"
        }

        return when (settings.getSplitTunnelMode()) {
            SplitTunnelMode.EXCLUDE -> {
                if (count == 1) {
                    "1 app bypasses VPN"
                } else {
                    "$count apps bypass VPN"
                }
            }
            SplitTunnelMode.INCLUDE -> {
                if (count == 1) {
                    "1 app uses VPN"
                } else {
                    "$count apps use VPN"
                }
            }
        }
    }

    override fun cleanupUninstalledApps() {
        val currentApps = settings.getSelectedApps()
        val installedApps = currentApps.filter { installedAppsProvider.isAppInstalled(it) }.toSet()
        settings.setSelectedApps(installedApps)
    }
}
