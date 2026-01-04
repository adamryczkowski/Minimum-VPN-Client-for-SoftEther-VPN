package kittoku.mvc.repository

import kittoku.mvc.model.VpnProfile
import kittoku.mvc.model.VpnProfileDao
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for VPN profile operations.
 *
 * Provides a clean API for accessing and managing VPN profiles,
 * abstracting the underlying data source (Room database).
 */
interface ProfileRepository {
    /**
     * Get all VPN profiles as a Flow for reactive updates.
     */
    fun getAllProfiles(): Flow<List<VpnProfile>>

    /**
     * Get a specific profile by its ID.
     *
     * @param id The profile ID
     * @return The profile if found, null otherwise
     */
    suspend fun getProfileById(id: Long): VpnProfile?

    /**
     * Get a specific profile by its name.
     *
     * @param name The profile name
     * @return The profile if found, null otherwise
     */
    suspend fun getProfileByName(name: String): VpnProfile?

    /**
     * Insert a new profile.
     *
     * @param profile The profile to insert
     * @return The generated ID if successful, -1 if validation failed
     */
    suspend fun insertProfile(profile: VpnProfile): Long

    /**
     * Update an existing profile.
     *
     * @param profile The profile to update
     * @return true if successful, false if validation failed
     */
    suspend fun updateProfile(profile: VpnProfile): Boolean

    /**
     * Delete a profile.
     *
     * @param profile The profile to delete
     */
    suspend fun deleteProfile(profile: VpnProfile)

    /**
     * Delete a profile by its ID.
     *
     * @param id The profile ID to delete
     */
    suspend fun deleteProfileById(id: Long)

    /**
     * Get all profiles with auto-connect enabled.
     *
     * @return List of profiles with autoConnect = true
     */
    suspend fun getAutoConnectProfiles(): List<VpnProfile>

    /**
     * Get the total number of profiles.
     *
     * @return The count of all profiles
     */
    suspend fun getProfileCount(): Int

    /**
     * Check if a profile with the given name exists.
     *
     * @param name The profile name to check
     * @return true if a profile with this name exists
     */
    suspend fun profileExists(name: String): Boolean
}

/**
 * Implementation of ProfileRepository using Room database.
 *
 * @param dao The VpnProfileDao for database operations
 */
class ProfileRepositoryImpl(
    private val dao: VpnProfileDao,
) : ProfileRepository {
    override fun getAllProfiles(): Flow<List<VpnProfile>> {
        return dao.getAllProfiles()
    }

    override suspend fun getProfileById(id: Long): VpnProfile? {
        return dao.getProfileById(id)
    }

    override suspend fun getProfileByName(name: String): VpnProfile? {
        return dao.getProfileByName(name)
    }

    override suspend fun insertProfile(profile: VpnProfile): Long {
        // Validate profile before inserting
        if (!profile.isValid()) {
            return -1L
        }
        return dao.insertProfile(profile)
    }

    override suspend fun updateProfile(profile: VpnProfile): Boolean {
        // Validate profile before updating
        if (!profile.isValid()) {
            return false
        }

        // Update the timestamp
        val updatedProfile = profile.withUpdatedTimestamp()
        dao.updateProfile(updatedProfile)
        return true
    }

    override suspend fun deleteProfile(profile: VpnProfile) {
        dao.deleteProfile(profile)
    }

    override suspend fun deleteProfileById(id: Long) {
        dao.deleteProfileById(id)
    }

    override suspend fun getAutoConnectProfiles(): List<VpnProfile> {
        return dao.getAutoConnectProfiles()
    }

    override suspend fun getProfileCount(): Int {
        return dao.getProfileCount()
    }

    override suspend fun profileExists(name: String): Boolean {
        return dao.profileExistsByName(name)
    }
}
