package kittoku.mvc.logging

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@DisplayName("VpnLogger")
class VpnLoggerTest {
    @BeforeEach
    fun setUp() {
        VpnLogger.clearBuffer()
        VpnLogger.clearGlobalContext()
        VpnLogger.setConnectionId(null)
        VpnLogger.configure {
            minLevel = LogLevel.TRACE
            logToAndroid = false // Disable Android Log in tests
            bufferLogs = true
            maxBufferSize = 100
        }
    }

    @AfterEach
    fun tearDown() {
        VpnLogger.clearBuffer()
        VpnLogger.clearGlobalContext()
    }

    @Nested
    @DisplayName("Logger Creation")
    inner class LoggerCreation {
        @Test
        @DisplayName("should create logger with class name")
        fun createLoggerWithClassName() {
            val logger = VpnLogger.getLogger<VpnLoggerTest>()
            logger.info("Test message")

            val logs = VpnLogger.getBufferedLogs()
            assertThat(logs).hasSize(1)
            assertThat(logs[0].logger).contains("VpnLoggerTest")
        }

        @Test
        @DisplayName("should create logger with custom name")
        fun createLoggerWithCustomName() {
            val logger = VpnLogger.getLogger("CustomLogger")
            logger.info("Test message")

            val logs = VpnLogger.getBufferedLogs()
            assertThat(logs).hasSize(1)
            assertThat(logs[0].logger).isEqualTo("CustomLogger")
        }

        @Test
        @DisplayName("should reuse same logger instance")
        fun reuseLoggerInstance() {
            val logger1 = VpnLogger.getLogger<VpnLoggerTest>()
            val logger2 = VpnLogger.getLogger<VpnLoggerTest>()

            assertThat(logger1).isSameInstanceAs(logger2)
        }
    }

    @Nested
    @DisplayName("Log Levels")
    inner class LogLevels {
        @Test
        @DisplayName("should log at all levels")
        fun logAtAllLevels() {
            val logger = VpnLogger.getLogger<VpnLoggerTest>()

            logger.trace("Trace message")
            logger.debug("Debug message")
            logger.info("Info message")
            logger.warn("Warn message")
            logger.error("Error message")
            logger.fatal("Fatal message")

            val logs = VpnLogger.getBufferedLogs()
            assertThat(logs).hasSize(6)
            assertThat(logs.map { it.level }).containsExactly(
                "TRACE",
                "DEBUG",
                "INFO",
                "WARN",
                "ERROR",
                "FATAL",
            )
        }

        @Test
        @DisplayName("should respect minimum log level")
        fun respectMinLogLevel() {
            VpnLogger.configure { minLevel = LogLevel.WARN }
            val logger = VpnLogger.getLogger<VpnLoggerTest>()

            logger.trace("Trace message")
            logger.debug("Debug message")
            logger.info("Info message")
            logger.warn("Warn message")
            logger.error("Error message")

            val logs = VpnLogger.getBufferedLogs()
            assertThat(logs).hasSize(2)
            assertThat(logs.map { it.level }).containsExactly("WARN", "ERROR")
        }

        @Test
        @DisplayName("should check if level is enabled")
        fun checkLevelEnabled() {
            VpnLogger.configure { minLevel = LogLevel.INFO }
            val logger = VpnLogger.getLogger<VpnLoggerTest>()

            assertThat(logger.isEnabled(LogLevel.TRACE)).isFalse()
            assertThat(logger.isEnabled(LogLevel.DEBUG)).isFalse()
            assertThat(logger.isEnabled(LogLevel.INFO)).isTrue()
            assertThat(logger.isEnabled(LogLevel.WARN)).isTrue()
            assertThat(logger.isEnabled(LogLevel.ERROR)).isTrue()
        }
    }

