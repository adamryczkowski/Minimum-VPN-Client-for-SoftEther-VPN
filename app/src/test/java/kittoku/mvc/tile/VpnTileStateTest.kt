package kittoku.mvc.tile

import com.google.common.truth.Truth.assertThat
import kittoku.mvc.connection.ConnectionState
import kittoku.mvc.connection.ConnectionStats
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * TDD tests for VpnTileState.
 *
 * These tests define the expected behavior of the Quick Settings tile state
 * before implementation (Red-Green-Refactor cycle).
 */
@DisplayName("VpnTileState")
class VpnTileStateTest {
    @Nested
    @DisplayName("State mapping")
    inner class StateMapping {
        @Test
        @DisplayName("should map Disconnected to INACTIVE")
        fun shouldMapDisconnectedToInactive() {
            val tileState = VpnTileState.fromConnectionState(ConnectionState.Disconnected)
            assertThat(tileState.state).isEqualTo(TileState.INACTIVE)
        }

        @Test
        @DisplayName("should map Connecting to ACTIVE")
        fun shouldMapConnectingToActive() {
            val state = ConnectionState.Connecting(step = "Connecting")
            val tileState = VpnTileState.fromConnectionState(state)
            assertThat(tileState.state).isEqualTo(TileState.ACTIVE)
        }

        @Test
        @DisplayName("should map Connected to ACTIVE")
        fun shouldMapConnectedToActive() {
            val stats = createTestStats()
            val state = ConnectionState.Connected(stats = stats)
            val tileState = VpnTileState.fromConnectionState(state)
            assertThat(tileState.state).isEqualTo(TileState.ACTIVE)
        }

        @Test
        @DisplayName("should map Disconnecting to ACTIVE")
        fun shouldMapDisconnectingToActive() {
            val tileState = VpnTileState.fromConnectionState(ConnectionState.Disconnecting)
            assertThat(tileState.state).isEqualTo(TileState.ACTIVE)
        }

        @Test
        @DisplayName("should map Error to INACTIVE")
        fun shouldMapErrorToInactive() {
            val state = ConnectionState.Error(message = "Error")
            val tileState = VpnTileState.fromConnectionState(state)
            assertThat(tileState.state).isEqualTo(TileState.INACTIVE)
        }
    }

    @Nested
    @DisplayName("Label generation")
    inner class LabelGeneration {
        @Test
        @DisplayName("should show 'VPN' for Disconnected state")
        fun shouldShowVpnForDisconnectedState() {
            val tileState = VpnTileState.fromConnectionState(ConnectionState.Disconnected)
            assertThat(tileState.label).isEqualTo("VPN")
        }

        @Test
        @DisplayName("should show 'Connecting...' for Connecting state")
        fun shouldShowConnectingForConnectingState() {
            val state = ConnectionState.Connecting(step = "Step")
            val tileState = VpnTileState.fromConnectionState(state)
            assertThat(tileState.label).isEqualTo("Connecting...")
        }

        @Test
        @DisplayName("should show 'VPN Connected' for Connected state")
        fun shouldShowVpnConnectedForConnectedState() {
            val stats = createTestStats()
            val state = ConnectionState.Connected(stats = stats)
            val tileState = VpnTileState.fromConnectionState(state)
            assertThat(tileState.label).isEqualTo("VPN Connected")
        }

        @Test
        @DisplayName("should show 'Disconnecting...' for Disconnecting state")
        fun shouldShowDisconnectingForDisconnectingState() {
            val tileState = VpnTileState.fromConnectionState(ConnectionState.Disconnecting)
            assertThat(tileState.label).isEqualTo("Disconnecting...")
        }

        @Test
        @DisplayName("should show 'VPN Error' for Error state")
        fun shouldShowVpnErrorForErrorState() {
            val state = ConnectionState.Error(message = "Error")
            val tileState = VpnTileState.fromConnectionState(state)
            assertThat(tileState.label).isEqualTo("VPN Error")
        }
    }

