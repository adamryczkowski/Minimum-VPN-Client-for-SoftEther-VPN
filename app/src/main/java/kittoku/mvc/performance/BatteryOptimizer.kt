package kittoku.mvc.performance

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.getSystemService

/**
 * Battery optimization utilities for VPN connections.
 *
 * Provides strategies to reduce battery consumption while maintaining
 * VPN connection quality. Adapts behavior based on battery level,
 * charging status, and network conditions.
 *
 * Usage:
 * ```kotlin
 * val optimizer = BatteryOptimizer(context)
 * val config = optimizer.getOptimalConfiguration()
 *
 * // Apply configuration
 * keepAliveInterval = config.keepAliveIntervalMs
 * ```
 */
class BatteryOptimizer(private val context: Context) {
    private val powerManager: PowerManager? = context.getSystemService()
    private val batteryManager: BatteryManager? = context.getSystemService()
    private val connectivityManager: ConnectivityManager? = context.getSystemService()

    /**
     * Get current battery status.
     */
    fun getBatteryStatus(): BatteryStatus {
        val level = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        val isCharging = batteryManager?.isCharging ?: false
        val isPowerSaveMode = powerManager?.isPowerSaveMode ?: false

        return BatteryStatus(
            level = level,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode,
            isLowBattery = level <= LOW_BATTERY_THRESHOLD,
            isCriticalBattery = level <= CRITICAL_BATTERY_THRESHOLD,
        )
    }

    /**
     * Get current network conditions.
     */
    fun getNetworkConditions(): NetworkConditions {
        val network = connectivityManager?.activeNetwork
        val capabilities = network?.let { connectivityManager?.getNetworkCapabilities(it) }

        val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
        val isMetered = connectivityManager?.isActiveNetworkMetered ?: false

        // Estimate link speed (simplified)
        val linkSpeedMbps =
            when {
                isWifi -> capabilities?.linkDownstreamBandwidthKbps?.div(1000) ?: 100
                isCellular -> capabilities?.linkDownstreamBandwidthKbps?.div(1000) ?: 20
                else -> 10
            }

        return NetworkConditions(
            isWifi = isWifi,
            isCellular = isCellular,
            isMetered = isMetered,
            estimatedSpeedMbps = linkSpeedMbps,
        )
    }

    /**
     * Get optimal VPN configuration based on current conditions.
     *
     * Balances battery life with connection quality.
     */
    fun getOptimalConfiguration(): OptimalConfiguration {
        val battery = getBatteryStatus()
        val network = getNetworkConditions()

        // Determine power mode
        val powerMode =
            when {
                battery.isCharging -> PowerMode.PERFORMANCE
                battery.isCriticalBattery -> PowerMode.ULTRA_SAVER
                battery.isLowBattery || battery.isPowerSaveMode -> PowerMode.BATTERY_SAVER
                network.isCellular -> PowerMode.BALANCED
                else -> PowerMode.PERFORMANCE
            }

        // Calculate intervals based on power mode
        val keepAliveIntervalMs =
            when (powerMode) {
                PowerMode.PERFORMANCE -> KEEP_ALIVE_INTERVAL_PERFORMANCE
                PowerMode.BALANCED -> KEEP_ALIVE_INTERVAL_BALANCED
                PowerMode.BATTERY_SAVER -> KEEP_ALIVE_INTERVAL_BATTERY_SAVER
                PowerMode.ULTRA_SAVER -> KEEP_ALIVE_INTERVAL_ULTRA_SAVER
            }

        val throughputSampleIntervalMs =
            when (powerMode) {
                PowerMode.PERFORMANCE -> THROUGHPUT_SAMPLE_INTERVAL_PERFORMANCE
                PowerMode.BALANCED -> THROUGHPUT_SAMPLE_INTERVAL_BALANCED
                PowerMode.BATTERY_SAVER -> THROUGHPUT_SAMPLE_INTERVAL_BATTERY_SAVER
                PowerMode.ULTRA_SAVER -> THROUGHPUT_SAMPLE_INTERVAL_ULTRA_SAVER
            }

        // Determine if UDP acceleration should be used
        val useUdpAcceleration =
            when (powerMode) {
                PowerMode.PERFORMANCE -> true
                PowerMode.BALANCED -> network.isWifi
                PowerMode.BATTERY_SAVER -> false
                PowerMode.ULTRA_SAVER -> false
            }

        // Buffer pool size based on power mode
        val bufferPoolSize =
            when (powerMode) {
                PowerMode.PERFORMANCE -> 16
                PowerMode.BALANCED -> 8
                PowerMode.BATTERY_SAVER -> 4
                PowerMode.ULTRA_SAVER -> 2
            }

        return OptimalConfiguration(
            powerMode = powerMode,
            keepAliveIntervalMs = keepAliveIntervalMs,
            throughputSampleIntervalMs = throughputSampleIntervalMs,
            useUdpAcceleration = useUdpAcceleration,
            bufferPoolSize = bufferPoolSize,
            batteryStatus = battery,
            networkConditions = network,
        )
    }

