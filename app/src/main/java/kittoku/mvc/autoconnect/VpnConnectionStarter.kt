package kittoku.mvc.autoconnect

import android.content.Context

/**
 * Interface for starting VPN connections.
 * This abstraction allows for easier testing and decoupling.
 */
interface VpnConnectionStarter {
    /**
     * Starts a VPN connection using the specified profile.
     *
     * @param context The context to use for starting the connection
     * @param profileId The ID of the profile to connect with
     */
    fun startVpnConnection(
        context: Context,
        profileId: Long,
    )
}
