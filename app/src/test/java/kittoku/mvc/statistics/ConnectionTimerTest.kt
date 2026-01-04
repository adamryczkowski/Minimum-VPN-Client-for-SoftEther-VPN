package kittoku.mvc.statistics

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * TDD tests for ConnectionTimer.
 *
 * ConnectionTimer tracks the duration of a VPN connection,
 * providing formatted duration strings and elapsed time in various units.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("ConnectionTimer")
class ConnectionTimerTest {
    private lateinit var connectionTimer: ConnectionTimer

    @BeforeEach
    fun setUp() {
        connectionTimer = ConnectionTimer()
    }

    @Nested
    @DisplayName("Initial state")
    inner class InitialState {
        @Test
        @DisplayName("should not be running initially")
        fun shouldNotBeRunningInitially() {
            assertThat(connectionTimer.isRunning).isFalse()
        }

        @Test
        @DisplayName("should have zero elapsed time initially")
        fun shouldHaveZeroElapsedTimeInitially() {
            assertThat(connectionTimer.elapsedMillis).isEqualTo(0L)
        }

        @Test
        @DisplayName("should have null start time initially")
        fun shouldHaveNullStartTimeInitially() {
            assertThat(connectionTimer.startTime).isNull()
        }
    }

    @Nested
    @DisplayName("Starting timer")
    inner class StartingTimer {
        @Test
        @DisplayName("should set running to true")
        fun shouldSetRunningToTrue() {
            connectionTimer.start()
            assertThat(connectionTimer.isRunning).isTrue()
        }

        @Test
        @DisplayName("should set start time")
        fun shouldSetStartTime() {
            val before = System.currentTimeMillis()
            connectionTimer.start()
            val after = System.currentTimeMillis()

            assertThat(connectionTimer.startTime).isNotNull()
            assertThat(connectionTimer.startTime).isAtLeast(before)
            assertThat(connectionTimer.startTime).isAtMost(after)
        }

        @Test
        @DisplayName("should allow starting with custom time")
        fun shouldAllowStartingWithCustomTime() {
            val customTime = 1000000L
            connectionTimer.start(customTime)
            assertThat(connectionTimer.startTime).isEqualTo(customTime)
        }

        @Test
        @DisplayName("should not reset start time if already running")
        fun shouldNotResetStartTimeIfAlreadyRunning() {
            connectionTimer.start(1000L)
            val originalStartTime = connectionTimer.startTime

            connectionTimer.start(2000L)
            assertThat(connectionTimer.startTime).isEqualTo(originalStartTime)
        }
    }

    @Nested
    @DisplayName("Stopping timer")
    inner class StoppingTimer {
        @Test
        @DisplayName("should set running to false")
        fun shouldSetRunningToFalse() {
            connectionTimer.start()
            connectionTimer.stop()
            assertThat(connectionTimer.isRunning).isFalse()
        }

        @Test
        @DisplayName("should preserve elapsed time after stop")
        fun shouldPreserveElapsedTimeAfterStop() {
            connectionTimer.start(System.currentTimeMillis() - 5000)
            connectionTimer.stop()

            val elapsed = connectionTimer.elapsedMillis
            assertThat(elapsed).isAtLeast(5000L)
        }
    }

