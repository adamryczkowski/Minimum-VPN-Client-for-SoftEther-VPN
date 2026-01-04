package kittoku.mvc.statistics

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ConnectionSession entities.
 */
@Dao
interface ConnectionSessionDao {
    /**
     * Insert a new connection session.
     *
     * @param session The session to insert
     * @return The ID of the inserted session
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ConnectionSession): Long

    /**
     * Get all connection sessions ordered by start time (newest first).
     *
     * @return Flow of all sessions
     */
    @Query("SELECT * FROM connection_sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ConnectionSession>>

    /**
     * Get connection sessions for a specific profile.
     *
     * @param profileId The profile ID to filter by
     * @return Flow of sessions for the profile
     */
    @Query("SELECT * FROM connection_sessions WHERE profileId = :profileId ORDER BY startTime DESC")
    fun getSessionsForProfile(profileId: Long): Flow<List<ConnectionSession>>

    /**
     * Get the most recent N sessions.
     *
     * @param limit Maximum number of sessions to return
     * @return Flow of recent sessions
     */
    @Query("SELECT * FROM connection_sessions ORDER BY startTime DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<ConnectionSession>>

    /**
     * Get a session by ID.
     *
     * @param id The session ID
     * @return The session or null if not found
     */
    @Query("SELECT * FROM connection_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): ConnectionSession?

    /**
     * Get sessions within a time range.
     *
     * @param startTime Start of the time range (inclusive)
     * @param endTime End of the time range (inclusive)
     * @return Flow of sessions in the range
     */
    @Query("SELECT * FROM connection_sessions WHERE startTime >= :startTime AND startTime <= :endTime ORDER BY startTime DESC")
    fun getSessionsInRange(
        startTime: Long,
        endTime: Long,
    ): Flow<List<ConnectionSession>>

    /**
     * Get total bytes sent across all sessions.
     *
     * @return Total bytes sent
     */
    @Query("SELECT COALESCE(SUM(bytesSent), 0) FROM connection_sessions")
    suspend fun getTotalBytesSent(): Long

    /**
     * Get total bytes received across all sessions.
     *
     * @return Total bytes received
     */
    @Query("SELECT COALESCE(SUM(bytesReceived), 0) FROM connection_sessions")
    suspend fun getTotalBytesReceived(): Long

    /**
     * Get total connection duration across all sessions.
     *
     * @return Total duration in milliseconds
     */
    @Query("SELECT COALESCE(SUM(endTime - startTime), 0) FROM connection_sessions")
    suspend fun getTotalDuration(): Long

    /**
     * Get the count of all sessions.
     *
     * @return Number of sessions
     */
    @Query("SELECT COUNT(*) FROM connection_sessions")
    suspend fun getSessionCount(): Int

    /**
     * Get the count of successful sessions.
     *
     * @return Number of successful sessions
     */
    @Query("SELECT COUNT(*) FROM connection_sessions WHERE wasSuccessful = 1")
    suspend fun getSuccessfulSessionCount(): Int

    /**
     * Delete a session.
     *
     * @param session The session to delete
     */
    @Delete
    suspend fun delete(session: ConnectionSession)

    /**
     * Delete all sessions.
     */
    @Query("DELETE FROM connection_sessions")
    suspend fun deleteAll()

    /**
     * Delete sessions older than a given timestamp.
     *
     * @param timestamp Sessions with startTime before this will be deleted
     * @return Number of sessions deleted
     */
    @Query("DELETE FROM connection_sessions WHERE startTime < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int

    /**
     * Delete sessions for a specific profile.
     *
     * @param profileId The profile ID
     * @return Number of sessions deleted
     */
    @Query("DELETE FROM connection_sessions WHERE profileId = :profileId")
    suspend fun deleteForProfile(profileId: Long): Int
}
