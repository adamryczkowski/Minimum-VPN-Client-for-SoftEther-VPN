package kittoku.mvc.connection

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * TDD tests for ConnectionState sealed class.
 *
 * These tests define the expected behavior of the ConnectionState
 * before implementation (Red-Green-Refactor cycle).
 */
@DisplayName("ConnectionState")
class ConnectionStateTest {
    @Nested
    @DisplayName("Disconnected state")
    inner class DisconnectedState {
        @Test
        @DisplayName("should be a singleton object")
        fun shouldBeSingletonObject() {
            val state1 = ConnectionState.Disconnected
            val state2 = ConnectionState.Disconnected
            assertThat(state1).isSameInstanceAs(state2)
        }

        @Test
        @DisplayName("should have isConnected = false")
        fun shouldHaveIsConnectedFalse() {
            val state = ConnectionState.Disconnected
            assertThat(state.isConnected).isFalse()
        }

        @Test
        @DisplayName("should have isConnecting = false")
        fun shouldHaveIsConnectingFalse() {
            val state = ConnectionState.Disconnected
            assertThat(state.isConnecting).isFalse()
        }

        @Test
        @DisplayName("should have displayName")
        fun shouldHaveDisplayName() {
            val state = ConnectionState.Disconnected
            assertThat(state.displayName).isEqualTo("Disconnected")
        }
    }

    @Nested
    @DisplayName("Connecting state")
    inner class ConnectingState {
        @Test
        @DisplayName("should contain step description")
        fun shouldContainStepDescription() {
            val state = ConnectionState.Connecting(step = "Establishing SSL connection")
            assertThat(state.step).isEqualTo("Establishing SSL connection")
        }

        @Test
        @DisplayName("should have isConnected = false")
        fun shouldHaveIsConnectedFalse() {
            val state = ConnectionState.Connecting(step = "Connecting")
            assertThat(state.isConnected).isFalse()
        }

        @Test
        @DisplayName("should have isConnecting = true")
        fun shouldHaveIsConnectingTrue() {
            val state = ConnectionState.Connecting(step = "Connecting")
            assertThat(state.isConnecting).isTrue()
        }

        @Test
        @DisplayName("should have displayName with step")
        fun shouldHaveDisplayNameWithStep() {
            val state = ConnectionState.Connecting(step = "DHCP negotiation")
            assertThat(state.displayName).isEqualTo("Connecting: DHCP negotiation")
        }

        @Test
        @DisplayName("should support progress percentage")
        fun shouldSupportProgressPercentage() {
            val state = ConnectionState.Connecting(step = "Step 2", progress = 50)
            assertThat(state.progress).isEqualTo(50)
        }

        @Test
        @DisplayName("should default progress to null")
        fun shouldDefaultProgressToNull() {
            val state = ConnectionState.Connecting(step = "Connecting")
            assertThat(state.progress).isNull()
        }
    }

    @Nested
    @DisplayName("Connected state")
    inner class ConnectedState {
        @Test
        @DisplayName("should contain connection stats")
        fun shouldContainConnectionStats() {
            val stats =
                ConnectionStats(
                    connectedAt = 1000L,
                    serverAddress = "vpn.example.com",
                    assignedIp = "10.0.0.5",
                )
            val state = ConnectionState.Connected(stats = stats)
            assertThat(state.stats).isEqualTo(stats)
        }

        @Test
        @DisplayName("should have isConnected = true")
        fun shouldHaveIsConnectedTrue() {
            val stats = createTestStats()
            val state = ConnectionState.Connected(stats = stats)
            assertThat(state.isConnected).isTrue()
        }

        @Test
        @DisplayName("should have isConnecting = false")
        fun shouldHaveIsConnectingFalse() {
            val stats = createTestStats()
            val state = ConnectionState.Connected(stats = stats)
            assertThat(state.isConnecting).isFalse()
        }

        @Test
        @DisplayName("should have displayName")
        fun shouldHaveDisplayName() {
            val stats = createTestStats()
            val state = ConnectionState.Connected(stats = stats)
            assertThat(state.displayName).isEqualTo("Connected")
        }
    }

    @Nested
    @DisplayName("Disconnecting state")
    inner class DisconnectingState {
        @Test
        @DisplayName("should be a singleton object")
        fun shouldBeSingletonObject() {
            val state1 = ConnectionState.Disconnecting
            val state2 = ConnectionState.Disconnecting
            assertThat(state1).isSameInstanceAs(state2)
        }

        @Test
        @DisplayName("should have isConnected = false")
        fun shouldHaveIsConnectedFalse() {
            val state = ConnectionState.Disconnecting
            assertThat(state.isConnected).isFalse()
        }

        @Test
        @DisplayName("should have isConnecting = false")
        fun shouldHaveIsConnectingFalse() {
            val state = ConnectionState.Disconnecting
            assertThat(state.isConnecting).isFalse()
        }

        @Test
        @DisplayName("should have displayName")
        fun shouldHaveDisplayName() {
            val state = ConnectionState.Disconnecting
            assertThat(state.displayName).isEqualTo("Disconnecting")
        }
    }

    @Nested
    @DisplayName("Error state")
    inner class ErrorState {
        @Test
        @DisplayName("should contain error message")
        fun shouldContainErrorMessage() {
            val state = ConnectionState.Error(message = "Connection timeout")
            assertThat(state.message).isEqualTo("Connection timeout")
        }

        @Test
        @DisplayName("should have isConnected = false")
        fun shouldHaveIsConnectedFalse() {
            val state = ConnectionState.Error(message = "Error")
            assertThat(state.isConnected).isFalse()
        }

        @Test
        @DisplayName("should have isConnecting = false")
        fun shouldHaveIsConnectingFalse() {
            val state = ConnectionState.Error(message = "Error")
            assertThat(state.isConnecting).isFalse()
        }

        @Test
        @DisplayName("should have displayName with error")
        fun shouldHaveDisplayNameWithError() {
            val state = ConnectionState.Error(message = "Authentication failed")
            assertThat(state.displayName).isEqualTo("Error: Authentication failed")
        }

        @Test
        @DisplayName("should support optional cause exception")
        fun shouldSupportOptionalCauseException() {
            val cause = RuntimeException("Network error")
            val state = ConnectionState.Error(message = "Connection failed", cause = cause)
            assertThat(state.cause).isEqualTo(cause)
        }

        @Test
        @DisplayName("should default cause to null")
        fun shouldDefaultCauseToNull() {
            val state = ConnectionState.Error(message = "Error")
            assertThat(state.cause).isNull()
        }

        @Test
        @DisplayName("should support recoverable flag")
        fun shouldSupportRecoverableFlag() {
            val state = ConnectionState.Error(message = "Timeout", isRecoverable = true)
            assertThat(state.isRecoverable).isTrue()
        }

        @Test
        @DisplayName("should default isRecoverable to false")
        fun shouldDefaultIsRecoverableToFalse() {
            val state = ConnectionState.Error(message = "Error")
            assertThat(state.isRecoverable).isFalse()
        }
    }

    private fun createTestStats(): ConnectionStats =
        ConnectionStats(
            connectedAt = System.currentTimeMillis(),
            serverAddress = "vpn.example.com",
            assignedIp = "10.0.0.5",
        )
}
