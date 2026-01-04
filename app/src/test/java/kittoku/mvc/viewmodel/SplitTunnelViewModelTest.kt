package kittoku.mvc.viewmodel

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kittoku.mvc.splittunnel.AppInfo
import kittoku.mvc.splittunnel.InstalledAppsProvider
import kittoku.mvc.splittunnel.SplitTunnelMode
import kittoku.mvc.splittunnel.SplitTunnelSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for SplitTunnelViewModel - manages UI state for split tunnel app selection.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("SplitTunnelViewModel")
class SplitTunnelViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var installedAppsProvider: InstalledAppsProvider
    private lateinit var splitTunnelSettings: SplitTunnelSettings

    private val testApps =
        listOf(
            AppInfo("com.example.app1", "App One", false),
            AppInfo("com.example.app2", "App Two", false),
            AppInfo("com.android.system", "System App", true),
            AppInfo("com.example.browser", "Browser", false),
            AppInfo("com.example.email", "Email", false),
        )

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        installedAppsProvider = mockk(relaxed = true)
        splitTunnelSettings = mockk(relaxed = true)

        every { installedAppsProvider.getInstalledApps(any()) } returns testApps
        every { splitTunnelSettings.isSplitTunnelEnabled() } returns false
        every { splitTunnelSettings.getSplitTunnelMode() } returns SplitTunnelMode.EXCLUDE
        every { splitTunnelSettings.getSelectedApps() } returns emptySet()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SplitTunnelViewModel {
        return SplitTunnelViewModel(
            installedAppsProvider,
            splitTunnelSettings,
            testDispatcher,
        )
    }

    @Nested
    @DisplayName("Initial State")
    inner class InitialStateTests {
        @Test
        @DisplayName("should load installed apps on initialization")
        fun shouldLoadInstalledAppsOnInitialization() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                assertEquals(5, state.apps.size)
            }

        @Test
        @DisplayName("should load split tunnel enabled state")
        fun shouldLoadSplitTunnelEnabledState() =
            runTest(testDispatcher) {
                every { splitTunnelSettings.isSplitTunnelEnabled() } returns true
                val viewModel = createViewModel()
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                assertTrue(state.isEnabled)
            }

        @Test
        @DisplayName("should load split tunnel mode")
        fun shouldLoadSplitTunnelMode() =
            runTest(testDispatcher) {
                every { splitTunnelSettings.getSplitTunnelMode() } returns SplitTunnelMode.INCLUDE
                val viewModel = createViewModel()
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                assertEquals(SplitTunnelMode.INCLUDE, state.mode)
            }

        @Test
        @DisplayName("should load selected apps")
        fun shouldLoadSelectedApps() =
            runTest(testDispatcher) {
                every { splitTunnelSettings.getSelectedApps() } returns
                    setOf("com.example.app1", "com.example.app2")
                val viewModel = createViewModel()
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                assertEquals(2, state.selectedApps.size)
                assertTrue(state.selectedApps.contains("com.example.app1"))
                assertTrue(state.selectedApps.contains("com.example.app2"))
            }
    }

    @Nested
    @DisplayName("Toggle Split Tunnel")
    inner class ToggleSplitTunnelTests {
        @Test
        @DisplayName("should enable split tunnel")
        fun shouldEnableSplitTunnel() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.setEnabled(true)
                advanceUntilIdle()

                verify { splitTunnelSettings.setSplitTunnelEnabled(true) }
                val state = viewModel.uiState.first()
                assertTrue(state.isEnabled)
            }

        @Test
        @DisplayName("should disable split tunnel")
        fun shouldDisableSplitTunnel() =
            runTest(testDispatcher) {
                every { splitTunnelSettings.isSplitTunnelEnabled() } returns true
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.setEnabled(false)
                advanceUntilIdle()

                verify { splitTunnelSettings.setSplitTunnelEnabled(false) }
                val state = viewModel.uiState.first()
                assertFalse(state.isEnabled)
            }
    }

    @Nested
    @DisplayName("Change Mode")
    inner class ChangeModeTests {
        @Test
        @DisplayName("should change to INCLUDE mode")
        fun shouldChangeToIncludeMode() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.setMode(SplitTunnelMode.INCLUDE)
                advanceUntilIdle()

                verify { splitTunnelSettings.setSplitTunnelMode(SplitTunnelMode.INCLUDE) }
                val state = viewModel.uiState.first()
                assertEquals(SplitTunnelMode.INCLUDE, state.mode)
            }

        @Test
        @DisplayName("should change to EXCLUDE mode")
        fun shouldChangeToExcludeMode() =
            runTest(testDispatcher) {
                every { splitTunnelSettings.getSplitTunnelMode() } returns SplitTunnelMode.INCLUDE
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.setMode(SplitTunnelMode.EXCLUDE)
                advanceUntilIdle()

                verify { splitTunnelSettings.setSplitTunnelMode(SplitTunnelMode.EXCLUDE) }
                val state = viewModel.uiState.first()
                assertEquals(SplitTunnelMode.EXCLUDE, state.mode)
            }
    }

    @Nested
    @DisplayName("Toggle App Selection")
    inner class ToggleAppSelectionTests {
        @Test
        @DisplayName("should select an app")
        fun shouldSelectAnApp() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.toggleAppSelection("com.example.app1")
                advanceUntilIdle()

                verify { splitTunnelSettings.addSelectedApp("com.example.app1") }
                val state = viewModel.uiState.first()
                assertTrue(state.selectedApps.contains("com.example.app1"))
            }

        @Test
        @DisplayName("should deselect an app")
        fun shouldDeselectAnApp() =
            runTest(testDispatcher) {
                every { splitTunnelSettings.getSelectedApps() } returns setOf("com.example.app1")
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.toggleAppSelection("com.example.app1")
                advanceUntilIdle()

                verify { splitTunnelSettings.removeSelectedApp("com.example.app1") }
                val state = viewModel.uiState.first()
                assertFalse(state.selectedApps.contains("com.example.app1"))
            }

        @Test
        @DisplayName("should select multiple apps")
        fun shouldSelectMultipleApps() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.toggleAppSelection("com.example.app1")
                viewModel.toggleAppSelection("com.example.app2")
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                assertEquals(2, state.selectedApps.size)
            }
    }

    @Nested
    @DisplayName("Filter Apps")
    inner class FilterAppsTests {
        @Test
        @DisplayName("should filter apps by search query")
        fun shouldFilterAppsBySearchQuery() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.setSearchQuery("browser")
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                assertEquals(1, state.filteredApps.size)
                assertEquals("com.example.browser", state.filteredApps[0].packageName)
            }

        @Test
        @DisplayName("should filter apps by package name")
        fun shouldFilterAppsByPackageName() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.setSearchQuery("com.example.email")
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                assertEquals(1, state.filteredApps.size)
                assertEquals("Email", state.filteredApps[0].appName)
            }

        @Test
        @DisplayName("should show all apps when search query is empty")
        fun shouldShowAllAppsWhenSearchQueryIsEmpty() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.setSearchQuery("browser")
                advanceUntilIdle()
                viewModel.setSearchQuery("")
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                // 4 non-system apps by default (system apps hidden)
                assertEquals(4, state.filteredApps.size)
            }

        @Test
        @DisplayName("should be case insensitive")
        fun shouldBeCaseInsensitive() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.setSearchQuery("BROWSER")
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                assertEquals(1, state.filteredApps.size)
            }
    }

    @Nested
    @DisplayName("Show System Apps Toggle")
    inner class ShowSystemAppsTests {
        @Test
        @DisplayName("should hide system apps by default")
        fun shouldHideSystemAppsByDefault() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                assertFalse(state.showSystemApps)
            }

        @Test
        @DisplayName("should show system apps when toggled")
        fun shouldShowSystemAppsWhenToggled() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.setShowSystemApps(true)
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                assertTrue(state.showSystemApps)
            }

        @Test
        @DisplayName("should filter out system apps when hidden")
        fun shouldFilterOutSystemAppsWhenHidden() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                // Only non-system apps should be shown (4 out of 5)
                assertEquals(4, state.filteredApps.size)
                assertFalse(state.filteredApps.any { it.isSystemApp })
            }

        @Test
        @DisplayName("should include system apps when shown")
        fun shouldIncludeSystemAppsWhenShown() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.setShowSystemApps(true)
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                assertEquals(5, state.filteredApps.size)
                assertTrue(state.filteredApps.any { it.isSystemApp })
            }
    }

    @Nested
    @DisplayName("Select All / Deselect All")
    inner class SelectAllTests {
        @Test
        @DisplayName("should select all visible apps")
        fun shouldSelectAllVisibleApps() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.selectAll()
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                // Should select all non-system apps (4)
                assertEquals(4, state.selectedApps.size)
            }

        @Test
        @DisplayName("should deselect all apps")
        fun shouldDeselectAllApps() =
            runTest(testDispatcher) {
                every { splitTunnelSettings.getSelectedApps() } returns
                    setOf("com.example.app1", "com.example.app2")
                val viewModel = createViewModel()
                advanceUntilIdle()

                viewModel.deselectAll()
                advanceUntilIdle()

                verify { splitTunnelSettings.setSelectedApps(emptySet()) }
                val state = viewModel.uiState.first()
                assertTrue(state.selectedApps.isEmpty())
            }
    }

    @Nested
    @DisplayName("Summary")
    inner class SummaryTests {
        @Test
        @DisplayName("should show disabled summary when disabled")
        fun shouldShowDisabledSummaryWhenDisabled() =
            runTest(testDispatcher) {
                val viewModel = createViewModel()
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                assertEquals("Split tunneling disabled", state.summary)
            }

        @Test
        @DisplayName("should show EXCLUDE mode summary")
        fun shouldShowExcludeModeSummary() =
            runTest(testDispatcher) {
                every { splitTunnelSettings.isSplitTunnelEnabled() } returns true
                every { splitTunnelSettings.getSelectedApps() } returns
                    setOf("com.example.app1", "com.example.app2")
                val viewModel = createViewModel()
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                assertEquals("2 apps bypass VPN", state.summary)
            }

        @Test
        @DisplayName("should show INCLUDE mode summary")
        fun shouldShowIncludeModeSummary() =
            runTest(testDispatcher) {
                every { splitTunnelSettings.isSplitTunnelEnabled() } returns true
                every { splitTunnelSettings.getSplitTunnelMode() } returns SplitTunnelMode.INCLUDE
                every { splitTunnelSettings.getSelectedApps() } returns setOf("com.example.app1")
                val viewModel = createViewModel()
                advanceUntilIdle()

                val state = viewModel.uiState.first()
                assertEquals("1 app uses VPN", state.summary)
            }
    }
}
