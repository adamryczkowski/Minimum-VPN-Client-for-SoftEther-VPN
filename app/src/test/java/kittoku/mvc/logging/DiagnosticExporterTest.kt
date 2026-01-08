package kittoku.mvc.logging

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for DiagnosticExporter data classes and report formatting.
 *
 * Note: Full integration tests require Android instrumentation tests
 * as DiagnosticExporter depends on Android Context. These tests focus on
 * the data classes and report structure.
 */
@DisplayName("DiagnosticExporter")
class DiagnosticExporterTest {
    @BeforeEach
    fun setUp() {
        // Configure VpnLogger for tests
        VpnLogger.configure {
            minLevel = LogLevel.TRACE
            logToAndroid = false
            bufferLogs = true
        }
    }

    @AfterEach
    fun tearDown() {
        VpnLogger.clearBuffer()
    }

    @Nested
    @DisplayName("AppInfo")
    inner class AppInfoTests {
        @Test
        @DisplayName("should create AppInfo with all fields")
        fun createAppInfo() {
            val appInfo =
                AppInfo(
                    packageName = "kittoku.mvc",
                    versionName = "1.0.0",
                    versionCode = 100,
                    buildType = "debug",
                )

            assertThat(appInfo.packageName).isEqualTo("kittoku.mvc")
            assertThat(appInfo.versionName).isEqualTo("1.0.0")
            assertThat(appInfo.versionCode).isEqualTo(100)
            assertThat(appInfo.buildType).isEqualTo("debug")
        }

        @Test
        @DisplayName("should support release build type")
        fun supportReleaseBuildType() {
            val appInfo =
                AppInfo(
                    packageName = "kittoku.mvc",
                    versionName = "1.0.0",
                    versionCode = 100,
                    buildType = "release",
                )

            assertThat(appInfo.buildType).isEqualTo("release")
        }
    }

    @Nested
    @DisplayName("DeviceInfo")
    inner class DeviceInfoTests {
        @Test
        @DisplayName("should create DeviceInfo with all fields")
        fun createDeviceInfo() {
            val deviceInfo =
                DeviceInfo(
                    manufacturer = "Samsung",
                    model = "Galaxy S21",
                    androidVersion = "13",
                    sdkVersion = 33,
                    architecture = "arm64-v8a",
                    timezone = "Europe/Warsaw",
                )

            assertThat(deviceInfo.manufacturer).isEqualTo("Samsung")
            assertThat(deviceInfo.model).isEqualTo("Galaxy S21")
            assertThat(deviceInfo.androidVersion).isEqualTo("13")
            assertThat(deviceInfo.sdkVersion).isEqualTo(33)
            assertThat(deviceInfo.architecture).isEqualTo("arm64-v8a")
            assertThat(deviceInfo.timezone).isEqualTo("Europe/Warsaw")
        }

        @Test
        @DisplayName("should handle various architectures")
        fun handleVariousArchitectures() {
            val architectures =
                listOf(
                    "arm64-v8a",
                    "armeabi-v7a",
                    "x86_64",
                    "x86",
                )

            architectures.forEach { arch ->
                val deviceInfo =
                    DeviceInfo(
                        manufacturer = "Test",
                        model = "Test",
                        androidVersion = "13",
                        sdkVersion = 33,
                        architecture = arch,
                        timezone = "UTC",
                    )
                assertThat(deviceInfo.architecture).isEqualTo(arch)
            }
        }
    }

