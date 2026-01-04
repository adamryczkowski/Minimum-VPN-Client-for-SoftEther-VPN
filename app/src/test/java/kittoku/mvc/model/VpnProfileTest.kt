package kittoku.mvc.model

import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * TDD tests for VpnProfile data model.
 *
 * These tests define the expected behavior of the VpnProfile entity
 * before implementation (Red-Green-Refactor cycle).
 */
@DisplayName("VpnProfile")
class VpnProfileTest {
    @Nested
    @DisplayName("Creation")
    inner class Creation {
        @Test
        @DisplayName("should create profile with required fields")
        fun shouldCreateProfileWithRequiredFields() {
            val profile =
                VpnProfile(
                    id = 1L,
                    name = "Work VPN",
                    serverAddress = "vpn.example.com",
                    username = "user@example.com",
                )

            assertThat(profile.id).isEqualTo(1L)
            assertThat(profile.name).isEqualTo("Work VPN")
            assertThat(profile.serverAddress).isEqualTo("vpn.example.com")
            assertThat(profile.username).isEqualTo("user@example.com")
        }

        @Test
        @DisplayName("should have default values for optional fields")
        fun shouldHaveDefaultValuesForOptionalFields() {
            val profile =
                VpnProfile(
                    name = "Test VPN",
                    serverAddress = "vpn.test.com",
                    username = "testuser",
                )

            assertThat(profile.id).isEqualTo(0L)
            assertThat(profile.port).isEqualTo(443)
            assertThat(profile.hubName).isEqualTo("DEFAULT")
            assertThat(profile.password).isEmpty()
            assertThat(profile.useTls).isTrue()
            assertThat(profile.verifyServerCertificate).isTrue()
            assertThat(profile.autoConnect).isFalse()
            assertThat(profile.autoConnectOnWifi).isFalse()
            assertThat(profile.autoConnectOnMobile).isFalse()
            assertThat(profile.splitTunneling).isFalse()
            assertThat(profile.excludedApps).isEmpty()
            assertThat(profile.includedApps).isEmpty()
            assertThat(profile.customDns).isNull()
            assertThat(profile.createdAt).isGreaterThan(0L)
            assertThat(profile.updatedAt).isGreaterThan(0L)
        }

        @Test
        @DisplayName("should create profile with custom port")
        fun shouldCreateProfileWithCustomPort() {
            val profile =
                VpnProfile(
                    name = "Custom Port VPN",
                    serverAddress = "vpn.custom.com",
                    username = "user",
                    port = 8443,
                )

            assertThat(profile.port).isEqualTo(8443)
        }

        @Test
        @DisplayName("should create profile with custom hub name")
        fun shouldCreateProfileWithCustomHubName() {
            val profile =
                VpnProfile(
                    name = "Custom Hub VPN",
                    serverAddress = "vpn.hub.com",
                    username = "user",
                    hubName = "CUSTOM_HUB",
                )

            assertThat(profile.hubName).isEqualTo("CUSTOM_HUB")
        }
    }

