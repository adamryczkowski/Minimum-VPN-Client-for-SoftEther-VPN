package kittoku.mvc.statistics

import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing connection statistics.
 *
 * Provides a clean API for saving, retrieving, and aggregating
 * connection session data.
 *
 * @property dao The ConnectionSessionDao for database operations
 */
class StatisticsRepository(
    private val dao: ConnectionSessionDao,
) {
    /**
     * Save a connection session to the database.
     *
     * @param session The session to save
     * @return The ID of the saved session
     */
    suspend fun saveSession(session: ConnectionSession): Long {
        return dao.insert(session)
    }

    /**
     * Record a new connection session from current state.
     *
     * @param profileId VPN profile ID
     * @param serverAddress Server address
     * @param startTime Connection start time
     * @param trafficSnapshot Current traffic snapshot
     * @param hubName Hub name (optional)
     * @param protocol Protocol type (optional, default "TCP")
     * @param assignedIp Assigned IP address (optional)
     * @param wasSuccessful Whether connection was successful
     * @param errorMessage Error message if failed
     * @param disconnectReason Reason for disconnection
     * @return The ID of the saved session
     */
    suspend fun recordSession(
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
    ): Long {
        val session =
            ConnectionSession.create(
                profileId = profileId,
                serverAddress = serverAddress,
                startTime = startTime,
                trafficSnapshot = trafficSnapshot,
                hubName = hubName,
                protocol = protocol,
                assignedIp = assignedIp,
                wasSuccessful = wasSuccessful,
                errorMessage = errorMessage,
                disconnectReason = disconnectReason,
            )
        return dao.insert(session)
    }

    /**
     * Get all connection sessions.
     *
     * @return Flow of all sessions ordered by start time (newest first)
     */
    fun getAllSessions(): Flow<List<ConnectionSession>> {
        return dao.getAllSessions()
    }

    /**
     * Get sessions for a specific profile.
     *
     * @param profileId The profile ID to filter by
     * @return Flow of sessions for the profile
     */
    fun getSessionsForProfile(profileId: Long): Flow<List<ConnectionSession>> {
        return dao.getSessionsForProfile(profileId)
    }

    /**
     * Get the most recent sessions.
     *
     * @param limit Maximum number of sessions to return
     * @return Flow of recent sessions
     */
    fun getRecentSessions(limit: Int): Flow<List<ConnectionSession>> {
        return dao.getRecentSessions(limit)
    }

    /**
     * Get a session by ID.
     *
     * @param id The session ID
     * @return The session or null if not found
     */
    suspend fun getSessionById(id: Long): ConnectionSession? {
        return dao.getSessionById(id)
    }

    /**
     * Get sessions within a time range.
     *
     * @param startTime Start of the time range
     * @param endTime End of the time range
     * @return Flow of sessions in the range
     */
    fun getSessionsInRange(
        startTime: Long,
        endTime: Long,
    ): Flow<List<ConnectionSession>> {
        return dao.getSessionsInRange(startTime, endTime)
    }

    /**
     * Get total bytes sent across all sessions.
     */
    suspend fun getTotalBytesSent(): Long {
        return dao.getTotalBytesSent()
    }

    /**
     * Get total bytes received across all sessions.
     */
    suspend fun getTotalBytesReceived(): Long {
        return dao.getTotalBytesReceived()
    }

    /**
     * Get total connection duration across all sessions.
     */
    suspend fun getTotalDuration(): Long {
        return dao.getTotalDuration()
    }

    /**
     * Get the count of all sessions.
     */
    suspend fun getSessionCount(): Int {
        return dao.getSessionCount()
    }

    /**
     * Get the count of successful sessions.
     */
    suspend fun getSuccessfulSessionCount(): Int {
        return dao.getSuccessfulSessionCount()
    }

    /**
     * Calculate the success rate of connections.
     *
     * @return Success rate as a value between 0.0 and 1.0
     */
    suspend fun getSuccessRate(): Double {
        val total = dao.getSessionCount()
        if (total == 0) return 0.0
        val successful = dao.getSuccessfulSessionCount()
        return successful.toDouble() / total
    }

    /**
     * Get aggregate statistics across all sessions.
     *
     * @return AggregateStatistics with totals and counts
     */
    suspend fun getAggregateStatistics(): AggregateStatistics {
        return AggregateStatistics(
            totalBytesSent = dao.getTotalBytesSent(),
            totalBytesReceived = dao.getTotalBytesReceived(),
            totalDurationMillis = dao.getTotalDuration(),
            sessionCount = dao.getSessionCount(),
            successfulSessionCount = dao.getSuccessfulSessionCount(),
        )
    }

    /**
     * Delete a session.
     *
     * @param session The session to delete
     */
    suspend fun deleteSession(session: ConnectionSession) {
        dao.delete(session)
    }

    /**
     * Delete all sessions.
     */
    suspend fun deleteAllSessions() {
        dao.deleteAll()
    }

    /**
     * Delete sessions older than a given timestamp.
     *
     * @param timestamp Sessions with startTime before this will be deleted
     * @return Number of sessions deleted
     */
    suspend fun deleteSessionsOlderThan(timestamp: Long): Int {
        return dao.deleteOlderThan(timestamp)
    }

    /**
     * Delete sessions for a specific profile.
     *
     * @param profileId The profile ID
     * @return Number of sessions deleted
     */
    suspend fun deleteSessionsForProfile(profileId: Long): Int {
        return dao.deleteForProfile(profileId)
    }

    /**
     * Clean up old sessions, keeping only the most recent ones.
     *
     * @param daysToKeep Number of days of history to keep
     * @return Number of sessions deleted
     */
    suspend fun cleanupOldSessions(daysToKeep: Int = 30): Int {
        val cutoffTime = System.currentTimeMillis() - daysToKeep * 24 * 60 * 60 * 1000L
        return dao.deleteOlderThan(cutoffTime)
    }
}