    @Nested
    @DisplayName("Context")
    inner class Context {
        @Test
        @DisplayName("should include context in log events")
        fun includeContext() {
            val logger = VpnLogger.getLogger<VpnLoggerTest>()
            logger.info("Test message", "key1" to "value1", "key2" to "value2")

            val logs = VpnLogger.getBufferedLogs()
            assertThat(logs).hasSize(1)
            assertThat(logs[0].context).containsEntry("key1", "value1")
            assertThat(logs[0].context).containsEntry("key2", "value2")
        }

        @Test
        @DisplayName("should include global context")
        fun includeGlobalContext() {
            VpnLogger.setGlobalContext("app" to "test", "version" to "1.0")
            val logger = VpnLogger.getLogger<VpnLoggerTest>()
            logger.info("Test message")

            val logs = VpnLogger.getBufferedLogs()
            assertThat(logs).hasSize(1)
            assertThat(logs[0].context).containsEntry("app", "test")
            assertThat(logs[0].context).containsEntry("version", "1.0")
        }

        @Test
        @DisplayName("should merge global and local context")
        fun mergeContext() {
            VpnLogger.setGlobalContext("global" to "value")
            val logger = VpnLogger.getLogger<VpnLoggerTest>()
            logger.info("Test message", "local" to "value")

            val logs = VpnLogger.getBufferedLogs()
            assertThat(logs).hasSize(1)
            assertThat(logs[0].context).containsEntry("global", "value")
            assertThat(logs[0].context).containsEntry("local", "value")
        }

        @Test
        @DisplayName("should include connection ID")
        fun includeConnectionId() {
            VpnLogger.setConnectionId("conn-123")
            val logger = VpnLogger.getLogger<VpnLoggerTest>()
            logger.info("Test message")

            val logs = VpnLogger.getBufferedLogs()
            assertThat(logs).hasSize(1)
            assertThat(logs[0].connectionId).isEqualTo("conn-123")
        }
    }

    @Nested
    @DisplayName("Exception Logging")
    inner class ExceptionLogging {
        @Test
        @DisplayName("should log exception details")
        fun logExceptionDetails() {
            val logger = VpnLogger.getLogger<VpnLoggerTest>()
            val exception = RuntimeException("Test exception")
            logger.error("Error occurred", exception)

            val logs = VpnLogger.getBufferedLogs()
            assertThat(logs).hasSize(1)
            assertThat(logs[0].exceptionType).isEqualTo("java.lang.RuntimeException")
            assertThat(logs[0].exceptionMessage).isEqualTo("Test exception")
            assertThat(logs[0].stackTrace).isNotNull()
            assertThat(logs[0].stackTrace).contains("RuntimeException")
        }

        @Test
        @DisplayName("should log nested exception")
        fun logNestedException() {
            val logger = VpnLogger.getLogger<VpnLoggerTest>()
            val cause = IllegalArgumentException("Root cause")
            val exception = RuntimeException("Wrapper exception", cause)
            logger.error("Error occurred", exception)

            val logs = VpnLogger.getBufferedLogs()
            assertThat(logs).hasSize(1)
            assertThat(logs[0].stackTrace).contains("IllegalArgumentException")
            assertThat(logs[0].stackTrace).contains("Root cause")
        }
    }

    @Nested
    @DisplayName("Buffer Management")
    inner class BufferManagement {
        @Test
        @DisplayName("should buffer log events")
        fun bufferLogEvents() {
            val logger = VpnLogger.getLogger<VpnLoggerTest>()
            repeat(10) { i ->
                logger.info("Message $i")
            }

            val logs = VpnLogger.getBufferedLogs()
            assertThat(logs).hasSize(10)
        }

        @Test
        @DisplayName("should limit buffer size")
        fun limitBufferSize() {
            VpnLogger.configure { maxBufferSize = 5 }
            val logger = VpnLogger.getLogger<VpnLoggerTest>()
            repeat(10) { i ->
                logger.info("Message $i")
            }

            val logs = VpnLogger.getBufferedLogs()
            assertThat(logs).hasSize(5)
            // Should have the most recent messages
            assertThat(logs.last().message).isEqualTo("Message 9")
        }

        @Test
        @DisplayName("should clear buffer")
        fun clearBuffer() {
            val logger = VpnLogger.getLogger<VpnLoggerTest>()
            logger.info("Test message")
            assertThat(VpnLogger.getBufferedLogs()).isNotEmpty()

            VpnLogger.clearBuffer()
            assertThat(VpnLogger.getBufferedLogs()).isEmpty()
        }

        @Test
        @DisplayName("should filter logs by level")
        fun filterLogsByLevel() {
            val logger = VpnLogger.getLogger<VpnLoggerTest>()
            logger.debug("Debug message")
            logger.info("Info message")
            logger.warn("Warn message")
            logger.error("Error message")

            val warnAndAbove = VpnLogger.getBufferedLogs(LogLevel.WARN)
            assertThat(warnAndAbove).hasSize(2)
            assertThat(warnAndAbove.map { it.level }).containsExactly("WARN", "ERROR")
        }
    }