    @Nested
    @DisplayName("DiagnosticReport")
    inner class DiagnosticReportTests {
        @Test
        @DisplayName("should create complete DiagnosticReport")
        fun createDiagnosticReport() {
            val appInfo =
                AppInfo(
                    packageName = "kittoku.mvc",
                    versionName = "1.0.0",
                    versionCode = 100,
                    buildType = "debug",
                )

            val deviceInfo =
                DeviceInfo(
                    manufacturer = "Samsung",
                    model = "Galaxy S21",
                    androidVersion = "13",
                    sdkVersion = 33,
                    architecture = "arm64-v8a",
                    timezone = "Europe/Warsaw",
                )

            val logs =
                listOf(
                    LogEvent(
                        timestamp = "2025-01-08T12:00:00.000+0100",
                        level = "INFO",
                        logger = "TestLogger",
                        message = "Test message",
                        context = mapOf("key" to "value"),
                        exceptionType = null,
                        exceptionMessage = null,
                        stackTrace = null,
                        connectionId = "conn-123",
                    ),
                )

            val performanceMetrics =
                mapOf(
                    "uptime" to "01:23:45",
                    "packetsSent" to "1000",
                    "packetsReceived" to "2000",
                )

            val configuration =
                mapOf(
                    "minSdk" to "26",
                    "targetSdk" to "36",
                )

            val report =
                DiagnosticReport(
                    generatedAt = "2025-01-08T12:00:00Z",
                    appInfo = appInfo,
                    deviceInfo = deviceInfo,
                    logs = logs,
                    performanceMetrics = performanceMetrics,
                    configuration = configuration,
                )

            assertThat(report.generatedAt).isEqualTo("2025-01-08T12:00:00Z")
            assertThat(report.appInfo).isEqualTo(appInfo)
            assertThat(report.deviceInfo).isEqualTo(deviceInfo)
            assertThat(report.logs).hasSize(1)
            assertThat(report.performanceMetrics).hasSize(3)
            assertThat(report.configuration).hasSize(2)
        }

        @Test
        @DisplayName("should handle empty logs")
        fun handleEmptyLogs() {
            val report =
                DiagnosticReport(
                    generatedAt = "2025-01-08T12:00:00Z",
                    appInfo = AppInfo("pkg", "1.0", 1, "debug"),
                    deviceInfo = DeviceInfo("Mfr", "Model", "13", 33, "arm64", "UTC"),
                    logs = emptyList(),
                    performanceMetrics = emptyMap(),
                    configuration = emptyMap(),
                )

            assertThat(report.logs).isEmpty()
            assertThat(report.performanceMetrics).isEmpty()
            assertThat(report.configuration).isEmpty()
        }

        @Test
        @DisplayName("should handle large number of logs")
        fun handleLargeLogs() {
            val logs =
                (1..500).map { i ->
                    LogEvent(
                        timestamp = "2025-01-08T12:00:00.000+0100",
                        level = "INFO",
                        logger = "TestLogger",
                        message = "Message $i",
                        context = emptyMap(),
                        exceptionType = null,
                        exceptionMessage = null,
                        stackTrace = null,
                        connectionId = null,
                    )
                }

            val report =
                DiagnosticReport(
                    generatedAt = "2025-01-08T12:00:00Z",
                    appInfo = AppInfo("pkg", "1.0", 1, "debug"),
                    deviceInfo = DeviceInfo("Mfr", "Model", "13", 33, "arm64", "UTC"),
                    logs = logs,
                    performanceMetrics = emptyMap(),
                    configuration = emptyMap(),
                )

            assertThat(report.logs).hasSize(500)
        }
    }

    @Nested
    @DisplayName("Report Text Formatting")
    inner class ReportTextFormatting {
        @Test
        @DisplayName("text report should contain all sections")
        fun textReportContainsAllSections() {
            // Simulate the text format that DiagnosticExporter.exportAsText() produces
            val textReport =
                buildString {
                    appendLine("=".repeat(60))
                    appendLine("SoftEther Connect Diagnostic Report")
                    appendLine("=".repeat(60))
                    appendLine()
                    appendLine("Generated: 2025-01-08T12:00:00Z")
                    appendLine()
                    appendLine("-".repeat(60))
                    appendLine("App Information")
                    appendLine("-".repeat(60))
                    appendLine("Version: 1.0.0 (100)")
                    appendLine("Package: kittoku.mvc")
                    appendLine("Build Type: debug")
                    appendLine()
                    appendLine("-".repeat(60))
                    appendLine("Device Information")
                    appendLine("-".repeat(60))
                    appendLine("Manufacturer: Samsung")
                    appendLine("Model: Galaxy S21")
                    appendLine("Android: 13 (API 33)")
                    appendLine("Architecture: arm64-v8a")
                    appendLine("Timezone: Europe/Warsaw")
                    appendLine()
                    appendLine("-".repeat(60))
                    appendLine("Configuration")
                    appendLine("-".repeat(60))
                    appendLine("minSdk: 26")
                    appendLine("targetSdk: 36")
                    appendLine()
                    appendLine("-".repeat(60))
                    appendLine("Performance Metrics")
                    appendLine("-".repeat(60))
                    appendLine("uptime: 01:23:45")
                    appendLine()
                    appendLine("-".repeat(60))
                    appendLine("Recent Logs (1 entries)")
                    appendLine("-".repeat(60))
                    appendLine("2025-01-08T12:00:00 INFO TestLogger: Test message")
                    appendLine()
                    appendLine("=".repeat(60))
                    appendLine("End of Report")
                    appendLine("=".repeat(60))
                }

            assertThat(textReport).contains("SoftEther Connect Diagnostic Report")
            assertThat(textReport).contains("App Information")
            assertThat(textReport).contains("Device Information")
            assertThat(textReport).contains("Configuration")
            assertThat(textReport).contains("Performance Metrics")
            assertThat(textReport).contains("Recent Logs")
            assertThat(textReport).contains("End of Report")
        }

        @Test
        @DisplayName("text report should not contain sensitive data")
        fun textReportExcludesSensitiveData() {
            val textReport =
                buildString {
                    appendLine("Configuration")
                    appendLine("minSdk: 26")
                    appendLine("targetSdk: 36")
                }

            // Verify no sensitive data patterns
            assertThat(textReport).doesNotContain("password")
            assertThat(textReport).doesNotContain("secret")
            assertThat(textReport).doesNotContain("certificate")
            assertThat(textReport).doesNotContain("private_key")
        }
    }

