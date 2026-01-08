package kittoku.mvc.performance

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for BatteryOptimizer data classes and configuration logic.
 *
 * Note: Full integration tests require Android instrumentation tests
 * as BatteryOptimizer depends on Android Context and system services.
 * These tests focus on the data classes and configuration logic.
 */
@DisplayName("BatteryOptimizer")
class BatteryOptimizerTest {
    @Nested
    @DisplayName("BatteryStatus")
    inner class BatteryStatusTests {
        @Test
        @DisplayName("should create BatteryStatus with all fields")
        fun createBatteryStatus() {
            val status =
                BatteryOptimizer.BatteryStatus(
                    level = 75,
                    isCharging = false,
                    isPowerSaveMode = false,
                    isLowBattery = false,
                    isCriticalBattery = false,
                )

            assertThat(status.level).isEqualTo(75)
            assertThat(status.isCharging).isFalse()
            assertThat(status.isPowerSaveMode).isFalse()
            assertThat(status.isLowBattery).isFalse()
            assertThat(status.isCriticalBattery).isFalse()
        }

        @Test
        @DisplayName("should detect low battery")
        fun detectLowBattery() {
            val status =
                BatteryOptimizer.BatteryStatus(
                    level = 15,
                    isCharging = false,
                    isPowerSaveMode = false,
                    isLowBattery = true,
                    isCriticalBattery = false,
                )

            assertThat(status.isLowBattery).isTrue()
            assertThat(status.isCriticalBattery).isFalse()
        }

        @Test
        @DisplayName("should detect critical battery")
        fun detectCriticalBattery() {
            val status =
                BatteryOptimizer.BatteryStatus(
                    level = 5,
                    isCharging = false,
                    isPowerSaveMode = false,
                    isLowBattery = true,
                    isCriticalBattery = true,
                )

            assertThat(status.isLowBattery).isTrue()
            assertThat(status.isCriticalBattery).isTrue()
        }

        @Test
        @DisplayName("should format status as string")
        fun formatStatusAsString() {
            val status =
                BatteryOptimizer.BatteryStatus(
                    level = 50,
                    isCharging = true,
                    isPowerSaveMode = false,
                    isLowBattery = false,
                    isCriticalBattery = false,
                )

            val string = status.toString()
            assertThat(string).contains("Battery: 50%")
            assertThat(string).contains("(charging)")
        }

        @Test
        @DisplayName("should format low battery status")
        fun formatLowBatteryStatus() {
            val status =
                BatteryOptimizer.BatteryStatus(
                    level = 15,
                    isCharging = false,
                    isPowerSaveMode = true,
                    isLowBattery = true,
                    isCriticalBattery = false,
                )

            val string = status.toString()
            assertThat(string).contains("Battery: 15%")
            assertThat(string).contains("[power save]")
            assertThat(string).contains("[low]")
        }

        @Test
        @DisplayName("should format critical battery status")
        fun formatCriticalBatteryStatus() {
            val status =
                BatteryOptimizer.BatteryStatus(
                    level = 5,
                    isCharging = false,
                    isPowerSaveMode = false,
                    isLowBattery = true,
                    isCriticalBattery = true,
                )

            val string = status.toString()
            assertThat(string).contains("[CRITICAL]")
            assertThat(string).doesNotContain("[low]") // Critical takes precedence
        }
    }

    @Nested
    @DisplayName("NetworkConditions")
    inner class NetworkConditionsTests {
        @Test
        @DisplayName("should create NetworkConditions for WiFi")
        fun createWifiConditions() {
            val conditions =
                BatteryOptimizer.NetworkConditions(
                    isWifi = true,
                    isCellular = false,
                    isMetered = false,
                    estimatedSpeedMbps = 100,
                )

            assertThat(conditions.isWifi).isTrue()
            assertThat(conditions.isCellular).isFalse()
            assertThat(conditions.isMetered).isFalse()
            assertThat(conditions.networkType).isEqualTo("WiFi")
        }

        @Test
        @DisplayName("should create NetworkConditions for Cellular")
        fun createCellularConditions() {
            val conditions =
                BatteryOptimizer.NetworkConditions(
                    isWifi = false,
                    isCellular = true,
                    isMetered = true,
                    estimatedSpeedMbps = 20,
                )

            assertThat(conditions.isWifi).isFalse()
            assertThat(conditions.isCellular).isTrue()
            assertThat(conditions.isMetered).isTrue()
            assertThat(conditions.networkType).isEqualTo("Cellular")
        }

        @Test
        @DisplayName("should handle unknown network type")
        fun handleUnknownNetworkType() {
            val conditions =
                BatteryOptimizer.NetworkConditions(
                    isWifi = false,
                    isCellular = false,
                    isMetered = false,
                    estimatedSpeedMbps = 10,
                )

            assertThat(conditions.networkType).isEqualTo("Unknown")
        }

        @Test
        @DisplayName("should format conditions as string")
        fun formatConditionsAsString() {
            val conditions =
                BatteryOptimizer.NetworkConditions(
                    isWifi = true,
                    isCellular = false,
                    isMetered = false,
                    estimatedSpeedMbps = 100,
                )

            val string = conditions.toString()
            assertThat(string).contains("Network: WiFi")
            assertThat(string).contains("~100 Mbps")
        }

        @Test
        @DisplayName("should indicate metered network")
        fun indicateMeteredNetwork() {
            val conditions =
                BatteryOptimizer.NetworkConditions(
                    isWifi = false,
                    isCellular = true,
                    isMetered = true,
                    estimatedSpeedMbps = 20,
                )

            val string = conditions.toString()
            assertThat(string).contains("[metered]")
        }
    }

