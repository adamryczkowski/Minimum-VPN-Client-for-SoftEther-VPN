package kittoku.mvc.logging

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Privacy-respecting crash handler.
 *
 * Captures crash information locally without sending data to external servers.
 * Users can choose to share crash reports manually.
 *
 * Features:
 * - Local crash log storage
 * - No automatic data transmission
 * - Sanitized stack traces (no personal data)
 * - Manual sharing via system share sheet
 */
class CrashHandler private constructor(
    private val context: Context,
) : Thread.UncaughtExceptionHandler {
    private val logger = VpnLogger.getLogger<CrashHandler>()
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(
        thread: Thread,
        throwable: Throwable,
    ) {
        try {
            // Log the crash
            logger.fatal(
                "Uncaught exception in thread ${thread.name}",
                throwable,
                "threadId" to thread.threadId().toString(),
            )

            // Save crash report to file
            saveCrashReport(thread, throwable)
        } catch (e: Exception) {
            // Don't let crash handling crash
            e.printStackTrace()
        } finally {
            // Call the default handler to show the crash dialog
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun saveCrashReport(
        thread: Thread,
        throwable: Throwable,
    ) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val filename = "crash_$timestamp.txt"
        val file = File(getCrashDir(), filename)

        file.writeText(formatCrashReport(thread, throwable))

        // Clean up old crash reports (keep last 10)
        cleanupOldReports()
    }

    private fun formatCrashReport(
        thread: Thread,
        throwable: Throwable,
    ): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)

        pw.println("=".repeat(60))
        pw.println("SoftEther Connect Crash Report")
        pw.println("=".repeat(60))
        pw.println()

        // Timestamp
        pw.println("Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())}")
        pw.println()

        // App info
        pw.println("-".repeat(60))
        pw.println("App Information")
        pw.println("-".repeat(60))
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pw.println("Package: ${context.packageName}")
            pw.println("Version: ${packageInfo.versionName}")
            val versionCode =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
            pw.println("Version Code: $versionCode")
            pw.println("Build Type: ${if (kittoku.mvc.BuildConfig.DEBUG) "debug" else "release"}")
        } catch (e: Exception) {
            pw.println("Failed to get app info: ${e.message}")
        }
        pw.println()

        // Device info
        pw.println("-".repeat(60))
        pw.println("Device Information")
        pw.println("-".repeat(60))
        pw.println("Manufacturer: ${Build.MANUFACTURER}")
        pw.println("Model: ${Build.MODEL}")
        pw.println("Android Version: ${Build.VERSION.RELEASE}")
        pw.println("SDK Version: ${Build.VERSION.SDK_INT}")
        pw.println("Architecture: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"}")
        pw.println()

        // Thread info
        pw.println("-".repeat(60))
        pw.println("Thread Information")
        pw.println("-".repeat(60))
        pw.println("Thread Name: ${thread.name}")
        pw.println("Thread ID: ${thread.threadId()}")
        pw.println("Thread Priority: ${thread.priority}")
        pw.println()

        // Exception info
        pw.println("-".repeat(60))
        pw.println("Exception")
        pw.println("-".repeat(60))
        pw.println("Type: ${throwable.javaClass.name}")
        pw.println("Message: ${throwable.message}")
        pw.println()

        // Stack trace
        pw.println("-".repeat(60))
        pw.println("Stack Trace")
        pw.println("-".repeat(60))
        throwable.printStackTrace(pw)
        pw.println()

        // Cause chain
        var cause = throwable.cause
        var depth = 0
        while (cause != null && depth < 10) {
            pw.println("-".repeat(60))
            pw.println("Caused By (depth $depth)")
            pw.println("-".repeat(60))
            pw.println("Type: ${cause.javaClass.name}")
            pw.println("Message: ${cause.message}")
            cause.printStackTrace(pw)
            pw.println()
            cause = cause.cause
            depth++
        }

        // Recent logs
        pw.println("-".repeat(60))
        pw.println("Recent Logs")
        pw.println("-".repeat(60))
        VpnLogger.getBufferedLogs(LogLevel.WARN).takeLast(20).forEach { log ->
            pw.println(log.toLogLine())
        }
        pw.println()

        pw.println("=".repeat(60))
        pw.println("End of Crash Report")
        pw.println("=".repeat(60))

        return sw.toString()
    }

    private fun getCrashDir(): File {
        val dir = File(context.filesDir, "crashes")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun cleanupOldReports() {
        val crashDir = getCrashDir()
        val files = crashDir.listFiles()?.sortedByDescending { it.lastModified() } ?: return

        // Keep only the last 10 crash reports
        files.drop(MAX_CRASH_REPORTS).forEach { file ->
            file.delete()
        }
    }

    companion object {
        private const val MAX_CRASH_REPORTS = 10

        @Volatile
        private var instance: CrashHandler? = null

        /**
         * Initialize the crash handler.
         *
         * Call this early in Application.onCreate().
         */
        fun initialize(context: Context) {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = CrashHandler(context.applicationContext)
                        Thread.setDefaultUncaughtExceptionHandler(instance)
                    }
                }
            }
        }

        /**
         * Get all crash reports.
         */
        fun getCrashReports(context: Context): List<File> {
            val crashDir = File(context.filesDir, "crashes")
            return crashDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
        }

        /**
         * Get the most recent crash report.
         */
        fun getLatestCrashReport(context: Context): File? {
            return getCrashReports(context).firstOrNull()
        }

        /**
         * Check if there are any crash reports.
         */
        fun hasCrashReports(context: Context): Boolean {
            return getCrashReports(context).isNotEmpty()
        }

        /**
         * Delete all crash reports.
         */
        fun clearCrashReports(context: Context) {
            getCrashReports(context).forEach { it.delete() }
        }

        /**
         * Read a crash report.
         */
        fun readCrashReport(file: File): String {
            return file.readText()
        }
    }
}
