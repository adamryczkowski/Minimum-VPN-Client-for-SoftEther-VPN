package kittoku.mvc.splittunnel

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for SplitTunnelSettings - manages split tunneling configuration.
 */
@DisplayName("SplitTunnelSettings")
class SplitTunnelSettingsTest {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var settings: SplitTunnelSettings

    @BeforeEach
    fun setUp() {
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putStringSet(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        every { editor.apply() } returns Unit

        settings = SplitTunnelSettingsImpl(sharedPreferences)
    }

    @Nested
    @DisplayName("isSplitTunnelEnabled")
    inner class IsSplitTunnelEnabledTests {
        @Test
        @DisplayName("should return false by default")
        fun shouldReturnFalseByDefault() {
            every { sharedPreferences.getBoolean("split_tunnel_enabled", false) } returns false

            assertFalse(settings.isSplitTunnelEnabled())
        }

        @Test
        @DisplayName("should return true when enabled")
        fun shouldReturnTrueWhenEnabled() {
            every { sharedPreferences.getBoolean("split_tunnel_enabled", false) } returns true

            assertTrue(settings.isSplitTunnelEnabled())
        }
    }

    @Nested
    @DisplayName("setSplitTunnelEnabled")
    inner class SetSplitTunnelEnabledTests {
        @Test
        @DisplayName("should save enabled state")
        fun shouldSaveEnabledState() {
            settings.setSplitTunnelEnabled(true)

            verify { editor.putBoolean("split_tunnel_enabled", true) }
            verify { editor.apply() }
        }

        @Test
        @DisplayName("should save disabled state")
        fun shouldSaveDisabledState() {
            settings.setSplitTunnelEnabled(false)

            verify { editor.putBoolean("split_tunnel_enabled", false) }
            verify { editor.apply() }
        }
    }

    @Nested
    @DisplayName("getSplitTunnelMode")
    inner class GetSplitTunnelModeTests {
        @Test
        @DisplayName("should return EXCLUDE by default")
        fun shouldReturnExcludeByDefault() {
            every { sharedPreferences.getString("split_tunnel_mode", "EXCLUDE") } returns "EXCLUDE"

            assertEquals(SplitTunnelMode.EXCLUDE, settings.getSplitTunnelMode())
        }

        @Test
        @DisplayName("should return INCLUDE when set")
        fun shouldReturnIncludeWhenSet() {
            every { sharedPreferences.getString("split_tunnel_mode", "EXCLUDE") } returns "INCLUDE"

            assertEquals(SplitTunnelMode.INCLUDE, settings.getSplitTunnelMode())
        }

        @Test
        @DisplayName("should return EXCLUDE for invalid value")
        fun shouldReturnExcludeForInvalidValue() {
            every { sharedPreferences.getString("split_tunnel_mode", "EXCLUDE") } returns "INVALID"

            assertEquals(SplitTunnelMode.EXCLUDE, settings.getSplitTunnelMode())
        }
    }

    @Nested
    @DisplayName("setSplitTunnelMode")
    inner class SetSplitTunnelModeTests {
        @Test
        @DisplayName("should save EXCLUDE mode")
        fun shouldSaveExcludeMode() {
            settings.setSplitTunnelMode(SplitTunnelMode.EXCLUDE)

            verify { editor.putString("split_tunnel_mode", "EXCLUDE") }
            verify { editor.apply() }
        }

        @Test
        @DisplayName("should save INCLUDE mode")
        fun shouldSaveIncludeMode() {
            settings.setSplitTunnelMode(SplitTunnelMode.INCLUDE)

            verify { editor.putString("split_tunnel_mode", "INCLUDE") }
            verify { editor.apply() }
        }
    }

    @Nested
    @DisplayName("getSelectedApps")
    inner class GetSelectedAppsTests {
        @Test
        @DisplayName("should return empty set by default")
        fun shouldReturnEmptySetByDefault() {
            every { sharedPreferences.getStringSet("split_tunnel_apps", emptySet()) } returns emptySet()

            assertTrue(settings.getSelectedApps().isEmpty())
        }

        @Test
        @DisplayName("should return saved apps")
        fun shouldReturnSavedApps() {
            val apps = setOf("com.example.app1", "com.example.app2")
            every { sharedPreferences.getStringSet("split_tunnel_apps", emptySet()) } returns apps

            val result = settings.getSelectedApps()

            assertEquals(2, result.size)
            assertTrue(result.contains("com.example.app1"))
            assertTrue(result.contains("com.example.app2"))
        }
    }

    @Nested
    @DisplayName("setSelectedApps")
    inner class SetSelectedAppsTests {
        @Test
        @DisplayName("should save selected apps")
        fun shouldSaveSelectedApps() {
            val apps = setOf("com.example.app1", "com.example.app2")

            settings.setSelectedApps(apps)

            verify { editor.putStringSet("split_tunnel_apps", apps) }
            verify { editor.apply() }
        }

        @Test
        @DisplayName("should save empty set")
        fun shouldSaveEmptySet() {
            settings.setSelectedApps(emptySet())

            verify { editor.putStringSet("split_tunnel_apps", emptySet()) }
            verify { editor.apply() }
        }
    }

    @Nested
    @DisplayName("addSelectedApp")
    inner class AddSelectedAppTests {
        @Test
        @DisplayName("should add app to empty set")
        fun shouldAddAppToEmptySet() {
            every { sharedPreferences.getStringSet("split_tunnel_apps", emptySet()) } returns emptySet()
            val capturedSet = slot<Set<String>>()

            settings.addSelectedApp("com.example.app")

            verify { editor.putStringSet("split_tunnel_apps", capture(capturedSet)) }
            assertTrue(capturedSet.captured.contains("com.example.app"))
        }

        @Test
        @DisplayName("should add app to existing set")
        fun shouldAddAppToExistingSet() {
            val existingApps = mutableSetOf("com.example.existing")
            every { sharedPreferences.getStringSet("split_tunnel_apps", emptySet()) } returns existingApps
            val capturedSet = slot<Set<String>>()

            settings.addSelectedApp("com.example.new")

            verify { editor.putStringSet("split_tunnel_apps", capture(capturedSet)) }
            assertTrue(capturedSet.captured.contains("com.example.existing"))
            assertTrue(capturedSet.captured.contains("com.example.new"))
        }
    }

    @Nested
    @DisplayName("removeSelectedApp")
    inner class RemoveSelectedAppTests {
        @Test
        @DisplayName("should remove app from set")
        fun shouldRemoveAppFromSet() {
            val existingApps = mutableSetOf("com.example.app1", "com.example.app2")
            every { sharedPreferences.getStringSet("split_tunnel_apps", emptySet()) } returns existingApps
            val capturedSet = slot<Set<String>>()

            settings.removeSelectedApp("com.example.app1")

            verify { editor.putStringSet("split_tunnel_apps", capture(capturedSet)) }
            assertFalse(capturedSet.captured.contains("com.example.app1"))
            assertTrue(capturedSet.captured.contains("com.example.app2"))
        }

        @Test
        @DisplayName("should handle removing non-existent app")
        fun shouldHandleRemovingNonExistentApp() {
            val existingApps = mutableSetOf("com.example.app1")
            every { sharedPreferences.getStringSet("split_tunnel_apps", emptySet()) } returns existingApps
            val capturedSet = slot<Set<String>>()

            settings.removeSelectedApp("com.example.nonexistent")

            verify { editor.putStringSet("split_tunnel_apps", capture(capturedSet)) }
            assertTrue(capturedSet.captured.contains("com.example.app1"))
            assertEquals(1, capturedSet.captured.size)
        }
    }

    @Nested
    @DisplayName("isAppSelected")
    inner class IsAppSelectedTests {
        @Test
        @DisplayName("should return true for selected app")
        fun shouldReturnTrueForSelectedApp() {
            val apps = setOf("com.example.app1", "com.example.app2")
            every { sharedPreferences.getStringSet("split_tunnel_apps", emptySet()) } returns apps

            assertTrue(settings.isAppSelected("com.example.app1"))
        }

        @Test
        @DisplayName("should return false for non-selected app")
        fun shouldReturnFalseForNonSelectedApp() {
            val apps = setOf("com.example.app1")
            every { sharedPreferences.getStringSet("split_tunnel_apps", emptySet()) } returns apps

            assertFalse(settings.isAppSelected("com.example.other"))
        }
    }

    @Nested
    @DisplayName("clearSelectedApps")
    inner class ClearSelectedAppsTests {
        @Test
        @DisplayName("should clear all selected apps")
        fun shouldClearAllSelectedApps() {
            settings.clearSelectedApps()

            verify { editor.putStringSet("split_tunnel_apps", emptySet()) }
            verify { editor.apply() }
        }
    }

    @Nested
    @DisplayName("getSelectedAppCount")
    inner class GetSelectedAppCountTests {
        @Test
        @DisplayName("should return 0 for empty set")
        fun shouldReturnZeroForEmptySet() {
            every { sharedPreferences.getStringSet("split_tunnel_apps", emptySet()) } returns emptySet()

            assertEquals(0, settings.getSelectedAppCount())
        }

        @Test
        @DisplayName("should return correct count")
        fun shouldReturnCorrectCount() {
            val apps = setOf("com.example.app1", "com.example.app2", "com.example.app3")
            every { sharedPreferences.getStringSet("split_tunnel_apps", emptySet()) } returns apps

            assertEquals(3, settings.getSelectedAppCount())
        }
    }
}
