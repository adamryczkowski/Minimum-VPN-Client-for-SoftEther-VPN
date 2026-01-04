package kittoku.mvc.logging

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

/**
 * Structured logging for VPN operations.
 *
 * Provides structured logging with context, log buffering for export,
 * and integration with Android's Log system.
 *
 * Usage:
 * ```kotlin
 * class MyClass {
 *     private val logger = VpnLogger.getLogger<MyClass>()
 *
 *     fun doSomething() {
 *         logger.info("Starting operation", "userId" to "123")
 *         try {
 *             // ...
 *         } catch (e: Exception) {
 *             logger.error("Operation failed", e, "userId" to "123")
 *         }
 *     }
 * }
 * ```
 */
class VpnLogger private constructor(
    private val name: String,
) {
    private val shortName: String = name.substringAfterLast('.')

    /**
     * Log a trace message.
     */
    fun trace(
        message: String,
        vararg context: Pair<String, String>,
    ) {
        log(LogLevel.TRACE, message, null, *context)
    }

    /**
     * Log a trace message with lazy evaluation.
     */
    fun trace(
        vararg context: Pair<String, String>,
        message: () -> String,
    ) {
        if (isEnabled(LogLevel.TRACE)) {
            log(LogLevel.TRACE, message(), null, *context)
        }
    }

    /**
     * Log a debug message.
     */
    fun debug(
        message: String,
        vararg context: Pair<String, String>,
    ) {
        log(LogLevel.DEBUG, message, null, *context)
    }

    /**
     * Log a debug message with lazy evaluation.
     */
    fun debug(
        vararg context: Pair<String, String>,
        message: () -> String,
    ) {
        if (isEnabled(LogLevel.DEBUG)) {
            log(LogLevel.DEBUG, message(), null, *context)
        }
    }

    /**
     * Log an info message.
     */
    fun info(
        message: String,
        vararg context: Pair<String, String>,
    ) {
        log(LogLevel.INFO, message, null, *context)
    }

    /**
     * Log an info message with lazy evaluation.
     */
    fun info(
        vararg context: Pair<String, String>,
        message: () -> String,
    ) {
        if (isEnabled(LogLevel.INFO)) {
            log(LogLevel.INFO, message(), null, *context)
        }
    }

    /**
     * Log a warning message.
     */
    fun warn(
        message: String,
        vararg context: Pair<String, String>,
    ) {
        log(LogLevel.WARN, message, null, *context)
    }

    /**
     * Log a warning message with exception.
     */
    fun warn(
        message: String,
        throwable: Throwable,
        vararg context: Pair<String, String>,
    ) {
        log(LogLevel.WARN, message, throwable, *context)
    }

    /**
     * Log an error message.
     */
    fun error(
        message: String,
        vararg context: Pair<String, String>,
    ) {
        log(LogLevel.ERROR, message, null, *context)
    }

    /**
     * Log an error message with exception.
     */
    fun error(
        message: String,
        throwable: Throwable,
        vararg context: Pair<String, String>,
    ) {
        log(LogLevel.ERROR, message, throwable, *context)
    }

    /**
     * Log a fatal error message.
     */
    fun fatal(
        message: String,
        vararg context: Pair<String, String>,
    ) {
        log(LogLevel.FATAL, message, null, *context)
    }

    /**
     * Log a fatal error message with exception.
     */
    fun fatal(
        message: String,
        throwable: Throwable,
        vararg context: Pair<String, String>,
    ) {
        log(LogLevel.FATAL, message, throwable, *context)
    }

    /**
     * Check if a log level is enabled.
     */
    fun isEnabled(level: LogLevel): Boolean {
        return level.priority >= config.get().minLevel.priority
    }

    private fun log(
        level: LogLevel,
        message: String,
        throwable: Throwable?,
        vararg context: Pair<String, String>,
    ) {
        if (!isEnabled(level)) return

        val currentConfig = config.get()
        val timestamp = dateFormat.get()!!.format(Date())

        // Build context map
        val contextMap = mutableMapOf<String, String>()
        globalContext.get().forEach { contextMap[it.key] = it.value }
        context.forEach { contextMap[it.first] = it.second }

        // Create log event
        val event =
            LogEvent(
                timestamp = timestamp,
                level = level.name,
                logger = name,
                message = message,
                context = contextMap,
                exceptionType = throwable?.javaClass?.name,
                exceptionMessage = throwable?.message,
                stackTrace = throwable?.let { getStackTrace(it) },
                connectionId = currentConnectionId.get(),
            )

        // Output to Android Log
        if (currentConfig.logToAndroid) {
            val tag = "VPN:$shortName"
            val logMessage = formatForAndroid(event)
            when (level) {
                LogLevel.TRACE -> Log.v(tag, logMessage, throwable)
                LogLevel.DEBUG -> Log.d(tag, logMessage, throwable)
                LogLevel.INFO -> Log.i(tag, logMessage, throwable)
                LogLevel.WARN -> Log.w(tag, logMessage, throwable)
                LogLevel.ERROR -> Log.e(tag, logMessage, throwable)
                LogLevel.FATAL -> Log.wtf(tag, logMessage, throwable)
            }
        }

        // Buffer for export
        if (currentConfig.bufferLogs) {
            logBuffer.offer(event)
            // Trim buffer if too large
            while (logBuffer.size > currentConfig.maxBufferSize) {
                logBuffer.poll()
            }
        }

        // Notify listeners
        listeners.forEach { it.onLogEvent(event) }
    }

    private fun formatForAndroid(event: LogEvent): String {
        val contextStr =
            if (event.context.isNotEmpty()) {
                event.context.entries.joinToString(", ", " [", "]") { "${it.key}=${it.value}" }
            } else {
                ""
            }
        return "${event.message}$contextStr"
    }

    private fun getStackTrace(throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

    companion object {
        private val loggers = mutableMapOf<String, VpnLogger>()
        private val config = AtomicReference(LogConfig())
        private val logBuffer = ConcurrentLinkedQueue<LogEvent>()
        private val globalContext = AtomicReference(mapOf<String, String>())
        private val currentConnectionId = AtomicReference<String?>(null)
        private val listeners = mutableListOf<LogListener>()

        private val dateFormat =
            object : ThreadLocal<SimpleDateFormat>() {
                override fun initialValue(): SimpleDateFormat {
                    return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
                }
            }

        /**
         * Get a logger for a class.
         */
        inline fun <reified T> getLogger(): VpnLogger {
            return getLogger(T::class.java.name)
        }

        /**
         * Get a logger by name.
         */
        fun getLogger(name: String): VpnLogger {
            return synchronized(loggers) {
                loggers.getOrPut(name) { VpnLogger(name) }
            }
        }

        /**
         * Configure logging.
         */
        fun configure(block: LogConfig.() -> Unit) {
            val newConfig = config.get().copy()
            newConfig.block()
            config.set(newConfig)
        }

        /**
         * Set global context that will be included in all log events.
         */
        fun setGlobalContext(vararg pairs: Pair<String, String>) {
            globalContext.set(pairs.toMap())
        }

        /**
         * Add to global context.
         */
        fun addGlobalContext(vararg pairs: Pair<String, String>) {
            val current = globalContext.get()
            globalContext.set(current + pairs.toMap())
        }

        /**
         * Clear global context.
         */
        fun clearGlobalContext() {
            globalContext.set(emptyMap())
        }

        /**
         * Set current connection ID for correlation.
         */
        fun setConnectionId(id: String?) {
            currentConnectionId.set(id)
        }

        /**
         * Get current connection ID.
         */
        fun getConnectionId(): String? {
            return currentConnectionId.get()
        }

        /**
         * Add a log listener.
         */
        fun addListener(listener: LogListener) {
            synchronized(listeners) {
                listeners.add(listener)
            }
        }

        /**
         * Remove a log listener.
         */
        fun removeListener(listener: LogListener) {
            synchronized(listeners) {
                listeners.remove(listener)
            }
        }

        /**
         * Get buffered log events.
         */
        fun getBufferedLogs(): List<LogEvent> {
            return logBuffer.toList()
        }

        /**
         * Get buffered logs filtered by level.
         */
        fun getBufferedLogs(minLevel: LogLevel): List<LogEvent> {
            return logBuffer.filter {
                LogLevel.valueOf(it.level).priority >= minLevel.priority
            }
        }

        /**
         * Clear the log buffer.
         */
        fun clearBuffer() {
            logBuffer.clear()
        }

        /**
         * Export logs as JSON array.
         */
        fun exportLogsAsJson(pretty: Boolean = false): String {
            val logs = getBufferedLogs()
            return if (pretty) {
                "[\n${logs.joinToString(",\n") { it.toJson(true) }}\n]"
            } else {
                "[${logs.joinToString(",") { it.toJson(false) }}]"
            }
        }

        /**
         * Export logs as text.
         */
        fun exportLogsAsText(): String {
            return getBufferedLogs().joinToString("\n") { it.toLogLine() }
        }
    }

    /**
     * Listener for log events.
     */
    fun interface LogListener {
        fun onLogEvent(event: LogEvent)
    }
}

/**
 * Logging configuration.
 */
data class LogConfig(
    /** Minimum log level to output */
    var minLevel: LogLevel = LogLevel.DEBUG,
    /** Whether to output to Android Log */
    var logToAndroid: Boolean = true,
    /** Whether to buffer logs for export */
    var bufferLogs: Boolean = true,
    /** Maximum number of log events to buffer */
    var maxBufferSize: Int = 1000,
)
