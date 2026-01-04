package kittoku.mvc.connection

import java.time.Duration
import java.util.Locale

/**
 * Data class representing connection statistics.
 *
 * This class holds all the statistics related to an active VPN connection,
 * including traffic data, timing information, and connection details.
 */
data class ConnectionStats(
    /** Timestamp when the connection was established (milliseconds since epoch) */
    val connectedAt: Long = System.currentTimeMillis(),
    /** Server hostname or IP address */
    val serverAddress: String = "",
    /** IP address assigned to the client by the VPN server */
    val assignedIp: String = "",
    /** Name of the VPN hub connected to */
    val hubName: String = "",
    /** Protocol type (TCP or UDP) */
    val protocol: String = "TCP",
    /** Whether UDP acceleration is active */
    val isUdpAccelerated: Boolean = false,
    /** Total bytes sent through the VPN tunnel */
    val bytesSent: Long = 0L,
    /** Total bytes received through the VPN tunnel */
    val bytesReceived: Long = 0L,
) {
    /** Total bytes transferred (sent + received) */
    val totalBytes: Long
        get() = bytesSent + bytesReceived

    /**
     * Calculate the duration of the connection.
     *
     * @return Duration since connection was established
     */
    fun getDuration(): Duration {
        val now = System.currentTimeMillis()
        return Duration.ofMillis(now - connectedAt)
    }

    /**
     * Get the connection duration formatted as HH:MM:SS.
     *
     * @return Formatted duration string
     */
    fun getFormattedDuration(): String {
        val duration = getDuration()
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    /**
     * Get formatted bytes sent string.
     *
     * @return Human-readable bytes sent (e.g., "1.5 MB")
     */
    fun getFormattedBytesSent(): String = formatBytes(bytesSent)

    /**
     * Get formatted bytes received string.
     *
     * @return Human-readable bytes received (e.g., "2.3 GB")
     */
    fun getFormattedBytesReceived(): String = formatBytes(bytesReceived)

    /**
     * Get formatted total bytes string.
     *
     * @return Human-readable total bytes (e.g., "3.8 GB")
     */
    fun getFormattedTotalBytes(): String = formatBytes(totalBytes)

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
