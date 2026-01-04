package kittoku.mvc.service

import kittoku.mvc.model.VpnProfile
import kittoku.mvc.repository.ProfileRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Result of import validation.
 */
data class ImportValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val profileCount: Int,
)

/**
 * Service interface for importing and exporting VPN profiles.
 */
interface ProfileImportExportService {
    /**
     * Export a single profile to JSON.
     *
     * @param profile The profile to export
     * @param includePassword Whether to include the password in the export
     * @return Result containing the JSON string or an error
     */
    suspend fun exportProfile(
        profile: VpnProfile,
        includePassword: Boolean = false,
    ): Result<String>

    /**
     * Export multiple profiles to JSON array.
     *
     * @param profiles The profiles to export
     * @param includePassword Whether to include passwords in the export
     * @return Result containing the JSON string or an error
     */
    suspend fun exportProfiles(
        profiles: List<VpnProfile>,
        includePassword: Boolean = false,
    ): Result<String>

    /**
     * Import a single profile from JSON (without saving).
     *
     * @param json The JSON string to import
     * @return Result containing the parsed profile or an error
     */
    suspend fun importProfile(json: String): Result<VpnProfile>

    /**
     * Import multiple profiles from JSON array (without saving).
     *
     * @param json The JSON string to import
     * @return Result containing the parsed profiles or an error
     */
    suspend fun importProfiles(json: String): Result<List<VpnProfile>>

    /**
     * Import a profile from JSON and save it to the repository.
     *
     * @param json The JSON string to import
     * @param renameIfDuplicate Whether to rename the profile if a duplicate exists
     * @return Result containing the saved profile ID or an error
     */
    suspend fun importProfileAndSave(
        json: String,
        renameIfDuplicate: Boolean = false,
    ): Result<Long>

    /**
     * Validate JSON before import.
     *
     * @param json The JSON string to validate
     * @return Validation result with errors and profile count
     */
    suspend fun validateImportJson(json: String): ImportValidationResult
}

/**
 * Implementation of ProfileImportExportService.
 */
