package kittoku.mvc.statistics

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
    fun getFormattedBytesSent(): String = formatBytes(totalBytesSent)

    /**
     * Get formatted total bytes received.
     */
    fun getFormattedBytesReceived(): String = formatBytes(totalBytesReceived)

    /**
     * Get formatted total bytes.
     */
    fun getFormattedTotalBytes(): String = formatBytes(totalBytes)

    /**
     * Get formatted total duration as HH:MM:SS.
     */
    fun getFormattedDuration(): String {
        val totalSeconds = totalDurationSeconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Get formatted success rate as percentage.
     */
    fun getFormattedSuccessRate(): String {
        return String.format(Locale.US, "%.1f%%", successRate * 100)
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
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
