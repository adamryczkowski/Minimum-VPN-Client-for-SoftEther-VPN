package kittoku.mvc.statistics

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * TDD tests for StatisticsRepository.
 *
 * StatisticsRepository provides a clean API for managing connection statistics,
 * wrapping the DAO and providing additional business logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("StatisticsRepository")
class StatisticsRepositoryTest {
    private lateinit var dao: ConnectionSessionDao
    private lateinit var repository: StatisticsRepository

    @BeforeEach
    fun setUp() {
        dao = mockk(relaxed = true)
        repository = StatisticsRepository(dao)
    }

    @Nested
    @DisplayName("Saving sessions")
    inner class SavingSessions {
        @Test
        @DisplayName("should save a connection session")
        fun shouldSaveConnectionSession() =
            runTest {
                val session = createTestSession()
                coEvery { dao.insert(session) } returns 1L

                val id = repository.saveSession(session)

                assertThat(id).isEqualTo(1L)
                coVerify { dao.insert(session) }
            }

        @Test
        @DisplayName("should create session from current state")
        fun shouldCreateSessionFromCurrentState() =
            runTest {
                val trafficSnapshot = TrafficSnapshot(bytesSent = 1000L, bytesReceived = 2000L)
                coEvery { dao.insert(any()) } returns 1L

                val id =
                    repository.recordSession(
                        profileId = 1L,
                        serverAddress = "vpn.example.com",
                        startTime = System.currentTimeMillis() - 60_000,
                        trafficSnapshot = trafficSnapshot,
                        wasSuccessful = true,
                    )

                assertThat(id).isEqualTo(1L)
                coVerify {
                    dao.insert(
                        match { session ->
                            session.profileId == 1L &&
                                session.serverAddress == "vpn.example.com" &&
                                session.bytesSent == 1000L &&
                                session.bytesReceived == 2000L
                        },
                    )
                }
            }
    }

    @Nested
    @DisplayName("Retrieving sessions")
    inner class RetrievingSessions {
        @Test
        @DisplayName("should get all sessions")
        fun shouldGetAllSessions() =
            runTest {
                val sessions = listOf(createTestSession(), createTestSession())
                every { dao.getAllSessions() } returns flowOf(sessions)

                val result = repository.getAllSessions().first()

                assertThat(result).hasSize(2)
            }

        @Test
        @DisplayName("should get sessions for profile")
        fun shouldGetSessionsForProfile() =
            runTest {
                val sessions = listOf(createTestSession(profileId = 1L))
                every { dao.getSessionsForProfile(1L) } returns flowOf(sessions)

                val result = repository.getSessionsForProfile(1L).first()

                assertThat(result).hasSize(1)
                assertThat(result[0].profileId).isEqualTo(1L)
            }

        @Test
        @DisplayName("should get recent sessions")
        fun shouldGetRecentSessions() =
            runTest {
                val sessions = listOf(createTestSession())
                every { dao.getRecentSessions(10) } returns flowOf(sessions)

                val result = repository.getRecentSessions(10).first()

                assertThat(result).hasSize(1)
            }

        @Test
        @DisplayName("should get session by ID")
        fun shouldGetSessionById() =
            runTest {
                val session = createTestSession(id = 42L)
                coEvery { dao.getSessionById(42L) } returns session

                val result = repository.getSessionById(42L)

                assertThat(result).isNotNull()
                assertThat(result?.id).isEqualTo(42L)
            }
    }

