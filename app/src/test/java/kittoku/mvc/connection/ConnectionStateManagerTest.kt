package kittoku.mvc.connection

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * TDD tests for ConnectionStateManager.
 *
 * These tests define the expected behavior of the ConnectionStateManager
 * before implementation (Red-Green-Refactor cycle).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("ConnectionStateManager")
class ConnectionStateManagerTest {
    private lateinit var stateManager: ConnectionStateManager

    @BeforeEach
    fun setup() {
        stateManager = ConnectionStateManager()
    }

    @Nested
    @DisplayName("Initial state")
    inner class InitialState {
        @Test
        @DisplayName("should start in Disconnected state")
        fun shouldStartInDisconnectedState() =
            runTest {
                val state = stateManager.state.first()
                assertThat(state).isEqualTo(ConnectionState.Disconnected)
            }
    }

    @Nested
    @DisplayName("State transitions")
    inner class StateTransitions {
        @Test
        @DisplayName("should transition from Disconnected to Connecting")
        fun shouldTransitionFromDisconnectedToConnecting() =
            runTest {
                stateManager.startConnecting("Initializing")
                val state = stateManager.state.first()
                assertThat(state).isInstanceOf(ConnectionState.Connecting::class.java)
                assertThat((state as ConnectionState.Connecting).step).isEqualTo("Initializing")
            }

        @Test
        @DisplayName("should update Connecting step")
        fun shouldUpdateConnectingStep() =
            runTest {
                stateManager.startConnecting("Step 1")
                stateManager.updateConnectingStep("Step 2")
                val state = stateManager.state.first()
                assertThat((state as ConnectionState.Connecting).step).isEqualTo("Step 2")
            }

        @Test
        @DisplayName("should update Connecting progress")
        fun shouldUpdateConnectingProgress() =
            runTest {
                stateManager.startConnecting("Step 1", progress = 0)
                stateManager.updateConnectingProgress(50)
                val state = stateManager.state.first()
                assertThat((state as ConnectionState.Connecting).progress).isEqualTo(50)
            }

        @Test
        @DisplayName("should transition from Connecting to Connected")
        fun shouldTransitionFromConnectingToConnected() =
            runTest {
                val stats = createTestStats()
                stateManager.startConnecting("Connecting")
                stateManager.setConnected(stats)
                val state = stateManager.state.first()
                assertThat(state).isInstanceOf(ConnectionState.Connected::class.java)
                assertThat((state as ConnectionState.Connected).stats).isEqualTo(stats)
            }

        @Test
        @DisplayName("should transition from Connected to Disconnecting")
        fun shouldTransitionFromConnectedToDisconnecting() =
            runTest {
                stateManager.startConnecting("Connecting")
                stateManager.setConnected(createTestStats())
                stateManager.startDisconnecting()
                val state = stateManager.state.first()
                assertThat(state).isEqualTo(ConnectionState.Disconnecting)
            }

        @Test
        @DisplayName("should transition from Disconnecting to Disconnected")
        fun shouldTransitionFromDisconnectingToDisconnected() =
            runTest {
                stateManager.startConnecting("Connecting")
                stateManager.setConnected(createTestStats())
                stateManager.startDisconnecting()
                stateManager.setDisconnected()
                val state = stateManager.state.first()
                assertThat(state).isEqualTo(ConnectionState.Disconnected)
            }

        @Test
        @DisplayName("should transition from Connecting to Error")
        fun shouldTransitionFromConnectingToError() =
            runTest {
                stateManager.startConnecting("Connecting")
                stateManager.setError("Connection timeout")
                val state = stateManager.state.first()
                assertThat(state).isInstanceOf(ConnectionState.Error::class.java)
                assertThat((state as ConnectionState.Error).message).isEqualTo("Connection timeout")
            }

        @Test
        @DisplayName("should transition from Connected to Error")
        fun shouldTransitionFromConnectedToError() =
            runTest {
                stateManager.startConnecting("Connecting")
                stateManager.setConnected(createTestStats())
                stateManager.setError("Connection lost")
                val state = stateManager.state.first()
                assertThat(state).isInstanceOf(ConnectionState.Error::class.java)
            }

        @Test
        @DisplayName("should transition from Error to Disconnected")
        fun shouldTransitionFromErrorToDisconnected() =
            runTest {
                stateManager.startConnecting("Connecting")
                stateManager.setError("Error")
                stateManager.setDisconnected()
                val state = stateManager.state.first()
                assertThat(state).isEqualTo(ConnectionState.Disconnected)
            }

        @Test
        @DisplayName("should transition from Error to Connecting for retry")
        fun shouldTransitionFromErrorToConnectingForRetry() =
            runTest {
                stateManager.startConnecting("Connecting")
                stateManager.setError("Timeout", isRecoverable = true)
                stateManager.startConnecting("Retrying")
                val state = stateManager.state.first()
                assertThat(state).isInstanceOf(ConnectionState.Connecting::class.java)
            }
    }