    @Nested
    @DisplayName("Resetting timer")
    inner class ResettingTimer {
        @Test
        @DisplayName("should set running to false")
        fun shouldSetRunningToFalse() {
            connectionTimer.start()
            connectionTimer.reset()
            assertThat(connectionTimer.isRunning).isFalse()
        }

        @Test
        @DisplayName("should clear start time")
        fun shouldClearStartTime() {
            connectionTimer.start()
            connectionTimer.reset()
            assertThat(connectionTimer.startTime).isNull()
        }

        @Test
        @DisplayName("should reset elapsed time to zero")
        fun shouldResetElapsedTimeToZero() {
            connectionTimer.start(System.currentTimeMillis() - 5000)
            connectionTimer.reset()
            assertThat(connectionTimer.elapsedMillis).isEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("Elapsed time calculation")
    inner class ElapsedTimeCalculation {
        @Test
        @DisplayName("should calculate elapsed seconds")
        fun shouldCalculateElapsedSeconds() {
            connectionTimer.start(System.currentTimeMillis() - 5000)
            assertThat(connectionTimer.elapsedSeconds).isAtLeast(5L)
        }

        @Test
        @DisplayName("should calculate elapsed minutes")
        fun shouldCalculateElapsedMinutes() {
            connectionTimer.start(System.currentTimeMillis() - 120_000)
            assertThat(connectionTimer.elapsedMinutes).isAtLeast(2L)
        }

        @Test
        @DisplayName("should calculate elapsed hours")
        fun shouldCalculateElapsedHours() {
            connectionTimer.start(System.currentTimeMillis() - 7200_000)
            assertThat(connectionTimer.elapsedHours).isAtLeast(2L)
        }
    }

    @Nested
    @DisplayName("Formatted duration")
    inner class FormattedDuration {
        @Test
        @DisplayName("should format as HH:MM:SS")
        fun shouldFormatAsHhMmSs() {
            // 1 hour, 23 minutes, 45 seconds = 5025 seconds = 5025000 ms
            val duration = (1 * 3600 + 23 * 60 + 45) * 1000L
            connectionTimer.start(System.currentTimeMillis() - duration)

            val formatted = connectionTimer.getFormattedDuration()
            assertThat(formatted).matches("\\d{2}:\\d{2}:\\d{2}")
        }

        @Test
        @DisplayName("should show 00:00:00 when not started")
        fun shouldShowZeroWhenNotStarted() {
            assertThat(connectionTimer.getFormattedDuration()).isEqualTo("00:00:00")
        }

        @Test
        @DisplayName("should handle hours over 99")
        fun shouldHandleHoursOver99() {
            // 100 hours
            val duration = 100 * 3600 * 1000L
            connectionTimer.start(System.currentTimeMillis() - duration)

            val formatted = connectionTimer.getFormattedDuration()
            assertThat(formatted).startsWith("100:")
        }
    }

    @Nested
    @DisplayName("Flow observation")
    inner class FlowObservation {
        @Test
        @DisplayName("should emit initial duration of zero")
        fun shouldEmitInitialDurationOfZero() =
            runTest {
                val duration = connectionTimer.durationFlow.first()
                assertThat(duration).isEqualTo(0L)
            }

        @Test
        @DisplayName("should emit elapsed time when running")
        fun shouldEmitElapsedTimeWhenRunning() =
            runTest {
                connectionTimer.start(System.currentTimeMillis() - 1000)
                val duration = connectionTimer.durationFlow.first()
                assertThat(duration).isAtLeast(1000L)
            }
    }

    @Nested
    @DisplayName("Compact formatted duration")
    inner class CompactFormattedDuration {
        @Test
        @DisplayName("should show seconds only when under 1 minute")
        fun shouldShowSecondsOnlyWhenUnder1Minute() {
            connectionTimer.start(System.currentTimeMillis() - 45_000)
            val formatted = connectionTimer.getCompactDuration()
            assertThat(formatted).isEqualTo("45s")
        }

        @Test
        @DisplayName("should show minutes and seconds when under 1 hour")
        fun shouldShowMinutesAndSecondsWhenUnder1Hour() {
            // 5 minutes 30 seconds
            connectionTimer.start(System.currentTimeMillis() - 330_000)
            val formatted = connectionTimer.getCompactDuration()
            assertThat(formatted).matches("\\d+m \\d+s")
        }

        @Test
        @DisplayName("should show hours and minutes when over 1 hour")
        fun shouldShowHoursAndMinutesWhenOver1Hour() {
            // 2 hours 15 minutes
            connectionTimer.start(System.currentTimeMillis() - 8100_000)
            val formatted = connectionTimer.getCompactDuration()
            assertThat(formatted).matches("\\d+h \\d+m")
        }
    }
}