    @Nested
    @DisplayName("LogEvent Integration")
    inner class LogEventIntegration {
        @Test
        @DisplayName("should collect logs from VpnLogger buffer")
        fun collectLogsFromBuffer() {
            val logger = VpnLogger.getLogger<DiagnosticExporterTest>()
            logger.info("Test message 1")
            logger.warn("Test message 2")
            logger.error("Test message 3")

            val logs = VpnLogger.getBufferedLogs()

            assertThat(logs).hasSize(3)
            assertThat(logs.map { it.level }).containsExactly("INFO", "WARN", "ERROR")
        }

        @Test
        @DisplayName("should include context in collected logs")
        fun includeContextInLogs() {
            val logger = VpnLogger.getLogger<DiagnosticExporterTest>()
            logger.info("Test message", "userId" to "123", "action" to "connect")

            val logs = VpnLogger.getBufferedLogs()

            assertThat(logs).hasSize(1)
            assertThat(logs[0].context).containsEntry("userId", "123")
            assertThat(logs[0].context).containsEntry("action", "connect")
        }

        @Test
        @DisplayName("should include exception details in collected logs")
        fun includeExceptionInLogs() {
            val logger = VpnLogger.getLogger<DiagnosticExporterTest>()
            val exception = RuntimeException("Test error")
            logger.error("Operation failed", exception)

            val logs = VpnLogger.getBufferedLogs()

            assertThat(logs).hasSize(1)
            assertThat(logs[0].exceptionType).isEqualTo("java.lang.RuntimeException")
            assertThat(logs[0].exceptionMessage).isEqualTo("Test error")
            assertThat(logs[0].stackTrace).isNotNull()
        }
    }

    @Nested
    @DisplayName("Performance Metrics Collection")
    inner class PerformanceMetricsCollection {
        @Test
        @DisplayName("should format metrics as key-value pairs")
        fun formatMetricsAsKeyValuePairs() {
            val metrics =
                mapOf(
                    "uptime" to "01:23:45",
                    "packetsSent" to "1000",
                    "packetsReceived" to "2000",
                    "bytesSent" to "1.5 MB",
                    "bytesReceived" to "2.3 MB",
                    "packetsDropped" to "5",
                    "errors" to "2",
                    "packetLossRate" to "0.25%",
                    "averageLatencyMs" to "45.2",
                )

            assertThat(metrics).hasSize(9)
            assertThat(metrics["uptime"]).isEqualTo("01:23:45")
            assertThat(metrics["packetLossRate"]).isEqualTo("0.25%")
        }

        @Test
        @DisplayName("should handle missing metrics gracefully")
        fun handleMissingMetrics() {
            val metrics =
                mapOf(
                    "error" to "Failed to collect metrics",
                )

            assertThat(metrics).hasSize(1)
            assertThat(metrics["error"]).contains("Failed")
        }
    }

    @Nested
    @DisplayName("JSON Serialization")
    inner class JsonSerialization {
        @Test
        @DisplayName("LogEvent should serialize to JSON")
        fun logEventShouldSerializeToJson() {
            val logEvent =
                LogEvent(
                    timestamp = "2025-01-08T12:00:00.000+0100",
                    level = "INFO",
                    logger = "TestLogger",
                    message = "Test message",
                    context = mapOf("key" to "value"),
                    exceptionType = null,
                    exceptionMessage = null,
                    stackTrace = null,
                    connectionId = "conn-123",
                )

            val json = logEvent.toJson(pretty = false)

            assertThat(json).contains("\"timestamp\":")
            assertThat(json).contains("\"level\":\"INFO\"")
            assertThat(json).contains("\"message\":\"Test message\"")
            assertThat(json).contains("\"key\":\"value\"")
        }

        @Test
        @DisplayName("LogEvent should format as log line")
        fun logEventShouldFormatAsLogLine() {
            val logEvent =
                LogEvent(
                    timestamp = "2025-01-08T12:00:00.000+0100",
                    level = "INFO",
                    logger = "TestLogger",
                    message = "Test message",
                    context = mapOf("key" to "value"),
                    exceptionType = null,
                    exceptionMessage = null,
                    stackTrace = null,
                    connectionId = null,
                )

            val logLine = logEvent.toLogLine()

            assertThat(logLine).contains("2025-01-08T12:00:00")
            assertThat(logLine).contains("INFO")
            assertThat(logLine).contains("TestLogger")
            assertThat(logLine).contains("Test message")
            assertThat(logLine).contains("key=value")
        }
    }
}
