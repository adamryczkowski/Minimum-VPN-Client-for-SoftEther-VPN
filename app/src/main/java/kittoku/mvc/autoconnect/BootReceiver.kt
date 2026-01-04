package kittoku.mvc.autoconnect

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver that handles device boot and app update events
 * to optionally start VPN connection automatically.
 *
 * Handles the following intents:
 * - ACTION_BOOT_COMPLETED: Device finished booting
 * - ACTION_LOCKED_BOOT_COMPLETED: Device finished booting (direct boot mode)
 * - ACTION_MY_PACKAGE_REPLACED: App was updated
 */
class BootReceiver(
    private val autoConnectSettings: AutoConnectSettings,
    private val vpnConnectionStarter: VpnConnectionStarter,
) : BroadcastReceiver() {
    /**
     * Default constructor for Android system to instantiate.
     * Uses Koin for dependency injection.
     */
    constructor() : this(
        org.koin.java.KoinJavaComponent.get(AutoConnectSettings::class.java),
        org.koin.java.KoinJavaComponent.get(VpnConnectionStarter::class.java),
    )

    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        if (context == null || intent == null) return

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            -> handleBootCompleted(context)

            Intent.ACTION_MY_PACKAGE_REPLACED -> handlePackageReplaced(context)
        }
    }

    private fun handleBootCompleted(context: Context) {
        if (!autoConnectSettings.isConnectOnBootEnabled()) return

        val profileId = autoConnectSettings.getDefaultProfileId() ?: return

        vpnConnectionStarter.startVpnConnection(context, profileId)
    }

    private fun handlePackageReplaced(context: Context) {
        if (!autoConnectSettings.isConnectOnBootEnabled()) return
        if (!autoConnectSettings.isReconnectAfterUpdateEnabled()) return

        val profileId = autoConnectSettings.getDefaultProfileId() ?: return

        vpnConnectionStarter.startVpnConnection(context, profileId)
    }
}
