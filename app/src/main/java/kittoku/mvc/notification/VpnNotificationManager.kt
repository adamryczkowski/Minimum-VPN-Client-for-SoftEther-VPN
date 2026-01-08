package kittoku.mvc.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import kittoku.mvc.MainActivity
import kittoku.mvc.R
import kittoku.mvc.connection.ConnectionState
import kittoku.mvc.service.ACTION_VPN_DISCONNECT

/**
 * Manager for VPN connection notifications.
 *
 * This class handles creating and updating the persistent notification
 * that shows the current VPN connection status.
 */
class VpnNotificationManager(
    private val context: Context,
) {
    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "vpn_connection_channel"
        const val CHANNEL_NAME = "VPN Connection"
        const val CHANNEL_DESCRIPTION = "Shows VPN connection status"

        const val ACTION_CONNECT = "kittoku.mvc.action.CONNECT"
        const val ACTION_DISCONNECT = "kittoku.mvc.action.DISCONNECT"
        const val ACTION_CANCEL = "kittoku.mvc.action.CANCEL"
        const val ACTION_RETRY = "kittoku.mvc.action.RETRY"
        const val ACTION_DISMISS = "kittoku.mvc.action.DISMISS"
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    setShowBadge(false)
                }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Build a notification for the given connection state.
     *
     * @param state Current connection state
     * @return Built notification
     */
    fun buildNotification(state: ConnectionState): Notification {
        val content = VpnNotificationContent.fromState(state)
        return buildNotificationFromContent(content)
    }

    /**
     * Update the notification with the current connection state.
     *
     * @param state Current connection state
     */
    fun updateNotification(state: ConnectionState) {
        val notification = buildNotification(state)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Cancel the notification.
     */
    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * Build a foreground notification for the VPN service.
     *
     * This notification is specifically designed for use with [android.app.Service.startForeground].
     * It includes a disconnect action that sends an intent to the VPN service.
     *
     * @param serviceClass The VPN service class to send the disconnect intent to
     * @return Built notification suitable for foreground service
     */
    fun buildForegroundNotification(serviceClass: Class<out android.app.Service>): Notification {
        val disconnectIntent =
            Intent(context, serviceClass).apply {
                action = ACTION_VPN_DISCONNECT
            }
        val pendingIntent =
            PendingIntent.getService(
                context,
                0,
                disconnectIntent,
                PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_vpn_lock_24)
            .setContentTitle("VPN Connected")
            .setContentText("Disconnect SoftEther VPN connection")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()
    }

    private fun buildNotificationFromContent(content: VpnNotificationContent): Notification {
        val builder =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(content.title)
                .setContentText(content.message)
                .setSmallIcon(getIconResource(content.iconType))
                .setOngoing(content.isOngoing)
                .setPriority(getPriority(content.priority))
                .setContentIntent(createContentIntent())
                .setAutoCancel(!content.isOngoing)

        // Add expanded style if there's expanded content
        if (content.expandedMessage.isNotEmpty() && content.expandedMessage != content.message) {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(content.expandedMessage),
            )
        }

        // Add action buttons
        content.actions.forEach { action ->
            builder.addAction(createNotificationAction(action))
        }

        return builder.build()
    }

    private fun getIconResource(iconType: NotificationIconType): Int =
        when (iconType) {
            NotificationIconType.DISCONNECTED -> R.drawable.ic_vpn_key
            NotificationIconType.CONNECTING -> R.drawable.ic_vpn_key
            NotificationIconType.CONNECTED -> R.drawable.ic_vpn_key
            NotificationIconType.ERROR -> R.drawable.ic_vpn_key
        }

    private fun getPriority(priority: NotificationPriority): Int =
        when (priority) {
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
            NotificationPriority.DEFAULT -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
        }

    private fun createContentIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationAction(action: NotificationAction): NotificationCompat.Action {
        val (actionString, title) =
            when (action) {
                NotificationAction.CONNECT -> ACTION_CONNECT to "Connect"
                NotificationAction.DISCONNECT -> ACTION_DISCONNECT to "Disconnect"
                NotificationAction.CANCEL -> ACTION_CANCEL to "Cancel"
                NotificationAction.RETRY -> ACTION_RETRY to "Retry"
                NotificationAction.DISMISS -> ACTION_DISMISS to "Dismiss"
            }

        val intent =
            Intent(actionString).apply {
                setPackage(context.packageName)
            }

        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                action.ordinal,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        return NotificationCompat.Action.Builder(
            0,
            title,
            pendingIntent,
        ).build()
    }
}
