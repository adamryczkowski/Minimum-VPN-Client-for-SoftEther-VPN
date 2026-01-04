package kittoku.mvc.tile

import kittoku.mvc.connection.ConnectionState

/**
 * Represents the state of the Quick Settings tile.
 */
enum class TileState {
    INACTIVE,
    ACTIVE,
}

/**
 * Actions that can be performed when the tile is clicked.
 */
enum class TileAction {
    NONE,
    CONNECT,
    DISCONNECT,
}

/**
 * Data class representing the state of the VPN Quick Settings tile.
 *
 * This class encapsulates all the information needed to update
 * the Quick Settings tile appearance and behavior.
 */
data class VpnTileState(
    /** Current tile state (active/inactive) */
    val state: TileState,
    /** Main label text */
    val label: String,
    /** Subtitle text */
    val subtitle: String,
    /** Whether the tile can be clicked */
    val isClickable: Boolean,
    /** Action to perform when clicked */
    val action: TileAction,
) {
    companion object {
        /**
         * Create tile state from a connection state.
         *
         * @param connectionState Current connection state
         * @return Tile state for the connection state
         */
        fun fromConnectionState(connectionState: ConnectionState): VpnTileState =
            when (connectionState) {
                is ConnectionState.Disconnected -> createDisconnectedTileState()
                is ConnectionState.Connecting -> createConnectingTileState(connectionState)
                is ConnectionState.Connected -> createConnectedTileState(connectionState)
                is ConnectionState.Disconnecting -> createDisconnectingTileState()
                is ConnectionState.Error -> createErrorTileState(connectionState)
            }

        private fun createDisconnectedTileState(): VpnTileState =
            VpnTileState(
                state = TileState.INACTIVE,
                label = "VPN",
                subtitle = "Tap to connect",
                isClickable = true,
                action = TileAction.CONNECT,
            )

        private fun createConnectingTileState(state: ConnectionState.Connecting): VpnTileState =
            VpnTileState(
                state = TileState.ACTIVE,
                label = "Connecting...",
                subtitle = state.step,
                isClickable = false,
                action = TileAction.NONE,
            )

        private fun createConnectedTileState(state: ConnectionState.Connected): VpnTileState =
            VpnTileState(
                state = TileState.ACTIVE,
                label = "VPN Connected",
                subtitle = state.stats.serverAddress,
                isClickable = true,
                action = TileAction.DISCONNECT,
            )

        private fun createDisconnectingTileState(): VpnTileState =
            VpnTileState(
                state = TileState.ACTIVE,
                label = "Disconnecting...",
                subtitle = "",
                isClickable = false,
                action = TileAction.NONE,
            )

        private fun createErrorTileState(state: ConnectionState.Error): VpnTileState =
            VpnTileState(
                state = TileState.INACTIVE,
                label = "VPN Error",
                subtitle = state.message,
                isClickable = true,
                action = TileAction.CONNECT,
            )
    }
}
