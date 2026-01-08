package kittoku.mvc.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import kittoku.mvc.connection.ConnectionState
import kittoku.mvc.connection.ConnectionStateManager
import kittoku.mvc.connection.ConnectionStats
import kittoku.mvc.preference.MvcPreference
import kittoku.mvc.preference.accessor.setBooleanPrefValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Repository for VPN connection state management.
 *
 * This class delegates to [ConnectionStateManager] as the single source of truth
 * for connection state, while also synchronizing with SharedPreferences for
 * UI binding with the preference-based UI components.
 *
 * @property context Application context for accessing SharedPreferences
 * @property stateManager The single source of truth for connection state
 */
class VpnConnectionRepository(
    private val context: Context,
    private val stateManager: ConnectionStateManager,
) {
    private val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * Observable connection state as a StateFlow.
     * UI components can collect this flow to react to state changes.
     * Delegates to [ConnectionStateManager] as the single source of truth.
     */
    val connectionState: StateFlow<ConnectionState> = stateManager.state

    /**
     * Simple boolean indicating if VPN is currently connected.
     * Derived from the connection state.
     */
    val isConnected: StateFlow<Boolean> =
        runBlocking {
            stateManager.state
                .map { it.isConnected }
                .stateIn(repositoryScope)
        }

    init {
        // Synchronize SharedPreferences with state changes from ConnectionStateManager
        repositoryScope.launch {
            stateManager.state.collect { state ->
                syncPreferenceWithState(state)
            }
        }
    }

    /**
     * Synchronizes the HOME_CONNECTOR preference with the current state.
     * This keeps the preference-based UI in sync with the actual state.
     */
    private fun syncPreferenceWithState(state: ConnectionState) {
        val isConnected = state is ConnectionState.Connected
        setBooleanPrefValue(isConnected, MvcPreference.HOME_CONNECTOR, sharedPreferences)
    }

    /**
     * Updates the connection state.
     * Called by the VPN service to report state changes.
     * Delegates to [ConnectionStateManager].
     *
     * @param state The new connection state
     */
    fun updateConnectionState(state: ConnectionState) {
        when (state) {
            is ConnectionState.Disconnected -> stateManager.setDisconnected()
            is ConnectionState.Connecting -> stateManager.startConnecting(state.step, state.progress)
            is ConnectionState.Connected -> stateManager.setConnected(state.stats)
            is ConnectionState.Disconnecting -> stateManager.startDisconnecting()
            is ConnectionState.Error -> stateManager.setError(state.message, state.cause, state.isRecoverable)
        }
    }

    /**
     * Sets the connecting state with a step description.
     * Delegates to [ConnectionStateManager].
     *
     * @param step Description of the current connection step
     */
    fun setConnecting(step: String) {
        stateManager.startConnecting(step)
    }

    /**
     * Sets the connected state with connection details.
     * Delegates to [ConnectionStateManager].
     *
     * @param assignedIp The IP address assigned to the VPN client
     * @param serverIp The IP address of the VPN server
     */
    fun setConnected(
        assignedIp: String,
        serverIp: String,
    ) {
        val stats =
            ConnectionStats(
                connectedAt = System.currentTimeMillis(),
                bytesSent = 0,
                bytesReceived = 0,
                isUdpAccelerated = false,
                assignedIp = assignedIp,
                serverAddress = serverIp,
            )
        stateManager.setConnected(stats)
    }

    /**
     * Sets the disconnected state.
     * Delegates to [ConnectionStateManager].
     */
    fun setDisconnected() {
        stateManager.setDisconnected()
    }

    /**
     * Sets the error state with an error message.
     * Delegates to [ConnectionStateManager].
     *
     * @param message The error message
     * @param cause Optional throwable that caused the error
     */
    fun setError(
        message: String,
        cause: Throwable? = null,
    ) {
        stateManager.setError(message, cause)
    }

    /**
     * Cleans up resources when the repository is no longer needed.
     */
    fun cleanup() {
        repositoryScope.cancel()
    }
}
