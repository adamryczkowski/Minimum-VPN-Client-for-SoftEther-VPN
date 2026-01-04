package kittoku.mvc.autoconnect

import android.content.Context
import android.content.Intent
import android.util.Log
import kittoku.mvc.service.SoftEtherVpnService

/**
 * Implementation of [VpnConnectionStarter] that starts the VPN service
 * with the specified profile.
 */
class VpnConnectionStarterImpl : VpnConnectionStarter {
    companion object {
        private const val TAG = "VpnConnectionStarter"
        const val EXTRA_PROFILE_ID = "extra_profile_id"
        const val EXTRA_AUTO_CONNECT = "extra_auto_connect"
    }

    override fun startVpnConnection(
        context: Context,
        profileId: Long,
    ) {
        Log.i(TAG, "Starting VPN connection with profile ID: $profileId")

        val intent =
            Intent(context, SoftEtherVpnService::class.java).apply {
                putExtra(EXTRA_PROFILE_ID, profileId)
                putExtra(EXTRA_AUTO_CONNECT, true)
            }

        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start VPN service", e)
        }
    }
}