class ProfileImportExportServiceImpl(
    private val repository: ProfileRepository,
) : ProfileImportExportService {
    private val jsonParser =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }

    override suspend fun exportProfile(
        profile: VpnProfile,
        includePassword: Boolean,
    ): Result<String> {
        return runCatching {
            val exportable = ExportableProfile.fromVpnProfile(profile, includePassword)
            jsonParser.encodeToString(exportable)
        }
    }

    override suspend fun exportProfiles(
        profiles: List<VpnProfile>,
        includePassword: Boolean,
    ): Result<String> {
        return runCatching {
            val exportables = profiles.map { ExportableProfile.fromVpnProfile(it, includePassword) }
            jsonParser.encodeToString(exportables)
        }
    }

    override suspend fun importProfile(json: String): Result<VpnProfile> {
        return runCatching {
            val trimmed = json.trim()

            // Try to parse as single object
            val importable = jsonParser.decodeFromString<ImportableProfile>(trimmed)

            // Validate required fields
            if (importable.name.isNullOrBlank()) {
                throw IllegalArgumentException("Name is required")
            }
            if (importable.serverAddress.isNullOrBlank()) {
                throw IllegalArgumentException("Server address is required")
            }
            if (importable.username.isNullOrBlank()) {
                throw IllegalArgumentException("Username is required")
            }

            importable.toVpnProfile()
        }
    }

    override suspend fun importProfiles(json: String): Result<List<VpnProfile>> {
        return runCatching {
            val trimmed = json.trim()

            val importables = jsonParser.decodeFromString<List<ImportableProfile>>(trimmed)

            importables.map { importable ->
                if (importable.name.isNullOrBlank() ||
                    importable.serverAddress.isNullOrBlank() ||
                    importable.username.isNullOrBlank()
                ) {
                    throw IllegalArgumentException("Required fields missing in one or more profiles")
                }
                importable.toVpnProfile()
            }
        }
    }

    override suspend fun importProfileAndSave(
        json: String,
        renameIfDuplicate: Boolean,
    ): Result<Long> {
        return runCatching {
            val profileResult = importProfile(json)
            if (profileResult.isFailure) {
                throw profileResult.exceptionOrNull()!!
            }

            var profile = profileResult.getOrThrow()

            // Check for duplicate name
            if (repository.profileExists(profile.name)) {
                if (!renameIfDuplicate) {
                    throw IllegalArgumentException("Profile with name '${profile.name}' already exists")
                }

                // Find a unique name
                var counter = 1
                var newName = "${profile.name} ($counter)"
                while (repository.profileExists(newName)) {
                    counter++
                    newName = "${profile.name} ($counter)"
                }
                profile = profile.copy(name = newName)
            }

            // Reset ID for new insertion
            val profileToSave =
                profile.copy(
                    id = 0L,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )

            repository.insertProfile(profileToSave)
        }
    }

    override suspend fun validateImportJson(json: String): ImportValidationResult {
        val errors = mutableListOf<String>()
        var profileCount = 0

        try {
            val trimmed = json.trim()

            if (trimmed.startsWith("[")) {
                // Array of profiles
                val importables = jsonParser.decodeFromString<List<ImportableProfile>>(trimmed)
                profileCount = importables.size

                importables.forEachIndexed { index, importable ->
                    val profileErrors = validateImportable(importable, index + 1)
                    errors.addAll(profileErrors)
                }
            } else {
                // Single profile
                val importable = jsonParser.decodeFromString<ImportableProfile>(trimmed)
                profileCount = 1

                val profileErrors = validateImportable(importable, 1)
                errors.addAll(profileErrors)
            }
        } catch (e: Exception) {
            errors.add("Invalid JSON format: ${e.message}")
        }

        return ImportValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            profileCount = profileCount,
        )
    }

    private fun validateImportable(
        importable: ImportableProfile,
        profileNumber: Int,
    ): List<String> {
        val errors = mutableListOf<String>()
        val prefix = if (profileNumber > 0) "Profile $profileNumber: " else ""

        if (importable.name.isNullOrBlank()) {
            errors.add("${prefix}Name cannot be blank")
        }
        if (importable.serverAddress.isNullOrBlank()) {
            errors.add("${prefix}Server address cannot be blank")
        }
        if (importable.username.isNullOrBlank()) {
            errors.add("${prefix}Username cannot be blank")
        }
        if (importable.port != null && (importable.port < 1 || importable.port > 65535)) {
            errors.add("${prefix}Port must be between 1 and 65535")
        }

        return errors
    }

    /**
     * Data class for exporting profiles (controls what fields are included).
     */
    @Serializable
    private data class ExportableProfile(
        val name: String,
        val serverAddress: String,
        val username: String,
        val port: Int,
        val hubName: String,
        val password: String? = null,
        val useTls: Boolean,
        val verifyServerCertificate: Boolean,
        val autoConnect: Boolean,
        val autoConnectOnWifi: Boolean,
        val autoConnectOnMobile: Boolean,
        val splitTunneling: Boolean,
        val excludedApps: List<String>,
        val includedApps: List<String>,
        val customDns: String?,
    ) {
        companion object {
            fun fromVpnProfile(
                profile: VpnProfile,
                includePassword: Boolean,
            ): ExportableProfile {
                return ExportableProfile(
                    name = profile.name,
                    serverAddress = profile.serverAddress,
                    username = profile.username,
                    port = profile.port,
                    hubName = profile.hubName,
                    password = if (includePassword) profile.password else null,
                    useTls = profile.useTls,
                    verifyServerCertificate = profile.verifyServerCertificate,
                    autoConnect = profile.autoConnect,
                    autoConnectOnWifi = profile.autoConnectOnWifi,
                    autoConnectOnMobile = profile.autoConnectOnMobile,
                    splitTunneling = profile.splitTunneling,
                    excludedApps = profile.excludedApps,
                    includedApps = profile.includedApps,
                    customDns = profile.customDns,
                )
            }
        }
    }

    /**
     * Data class for importing profiles (allows optional fields).
     */
    @Serializable
    private data class ImportableProfile(
        val name: String? = null,
        val serverAddress: String? = null,
        val username: String? = null,
        val port: Int? = null,
        val hubName: String? = null,
        val password: String? = null,
        val useTls: Boolean? = null,
        val verifyServerCertificate: Boolean? = null,
        val autoConnect: Boolean? = null,
        val autoConnectOnWifi: Boolean? = null,
        val autoConnectOnMobile: Boolean? = null,
        val splitTunneling: Boolean? = null,
        val excludedApps: List<String>? = null,
        val includedApps: List<String>? = null,
        val customDns: String? = null,
    ) {
        fun toVpnProfile(): VpnProfile {
            return VpnProfile(
                id = 0L,
                name = name ?: "",
                serverAddress = serverAddress ?: "",
                username = username ?: "",
                port = port ?: VpnProfile.DEFAULT_PORT,
                hubName = hubName ?: VpnProfile.DEFAULT_HUB_NAME,
                password = password ?: "",
                useTls = useTls ?: true,
                verifyServerCertificate = verifyServerCertificate ?: true,
                autoConnect = autoConnect ?: false,
                autoConnectOnWifi = autoConnectOnWifi ?: false,
                autoConnectOnMobile = autoConnectOnMobile ?: false,
                splitTunneling = splitTunneling ?: false,
                excludedApps = excludedApps ?: emptyList(),
                includedApps = includedApps ?: emptyList(),
                customDns = customDns,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
            )
        }
    }
}
