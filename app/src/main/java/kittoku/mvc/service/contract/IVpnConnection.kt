package kittoku.mvc.service.contract

import kittoku.mvc.connection.ConnectionState
import kittoku.mvc.connection.ConnectionStats

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
