package kittoku.mvc.notification

/**
 * Types of icons used in VPN notifications.
 */
enum class NotificationIconType {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR,
}

/**
 * Actions available in VPN notifications.
 */
enum class NotificationAction {
    CONNECT,
    DISCONNECT,
    CANCEL,
    RETRY,
    DISMISS,
}

/**
 * Priority levels for notifications.
 */
enum class NotificationPriority {
    LOW,
    DEFAULT,
    HIGH,
}
