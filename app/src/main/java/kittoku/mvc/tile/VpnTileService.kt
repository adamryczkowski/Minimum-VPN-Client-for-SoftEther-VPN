package kittoku.mvc.tile

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import kittoku.mvc.R
import kittoku.mvc.connection.ConnectionState
import kittoku.mvc.connection.ConnectionStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

/**
 * Quick Settings tile service for VPN connection.
 *
 * This service provides a Quick Settings tile that allows users to
 * quickly connect or disconnect from the VPN.
 */
class VpnTileService : TileService() {
    private val stateManager: ConnectionStateManager by inject()

    private var serviceScope: CoroutineScope? = null
    private var stateCollectionJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()

        serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

        // Collect state changes and update tile
        stateCollectionJob =
            serviceScope?.launch {
                stateManager.state.collectLatest { state ->
                    updateTile(state)
                }
            }
    }

    override fun onStopListening() {
        super.onStopListening()

        stateCollectionJob?.cancel()
        stateCollectionJob = null
        serviceScope?.cancel()
        serviceScope = null
    }

    override fun onClick() {
        super.onClick()

        val tileState = VpnTileState.fromConnectionState(stateManager.currentState)

        if (!tileState.isClickable) {
            return
        }

        when (tileState.action) {
            TileAction.CONNECT -> startVpnConnection()
            TileAction.DISCONNECT -> stopVpnConnection()
            TileAction.NONE -> { /* Do nothing */ }
        }
    }

    private fun updateTile(connectionState: ConnectionState) {
        val tile = qsTile ?: return
        val tileState = VpnTileState.fromConnectionState(connectionState)

        tile.state =
            when (tileState.state) {
                TileState.ACTIVE -> Tile.STATE_ACTIVE
                TileState.INACTIVE -> Tile.STATE_INACTIVE
            }

        tile.label = tileState.label

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = tileState.subtitle
        }

        tile.icon = Icon.createWithResource(this, R.drawable.ic_vpn_key)

        tile.updateTile()
    }

    private fun startVpnConnection() {
        // Send broadcast to start VPN connection
        val intent =
            Intent(ACTION_CONNECT).apply {
                setPackage(packageName)
            }
        sendBroadcast(intent)
    }

    private fun stopVpnConnection() {
        // Send broadcast to stop VPN connection
        val intent =
            Intent(ACTION_DISCONNECT).apply {
                setPackage(packageName)
            }
        sendBroadcast(intent)
    }

    companion object {
        const val ACTION_CONNECT = "kittoku.mvc.action.CONNECT"
        const val ACTION_DISCONNECT = "kittoku.mvc.action.DISCONNECT"
    }
}
