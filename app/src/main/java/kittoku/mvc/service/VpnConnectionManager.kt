package kittoku.mvc.service

import android.content.Context
import android.content.Intent
import android.net.VpnService
import kittoku.mvc.repository.VpnConnectionRepository
import kittoku.mvc.service.contract.ConnectionState

/**
 * Manager class for VPN connection operations.
 *
 * This class encapsulates the logic for starting and stopping VPN connections,
 * separating it from the UI layer. It coordinates between the UI (via repository)
 * and the VPN service.
 *
 * @property context Application context for starting services
 * @property repository Repository for connection state management
 */
class VpnConnectionManager(
    private val context: Context,
    private val repository: VpnConnectionRepository,
) {
    /**
     * Checks if VPN permission is required.
     *
     * @return Intent to request VPN permission, or null if already granted
     */
    fun prepareVpn(): Intent? {
        return VpnService.prepare(context)
    }

    /**
     * Attempts to connect to the VPN.
     *
     * This method first checks if VPN permission is granted. If not, it returns
     * the intent needed to request permission. If permission is granted, it
     * starts the VPN service.
     *
     * @return Intent to request VPN permission, or null if connection started
     */
    fun connect(): Intent? {
        val prepareIntent = prepareVpn()
        if (prepareIntent != null) {
            return prepareIntent
        }

        startVpnService()
        return null
    }

    /**
     * Disconnects from the VPN.
     */
    fun disconnect() {
        stopVpnService()
    }

    /**
     * Toggles the VPN connection state.
     *
     * @param shouldConnect True to connect, false to disconnect
     * @return Intent to request VPN permission if needed, null otherwise
     */
    fun toggleConnection(shouldConnect: Boolean): Intent? {
        return if (shouldConnect) {
            connect()
        } else {
            disconnect()
            null
        }
    }

    /**
     * Starts the VPN service with connect action.
     */
    private fun startVpnService() {
        repository.setConnecting("Starting VPN service...")
        val intent =
            Intent(context, SoftEtherVpnService::class.java)
                .setAction(ACTION_VPN_CONNECT)
        context.startService(intent)
    }

    /**
     * Stops the VPN service with disconnect action.
     */
    private fun stopVpnService() {
        val intent =
            Intent(context, SoftEtherVpnService::class.java)
                .setAction(ACTION_VPN_DISCONNECT)
        context.startService(intent)
        repository.setDisconnected()
    }

    /**
     * Called when VPN permission is granted after requesting it.
     * Continues with the connection process.
     */
    fun onVpnPermissionGranted() {
        startVpnService()
    }

    /**
     * Called when VPN permission is denied.
     */
    fun onVpnPermissionDenied() {
        repository.setError("VPN permission denied")
    }

    /**
     * Gets the current connection state.
     *
     * @return Current ConnectionState
     */
    fun getCurrentState(): ConnectionState {
        return repository.connectionState.value
    }

    /**
     * Checks if currently connected.
     *
     * @return True if connected, false otherwise
     */
    fun isConnected(): Boolean {
        return repository.isConnected.value
    }
}
