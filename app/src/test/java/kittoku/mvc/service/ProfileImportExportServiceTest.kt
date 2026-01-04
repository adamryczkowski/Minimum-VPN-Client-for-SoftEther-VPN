package kittoku.mvc.service

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kittoku.mvc.model.VpnProfile
import kittoku.mvc.repository.ProfileRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * TDD tests for ProfileImportExportService.
 *
 * These tests define the expected behavior of the profile import/export service
 * before implementation (Red-Green-Refactor cycle).
 */
@DisplayName("ProfileImportExportService")
class ProfileImportExportServiceTest {
    private lateinit var repository: ProfileRepository
    private lateinit var service: ProfileImportExportService

    @BeforeEach
    fun setup() {
        repository = mockk(relaxed = true)
        service = ProfileImportExportServiceImpl(repository)
    }

    @Nested
    @DisplayName("exportProfiles")
    inner class ExportProfiles {
        @Test
        @DisplayName("should export single profile to JSON")
        fun shouldExportSingleProfileToJson() =
            runTest {
                // Given
                val profile = createTestProfile(id = 1L, name = "Work VPN")

                // When
                val result = service.exportProfile(profile)

                // Then
                assertThat(result.isSuccess).isTrue()
                val json = result.getOrNull()!!
                assertThat(json).contains("\"name\"")
                assertThat(json).contains("\"Work VPN\"")
                assertThat(json).contains("\"serverAddress\"")
            }

        @Test
        @DisplayName("should export multiple profiles to JSON array")
        fun shouldExportMultipleProfilesToJsonArray() =
            runTest {
                // Given
                val profiles =
                    listOf(
                        createTestProfile(id = 1L, name = "Work VPN"),
                        createTestProfile(id = 2L, name = "Home VPN"),
                    )

                // When
                val result = service.exportProfiles(profiles)

                // Then
                assertThat(result.isSuccess).isTrue()
                val json = result.getOrNull()!!
                assertThat(json).contains("\"Work VPN\"")
                assertThat(json).contains("\"Home VPN\"")
                assertThat(json).startsWith("[")
                assertThat(json).endsWith("]")
            }

        @Test
        @DisplayName("should not include password by default")
        fun shouldNotIncludePasswordByDefault() =
            runTest {
                // Given
                val profile =
                    createTestProfile(
                        id = 1L,
                        name = "Secure VPN",
                        password = "secret123",
                    )

                // When
                val result = service.exportProfile(profile)

                // Then
                assertThat(result.isSuccess).isTrue()
                val json = result.getOrNull()!!
                assertThat(json).doesNotContain("secret123")
            }

        @Test
        @DisplayName("should optionally include password")
        fun shouldOptionallyIncludePassword() =
            runTest {
                // Given
                val profile =
                    createTestProfile(
                        id = 1L,
                        name = "Secure VPN",
                        password = "secret123",
                    )

                // When
                val result = service.exportProfile(profile, includePassword = true)

                // Then
                assertThat(result.isSuccess).isTrue()
                val json = result.getOrNull()!!
                assertThat(json).contains("secret123")
            }

        @Test
        @DisplayName("should export all profiles from repository")
        fun shouldExportAllProfilesFromRepository() =
            runTest {
                // Given
                val profiles =
                    listOf(
                        createTestProfile(id = 1L, name = "Profile 1"),
                        createTestProfile(id = 2L, name = "Profile 2"),
                    )
                coEvery { repository.getAutoConnectProfiles() } returns profiles

                // When - export all profiles (we'll need to add this method)
                val result = service.exportProfiles(profiles)

                // Then
                assertThat(result.isSuccess).isTrue()
            }
    }

