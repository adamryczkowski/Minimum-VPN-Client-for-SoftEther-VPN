package kittoku.mvc.statistics

import kittoku.mvc.extension.Formatting
import java.util.Locale

/**
 * Aggregate statistics across all connection sessions.
 *
 * @property totalBytesSent Total bytes sent across all sessions
 * @property totalBytesReceived Total bytes received across all sessions
 * @property totalDurationMillis Total connection duration in milliseconds
 * @property sessionCount Total number of sessions
 * @property successfulSessionCount Number of successful sessions
 */
data class AggregateStatistics(
    val totalBytesSent: Long,
    val totalBytesReceived: Long,
    val totalDurationMillis: Long,
    val sessionCount: Int,
    val successfulSessionCount: Int,
) {
    /**
     * Total bytes transferred (sent + received).
     */
    val totalBytes: Long
        get() = totalBytesSent + totalBytesReceived

    /**
     * Success rate as a value between 0.0 and 1.0.
     */
    val successRate: Double
        get() = if (sessionCount > 0) successfulSessionCount.toDouble() / sessionCount else 0.0

    /**
     * Total duration in seconds.
     */
    val totalDurationSeconds: Long
        get() = totalDurationMillis / 1000

    /**
     * Total duration in hours.
     */
    val totalDurationHours: Double
        get() = totalDurationMillis / 3600_000.0

    /**
     * Average session duration in milliseconds.
     */
    val averageSessionDurationMillis: Long
        get() = if (sessionCount > 0) totalDurationMillis / sessionCount else 0L

    /**
     * Get formatted total bytes sent.
     */
    fun getFormattedBytesSent(): String = Formatting.formatBytes(totalBytesSent)

    /**
     * Get formatted total bytes received.
     */
    fun getFormattedBytesReceived(): String = Formatting.formatBytes(totalBytesReceived)

    /**
     * Get formatted total bytes.
     */
    fun getFormattedTotalBytes(): String = Formatting.formatBytes(totalBytes)

    /**
     * Get formatted total duration as HH:MM:SS.
     */
    fun getFormattedDuration(): String = Formatting.formatDuration(totalDurationMillis)

    /**
     * Get formatted success rate as percentage.
     */
    fun getFormattedSuccessRate(): String {
        return String.format(Locale.US, "%.1f%%", successRate * 100)
    }

    companion object {
        /**
         * Empty statistics with all values at zero.
         */
        val EMPTY =
            AggregateStatistics(
                totalBytesSent = 0L,
                totalBytesReceived = 0L,
                totalDurationMillis = 0L,
                sessionCount = 0,
                successfulSessionCount = 0,
            )
    }
}
