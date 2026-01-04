package kittoku.mvc.notification

import kittoku.mvc.connection.ConnectionState
import kittoku.mvc.connection.ConnectionStats

/**
 * Data class representing the content of a VPN notification.
 *
 * This class encapsulates all the information needed to build
 * a notification for the current VPN connection state.
 */
data class VpnNotificationContent(
    /** Notification title */
    val title: String,
    /** Short notification message */
    val message: String,
    /** Expanded notification message with more details */
    val expandedMessage: String,
    /** Type of icon to display */
    val iconType: NotificationIconType,
    /** Available actions for this notification */
    val actions: List<NotificationAction>,
    /** Whether the notification should be ongoing (non-dismissible) */
    val isOngoing: Boolean,
    /** Notification priority */
    val priority: NotificationPriority,
) {
    companion object {
        /**
         * Create notification content from a connection state.
         *
         * @param state Current connection state
         * @return Notification content for the state
         */
        fun fromState(state: ConnectionState): VpnNotificationContent =
            when (state) {
                is ConnectionState.Disconnected -> createDisconnectedContent()
                is ConnectionState.Connecting -> createConnectingContent(state)
                is ConnectionState.Connected -> createConnectedContent(state)
                is ConnectionState.Disconnecting -> createDisconnectingContent()
                is ConnectionState.Error -> createErrorContent(state)
            }

        private fun createDisconnectedContent(): VpnNotificationContent =
            VpnNotificationContent(
                title = "VPN Disconnected",
                message = "",
                expandedMessage = "",
                iconType = NotificationIconType.DISCONNECTED,
                actions = listOf(NotificationAction.CONNECT),
                isOngoing = false,
                priority = NotificationPriority.LOW,
            )

        private fun createConnectingContent(state: ConnectionState.Connecting): VpnNotificationContent =
            VpnNotificationContent(
                title = "VPN Connecting...",
                message = state.step,
                expandedMessage = state.step,
                iconType = NotificationIconType.CONNECTING,
                actions = listOf(NotificationAction.CANCEL),
                isOngoing = true,
                priority = NotificationPriority.DEFAULT,
            )

        private fun createConnectedContent(state: ConnectionState.Connected): VpnNotificationContent {
            val stats = state.stats
            val message = buildConnectedMessage(stats)
            val expandedMessage = buildExpandedConnectedMessage(stats)

            return VpnNotificationContent(
                title = "VPN Connected",
                message = message,
                expandedMessage = expandedMessage,
                iconType = NotificationIconType.CONNECTED,
                actions = listOf(NotificationAction.DISCONNECT),
                isOngoing = true,
                priority = NotificationPriority.LOW,
            )
        }

        private fun createDisconnectingContent(): VpnNotificationContent =
            VpnNotificationContent(
                title = "VPN Disconnecting...",
                message = "",
                expandedMessage = "",
                iconType = NotificationIconType.CONNECTING,
                actions = emptyList(),
                isOngoing = true,
                priority = NotificationPriority.DEFAULT,
            )

        private fun createErrorContent(state: ConnectionState.Error): VpnNotificationContent {
            val actions =
                if (state.isRecoverable) {
                    listOf(NotificationAction.RETRY, NotificationAction.DISMISS)
                } else {
                    listOf(NotificationAction.DISMISS)
                }

            return VpnNotificationContent(
                title = "VPN Error",
                message = state.message,
                expandedMessage = state.message,
                iconType = NotificationIconType.ERROR,
                actions = actions,
                isOngoing = false,
                priority = NotificationPriority.HIGH,
            )
        }

        private fun buildConnectedMessage(stats: ConnectionStats): String {
            val parts = mutableListOf<String>()

            if (stats.serverAddress.isNotEmpty()) {
                parts.add(stats.serverAddress)
            }

            parts.add(stats.getFormattedDuration())

            return parts.joinToString(" • ")
        }

        private fun buildExpandedConnectedMessage(stats: ConnectionStats): String {
            val lines = mutableListOf<String>()

            if (stats.serverAddress.isNotEmpty()) {
                lines.add("Server: ${stats.serverAddress}")
            }

            if (stats.assignedIp.isNotEmpty()) {
                lines.add("IP: ${stats.assignedIp}")
            }

            lines.add("Duration: ${stats.getFormattedDuration()}")

            if (stats.bytesSent > 0 || stats.bytesReceived > 0) {
                lines.add("↑ ${stats.getFormattedBytesSent()} • ↓ ${stats.getFormattedBytesReceived()}")
            }

            if (stats.isUdpAccelerated) {
                lines.add("UDP Acceleration: Active")
            }

            return lines.joinToString("\n")
        }
    }
}