    @Nested
    @DisplayName("Export (Deprecated)")
    inner class Export {
        @Test
        @DisplayName("should export logs as JSON (deprecated - use DiagnosticExporter)")
        @Suppress("DEPRECATION")
        fun exportAsJson() {
            val logger = VpnLogger.getLogger<VpnLoggerTest>()
            logger.info("Test message", "key" to "value")

            val json = VpnLogger.exportLogsAsJson()
            assertThat(json).startsWith("[")
            assertThat(json).endsWith("]")
            assertThat(json).contains("Test message")
            assertThat(json).contains("\"key\":\"value\"")
        }

        @Test
        @DisplayName("should export logs as text (deprecated - use DiagnosticExporter)")
        @Suppress("DEPRECATION")
        fun exportAsText() {
            val logger = VpnLogger.getLogger<VpnLoggerTest>()
            logger.info("Test message", "key" to "value")

            val text = VpnLogger.exportLogsAsText()
            assertThat(text).contains("Test message")
            assertThat(text).contains("key=value")
            assertThat(text).contains("INFO")
        }
    }

    @Nested
    @DisplayName("Listeners")
    inner class Listeners {
        @Test
        @DisplayName("should notify listeners")
        fun notifyListeners() {
            val receivedEvents = mutableListOf<LogEvent>()
            val listener =
                VpnLogger.LogListener { event ->
                    receivedEvents.add(event)
                }

            VpnLogger.addListener(listener)
            try {
                val logger = VpnLogger.getLogger<VpnLoggerTest>()
                logger.info("Test message")

                assertThat(receivedEvents).hasSize(1)
                assertThat(receivedEvents[0].message).isEqualTo("Test message")
            } finally {
                VpnLogger.removeListener(listener)
            }
        }

        @Test
        @DisplayName("should remove listeners")
        fun removeListeners() {
            val receivedEvents = mutableListOf<LogEvent>()
            val listener =
                VpnLogger.LogListener { event ->
                    receivedEvents.add(event)
                }

            VpnLogger.addListener(listener)
            VpnLogger.removeListener(listener)

            val logger = VpnLogger.getLogger<VpnLoggerTest>()
            logger.info("Test message")

            assertThat(receivedEvents).isEmpty()
        }
    }

    @Nested
    @DisplayName("Lazy Evaluation")
    inner class LazyEvaluation {
        @Test
        @DisplayName("should not evaluate message when level is disabled")
        fun skipEvaluationWhenDisabled() {
            VpnLogger.configure { minLevel = LogLevel.ERROR }
            val logger = VpnLogger.getLogger<VpnLoggerTest>()

            var evaluated = false
            logger.debug {
                evaluated = true
                "This should not be evaluated"
            }

            assertThat(evaluated).isFalse()
        }

        @Test
        @DisplayName("should evaluate message when level is enabled")
        fun evaluateWhenEnabled() {
            VpnLogger.configure { minLevel = LogLevel.DEBUG }
            val logger = VpnLogger.getLogger<VpnLoggerTest>()

            var evaluated = false
            logger.debug {
                evaluated = true
                "This should be evaluated"
            }

            assertThat(evaluated).isTrue()
        }
    }

    @Nested
    @DisplayName("Thread Safety")
    inner class ThreadSafety {
        @Test
        @DisplayName("should handle concurrent logging")
        fun handleConcurrentLogging() {
            val logger = VpnLogger.getLogger<VpnLoggerTest>()
            val threadCount = 10
            val messagesPerThread = 100
            val latch = CountDownLatch(threadCount)

            repeat(threadCount) { threadId ->
                Thread {
                    repeat(messagesPerThread) { msgId ->
                        logger.info("Thread $threadId message $msgId")
                    }
                    latch.countDown()
                }.start()
            }

            latch.await(10, TimeUnit.SECONDS)

            val logs = VpnLogger.getBufferedLogs()
            // Buffer is limited, but should have some logs
            assertThat(logs).isNotEmpty()
        }
    }
}
