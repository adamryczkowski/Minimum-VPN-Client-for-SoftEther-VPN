package kittoku.mvc.performance

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [PerformanceMonitor].
 */
@DisplayName("PerformanceMonitor")
class PerformanceMonitorTest {
    private lateinit var monitor: PerformanceMonitor

    @BeforeEach
    fun setUp() {
        monitor = PerformanceMonitor()
        monitor.start()
    }

    @Nested
    @DisplayName("Packet tracking")
    inner class PacketTracking {
        @Test
        @DisplayName("recordPacketSent increments counter")
        fun recordPacketSentIncrementsCounter() {
            monitor.recordPacketSent(100)
            monitor.recordPacketSent(200)
            monitor.recordPacketSent(300)

            val metrics = monitor.getMetrics()
            assertThat(metrics.packetsSent).isEqualTo(3)
        }

        @Test
        @DisplayName("recordPacketSent accumulates bytes")
        fun recordPacketSentAccumulatesBytes() {
            monitor.recordPacketSent(100)
            monitor.recordPacketSent(200)
            monitor.recordPacketSent(300)

            val metrics = monitor.getMetrics()
            assertThat(metrics.bytesSent).isEqualTo(600)
        }

        @Test
        @DisplayName("recordPacketReceived increments counter")
        fun recordPacketReceivedIncrementsCounter() {
            monitor.recordPacketReceived(100)
            monitor.recordPacketReceived(200)

            val metrics = monitor.getMetrics()
            assertThat(metrics.packetsReceived).isEqualTo(2)
        }

        @Test
        @DisplayName("recordPacketReceived accumulates bytes")
        fun recordPacketReceivedAccumulatesBytes() {
            monitor.recordPacketReceived(100)
            monitor.recordPacketReceived(200)

            val metrics = monitor.getMetrics()
            assertThat(metrics.bytesReceived).isEqualTo(300)
        }

        @Test
        @DisplayName("recordPacketDropped increments counter")
        fun recordPacketDroppedIncrementsCounter() {
            monitor.recordPacketDropped()
            monitor.recordPacketDropped()

            val metrics = monitor.getMetrics()
            assertThat(metrics.packetsDropped).isEqualTo(2)
        }

        @Test
        @DisplayName("recordError increments counter")
        fun recordErrorIncrementsCounter() {
            monitor.recordError()
            monitor.recordError()
            monitor.recordError()

            val metrics = monitor.getMetrics()
            assertThat(metrics.errorsCount).isEqualTo(3)
        }
    }

    @Nested
    @DisplayName("Latency tracking")
    inner class LatencyTracking {
        @Test
        @DisplayName("recordLatency stores samples")
        fun recordLatencyStoresSamples() {
            monitor.recordLatency(10)
            monitor.recordLatency(20)
            monitor.recordLatency(30)

            val metrics = monitor.getMetrics()
            assertThat(metrics.averageLatencyMs).isWithin(0.1).of(20.0)
        }

        @Test
        @DisplayName("calculates min latency correctly")
        fun calculatesMinLatencyCorrectly() {
            monitor.recordLatency(50)
            monitor.recordLatency(10)
            monitor.recordLatency(30)

            val metrics = monitor.getMetrics()
            assertThat(metrics.minLatencyMs).isEqualTo(10)
        }

        @Test
        @DisplayName("calculates max latency correctly")
        fun calculatesMaxLatencyCorrectly() {
            monitor.recordLatency(50)
            monitor.recordLatency(10)
            monitor.recordLatency(30)

            val metrics = monitor.getMetrics()
            assertThat(metrics.maxLatencyMs).isEqualTo(50)
        }
    }

