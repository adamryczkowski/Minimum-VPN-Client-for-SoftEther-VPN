package kittoku.mvc.performance

import kittoku.mvc.extension.Formatting
import java.util.concurrent.atomic.AtomicLong

/**
 * Performance monitoring for VPN connection metrics.
 *
 * Tracks throughput, latency, and resource usage to help identify
 * performance bottlenecks and optimize the VPN connection.
 *
 * Usage:
 * ```kotlin
 * val monitor = PerformanceMonitor()
 * monitor.start()
 *
 * // Track packet processing
 * monitor.recordPacketSent(packetSize)
 * monitor.recordPacketReceived(packetSize)
 *
 * // Get metrics
 * val metrics = monitor.getMetrics()
 * println("Throughput: ${metrics.currentThroughputMbps} Mbps")
 * ```
 */
class PerformanceMonitor {
    private var startTimeMs: Long = 0
    private var lastSampleTimeMs: Long = 0

    // Packet counters
    private val packetsSent = AtomicLong(0)
    private val packetsReceived = AtomicLong(0)
    private val bytesSent = AtomicLong(0)
    private val bytesReceived = AtomicLong(0)

    // Error counters
    private val packetsDropped = AtomicLong(0)
    private val errorsCount = AtomicLong(0)

    // Latency tracking
    private val latencySamples = mutableListOf<Long>()
    private val maxLatencySamples = 100

    // Throughput sampling
    private var lastBytesSent: Long = 0
    private var lastBytesReceived: Long = 0
    private var currentSendThroughput: Double = 0.0
    private var currentReceiveThroughput: Double = 0.0

    /**
     * Start monitoring.
     */
    fun start() {
        startTimeMs = System.currentTimeMillis()
        lastSampleTimeMs = startTimeMs
        reset()
    }

    /**
     * Reset all counters.
     */
    fun reset() {
        packetsSent.set(0)
        packetsReceived.set(0)
        bytesSent.set(0)
        bytesReceived.set(0)
        packetsDropped.set(0)
        errorsCount.set(0)
        latencySamples.clear()
        lastBytesSent = 0
        lastBytesReceived = 0
        currentSendThroughput = 0.0
        currentReceiveThroughput = 0.0
    }

    /**
     * Record a packet being sent.
     *
     * @param sizeBytes Size of the packet in bytes
     */
    fun recordPacketSent(sizeBytes: Int) {
        packetsSent.incrementAndGet()
        bytesSent.addAndGet(sizeBytes.toLong())
    }

    /**
     * Record a packet being received.
     *
     * @param sizeBytes Size of the packet in bytes
     */
    fun recordPacketReceived(sizeBytes: Int) {
        packetsReceived.incrementAndGet()
        bytesReceived.addAndGet(sizeBytes.toLong())
    }

    /**
     * Record a dropped packet.
     */
    fun recordPacketDropped() {
        packetsDropped.incrementAndGet()
    }

    /**
     * Record an error.
     */
    fun recordError() {
        errorsCount.incrementAndGet()
    }

    /**
     * Record a latency sample.
     *
     * @param latencyMs Round-trip latency in milliseconds
     */
    fun recordLatency(latencyMs: Long) {
        synchronized(latencySamples) {
            latencySamples.add(latencyMs)
            if (latencySamples.size > maxLatencySamples) {
                latencySamples.removeAt(0)
            }
        }
    }

    /**
     * Update throughput calculations.
     *
     * Call this periodically (e.g., every second) to update throughput metrics.
     */
    fun updateThroughput() {
        val currentTimeMs = System.currentTimeMillis()
        val elapsedMs = currentTimeMs - lastSampleTimeMs

        if (elapsedMs > 0) {
            val currentBytesSent = bytesSent.get()
            val currentBytesReceived = bytesReceived.get()

            val sentDiff = currentBytesSent - lastBytesSent
            val receivedDiff = currentBytesReceived - lastBytesReceived

            // Calculate bytes per second, then convert to Mbps
            currentSendThroughput = (sentDiff * 1000.0 / elapsedMs) * 8 / 1_000_000
            currentReceiveThroughput = (receivedDiff * 1000.0 / elapsedMs) * 8 / 1_000_000

            lastBytesSent = currentBytesSent
            lastBytesReceived = currentBytesReceived
            lastSampleTimeMs = currentTimeMs
        }
    }

