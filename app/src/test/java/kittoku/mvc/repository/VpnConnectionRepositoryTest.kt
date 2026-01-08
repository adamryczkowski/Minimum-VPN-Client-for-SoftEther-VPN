package kittoku.mvc.repository

import com.google.common.truth.Truth.assertThat
import kittoku.mvc.connection.ConnectionState
import kittoku.mvc.connection.ConnectionStateManager
import kittoku.mvc.connection.ConnectionStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for VpnConnectionRepository.
 *
 * These tests verify that VpnConnectionRepository correctly delegates
 * to ConnectionStateManager as the single source of truth for connection state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("VpnConnectionRepository")
class VpnConnectionRepositoryTest {
    private lateinit var stateManager: ConnectionStateManager
    private lateinit var repository: FakeVpnConnectionRepository

    @BeforeEach
    fun setup() {
        stateManager = ConnectionStateManager()
        repository = FakeVpnConnectionRepository(stateManager)
    }

    @Nested
    @DisplayName("Delegation to ConnectionStateManager")
    inner class DelegationTests {
        @Test
        @DisplayName("should expose connection state from StateManager")
        fun shouldExposeConnectionStateFromStateManager() =
            runTest {
                // Initial state should be Disconnected
                assertThat(repository.connectionState.first()).isEqualTo(ConnectionState.Disconnected)

                // When StateManager changes state
                stateManager.startConnecting("Test")

                // Repository should reflect the change
                assertThat(repository.connectionState.first()).isInstanceOf(ConnectionState.Connecting::class.java)
            }

        @Test
        @DisplayName("should delegate setConnecting to StateManager")
        fun shouldDelegateSetConnectingToStateManager() =
            runTest {
                repository.setConnecting("Starting VPN...")

                val state = stateManager.state.first()
                assertThat(state).isInstanceOf(ConnectionState.Connecting::class.java)
                assertThat((state as ConnectionState.Connecting).step).isEqualTo("Starting VPN...")
            }

        @Test
        @DisplayName("should delegate setConnected to StateManager")
        fun shouldDelegateSetConnectedToStateManager() =
            runTest {
                repository.setConnected("10.0.0.5", "vpn.example.com")

                val state = stateManager.state.first()
                assertThat(state).isInstanceOf(ConnectionState.Connected::class.java)
                val connectedState = state as ConnectionState.Connected
                assertThat(connectedState.stats.assignedIp).isEqualTo("10.0.0.5")
                assertThat(connectedState.stats.serverAddress).isEqualTo("vpn.example.com")
            }

        @Test
        @DisplayName("should delegate setDisconnected to StateManager")
        fun shouldDelegateSetDisconnectedToStateManager() =
            runTest {
                // First connect
                stateManager.startConnecting("Test")
                stateManager.setConnected(createTestStats())

                // Then disconnect via repository
                repository.setDisconnected()

                val state = stateManager.state.first()
                assertThat(state).isEqualTo(ConnectionState.Disconnected)
            }

        @Test
        @DisplayName("should delegate setError to StateManager")
        fun shouldDelegateSetErrorToStateManager() =
            runTest {
                val cause = RuntimeException("Network error")
                repository.setError("Connection failed", cause)

                val state = stateManager.state.first()
                assertThat(state).isInstanceOf(ConnectionState.Error::class.java)
                val errorState = state as ConnectionState.Error
                assertThat(errorState.message).isEqualTo("Connection failed")
                assertThat(errorState.cause).isEqualTo(cause)
            }

        @Test
        @DisplayName("should delegate updateConnectionState to StateManager")
        fun shouldDelegateUpdateConnectionStateToStateManager() =
            runTest {
                val stats = createTestStats()
                val connectedState = ConnectionState.Connected(stats)

                repository.updateConnectionState(connectedState)

                val state = stateManager.state.first()
                assertThat(state).isInstanceOf(ConnectionState.Connected::class.java)
            }
    }

