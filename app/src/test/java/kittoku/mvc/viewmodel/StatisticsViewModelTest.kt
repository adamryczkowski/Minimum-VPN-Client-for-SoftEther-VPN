package kittoku.mvc.viewmodel

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kittoku.mvc.statistics.AggregateStatistics
import kittoku.mvc.statistics.ConnectionSession
import kittoku.mvc.statistics.ConnectionTimer
import kittoku.mvc.statistics.StatisticsRepository
import kittoku.mvc.statistics.TrafficCounter
import kittoku.mvc.statistics.TrafficSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * TDD tests for StatisticsViewModel.
 *
 * StatisticsViewModel provides UI state for displaying connection statistics,
 * including current session stats, historical data, and aggregate statistics.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("StatisticsViewModel")
class StatisticsViewModelTest {
    private lateinit var repository: StatisticsRepository
    private lateinit var trafficCounter: TrafficCounter
    private lateinit var connectionTimer: ConnectionTimer
    private lateinit var viewModel: StatisticsViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val trafficFlow = MutableStateFlow(TrafficSnapshot(0L, 0L))
    private val durationFlow = MutableStateFlow(0L)

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        trafficCounter = mockk(relaxed = true)
        connectionTimer = mockk(relaxed = true)

        // Default mock values
        every { trafficCounter.trafficFlow } returns trafficFlow
        every { connectionTimer.durationFlow } returns durationFlow
        every { repository.getRecentSessions(any()) } returns flowOf(emptyList())
        coEvery { repository.getAggregateStatistics() } returns AggregateStatistics.EMPTY
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        // Reset flows
        trafficFlow.value = TrafficSnapshot(0L, 0L)
        durationFlow.value = 0L
    }

    @Nested
    @DisplayName("Current session stats")
    inner class CurrentSessionStats {
        @Test
        @DisplayName("should have currentTraffic StateFlow")
        fun shouldHaveCurrentTrafficStateFlow() =
            runTest {
                viewModel = StatisticsViewModel(repository, trafficCounter, connectionTimer)
                testDispatcher.scheduler.advanceUntilIdle()

                // Verify the flow exists and has initial value
                assertThat(viewModel.currentTraffic).isNotNull()
                assertThat(viewModel.currentTraffic.value).isNotNull()
            }

        @Test
        @DisplayName("should have currentDuration StateFlow")
        fun shouldHaveCurrentDurationStateFlow() =
            runTest {
                viewModel = StatisticsViewModel(repository, trafficCounter, connectionTimer)
                testDispatcher.scheduler.advanceUntilIdle()

                // Verify the flow exists and has initial value
                assertThat(viewModel.currentDuration).isNotNull()
                assertThat(viewModel.currentDuration.value).isAtLeast(0L)
            }
    }

    @Nested
    @DisplayName("Aggregate statistics")
    inner class AggregateStats {
        @Test
        @DisplayName("should load aggregate statistics on init")
        fun shouldLoadAggregateStatisticsOnInit() =
            runTest {
                val stats =
                    AggregateStatistics(
                        totalBytesSent = 1_000_000L,
                        totalBytesReceived = 2_000_000L,
                        totalDurationMillis = 3600_000L,
                        sessionCount = 10,
                        successfulSessionCount = 8,
                    )
                coEvery { repository.getAggregateStatistics() } returns stats

                viewModel = StatisticsViewModel(repository, trafficCounter, connectionTimer)
                testDispatcher.scheduler.advanceUntilIdle()

                assertThat(viewModel.aggregateStatistics.value.totalBytesSent).isEqualTo(1_000_000L)
                assertThat(viewModel.aggregateStatistics.value.sessionCount).isEqualTo(10)
            }
    }

    @Nested
    @DisplayName("UI state")
    inner class UiState {
        @Test
        @DisplayName("should indicate not loading after data is fetched")
        fun shouldIndicateNotLoadingAfterDataIsFetched() =
            runTest {
                viewModel = StatisticsViewModel(repository, trafficCounter, connectionTimer)
                testDispatcher.scheduler.advanceUntilIdle()

                assertThat(viewModel.isLoading.value).isFalse()
            }
    }

    @Nested
    @DisplayName("Refresh")
    inner class Refresh {
        @Test
        @DisplayName("should call repository to refresh statistics")
        fun shouldCallRepositoryToRefreshStatistics() =
            runTest {
                viewModel = StatisticsViewModel(repository, trafficCounter, connectionTimer)
                testDispatcher.scheduler.advanceUntilIdle()

                viewModel.refresh()
                testDispatcher.scheduler.advanceUntilIdle()

                // Should have called getAggregateStatistics twice (init + refresh)
                coVerify(exactly = 2) { repository.getAggregateStatistics() }
            }
    }

    @Nested
    @DisplayName("Delete operations")
    inner class DeleteOperations {
        @Test
        @DisplayName("should delete session through repository")
        fun shouldDeleteSessionThroughRepository() =
            runTest {
                val session = createTestSession()
                viewModel = StatisticsViewModel(repository, trafficCounter, connectionTimer)
                testDispatcher.scheduler.advanceUntilIdle()

                viewModel.deleteSession(session)
                testDispatcher.scheduler.advanceUntilIdle()

                coVerify { repository.deleteSession(session) }
            }

        @Test
        @DisplayName("should delete all sessions through repository")
        fun shouldDeleteAllSessionsThroughRepository() =
            runTest {
                viewModel = StatisticsViewModel(repository, trafficCounter, connectionTimer)
                testDispatcher.scheduler.advanceUntilIdle()

                viewModel.deleteAllSessions()
                testDispatcher.scheduler.advanceUntilIdle()

                coVerify { repository.deleteAllSessions() }
            }
    }

    private fun createTestSession(id: Long = 0L): ConnectionSession {
        return ConnectionSession(
            id = id,
            profileId = 1L,
            serverAddress = "vpn.example.com",
            startTime = System.currentTimeMillis() - 60_000,
            endTime = System.currentTimeMillis(),
            bytesSent = 1000L,
            bytesReceived = 2000L,
        )
    }
}
