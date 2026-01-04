package kittoku.mvc.statistics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Timer for tracking VPN connection duration.
 *
 * Provides elapsed time in various units and formatted strings,
 * with Flow support for observation.
 */
class ConnectionTimer {
    private var _startTime: Long? = null
    private var _isRunning: Boolean = false
    private var _stoppedElapsedMillis: Long = 0L

    private val _durationFlow = MutableStateFlow(0L)

    /**
     * Whether the timer is currently running.
     */
    val isRunning: Boolean
        get() = _isRunning

    /**
     * The timestamp when the timer was started (null if never started).
     */
    val startTime: Long?
        get() = _startTime

    /**
     * Elapsed time in milliseconds.
     */
    val elapsedMillis: Long
        get() {
            return if (_isRunning && _startTime != null) {
                System.currentTimeMillis() - _startTime!!
            } else {
                _stoppedElapsedMillis
            }
        }

    /**
     * Elapsed time in seconds.
     */
    val elapsedSeconds: Long
        get() = elapsedMillis / 1000

    /**
     * Elapsed time in minutes.
     */
    val elapsedMinutes: Long
        get() = elapsedMillis / 60_000

    /**
     * Elapsed time in hours.
     */
    val elapsedHours: Long
        get() = elapsedMillis / 3600_000

    /**
     * Flow of elapsed duration in milliseconds for observation.
     */
    val durationFlow: StateFlow<Long>
        get() {
            updateDurationFlow()
            return _durationFlow.asStateFlow()
        }

    /**
     * Start the timer.
     *
     * If the timer is already running, this has no effect.
     *
     * @param startTime Optional custom start time (milliseconds since epoch).
     *                  Defaults to current time.
     */
    fun start(startTime: Long = System.currentTimeMillis()) {
        if (_isRunning) return

        _startTime = startTime
        _isRunning = true
        _stoppedElapsedMillis = 0L
        updateDurationFlow()
    }

    /**
     * Stop the timer.
     *
     * The elapsed time is preserved and can be retrieved after stopping.
     */
    fun stop() {
        if (!_isRunning) return

        _stoppedElapsedMillis = elapsedMillis
        _isRunning = false
        updateDurationFlow()
    }

    /**
     * Reset the timer to initial state.
     *
     * Clears start time and elapsed time.
     */
    fun reset() {
        _startTime = null
        _isRunning = false
        _stoppedElapsedMillis = 0L
        updateDurationFlow()
    }

    /**
     * Get the duration formatted as HH:MM:SS.
     *
     * @return Formatted duration string (e.g., "01:23:45")
     */
    fun getFormattedDuration(): String {
        val totalSeconds = elapsedSeconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (hours < 100) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        }
    }

    /**
     * Get a compact duration string.
     *
     * - Under 1 minute: "45s"
     * - Under 1 hour: "5m 30s"
     * - Over 1 hour: "2h 15m"
     *
     * @return Compact duration string
     */
    fun getCompactDuration(): String {
        val totalSeconds = elapsedSeconds
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    private fun updateDurationFlow() {
        _durationFlow.value = elapsedMillis
    }
}
