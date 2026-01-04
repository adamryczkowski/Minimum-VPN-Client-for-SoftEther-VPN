package kittoku.mvc.connection

/**
 * Sealed class representing the possible states of a VPN connection.
 *
 * This provides a type-safe way to represent and handle different
 * connection states in the UI and business logic.
 */
sealed class ConnectionState {
    /** Whether the VPN is currently connected */
    abstract val isConnected: Boolean

    /** Whether the VPN is currently in the process of connecting */
    abstract val isConnecting: Boolean

    /** Human-readable display name for the state */
    abstract val displayName: String

    /**
     * State when the VPN is not connected.
     */
    data object Disconnected : ConnectionState() {
        override val isConnected: Boolean = false
        override val isConnecting: Boolean = false
        override val displayName: String = "Disconnected"
    }

    /**
     * State when the VPN is in the process of connecting.
     *
     * @property step Current step description (e.g., "Establishing SSL connection")
     * @property progress Optional progress percentage (0-100)
     */
    data class Connecting(
        val step: String,
        val progress: Int? = null,
    ) : ConnectionState() {
        override val isConnected: Boolean = false
        override val isConnecting: Boolean = true
        override val displayName: String = "Connecting: $step"
    }

    /**
     * State when the VPN is connected and active.
     *
     * @property stats Connection statistics
     */
    data class Connected(
        val stats: ConnectionStats,
    ) : ConnectionState() {
        override val isConnected: Boolean = true
        override val isConnecting: Boolean = false
        override val displayName: String = "Connected"
    }

    /**
     * State when the VPN is in the process of disconnecting.
     */
    data object Disconnecting : ConnectionState() {
        override val isConnected: Boolean = false
        override val isConnecting: Boolean = false
        override val displayName: String = "Disconnecting"
    }

    /**
     * State when an error has occurred.
     *
     * @property message Error message describing what went wrong
     * @property cause Optional underlying exception that caused the error
     * @property isRecoverable Whether the error can be recovered from (e.g., by retrying)
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val isRecoverable: Boolean = false,
    ) : ConnectionState() {
        override val isConnected: Boolean = false
        override val isConnecting: Boolean = false
        override val displayName: String = "Error: $message"
    }
}
