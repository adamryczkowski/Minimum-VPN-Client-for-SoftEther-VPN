package kittoku.mvc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kittoku.mvc.splittunnel.InstalledAppsProvider
import kittoku.mvc.splittunnel.SplitTunnelMode
import kittoku.mvc.splittunnel.SplitTunnelSettings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the split tunnel app selection screen.
 *
 * Manages the UI state for selecting which apps should use or bypass the VPN.
 */
class SplitTunnelViewModel(
    private val installedAppsProvider: InstalledAppsProvider,
    private val splitTunnelSettings: SplitTunnelSettings,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SplitTunnelUiState())
    val uiState: StateFlow<SplitTunnelUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Load settings
                val isEnabled = splitTunnelSettings.isSplitTunnelEnabled()
                val mode = splitTunnelSettings.getSplitTunnelMode()
                val selectedApps = splitTunnelSettings.getSelectedApps()

                // Load apps in background
                val apps =
                    withContext(ioDispatcher) {
                        installedAppsProvider.getInstalledApps(includeSystemApps = true)
                    }

                _uiState.update { state ->
                    state.copy(
                        isEnabled = isEnabled,
                        mode = mode,
                        apps = apps,
                        selectedApps = selectedApps,
                        isLoading = false,
                        summary = generateSummary(isEnabled, mode, selectedApps.size),
                    )
                }

                // Apply filters
                applyFilters()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load apps: ${e.message}",
                    )
                }
            }
        }
    }

    /**
     * Enable or disable split tunneling.
     */
    fun setEnabled(enabled: Boolean) {
        splitTunnelSettings.setSplitTunnelEnabled(enabled)
        _uiState.update { state ->
            state.copy(
                isEnabled = enabled,
                summary = generateSummary(enabled, state.mode, state.selectedApps.size),
            )
        }
    }

    /**
     * Set the split tunnel mode.
     */
    fun setMode(mode: SplitTunnelMode) {
        splitTunnelSettings.setSplitTunnelMode(mode)
        _uiState.update { state ->
            state.copy(
                mode = mode,
                summary = generateSummary(state.isEnabled, mode, state.selectedApps.size),
            )
        }
    }

    /**
     * Toggle selection of an app.
     */
    fun toggleAppSelection(packageName: String) {
        val currentSelected = _uiState.value.selectedApps
        val newSelected =
            if (currentSelected.contains(packageName)) {
                splitTunnelSettings.removeSelectedApp(packageName)
                currentSelected - packageName
            } else {
                splitTunnelSettings.addSelectedApp(packageName)
                currentSelected + packageName
            }

        _uiState.update { state ->
            state.copy(
                selectedApps = newSelected,
                summary = generateSummary(state.isEnabled, state.mode, newSelected.size),
            )
        }
    }

    /**
     * Set the search query for filtering apps.
     */
    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    /**
     * Set whether to show system apps.
     */
    fun setShowSystemApps(show: Boolean) {
        _uiState.update { it.copy(showSystemApps = show) }
        applyFilters()
    }

    /**
     * Select all visible apps.
     */
    fun selectAll() {
        val visibleApps = _uiState.value.filteredApps.map { it.packageName }.toSet()
        val newSelected = _uiState.value.selectedApps + visibleApps

        splitTunnelSettings.setSelectedApps(newSelected)

        _uiState.update { state ->
            state.copy(
                selectedApps = newSelected,
                summary = generateSummary(state.isEnabled, state.mode, newSelected.size),
            )
        }
    }

    /**
     * Deselect all apps.
     */
    fun deselectAll() {
        splitTunnelSettings.setSelectedApps(emptySet())

        _uiState.update { state ->
            state.copy(
                selectedApps = emptySet(),
                summary = generateSummary(state.isEnabled, state.mode, 0),
            )
        }
    }

    /**
     * Dismiss the error message.
     */
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.lowercase()

        val filtered =
            state.apps
                .filter { app ->
                    // Filter by system apps toggle
                    state.showSystemApps || !app.isSystemApp
                }
                .filter { app ->
                    // Filter by search query
                    if (query.isEmpty()) {
                        true
                    } else {
                        app.appName.lowercase().contains(query) ||
                            app.packageName.lowercase().contains(query)
                    }
                }

        _uiState.update { it.copy(filteredApps = filtered) }
    }

    private fun generateSummary(
        isEnabled: Boolean,
        mode: SplitTunnelMode,
        selectedCount: Int,
    ): String {
        if (!isEnabled) {
            return "Split tunneling disabled"
        }

        if (selectedCount == 0) {
            return "No apps selected"
        }

        return when (mode) {
            SplitTunnelMode.EXCLUDE -> {
                if (selectedCount == 1) {
                    "1 app bypasses VPN"
                } else {
                    "$selectedCount apps bypass VPN"
                }
            }
            SplitTunnelMode.INCLUDE -> {
                if (selectedCount == 1) {
                    "1 app uses VPN"
                } else {
                    "$selectedCount apps use VPN"
                }
            }
        }
    }
}
