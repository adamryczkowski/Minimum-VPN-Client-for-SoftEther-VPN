package kittoku.mvc.logging

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("VpnError")
class VpnErrorTest {
    @Nested
    @DisplayName("Connection Errors")
    inner class ConnectionErrors {
        @Test
        @DisplayName("ConnectionTimeout should have correct code and message")
        fun connectionTimeout() {
            val error = VpnError.ConnectionTimeout("example.com", 30000)

            assertThat(error.code).isEqualTo("VPN_E001")
            assertThat(error.userMessage).contains("timed out")
            assertThat(error.technicalDetails).contains("example.com")
            assertThat(error.technicalDetails).contains("30000ms")
            assertThat(error.suggestedActions).isNotEmpty()
        }

        @Test
        @DisplayName("ConnectionRefused should have correct code and message")
        fun connectionRefused() {
            val error = VpnError.ConnectionRefused("example.com", 443)

            assertThat(error.code).isEqualTo("VPN_E002")
            assertThat(error.userMessage).contains("refused")
            assertThat(error.technicalDetails).contains("example.com:443")
            assertThat(error.suggestedActions).isNotEmpty()
        }

        @Test
        @DisplayName("HostUnreachable should have correct code and message")
        fun hostUnreachable() {
            val error = VpnError.HostUnreachable("example.com")

            assertThat(error.code).isEqualTo("VPN_E003")
            assertThat(error.userMessage).contains("unreachable")
            assertThat(error.technicalDetails).contains("example.com")
            assertThat(error.suggestedActions).isNotEmpty()
        }

        @Test
        @DisplayName("DnsResolutionFailed should have correct code and message")
        fun dnsResolutionFailed() {
            val error = VpnError.DnsResolutionFailed("invalid.hostname")

            assertThat(error.code).isEqualTo("VPN_E004")
            assertThat(error.userMessage).contains("resolve")
            assertThat(error.technicalDetails).contains("invalid.hostname")
            assertThat(error.suggestedActions).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("Authentication Errors")
    inner class AuthenticationErrors {
        @Test
        @DisplayName("AuthenticationFailed should have correct code and message")
        fun authenticationFailed() {
            val error = VpnError.AuthenticationFailed("Invalid password")

            assertThat(error.code).isEqualTo("VPN_E010")
            assertThat(error.userMessage).contains("Authentication failed")
            assertThat(error.technicalDetails).contains("Invalid password")
            assertThat(error.suggestedActions).isNotEmpty()
        }

        @Test
        @DisplayName("CertificateError should have correct code and message")
        fun certificateError() {
            val error = VpnError.CertificateError("Certificate expired")

            assertThat(error.code).isEqualTo("VPN_E011")
            assertThat(error.userMessage).contains("Certificate")
            assertThat(error.technicalDetails).contains("Certificate expired")
            assertThat(error.suggestedActions).isNotEmpty()
        }

        @Test
        @DisplayName("AccountExpired should have correct code and message")
        fun accountExpired() {
            val error = VpnError.AccountExpired()

            assertThat(error.code).isEqualTo("VPN_E012")
            assertThat(error.userMessage).contains("expired")
            assertThat(error.suggestedActions).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("Protocol Errors")
    inner class ProtocolErrors {
        @Test
        @DisplayName("ProtocolError should have correct code and message")
        fun protocolError() {
            val error = VpnError.ProtocolError("handshake", "Invalid response")

            assertThat(error.code).isEqualTo("VPN_E020")
            assertThat(error.userMessage).contains("protocol error")
            assertThat(error.technicalDetails).contains("handshake")
            assertThat(error.technicalDetails).contains("Invalid response")
            assertThat(error.suggestedActions).isNotEmpty()
        }

        @Test
        @DisplayName("UnsupportedProtocolVersion should have correct code and message")
        fun unsupportedProtocolVersion() {
            val error = VpnError.UnsupportedProtocolVersion("1.0", "2.0")

            assertThat(error.code).isEqualTo("VPN_E021")
            assertThat(error.userMessage).contains("incompatible")
            assertThat(error.technicalDetails).contains("client=1.0")
            assertThat(error.technicalDetails).contains("server=2.0")
            assertThat(error.suggestedActions).isNotEmpty()
        }

        @Test
        @DisplayName("DhcpFailed should have correct code and message")
        fun dhcpFailed() {
            val error = VpnError.DhcpFailed("No response from server")

            assertThat(error.code).isEqualTo("VPN_E022")
            assertThat(error.userMessage).contains("network configuration")
            assertThat(error.technicalDetails).contains("No response from server")
            assertThat(error.suggestedActions).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("Network Errors")
    inner class NetworkErrors {
        @Test
        @DisplayName("NetworkLost should have correct code and message")
        fun networkLost() {
            val error = VpnError.NetworkLost()

            assertThat(error.code).isEqualTo("VPN_E030")
            assertThat(error.userMessage).contains("connection lost")
            assertThat(error.suggestedActions).isNotEmpty()
        }

        @Test
        @DisplayName("TunnelCreationFailed should have correct code and message")
        fun tunnelCreationFailed() {
            val error = VpnError.TunnelCreationFailed("Permission denied")

            assertThat(error.code).isEqualTo("VPN_E031")
            assertThat(error.userMessage).contains("tunnel")
            assertThat(error.technicalDetails).contains("Permission denied")
            assertThat(error.suggestedActions).isNotEmpty()
        }

        @Test
        @DisplayName("UdpAccelerationFailed should have correct code and message")
        fun udpAccelerationFailed() {
            val error = VpnError.UdpAccelerationFailed("UDP blocked")

            assertThat(error.code).isEqualTo("VPN_E032")
            assertThat(error.userMessage).contains("UDP acceleration")
            assertThat(error.technicalDetails).contains("UDP blocked")
            assertThat(error.suggestedActions).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("Permission Errors")
    inner class PermissionErrors {
        @Test
        @DisplayName("VpnPermissionDenied should have correct code and message")
        fun vpnPermissionDenied() {
            val error = VpnError.VpnPermissionDenied()

            assertThat(error.code).isEqualTo("VPN_E040")
            assertThat(error.userMessage).contains("permission")
            assertThat(error.suggestedActions).isNotEmpty()
        }

        @Test
        @DisplayName("AlwaysOnVpnConflict should have correct code and message")
        fun alwaysOnVpnConflict() {
            val error = VpnError.AlwaysOnVpnConflict("OtherVPN")

            assertThat(error.code).isEqualTo("VPN_E041")
            assertThat(error.userMessage).contains("exclusive access")
            assertThat(error.technicalDetails).contains("OtherVPN")
            assertThat(error.suggestedActions).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("Internal Errors")
    inner class InternalErrors {
        @Test
        @DisplayName("InternalError should have correct code and message")
        fun internalError() {
            val error = VpnError.InternalError("Unexpected state")

            assertThat(error.code).isEqualTo("VPN_E099")
            assertThat(error.userMessage).contains("unexpected error")
            assertThat(error.technicalDetails).contains("Unexpected state")
            assertThat(error.suggestedActions).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("Error Conversion")
    inner class ErrorConversion {
        @Test
        @DisplayName("should convert timeout exception to ConnectionTimeout")
        fun convertTimeoutException() {
            val exception = RuntimeException("Connection timeout")
            val error = VpnError.fromException(exception)

            assertThat(error).isInstanceOf(VpnError.ConnectionTimeout::class.java)
        }

        @Test
        @DisplayName("should convert refused exception to ConnectionRefused")
        fun convertRefusedException() {
            val exception = RuntimeException("Connection refused")
            val error = VpnError.fromException(exception)

            assertThat(error).isInstanceOf(VpnError.ConnectionRefused::class.java)
        }

        @Test
        @DisplayName("should convert authentication exception to AuthenticationFailed")
        fun convertAuthException() {
            val exception = RuntimeException("Authentication failed")
            val error = VpnError.fromException(exception)

            assertThat(error).isInstanceOf(VpnError.AuthenticationFailed::class.java)
        }

        @Test
        @DisplayName("should convert certificate exception to CertificateError")
        fun convertCertException() {
            val exception = RuntimeException("Certificate validation failed")
            val error = VpnError.fromException(exception)

            assertThat(error).isInstanceOf(VpnError.CertificateError::class.java)
        }

        @Test
        @DisplayName("should convert unknown exception to InternalError")
        fun convertUnknownException() {
            val exception = RuntimeException("Something went wrong")
            val error = VpnError.fromException(exception)

            assertThat(error).isInstanceOf(VpnError.InternalError::class.java)
        }

        @Test
        @DisplayName("should return same error if already VpnError")
        fun returnSameVpnError() {
            val originalError = VpnError.NetworkLost()
            val convertedError = VpnError.fromException(originalError)

            assertThat(convertedError).isSameInstanceAs(originalError)
        }
    }

    @Nested
    @DisplayName("Error Formatting")
    inner class ErrorFormatting {
        @Test
        @DisplayName("toLogString should include code and technical details")
        fun toLogString() {
            val error = VpnError.ConnectionTimeout("example.com", 30000)
            val logString = error.toLogString()

            assertThat(logString).contains("VPN_E001")
            assertThat(logString).contains("example.com")
            assertThat(logString).contains("30000ms")
        }

        @Test
        @DisplayName("toDisplayString should include user message and actions")
        fun toDisplayString() {
            val error = VpnError.ConnectionTimeout("example.com", 30000)
            val displayString = error.toDisplayString()

            assertThat(displayString).contains(error.userMessage)
            assertThat(displayString).contains("Try:")
            error.suggestedActions.forEach { action ->
                assertThat(displayString).contains(action)
            }
        }
    }

    @Nested
    @DisplayName("Cause Chain")
    inner class CauseChain {
        @Test
        @DisplayName("should preserve cause in error")
        fun preserveCause() {
            val cause = IllegalStateException("Root cause")
            val error = VpnError.InternalError("Wrapper", cause)

            assertThat(error.cause).isEqualTo(cause)
        }

        @Test
        @DisplayName("should work as exception")
        fun workAsException() {
            val error = VpnError.NetworkLost()

            // Should be throwable
            val caught =
                try {
                    throw error
                } catch (e: VpnError) {
                    e
                }

            assertThat(caught).isEqualTo(error)
            assertThat(caught.message).isEqualTo(error.userMessage)
        }
    }
}
