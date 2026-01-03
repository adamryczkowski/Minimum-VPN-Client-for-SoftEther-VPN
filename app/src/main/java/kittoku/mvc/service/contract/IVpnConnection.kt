package kittoku.mvc.service.contract

/**
 * Connection state representing the VPN connection lifecycle.
 */
sealed class ConnectionState {
    /**
     * VPN is disconnected and idle.
     */
    data object Disconnected : ConnectionState()

    /**
     * VPN is in the process of connecting.
     *
     * @property step Description of the current connection step
     * @property progress Optional progress percentage (0-100)
     */
    data class Connecting(
        val step: String,
        val progress: Int? = null,
    ) : ConnectionState()

    /**
     * VPN is connected and active.
     *
     * @property stats Current connection statistics
     */
    data class Connected(val stats: ConnectionStats) : ConnectionState()

    /**
     * VPN connection failed with an error.
     *
     * @property message Error message describing the failure
     * @property cause Optional underlying exception
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : ConnectionState()

    /**
     * VPN is in the process of disconnecting.
     */
    data object Disconnecting : ConnectionState()
}

/**
 * Statistics about the current VPN connection.
 */
data class ConnectionStats(
    /**
     * Total bytes sent through the VPN tunnel.
     */
    val bytesSent: Long = 0,
    /**
     * Total bytes received through the VPN tunnel.
     */
    val bytesReceived: Long = 0,
    /**
     * Connection duration in milliseconds.
     */
    val durationMs: Long = 0,
    /**
     * Whether UDP acceleration is active.
     */
    val isUdpAccelerated: Boolean = false,
    /**
     * Assigned IP address.
     */
    val assignedIpAddress: String? = null,
    /**
     * Server hostname.
     */
    val serverHostname: String? = null,
    /**
     * Hub name.
     */
    val hubName: String? = null,
)

/**
 * Interface for VPN connection management.
 *
 * This interface abstracts the VPN connection lifecycle, allowing for
 * different implementations and enabling mock implementations for testing.
 */
interface IVpnConnection {
    /**
     * Current connection state.
     */
    val state: ConnectionState

    /**
     * Starts the VPN connection process.
     *
     * This method initiates the connection sequence:
     * 1. SoftEther protocol negotiation
     * 2. DHCP address assignment
     * 3. ARP gateway resolution
     * 4. VPN tunnel establishment
     */
    fun connect()

    /**
     * Disconnects the VPN connection.
     *
     * @param error Optional error that caused the disconnection
     */
    fun disconnect(error: Throwable? = null)

    /**
     * Checks if the VPN is currently connected.
     *
     * @return true if connected, false otherwise
     */
    fun isConnected(): Boolean

    /**
     * Gets the current connection statistics.
     *
     * @return Current statistics, or null if not connected
     */
    fun getStats(): ConnectionStats?
}

/**
 * Listener interface for VPN connection state changes.
 */
interface IVpnConnectionListener {
    /**
     * Called when the connection state changes.
     *
     * @param oldState The previous connection state
     * @param newState The new connection state
     */
    fun onStateChanged(
        oldState: ConnectionState,
        newState: ConnectionState,
    )

    /**
     * Called when connection statistics are updated.
     *
     * @param stats The updated statistics
     */
    fun onStatsUpdated(stats: ConnectionStats)
}

/**
 * Factory interface for creating VPN connections.
 *
 * This allows for dependency injection and testing with mock connections.
 */
interface IVpnConnectionFactory {
    /**
     * Creates a new VPN connection instance.
     *
     * @return A new [IVpnConnection] instance
     */
    fun create(): IVpnConnection
}
