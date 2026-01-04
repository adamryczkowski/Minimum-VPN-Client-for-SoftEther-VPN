package kittoku.mvc.repository

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kittoku.mvc.model.VpnProfile
import kittoku.mvc.model.VpnProfileDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * TDD tests for ProfileRepository.
 *
 * These tests define the expected behavior of the ProfileRepository
 * before implementation (Red-Green-Refactor cycle).
 */
@DisplayName("ProfileRepository")
class ProfileRepositoryTest {
    private lateinit var dao: VpnProfileDao
    private lateinit var repository: ProfileRepository

    @BeforeEach
    fun setup() {
        dao = mockk(relaxed = true)
        repository = ProfileRepositoryImpl(dao)
    }

    @Nested
    @DisplayName("getAllProfiles")
    inner class GetAllProfiles {
        @Test
        @DisplayName("should return all profiles from DAO as Flow")
        fun shouldReturnAllProfilesFromDaoAsFlow() =
            runTest {
                // Given
                val profiles =
                    listOf(
                        createTestProfile(id = 1L, name = "Profile 1"),
                        createTestProfile(id = 2L, name = "Profile 2"),
                    )
                coEvery { dao.getAllProfiles() } returns flowOf(profiles)

                // When
                val result = repository.getAllProfiles().first()

                // Then
                assertThat(result).hasSize(2)
                assertThat(result[0].name).isEqualTo("Profile 1")
                assertThat(result[1].name).isEqualTo("Profile 2")
            }

        @Test
        @DisplayName("should return empty list when no profiles exist")
        fun shouldReturnEmptyListWhenNoProfilesExist() =
            runTest {
                // Given
                coEvery { dao.getAllProfiles() } returns flowOf(emptyList())

                // When
                val result = repository.getAllProfiles().first()

                // Then
                assertThat(result).isEmpty()
            }
    }

    @Nested
    @DisplayName("getProfileById")
    inner class GetProfileById {
        @Test
        @DisplayName("should return profile when it exists")
        fun shouldReturnProfileWhenItExists() =
            runTest {
                // Given
                val profile = createTestProfile(id = 1L, name = "Test Profile")
                coEvery { dao.getProfileById(1L) } returns profile

                // When
                val result = repository.getProfileById(1L)

                // Then
                assertThat(result).isNotNull()
                assertThat(result?.name).isEqualTo("Test Profile")
            }

        @Test
        @DisplayName("should return null when profile does not exist")
        fun shouldReturnNullWhenProfileDoesNotExist() =
            runTest {
                // Given
                coEvery { dao.getProfileById(999L) } returns null

                // When
                val result = repository.getProfileById(999L)

                // Then
                assertThat(result).isNull()
            }
    }

    @Nested
    @DisplayName("getProfileByName")
    inner class GetProfileByName {
        @Test
        @DisplayName("should return profile when name matches")
        fun shouldReturnProfileWhenNameMatches() =
            runTest {
                // Given
                val profile = createTestProfile(id = 1L, name = "Work VPN")
                coEvery { dao.getProfileByName("Work VPN") } returns profile

                // When
                val result = repository.getProfileByName("Work VPN")

                // Then
                assertThat(result).isNotNull()
                assertThat(result?.name).isEqualTo("Work VPN")
            }

        @Test
        @DisplayName("should return null when name does not match")
        fun shouldReturnNullWhenNameDoesNotMatch() =
            runTest {
                // Given
                coEvery { dao.getProfileByName("Nonexistent") } returns null

                // When
                val result = repository.getProfileByName("Nonexistent")

                // Then
                assertThat(result).isNull()
            }
    }

    @Nested
    @DisplayName("insertProfile")
    inner class InsertProfile {
        @Test
        @DisplayName("should insert profile and return generated ID")
        fun shouldInsertProfileAndReturnGeneratedId() =
            runTest {
                // Given
                val profile = createTestProfile(id = 0L, name = "New Profile")
                coEvery { dao.insertProfile(any()) } returns 42L

                // When
                val result = repository.insertProfile(profile)

                // Then
                assertThat(result).isEqualTo(42L)
                coVerify { dao.insertProfile(profile) }
            }

        @Test
        @DisplayName("should validate profile before inserting")
        fun shouldValidateProfileBeforeInserting() =
            runTest {
                // Given
                // Invalid: blank name
                val invalidProfile =
                    VpnProfile(
                        name = "",
                        serverAddress = "vpn.example.com",
                        username = "user",
                    )

                // When
                val result = repository.insertProfile(invalidProfile)

                // Then
                assertThat(result).isEqualTo(-1L) // Indicates failure
                coVerify(exactly = 0) { dao.insertProfile(any()) }
            }
    }