    @Nested
    @DisplayName("Statistics aggregation")
    inner class StatisticsAggregation {
        @Test
        @DisplayName("should get total bytes sent")
        fun shouldGetTotalBytesSent() =
            runTest {
                coEvery { dao.getTotalBytesSent() } returns 1_000_000L

                val result = repository.getTotalBytesSent()

                assertThat(result).isEqualTo(1_000_000L)
            }

        @Test
        @DisplayName("should get total bytes received")
        fun shouldGetTotalBytesReceived() =
            runTest {
                coEvery { dao.getTotalBytesReceived() } returns 2_000_000L

                val result = repository.getTotalBytesReceived()

                assertThat(result).isEqualTo(2_000_000L)
            }

        @Test
        @DisplayName("should get total duration")
        fun shouldGetTotalDuration() =
            runTest {
                coEvery { dao.getTotalDuration() } returns 3600_000L

                val result = repository.getTotalDuration()

                assertThat(result).isEqualTo(3600_000L)
            }

        @Test
        @DisplayName("should get session count")
        fun shouldGetSessionCount() =
            runTest {
                coEvery { dao.getSessionCount() } returns 25

                val result = repository.getSessionCount()

                assertThat(result).isEqualTo(25)
            }

        @Test
        @DisplayName("should get successful session count")
        fun shouldGetSuccessfulSessionCount() =
            runTest {
                coEvery { dao.getSuccessfulSessionCount() } returns 20

                val result = repository.getSuccessfulSessionCount()

                assertThat(result).isEqualTo(20)
            }

        @Test
        @DisplayName("should calculate success rate")
        fun shouldCalculateSuccessRate() =
            runTest {
                coEvery { dao.getSessionCount() } returns 100
                coEvery { dao.getSuccessfulSessionCount() } returns 80

                val result = repository.getSuccessRate()

                assertThat(result).isWithin(0.01).of(0.8)
            }

        @Test
        @DisplayName("should return zero success rate when no sessions")
        fun shouldReturnZeroSuccessRateWhenNoSessions() =
            runTest {
                coEvery { dao.getSessionCount() } returns 0
                coEvery { dao.getSuccessfulSessionCount() } returns 0

                val result = repository.getSuccessRate()

                assertThat(result).isEqualTo(0.0)
            }
    }

    @Nested
    @DisplayName("Aggregate statistics")
    inner class AggregateStatistics {
        @Test
        @DisplayName("should get aggregate statistics")
        fun shouldGetAggregateStatistics() =
            runTest {
                coEvery { dao.getTotalBytesSent() } returns 1_000_000L
                coEvery { dao.getTotalBytesReceived() } returns 2_000_000L
                coEvery { dao.getTotalDuration() } returns 3600_000L
                coEvery { dao.getSessionCount() } returns 10
                coEvery { dao.getSuccessfulSessionCount() } returns 8

                val stats = repository.getAggregateStatistics()

                assertThat(stats.totalBytesSent).isEqualTo(1_000_000L)
                assertThat(stats.totalBytesReceived).isEqualTo(2_000_000L)
                assertThat(stats.totalDurationMillis).isEqualTo(3600_000L)
                assertThat(stats.sessionCount).isEqualTo(10)
                assertThat(stats.successfulSessionCount).isEqualTo(8)
            }
    }

    @Nested
    @DisplayName("Deleting sessions")
    inner class DeletingSessions {
        @Test
        @DisplayName("should delete a session")
        fun shouldDeleteSession() =
            runTest {
                val session = createTestSession()

                repository.deleteSession(session)

                coVerify { dao.delete(session) }
            }

        @Test
        @DisplayName("should delete all sessions")
        fun shouldDeleteAllSessions() =
            runTest {
                repository.deleteAllSessions()

                coVerify { dao.deleteAll() }
            }

        @Test
        @DisplayName("should delete old sessions")
        fun shouldDeleteOldSessions() =
            runTest {
                val cutoffTime = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
                coEvery { dao.deleteOlderThan(any()) } returns 5

                val deleted = repository.deleteSessionsOlderThan(cutoffTime)

                assertThat(deleted).isEqualTo(5)
                coVerify { dao.deleteOlderThan(cutoffTime) }
            }

        @Test
        @DisplayName("should delete sessions for profile")
        fun shouldDeleteSessionsForProfile() =
            runTest {
                coEvery { dao.deleteForProfile(1L) } returns 3

                val deleted = repository.deleteSessionsForProfile(1L)

                assertThat(deleted).isEqualTo(3)
            }
    }

    private fun createTestSession(
        id: Long = 0L,
        profileId: Long = 1L,
    ): ConnectionSession {
        return ConnectionSession(
            id = id,
            profileId = profileId,
            serverAddress = "vpn.example.com",
            startTime = System.currentTimeMillis() - 60_000,
            endTime = System.currentTimeMillis(),
            bytesSent = 1000L,
            bytesReceived = 2000L,
        )
    }
}