    @Nested
    @DisplayName("Subtitle generation")
    inner class SubtitleGeneration {
        @Test
        @DisplayName("should show 'Tap to connect' for Disconnected state")
        fun shouldShowTapToConnectForDisconnectedState() {
            val tileState = VpnTileState.fromConnectionState(ConnectionState.Disconnected)
            assertThat(tileState.subtitle).isEqualTo("Tap to connect")
        }

        @Test
        @DisplayName("should show step for Connecting state")
        fun shouldShowStepForConnectingState() {
            val state = ConnectionState.Connecting(step = "DHCP negotiation")
            val tileState = VpnTileState.fromConnectionState(state)
            assertThat(tileState.subtitle).isEqualTo("DHCP negotiation")
        }

        @Test
        @DisplayName("should show server for Connected state")
        fun shouldShowServerForConnectedState() {
            val stats =
                ConnectionStats(
                    connectedAt = System.currentTimeMillis(),
                    serverAddress = "vpn.example.com",
                )
            val state = ConnectionState.Connected(stats = stats)
            val tileState = VpnTileState.fromConnectionState(state)
            assertThat(tileState.subtitle).isEqualTo("vpn.example.com")
        }

        @Test
        @DisplayName("should show empty subtitle for Disconnecting state")
        fun shouldShowEmptySubtitleForDisconnectingState() {
            val tileState = VpnTileState.fromConnectionState(ConnectionState.Disconnecting)
            assertThat(tileState.subtitle).isEmpty()
        }

        @Test
        @DisplayName("should show error message for Error state")
        fun shouldShowErrorMessageForErrorState() {
            val state = ConnectionState.Error(message = "Auth failed")
            val tileState = VpnTileState.fromConnectionState(state)
            assertThat(tileState.subtitle).isEqualTo("Auth failed")
        }
    }

    @Nested
    @DisplayName("Clickability")
    inner class Clickability {
        @Test
        @DisplayName("should be clickable for Disconnected state")
        fun shouldBeClickableForDisconnectedState() {
            val tileState = VpnTileState.fromConnectionState(ConnectionState.Disconnected)
            assertThat(tileState.isClickable).isTrue()
        }

        @Test
        @DisplayName("should not be clickable for Connecting state")
        fun shouldNotBeClickableForConnectingState() {
            val state = ConnectionState.Connecting(step = "Connecting")
            val tileState = VpnTileState.fromConnectionState(state)
            assertThat(tileState.isClickable).isFalse()
        }

        @Test
        @DisplayName("should be clickable for Connected state")
        fun shouldBeClickableForConnectedState() {
            val stats = createTestStats()
            val state = ConnectionState.Connected(stats = stats)
            val tileState = VpnTileState.fromConnectionState(state)
            assertThat(tileState.isClickable).isTrue()
        }

        @Test
        @DisplayName("should not be clickable for Disconnecting state")
        fun shouldNotBeClickableForDisconnectingState() {
            val tileState = VpnTileState.fromConnectionState(ConnectionState.Disconnecting)
            assertThat(tileState.isClickable).isFalse()
        }

        @Test
        @DisplayName("should be clickable for Error state")
        fun shouldBeClickableForErrorState() {
            val state = ConnectionState.Error(message = "Error")
            val tileState = VpnTileState.fromConnectionState(state)
            assertThat(tileState.isClickable).isTrue()
        }
    }

    @Nested
    @DisplayName("Action determination")
    inner class ActionDetermination {
        @Test
        @DisplayName("should return CONNECT action for Disconnected state")
        fun shouldReturnConnectActionForDisconnectedState() {
            val tileState = VpnTileState.fromConnectionState(ConnectionState.Disconnected)
            assertThat(tileState.action).isEqualTo(TileAction.CONNECT)
        }

        @Test
        @DisplayName("should return NONE action for Connecting state")
        fun shouldReturnNoneActionForConnectingState() {
            val state = ConnectionState.Connecting(step = "Connecting")
            val tileState = VpnTileState.fromConnectionState(state)
            assertThat(tileState.action).isEqualTo(TileAction.NONE)
        }

        @Test
        @DisplayName("should return DISCONNECT action for Connected state")
        fun shouldReturnDisconnectActionForConnectedState() {
            val stats = createTestStats()
            val state = ConnectionState.Connected(stats = stats)
            val tileState = VpnTileState.fromConnectionState(state)
            assertThat(tileState.action).isEqualTo(TileAction.DISCONNECT)
        }

        @Test
        @DisplayName("should return NONE action for Disconnecting state")
        fun shouldReturnNoneActionForDisconnectingState() {
            val tileState = VpnTileState.fromConnectionState(ConnectionState.Disconnecting)
            assertThat(tileState.action).isEqualTo(TileAction.NONE)
        }

        @Test
        @DisplayName("should return CONNECT action for Error state")
        fun shouldReturnConnectActionForErrorState() {
            val state = ConnectionState.Error(message = "Error")
            val tileState = VpnTileState.fromConnectionState(state)
            assertThat(tileState.action).isEqualTo(TileAction.CONNECT)
        }
    }

    private fun createTestStats(): ConnectionStats =
        ConnectionStats(
            connectedAt = System.currentTimeMillis(),
            serverAddress = "vpn.example.com",
            assignedIp = "10.0.0.5",
        )
}
