package kittoku.mvc.autoconnect

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for TrustedNetworkChecker - determines if VPN should connect
 * based on trusted network settings.
 */
@DisplayName("TrustedNetworkChecker")
class TrustedNetworkCheckerTest {
    private lateinit var autoConnectSettings: AutoConnectSettings
    private lateinit var wifiSsidProvider: WifiSsidProvider
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var checker: TrustedNetworkChecker

    @BeforeEach
    fun setUp() {
        autoConnectSettings = mockk(relaxed = true)
        wifiSsidProvider = mockk(relaxed = true)
        networkMonitor = mockk(relaxed = true)
        checker = TrustedNetworkChecker(autoConnectSettings, wifiSsidProvider, networkMonitor)
    }

    @Nested
    @DisplayName("shouldConnectVpn")
    inner class ShouldConnectVpnTests {
        @Test
        @DisplayName("should return true when trusted networks feature is disabled")
        fun shouldReturnTrueWhenTrustedNetworksDisabled() {
            every { autoConnectSettings.isTrustedNetworksEnabled() } returns false

            assertTrue(checker.shouldConnectVpn())
        }

        @Test
        @DisplayName("should return true when not connected to WiFi")
        fun shouldReturnTrueWhenNotConnectedToWifi() {
            every { autoConnectSettings.isTrustedNetworksEnabled() } returns true
            every { networkMonitor.getCurrentNetworkType() } returns NetworkType.CELLULAR

            assertTrue(checker.shouldConnectVpn())
        }

        @Test
        @DisplayName("should return true when connected to untrusted WiFi")
        fun shouldReturnTrueWhenConnectedToUntrustedWifi() {
            every { autoConnectSettings.isTrustedNetworksEnabled() } returns true
            every { networkMonitor.getCurrentNetworkType() } returns NetworkType.WIFI
            every { wifiSsidProvider.getCurrentSsid() } returns "CoffeeShopWiFi"
            every { autoConnectSettings.isNetworkTrusted("CoffeeShopWiFi") } returns false

            assertTrue(checker.shouldConnectVpn())
        }

        @Test
        @DisplayName("should return false when connected to trusted WiFi")
        fun shouldReturnFalseWhenConnectedToTrustedWifi() {
            every { autoConnectSettings.isTrustedNetworksEnabled() } returns true
            every { networkMonitor.getCurrentNetworkType() } returns NetworkType.WIFI
            every { wifiSsidProvider.getCurrentSsid() } returns "HomeWiFi"
            every { autoConnectSettings.isNetworkTrusted("HomeWiFi") } returns true

            assertFalse(checker.shouldConnectVpn())
        }

        @Test
        @DisplayName("should return true when SSID is null")
        fun shouldReturnTrueWhenSsidIsNull() {
            every { autoConnectSettings.isTrustedNetworksEnabled() } returns true
            every { networkMonitor.getCurrentNetworkType() } returns NetworkType.WIFI
            every { wifiSsidProvider.getCurrentSsid() } returns null

            assertTrue(checker.shouldConnectVpn())
        }

        @Test
        @DisplayName("should return true when SSID is unknown")
        fun shouldReturnTrueWhenSsidIsUnknown() {
            every { autoConnectSettings.isTrustedNetworksEnabled() } returns true
            every { networkMonitor.getCurrentNetworkType() } returns NetworkType.WIFI
            every { wifiSsidProvider.getCurrentSsid() } returns "<unknown ssid>"

            assertTrue(checker.shouldConnectVpn())
        }

        @Test
        @DisplayName("should return true when network type is unknown")
        fun shouldReturnTrueWhenNetworkTypeIsUnknown() {
            every { autoConnectSettings.isTrustedNetworksEnabled() } returns true
            every { networkMonitor.getCurrentNetworkType() } returns NetworkType.UNKNOWN

            assertTrue(checker.shouldConnectVpn())
        }

        @Test
        @DisplayName("should return true when not connected to any network")
        fun shouldReturnTrueWhenNotConnected() {
            every { autoConnectSettings.isTrustedNetworksEnabled() } returns true
            every { networkMonitor.getCurrentNetworkType() } returns null

            assertTrue(checker.shouldConnectVpn())
        }
    }

    @Nested
    @DisplayName("isCurrentNetworkTrusted")
    inner class IsCurrentNetworkTrustedTests {
        @Test
        @DisplayName("should return false when not connected to WiFi")
        fun shouldReturnFalseWhenNotConnectedToWifi() {
            every { networkMonitor.getCurrentNetworkType() } returns NetworkType.CELLULAR

            assertFalse(checker.isCurrentNetworkTrusted())
        }

        @Test
        @DisplayName("should return false when SSID is null")
        fun shouldReturnFalseWhenSsidIsNull() {
            every { networkMonitor.getCurrentNetworkType() } returns NetworkType.WIFI
            every { wifiSsidProvider.getCurrentSsid() } returns null

            assertFalse(checker.isCurrentNetworkTrusted())
        }

        @Test
        @DisplayName("should return false when network is not trusted")
        fun shouldReturnFalseWhenNetworkIsNotTrusted() {
            every { networkMonitor.getCurrentNetworkType() } returns NetworkType.WIFI
            every { wifiSsidProvider.getCurrentSsid() } returns "CoffeeShopWiFi"
            every { autoConnectSettings.isNetworkTrusted("CoffeeShopWiFi") } returns false

            assertFalse(checker.isCurrentNetworkTrusted())
        }

        @Test
        @DisplayName("should return true when network is trusted")
        fun shouldReturnTrueWhenNetworkIsTrusted() {
            every { networkMonitor.getCurrentNetworkType() } returns NetworkType.WIFI
            every { wifiSsidProvider.getCurrentSsid() } returns "HomeWiFi"
            every { autoConnectSettings.isNetworkTrusted("HomeWiFi") } returns true

            assertTrue(checker.isCurrentNetworkTrusted())
        }
    }

    @Nested
    @DisplayName("getCurrentSsid")
    inner class GetCurrentSsidTests {
        @Test
        @DisplayName("should return null when not connected to WiFi")
        fun shouldReturnNullWhenNotConnectedToWifi() {
            every { networkMonitor.getCurrentNetworkType() } returns NetworkType.CELLULAR

            assertFalse(checker.isCurrentNetworkTrusted())
        }

        @Test
        @DisplayName("should return SSID when connected to WiFi")
        fun shouldReturnSsidWhenConnectedToWifi() {
            every { networkMonitor.getCurrentNetworkType() } returns NetworkType.WIFI
            every { wifiSsidProvider.getCurrentSsid() } returns "HomeWiFi"
            every { autoConnectSettings.isNetworkTrusted("HomeWiFi") } returns true

            assertTrue(checker.isCurrentNetworkTrusted())
        }
    }
}
