package kittoku.mvc.connection

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * TDD tests for ConnectionStats data class.
 *
 * These tests define the expected behavior of the ConnectionStats
 * before implementation (Red-Green-Refactor cycle).
 */
@DisplayName("ConnectionStats")
class ConnectionStatsTest {
    @Nested
    @DisplayName("Basic properties")
    inner class BasicProperties {
        @Test
        @DisplayName("should store connected timestamp")
        fun shouldStoreConnectedTimestamp() {
            val timestamp = System.currentTimeMillis()
            val stats = ConnectionStats(connectedAt = timestamp)
            assertThat(stats.connectedAt).isEqualTo(timestamp)
        }

        @Test
        @DisplayName("should store server address")
        fun shouldStoreServerAddress() {
            val stats = ConnectionStats(serverAddress = "vpn.example.com")
            assertThat(stats.serverAddress).isEqualTo("vpn.example.com")
        }

        @Test
        @DisplayName("should store assigned IP address")
        fun shouldStoreAssignedIpAddress() {
            val stats = ConnectionStats(assignedIp = "10.0.0.5")
            assertThat(stats.assignedIp).isEqualTo("10.0.0.5")
        }

        @Test
        @DisplayName("should store hub name")
        fun shouldStoreHubName() {
            val stats = ConnectionStats(hubName = "VPN_HUB")
            assertThat(stats.hubName).isEqualTo("VPN_HUB")
        }

        @Test
        @DisplayName("should store protocol type")
        fun shouldStoreProtocolType() {
            val stats = ConnectionStats(protocol = "TCP")
            assertThat(stats.protocol).isEqualTo("TCP")
        }

        @Test
        @DisplayName("should store UDP acceleration status")
        fun shouldStoreUdpAccelerationStatus() {
            val stats = ConnectionStats(isUdpAccelerated = true)
            assertThat(stats.isUdpAccelerated).isTrue()
        }

        @Test
        @DisplayName("should default UDP acceleration to false")
        fun shouldDefaultUdpAccelerationToFalse() {
            val stats = ConnectionStats()
            assertThat(stats.isUdpAccelerated).isFalse()
        }
    }

    @Nested
    @DisplayName("Traffic statistics")
    inner class TrafficStatistics {
        @Test
        @DisplayName("should store bytes sent")
        fun shouldStoreBytesSent() {
            val stats = ConnectionStats(bytesSent = 1024L)
            assertThat(stats.bytesSent).isEqualTo(1024L)
        }

        @Test
        @DisplayName("should store bytes received")
        fun shouldStoreBytesReceived() {
            val stats = ConnectionStats(bytesReceived = 2048L)
            assertThat(stats.bytesReceived).isEqualTo(2048L)
        }

        @Test
        @DisplayName("should default bytes sent to 0")
        fun shouldDefaultBytesSentToZero() {
            val stats = ConnectionStats()
            assertThat(stats.bytesSent).isEqualTo(0L)
        }

        @Test
        @DisplayName("should default bytes received to 0")
        fun shouldDefaultBytesReceivedToZero() {
            val stats = ConnectionStats()
            assertThat(stats.bytesReceived).isEqualTo(0L)
        }

        @Test
        @DisplayName("should calculate total bytes")
        fun shouldCalculateTotalBytes() {
            val stats = ConnectionStats(bytesSent = 1000L, bytesReceived = 2000L)
            assertThat(stats.totalBytes).isEqualTo(3000L)
        }
    }

    @Nested
    @DisplayName("Duration calculation")
    inner class DurationCalculation {
        @Test
        @DisplayName("should calculate duration from connected timestamp")
        fun shouldCalculateDurationFromConnectedTimestamp() {
            val connectedAt = System.currentTimeMillis() - 60_000 // 1 minute ago
            val stats = ConnectionStats(connectedAt = connectedAt)
            val duration = stats.getDuration()
            // Allow some tolerance for test execution time
            assertThat(duration.toMillis()).isAtLeast(59_000)
            assertThat(duration.toMillis()).isAtMost(61_000)
        }

        @Test
        @DisplayName("should format duration as HH:MM:SS")
        fun shouldFormatDurationAsHhMmSs() {
            // 1 hour, 30 minutes, 45 seconds ago
            val connectedAt = System.currentTimeMillis() - (1 * 3600 + 30 * 60 + 45) * 1000
            val stats = ConnectionStats(connectedAt = connectedAt)
            val formatted = stats.getFormattedDuration()
            assertThat(formatted).matches("01:30:4[45]")
        }

        @Test
        @DisplayName("should format short duration correctly")
        fun shouldFormatShortDurationCorrectly() {
            val connectedAt = System.currentTimeMillis() - 5_000 // 5 seconds ago
            val stats = ConnectionStats(connectedAt = connectedAt)
            val formatted = stats.getFormattedDuration()
            assertThat(formatted).matches("00:00:0[45]")
        }
    }

