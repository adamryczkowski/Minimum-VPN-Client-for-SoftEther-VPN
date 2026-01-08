package kittoku.mvc.logging

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Unit tests for CrashHandler.
 *
 * Note: Full integration tests require Android instrumentation tests
 * as CrashHandler depends on Android Context. These tests focus on
 * the static utility methods and crash report file handling.
 */
@DisplayName("CrashHandler")
class CrashHandlerTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var crashDir: File

    @BeforeEach
    fun setUp() {
        crashDir = File(tempDir, "crashes")
        crashDir.mkdirs()

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
    @DisplayName("Crash Report File Operations")
    inner class CrashReportFileOperations {
        @Test
        @DisplayName("should read crash report content")
        fun readCrashReportContent() {
            val crashContent =
                """
                ============================================================
                SoftEther Connect Crash Report
                ============================================================

                Timestamp: 2025-01-08 12:00:00 +0100

                Exception Type: java.lang.RuntimeException
                Exception Message: Test crash
                """.trimIndent()

            val crashFile = File(crashDir, "crash_20250108_120000.txt")
            crashFile.writeText(crashContent)

            val readContent = CrashHandler.readCrashReport(crashFile)

            assertThat(readContent).isEqualTo(crashContent)
            assertThat(readContent).contains("RuntimeException")
            assertThat(readContent).contains("Test crash")
        }

        @Test
        @DisplayName("should handle empty crash report")
        fun handleEmptyCrashReport() {
            val crashFile = File(crashDir, "crash_empty.txt")
            crashFile.writeText("")

            val content = CrashHandler.readCrashReport(crashFile)

            assertThat(content).isEmpty()
        }

        @Test
        @DisplayName("should handle crash report with special characters")
        fun handleSpecialCharacters() {
            val crashContent =
                """
                Exception: Test with special chars: <>&"'
                Unicode: 日本語 中文 한국어
                Emoji: 🔥💥🐛
                """.trimIndent()

            val crashFile = File(crashDir, "crash_special.txt")
            crashFile.writeText(crashContent)

            val readContent = CrashHandler.readCrashReport(crashFile)

            assertThat(readContent).contains("<>&\"'")
            assertThat(readContent).contains("日本語")
            assertThat(readContent).contains("🔥")
        }
    }

    @Nested
    @DisplayName("Crash Report Format")
    inner class CrashReportFormat {
        @Test
        @DisplayName("crash report should contain required sections")
        fun crashReportContainsRequiredSections() {
            // Simulate a crash report format
            val expectedSections =
                listOf(
                    "SoftEther Connect Crash Report",
                    "Timestamp:",
                    "App Information",
                    "Device Information",
                    "Thread Information",
                    "Exception",
                    "Stack Trace",
                )

            // Create a mock crash report
            val crashReport =
                buildString {
                    appendLine("=".repeat(60))
                    appendLine("SoftEther Connect Crash Report")
                    appendLine("=".repeat(60))
                    appendLine()
                    appendLine("Timestamp: 2025-01-08 12:00:00 +0100")
                    appendLine()
                    appendLine("-".repeat(60))
                    appendLine("App Information")
                    appendLine("-".repeat(60))
                    appendLine("Package: kittoku.mvc")
                    appendLine("Version: 1.0.0")
                    appendLine()
                    appendLine("-".repeat(60))
                    appendLine("Device Information")
                    appendLine("-".repeat(60))
                    appendLine("Manufacturer: Test")
                    appendLine("Model: TestDevice")
                    appendLine()
                    appendLine("-".repeat(60))
                    appendLine("Thread Information")
                    appendLine("-".repeat(60))
                    appendLine("Thread Name: main")
                    appendLine()
                    appendLine("-".repeat(60))
                    appendLine("Exception")
                    appendLine("-".repeat(60))
                    appendLine("Type: java.lang.RuntimeException")
                    appendLine("Message: Test exception")
                    appendLine()
                    appendLine("-".repeat(60))
                    appendLine("Stack Trace")
                    appendLine("-".repeat(60))
                    appendLine("java.lang.RuntimeException: Test exception")
                    appendLine("\tat TestClass.testMethod(TestClass.kt:10)")
                }

            val crashFile = File(crashDir, "crash_format_test.txt")
            crashFile.writeText(crashReport)

            val content = CrashHandler.readCrashReport(crashFile)

            expectedSections.forEach { section ->
                assertThat(content).contains(section)
            }
        }
    }

    @Nested
    @DisplayName("Crash File Naming")
    inner class CrashFileNaming {
        @Test
        @DisplayName("crash files should follow naming convention")
        fun crashFilesFollowNamingConvention() {
            // Create files with expected naming pattern
            val validNames =
                listOf(
                    "crash_20250108_120000.txt",
                    "crash_20250108_235959.txt",
                    "crash_20240101_000000.txt",
                )

            validNames.forEach { name ->
                val file = File(crashDir, name)
                file.writeText("Crash content")
                assertThat(file.exists()).isTrue()
                assertThat(name).matches("crash_\\d{8}_\\d{6}\\.txt")
            }
        }

        @Test
        @DisplayName("should sort crash files by modification time")
        fun sortCrashFilesByModificationTime() {
            // Create files with different timestamps
            val file1 = File(crashDir, "crash_20250108_100000.txt")
            val file2 = File(crashDir, "crash_20250108_110000.txt")
            val file3 = File(crashDir, "crash_20250108_120000.txt")

            file1.writeText("Crash 1")
            Thread.sleep(10)
            file2.writeText("Crash 2")
            Thread.sleep(10)
            file3.writeText("Crash 3")

            val sortedFiles =
                crashDir.listFiles()
                    ?.sortedByDescending { it.lastModified() }
                    ?: emptyList()

            assertThat(sortedFiles).hasSize(3)
            assertThat(sortedFiles[0].name).isEqualTo("crash_20250108_120000.txt")
            assertThat(sortedFiles[2].name).isEqualTo("crash_20250108_100000.txt")
        }
    }

    @Nested
    @DisplayName("Crash Report Cleanup")
    inner class CrashReportCleanup {
        @Test
        @DisplayName("should identify files for cleanup when exceeding limit")
        fun identifyFilesForCleanup() {
            val maxReports = 10

            // Create more files than the limit
            repeat(15) { i ->
                val file = File(crashDir, "crash_2025010${i}_120000.txt")
                file.writeText("Crash $i")
                Thread.sleep(5) // Ensure different modification times
            }

            val allFiles =
                crashDir.listFiles()?.sortedByDescending { it.lastModified() }
                    ?: emptyList()

            assertThat(allFiles).hasSize(15)

            // Files to keep (most recent)
            val filesToKeep = allFiles.take(maxReports)
            assertThat(filesToKeep).hasSize(10)

            // Files to delete (oldest)
            val filesToDelete = allFiles.drop(maxReports)
            assertThat(filesToDelete).hasSize(5)
        }
    }

    @Nested
    @DisplayName("Exception Formatting")
    inner class ExceptionFormatting {
        @Test
        @DisplayName("should format simple exception")
        fun formatSimpleException() {
            val exception = RuntimeException("Test error")
            val stackTrace = exception.stackTraceToString()

            assertThat(stackTrace).contains("RuntimeException")
            assertThat(stackTrace).contains("Test error")
            assertThat(stackTrace).contains("at ")
        }

        @Test
        @DisplayName("should format nested exception with cause")
        fun formatNestedException() {
            val cause = IllegalArgumentException("Root cause")
            val exception = RuntimeException("Wrapper exception", cause)
            val stackTrace = exception.stackTraceToString()

            assertThat(stackTrace).contains("RuntimeException")
            assertThat(stackTrace).contains("Wrapper exception")
            assertThat(stackTrace).contains("Caused by")
            assertThat(stackTrace).contains("IllegalArgumentException")
            assertThat(stackTrace).contains("Root cause")
        }

        @Test
        @DisplayName("should format deeply nested exception")
        fun formatDeeplyNestedException() {
            val level3 = NullPointerException("Null value")
            val level2 = IllegalStateException("Invalid state", level3)
            val level1 = RuntimeException("Operation failed", level2)
            val stackTrace = level1.stackTraceToString()

            assertThat(stackTrace).contains("RuntimeException")
            assertThat(stackTrace).contains("IllegalStateException")
            assertThat(stackTrace).contains("NullPointerException")
        }
    }
}
