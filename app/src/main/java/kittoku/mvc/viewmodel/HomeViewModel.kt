package kittoku.mvc.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kittoku.mvc.connection.ConnectionState
import kittoku.mvc.repository.VpnConnectionRepository
import kittoku.mvc.service.VpnConnectionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 *
 * This ViewModel manages the UI state for the home screen, including
 * VPN connection state and user actions. It follows the MVVM pattern
 * to separate UI logic from the Fragment.
 *
 * @property connectionManager Manager for VPN connection operations
 * @property repository Repository for connection state
 */
class HomeViewModel(
    private val connectionManager: VpnConnectionManager,
    private val repository: VpnConnectionRepository,
) : ViewModel() {
    /**
     * Current connection state as a StateFlow.
     * UI can collect this to update the display.
     */
    val connectionState: StateFlow<ConnectionState> =
        repository.connectionState
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ConnectionState.Disconnected,
            )

    /**
     * Simple boolean indicating if VPN is connected.
     */
    val isConnected: StateFlow<Boolean> =
        repository.isConnected
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = false,
            )

    private val _vpnPermissionRequest = MutableSharedFlow<Intent>()

    /**
     * Flow of VPN permission request intents.
     * UI should collect this and launch the permission request activity.
     */
    val vpnPermissionRequest: SharedFlow<Intent> = _vpnPermissionRequest.asSharedFlow()

    private val _uiEvents = MutableSharedFlow<UiEvent>()

    /**
     * Flow of one-time UI events (e.g., showing toasts, navigation).
     */
    val uiEvents: SharedFlow<UiEvent> = _uiEvents.asSharedFlow()

    /**
     * Called when the user toggles the VPN connection switch.
     *
     * @param shouldConnect True if the user wants to connect, false to disconnect
     */
    fun onConnectionToggled(shouldConnect: Boolean) {
        viewModelScope.launch {
            val permissionIntent = connectionManager.toggleConnection(shouldConnect)
            if (permissionIntent != null) {
                _vpnPermissionRequest.emit(permissionIntent)
            }
        }
    }

    /**
     * Called when VPN permission result is received.
     *
     * @param granted True if permission was granted, false otherwise
     */
    fun onVpnPermissionResult(granted: Boolean) {
        if (granted) {
            connectionManager.onVpnPermissionGranted()
        } else {
            connectionManager.onVpnPermissionDenied()
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.ShowError("VPN permission is required to connect"))
            }
        }
    }

    /**
     * Gets the current connection state synchronously.
     *
     * @return Current ConnectionState
     */
    fun getCurrentState(): ConnectionState {
        return connectionManager.getCurrentState()
    }

    /**
     * Checks if currently connected synchronously.
     *
     * @return True if connected, false otherwise
     */
    fun isCurrentlyConnected(): Boolean {
        return connectionManager.isConnected()
    }

    /**
     * Sealed class representing one-time UI events.
     */
    sealed class UiEvent {
        /**
         * Event to show an error message to the user.
         *
         * @property message The error message to display
         */
        data class ShowError(val message: String) : UiEvent()

        /**
         * Event to show a success message to the user.
         *
         * @property message The success message to display
         */
        data class ShowSuccess(val message: String) : UiEvent()

        /**
         * Event to navigate to another screen.
         *
         * @property destination The destination identifier
         */
        data class Navigate(val destination: String) : UiEvent()
    }
}
