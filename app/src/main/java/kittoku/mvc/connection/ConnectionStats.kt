package kittoku.mvc.connection

import kittoku.mvc.extension.Formatting
import java.time.Duration

/**
 * Data class representing connection statistics.
 *
 * This class holds all the statistics related to an active VPN connection,
 * including traffic data, timing information, and connection details.
 *
 * This is the canonical ConnectionStats class - all other duplicates should
 * import from this package (kittoku.mvc.connection).
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
     * Alias for [assignedIp] for compatibility with code expecting this property name.
     * Prefer using [assignedIp] for new code.
     */
    val assignedIpAddress: String?
        get() = assignedIp.ifEmpty { null }

    /**
     * Alias for [serverAddress] for compatibility with code expecting this property name.
     * Prefer using [serverAddress] for new code.
     */
    val serverHostname: String?
        get() = serverAddress.ifEmpty { null }

    /**
     * Connection duration in milliseconds.
     * Computed from [connectedAt] timestamp.
     */
    val durationMs: Long
        get() = System.currentTimeMillis() - connectedAt

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
    fun getFormattedDuration(): String = Formatting.formatDuration(durationMs)

    /**
     * Get formatted bytes sent string.
     *
     * @return Human-readable bytes sent (e.g., "1.5 MB")
     */
    fun getFormattedBytesSent(): String = Formatting.formatBytes(bytesSent)

    /**
     * Get formatted bytes received string.
     *
     * @return Human-readable bytes received (e.g., "2.3 GB")
     */
    fun getFormattedBytesReceived(): String = Formatting.formatBytes(bytesReceived)

    /**
     * Get formatted total bytes string.
     *
     * @return Human-readable total bytes (e.g., "3.8 GB")
     */
    fun getFormattedTotalBytes(): String = Formatting.formatBytes(totalBytes)
}