    /**
     * Check if the app is exempt from battery optimization.
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: true
    }

    /**
     * Create an intent to request battery optimization exemption.
     *
     * Note: The user must approve this request.
     */
    fun createBatteryOptimizationExemptionIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }

    /**
     * Battery status information.
     */
    data class BatteryStatus(
        val level: Int,
        val isCharging: Boolean,
        val isPowerSaveMode: Boolean,
        val isLowBattery: Boolean,
        val isCriticalBattery: Boolean,
    ) {
        override fun toString(): String {
            return buildString {
                append("Battery: $level%")
                if (isCharging) append(" (charging)")
                if (isPowerSaveMode) append(" [power save]")
                if (isCriticalBattery) {
                    append(" [CRITICAL]")
                } else if (isLowBattery) {
                    append(" [low]")
                }
            }
        }
    }

    /**
     * Network conditions information.
     */
    data class NetworkConditions(
        val isWifi: Boolean,
        val isCellular: Boolean,
        val isMetered: Boolean,
        val estimatedSpeedMbps: Int,
    ) {
        val networkType: String
            get() =
                when {
                    isWifi -> "WiFi"
                    isCellular -> "Cellular"
                    else -> "Unknown"
                }

        override fun toString(): String {
            return buildString {
                append("Network: $networkType")
                append(" (~$estimatedSpeedMbps Mbps)")
                if (isMetered) append(" [metered]")
            }
        }
    }

    /**
     * Power mode for VPN operation.
     */
    enum class PowerMode {
        /** Maximum performance, higher battery usage */
        PERFORMANCE,

        /** Balance between performance and battery */
        BALANCED,

        /** Prioritize battery life */
        BATTERY_SAVER,

        /** Minimal battery usage, may affect connection quality */
        ULTRA_SAVER,
    }

    /**
     * Optimal VPN configuration based on current conditions.
     */
    data class OptimalConfiguration(
        val powerMode: PowerMode,
        val keepAliveIntervalMs: Long,
        val throughputSampleIntervalMs: Long,
        val useUdpAcceleration: Boolean,
        val bufferPoolSize: Int,
        val batteryStatus: BatteryStatus,
        val networkConditions: NetworkConditions,
    ) {
        override fun toString(): String {
            return buildString {
                appendLine("Optimal VPN Configuration:")
                appendLine("  Power mode: $powerMode")
                appendLine("  Keep-alive interval: ${keepAliveIntervalMs}ms")
                appendLine("  Throughput sample interval: ${throughputSampleIntervalMs}ms")
                appendLine("  UDP acceleration: $useUdpAcceleration")
                appendLine("  Buffer pool size: $bufferPoolSize")
                appendLine("  $batteryStatus")
                appendLine("  $networkConditions")
            }
        }
    }

    companion object {
        // Battery thresholds
        private const val LOW_BATTERY_THRESHOLD = 20
        private const val CRITICAL_BATTERY_THRESHOLD = 10

        // Keep-alive intervals (milliseconds)
        private const val KEEP_ALIVE_INTERVAL_PERFORMANCE = 500L
        private const val KEEP_ALIVE_INTERVAL_BALANCED = 1000L
        private const val KEEP_ALIVE_INTERVAL_BATTERY_SAVER = 2000L
        private const val KEEP_ALIVE_INTERVAL_ULTRA_SAVER = 5000L

        // Throughput sample intervals (milliseconds)
        private const val THROUGHPUT_SAMPLE_INTERVAL_PERFORMANCE = 1000L
        private const val THROUGHPUT_SAMPLE_INTERVAL_BALANCED = 2000L
        private const val THROUGHPUT_SAMPLE_INTERVAL_BATTERY_SAVER = 5000L
        private const val THROUGHPUT_SAMPLE_INTERVAL_ULTRA_SAVER = 10000L
    }
}