    /**
     * Get current performance metrics.
     */
    fun getMetrics(): Metrics {
        val currentTimeMs = System.currentTimeMillis()
        val uptimeMs = currentTimeMs - startTimeMs

        val avgLatency =
            synchronized(latencySamples) {
                if (latencySamples.isNotEmpty()) {
                    latencySamples.average()
                } else {
                    0.0
                }
            }

        val minLatency =
            synchronized(latencySamples) {
                latencySamples.minOrNull() ?: 0L
            }

        val maxLatency =
            synchronized(latencySamples) {
                latencySamples.maxOrNull() ?: 0L
            }

        val totalBytes = bytesSent.get() + bytesReceived.get()
        val averageThroughputMbps =
            if (uptimeMs > 0) {
                (totalBytes * 1000.0 / uptimeMs) * 8 / 1_000_000
            } else {
                0.0
            }

        return Metrics(
            uptimeMs = uptimeMs,
            packetsSent = packetsSent.get(),
            packetsReceived = packetsReceived.get(),
            bytesSent = bytesSent.get(),
            bytesReceived = bytesReceived.get(),
            packetsDropped = packetsDropped.get(),
            errorsCount = errorsCount.get(),
            currentSendThroughputMbps = currentSendThroughput,
            currentReceiveThroughputMbps = currentReceiveThroughput,
            averageThroughputMbps = averageThroughputMbps,
            averageLatencyMs = avgLatency,
            minLatencyMs = minLatency,
            maxLatencyMs = maxLatency,
        )
    }

    /**
     * Performance metrics snapshot.
     */
    data class Metrics(
        val uptimeMs: Long,
        val packetsSent: Long,
        val packetsReceived: Long,
        val bytesSent: Long,
        val bytesReceived: Long,
        val packetsDropped: Long,
        val errorsCount: Long,
        val currentSendThroughputMbps: Double,
        val currentReceiveThroughputMbps: Double,
        val averageThroughputMbps: Double,
        val averageLatencyMs: Double,
        val minLatencyMs: Long,
        val maxLatencyMs: Long,
    ) {
        val totalPackets: Long
            get() = packetsSent + packetsReceived

        val totalBytes: Long
            get() = bytesSent + bytesReceived

        val packetLossRate: Double
            get() =
                if (totalPackets > 0) {
                    packetsDropped.toDouble() / (totalPackets + packetsDropped)
                } else {
                    0.0
                }

        val uptimeFormatted: String
            get() = Formatting.formatDuration(uptimeMs)

        val bytesSentFormatted: String
            get() = Formatting.formatBytesDecimal(bytesSent)

        val bytesReceivedFormatted: String
            get() = Formatting.formatBytesDecimal(bytesReceived)

        override fun toString(): String {
            return buildString {
                appendLine("VPN Performance Metrics:")
                appendLine("  Uptime: $uptimeFormatted")
                appendLine("  Packets sent: $packetsSent")
                appendLine("  Packets received: $packetsReceived")
                appendLine("  Bytes sent: $bytesSentFormatted")
                appendLine("  Bytes received: $bytesReceivedFormatted")
                appendLine("  Packets dropped: $packetsDropped")
                appendLine("  Errors: $errorsCount")
                appendLine("  Packet loss rate: ${String.format("%.2f", packetLossRate * 100)}%")
                appendLine("  Current send throughput: ${String.format("%.2f", currentSendThroughputMbps)} Mbps")
                appendLine("  Current receive throughput: ${String.format("%.2f", currentReceiveThroughputMbps)} Mbps")
                appendLine("  Average throughput: ${String.format("%.2f", averageThroughputMbps)} Mbps")
                appendLine("  Average latency: ${String.format("%.1f", averageLatencyMs)} ms")
                appendLine("  Min latency: $minLatencyMs ms")
                appendLine("  Max latency: $maxLatencyMs ms")
            }
        }
    }

    companion object {
        /**
         * Singleton instance for global performance monitoring.
         */
        @Volatile
        private var instance: PerformanceMonitor? = null

        fun getInstance(): PerformanceMonitor {
            return instance ?: synchronized(this) {
                instance ?: PerformanceMonitor().also { instance = it }
            }
        }
    }
}
