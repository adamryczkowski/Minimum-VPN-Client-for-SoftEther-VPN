package kittoku.mvc.splittunnel

/**
 * Enum representing the split tunneling mode.
 */
enum class SplitTunnelMode {
    /**
     * EXCLUDE mode: Selected apps bypass the VPN (use direct connection).
     * All other apps use the VPN.
     * This is also known as "bypass" or "disallowed" mode.
     */
    EXCLUDE,

    /**
     * INCLUDE mode: Only selected apps use the VPN.
     * All other apps bypass the VPN (use direct connection).
     * This is also known as "whitelist" or "allowed" mode.
     */
    INCLUDE,
}