    @Nested
    @DisplayName("isConnected flow")
    inner class IsConnectedFlowTests {
        @Test
        @DisplayName("should return false when disconnected")
        fun shouldReturnFalseWhenDisconnected() =
            runTest {
                assertThat(repository.isConnected.first()).isFalse()
            }

        @Test
        @DisplayName("should return true when connected")
        fun shouldReturnTrueWhenConnected() =
            runTest {
                stateManager.setConnected(createTestStats())

                assertThat(repository.isConnected.first()).isTrue()
            }

        @Test
        @DisplayName("should return false when connecting")
        fun shouldReturnFalseWhenConnecting() =
            runTest {
                stateManager.startConnecting("Test")

                assertThat(repository.isConnected.first()).isFalse()
            }

        @Test
        @DisplayName("should return false when error")
        fun shouldReturnFalseWhenError() =
            runTest {
                stateManager.setError("Error")

                assertThat(repository.isConnected.first()).isFalse()
            }
    }

    @Nested
    @DisplayName("Single source of truth")
    inner class SingleSourceOfTruthTests {
        @Test
        @DisplayName("should reflect StateManager changes immediately")
        fun shouldReflectStateManagerChangesImmediately() =
            runTest {
                // Verify initial state
                assertThat(repository.connectionState.first()).isEqualTo(ConnectionState.Disconnected)

                // Change state via StateManager
                stateManager.startConnecting("Step 1")
                assertThat(repository.connectionState.first()).isInstanceOf(ConnectionState.Connecting::class.java)

                // Change state via Repository
                repository.setDisconnected()
                assertThat(stateManager.state.first()).isEqualTo(ConnectionState.Disconnected)

                // Both should be in sync
                assertThat(repository.connectionState.first()).isEqualTo(stateManager.state.first())
            }

        @Test
        @DisplayName("should maintain consistency between repository and StateManager")
        fun shouldMaintainConsistencyBetweenRepositoryAndStateManager() =
            runTest {
                // Perform various state changes
                repository.setConnecting("Initializing")
                assertThat(stateManager.currentState).isInstanceOf(ConnectionState.Connecting::class.java)

                repository.setConnected("10.0.0.1", "server.example.com")
                assertThat(stateManager.currentState).isInstanceOf(ConnectionState.Connected::class.java)

                repository.setError("Connection lost")
                assertThat(stateManager.currentState).isInstanceOf(ConnectionState.Error::class.java)

                repository.setDisconnected()
                assertThat(stateManager.currentState).isEqualTo(ConnectionState.Disconnected)
            }
    }

    private fun createTestStats(): ConnectionStats =
        ConnectionStats(
            connectedAt = System.currentTimeMillis(),
            serverAddress = "vpn.example.com",
            assignedIp = "10.0.0.5",
        )

    /**
     * Fake implementation of VpnConnectionRepository for testing.
     * This avoids the need for Android context in unit tests.
     * It mirrors the delegation logic of the real VpnConnectionRepository.
     */
    private class FakeVpnConnectionRepository(
        private val stateManager: ConnectionStateManager,
    ) {
        private val scope = CoroutineScope(Dispatchers.Unconfined)

        val connectionState: StateFlow<ConnectionState> = stateManager.state

        private val _isConnected = MutableStateFlow(false)
        val isConnected: StateFlow<Boolean> = _isConnected

        init {
            scope.launch {
                stateManager.state.collect { state ->
                    _isConnected.value = state.isConnected
                }
            }
        }

        fun updateConnectionState(state: ConnectionState) {
            when (state) {
                is ConnectionState.Disconnected -> stateManager.setDisconnected()
                is ConnectionState.Connecting -> stateManager.startConnecting(state.step, state.progress)
                is ConnectionState.Connected -> stateManager.setConnected(state.stats)
                is ConnectionState.Disconnecting -> stateManager.startDisconnecting()
                is ConnectionState.Error -> stateManager.setError(state.message, state.cause, state.isRecoverable)
            }
        }

        fun setConnecting(step: String) {
            stateManager.startConnecting(step)
        }

        fun setConnected(
            assignedIp: String,
            serverIp: String,
        ) {
            val stats =
                ConnectionStats(
                    connectedAt = System.currentTimeMillis(),
                    bytesSent = 0,
                    bytesReceived = 0,
                    isUdpAccelerated = false,
                    assignedIp = assignedIp,
                    serverAddress = serverIp,
                )
            stateManager.setConnected(stats)
        }

        fun setDisconnected() {
            stateManager.setDisconnected()
        }

        fun setError(
            message: String,
            cause: Throwable? = null,
        ) {
            stateManager.setError(message, cause)
        }
    }
}