    @Nested
    @DisplayName("updateProfile")
    inner class UpdateProfile {
        @Test
        @DisplayName("should update profile in DAO")
        fun shouldUpdateProfileInDao() =
            runTest {
                // Given
                val profile = createTestProfile(id = 1L, name = "Updated Profile")
                val profileSlot = slot<VpnProfile>()
                coEvery { dao.updateProfile(capture(profileSlot)) } returns Unit

                // When
                repository.updateProfile(profile)

                // Then
                coVerify { dao.updateProfile(any()) }
                assertThat(profileSlot.captured.id).isEqualTo(1L)
                assertThat(profileSlot.captured.name).isEqualTo("Updated Profile")
            }

        @Test
        @DisplayName("should update timestamp when updating profile")
        fun shouldUpdateTimestampWhenUpdatingProfile() =
            runTest {
                // Given
                val originalTimestamp = System.currentTimeMillis() - 10000
                val profile =
                    createTestProfile(
                        id = 1L,
                        name = "Profile",
                        updatedAt = originalTimestamp,
                    )
                val profileSlot = slot<VpnProfile>()
                coEvery { dao.updateProfile(capture(profileSlot)) } returns Unit

                // When
                repository.updateProfile(profile)

                // Then
                assertThat(profileSlot.captured.updatedAt).isGreaterThan(originalTimestamp)
            }

        @Test
        @DisplayName("should not update invalid profile")
        fun shouldNotUpdateInvalidProfile() =
            runTest {
                // Given
                // Invalid: blank name
                val invalidProfile =
                    VpnProfile(
                        id = 1L,
                        name = "",
                        serverAddress = "vpn.example.com",
                        username = "user",
                    )

                // When
                val result = repository.updateProfile(invalidProfile)

                // Then
                assertThat(result).isFalse()
                coVerify(exactly = 0) { dao.updateProfile(any()) }
            }
    }

    @Nested
    @DisplayName("deleteProfile")
    inner class DeleteProfile {
        @Test
        @DisplayName("should delete profile from DAO")
        fun shouldDeleteProfileFromDao() =
            runTest {
                // Given
                val profile = createTestProfile(id = 1L, name = "To Delete")
                coEvery { dao.deleteProfile(any()) } returns Unit

                // When
                repository.deleteProfile(profile)

                // Then
                coVerify { dao.deleteProfile(profile) }
            }

        @Test
        @DisplayName("should delete profile by ID")
        fun shouldDeleteProfileById() =
            runTest {
                // Given
                coEvery { dao.deleteProfileById(1L) } returns Unit

                // When
                repository.deleteProfileById(1L)

                // Then
                coVerify { dao.deleteProfileById(1L) }
            }
    }

    @Nested
    @DisplayName("getAutoConnectProfiles")
    inner class GetAutoConnectProfiles {
        @Test
        @DisplayName("should return profiles with auto-connect enabled")
        fun shouldReturnProfilesWithAutoConnectEnabled() =
            runTest {
                // Given
                val autoConnectProfiles =
                    listOf(
                        createTestProfile(id = 1L, name = "Auto 1", autoConnect = true),
                        createTestProfile(id = 2L, name = "Auto 2", autoConnect = true),
                    )
                coEvery { dao.getAutoConnectProfiles() } returns autoConnectProfiles

                // When
                val result = repository.getAutoConnectProfiles()

                // Then
                assertThat(result).hasSize(2)
                assertThat(result.all { it.autoConnect }).isTrue()
            }

        @Test
        @DisplayName("should return empty list when no auto-connect profiles")
        fun shouldReturnEmptyListWhenNoAutoConnectProfiles() =
            runTest {
                // Given
                coEvery { dao.getAutoConnectProfiles() } returns emptyList()

                // When
                val result = repository.getAutoConnectProfiles()

                // Then
                assertThat(result).isEmpty()
            }
    }

    @Nested
    @DisplayName("getProfileCount")
    inner class GetProfileCount {
        @Test
        @DisplayName("should return total number of profiles")
        fun shouldReturnTotalNumberOfProfiles() =
            runTest {
                // Given
                coEvery { dao.getProfileCount() } returns 5

                // When
                val result = repository.getProfileCount()

                // Then
                assertThat(result).isEqualTo(5)
            }
    }

    @Nested
    @DisplayName("profileExists")
    inner class ProfileExists {
        @Test
        @DisplayName("should return true when profile with name exists")
        fun shouldReturnTrueWhenProfileWithNameExists() =
            runTest {
                // Given
                coEvery { dao.profileExistsByName("Existing") } returns true

                // When
                val result = repository.profileExists("Existing")

                // Then
                assertThat(result).isTrue()
            }

        @Test
        @DisplayName("should return false when profile with name does not exist")
        fun shouldReturnFalseWhenProfileWithNameDoesNotExist() =
            runTest {
                // Given
                coEvery { dao.profileExistsByName("Nonexistent") } returns false

                // When
                val result = repository.profileExists("Nonexistent")

                // Then
                assertThat(result).isFalse()
            }
    }

    // Helper function to create test profiles
    private fun createTestProfile(
        id: Long = 0L,
        name: String = "Test VPN",
        serverAddress: String = "vpn.example.com",
        username: String = "testuser",
        autoConnect: Boolean = false,
        updatedAt: Long = System.currentTimeMillis(),
    ): VpnProfile {
        return VpnProfile(
            id = id,
            name = name,
            serverAddress = serverAddress,
            username = username,
            autoConnect = autoConnect,
            updatedAt = updatedAt,
        )
    }
}