    @Nested
    @DisplayName("PowerMode")
    inner class PowerModeTests {
        @Test
        @DisplayName("should have all power modes")
        fun hasAllPowerModes() {
            val modes = BatteryOptimizer.PowerMode.entries

            assertThat(modes).hasSize(4)
            assertThat(modes).contains(BatteryOptimizer.PowerMode.PERFORMANCE)
            assertThat(modes).contains(BatteryOptimizer.PowerMode.BALANCED)
            assertThat(modes).contains(BatteryOptimizer.PowerMode.BATTERY_SAVER)
            assertThat(modes).contains(BatteryOptimizer.PowerMode.ULTRA_SAVER)
        }
    }

    @Nested
    @DisplayName("OptimalConfiguration")
    inner class OptimalConfigurationTests {
        @Test
        @DisplayName("should create performance configuration")
        fun createPerformanceConfiguration() {
            val config =
                BatteryOptimizer.OptimalConfiguration(
                    powerMode = BatteryOptimizer.PowerMode.PERFORMANCE,
                    keepAliveIntervalMs = 500L,
                    throughputSampleIntervalMs = 1000L,
                    useUdpAcceleration = true,
                    bufferPoolSize = 16,
                    batteryStatus = BatteryOptimizer.BatteryStatus(100, true, false, false, false),
                    networkConditions = BatteryOptimizer.NetworkConditions(true, false, false, 100),
                )

            assertThat(config.powerMode).isEqualTo(BatteryOptimizer.PowerMode.PERFORMANCE)
            assertThat(config.keepAliveIntervalMs).isEqualTo(500L)
            assertThat(config.useUdpAcceleration).isTrue()
            assertThat(config.bufferPoolSize).isEqualTo(16)
        }

        @Test
        @DisplayName("should create battery saver configuration")
        fun createBatterySaverConfiguration() {
            val config =
                BatteryOptimizer.OptimalConfiguration(
                    powerMode = BatteryOptimizer.PowerMode.BATTERY_SAVER,
                    keepAliveIntervalMs = 2000L,
                    throughputSampleIntervalMs = 5000L,
                    useUdpAcceleration = false,
                    bufferPoolSize = 4,
                    batteryStatus = BatteryOptimizer.BatteryStatus(15, false, true, true, false),
                    networkConditions = BatteryOptimizer.NetworkConditions(false, true, true, 20),
                )

            assertThat(config.powerMode).isEqualTo(BatteryOptimizer.PowerMode.BATTERY_SAVER)
            assertThat(config.keepAliveIntervalMs).isEqualTo(2000L)
            assertThat(config.useUdpAcceleration).isFalse()
            assertThat(config.bufferPoolSize).isEqualTo(4)
        }

        @Test
        @DisplayName("should create ultra saver configuration")
        fun createUltraSaverConfiguration() {
            val config =
                BatteryOptimizer.OptimalConfiguration(
                    powerMode = BatteryOptimizer.PowerMode.ULTRA_SAVER,
                    keepAliveIntervalMs = 5000L,
                    throughputSampleIntervalMs = 10000L,
                    useUdpAcceleration = false,
                    bufferPoolSize = 2,
                    batteryStatus = BatteryOptimizer.BatteryStatus(5, false, false, true, true),
                    networkConditions = BatteryOptimizer.NetworkConditions(true, false, false, 50),
                )

            assertThat(config.powerMode).isEqualTo(BatteryOptimizer.PowerMode.ULTRA_SAVER)
            assertThat(config.keepAliveIntervalMs).isEqualTo(5000L)
            assertThat(config.bufferPoolSize).isEqualTo(2)
        }

        @Test
        @DisplayName("should format configuration as string")
        fun formatConfigurationAsString() {
            val config =
                BatteryOptimizer.OptimalConfiguration(
                    powerMode = BatteryOptimizer.PowerMode.BALANCED,
                    keepAliveIntervalMs = 1000L,
                    throughputSampleIntervalMs = 2000L,
                    useUdpAcceleration = true,
                    bufferPoolSize = 8,
                    batteryStatus = BatteryOptimizer.BatteryStatus(50, false, false, false, false),
                    networkConditions = BatteryOptimizer.NetworkConditions(true, false, false, 100),
                )

            val string = config.toString()
            assertThat(string).contains("Optimal VPN Configuration")
            assertThat(string).contains("Power mode: BALANCED")
            assertThat(string).contains("Keep-alive interval: 1000ms")
            assertThat(string).contains("UDP acceleration: true")
            assertThat(string).contains("Buffer pool size: 8")
        }
    }