    @Nested
    @DisplayName("Formatted traffic")
    inner class FormattedTraffic {
        @Test
        @DisplayName("should format bytes as B for small values")
        fun shouldFormatBytesAsBForSmallValues() {
            val stats = ConnectionStats(bytesSent = 500L)
            assertThat(stats.getFormattedBytesSent()).isEqualTo("500 B")
        }

        @Test
        @DisplayName("should format bytes as KB for kilobyte values")
        fun shouldFormatBytesAsKbForKilobyteValues() {
            val stats = ConnectionStats(bytesSent = 1536L) // 1.5 KB
            assertThat(stats.getFormattedBytesSent()).isEqualTo("1.5 KB")
        }

        @Test
        @DisplayName("should format bytes as MB for megabyte values")
        fun shouldFormatBytesAsMbForMegabyteValues() {
            val stats = ConnectionStats(bytesReceived = 1_572_864L) // 1.5 MB
            assertThat(stats.getFormattedBytesReceived()).isEqualTo("1.5 MB")
        }

        @Test
        @DisplayName("should format bytes as GB for gigabyte values")
        fun shouldFormatBytesAsGbForGigabyteValues() {
            val stats = ConnectionStats(bytesSent = 1_610_612_736L) // 1.5 GB
            assertThat(stats.getFormattedBytesSent()).isEqualTo("1.5 GB")
        }
    }

    @Nested
    @DisplayName("Copy with updates")
    inner class CopyWithUpdates {
        @Test
        @DisplayName("should create copy with updated bytes sent")
        fun shouldCreateCopyWithUpdatedBytesSent() {
            val original = ConnectionStats(bytesSent = 100L)
            val updated = original.copy(bytesSent = 200L)
            assertThat(updated.bytesSent).isEqualTo(200L)
            assertThat(original.bytesSent).isEqualTo(100L)
        }

        @Test
        @DisplayName("should create copy with updated bytes received")
        fun shouldCreateCopyWithUpdatedBytesReceived() {
            val original = ConnectionStats(bytesReceived = 100L)
            val updated = original.copy(bytesReceived = 300L)
            assertThat(updated.bytesReceived).isEqualTo(300L)
        }

        @Test
        @DisplayName("should preserve other fields when copying")
        fun shouldPreserveOtherFieldsWhenCopying() {
            val original =
                ConnectionStats(
                    connectedAt = 1000L,
                    serverAddress = "vpn.example.com",
                    assignedIp = "10.0.0.5",
                    bytesSent = 100L,
                )
            val updated = original.copy(bytesSent = 200L)
            assertThat(updated.connectedAt).isEqualTo(1000L)
            assertThat(updated.serverAddress).isEqualTo("vpn.example.com")
            assertThat(updated.assignedIp).isEqualTo("10.0.0.5")
        }
    }

    @Nested
    @DisplayName("Equality")
    inner class Equality {
        @Test
        @DisplayName("should be equal when all fields match")
        fun shouldBeEqualWhenAllFieldsMatch() {
            val stats1 =
                ConnectionStats(
                    connectedAt = 1000L,
                    serverAddress = "vpn.example.com",
                    assignedIp = "10.0.0.5",
                )
            val stats2 =
                ConnectionStats(
                    connectedAt = 1000L,
                    serverAddress = "vpn.example.com",
                    assignedIp = "10.0.0.5",
                )
            assertThat(stats1).isEqualTo(stats2)
        }

        @Test
        @DisplayName("should not be equal when fields differ")
        fun shouldNotBeEqualWhenFieldsDiffer() {
            val stats1 = ConnectionStats(serverAddress = "vpn1.example.com")
            val stats2 = ConnectionStats(serverAddress = "vpn2.example.com")
            assertThat(stats1).isNotEqualTo(stats2)
        }
    }
}
