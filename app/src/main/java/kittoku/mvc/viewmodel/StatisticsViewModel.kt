package kittoku.mvc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kittoku.mvc.statistics.AggregateStatistics
import kittoku.mvc.statistics.ConnectionSession
import kittoku.mvc.statistics.ConnectionTimer
import kittoku.mvc.statistics.StatisticsRepository
import kittoku.mvc.statistics.TrafficCounter
import kittoku.mvc.statistics.TrafficSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the statistics screen.
 *
 * Provides UI state for displaying connection statistics including:
 * - Current session traffic and duration
 * - Historical session data
 * - Aggregate statistics
 *
 * @property repository Repository for accessing stored statistics
 * @property trafficCounter Counter for current session traffic
 * @property connectionTimer Timer for current session duration
 */
class StatisticsViewModel(
    private val repository: StatisticsRepository,
    private val trafficCounter: TrafficCounter,
    private val connectionTimer: ConnectionTimer,
) : ViewModel() {
    companion object {
        private const val RECENT_SESSIONS_LIMIT = 10
    }

    private val _isLoading = MutableStateFlow(true)

    /**
     * Whether data is currently being loaded.
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _aggregateStatistics = MutableStateFlow(AggregateStatistics.EMPTY)

    /**
     * Aggregate statistics across all sessions.
     */
    val aggregateStatistics: StateFlow<AggregateStatistics> = _aggregateStatistics.asStateFlow()

    /**
     * Current session traffic statistics.
     */
    val currentTraffic: StateFlow<TrafficSnapshot> =
        trafficCounter.trafficFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = TrafficSnapshot(0L, 0L),
            )

    /**
     * Current session duration in milliseconds.
     */
    val currentDuration: StateFlow<Long> =
        connectionTimer.durationFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0L,
            )

    /**
     * Recent connection sessions.
     */
    val recentSessions: StateFlow<List<ConnectionSession>> =
        repository.getRecentSessions(RECENT_SESSIONS_LIMIT)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList(),
            )

    init {
        loadAggregateStatistics()
    }

    /**
     * Refresh statistics data.
     */
    fun refresh() {
        loadAggregateStatistics()
    }

    private fun loadAggregateStatistics() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _aggregateStatistics.value = repository.getAggregateStatistics()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Delete a specific session.
     *
     * @param session The session to delete
     */
    fun deleteSession(session: ConnectionSession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            loadAggregateStatistics()
        }
    }

    /**
     * Delete all sessions.
     */
    fun deleteAllSessions() {
        viewModelScope.launch {
            repository.deleteAllSessions()
            loadAggregateStatistics()
        }
    }

    /**
     * Clean up old sessions, keeping only recent history.
     *
     * @param daysToKeep Number of days of history to keep
     */
    fun cleanupOldSessions(daysToKeep: Int = 30) {
        viewModelScope.launch {
            repository.cleanupOldSessions(daysToKeep)
            loadAggregateStatistics()
        }
    }

    /**
     * Get formatted current traffic sent.
     */
    fun getFormattedBytesSent(): String {
        return formatBytes(currentTraffic.value.bytesSent)
    }

    /**
     * Get formatted current traffic received.
     */
    fun getFormattedBytesReceived(): String {
        return formatBytes(currentTraffic.value.bytesReceived)
    }

    /**
     * Get formatted current duration.
     */
    fun getFormattedDuration(): String {
        val totalSeconds = currentDuration.value / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 ->
                String.format(
                    java.util.Locale.US,
                    "%.1f MB",
                    bytes / (1024.0 * 1024),
                )
            else -> String.format(java.util.Locale.US, "%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }
}