    @Nested
    @DisplayName("Validation")
    inner class Validation {
        @Test
        @DisplayName("should validate that name is not blank")
        fun shouldValidateThatNameIsNotBlank() {
            val profile =
                VpnProfile(
                    name = "",
                    serverAddress = "vpn.example.com",
                    username = "user",
                )

            assertThat(profile.isValid()).isFalse()
            assertThat(profile.validationErrors()).contains("Name cannot be blank")
        }

        @Test
        @DisplayName("should validate that server address is not blank")
        fun shouldValidateThatServerAddressIsNotBlank() {
            val profile =
                VpnProfile(
                    name = "Test VPN",
                    serverAddress = "",
                    username = "user",
                )

            assertThat(profile.isValid()).isFalse()
            assertThat(profile.validationErrors()).contains("Server address cannot be blank")
        }

        @Test
        @DisplayName("should validate that username is not blank")
        fun shouldValidateThatUsernameIsNotBlank() {
            val profile =
                VpnProfile(
                    name = "Test VPN",
                    serverAddress = "vpn.example.com",
                    username = "",
                )

            assertThat(profile.isValid()).isFalse()
            assertThat(profile.validationErrors()).contains("Username cannot be blank")
        }

        @Test
        @DisplayName("should validate that port is in valid range")
        fun shouldValidateThatPortIsInValidRange() {
            val profileWithInvalidPort =
                VpnProfile(
                    name = "Test VPN",
                    serverAddress = "vpn.example.com",
                    username = "user",
                    port = 70000,
                )

            assertThat(profileWithInvalidPort.isValid()).isFalse()
            assertThat(profileWithInvalidPort.validationErrors()).contains("Port must be between 1 and 65535")
        }

        @Test
        @DisplayName("should validate that port is positive")
        fun shouldValidateThatPortIsPositive() {
            val profileWithNegativePort =
                VpnProfile(
                    name = "Test VPN",
                    serverAddress = "vpn.example.com",
                    username = "user",
                    port = -1,
                )

            assertThat(profileWithNegativePort.isValid()).isFalse()
            assertThat(profileWithNegativePort.validationErrors()).contains("Port must be between 1 and 65535")
        }

        @Test
        @DisplayName("should be valid with all required fields filled")
        fun shouldBeValidWithAllRequiredFieldsFilled() {
            val profile =
                VpnProfile(
                    name = "Valid VPN",
                    serverAddress = "vpn.example.com",
                    username = "user",
                )

            assertThat(profile.isValid()).isTrue()
            assertThat(profile.validationErrors()).isEmpty()
        }
    }

    @Nested
    @DisplayName("Copy and Update")
    inner class CopyAndUpdate {
        @Test
        @DisplayName("should update timestamp when profile is modified")
        fun shouldUpdateTimestampWhenProfileIsModified() {
            val originalProfile =
                VpnProfile(
                    name = "Original VPN",
                    serverAddress = "vpn.original.com",
                    username = "user",
                )

            // Simulate some time passing
            Thread.sleep(10)

            val updatedProfile = originalProfile.withUpdatedTimestamp()

            assertThat(updatedProfile.updatedAt).isGreaterThan(originalProfile.updatedAt)
            assertThat(updatedProfile.createdAt).isEqualTo(originalProfile.createdAt)
        }

        @Test
        @DisplayName("should copy profile with new name")
        fun shouldCopyProfileWithNewName() {
            val original =
                VpnProfile(
                    id = 1L,
                    name = "Original VPN",
                    serverAddress = "vpn.example.com",
                    username = "user",
                )

            val copied = original.copy(name = "Copied VPN")

            assertThat(copied.name).isEqualTo("Copied VPN")
            assertThat(copied.serverAddress).isEqualTo(original.serverAddress)
            assertThat(copied.username).isEqualTo(original.username)
        }
    }

    @Nested
    @DisplayName("Serialization")
    inner class Serialization {
        private val json =
            Json {
                prettyPrint = true
                ignoreUnknownKeys = true
            }

        @Test
        @DisplayName("should serialize profile to JSON")
        fun shouldSerializeProfileToJson() {
            val profile =
                VpnProfile(
                    id = 1L,
                    name = "Test VPN",
                    serverAddress = "vpn.example.com",
                    username = "user",
                    port = 443,
                    hubName = "DEFAULT",
                )

            val jsonString = json.encodeToString(profile)

            assertThat(jsonString).contains("\"name\"")
            assertThat(jsonString).contains("\"Test VPN\"")
            assertThat(jsonString).contains("\"serverAddress\"")
            assertThat(jsonString).contains("\"vpn.example.com\"")
        }

        @Test
        @DisplayName("should deserialize profile from JSON")
        fun shouldDeserializeProfileFromJson() {
            val jsonString =
                """
                {
                    "id": 1,
                    "name": "Test VPN",
                    "serverAddress": "vpn.example.com",
                    "username": "user",
                    "port": 443,
                    "hubName": "DEFAULT",
                    "password": "",
                    "useTls": true,
                    "verifyServerCertificate": true,
                    "autoConnect": false,
                    "autoConnectOnWifi": false,
                    "autoConnectOnMobile": false,
                    "splitTunneling": false,
                    "excludedApps": [],
                    "includedApps": [],
                    "customDns": null,
                    "createdAt": 1234567890,
                    "updatedAt": 1234567890
                }
                """.trimIndent()

            val profile = json.decodeFromString<VpnProfile>(jsonString)

            assertThat(profile.id).isEqualTo(1L)
            assertThat(profile.name).isEqualTo("Test VPN")
            assertThat(profile.serverAddress).isEqualTo("vpn.example.com")
            assertThat(profile.username).isEqualTo("user")
        }

        @Test
        @DisplayName("should not serialize password by default for export")
        fun shouldNotSerializePasswordByDefaultForExport() {
            val profile =
                VpnProfile(
                    name = "Secure VPN",
                    serverAddress = "vpn.secure.com",
                    username = "user",
                    password = "secret123",
                )

            val exportJson = profile.toExportJson()

            assertThat(exportJson).doesNotContain("secret123")
        }

        @Test
        @DisplayName("should optionally include password in export")
        fun shouldOptionallyIncludePasswordInExport() {
            val profile =
                VpnProfile(
                    name = "Secure VPN",
                    serverAddress = "vpn.secure.com",
                    username = "user",
                    password = "secret123",
                )

            val exportJson = profile.toExportJson(includePassword = true)

            assertThat(exportJson).contains("secret123")
        }
    }

