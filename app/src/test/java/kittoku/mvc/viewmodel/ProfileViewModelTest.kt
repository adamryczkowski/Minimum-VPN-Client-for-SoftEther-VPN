package kittoku.mvc.viewmodel

import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kittoku.mvc.model.VpnProfile
import kittoku.mvc.repository.ProfileRepository
import kittoku.mvc.service.ProfileImportExportService
import kittoku.mvc.testutil.CoroutineTestExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

/**
 * Tests for ProfileViewModel - the UI logic layer for profile management.
 *
 * These tests verify the ViewModel behavior without requiring actual UI components,
 * making them fast, reliable, and independent of the UI framework (View or Compose).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ProfileViewModelTest {
    private lateinit var repository: ProfileRepository
    private lateinit var importExportService: ProfileImportExportService
    private lateinit var viewModel: ProfileViewModel

    private val profilesFlow = MutableStateFlow<List<VpnProfile>>(emptyList())

    @BeforeEach
    fun setup() {
        repository = mockk(relaxed = true)
        importExportService = mockk(relaxed = true)

        coEvery { repository.getAllProfiles() } returns profilesFlow

        viewModel = ProfileViewModel(repository, importExportService)
    }

    @Nested
    @DisplayName("Profile List State")
    inner class ProfileListState {
        @Test
        fun `initial state should be loading`() =
            runTest {
                // Given a fresh ViewModel
                val newViewModel = ProfileViewModel(repository, importExportService)

                // Then initial state should be loading
                val state = newViewModel.uiState.first()
                assertThat(state.isLoading).isTrue()
            }

        @Test
        fun `should show empty state when no profiles exist`() =
            runTest {
                // Given no profiles
                profilesFlow.value = emptyList()

                // When ViewModel observes profiles
                advanceUntilIdle()

                // Then state should show empty
                val state = viewModel.uiState.first()
                assertThat(state.profiles).isEmpty()
                assertThat(state.isLoading).isFalse()
            }

        @Test
        fun `should display profiles when they exist`() =
            runTest {
                // Given some profiles
                val profiles =
                    listOf(
                        createTestProfile(1, "Work VPN"),
                        createTestProfile(2, "Home VPN"),
                    )
                profilesFlow.value = profiles

                // When ViewModel observes profiles
                advanceUntilIdle()

                // Then state should contain profiles
                val state = viewModel.uiState.first()
                assertThat(state.profiles).hasSize(2)
                assertThat(state.profiles.map { it.name }).containsExactly("Work VPN", "Home VPN")
            }

        @Test
        fun `should update when profiles change`() =
            runTest {
                // Given initial profiles
                profilesFlow.value = listOf(createTestProfile(1, "Initial"))
                advanceUntilIdle()

                // When profiles are updated
                profilesFlow.value =
                    listOf(
                        createTestProfile(1, "Initial"),
                        createTestProfile(2, "New Profile"),
                    )
                advanceUntilIdle()

                // Then state should reflect the change
                val state = viewModel.uiState.first()
                assertThat(state.profiles).hasSize(2)
            }
    }

    @Nested
    @DisplayName("Create Profile")
    inner class CreateProfile {
        @Test
        fun `should create profile with valid data`() =
            runTest {
                // Given valid profile data
                val name = "New VPN"
                val server = "vpn.example.com"
                val username = "user"
                val password = "pass"

                coEvery { repository.insertProfile(any()) } returns 1L

                // When creating profile
                viewModel.createProfile(name, server, username, password)
                advanceUntilIdle()

                // Then repository should be called with correct data
                val profileSlot = slot<VpnProfile>()
                coVerify { repository.insertProfile(capture(profileSlot)) }

                assertThat(profileSlot.captured.name).isEqualTo(name)
                assertThat(profileSlot.captured.serverAddress).isEqualTo(server)
                assertThat(profileSlot.captured.username).isEqualTo(username)
                assertThat(profileSlot.captured.password).isEqualTo(password)
            }

        @Test
        fun `should show error for empty name`() =
            runTest {
                // When creating profile with empty name
                viewModel.createProfile("", "vpn.example.com", "user", "pass")
                advanceUntilIdle()

                // Then error should be shown
                val state = viewModel.uiState.first()
                assertThat(state.error).isNotNull()
                assertThat(state.error).contains("name")

                // And repository should not be called
                coVerify(exactly = 0) { repository.insertProfile(any()) }
            }

        @Test
        fun `should show error for empty server address`() =
            runTest {
                // When creating profile with empty server
                viewModel.createProfile("Test", "", "user", "pass")
                advanceUntilIdle()

                // Then error should be shown
                val state = viewModel.uiState.first()
                assertThat(state.error).isNotNull()
                assertThat(state.error!!.lowercase()).contains("server")

                // And repository should not be called
                coVerify(exactly = 0) { repository.insertProfile(any()) }
            }

        @Test
        fun `should show success message after creation`() =
            runTest {
                // Given valid data
                coEvery { repository.insertProfile(any()) } returns 1L

                // When creating profile
                viewModel.createProfile("Test", "vpn.example.com", "user", "pass")
                advanceUntilIdle()

                // Then success message should be shown
                val state = viewModel.uiState.first()
                assertThat(state.successMessage).isNotNull()
            }
    }

    @Nested
    @DisplayName("Delete Profile")
    inner class DeleteProfile {
        @Test
        fun `should delete profile by id`() =
            runTest {
                // Given a profile exists
                val profile = createTestProfile(1, "To Delete")

                // When deleting
                viewModel.deleteProfile(profile)
                advanceUntilIdle()

                // Then repository should be called
                coVerify { repository.deleteProfile(profile) }
            }

        @Test
        fun `should show confirmation before delete`() =
            runTest {
                // Given a profile
                val profile = createTestProfile(1, "Test")

                // When requesting delete
                viewModel.requestDeleteProfile(profile)

                // Then confirmation state should be set
                val state = viewModel.uiState.first()
                assertThat(state.profileToDelete).isEqualTo(profile)
            }

        @Test
        fun `should cancel delete when dismissed`() =
            runTest {
                // Given delete confirmation is shown
                val profile = createTestProfile(1, "Test")
                viewModel.requestDeleteProfile(profile)

                // When dismissing
                viewModel.dismissDeleteConfirmation()

                // Then confirmation should be cleared
                val state = viewModel.uiState.first()
                assertThat(state.profileToDelete).isNull()

                // And repository should not be called
                coVerify(exactly = 0) { repository.deleteProfile(any()) }
            }
    }

    @Nested
    @DisplayName("Edit Profile")
    inner class EditProfile {
        @Test
        fun `should load profile for editing`() =
            runTest {
                // Given a profile exists
                val profile = createTestProfile(1, "Existing")
                coEvery { repository.getProfileById(1) } returns profile

                // When loading for edit
                viewModel.loadProfileForEdit(1)
                advanceUntilIdle()

                // Then profile should be in edit state
                val state = viewModel.uiState.first()
                assertThat(state.editingProfile).isEqualTo(profile)
            }

        @Test
        fun `should update profile with new data`() =
            runTest {
                // Given a profile is being edited
                val original = createTestProfile(1, "Original")
                coEvery { repository.getProfileById(1) } returns original
                viewModel.loadProfileForEdit(1)
                advanceUntilIdle()

                // When updating
                viewModel.updateProfile(
                    id = 1,
                    name = "Updated",
                    serverAddress = "new.server.com",
                    username = "newuser",
                    password = "newpass",
                )
                advanceUntilIdle()

                // Then repository should be called with updated data
                val profileSlot = slot<VpnProfile>()
                coVerify { repository.updateProfile(capture(profileSlot)) }

                assertThat(profileSlot.captured.id).isEqualTo(1)
                assertThat(profileSlot.captured.name).isEqualTo("Updated")
                assertThat(profileSlot.captured.serverAddress).isEqualTo("new.server.com")
            }

        @Test
        fun `should show error when profile not found`() =
            runTest {
                // Given profile doesn't exist
                coEvery { repository.getProfileById(999) } returns null

                // When loading for edit
                viewModel.loadProfileForEdit(999)
                advanceUntilIdle()

                // Then error should be shown
                val state = viewModel.uiState.first()
                assertThat(state.error).isNotNull()
                assertThat(state.error).contains("not found")
            }
    }

    @Nested
    @DisplayName("Select Profile for Connection")
    inner class SelectProfile {
        @Test
        fun `should select profile for connection`() =
            runTest {
                // Given a profile
                val profile = createTestProfile(1, "Work VPN")

                // When selecting
                viewModel.selectProfile(profile)

                // Then profile should be selected
                val state = viewModel.uiState.first()
                assertThat(state.selectedProfile).isEqualTo(profile)
            }

        @Test
        fun `should deselect when selecting same profile`() =
            runTest {
                // Given a profile is selected
                val profile = createTestProfile(1, "Work VPN")
                viewModel.selectProfile(profile)

                // When selecting same profile again
                viewModel.selectProfile(profile)

                // Then profile should be deselected
                val state = viewModel.uiState.first()
                assertThat(state.selectedProfile).isNull()
            }
    }

    @Nested
    @DisplayName("Import/Export")
    inner class ImportExport {
        @Test
        fun `should export selected profiles`() =
            runTest {
                // Given profiles to export
                val profiles =
                    listOf(
                        createTestProfile(1, "Profile 1"),
                        createTestProfile(2, "Profile 2"),
                    )
                coEvery { importExportService.exportProfiles(profiles, false) } returns Result.success("""{"profiles":[]}""")

                // When exporting
                val result = viewModel.exportProfiles(profiles, includePasswords = false)
                advanceUntilIdle()

                // Then export service should be called
                coVerify { importExportService.exportProfiles(profiles, false) }
                assertThat(result).isNotNull()
            }

        @Test
        fun `should import profiles from JSON`() =
            runTest {
                // Given valid JSON
                val json = """{"profiles":[]}"""
                val importedProfiles = listOf(createTestProfile(0, "Imported"))
                coEvery { importExportService.importProfiles(json) } returns Result.success(importedProfiles)
                coEvery { repository.insertProfile(any()) } returns 1L

                // When importing
                viewModel.importProfiles(json)
                advanceUntilIdle()

                // Then profiles should be saved
                coVerify { repository.insertProfile(any()) }
            }

        @Test
        fun `should show error for invalid import JSON`() =
            runTest {
                // Given invalid JSON
                val json = "invalid json"
                coEvery { importExportService.importProfiles(json) } returns
                    Result.failure(IllegalArgumentException("Invalid JSON"))

                // When importing
                viewModel.importProfiles(json)
                advanceUntilIdle()

                // Then error should be shown
                val state = viewModel.uiState.first()
                assertThat(state.error).isNotNull()
            }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {
        @Test
        fun `should clear error when dismissed`() =
            runTest {
                // Given an error state
                viewModel.createProfile("", "", "", "")
                advanceUntilIdle()
                assertThat(viewModel.uiState.first().error).isNotNull()

                // When dismissing error
                viewModel.dismissError()

                // Then error should be cleared
                val state = viewModel.uiState.first()
                assertThat(state.error).isNull()
            }

        @Test
        fun `should clear success message when dismissed`() =
            runTest {
                // Given a success state
                coEvery { repository.insertProfile(any()) } returns 1L
                viewModel.createProfile("Test", "server.com", "user", "pass")
                advanceUntilIdle()
                assertThat(viewModel.uiState.first().successMessage).isNotNull()

                // When dismissing
                viewModel.dismissSuccessMessage()

                // Then message should be cleared
                val state = viewModel.uiState.first()
                assertThat(state.successMessage).isNull()
            }
    }

    // Helper function to create test profiles
    private fun createTestProfile(
        id: Long,
        name: String,
    ): VpnProfile {
        return VpnProfile(
            id = id,
            name = name,
            serverAddress = "vpn.example.com",
            username = "testuser",
            password = "testpass",
        )
    }
}
