package kittoku.mvc.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kittoku.mvc.preference.MvcPreference
import kittoku.mvc.preference.accessor.getBooleanPrefValue
import kittoku.mvc.preference.accessor.setBooleanPrefValue
import kittoku.mvc.service.contract.ConnectionState
import kittoku.mvc.service.contract.ConnectionStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for VPN connection state management.
 *
 * This class separates the connection state management from the UI layer,
 * providing a clean interface for observing and modifying connection state.
 *
 * @property context Application context for accessing SharedPreferences
 */
class VpnConnectionRepository(private val context: Context) {
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    /**
     * Observable connection state as a StateFlow.
     * UI components can collect this flow to react to state changes.
     */
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)

    /**
     * Simple boolean indicating if VPN is currently connected.
     * This is derived from the HOME_CONNECTOR preference.
     */
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == MvcPreference.HOME_CONNECTOR.name) {
                val connected = getBooleanPrefValue(MvcPreference.HOME_CONNECTOR, prefs)
                _isConnected.value = connected
                updateConnectionStateFromPreference(connected)
            }
        }

    init {
        // Initialize with current preference value
        val currentValue = getBooleanPrefValue(MvcPreference.HOME_CONNECTOR, sharedPreferences)
        _isConnected.value = currentValue
        updateConnectionStateFromPreference(currentValue)

        // Register listener for preference changes
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    /**
     * Updates the connection state based on the preference value.
     */
    private fun updateConnectionStateFromPreference(connected: Boolean) {
        _connectionState.value =
            if (connected) {
                ConnectionState.Connected(
                    stats =
                        ConnectionStats(
                            bytesSent = 0,
                            bytesReceived = 0,
                            durationMs = 0,
                            isUdpAccelerated = false,
                        ),
                )
            } else {
                ConnectionState.Disconnected
            }
    }

    /**
     * Updates the connection state.
     * Called by the VPN service to report state changes.
     *
     * @param state The new connection state
     */
    fun updateConnectionState(state: ConnectionState) {
        _connectionState.value = state

        // Update the preference based on connection state
        val isConnected = state is ConnectionState.Connected
        setBooleanPrefValue(isConnected, MvcPreference.HOME_CONNECTOR, sharedPreferences)
        _isConnected.value = isConnected
    }

    /**
     * Sets the connecting state with a step description.
     *
     * @param step Description of the current connection step
     */
    fun setConnecting(step: String) {
        _connectionState.value = ConnectionState.Connecting(step)
    }

    /**
     * Sets the connected state with connection details.
     *
     * @param assignedIp The IP address assigned to the VPN client
     * @param serverIp The IP address of the VPN server
     */
    fun setConnected(
        assignedIp: String,
        serverIp: String,
    ) {
        val state =
            ConnectionState.Connected(
                stats =
                    ConnectionStats(
                        bytesSent = 0,
                        bytesReceived = 0,
                        durationMs = 0,
                        isUdpAccelerated = false,
                        assignedIpAddress = assignedIp,
                        serverHostname = serverIp,
                    ),
            )
        updateConnectionState(state)
    }

    /**
     * Sets the disconnected state.
     */
    fun setDisconnected() {
        updateConnectionState(ConnectionState.Disconnected)
    }

    /**
     * Sets the error state with an error message.
     *
     * @param message The error message
     * @param cause Optional throwable that caused the error
     */
    fun setError(
        message: String,
        cause: Throwable? = null,
    ) {
        _connectionState.value = ConnectionState.Error(message, cause)
        _isConnected.value = false
        setBooleanPrefValue(false, MvcPreference.HOME_CONNECTOR, sharedPreferences)
    }

    /**
     * Cleans up resources when the repository is no longer needed.
     */
    fun cleanup() {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceListener)
    }
}
