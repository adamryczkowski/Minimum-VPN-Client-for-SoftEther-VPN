package kittoku.mvc.autoconnect

import android.content.Context
import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for BootReceiver - handles BOOT_COMPLETED broadcast
 * to optionally start VPN connection on device boot.
 */
@DisplayName("BootReceiver")
class BootReceiverTest {
    private lateinit var bootReceiver: BootReceiver
    private lateinit var context: Context
    private lateinit var autoConnectSettings: AutoConnectSettings
    private lateinit var vpnConnectionStarter: VpnConnectionStarter

    @BeforeEach
    fun setUp() {
        context = mockk(relaxed = true)
        autoConnectSettings = mockk(relaxed = true)
        vpnConnectionStarter = mockk(relaxed = true)
        bootReceiver = BootReceiver(autoConnectSettings, vpnConnectionStarter)
    }

    @Nested
    @DisplayName("onReceive")
    inner class OnReceiveTests {
        @Test
        @DisplayName("should start VPN when connect-on-boot is enabled and action is BOOT_COMPLETED")
        fun shouldStartVpnWhenConnectOnBootEnabled() {
            val intent = mockk<Intent>()
            every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
            every { autoConnectSettings.isConnectOnBootEnabled() } returns true
            every { autoConnectSettings.getDefaultProfileId() } returns 1L

            bootReceiver.onReceive(context, intent)

            verify { vpnConnectionStarter.startVpnConnection(context, 1L) }
        }

        @Test
        @DisplayName("should not start VPN when connect-on-boot is disabled")
        fun shouldNotStartVpnWhenConnectOnBootDisabled() {
            val intent = mockk<Intent>()
            every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
            every { autoConnectSettings.isConnectOnBootEnabled() } returns false

            bootReceiver.onReceive(context, intent)

            verify(exactly = 0) { vpnConnectionStarter.startVpnConnection(any(), any()) }
        }

        @Test
        @DisplayName("should not start VPN when action is not BOOT_COMPLETED")
        fun shouldNotStartVpnWhenActionIsNotBootCompleted() {
            val intent = mockk<Intent>()
            every { intent.action } returns "some.other.action"
            every { autoConnectSettings.isConnectOnBootEnabled() } returns true

            bootReceiver.onReceive(context, intent)

            verify(exactly = 0) { vpnConnectionStarter.startVpnConnection(any(), any()) }
        }

        @Test
        @DisplayName("should not start VPN when intent is null")
        fun shouldNotStartVpnWhenIntentIsNull() {
            bootReceiver.onReceive(context, null)

            verify(exactly = 0) { vpnConnectionStarter.startVpnConnection(any(), any()) }
        }

        @Test
        @DisplayName("should not start VPN when no default profile is set")
        fun shouldNotStartVpnWhenNoDefaultProfile() {
            val intent = mockk<Intent>()
            every { intent.action } returns Intent.ACTION_BOOT_COMPLETED
            every { autoConnectSettings.isConnectOnBootEnabled() } returns true
            every { autoConnectSettings.getDefaultProfileId() } returns null

            bootReceiver.onReceive(context, intent)

            verify(exactly = 0) { vpnConnectionStarter.startVpnConnection(any(), any()) }
        }

        @Test
        @DisplayName("should handle LOCKED_BOOT_COMPLETED action")
        fun shouldHandleLockedBootCompleted() {
            val intent = mockk<Intent>()
            every { intent.action } returns Intent.ACTION_LOCKED_BOOT_COMPLETED
            every { autoConnectSettings.isConnectOnBootEnabled() } returns true
            every { autoConnectSettings.getDefaultProfileId() } returns 1L

            bootReceiver.onReceive(context, intent)

            verify { vpnConnectionStarter.startVpnConnection(context, 1L) }
        }

        @Test
        @DisplayName("should handle MY_PACKAGE_REPLACED action for app updates")
        fun shouldHandleMyPackageReplaced() {
            val intent = mockk<Intent>()
            every { intent.action } returns Intent.ACTION_MY_PACKAGE_REPLACED
            every { autoConnectSettings.isConnectOnBootEnabled() } returns true
            every { autoConnectSettings.isReconnectAfterUpdateEnabled() } returns true
            every { autoConnectSettings.getDefaultProfileId() } returns 1L

            bootReceiver.onReceive(context, intent)

            verify { vpnConnectionStarter.startVpnConnection(context, 1L) }
        }

        @Test
        @DisplayName("should not start VPN on package replaced when reconnect after update is disabled")
        fun shouldNotStartVpnOnPackageReplacedWhenDisabled() {
            val intent = mockk<Intent>()
            every { intent.action } returns Intent.ACTION_MY_PACKAGE_REPLACED
            every { autoConnectSettings.isConnectOnBootEnabled() } returns true
            every { autoConnectSettings.isReconnectAfterUpdateEnabled() } returns false

            bootReceiver.onReceive(context, intent)

            verify(exactly = 0) { vpnConnectionStarter.startVpnConnection(any(), any()) }
        }
    }
}
