package kittoku.mvc.statistics

/**
 * Immutable snapshot of traffic statistics at a point in time.
 *
 * Used for calculating speeds and storing traffic history.
 *
 * @property bytesSent Total bytes sent at snapshot time
 * @property bytesReceived Total bytes received at snapshot time
 * @property timestamp Timestamp when snapshot was taken (milliseconds since epoch)
 */
data class TrafficSnapshot(
    val bytesSent: Long,
    val bytesReceived: Long,
    val timestamp: Long = System.currentTimeMillis(),
) {
    /**
     * Total bytes transferred (sent + received).
     */
    val totalBytes: Long
        get() = bytesSent + bytesReceived
}
