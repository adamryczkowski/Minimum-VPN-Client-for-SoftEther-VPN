package kittoku.mvc.logging

/**
 * VPN-specific error types with user-friendly messages.
 *
 * Provides structured error information including:
 * - Error code for programmatic handling
 * - User-friendly message for display
 * - Technical details for debugging
 * - Suggested recovery actions
 */
sealed class VpnError(
    /** Unique error code */
    val code: String,
    /** User-friendly error message */
    val userMessage: String,
    /** Technical details for logging */
    val technicalDetails: String,
    /** Suggested actions for the user */
    val suggestedActions: List<String>,
    /** Underlying cause if any */
    cause: Throwable? = null,
) : Exception(userMessage, cause) {
    // === Connection Errors ===

    class ConnectionTimeout(
        host: String,
        timeoutMs: Long,
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E001",
            userMessage = "Connection timed out. The server may be unreachable.",
            technicalDetails = "Connection to $host timed out after ${timeoutMs}ms",
            suggestedActions =
                listOf(
                    "Check your internet connection",
                    "Verify the server address is correct",
                    "Try again later",
                ),
            cause = cause,
        )

    class ConnectionRefused(
        host: String,
        port: Int,
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E002",
            userMessage = "Connection refused. The VPN server may be offline.",
            technicalDetails = "Connection refused by $host:$port",
            suggestedActions =
                listOf(
                    "Verify the server is running",
                    "Check the port number",
                    "Contact your VPN administrator",
                ),
            cause = cause,
        )

    class HostUnreachable(
        host: String,
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E003",
            userMessage = "Server unreachable. Check your network connection.",
            technicalDetails = "Host $host is unreachable",
            suggestedActions =
                listOf(
                    "Check your internet connection",
                    "Disable airplane mode if enabled",
                    "Try connecting to a different network",
                ),
            cause = cause,
        )

    class DnsResolutionFailed(
        hostname: String,
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E004",
            userMessage = "Cannot resolve server address. Check the hostname.",
            technicalDetails = "DNS resolution failed for $hostname",
            suggestedActions =
                listOf(
                    "Verify the server hostname is correct",
                    "Try using the server's IP address instead",
                    "Check your DNS settings",
                ),
            cause = cause,
        )

    // === Authentication Errors ===

    class AuthenticationFailed(
        reason: String = "Invalid credentials",
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E010",
            userMessage = "Authentication failed. Please check your credentials.",
            technicalDetails = "Authentication failed: $reason",
            suggestedActions =
                listOf(
                    "Verify your username and password",
                    "Check if your account is active",
                    "Contact your VPN administrator",
                ),
            cause = cause,
        )

    class CertificateError(
        reason: String,
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E011",
            userMessage = "Certificate verification failed. The connection may not be secure.",
            technicalDetails = "Certificate error: $reason",
            suggestedActions =
                listOf(
                    "Verify you're connecting to the correct server",
                    "Check if the server certificate is valid",
                    "Contact your VPN administrator",
                ),
            cause = cause,
        )

    class AccountExpired(
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E012",
            userMessage = "Your VPN account has expired.",
            technicalDetails = "Account expired or disabled",
            suggestedActions =
                listOf(
                    "Renew your VPN subscription",
                    "Contact your VPN administrator",
                ),
            cause = cause,
        )

    // === Protocol Errors ===

    class ProtocolError(
        phase: String,
        details: String,
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E020",
            userMessage = "Connection failed due to a protocol error.",
            technicalDetails = "Protocol error during $phase: $details",
            suggestedActions =
                listOf(
                    "Try reconnecting",
                    "Update the app to the latest version",
                    "Contact support if the problem persists",
                ),
            cause = cause,
        )

    class UnsupportedProtocolVersion(
        clientVersion: String,
        serverVersion: String,
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E021",
            userMessage = "The server uses an incompatible protocol version.",
            technicalDetails = "Protocol version mismatch: client=$clientVersion, server=$serverVersion",
            suggestedActions =
                listOf(
                    "Update the app to the latest version",
                    "Contact your VPN administrator",
                ),
            cause = cause,
        )

    class DhcpFailed(
        reason: String,
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E022",
            userMessage = "Failed to obtain network configuration from the server.",
            technicalDetails = "DHCP failed: $reason",
            suggestedActions =
                listOf(
                    "Try reconnecting",
                    "Contact your VPN administrator",
                ),
            cause = cause,
        )

    // === Network Errors ===

    class NetworkLost(
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E030",
            userMessage = "Network connection lost.",
            technicalDetails = "Network connectivity lost during VPN session",
            suggestedActions =
                listOf(
                    "Check your internet connection",
                    "The VPN will reconnect automatically when network is available",
                ),
            cause = cause,
        )

    class TunnelCreationFailed(
        reason: String,
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E031",
            userMessage = "Failed to create VPN tunnel.",
            technicalDetails = "Tunnel creation failed: $reason",
            suggestedActions =
                listOf(
                    "Grant VPN permission if prompted",
                    "Disable other VPN apps",
                    "Restart the device if the problem persists",
                ),
            cause = cause,
        )

    class UdpAccelerationFailed(
        reason: String,
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E032",
            userMessage = "UDP acceleration unavailable. Using TCP mode.",
            technicalDetails = "UDP acceleration failed: $reason",
            suggestedActions =
                listOf(
                    "This is not critical - the VPN will work over TCP",
                    "Check if UDP is blocked on your network",
                ),
            cause = cause,
        )

    // === Permission Errors ===

    class VpnPermissionDenied(
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E040",
            userMessage = "VPN permission was denied.",
            technicalDetails = "User denied VPN permission",
            suggestedActions =
                listOf(
                    "Grant VPN permission when prompted",
                    "Go to Settings > Apps to grant permission manually",
                ),
            cause = cause,
        )

    class AlwaysOnVpnConflict(
        conflictingApp: String? = null,
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E041",
            userMessage = "Another VPN app has exclusive access.",
            technicalDetails = "Always-on VPN conflict${conflictingApp?.let { " with $it" } ?: ""}",
            suggestedActions =
                listOf(
                    "Disable Always-on VPN in system settings",
                    "Disconnect the other VPN app",
                ),
            cause = cause,
        )

    // === Internal Errors ===

    class InternalError(
        details: String,
        cause: Throwable? = null,
    ) : VpnError(
            code = "VPN_E099",
            userMessage = "An unexpected error occurred.",
            technicalDetails = "Internal error: $details",
            suggestedActions =
                listOf(
                    "Try reconnecting",
                    "Restart the app",
                    "Contact support if the problem persists",
                ),
            cause = cause,
        )

    companion object {
        /**
         * Create a VpnError from a generic exception.
         */
        fun fromException(e: Throwable): VpnError {
            return when {
                e is VpnError -> e
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    ConnectionTimeout("unknown", 0, e)
                e.message?.contains("refused", ignoreCase = true) == true ->
                    ConnectionRefused("unknown", 0, e)
                e.message?.contains("unreachable", ignoreCase = true) == true ->
                    HostUnreachable("unknown", e)
                e.message?.contains("authentication", ignoreCase = true) == true ->
                    AuthenticationFailed(e.message ?: "Unknown", e)
                e.message?.contains("certificate", ignoreCase = true) == true ->
                    CertificateError(e.message ?: "Unknown", e)
                else -> InternalError(e.message ?: e.javaClass.simpleName, e)
            }
        }
    }

    /**
     * Format error for logging.
     */
    fun toLogString(): String {
        return "[$code] $technicalDetails"
    }

    /**
     * Format error for user display.
     */
    fun toDisplayString(): String {
        return buildString {
            appendLine(userMessage)
            if (suggestedActions.isNotEmpty()) {
                appendLine()
                appendLine("Try:")
                suggestedActions.forEach { action ->
                    appendLine("• $action")
                }
            }
        }
    }
}
