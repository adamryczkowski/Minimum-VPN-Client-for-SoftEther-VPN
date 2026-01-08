package kittoku.mvc.extension

import java.util.Locale

/**
 * Utility object for formatting values for display.
 *
 * This is the canonical source for formatting functions.
 * All formatting of bytes and durations should use this object.
 */
object Formatting {
    // Binary units (1 KB = 1024 bytes) - used for file sizes and network traffic
    private const val BINARY_KB = 1024.0
    private const val BINARY_MB = 1024.0 * 1024
    private const val BINARY_GB = 1024.0 * 1024 * 1024

    // Decimal units (1 KB = 1000 bytes) - used for some performance metrics
    private const val DECIMAL_KB = 1000.0
    private const val DECIMAL_MB = 1000.0 * 1000
    private const val DECIMAL_GB = 1000.0 * 1000 * 1000

    /**
     * Format bytes into a human-readable string using binary units.
     *
     * Uses binary units (1 KB = 1024 bytes) for consistency with
     * how storage and network traffic are typically displayed.
     *
     * @param bytes Number of bytes (must be non-negative)
     * @return Formatted string (e.g., "1.5 MB", "2.3 GB")
     */
    fun formatBytes(bytes: Long): String {
        require(bytes >= 0) { "Bytes must be non-negative, got: $bytes" }

        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / BINARY_KB)
            bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / BINARY_MB)
            else -> String.format(Locale.US, "%.1f GB", bytes / BINARY_GB)
        }
    }

    /**
     * Format bytes into a human-readable string using decimal units.
     *
     * Uses decimal units (1 KB = 1000 bytes) as used in some
     * performance monitoring contexts.
     *
     * @param bytes Number of bytes (must be non-negative)
     * @param decimalPlaces Number of decimal places (default: 2)
     * @return Formatted string (e.g., "1.50 MB", "2.30 GB")
     */
    fun formatBytesDecimal(
        bytes: Long,
        decimalPlaces: Int = 2,
    ): String {
        require(bytes >= 0) { "Bytes must be non-negative, got: $bytes" }

        val format = "%.${decimalPlaces}f"
        return when {
            bytes >= 1_000_000_000 -> String.format(Locale.US, "$format GB", bytes / DECIMAL_GB)
            bytes >= 1_000_000 -> String.format(Locale.US, "$format MB", bytes / DECIMAL_MB)
            bytes >= 1_000 -> String.format(Locale.US, "$format KB", bytes / DECIMAL_KB)
            else -> "$bytes B"
        }
    }

    /**
     * Format duration in milliseconds into HH:MM:SS format.
     *
     * For durations under 100 hours, uses zero-padded format (e.g., "01:23:45").
     * For durations of 100 hours or more, the hours are not zero-padded.
     *
     * @param durationMs Duration in milliseconds (must be non-negative)
     * @return Formatted string (e.g., "01:23:45")
     */
    fun formatDuration(durationMs: Long): String {
        require(durationMs >= 0) { "Duration must be non-negative, got: $durationMs" }

        val totalSeconds = durationMs / 1000
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
     * Format duration in seconds into HH:MM:SS format.
     *
     * Convenience method that converts seconds to milliseconds
     * and delegates to [formatDuration].
     *
     * @param durationSeconds Duration in seconds (must be non-negative)
     * @return Formatted string (e.g., "01:23:45")
     */
    fun formatDurationSeconds(durationSeconds: Long): String {
        require(durationSeconds >= 0) { "Duration must be non-negative, got: $durationSeconds" }
        return formatDuration(durationSeconds * 1000)
    }

    /**
     * Format duration into a short format.
     *
     * - Under 1 hour: "M:SS" (e.g., "5:30")
     * - 1 hour or more: "H:MM:SS" (e.g., "1:05:30")
     *
     * This format is useful for UI display where space is limited.
     *
     * @param durationMs Duration in milliseconds (must be non-negative)
     * @return Short duration string
     */
    fun formatDurationShort(durationMs: Long): String {
        require(durationMs >= 0) { "Duration must be non-negative, got: $durationMs" }

        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format(Locale.US, "%d:%02d", minutes, seconds)
        }
    }

    /**
     * Format duration into a compact human-readable string.
     *
     * - Under 1 minute: "45s"
     * - Under 1 hour: "5m 30s"
     * - Over 1 hour: "2h 15m"
     *
     * @param durationMs Duration in milliseconds (must be non-negative)
     * @return Compact duration string
     */
    fun formatDurationCompact(durationMs: Long): String {
        require(durationMs >= 0) { "Duration must be non-negative, got: $durationMs" }

        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }
}
