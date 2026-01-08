package kittoku.mvc.notification

import com.google.common.truth.Truth.assertThat
import kittoku.mvc.connection.ConnectionState
import kittoku.mvc.connection.ConnectionStats
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * TDD tests for VpnNotificationContent.
 *
 * These tests define the expected behavior of the notification content
 * before implementation (Red-Green-Refactor cycle).
 */
@DisplayName("VpnNotificationContent")
class VpnNotificationContentTest {
    @Nested
    @DisplayName("Title generation")
    inner class TitleGeneration {
        @Test
        @DisplayName("should show 'Disconnected' for Disconnected state")
        fun shouldShowDisconnectedForDisconnectedState() {
            val content = VpnNotificationContent.fromState(ConnectionState.Disconnected)
            assertThat(content.title).isEqualTo("VPN Disconnected")
        }

        @Test
        @DisplayName("should show 'Connecting...' for Connecting state")
        fun shouldShowConnectingForConnectingState() {
            val state = ConnectionState.Connecting(step = "Establishing SSL")
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.title).isEqualTo("VPN Connecting...")
        }

        @Test
        @DisplayName("should show 'Connected' for Connected state")
        fun shouldShowConnectedForConnectedState() {
            val stats = createTestStats()
            val state = ConnectionState.Connected(stats = stats)
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.title).isEqualTo("VPN Connected")
        }

        @Test
        @DisplayName("should show 'Disconnecting...' for Disconnecting state")
        fun shouldShowDisconnectingForDisconnectingState() {
            val content = VpnNotificationContent.fromState(ConnectionState.Disconnecting)
            assertThat(content.title).isEqualTo("VPN Disconnecting...")
        }

        @Test
        @DisplayName("should show 'VPN Error' for Error state")
        fun shouldShowErrorForErrorState() {
            val state = ConnectionState.Error(message = "Connection failed")
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.title).isEqualTo("VPN Error")
        }
    }

    @Nested
    @DisplayName("Message generation")
    inner class MessageGeneration {
        @Test
        @DisplayName("should show empty message for Disconnected state")
        fun shouldShowEmptyMessageForDisconnectedState() {
            val content = VpnNotificationContent.fromState(ConnectionState.Disconnected)
            assertThat(content.message).isEmpty()
        }

        @Test
        @DisplayName("should show step for Connecting state")
        fun shouldShowStepForConnectingState() {
            val state = ConnectionState.Connecting(step = "DHCP negotiation")
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.message).isEqualTo("DHCP negotiation")
        }

        @Test
        @DisplayName("should show server and duration for Connected state")
        fun shouldShowServerAndDurationForConnectedState() {
            // 1 hour ago
            val stats =
                ConnectionStats(
                    connectedAt = System.currentTimeMillis() - 3600_000,
                    serverAddress = "vpn.example.com",
                    assignedIp = "10.0.0.5",
                )
            val state = ConnectionState.Connected(stats = stats)
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.message).contains("vpn.example.com")
        }

        @Test
        @DisplayName("should show error message for Error state")
        fun shouldShowErrorMessageForErrorState() {
            val state = ConnectionState.Error(message = "Authentication failed")
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.message).isEqualTo("Authentication failed")
        }
    }

    @Nested
    @DisplayName("Icon selection")
    inner class IconSelection {
        @Test
        @DisplayName("should use disconnected icon for Disconnected state")
        fun shouldUseDisconnectedIconForDisconnectedState() {
            val content = VpnNotificationContent.fromState(ConnectionState.Disconnected)
            assertThat(content.iconType).isEqualTo(NotificationIconType.DISCONNECTED)
        }

        @Test
        @DisplayName("should use connecting icon for Connecting state")
        fun shouldUseConnectingIconForConnectingState() {
            val state = ConnectionState.Connecting(step = "Connecting")
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.iconType).isEqualTo(NotificationIconType.CONNECTING)
        }

        @Test
        @DisplayName("should use connected icon for Connected state")
        fun shouldUseConnectedIconForConnectedState() {
            val stats = createTestStats()
            val state = ConnectionState.Connected(stats = stats)
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.iconType).isEqualTo(NotificationIconType.CONNECTED)
        }

        @Test
        @DisplayName("should use error icon for Error state")
        fun shouldUseErrorIconForErrorState() {
            val state = ConnectionState.Error(message = "Error")
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.iconType).isEqualTo(NotificationIconType.ERROR)
        }
    }

    @Nested
    @DisplayName("Action buttons")
    inner class ActionButtons {
        @Test
        @DisplayName("should show Connect action for Disconnected state")
        fun shouldShowConnectActionForDisconnectedState() {
            val content = VpnNotificationContent.fromState(ConnectionState.Disconnected)
            assertThat(content.actions).contains(NotificationAction.CONNECT)
        }

        @Test
        @DisplayName("should show Cancel action for Connecting state")
        fun shouldShowCancelActionForConnectingState() {
            val state = ConnectionState.Connecting(step = "Connecting")
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.actions).contains(NotificationAction.CANCEL)
        }

        @Test
        @DisplayName("should show Disconnect action for Connected state")
        fun shouldShowDisconnectActionForConnectedState() {
            val stats = createTestStats()
            val state = ConnectionState.Connected(stats = stats)
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.actions).contains(NotificationAction.DISCONNECT)
        }

        @Test
        @DisplayName("should show Retry action for recoverable Error state")
        fun shouldShowRetryActionForRecoverableErrorState() {
            val state = ConnectionState.Error(message = "Timeout", isRecoverable = true)
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.actions).contains(NotificationAction.RETRY)
        }

        @Test
        @DisplayName("should show Dismiss action for non-recoverable Error state")
        fun shouldShowDismissActionForNonRecoverableErrorState() {
            val state = ConnectionState.Error(message = "Auth failed", isRecoverable = false)
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.actions).contains(NotificationAction.DISMISS)
        }
    }

    @Nested
    @DisplayName("Ongoing flag")
    inner class OngoingFlag {
        @Test
        @DisplayName("should not be ongoing for Disconnected state")
        fun shouldNotBeOngoingForDisconnectedState() {
            val content = VpnNotificationContent.fromState(ConnectionState.Disconnected)
            assertThat(content.isOngoing).isFalse()
        }

        @Test
        @DisplayName("should be ongoing for Connecting state")
        fun shouldBeOngoingForConnectingState() {
            val state = ConnectionState.Connecting(step = "Connecting")
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.isOngoing).isTrue()
        }

        @Test
        @DisplayName("should be ongoing for Connected state")
        fun shouldBeOngoingForConnectedState() {
            val stats = createTestStats()
            val state = ConnectionState.Connected(stats = stats)
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.isOngoing).isTrue()
        }

        @Test
        @DisplayName("should be ongoing for Disconnecting state")
        fun shouldBeOngoingForDisconnectingState() {
            val content = VpnNotificationContent.fromState(ConnectionState.Disconnecting)
            assertThat(content.isOngoing).isTrue()
        }

        @Test
        @DisplayName("should not be ongoing for Error state")
        fun shouldNotBeOngoingForErrorState() {
            val state = ConnectionState.Error(message = "Error")
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.isOngoing).isFalse()
        }
    }

    @Nested
    @DisplayName("Priority")
    inner class Priority {
        @Test
        @DisplayName("should have low priority for Disconnected state")
        fun shouldHaveLowPriorityForDisconnectedState() {
            val content = VpnNotificationContent.fromState(ConnectionState.Disconnected)
            assertThat(content.priority).isEqualTo(NotificationPriority.LOW)
        }

        @Test
        @DisplayName("should have default priority for Connecting state")
        fun shouldHaveDefaultPriorityForConnectingState() {
            val state = ConnectionState.Connecting(step = "Connecting")
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.priority).isEqualTo(NotificationPriority.DEFAULT)
        }

        @Test
        @DisplayName("should have low priority for Connected state")
        fun shouldHaveLowPriorityForConnectedState() {
            val stats = createTestStats()
            val state = ConnectionState.Connected(stats = stats)
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.priority).isEqualTo(NotificationPriority.LOW)
        }

        @Test
        @DisplayName("should have high priority for Error state")
        fun shouldHaveHighPriorityForErrorState() {
            val state = ConnectionState.Error(message = "Error")
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.priority).isEqualTo(NotificationPriority.HIGH)
        }
    }

    @Nested
    @DisplayName("Traffic stats in message")
    inner class TrafficStatsInMessage {
        @Test
        @DisplayName("should include traffic stats when available")
        fun shouldIncludeTrafficStatsWhenAvailable() {
            // 1 MB sent, 2 MB received
            val stats =
                ConnectionStats(
                    connectedAt = System.currentTimeMillis(),
                    serverAddress = "vpn.example.com",
                    bytesSent = 1024L * 1024L,
                    bytesReceived = 2048L * 1024L,
                )
            val state = ConnectionState.Connected(stats = stats)
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.expandedMessage).contains("↑")
            assertThat(content.expandedMessage).contains("↓")
        }

        @Test
        @DisplayName("should format traffic stats in human-readable format")
        fun shouldFormatTrafficStatsInHumanReadableFormat() {
            // 1 MB sent, 2 MB received
            val stats =
                ConnectionStats(
                    connectedAt = System.currentTimeMillis(),
                    serverAddress = "vpn.example.com",
                    bytesSent = 1024L * 1024L,
                    bytesReceived = 2048L * 1024L,
                )
            val state = ConnectionState.Connected(stats = stats)
            val content = VpnNotificationContent.fromState(state)
            assertThat(content.expandedMessage).contains("MB")
        }
    }

    @Nested
    @DisplayName("VpnNotificationManager constants")
    inner class VpnNotificationManagerConstants {
        @Test
        @DisplayName("should have correct notification ID")
        fun shouldHaveCorrectNotificationId() {
            assertThat(VpnNotificationManager.NOTIFICATION_ID).isEqualTo(1)
        }

        @Test
        @DisplayName("should have correct channel ID")
        fun shouldHaveCorrectChannelId() {
            assertThat(VpnNotificationManager.CHANNEL_ID).isEqualTo("vpn_connection_channel")
        }

        @Test
        @DisplayName("should have correct channel name")
        fun shouldHaveCorrectChannelName() {
            assertThat(VpnNotificationManager.CHANNEL_NAME).isEqualTo("VPN Connection")
        }

        @Test
        @DisplayName("should have correct channel description")
        fun shouldHaveCorrectChannelDescription() {
            assertThat(VpnNotificationManager.CHANNEL_DESCRIPTION).isEqualTo("Shows VPN connection status")
        }
    }

    private fun createTestStats(): ConnectionStats =
        ConnectionStats(
            connectedAt = System.currentTimeMillis(),
            serverAddress = "vpn.example.com",
            assignedIp = "10.0.0.5",
        )
}
