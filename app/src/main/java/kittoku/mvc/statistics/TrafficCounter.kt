package kittoku.mvc.statistics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicLong

/**
 * Thread-safe counter for tracking VPN traffic statistics.
 *
 * Tracks bytes sent and received through the VPN tunnel in real-time,
 * providing both current values and a Flow for observation.
 */
class TrafficCounter {
    private val _bytesSent = AtomicLong(0L)
    private val _bytesReceived = AtomicLong(0L)

    private val _trafficFlow = MutableStateFlow(TrafficSnapshot(0L, 0L))

    /**
     * Total bytes sent through the VPN tunnel.
     */
    val bytesSent: Long
        get() = _bytesSent.get()

    /**
     * Total bytes received through the VPN tunnel.
     */
    val bytesReceived: Long
        get() = _bytesReceived.get()

    /**
     * Total bytes transferred (sent + received).
     */
    val totalBytes: Long
        get() = bytesSent + bytesReceived

    /**
     * Flow of traffic snapshots for observation.
     * Emits a new snapshot whenever traffic is updated.
     */
    val trafficFlow: StateFlow<TrafficSnapshot>
        get() = _trafficFlow.asStateFlow()

    /**
     * Add bytes to the sent counter.
     *
     * @param bytes Number of bytes sent
     */
    fun addBytesSent(bytes: Long) {
        _bytesSent.addAndGet(bytes)
        emitSnapshot()
    }

    /**
     * Add bytes to the received counter.
     *
     * @param bytes Number of bytes received
     */
    fun addBytesReceived(bytes: Long) {
        _bytesReceived.addAndGet(bytes)
        emitSnapshot()
    }

    /**
     * Reset all counters to zero.
     */
    fun reset() {
        _bytesSent.set(0L)
        _bytesReceived.set(0L)
        emitSnapshot()
    }

    /**
     * Get a snapshot of current traffic statistics.
     *
     * @return TrafficSnapshot with current values and timestamp
     */
    fun getSnapshot(): TrafficSnapshot {
        return TrafficSnapshot(
            bytesSent = bytesSent,
            bytesReceived = bytesReceived,
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun emitSnapshot() {
        _trafficFlow.value = getSnapshot()
    }

    companion object {
        /**
         * Calculate upload speed in bytes per second between two snapshots.
         *
         * @param previous Previous traffic snapshot
         * @param current Current traffic snapshot
         * @return Upload speed in bytes per second, or 0.0 if timestamps are equal
         */
        fun calculateUploadSpeed(
            previous: TrafficSnapshot,
            current: TrafficSnapshot,
        ): Double {
            val timeDiff = current.timestamp - previous.timestamp
            if (timeDiff <= 0) return 0.0

            val bytesDiff = current.bytesSent - previous.bytesSent
            return bytesDiff.toDouble() / timeDiff * 1000.0
        }

        /**
         * Calculate download speed in bytes per second between two snapshots.
         *
         * @param previous Previous traffic snapshot
         * @param current Current traffic snapshot
         * @return Download speed in bytes per second, or 0.0 if timestamps are equal
         */
        fun calculateDownloadSpeed(
            previous: TrafficSnapshot,
            current: TrafficSnapshot,
        ): Double {
            val timeDiff = current.timestamp - previous.timestamp
            if (timeDiff <= 0) return 0.0

            val bytesDiff = current.bytesReceived - previous.bytesReceived
            return bytesDiff.toDouble() / timeDiff * 1000.0
        }
    }
}