    @Nested
    @DisplayName("Stats updates")
    inner class StatsUpdates {
        @Test
        @DisplayName("should update stats while connected")
        fun shouldUpdateStatsWhileConnected() =
            runTest {
                val initialStats = createTestStats()
                stateManager.startConnecting("Connecting")
                stateManager.setConnected(initialStats)

                val updatedStats = initialStats.copy(bytesSent = 1000L, bytesReceived = 2000L)
                stateManager.updateStats(updatedStats)

                val state = stateManager.state.first()
                assertThat((state as ConnectionState.Connected).stats.bytesSent).isEqualTo(1000L)
                assertThat(state.stats.bytesReceived).isEqualTo(2000L)
            }

        @Test
        @DisplayName("should ignore stats update when not connected")
        fun shouldIgnoreStatsUpdateWhenNotConnected() =
            runTest {
                val stats = createTestStats()
                stateManager.updateStats(stats)
                val state = stateManager.state.first()
                assertThat(state).isEqualTo(ConnectionState.Disconnected)
            }
    }

    @Nested
    @DisplayName("State observation")
    inner class StateObservation {
        @Test
        @DisplayName("should emit state changes to collectors")
        fun shouldEmitStateChangesToCollectors() =
            runTest {
                val states = mutableListOf<ConnectionState>()

                // Collect initial state
                states.add(stateManager.state.first())

                stateManager.startConnecting("Step 1")
                states.add(stateManager.state.first())

                stateManager.setConnected(createTestStats())
                states.add(stateManager.state.first())

                assertThat(states).hasSize(3)
                assertThat(states[0]).isEqualTo(ConnectionState.Disconnected)
                assertThat(states[1]).isInstanceOf(ConnectionState.Connecting::class.java)
                assertThat(states[2]).isInstanceOf(ConnectionState.Connected::class.java)
            }

        @Test
        @DisplayName("should provide current state synchronously")
        fun shouldProvideCurrentStateSynchronously() =
            runTest {
                assertThat(stateManager.currentState).isEqualTo(ConnectionState.Disconnected)

                stateManager.startConnecting("Connecting")
                assertThat(stateManager.currentState).isInstanceOf(ConnectionState.Connecting::class.java)
            }
    }

    @Nested
    @DisplayName("Error handling")
    inner class ErrorHandling {
        @Test
        @DisplayName("should set error with cause exception")
        fun shouldSetErrorWithCauseException() =
            runTest {
                val cause = RuntimeException("Network error")
                stateManager.startConnecting("Connecting")
                stateManager.setError("Connection failed", cause = cause)
                val state = stateManager.state.first() as ConnectionState.Error
                assertThat(state.cause).isEqualTo(cause)
            }

        @Test
        @DisplayName("should set recoverable error")
        fun shouldSetRecoverableError() =
            runTest {
                stateManager.startConnecting("Connecting")
                stateManager.setError("Timeout", isRecoverable = true)
                val state = stateManager.state.first() as ConnectionState.Error
                assertThat(state.isRecoverable).isTrue()
            }
    }

    @Nested
    @DisplayName("Single source of truth behavior")
    inner class SingleSourceOfTruth {
        @Test
        @DisplayName("should be usable as the single source of truth for connection state")
        fun shouldBeUsableAsSingleSourceOfTruth() =
            runTest {
                // Verify that the state manager can be used as the single source of truth
                // by multiple consumers observing the same state

                // Initial state
                assertThat(stateManager.state.first()).isEqualTo(ConnectionState.Disconnected)
                assertThat(stateManager.currentState).isEqualTo(ConnectionState.Disconnected)

                // State changes are immediately visible
                stateManager.startConnecting("Step 1")
                assertThat(stateManager.state.first()).isInstanceOf(ConnectionState.Connecting::class.java)
                assertThat(stateManager.currentState).isInstanceOf(ConnectionState.Connecting::class.java)

                // Connected state with stats
                val stats = createTestStats()
                stateManager.setConnected(stats)
                assertThat(stateManager.state.first()).isInstanceOf(ConnectionState.Connected::class.java)
                assertThat((stateManager.currentState as ConnectionState.Connected).stats).isEqualTo(stats)
            }

        @Test
        @DisplayName("should maintain state consistency across multiple operations")
        fun shouldMaintainStateConsistencyAcrossMultipleOperations() =
            runTest {
                // Simulate a typical connection lifecycle
                stateManager.startConnecting("Initializing")
                assertThat(stateManager.currentState.isConnecting).isTrue()

                stateManager.updateConnectingStep("Establishing SSL")
                assertThat((stateManager.currentState as ConnectionState.Connecting).step)
                    .isEqualTo("Establishing SSL")

                stateManager.updateConnectingProgress(50)
                assertThat((stateManager.currentState as ConnectionState.Connecting).progress)
                    .isEqualTo(50)

                stateManager.setConnected(createTestStats())
                assertThat(stateManager.currentState.isConnected).isTrue()

                stateManager.startDisconnecting()
                assertThat(stateManager.currentState).isEqualTo(ConnectionState.Disconnecting)

                stateManager.setDisconnected()
                assertThat(stateManager.currentState).isEqualTo(ConnectionState.Disconnected)
            }
    }

    private fun createTestStats(): ConnectionStats =
        ConnectionStats(
            connectedAt = System.currentTimeMillis(),
            serverAddress = "vpn.example.com",
            assignedIp = "10.0.0.5",
        )
}
