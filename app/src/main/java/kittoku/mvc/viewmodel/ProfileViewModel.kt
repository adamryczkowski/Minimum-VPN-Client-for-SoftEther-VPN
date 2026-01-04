package kittoku.mvc.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kittoku.mvc.model.VpnProfile
import kittoku.mvc.repository.ProfileRepository
import kittoku.mvc.service.ProfileImportExportService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for profile management screens.
 *
 * This data class represents all the state needed by the UI to render
 * the profile list, edit forms, and handle user interactions.
 */
data class ProfileUiState(
    /** List of all VPN profiles */
    val profiles: List<VpnProfile> = emptyList(),
    /** Whether the profile list is currently loading */
    val isLoading: Boolean = true,
    /** Currently selected profile for connection */
    val selectedProfile: VpnProfile? = null,
    /** Profile being edited (null when not in edit mode) */
    val editingProfile: VpnProfile? = null,
    /** Profile pending deletion confirmation */
    val profileToDelete: VpnProfile? = null,
    /** Error message to display (null when no error) */
    val error: String? = null,
    /** Success message to display (null when no message) */
    val successMessage: String? = null,
)

/**
 * ViewModel for managing VPN profiles.
 *
 * This ViewModel handles all the business logic for profile management,
 * including CRUD operations, import/export, and UI state management.
 * It is designed to be UI-framework agnostic, working with both
 * traditional Views and Jetpack Compose.
 */
class ProfileViewModel(
    private val repository: ProfileRepository,
    private val importExportService: ProfileImportExportService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeProfiles()
    }

    /**
     * Observe profile changes from the repository.
     */
    private fun observeProfiles() {
        viewModelScope.launch {
            repository.getAllProfiles()
                .catch { e ->
                    _uiState.update { it.copy(error = "Failed to load profiles: ${e.message}") }
                }
                .collect { profiles ->
                    _uiState.update {
                        it.copy(
                            profiles = profiles,
                            isLoading = false,
                        )
                    }
                }
        }
    }

    /**
     * Create a new VPN profile.
     *
     * @param name Profile display name
     * @param serverAddress VPN server address
     * @param username Authentication username
     * @param password Authentication password
     * @param hubName Optional hub name (defaults to "DEFAULT")
     * @param port Optional port number (defaults to 443)
     */
    fun createProfile(
        name: String,
        serverAddress: String,
        username: String,
        password: String,
        hubName: String = "DEFAULT",
        port: Int = 443,
    ) {
        // Validate input
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Profile name cannot be empty") }
            return
        }
        if (serverAddress.isBlank()) {
            _uiState.update { it.copy(error = "Server address cannot be empty") }
            return
        }

        viewModelScope.launch {
            try {
                val profile =
                    VpnProfile(
                        name = name.trim(),
                        serverAddress = serverAddress.trim(),
                        username = username.trim(),
                        password = password,
                        hubName = hubName.trim(),
                        port = port,
                    )

                repository.insertProfile(profile)
                _uiState.update {
                    it.copy(successMessage = "Profile '$name' created successfully")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create profile: ${e.message}") }
            }
        }
    }

    /**
     * Request deletion of a profile (shows confirmation).
     */
    fun requestDeleteProfile(profile: VpnProfile) {
        _uiState.update { it.copy(profileToDelete = profile) }
    }

    /**
     * Dismiss the delete confirmation dialog.
     */
    fun dismissDeleteConfirmation() {
        _uiState.update { it.copy(profileToDelete = null) }
    }

    /**
     * Delete a profile after confirmation.
     */
    fun deleteProfile(profile: VpnProfile) {
        viewModelScope.launch {
            try {
                repository.deleteProfile(profile)
                _uiState.update {
                    it.copy(
                        profileToDelete = null,
                        successMessage = "Profile '${profile.name}' deleted",
                        // Clear selection if deleted profile was selected
                        selectedProfile = if (it.selectedProfile?.id == profile.id) null else it.selectedProfile,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to delete profile: ${e.message}") }
            }
        }
    }

    /**
     * Load a profile for editing.
     */
    fun loadProfileForEdit(profileId: Long) {
        viewModelScope.launch {
            try {
                val profile = repository.getProfileById(profileId)
                if (profile != null) {
                    _uiState.update { it.copy(editingProfile = profile) }
                } else {
                    _uiState.update { it.copy(error = "Profile not found") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load profile: ${e.message}") }
            }
        }
    }

    /**
     * Cancel editing and clear the edit state.
     */
    fun cancelEdit() {
        _uiState.update { it.copy(editingProfile = null) }
    }

    /**
     * Update an existing profile.
     */
    fun updateProfile(
        id: Long,
        name: String,
        serverAddress: String,
        username: String,
        password: String,
        hubName: String = "DEFAULT",
        port: Int = 443,
    ) {
        // Validate input
        if (name.isBlank()) {
            _uiState.update { it.copy(error = "Profile name cannot be empty") }
            return
        }
        if (serverAddress.isBlank()) {
            _uiState.update { it.copy(error = "Server address cannot be empty") }
            return
        }

        viewModelScope.launch {
            try {
                val existingProfile = repository.getProfileById(id)
                if (existingProfile == null) {
                    _uiState.update { it.copy(error = "Profile not found") }
                    return@launch
                }

                val updatedProfile =
                    existingProfile.copy(
                        name = name.trim(),
                        serverAddress = serverAddress.trim(),
                        username = username.trim(),
                        password = password,
                        hubName = hubName.trim(),
                        port = port,
                    )

                repository.updateProfile(updatedProfile)
                _uiState.update {
                    it.copy(
                        editingProfile = null,
                        successMessage = "Profile '$name' updated successfully",
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to update profile: ${e.message}") }
            }
        }
    }

    /**
     * Select a profile for connection.
     * Selecting the same profile again will deselect it.
     */
    fun selectProfile(profile: VpnProfile) {
        _uiState.update {
            val newSelection = if (it.selectedProfile?.id == profile.id) null else profile
            it.copy(selectedProfile = newSelection)
        }
    }

    /**
     * Export profiles to JSON.
     *
     * @param profiles List of profiles to export
     * @param includePasswords Whether to include passwords in the export
     * @return JSON string of exported profiles, or null if export failed
     */
    suspend fun exportProfiles(
        profiles: List<VpnProfile>,
        includePasswords: Boolean,
    ): String? {
        val result = importExportService.exportProfiles(profiles, includePasswords)
        return result.getOrNull()
    }

    /**
     * Import profiles from JSON.
     *
     * @param json JSON string containing profiles to import
     */
    fun importProfiles(json: String) {
        viewModelScope.launch {
            val result = importExportService.importProfiles(json)
            result.fold(
                onSuccess = { profiles ->
                    try {
                        var importedCount = 0
                        for (profile in profiles) {
                            // Create new profile without ID to let Room generate one
                            val newProfile = profile.copy(id = 0)
                            repository.insertProfile(newProfile)
                            importedCount++
                        }
                        _uiState.update {
                            it.copy(successMessage = "Imported $importedCount profile(s) successfully")
                        }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(error = "Failed to save imported profiles: ${e.message}") }
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = "Failed to import profiles: ${e.message}") }
                },
            )
        }
    }

    /**
     * Dismiss the current error message.
     */
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Dismiss the current success message.
     */
    fun dismissSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