    @Nested
    @DisplayName("Split Tunneling")
    inner class SplitTunneling {
        @Test
        @DisplayName("should store excluded apps list")
        fun shouldStoreExcludedAppsList() {
            val profile =
                VpnProfile(
                    name = "Split VPN",
                    serverAddress = "vpn.split.com",
                    username = "user",
                    splitTunneling = true,
                    excludedApps = listOf("com.example.app1", "com.example.app2"),
                )

            assertThat(profile.excludedApps).containsExactly("com.example.app1", "com.example.app2")
        }

        @Test
        @DisplayName("should store included apps list")
        fun shouldStoreIncludedAppsList() {
            val profile =
                VpnProfile(
                    name = "Split VPN",
                    serverAddress = "vpn.split.com",
                    username = "user",
                    splitTunneling = true,
                    includedApps = listOf("com.example.vpnonly"),
                )

            assertThat(profile.includedApps).containsExactly("com.example.vpnonly")
        }
    }

    @Nested
    @DisplayName("Auto-Connect")
    inner class AutoConnect {
        @Test
        @DisplayName("should enable auto-connect on WiFi")
        fun shouldEnableAutoConnectOnWifi() {
            val profile =
                VpnProfile(
                    name = "Auto VPN",
                    serverAddress = "vpn.auto.com",
                    username = "user",
                    autoConnect = true,
                    autoConnectOnWifi = true,
                )

            assertThat(profile.autoConnect).isTrue()
            assertThat(profile.autoConnectOnWifi).isTrue()
        }

        @Test
        @DisplayName("should enable auto-connect on mobile data")
        fun shouldEnableAutoConnectOnMobileData() {
            val profile =
                VpnProfile(
                    name = "Auto VPN",
                    serverAddress = "vpn.auto.com",
                    username = "user",
                    autoConnect = true,
                    autoConnectOnMobile = true,
                )

            assertThat(profile.autoConnect).isTrue()
            assertThat(profile.autoConnectOnMobile).isTrue()
        }
    }

    @Nested
    @DisplayName("Custom DNS")
    inner class CustomDns {
        @Test
        @DisplayName("should store custom DNS server")
        fun shouldStoreCustomDnsServer() {
            val profile =
                VpnProfile(
                    name = "DNS VPN",
                    serverAddress = "vpn.dns.com",
                    username = "user",
                    customDns = "8.8.8.8",
                )

            assertThat(profile.customDns).isEqualTo("8.8.8.8")
        }

        @Test
        @DisplayName("should allow null custom DNS")
        fun shouldAllowNullCustomDns() {
            val profile =
                VpnProfile(
                    name = "Default DNS VPN",
                    serverAddress = "vpn.default.com",
                    username = "user",
                    customDns = null,
                )

            assertThat(profile.customDns).isNull()
        }
    }
}
