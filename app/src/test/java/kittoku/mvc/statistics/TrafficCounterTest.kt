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
 * TDD tests for TrafficCounter.
 *
 * TrafficCounter tracks bytes sent and received through the VPN tunnel
 * in real-time, providing both current values and a Flow for observation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("TrafficCounter")
class TrafficCounterTest {
    private lateinit var trafficCounter: TrafficCounter

    @BeforeEach
    fun setUp() {
        trafficCounter = TrafficCounter()
    }

    @Nested
    @DisplayName("Initial state")
    inner class InitialState {
        @Test
        @DisplayName("should start with zero bytes sent")
        fun shouldStartWithZeroBytesSent() {
            assertThat(trafficCounter.bytesSent).isEqualTo(0L)
        }

        @Test
        @DisplayName("should start with zero bytes received")
        fun shouldStartWithZeroBytesReceived() {
            assertThat(trafficCounter.bytesReceived).isEqualTo(0L)
        }

        @Test
        @DisplayName("should start with zero total bytes")
        fun shouldStartWithZeroTotalBytes() {
            assertThat(trafficCounter.totalBytes).isEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("Adding bytes sent")
    inner class AddingBytesSent {
        @Test
        @DisplayName("should increment bytes sent")
        fun shouldIncrementBytesSent() {
            trafficCounter.addBytesSent(100)
            assertThat(trafficCounter.bytesSent).isEqualTo(100L)
        }

        @Test
        @DisplayName("should accumulate multiple additions")
        fun shouldAccumulateMultipleAdditions() {
            trafficCounter.addBytesSent(100)
            trafficCounter.addBytesSent(200)
            trafficCounter.addBytesSent(300)
            assertThat(trafficCounter.bytesSent).isEqualTo(600L)
        }

        @Test
        @DisplayName("should update total bytes")
        fun shouldUpdateTotalBytes() {
            trafficCounter.addBytesSent(500)
            assertThat(trafficCounter.totalBytes).isEqualTo(500L)
        }

        @Test
        @DisplayName("should not affect bytes received")
        fun shouldNotAffectBytesReceived() {
            trafficCounter.addBytesSent(100)
            assertThat(trafficCounter.bytesReceived).isEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("Adding bytes received")
    inner class AddingBytesReceived {
        @Test
        @DisplayName("should increment bytes received")
        fun shouldIncrementBytesReceived() {
            trafficCounter.addBytesReceived(100)
            assertThat(trafficCounter.bytesReceived).isEqualTo(100L)
        }

        @Test
        @DisplayName("should accumulate multiple additions")
        fun shouldAccumulateMultipleAdditions() {
            trafficCounter.addBytesReceived(100)
            trafficCounter.addBytesReceived(200)
            trafficCounter.addBytesReceived(300)
            assertThat(trafficCounter.bytesReceived).isEqualTo(600L)
        }

        @Test
        @DisplayName("should update total bytes")
        fun shouldUpdateTotalBytes() {
            trafficCounter.addBytesReceived(500)
            assertThat(trafficCounter.totalBytes).isEqualTo(500L)
        }

        @Test
        @DisplayName("should not affect bytes sent")
        fun shouldNotAffectBytesSent() {
            trafficCounter.addBytesReceived(100)
            assertThat(trafficCounter.bytesSent).isEqualTo(0L)
        }
    }

    @Nested
    @DisplayName("Total bytes calculation")
    inner class TotalBytesCalculation {
        @Test
        @DisplayName("should sum sent and received bytes")
        fun shouldSumSentAndReceivedBytes() {
            trafficCounter.addBytesSent(300)
            trafficCounter.addBytesReceived(700)
            assertThat(trafficCounter.totalBytes).isEqualTo(1000L)
        }

        @Test
        @DisplayName("should handle large values")
        fun shouldHandleLargeValues() {
            val oneTB = 1024L * 1024L * 1024L * 1024L
            trafficCounter.addBytesSent(oneTB)
            trafficCounter.addBytesReceived(oneTB)
            assertThat(trafficCounter.totalBytes).isEqualTo(2 * oneTB)
        }
    }

    @Nested
    @DisplayName("Flow observation")
    inner class FlowObservation {
        @Test
        @DisplayName("should emit initial snapshot")
        fun shouldEmitInitialSnapshot() =
            runTest {
                val snapshot = trafficCounter.trafficFlow.first()
                assertThat(snapshot.bytesSent).isEqualTo(0L)
                assertThat(snapshot.bytesReceived).isEqualTo(0L)
            }

        @Test
        @DisplayName("should emit updated snapshot after adding bytes sent")
        fun shouldEmitUpdatedSnapshotAfterAddingBytesSent() =
            runTest {
                trafficCounter.addBytesSent(100)
                val snapshot = trafficCounter.trafficFlow.first()
                assertThat(snapshot.bytesSent).isEqualTo(100L)
            }

        @Test
        @DisplayName("should emit updated snapshot after adding bytes received")
        fun shouldEmitUpdatedSnapshotAfterAddingBytesReceived() =
            runTest {
                trafficCounter.addBytesReceived(200)
                val snapshot = trafficCounter.trafficFlow.first()
                assertThat(snapshot.bytesReceived).isEqualTo(200L)
            }
    }

    @Nested
    @DisplayName("Reset functionality")
    inner class ResetFunctionality {
        @Test
        @DisplayName("should reset bytes sent to zero")
        fun shouldResetBytesSentToZero() {
            trafficCounter.addBytesSent(1000)
            trafficCounter.reset()
            assertThat(trafficCounter.bytesSent).isEqualTo(0L)
        }

        @Test
        @DisplayName("should reset bytes received to zero")
        fun shouldResetBytesReceivedToZero() {
            trafficCounter.addBytesReceived(1000)
            trafficCounter.reset()
            assertThat(trafficCounter.bytesReceived).isEqualTo(0L)
        }

        @Test
        @DisplayName("should emit zero snapshot after reset")
        fun shouldEmitZeroSnapshotAfterReset() =
            runTest {
                trafficCounter.addBytesSent(500)
                trafficCounter.addBytesReceived(500)
                trafficCounter.reset()
                val snapshot = trafficCounter.trafficFlow.first()
                assertThat(snapshot.bytesSent).isEqualTo(0L)
                assertThat(snapshot.bytesReceived).isEqualTo(0L)
            }
    }

    @Nested
    @DisplayName("Snapshot creation")
    inner class SnapshotCreation {
        @Test
        @DisplayName("should create snapshot with current values")
        fun shouldCreateSnapshotWithCurrentValues() {
            trafficCounter.addBytesSent(123)
            trafficCounter.addBytesReceived(456)
            val snapshot = trafficCounter.getSnapshot()
            assertThat(snapshot.bytesSent).isEqualTo(123L)
            assertThat(snapshot.bytesReceived).isEqualTo(456L)
        }

        @Test
        @DisplayName("snapshot should include total bytes")
        fun snapshotShouldIncludeTotalBytes() {
            trafficCounter.addBytesSent(100)
            trafficCounter.addBytesReceived(200)
            val snapshot = trafficCounter.getSnapshot()
            assertThat(snapshot.totalBytes).isEqualTo(300L)
        }

        @Test
        @DisplayName("snapshot should include timestamp")
        fun snapshotShouldIncludeTimestamp() {
            val before = System.currentTimeMillis()
            val snapshot = trafficCounter.getSnapshot()
            val after = System.currentTimeMillis()
            assertThat(snapshot.timestamp).isAtLeast(before)
            assertThat(snapshot.timestamp).isAtMost(after)
        }
    }

    @Nested
    @DisplayName("Speed calculation")
    inner class SpeedCalculation {
        @Test
        @DisplayName("should calculate upload speed between snapshots")
        fun shouldCalculateUploadSpeedBetweenSnapshots() {
            val snapshot1 =
                TrafficSnapshot(
                    bytesSent = 0,
                    bytesReceived = 0,
                    timestamp = 0,
                )
            val snapshot2 =
                TrafficSnapshot(
                    bytesSent = 1000,
                    bytesReceived = 0,
                    timestamp = 1000,
                )
            // 1000 bytes in 1 second = 1000 bytes/sec
            val speed = TrafficCounter.calculateUploadSpeed(snapshot1, snapshot2)
            assertThat(speed).isEqualTo(1000.0)
        }

        @Test
        @DisplayName("should calculate download speed between snapshots")
        fun shouldCalculateDownloadSpeedBetweenSnapshots() {
            val snapshot1 =
                TrafficSnapshot(
                    bytesSent = 0,
                    bytesReceived = 0,
                    timestamp = 0,
                )
            val snapshot2 =
                TrafficSnapshot(
                    bytesSent = 0,
                    bytesReceived = 2000,
                    timestamp = 1000,
                )
            // 2000 bytes in 1 second = 2000 bytes/sec
            val speed = TrafficCounter.calculateDownloadSpeed(snapshot1, snapshot2)
            assertThat(speed).isEqualTo(2000.0)
        }

        @Test
        @DisplayName("should return zero speed for same timestamp")
        fun shouldReturnZeroSpeedForSameTimestamp() {
            val snapshot1 =
                TrafficSnapshot(
                    bytesSent = 0,
                    bytesReceived = 0,
                    timestamp = 1000,
                )
            val snapshot2 =
                TrafficSnapshot(
                    bytesSent = 1000,
                    bytesReceived = 1000,
                    timestamp = 1000,
                )
            assertThat(TrafficCounter.calculateUploadSpeed(snapshot1, snapshot2)).isEqualTo(0.0)
            assertThat(TrafficCounter.calculateDownloadSpeed(snapshot1, snapshot2)).isEqualTo(0.0)
        }
    }
}
