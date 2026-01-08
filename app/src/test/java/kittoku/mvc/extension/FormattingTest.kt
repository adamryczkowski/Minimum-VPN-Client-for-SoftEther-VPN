package kittoku.mvc.extension

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for the Formatting utility object.
 *
 * Tests cover all formatting functions with various edge cases
 * including boundary values, typical values, and error conditions.
 */
@DisplayName("Formatting")
class FormattingTest {
    @Nested
    @DisplayName("formatBytes (binary units)")
    inner class FormatBytesTest {
        @Test
        @DisplayName("should format 0 bytes")
        fun shouldFormatZeroBytes() {
            assertThat(Formatting.formatBytes(0)).isEqualTo("0 B")
        }

        @Test
        @DisplayName("should format bytes under 1 KB")
        fun shouldFormatBytesUnder1KB() {
            assertThat(Formatting.formatBytes(1)).isEqualTo("1 B")
            assertThat(Formatting.formatBytes(512)).isEqualTo("512 B")
            assertThat(Formatting.formatBytes(1023)).isEqualTo("1023 B")
        }

        @Test
        @DisplayName("should format exactly 1 KB")
        fun shouldFormatExactly1KB() {
            assertThat(Formatting.formatBytes(1024)).isEqualTo("1.0 KB")
        }

        @Test
        @DisplayName("should format kilobytes")
        fun shouldFormatKilobytes() {
            assertThat(Formatting.formatBytes(1536)).isEqualTo("1.5 KB")
            assertThat(Formatting.formatBytes(10240)).isEqualTo("10.0 KB")
            assertThat(Formatting.formatBytes(1048575)).isEqualTo("1024.0 KB")
        }

        @Test
        @DisplayName("should format exactly 1 MB")
        fun shouldFormatExactly1MB() {
            assertThat(Formatting.formatBytes(1048576)).isEqualTo("1.0 MB")
        }

        @Test
        @DisplayName("should format megabytes")
        fun shouldFormatMegabytes() {
            assertThat(Formatting.formatBytes(1572864)).isEqualTo("1.5 MB")
            assertThat(Formatting.formatBytes(104857600)).isEqualTo("100.0 MB")
        }

        @Test
        @DisplayName("should format exactly 1 GB")
        fun shouldFormatExactly1GB() {
            assertThat(Formatting.formatBytes(1073741824)).isEqualTo("1.0 GB")
        }

        @Test
        @DisplayName("should format gigabytes")
        fun shouldFormatGigabytes() {
            assertThat(Formatting.formatBytes(1610612736)).isEqualTo("1.5 GB")
            assertThat(Formatting.formatBytes(10737418240)).isEqualTo("10.0 GB")
        }

        @Test
        @DisplayName("should throw for negative bytes")
        fun shouldThrowForNegativeBytes() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    Formatting.formatBytes(-1)
                }
            assertThat(exception.message).contains("non-negative")
        }
    }

    @Nested
    @DisplayName("formatBytesDecimal (decimal units)")
    inner class FormatBytesDecimalTest {
        @Test
        @DisplayName("should format 0 bytes")
        fun shouldFormatZeroBytes() {
            assertThat(Formatting.formatBytesDecimal(0)).isEqualTo("0 B")
        }

        @Test
        @DisplayName("should format bytes under 1 KB")
        fun shouldFormatBytesUnder1KB() {
            assertThat(Formatting.formatBytesDecimal(999)).isEqualTo("999 B")
        }

        @Test
        @DisplayName("should format exactly 1 KB (1000 bytes)")
        fun shouldFormatExactly1KB() {
            assertThat(Formatting.formatBytesDecimal(1000)).isEqualTo("1.00 KB")
        }

        @Test
        @DisplayName("should format kilobytes with 2 decimal places by default")
        fun shouldFormatKilobytesWithDefaultPrecision() {
            assertThat(Formatting.formatBytesDecimal(1500)).isEqualTo("1.50 KB")
        }

        @Test
        @DisplayName("should format megabytes")
        fun shouldFormatMegabytes() {
            assertThat(Formatting.formatBytesDecimal(1000000)).isEqualTo("1.00 MB")
            assertThat(Formatting.formatBytesDecimal(1500000)).isEqualTo("1.50 MB")
        }

        @Test
        @DisplayName("should format gigabytes")
        fun shouldFormatGigabytes() {
            assertThat(Formatting.formatBytesDecimal(1000000000)).isEqualTo("1.00 GB")
            assertThat(Formatting.formatBytesDecimal(1500000000)).isEqualTo("1.50 GB")
        }

        @Test
        @DisplayName("should respect custom decimal places")
        fun shouldRespectCustomDecimalPlaces() {
            assertThat(Formatting.formatBytesDecimal(1500, decimalPlaces = 1)).isEqualTo("1.5 KB")
            assertThat(Formatting.formatBytesDecimal(1500, decimalPlaces = 3)).isEqualTo("1.500 KB")
        }

        @Test
        @DisplayName("should throw for negative bytes")
        fun shouldThrowForNegativeBytes() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    Formatting.formatBytesDecimal(-1)
                }
            assertThat(exception.message).contains("non-negative")
        }
    }

    @Nested
    @DisplayName("formatDuration (HH:MM:SS)")
    inner class FormatDurationTest {
        @Test
        @DisplayName("should format 0 milliseconds")
        fun shouldFormatZeroMilliseconds() {
            assertThat(Formatting.formatDuration(0)).isEqualTo("00:00:00")
        }

        @Test
        @DisplayName("should format seconds only")
        fun shouldFormatSecondsOnly() {
            assertThat(Formatting.formatDuration(1000)).isEqualTo("00:00:01")
            assertThat(Formatting.formatDuration(30000)).isEqualTo("00:00:30")
            assertThat(Formatting.formatDuration(59000)).isEqualTo("00:00:59")
        }

        @Test
        @DisplayName("should format minutes and seconds")
        fun shouldFormatMinutesAndSeconds() {
            assertThat(Formatting.formatDuration(60000)).isEqualTo("00:01:00")
            assertThat(Formatting.formatDuration(90000)).isEqualTo("00:01:30")
            assertThat(Formatting.formatDuration(3599000)).isEqualTo("00:59:59")
        }

        @Test
        @DisplayName("should format hours, minutes, and seconds")
        fun shouldFormatHoursMinutesAndSeconds() {
            assertThat(Formatting.formatDuration(3600000)).isEqualTo("01:00:00")
            assertThat(Formatting.formatDuration(5025000)).isEqualTo("01:23:45")
            assertThat(Formatting.formatDuration(86399000)).isEqualTo("23:59:59")
        }

        @Test
        @DisplayName("should format durations over 24 hours")
        fun shouldFormatDurationsOver24Hours() {
            assertThat(Formatting.formatDuration(86400000)).isEqualTo("24:00:00")
            assertThat(Formatting.formatDuration(90000000)).isEqualTo("25:00:00")
        }

        @Test
        @DisplayName("should format durations over 99 hours without padding")
        fun shouldFormatDurationsOver99HoursWithoutPadding() {
            assertThat(Formatting.formatDuration(360000000)).isEqualTo("100:00:00")
            assertThat(Formatting.formatDuration(3600000000)).isEqualTo("1000:00:00")
        }

        @Test
        @DisplayName("should throw for negative duration")
        fun shouldThrowForNegativeDuration() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    Formatting.formatDuration(-1)
                }
            assertThat(exception.message).contains("non-negative")
        }
    }

    @Nested
    @DisplayName("formatDurationSeconds")
    inner class FormatDurationSecondsTest {
        @Test
        @DisplayName("should format 0 seconds")
        fun shouldFormatZeroSeconds() {
            assertThat(Formatting.formatDurationSeconds(0)).isEqualTo("00:00:00")
        }

        @Test
        @DisplayName("should format seconds correctly")
        fun shouldFormatSecondsCorrectly() {
            assertThat(Formatting.formatDurationSeconds(1)).isEqualTo("00:00:01")
            assertThat(Formatting.formatDurationSeconds(90)).isEqualTo("00:01:30")
            assertThat(Formatting.formatDurationSeconds(3661)).isEqualTo("01:01:01")
        }

        @Test
        @DisplayName("should throw for negative seconds")
        fun shouldThrowForNegativeSeconds() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    Formatting.formatDurationSeconds(-1)
                }
            assertThat(exception.message).contains("non-negative")
        }
    }

    @Nested
    @DisplayName("formatDurationShort (M:SS or H:MM:SS)")
    inner class FormatDurationShortTest {
        @Test
        @DisplayName("should format 0 milliseconds")
        fun shouldFormatZeroMilliseconds() {
            assertThat(Formatting.formatDurationShort(0)).isEqualTo("0:00")
        }

        @Test
        @DisplayName("should format under 1 minute as M:SS")
        fun shouldFormatUnder1MinuteAsMSS() {
            assertThat(Formatting.formatDurationShort(1000)).isEqualTo("0:01")
            assertThat(Formatting.formatDurationShort(30000)).isEqualTo("0:30")
            assertThat(Formatting.formatDurationShort(59000)).isEqualTo("0:59")
        }

        @Test
        @DisplayName("should format under 1 hour as M:SS")
        fun shouldFormatUnder1HourAsMSS() {
            assertThat(Formatting.formatDurationShort(60000)).isEqualTo("1:00")
            assertThat(Formatting.formatDurationShort(330000)).isEqualTo("5:30")
            assertThat(Formatting.formatDurationShort(3599000)).isEqualTo("59:59")
        }

        @Test
        @DisplayName("should format 1 hour or more as H:MM:SS")
        fun shouldFormat1HourOrMoreAsHMMSS() {
            assertThat(Formatting.formatDurationShort(3600000)).isEqualTo("1:00:00")
            assertThat(Formatting.formatDurationShort(5025000)).isEqualTo("1:23:45")
            assertThat(Formatting.formatDurationShort(36000000)).isEqualTo("10:00:00")
        }

        @Test
        @DisplayName("should throw for negative duration")
        fun shouldThrowForNegativeDuration() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    Formatting.formatDurationShort(-1)
                }
            assertThat(exception.message).contains("non-negative")
        }
    }

    @Nested
    @DisplayName("formatDurationCompact")
    inner class FormatDurationCompactTest {
        @Test
        @DisplayName("should format 0 milliseconds as 0s")
        fun shouldFormatZeroMillisecondsAs0s() {
            assertThat(Formatting.formatDurationCompact(0)).isEqualTo("0s")
        }

        @Test
        @DisplayName("should format under 1 minute as Xs")
        fun shouldFormatUnder1MinuteAsXs() {
            assertThat(Formatting.formatDurationCompact(1000)).isEqualTo("1s")
            assertThat(Formatting.formatDurationCompact(45000)).isEqualTo("45s")
            assertThat(Formatting.formatDurationCompact(59000)).isEqualTo("59s")
        }

        @Test
        @DisplayName("should format under 1 hour as Xm Ys")
        fun shouldFormatUnder1HourAsXmYs() {
            assertThat(Formatting.formatDurationCompact(60000)).isEqualTo("1m 0s")
            assertThat(Formatting.formatDurationCompact(330000)).isEqualTo("5m 30s")
            assertThat(Formatting.formatDurationCompact(3599000)).isEqualTo("59m 59s")
        }

        @Test
        @DisplayName("should format 1 hour or more as Xh Ym")
        fun shouldFormat1HourOrMoreAsXhYm() {
            assertThat(Formatting.formatDurationCompact(3600000)).isEqualTo("1h 0m")
            assertThat(Formatting.formatDurationCompact(5400000)).isEqualTo("1h 30m")
            assertThat(Formatting.formatDurationCompact(8100000)).isEqualTo("2h 15m")
        }

        @Test
        @DisplayName("should throw for negative duration")
        fun shouldThrowForNegativeDuration() {
            val exception =
                assertThrows<IllegalArgumentException> {
                    Formatting.formatDurationCompact(-1)
                }
            assertThat(exception.message).contains("non-negative")
        }
    }
}
