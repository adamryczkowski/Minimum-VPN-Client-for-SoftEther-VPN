package kittoku.mvc.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import kittoku.mvc.R
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
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(this, SoftEtherVpnService::class.java).setAction(ACTION_VPN_DISCONNECT)
        val pendingIntent = PendingIntent.getService(this, 0, intent, 0)
        val builder =
            NotificationCompat.Builder(this, CHANNEL_ID).also {
                it.setSmallIcon(R.drawable.ic_baseline_vpn_lock_24)
                it.setContentText("Disconnect SoftEther VPN connection")
                it.priority = NotificationCompat.PRIORITY_DEFAULT
                it.setContentIntent(pendingIntent)
                it.setAutoCancel(true)
            }

        startForeground(1, builder.build())
    }

    override fun onDestroy() {
        client?.kill(null)
        client = null
    }
}