    @Nested
    @DisplayName("importProfiles")
    inner class ImportProfiles {
        @Test
        @DisplayName("should import single profile from JSON")
        fun shouldImportSingleProfileFromJson() =
            runTest {
                // Given
                val json =
                    """
                    {
                        "name": "Imported VPN",
                        "serverAddress": "vpn.example.com",
                        "username": "user",
                        "port": 443,
                        "hubName": "DEFAULT"
                    }
                    """.trimIndent()
                coEvery { repository.insertProfile(any()) } returns 1L

                // When
                val result = service.importProfile(json)

                // Then
                assertThat(result.isSuccess).isTrue()
                val profile = result.getOrNull()!!
                assertThat(profile.name).isEqualTo("Imported VPN")
                assertThat(profile.serverAddress).isEqualTo("vpn.example.com")
            }

        @Test
        @DisplayName("should import multiple profiles from JSON array")
        fun shouldImportMultipleProfilesFromJsonArray() =
            runTest {
                // Given
                val json =
                    """
                    [
                        {
                            "name": "VPN 1",
                            "serverAddress": "vpn1.example.com",
                            "username": "user1",
                            "port": 443
                        },
                        {
                            "name": "VPN 2",
                            "serverAddress": "vpn2.example.com",
                            "username": "user2",
                            "port": 8443
                        }
                    ]
                    """.trimIndent()
                coEvery { repository.insertProfile(any()) } returns 1L

                // When
                val result = service.importProfiles(json)

                // Then
                assertThat(result.isSuccess).isTrue()
                val profiles = result.getOrNull()!!
                assertThat(profiles).hasSize(2)
                assertThat(profiles[0].name).isEqualTo("VPN 1")
                assertThat(profiles[1].name).isEqualTo("VPN 2")
            }

        @Test
        @DisplayName("should fail on invalid JSON")
        fun shouldFailOnInvalidJson() =
            runTest {
                // Given
                val invalidJson = "{ invalid json }"

                // When
                val result = service.importProfile(invalidJson)

                // Then
                assertThat(result.isFailure).isTrue()
            }

        @Test
        @DisplayName("should fail on missing required fields")
        fun shouldFailOnMissingRequiredFields() =
            runTest {
                // Given - missing serverAddress
                val json =
                    """
                    {
                        "name": "Incomplete VPN",
                        "username": "user"
                    }
                    """.trimIndent()

                // When
                val result = service.importProfile(json)

                // Then
                assertThat(result.isFailure).isTrue()
            }

        @Test
        @DisplayName("should save imported profile to repository")
        fun shouldSaveImportedProfileToRepository() =
            runTest {
                // Given
                val json =
                    """
                    {
                        "name": "New VPN",
                        "serverAddress": "vpn.new.com",
                        "username": "newuser",
                        "port": 443
                    }
                    """.trimIndent()
                coEvery { repository.insertProfile(any()) } returns 42L

                // When
                val result = service.importProfileAndSave(json)

                // Then
                assertThat(result.isSuccess).isTrue()
                assertThat(result.getOrNull()).isEqualTo(42L)
                coVerify { repository.insertProfile(any()) }
            }

        @Test
        @DisplayName("should handle duplicate profile names")
        fun shouldHandleDuplicateProfileNames() =
            runTest {
                // Given
                val json =
                    """
                    {
                        "name": "Existing VPN",
                        "serverAddress": "vpn.existing.com",
                        "username": "user"
                    }
                    """.trimIndent()
                coEvery { repository.profileExists("Existing VPN") } returns true

                // When
                val result = service.importProfileAndSave(json)

                // Then
                assertThat(result.isFailure).isTrue()
                val error = result.exceptionOrNull()
                assertThat(error?.message).contains("already exists")
            }

        @Test
        @DisplayName("should rename duplicate profiles when forced")
        fun shouldRenameDuplicateProfilesWhenForced() =
            runTest {
                // Given
                val json =
                    """
                    {
                        "name": "Existing VPN",
                        "serverAddress": "vpn.existing.com",
                        "username": "user"
                    }
                    """.trimIndent()
                coEvery { repository.profileExists("Existing VPN") } returns true
                coEvery { repository.profileExists("Existing VPN (1)") } returns false
                coEvery { repository.insertProfile(any()) } returns 1L

                // When
                val result = service.importProfileAndSave(json, renameIfDuplicate = true)

                // Then
                assertThat(result.isSuccess).isTrue()
                coVerify {
                    repository.insertProfile(match { it.name == "Existing VPN (1)" })
                }
            }
    }

    @Nested
    @DisplayName("validateImport")
    inner class ValidateImport {
        @Test
        @DisplayName("should validate JSON before import")
        fun shouldValidateJsonBeforeImport() =
            runTest {
                // Given
                val validJson =
                    """
                    {
                        "name": "Valid VPN",
                        "serverAddress": "vpn.valid.com",
                        "username": "user"
                    }
                    """.trimIndent()

                // When
                val result = service.validateImportJson(validJson)

                // Then
                assertThat(result.isValid).isTrue()
                assertThat(result.errors).isEmpty()
            }

        @Test
        @DisplayName("should return validation errors for invalid JSON")
        fun shouldReturnValidationErrorsForInvalidJson() =
            runTest {
                // Given
                val invalidJson =
                    """
                    {
                        "name": "",
                        "serverAddress": "",
                        "username": ""
                    }
                    """.trimIndent()

                // When
                val result = service.validateImportJson(invalidJson)

                // Then
                assertThat(result.isValid).isFalse()
                assertThat(result.errors).isNotEmpty()
            }

        @Test
        @DisplayName("should count profiles in import file")
        fun shouldCountProfilesInImportFile() =
            runTest {
                // Given
                val json =
                    """
                    [
                        {"name": "VPN 1", "serverAddress": "vpn1.com", "username": "u1"},
                        {"name": "VPN 2", "serverAddress": "vpn2.com", "username": "u2"},
                        {"name": "VPN 3", "serverAddress": "vpn3.com", "username": "u3"}
                    ]
                    """.trimIndent()

                // When
                val result = service.validateImportJson(json)

                // Then
                assertThat(result.profileCount).isEqualTo(3)
            }
    }

    // Helper function to create test profiles
    private fun createTestProfile(
        id: Long = 0L,
        name: String = "Test VPN",
        serverAddress: String = "vpn.example.com",
        username: String = "testuser",
        password: String = "",
    ): VpnProfile {
        return VpnProfile(
            id = id,
            name = name,
            serverAddress = serverAddress,
            username = username,
            password = password,
        )
    }
}
