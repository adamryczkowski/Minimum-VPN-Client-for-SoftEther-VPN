package kittoku.mvc.logging

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

@DisplayName("DiagnosticExporter")
class DiagnosticExporterTest {
    private lateinit var mockContext: Context
    private lateinit var exporter: DiagnosticExporter
    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        // Create temp directory for cache
        tempDir = File(System.getProperty("java.io.tmpdir"), "diagnostic_test_${System.currentTimeMillis()}")
        tempDir.mkdirs()

        // Mock context
        mockContext = mockk(relaxed = true)
        every { mockContext.cacheDir } returns tempDir

        exporter = DiagnosticExporter(mockContext)

        // Configure VpnLogger for testing
        VpnLogger.clearBuffer()
        VpnLogger.clearGlobalContext()
        VpnLogger.setConnectionId(null)
        VpnLogger.configure {
            minLevel = LogLevel.TRACE
            logToAndroid = false
            bufferLogs = true
            maxBufferSize = 100
        }
    }

    @AfterEach
    fun tearDown() {
        VpnLogger.clearBuffer()
        VpnLogger.clearGlobalContext()
        tempDir.deleteRecursively()
    }

    @Nested
    @DisplayName("Log Export as JSON")
    inner class LogExportAsJson {
        @Test
        @DisplayName("should export logs as JSON array")
        fun exportLogsAsJson() {
            val logger = VpnLogger.getLogger<DiagnosticExporterTest>()
            logger.info("Test message", "key" to "value")

            val json = exporter.exportLogsAsJson()

            assertThat(json).startsWith("[")
            assertThat(json).endsWith("]")
            assertThat(json).contains("Test message")
            assertThat(json).contains("\"key\":\"value\"")
        }

        @Test
        @DisplayName("should export logs as pretty JSON")
        fun exportLogsAsPrettyJson() {
            val logger = VpnLogger.getLogger<DiagnosticExporterTest>()
            logger.info("Test message")

            val json = exporter.exportLogsAsJson(pretty = true)

            assertThat(json).contains("\n")
            assertThat(json).contains("Test message")
        }

        @Test
        @DisplayName("should export empty array when no logs")
        fun exportEmptyArray() {
            val json = exporter.exportLogsAsJson()

            assertThat(json).isEqualTo("[]")
        }

        @Test
        @DisplayName("should filter logs by minimum level")
        fun filterLogsByLevel() {
            val logger = VpnLogger.getLogger<DiagnosticExporterTest>()
            logger.debug("Debug message")
            logger.info("Info message")
            logger.warn("Warn message")
            logger.error("Error message")

            val json = exporter.exportLogsAsJson(minLevel = LogLevel.WARN)

            assertThat(json).doesNotContain("Debug message")
            assertThat(json).doesNotContain("Info message")
            assertThat(json).contains("Warn message")
            assertThat(json).contains("Error message")
        }

        @Test
        @DisplayName("should include all log event fields in JSON")
        fun includeAllFields() {
            VpnLogger.setConnectionId("conn-123")
            val logger = VpnLogger.getLogger<DiagnosticExporterTest>()
            logger.error("Error occurred", RuntimeException("Test error"), "userId" to "user-456")

            val json = exporter.exportLogsAsJson()

            assertThat(json).contains("\"level\":\"ERROR\"")
            assertThat(json).contains("\"message\":\"Error occurred\"")
            assertThat(json).contains("\"connectionId\":\"conn-123\"")
            assertThat(json).contains("\"userId\":\"user-456\"")
            assertThat(json).contains("\"exceptionType\":\"java.lang.RuntimeException\"")
            assertThat(json).contains("\"exceptionMessage\":\"Test error\"")
        }
    }

    @Nested
    @DisplayName("Log Export as Text")
    inner class LogExportAsText {
        @Test
        @DisplayName("should export logs as text")
        fun exportLogsAsText() {
            val logger = VpnLogger.getLogger<DiagnosticExporterTest>()
            logger.info("Test message", "key" to "value")

            val text = exporter.exportLogsAsText()

            assertThat(text).contains("Test message")
            assertThat(text).contains("key=value")
            assertThat(text).contains("INFO")
        }

        @Test
        @DisplayName("should export empty string when no logs")
        fun exportEmptyString() {
            val text = exporter.exportLogsAsText()

            assertThat(text).isEmpty()
        }

        @Test
        @DisplayName("should filter logs by minimum level")
        fun filterLogsByLevel() {
            val logger = VpnLogger.getLogger<DiagnosticExporterTest>()
            logger.debug("Debug message")
            logger.info("Info message")
            logger.warn("Warn message")
            logger.error("Error message")

            val text = exporter.exportLogsAsText(minLevel = LogLevel.WARN)

            assertThat(text).doesNotContain("Debug message")
            assertThat(text).doesNotContain("Info message")
            assertThat(text).contains("Warn message")
            assertThat(text).contains("Error message")
        }

        @Test
        @DisplayName("should include exception info in text")
        fun includeExceptionInfo() {
            val logger = VpnLogger.getLogger<DiagnosticExporterTest>()
            logger.error("Error occurred", RuntimeException("Test error"))

            val text = exporter.exportLogsAsText()

            assertThat(text).contains("Error occurred")
            assertThat(text).contains("RuntimeException")
            assertThat(text).contains("Test error")
        }

        @Test
        @DisplayName("should include context in text")
        fun includeContext() {
            val logger = VpnLogger.getLogger<DiagnosticExporterTest>()
            logger.info("Test message", "key1" to "value1", "key2" to "value2")

            val text = exporter.exportLogsAsText()

            assertThat(text).contains("key1=value1")
            assertThat(text).contains("key2=value2")
        }

        @Test
        @DisplayName("should separate log entries with newlines")
        fun separateEntriesWithNewlines() {
            val logger = VpnLogger.getLogger<DiagnosticExporterTest>()
            logger.info("Message 1")
            logger.info("Message 2")
            logger.info("Message 3")

            val text = exporter.exportLogsAsText()
            val lines = text.split("\n")

            assertThat(lines).hasSize(3)
            assertThat(lines[0]).contains("Message 1")
            assertThat(lines[1]).contains("Message 2")
            assertThat(lines[2]).contains("Message 3")
        }
    }

    @Nested
    @DisplayName("Backward Compatibility")
    inner class BackwardCompatibility {
        @Test
        @DisplayName("should produce same JSON output as deprecated VpnLogger.exportLogsAsJson")
        @Suppress("DEPRECATION")
        fun sameJsonOutput() {
            val logger = VpnLogger.getLogger<DiagnosticExporterTest>()
            logger.info("Test message", "key" to "value")

            val exporterJson = exporter.exportLogsAsJson()
            val vpnLoggerJson = VpnLogger.exportLogsAsJson()

            assertThat(exporterJson).isEqualTo(vpnLoggerJson)
        }

        @Test
        @DisplayName("should produce same text output as deprecated VpnLogger.exportLogsAsText")
        @Suppress("DEPRECATION")
        fun sameTextOutput() {
            val logger = VpnLogger.getLogger<DiagnosticExporterTest>()
            logger.info("Test message", "key" to "value")

            val exporterText = exporter.exportLogsAsText()
            val vpnLoggerText = VpnLogger.exportLogsAsText()

            assertThat(exporterText).isEqualTo(vpnLoggerText)
        }
    }
}
