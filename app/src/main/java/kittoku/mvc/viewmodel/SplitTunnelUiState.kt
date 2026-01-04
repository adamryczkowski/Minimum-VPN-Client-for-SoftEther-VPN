package kittoku.mvc.viewmodel

import kittoku.mvc.splittunnel.AppInfo
import kittoku.mvc.splittunnel.SplitTunnelMode

/**
 * UI state for the split tunnel app selection screen.
 */
data class SplitTunnelUiState(
    /**
     * Whether split tunneling is enabled.
     */
    val isEnabled: Boolean = false,
    /**
     * The split tunnel mode (INCLUDE or EXCLUDE).
     */
    val mode: SplitTunnelMode = SplitTunnelMode.EXCLUDE,
    /**
     * All installed apps.
     */
    val apps: List<AppInfo> = emptyList(),
    /**
     * Filtered apps based on search query and system apps toggle.
     */
    val filteredApps: List<AppInfo> = emptyList(),
    /**
     * Package names of selected apps.
     */
    val selectedApps: Set<String> = emptySet(),
    /**
     * Current search query.
     */
    val searchQuery: String = "",
    /**
     * Whether to show system apps.
     */
    val showSystemApps: Boolean = false,
    /**
     * Whether the app list is loading.
     */
    val isLoading: Boolean = false,
    /**
     * Summary text for the current configuration.
     */
    val summary: String = "Split tunneling disabled",
    /**
     * Error message, if any.
     */
    val error: String? = null,
)
