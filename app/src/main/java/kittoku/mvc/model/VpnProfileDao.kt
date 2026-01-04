package kittoku.mvc.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for VpnProfile entities.
 *
 * Provides methods for CRUD operations on VPN profiles stored in the Room database.
 */
@Dao
interface VpnProfileDao {
    /**
     * Get all VPN profiles as a Flow for reactive updates.
     */
    @Query("SELECT * FROM vpn_profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<VpnProfile>>

    /**
     * Get a specific profile by its ID.
     */
    @Query("SELECT * FROM vpn_profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): VpnProfile?

    /**
     * Get a specific profile by its name.
     */
    @Query("SELECT * FROM vpn_profiles WHERE name = :name")
    suspend fun getProfileByName(name: String): VpnProfile?

    /**
     * Insert a new profile and return the generated ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: VpnProfile): Long

    /**
     * Update an existing profile.
     */
    @Update
    suspend fun updateProfile(profile: VpnProfile)

    /**
     * Delete a profile.
     */
    @Delete
    suspend fun deleteProfile(profile: VpnProfile)

    /**
     * Delete a profile by its ID.
     */
    @Query("DELETE FROM vpn_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Long)

    /**
     * Get all profiles with auto-connect enabled.
     */
    @Query("SELECT * FROM vpn_profiles WHERE autoConnect = 1")
    suspend fun getAutoConnectProfiles(): List<VpnProfile>

    /**
     * Get the total number of profiles.
     */
    @Query("SELECT COUNT(*) FROM vpn_profiles")
    suspend fun getProfileCount(): Int

    /**
     * Check if a profile with the given name exists.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM vpn_profiles WHERE name = :name)")
    suspend fun profileExistsByName(name: String): Boolean

    /**
     * Delete all profiles.
     */
    @Query("DELETE FROM vpn_profiles")
    suspend fun deleteAllProfiles()
}