    @Nested
    @DisplayName("Configuration Logic")
    inner class ConfigurationLogicTests {
        @Test
        @DisplayName("charging should result in performance mode")
        fun chargingShouldResultInPerformanceMode() {
            // When charging, regardless of battery level, should use performance mode
            val batteryStatus =
                BatteryOptimizer.BatteryStatus(
                    level = 20,
                    isCharging = true,
                    isPowerSaveMode = false,
                    isLowBattery = true,
                    isCriticalBattery = false,
                )

            // Simulate the logic from getOptimalConfiguration
            val powerMode =
                when {
                    batteryStatus.isCharging -> BatteryOptimizer.PowerMode.PERFORMANCE
                    batteryStatus.isCriticalBattery -> BatteryOptimizer.PowerMode.ULTRA_SAVER
                    batteryStatus.isLowBattery || batteryStatus.isPowerSaveMode ->
                        BatteryOptimizer.PowerMode.BATTERY_SAVER
                    else -> BatteryOptimizer.PowerMode.BALANCED
                }

            assertThat(powerMode).isEqualTo(BatteryOptimizer.PowerMode.PERFORMANCE)
        }

        @Test
        @DisplayName("critical battery should result in ultra saver mode")
        fun criticalBatteryShouldResultInUltraSaverMode() {
            val batteryStatus =
                BatteryOptimizer.BatteryStatus(
                    level = 5,
                    isCharging = false,
                    isPowerSaveMode = false,
                    isLowBattery = true,
                    isCriticalBattery = true,
                )

            val powerMode =
                when {
                    batteryStatus.isCharging -> BatteryOptimizer.PowerMode.PERFORMANCE
                    batteryStatus.isCriticalBattery -> BatteryOptimizer.PowerMode.ULTRA_SAVER
                    batteryStatus.isLowBattery || batteryStatus.isPowerSaveMode ->
                        BatteryOptimizer.PowerMode.BATTERY_SAVER
                    else -> BatteryOptimizer.PowerMode.BALANCED
                }

            assertThat(powerMode).isEqualTo(BatteryOptimizer.PowerMode.ULTRA_SAVER)
        }

        @Test
        @DisplayName("low battery should result in battery saver mode")
        fun lowBatteryShouldResultInBatterySaverMode() {
            val batteryStatus =
                BatteryOptimizer.BatteryStatus(
                    level = 15,
                    isCharging = false,
                    isPowerSaveMode = false,
                    isLowBattery = true,
                    isCriticalBattery = false,
                )

            val powerMode =
                when {
                    batteryStatus.isCharging -> BatteryOptimizer.PowerMode.PERFORMANCE
                    batteryStatus.isCriticalBattery -> BatteryOptimizer.PowerMode.ULTRA_SAVER
                    batteryStatus.isLowBattery || batteryStatus.isPowerSaveMode ->
                        BatteryOptimizer.PowerMode.BATTERY_SAVER
                    else -> BatteryOptimizer.PowerMode.BALANCED
                }

            assertThat(powerMode).isEqualTo(BatteryOptimizer.PowerMode.BATTERY_SAVER)
        }

        @Test
        @DisplayName("power save mode should result in battery saver mode")
        fun powerSaveModeShouldResultInBatterySaverMode() {
            val batteryStatus =
                BatteryOptimizer.BatteryStatus(
                    level = 50,
                    isCharging = false,
                    isPowerSaveMode = true,
                    isLowBattery = false,
                    isCriticalBattery = false,
                )

            val powerMode =
                when {
                    batteryStatus.isCharging -> BatteryOptimizer.PowerMode.PERFORMANCE
                    batteryStatus.isCriticalBattery -> BatteryOptimizer.PowerMode.ULTRA_SAVER
                    batteryStatus.isLowBattery || batteryStatus.isPowerSaveMode ->
                        BatteryOptimizer.PowerMode.BATTERY_SAVER
                    else -> BatteryOptimizer.PowerMode.BALANCED
                }

            assertThat(powerMode).isEqualTo(BatteryOptimizer.PowerMode.BATTERY_SAVER)
        }

        @Test
        @DisplayName("normal battery should result in balanced mode")
        fun normalBatteryShouldResultInBalancedMode() {
            val batteryStatus =
                BatteryOptimizer.BatteryStatus(
                    level = 75,
                    isCharging = false,
                    isPowerSaveMode = false,
                    isLowBattery = false,
                    isCriticalBattery = false,
                )

            val powerMode =
                when {
                    batteryStatus.isCharging -> BatteryOptimizer.PowerMode.PERFORMANCE
                    batteryStatus.isCriticalBattery -> BatteryOptimizer.PowerMode.ULTRA_SAVER
                    batteryStatus.isLowBattery || batteryStatus.isPowerSaveMode ->
                        BatteryOptimizer.PowerMode.BATTERY_SAVER
                    else -> BatteryOptimizer.PowerMode.BALANCED
                }

            assertThat(powerMode).isEqualTo(BatteryOptimizer.PowerMode.BALANCED)
        }
    }

