package kittoku.mvc.logging

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Structured log event with metadata.
 *
 * Captures all relevant information about a log event including
 * timestamp, level, message, context, and optional exception details.
 */
@Serializable
data class LogEvent(
    /** Timestamp in ISO 8601 format */
    val timestamp: String,
    /** Log severity level */
    val level: String,
    /** Logger name (usually class name) */
    val logger: String,
    /** Human-readable message */
    val message: String,
    /** Structured context data */
    val context: Map<String, String> = emptyMap(),
    /** Exception class name if present */
    val exceptionType: String? = null,
    /** Exception message if present */
    val exceptionMessage: String? = null,
    /** Stack trace if present */
    val stackTrace: String? = null,
    /** Thread name */
    val thread: String = Thread.currentThread().name,
    /** VPN connection ID for correlation */
    val connectionId: String? = null,
) {
    companion object {
        private val json =
            Json {
                prettyPrint = false
                encodeDefaults = false
            }

        private val prettyJson =
            Json {
                prettyPrint = true
                encodeDefaults = false
            }
    }

    /**
     * Convert to JSON string for storage/export.
     */
    fun toJson(pretty: Boolean = false): String {
        return if (pretty) prettyJson.encodeToString(this) else json.encodeToString(this)
    }

    /**
     * Format as human-readable log line.
     */
    fun toLogLine(): String {
        val contextStr =
            if (context.isNotEmpty()) {
                context.entries.joinToString(", ", " [", "]") { "${it.key}=${it.value}" }
            } else {
                ""
            }

        val exceptionStr =
            if (exceptionType != null) {
                "\n  Exception: $exceptionType: $exceptionMessage"
            } else {
                ""
            }

        return "$timestamp $level [$thread] $logger: $message$contextStr$exceptionStr"
    }

    /**
     * Create a copy with additional context.
     */
    fun withContext(vararg pairs: Pair<String, String>): LogEvent {
        return copy(context = context + pairs.toMap())
    }

    /**
     * Create a copy with connection ID.
     */
    fun withConnectionId(id: String): LogEvent {
        return copy(connectionId = id)
    }
}
