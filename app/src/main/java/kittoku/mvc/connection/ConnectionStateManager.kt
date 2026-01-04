package kittoku.mvc.connection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager for VPN connection state.
 *
 * This class provides a centralized way to manage and observe
 * the VPN connection state throughout the application.
 */
class ConnectionStateManager {
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)

    /** Observable state flow for connection state changes */
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    /** Current connection state (synchronous access) */
    val currentState: ConnectionState
        get() = _state.value

    /**
     * Start the connection process.
     *
     * @param step Initial step description
     * @param progress Optional initial progress percentage
     */
    fun startConnecting(
        step: String,
        progress: Int? = null,
    ) {
        _state.value = ConnectionState.Connecting(step = step, progress = progress)
    }

    /**
     * Update the current connecting step.
     *
     * @param step New step description
     */
    fun updateConnectingStep(step: String) {
        val current = _state.value
        if (current is ConnectionState.Connecting) {
            _state.value = current.copy(step = step)
        }
    }

    /**
     * Update the current connecting progress.
     *
     * @param progress New progress percentage (0-100)
     */
    fun updateConnectingProgress(progress: Int) {
        val current = _state.value
        if (current is ConnectionState.Connecting) {
            _state.value = current.copy(progress = progress)
        }
    }

    /**
     * Set the connection as established.
     *
     * @param stats Initial connection statistics
     */
    fun setConnected(stats: ConnectionStats) {
        _state.value = ConnectionState.Connected(stats = stats)
    }

    /**
     * Start the disconnection process.
     */
    fun startDisconnecting() {
        _state.value = ConnectionState.Disconnecting
    }

    /**
     * Set the connection as disconnected.
     */
    fun setDisconnected() {
        _state.value = ConnectionState.Disconnected
    }

    /**
     * Set an error state.
     *
     * @param message Error message
     * @param cause Optional underlying exception
     * @param isRecoverable Whether the error can be recovered from
     */
    fun setError(
        message: String,
        cause: Throwable? = null,
        isRecoverable: Boolean = false,
    ) {
        _state.value =
            ConnectionState.Error(
                message = message,
                cause = cause,
                isRecoverable = isRecoverable,
            )
    }

    /**
     * Update connection statistics while connected.
     *
     * @param stats Updated connection statistics
     */
    fun updateStats(stats: ConnectionStats) {
        val current = _state.value
        if (current is ConnectionState.Connected) {
            _state.value = ConnectionState.Connected(stats = stats)
        }
    }
}
