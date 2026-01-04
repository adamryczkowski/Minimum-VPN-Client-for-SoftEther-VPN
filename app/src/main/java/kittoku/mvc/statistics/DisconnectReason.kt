package kittoku.mvc.statistics

/**
 * Reasons for VPN disconnection.
 */
enum class DisconnectReason {
    /** User manually requested disconnection */
    USER_REQUESTED,

    /** Server closed the connection */
    SERVER_CLOSED,

    /** Connection timed out */
    TIMEOUT,

    /** Authentication failed */
    AUTH_FAILED,

    /** Network error occurred */
    NETWORK_ERROR,

    /** App was killed or crashed */
    APP_KILLED,

    /** Device went to sleep or lost network */
    DEVICE_SLEEP,

    /** Unknown reason */
    UNKNOWN,
}
