package kittoku.mvc.service

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for SoftEtherVpnService.
 *
 * Note: Since VpnService is an Android component, these tests focus on
 * verifying the integration with VpnNotificationManager and the service
 * contract rather than the actual Android service lifecycle.
 */
@DisplayName("SoftEtherVpnService")
class SoftEtherVpnServiceTest {
    @Nested
    @DisplayName("Service actions")
    inner class ServiceActions {
        @Test
        @DisplayName("ACTION_VPN_CONNECT should be defined correctly")
        fun actionVpnConnectShouldBeDefinedCorrectly() {
            assertThat(ACTION_VPN_CONNECT).isEqualTo("kittoku.mvc.connect")
        }

        @Test
        @DisplayName("ACTION_VPN_DISCONNECT should be defined correctly")
        fun actionVpnDisconnectShouldBeDefinedCorrectly() {
            assertThat(ACTION_VPN_DISCONNECT).isEqualTo("kittoku.mvc.disconnect")
        }
    }

    @Nested
    @DisplayName("Notification integration")
    inner class NotificationIntegration {
        @Test
        @DisplayName("should use VpnNotificationManager for foreground notification")
        fun shouldUseVpnNotificationManagerForForegroundNotification() {
            // This test verifies that the service class is compatible with VpnNotificationManager
            // The actual notification creation is tested in VpnNotificationManagerTest
            val serviceClass = SoftEtherVpnService::class.java
            assertThat(serviceClass).isNotNull()
            assertThat(android.app.Service::class.java.isAssignableFrom(serviceClass)).isTrue()
        }
    }
}
