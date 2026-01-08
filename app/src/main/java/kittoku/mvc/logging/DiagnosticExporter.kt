package kittoku.mvc.logging

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.FileProvider
import kittoku.mvc.performance.PerformanceMonitor
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Exports diagnostic information for troubleshooting.
 *
 * Collects and exports:
 * - Log events
 * - Device information
 * - App configuration (sanitized)
 * - Performance metrics
 * - Connection history
 *
 * All sensitive information (passwords, certificates) is excluded.
 */
class DiagnosticExporter(
    private val context: Context,
) {
    private val logger = VpnLogger.getLogger<DiagnosticExporter>()

    private val json =
        Json {
            prettyPrint = true
            encodeDefaults = true
        }

    /**
     * Generate a diagnostic report.
     */
    fun generateReport(): DiagnosticReport {
        logger.info("Generating diagnostic report")

        val timestamp =
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date())

        return DiagnosticReport(
            generatedAt = timestamp,
            appInfo = collectAppInfo(),
            deviceInfo = collectDeviceInfo(),
            logs = collectLogs(),
            performanceMetrics = collectPerformanceMetrics(),
            configuration = collectConfiguration(),
        )
    }

    /**
     * Export diagnostic report to a file.
     */
    fun exportToFile(report: DiagnosticReport = generateReport()): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "softether_diagnostic_$timestamp.json"

        val file = File(context.cacheDir, filename)
        file.writeText(json.encodeToString(report))

        logger.info("Diagnostic report exported", "file" to file.absolutePath)
        return file
    }

    /**
     * Export diagnostic report as text.
     */
    fun exportAsText(report: DiagnosticReport = generateReport()): String {
        return buildString {
            appendLine("=".repeat(60))
            appendLine("SoftEther Connect Diagnostic Report")
            appendLine("=".repeat(60))
            appendLine()

            appendLine("Generated: ${report.generatedAt}")
            appendLine()

            appendLine("-".repeat(60))
            appendLine("App Information")
            appendLine("-".repeat(60))
            appendLine("Version: ${report.appInfo.versionName} (${report.appInfo.versionCode})")
            appendLine("Package: ${report.appInfo.packageName}")
            appendLine("Build Type: ${report.appInfo.buildType}")
            appendLine()

            appendLine("-".repeat(60))
            appendLine("Device Information")
            appendLine("-".repeat(60))
            appendLine("Manufacturer: ${report.deviceInfo.manufacturer}")
            appendLine("Model: ${report.deviceInfo.model}")
            appendLine("Android: ${report.deviceInfo.androidVersion} (API ${report.deviceInfo.sdkVersion})")
            appendLine("Architecture: ${report.deviceInfo.architecture}")
            appendLine("Timezone: ${report.deviceInfo.timezone}")
            appendLine()

            appendLine("-".repeat(60))
            appendLine("Configuration")
            appendLine("-".repeat(60))
            report.configuration.forEach { (key, value) ->
                appendLine("$key: $value")
            }
            appendLine()

            appendLine("-".repeat(60))
            appendLine("Performance Metrics")
            appendLine("-".repeat(60))
            report.performanceMetrics.forEach { (key, value) ->
                appendLine("$key: $value")
            }
            appendLine()

            appendLine("-".repeat(60))
            appendLine("Recent Logs (${report.logs.size} entries)")
            appendLine("-".repeat(60))
            report.logs.takeLast(100).forEach { log ->
                appendLine(log.toLogLine())
            }
            appendLine()

            appendLine("=".repeat(60))
            appendLine("End of Report")
            appendLine("=".repeat(60))
        }
    }

    /**
     * Create a share intent for the diagnostic report.
     */
    fun createShareIntent(file: File): Intent {
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SoftEther Connect Diagnostic Report")
            putExtra(
                Intent.EXTRA_TEXT,
                "Please find attached the diagnostic report for troubleshooting.",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun collectAppInfo(): AppInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return AppInfo(
            packageName = context.packageName,
            versionName = packageInfo.versionName ?: "unknown",
            versionCode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
            buildType = if (kittoku.mvc.BuildConfig.DEBUG) "debug" else "release",
        )
    }

    private fun collectDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            sdkVersion = Build.VERSION.SDK_INT,
            architecture = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown",
            timezone = TimeZone.getDefault().id,
        )
    }

    private fun collectLogs(): List<LogEvent> {
        return VpnLogger.getBufferedLogs()
    }

    private fun collectPerformanceMetrics(): Map<String, String> {
        return try {
            val metrics = PerformanceMonitor.getInstance().getMetrics()
            mapOf(
                "uptime" to metrics.uptimeFormatted,
                "packetsSent" to metrics.packetsSent.toString(),
                "packetsReceived" to metrics.packetsReceived.toString(),
                "bytesSent" to metrics.bytesSentFormatted,
                "bytesReceived" to metrics.bytesReceivedFormatted,
                "packetsDropped" to metrics.packetsDropped.toString(),
                "errors" to metrics.errorsCount.toString(),
                "packetLossRate" to String.format("%.2f%%", metrics.packetLossRate * 100),
                "averageLatencyMs" to String.format("%.1f", metrics.averageLatencyMs),
            )
        } catch (e: Exception) {
            logger.warn("Failed to collect performance metrics", e)
            mapOf("error" to "Failed to collect metrics")
        }
    }

    private fun collectConfiguration(): Map<String, String> {
        // Collect non-sensitive configuration
        // Passwords, certificates, and other sensitive data are excluded
        return mapOf(
            "minSdk" to Build.VERSION_CODES.O.toString(),
            "targetSdk" to "36",
            // Add other non-sensitive configuration here
        )
    }

    /**
     * Export buffered logs as JSON array.
     *
     * @param pretty Whether to format with indentation
     * @param minLevel Optional minimum log level filter
     * @return JSON string containing log events
     */
    fun exportLogsAsJson(
        pretty: Boolean = false,
        minLevel: LogLevel? = null,
    ): String {
        val logs =
            if (minLevel != null) {
                VpnLogger.getBufferedLogs(minLevel)
            } else {
                VpnLogger.getBufferedLogs()
            }
        return if (pretty) {
            "[\n${logs.joinToString(",\n") { it.toJson(true) }}\n]"
        } else {
            "[${logs.joinToString(",") { it.toJson(false) }}]"
        }
    }

    /**
     * Export buffered logs as human-readable text.
     *
     * @param minLevel Optional minimum log level filter
     * @return Text string with one log event per line
     */
    fun exportLogsAsText(minLevel: LogLevel? = null): String {
        val logs =
            if (minLevel != null) {
                VpnLogger.getBufferedLogs(minLevel)
            } else {
                VpnLogger.getBufferedLogs()
            }
        return logs.joinToString("\n") { it.toLogLine() }
    }

    companion object {
        private const val MAX_LOG_ENTRIES = 500
    }
}

/**
 * Complete diagnostic report.
 */
@Serializable
data class DiagnosticReport(
    val generatedAt: String,
    val appInfo: AppInfo,
    val deviceInfo: DeviceInfo,
    val logs: List<LogEvent>,
    val performanceMetrics: Map<String, String>,
    val configuration: Map<String, String>,
)

/**
 * App information for diagnostics.
 */
@Serializable
data class AppInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val buildType: String,
)

/**
 * Device information for diagnostics.
 */
@Serializable
data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkVersion: Int,
    val architecture: String,
    val timezone: String,
)
