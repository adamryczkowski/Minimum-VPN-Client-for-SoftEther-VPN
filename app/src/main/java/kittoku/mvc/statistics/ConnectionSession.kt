package kittoku.mvc.statistics

import androidx.room.Entity
import androidx.room.PrimaryKey
import kittoku.mvc.extension.Formatting
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/**
 * Entity representing a completed VPN connection session.
 *
 * Stored in the database for connection history and statistics.
 *
 * @property id Unique identifier (auto-generated)
 * @property profileId ID of the VPN profile used for this connection
 * @property serverAddress Server hostname or IP address
 * @property startTime Connection start timestamp (milliseconds since epoch)
 * @property endTime Connection end timestamp (milliseconds since epoch)
 * @property bytesSent Total bytes sent during the session
 * @property bytesReceived Total bytes received during the session
 * @property wasSuccessful Whether the connection was successful
 * @property errorMessage Error message if connection failed
 * @property disconnectReason Reason for disconnection
 * @property hubName VPN hub name
 * @property protocol Protocol used (TCP/UDP)
 * @property assignedIp IP address assigned by the VPN server
 */
@Entity(tableName = "connection_sessions")
data class ConnectionSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val profileId: Long,
    val serverAddress: String,
    val startTime: Long,
    val endTime: Long,
    val bytesSent: Long = 0L,
    val bytesReceived: Long = 0L,
    val wasSuccessful: Boolean = true,
    val errorMessage: String? = null,
    val disconnectReason: DisconnectReason = DisconnectReason.UNKNOWN,
    val hubName: String = "",
    val protocol: String = "TCP",
    val assignedIp: String = "",
) {
    /**
     * Duration of the connection in milliseconds.
     */
    val durationMillis: Long
        get() = endTime - startTime

    /**
     * Duration of the connection in seconds.
     */
    val durationSeconds: Long
        get() = durationMillis / 1000

    /**
     * Total bytes transferred (sent + received).
     */
    val totalBytes: Long
        get() = bytesSent + bytesReceived

    /**
     * Get the duration formatted as HH:MM:SS.
     *
     * @return Formatted duration string
     */
    fun getFormattedDuration(): String = Formatting.formatDuration(durationMillis)

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

    /**
     * Get the start date formatted for display.
     *
     * @param locale Locale for formatting
     * @return Formatted date string
     */
    fun getFormattedStartDate(locale: Locale = Locale.getDefault()): String {
        val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
        return dateFormat.format(Date(startTime))
    }

    /**
     * Get the start time formatted for display.
     *
     * @param locale Locale for formatting
     * @return Formatted time string
     */
    fun getFormattedStartTime(locale: Locale = Locale.getDefault()): String {
        val timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT, locale)
        return timeFormat.format(Date(startTime))
    }

    /**
     * Get the start date and time formatted for display.
     *
     * @param locale Locale for formatting
     * @return Formatted date and time string
     */
    fun getFormattedStartDateTime(locale: Locale = Locale.getDefault()): String {
        val dateTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        return dateTimeFormat.format(Date(startTime))
    }

    companion object {
        /**
         * Create a ConnectionSession from current connection state.
         *
         * @param profileId VPN profile ID
         * @param serverAddress Server address
         * @param startTime Connection start time
         * @param trafficSnapshot Current traffic snapshot
         * @param hubName Hub name
         * @param protocol Protocol type
         * @param assignedIp Assigned IP address
         * @param wasSuccessful Whether connection was successful
         * @param errorMessage Error message if failed
         * @param disconnectReason Reason for disconnection
         * @return New ConnectionSession instance
         */
        fun create(
            profileId: Long,
            serverAddress: String,
            startTime: Long,
            trafficSnapshot: TrafficSnapshot,
            hubName: String = "",
            protocol: String = "TCP",
            assignedIp: String = "",
            wasSuccessful: Boolean = true,
            errorMessage: String? = null,
            disconnectReason: DisconnectReason = DisconnectReason.UNKNOWN,
        ): ConnectionSession {
            return ConnectionSession(
                profileId = profileId,
                serverAddress = serverAddress,
                startTime = startTime,
                endTime = System.currentTimeMillis(),
                bytesSent = trafficSnapshot.bytesSent,
                bytesReceived = trafficSnapshot.bytesReceived,
                wasSuccessful = wasSuccessful,
                errorMessage = errorMessage,
                disconnectReason = disconnectReason,
                hubName = hubName,
                protocol = protocol,
                assignedIp = assignedIp,
            )
        }
    }
}