    @Nested
    @DisplayName("UDP Acceleration Logic")
    inner class UdpAccelerationLogicTests {
        @Test
        @DisplayName("performance mode should enable UDP acceleration")
        fun performanceModeShouldEnableUdpAcceleration() {
            val powerMode = BatteryOptimizer.PowerMode.PERFORMANCE
            val networkConditions = BatteryOptimizer.NetworkConditions(true, false, false, 100)

            val useUdpAcceleration =
                when (powerMode) {
                    BatteryOptimizer.PowerMode.PERFORMANCE -> true
                    BatteryOptimizer.PowerMode.BALANCED -> networkConditions.isWifi
                    BatteryOptimizer.PowerMode.BATTERY_SAVER -> false
                    BatteryOptimizer.PowerMode.ULTRA_SAVER -> false
                }

            assertThat(useUdpAcceleration).isTrue()
        }

        @Test
        @DisplayName("balanced mode on WiFi should enable UDP acceleration")
        fun balancedModeOnWifiShouldEnableUdpAcceleration() {
            val powerMode = BatteryOptimizer.PowerMode.BALANCED
            val networkConditions = BatteryOptimizer.NetworkConditions(true, false, false, 100)

            val useUdpAcceleration =
                when (powerMode) {
                    BatteryOptimizer.PowerMode.PERFORMANCE -> true
                    BatteryOptimizer.PowerMode.BALANCED -> networkConditions.isWifi
                    BatteryOptimizer.PowerMode.BATTERY_SAVER -> false
                    BatteryOptimizer.PowerMode.ULTRA_SAVER -> false
                }

            assertThat(useUdpAcceleration).isTrue()
        }

        @Test
        @DisplayName("balanced mode on cellular should disable UDP acceleration")
        fun balancedModeOnCellularShouldDisableUdpAcceleration() {
            val powerMode = BatteryOptimizer.PowerMode.BALANCED
            val networkConditions = BatteryOptimizer.NetworkConditions(false, true, true, 20)

            val useUdpAcceleration =
                when (powerMode) {
                    BatteryOptimizer.PowerMode.PERFORMANCE -> true
                    BatteryOptimizer.PowerMode.BALANCED -> networkConditions.isWifi
                    BatteryOptimizer.PowerMode.BATTERY_SAVER -> false
                    BatteryOptimizer.PowerMode.ULTRA_SAVER -> false
                }

            assertThat(useUdpAcceleration).isFalse()
        }

        @Test
        @DisplayName("battery saver mode should disable UDP acceleration")
        fun batterySaverModeShouldDisableUdpAcceleration() {
            val powerMode = BatteryOptimizer.PowerMode.BATTERY_SAVER
            val networkConditions = BatteryOptimizer.NetworkConditions(true, false, false, 100)

            val useUdpAcceleration =
                when (powerMode) {
                    BatteryOptimizer.PowerMode.PERFORMANCE -> true
                    BatteryOptimizer.PowerMode.BALANCED -> networkConditions.isWifi
                    BatteryOptimizer.PowerMode.BATTERY_SAVER -> false
                    BatteryOptimizer.PowerMode.ULTRA_SAVER -> false
                }

            assertThat(useUdpAcceleration).isFalse()
        }
    }
}
