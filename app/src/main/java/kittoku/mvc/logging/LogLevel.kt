package kittoku.mvc.logging

/**
 * Log severity levels for structured logging.
 *
 * Levels are ordered from most verbose (TRACE) to most severe (FATAL).
 */
enum class LogLevel(val priority: Int, val tag: String) {
    /** Fine-grained debugging information */
    TRACE(0, "T"),

    /** Debugging information */
    DEBUG(1, "D"),

    /** Informational messages */
    INFO(2, "I"),

    /** Warning conditions */
    WARN(3, "W"),

    /** Error conditions */
    ERROR(4, "E"),

    /** Fatal errors that cause application termination */
    FATAL(5, "F"),
    ;

    companion object {
        fun fromPriority(priority: Int): LogLevel {
            return entries.find { it.priority == priority } ?: INFO
        }
    }
}