    @Nested
    @DisplayName("Metrics calculations")
    inner class MetricsCalculations {
        @Test
        @DisplayName("totalPackets sums sent and received")
        fun totalPacketsSumsSentAndReceived() {
            monitor.recordPacketSent(100)
            monitor.recordPacketSent(100)
            monitor.recordPacketReceived(100)

            val metrics = monitor.getMetrics()
            assertThat(metrics.totalPackets).isEqualTo(3)
        }

        @Test
        @DisplayName("totalBytes sums sent and received")
        fun totalBytesSumsSentAndReceived() {
            monitor.recordPacketSent(100)
            monitor.recordPacketReceived(200)

            val metrics = monitor.getMetrics()
            assertThat(metrics.totalBytes).isEqualTo(300)
        }

        @Test
        @DisplayName("packetLossRate calculates correctly")
        fun packetLossRateCalculatesCorrectly() {
            // 10 packets total, 2 dropped = 2/12 = 16.67% loss
            repeat(10) { monitor.recordPacketSent(100) }
            monitor.recordPacketDropped()
            monitor.recordPacketDropped()

            val metrics = monitor.getMetrics()
            assertThat(metrics.packetLossRate).isWithin(0.01).of(2.0 / 12.0)
        }

        @Test
        @DisplayName("packetLossRate is zero when no packets")
        fun packetLossRateIsZeroWhenNoPackets() {
            val metrics = monitor.getMetrics()
            assertThat(metrics.packetLossRate).isEqualTo(0.0)
        }
    }

    @Nested
    @DisplayName("Formatting")
    inner class Formatting {
        @Test
        @DisplayName("formats bytes in KB")
        fun formatsBytesInKB() {
            monitor.recordPacketSent(1500)

            val metrics = monitor.getMetrics()
            assertThat(metrics.bytesSentFormatted).contains("KB")
        }

        @Test
        @DisplayName("formats bytes in MB")
        fun formatsBytesInMB() {
            // Send 2MB
            repeat(2000) { monitor.recordPacketSent(1000) }

            val metrics = monitor.getMetrics()
            assertThat(metrics.bytesSentFormatted).contains("MB")
        }

        @Test
        @DisplayName("formats uptime correctly")
        fun formatsUptimeCorrectly() {
            // Just check format is HH:MM:SS
            val metrics = monitor.getMetrics()
            assertThat(metrics.uptimeFormatted).matches("\\d{2}:\\d{2}:\\d{2}")
        }
    }

    @Nested
    @DisplayName("reset()")
    inner class Reset {
        @Test
        @DisplayName("resets all counters")
        fun resetsAllCounters() {
            monitor.recordPacketSent(100)
            monitor.recordPacketReceived(100)
            monitor.recordPacketDropped()
            monitor.recordError()
            monitor.recordLatency(50)

            monitor.reset()

            val metrics = monitor.getMetrics()
            assertThat(metrics.packetsSent).isEqualTo(0)
            assertThat(metrics.packetsReceived).isEqualTo(0)
            assertThat(metrics.bytesSent).isEqualTo(0)
            assertThat(metrics.bytesReceived).isEqualTo(0)
            assertThat(metrics.packetsDropped).isEqualTo(0)
            assertThat(metrics.errorsCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("Singleton instance")
    inner class SingletonInstance {
        @Test
        @DisplayName("getInstance returns same instance")
        fun getInstanceReturnsSameInstance() {
            val instance1 = PerformanceMonitor.getInstance()
            val instance2 = PerformanceMonitor.getInstance()

            assertThat(instance1).isSameInstanceAs(instance2)
        }
    }

    @Nested
    @DisplayName("Metrics toString")
    inner class MetricsToString {
        @Test
        @DisplayName("toString contains all metrics")
        fun toStringContainsAllMetrics() {
            monitor.recordPacketSent(1000)
            monitor.recordPacketReceived(2000)
            monitor.recordLatency(25)

            val metrics = monitor.getMetrics()
            val string = metrics.toString()

            assertThat(string).contains("Packets sent:")
            assertThat(string).contains("Packets received:")
            assertThat(string).contains("Bytes sent:")
            assertThat(string).contains("Bytes received:")
            assertThat(string).contains("latency")
        }
    }
}
