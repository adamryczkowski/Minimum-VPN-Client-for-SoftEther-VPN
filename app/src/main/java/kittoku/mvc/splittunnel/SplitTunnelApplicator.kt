package kittoku.mvc.splittunnel

/**
 * Interface for applying split tunnel configuration to VPN connections.
 */
interface SplitTunnelApplicator {
    /**
     * Get the list of apps that should be allowed to use the VPN.
     * This is used in INCLUDE mode where only selected apps use VPN.
     *
     * @return List of package names to allow, empty if not in INCLUDE mode
     */
    fun getAppsToAllow(): List<String>

    /**
     * Get the list of apps that should be disallowed from using the VPN.
     * This is used in EXCLUDE mode where selected apps bypass VPN.
     *
     * @return List of package names to disallow, empty if not in EXCLUDE mode
     */
    fun getAppsToDisallow(): List<String>

    /**
     * Check if split tunneling is currently active.
     * Split tunneling is active when enabled AND at least one app is selected.
     *
     * @return true if split tunneling should be applied
     */
    fun isSplitTunnelActive(): Boolean

    /**
     * Get a human-readable summary of the current split tunnel configuration.
     *
     * @return Summary string for display
     */
    fun getSplitTunnelSummary(): String

    /**
     * Remove apps that are no longer installed from the selection.
     * Should be called periodically to clean up stale entries.
     */
    fun cleanupUninstalledApps()
}
