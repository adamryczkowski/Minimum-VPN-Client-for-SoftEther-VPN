package kittoku.mvc.service

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import androidx.preference.PreferenceManager
import kittoku.mvc.notification.VpnNotificationManager
import kittoku.mvc.service.client.ClientBridge
import kittoku.mvc.service.client.ControlClient
import kittoku.mvc.splittunnel.InstalledAppsProviderImpl
import kittoku.mvc.splittunnel.PackageManagerWrapperImpl
import kittoku.mvc.splittunnel.SplitTunnelApplicator
import kittoku.mvc.splittunnel.SplitTunnelApplicatorImpl
import kittoku.mvc.splittunnel.SplitTunnelSettingsImpl
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal class SoftEtherVpnService : VpnService() {
    private var client: ControlClient? = null
    private var notificationManager: VpnNotificationManager? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        return if (ACTION_VPN_CONNECT == intent?.action ?: false) {
            client?.kill(null)
            val splitTunnelApplicator = createSplitTunnelApplicator()
            client =
                ControlClient(createBridge(), splitTunnelApplicator).also {
                    beForegrounded()
                    it.run()
                }

            Service.START_STICKY
        } else {
            client?.kill(null)
            client = null

            Service.START_NOT_STICKY
        }
    }

    private fun createSplitTunnelApplicator(): SplitTunnelApplicator {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val settings = SplitTunnelSettingsImpl(prefs)
        val packageManagerWrapper = PackageManagerWrapperImpl(this)
        val installedAppsProvider = InstalledAppsProviderImpl(packageManagerWrapper)
        return SplitTunnelApplicatorImpl(settings, installedAppsProvider)
    }

    private fun createBridge(): ClientBridge {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        val handler =
            CoroutineExceptionHandler { _, throwable ->
                client?.kill(throwable)
                client = null
            }

        val bridge = ClientBridge(scope, handler)

        bridge.service = this
        bridge.prepareParameters(PreferenceManager.getDefaultSharedPreferences(this))

        return bridge
    }

    private fun beForegrounded() {
        notificationManager = VpnNotificationManager(this)
        val notification = notificationManager!!.buildForegroundNotification(SoftEtherVpnService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                VpnNotificationManager.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED,
            )
        } else {
            startForeground(VpnNotificationManager.NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        client?.kill(null)
        client = null
    }
}
