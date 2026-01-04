package kittoku.mvc.splittunnel

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for SplitTunnelApplicator - applies split tunnel configuration to VPN builder.
 */
@DisplayName("SplitTunnelApplicator")
class SplitTunnelApplicatorTest {
    private lateinit var settings: SplitTunnelSettings
    private lateinit var installedAppsProvider: InstalledAppsProvider
    private lateinit var applicator: SplitTunnelApplicator

    @BeforeEach
    fun setUp() {
        settings = mockk(relaxed = true)
        installedAppsProvider = mockk(relaxed = true)
        applicator = SplitTunnelApplicatorImpl(settings, installedAppsProvider)
    }

    @Nested
    @DisplayName("getAppsToAllow")
    inner class GetAppsToAllowTests {
        @Test
        @DisplayName("should return empty list when split tunnel is disabled")
        fun shouldReturnEmptyListWhenDisabled() {
            every { settings.isSplitTunnelEnabled() } returns false

            val result = applicator.getAppsToAllow()

            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("should return selected apps in INCLUDE mode")
        fun shouldReturnSelectedAppsInIncludeMode() {
            every { settings.isSplitTunnelEnabled() } returns true
            every { settings.getSplitTunnelMode() } returns SplitTunnelMode.INCLUDE
            every { settings.getSelectedApps() } returns setOf("com.example.app1", "com.example.app2")
            every { installedAppsProvider.isAppInstalled("com.example.app1") } returns true
            every { installedAppsProvider.isAppInstalled("com.example.app2") } returns true

            val result = applicator.getAppsToAllow()

            assertEquals(2, result.size)
            assertTrue(result.contains("com.example.app1"))
            assertTrue(result.contains("com.example.app2"))
        }

        @Test
        @DisplayName("should filter out uninstalled apps in INCLUDE mode")
        fun shouldFilterOutUninstalledAppsInIncludeMode() {
            every { settings.isSplitTunnelEnabled() } returns true
            every { settings.getSplitTunnelMode() } returns SplitTunnelMode.INCLUDE
            every { settings.getSelectedApps() } returns setOf("com.example.installed", "com.example.uninstalled")
            every { installedAppsProvider.isAppInstalled("com.example.installed") } returns true
            every { installedAppsProvider.isAppInstalled("com.example.uninstalled") } returns false

            val result = applicator.getAppsToAllow()

            assertEquals(1, result.size)
            assertTrue(result.contains("com.example.installed"))
        }

        @Test
        @DisplayName("should return empty list in EXCLUDE mode")
        fun shouldReturnEmptyListInExcludeMode() {
            every { settings.isSplitTunnelEnabled() } returns true
            every { settings.getSplitTunnelMode() } returns SplitTunnelMode.EXCLUDE
            every { settings.getSelectedApps() } returns setOf("com.example.app1")

            val result = applicator.getAppsToAllow()

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("getAppsToDisallow")
    inner class GetAppsToDisallowTests {
        @Test
        @DisplayName("should return empty list when split tunnel is disabled")
        fun shouldReturnEmptyListWhenDisabled() {
            every { settings.isSplitTunnelEnabled() } returns false

            val result = applicator.getAppsToDisallow()

            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("should return selected apps in EXCLUDE mode")
        fun shouldReturnSelectedAppsInExcludeMode() {
            every { settings.isSplitTunnelEnabled() } returns true
            every { settings.getSplitTunnelMode() } returns SplitTunnelMode.EXCLUDE
            every { settings.getSelectedApps() } returns setOf("com.example.app1", "com.example.app2")
            every { installedAppsProvider.isAppInstalled("com.example.app1") } returns true
            every { installedAppsProvider.isAppInstalled("com.example.app2") } returns true

            val result = applicator.getAppsToDisallow()

            assertEquals(2, result.size)
            assertTrue(result.contains("com.example.app1"))
            assertTrue(result.contains("com.example.app2"))
        }

        @Test
        @DisplayName("should filter out uninstalled apps in EXCLUDE mode")
        fun shouldFilterOutUninstalledAppsInExcludeMode() {
            every { settings.isSplitTunnelEnabled() } returns true
            every { settings.getSplitTunnelMode() } returns SplitTunnelMode.EXCLUDE
            every { settings.getSelectedApps() } returns setOf("com.example.installed", "com.example.uninstalled")
            every { installedAppsProvider.isAppInstalled("com.example.installed") } returns true
            every { installedAppsProvider.isAppInstalled("com.example.uninstalled") } returns false

            val result = applicator.getAppsToDisallow()

            assertEquals(1, result.size)
            assertTrue(result.contains("com.example.installed"))
        }

        @Test
        @DisplayName("should return empty list in INCLUDE mode")
        fun shouldReturnEmptyListInIncludeMode() {
            every { settings.isSplitTunnelEnabled() } returns true
            every { settings.getSplitTunnelMode() } returns SplitTunnelMode.INCLUDE
            every { settings.getSelectedApps() } returns setOf("com.example.app1")

            val result = applicator.getAppsToDisallow()

            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("isSplitTunnelActive")
    inner class IsSplitTunnelActiveTests {
        @Test
        @DisplayName("should return false when disabled")
        fun shouldReturnFalseWhenDisabled() {
            every { settings.isSplitTunnelEnabled() } returns false

            val result = applicator.isSplitTunnelActive()

            assertEquals(false, result)
        }

        @Test
        @DisplayName("should return false when enabled but no apps selected")
        fun shouldReturnFalseWhenNoAppsSelected() {
            every { settings.isSplitTunnelEnabled() } returns true
            every { settings.getSelectedApps() } returns emptySet()

            val result = applicator.isSplitTunnelActive()

            assertEquals(false, result)
        }

        @Test
        @DisplayName("should return true when enabled and apps selected")
        fun shouldReturnTrueWhenEnabledAndAppsSelected() {
            every { settings.isSplitTunnelEnabled() } returns true
            every { settings.getSelectedApps() } returns setOf("com.example.app1")

            val result = applicator.isSplitTunnelActive()

            assertEquals(true, result)
        }
    }

    @Nested
    @DisplayName("getSplitTunnelSummary")
    inner class GetSplitTunnelSummaryTests {
        @Test
        @DisplayName("should return disabled message when disabled")
        fun shouldReturnDisabledMessageWhenDisabled() {
            every { settings.isSplitTunnelEnabled() } returns false

            val result = applicator.getSplitTunnelSummary()

            assertEquals("Split tunneling disabled", result)
        }

        @Test
        @DisplayName("should return EXCLUDE mode summary")
        fun shouldReturnExcludeModeSummary() {
            every { settings.isSplitTunnelEnabled() } returns true
            every { settings.getSplitTunnelMode() } returns SplitTunnelMode.EXCLUDE
            every { settings.getSelectedAppCount() } returns 3

            val result = applicator.getSplitTunnelSummary()

            assertEquals("3 apps bypass VPN", result)
        }

        @Test
        @DisplayName("should return INCLUDE mode summary")
        fun shouldReturnIncludeModeSummary() {
            every { settings.isSplitTunnelEnabled() } returns true
            every { settings.getSplitTunnelMode() } returns SplitTunnelMode.INCLUDE
            every { settings.getSelectedAppCount() } returns 5

            val result = applicator.getSplitTunnelSummary()

            assertEquals("5 apps use VPN", result)
        }

        @Test
        @DisplayName("should handle singular app count")
        fun shouldHandleSingularAppCount() {
            every { settings.isSplitTunnelEnabled() } returns true
            every { settings.getSplitTunnelMode() } returns SplitTunnelMode.EXCLUDE
            every { settings.getSelectedAppCount() } returns 1

            val result = applicator.getSplitTunnelSummary()

            assertEquals("1 app bypasses VPN", result)
        }

        @Test
        @DisplayName("should handle zero apps selected")
        fun shouldHandleZeroAppsSelected() {
            every { settings.isSplitTunnelEnabled() } returns true
            every { settings.getSplitTunnelMode() } returns SplitTunnelMode.EXCLUDE
            every { settings.getSelectedAppCount() } returns 0

            val result = applicator.getSplitTunnelSummary()

            assertEquals("No apps selected", result)
        }
    }

    @Nested
    @DisplayName("cleanupUninstalledApps")
    inner class CleanupUninstalledAppsTests {
        @Test
        @DisplayName("should remove uninstalled apps from selection")
        fun shouldRemoveUninstalledApps() {
            every { settings.getSelectedApps() } returns
                setOf(
                    "com.example.installed",
                    "com.example.uninstalled",
                )
            every { installedAppsProvider.isAppInstalled("com.example.installed") } returns true
            every { installedAppsProvider.isAppInstalled("com.example.uninstalled") } returns false

            applicator.cleanupUninstalledApps()

            verify { settings.setSelectedApps(setOf("com.example.installed")) }
        }

        @Test
        @DisplayName("should not modify selection when all apps are installed")
        fun shouldNotModifyWhenAllAppsInstalled() {
            every { settings.getSelectedApps() } returns setOf("com.example.app1", "com.example.app2")
            every { installedAppsProvider.isAppInstalled(any()) } returns true

            applicator.cleanupUninstalledApps()

            verify { settings.setSelectedApps(setOf("com.example.app1", "com.example.app2")) }
        }
    }
}
