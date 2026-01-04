package kittoku.mvc.security

import android.content.Context
import android.content.SharedPreferences
import dev.spght.encryptedprefs.EncryptedSharedPreferences
import dev.spght.encryptedprefs.MasterKey

/**
 * Secure credential storage using EncryptedSharedPreferences.
 *
 * This class provides encrypted storage for sensitive VPN credentials
 * such as usernames, passwords, and certificates. All data is encrypted
 * using AES-256 with keys stored in the Android Keystore.
 *
 * Security features:
 * - AES-256-GCM encryption for values
 * - AES-256-SIV encryption for keys (deterministic, allows key lookup)
 * - Keys stored in Android Keystore (hardware-backed on supported devices)
 * - Automatic key rotation not required (Keystore handles security)
 *
 * Note: This implementation uses the ed-george/encrypted-shared-preferences fork
 * (dev.spght:encryptedprefs-ktx) which provides continued support after the
 * deprecation of androidx.security:security-crypto in April 2025.
 *
 * Usage:
 * ```kotlin
 * val storage = SecureCredentialStorage(context)
 * storage.saveCredential("vpn_password", "secret123")
 * val password = storage.getCredential("vpn_password")
 * ```
 *
 * @param context Application context
 */
class SecureCredentialStorage(context: Context) {
    private val masterKey: MasterKey =
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

    private val encryptedPrefs: SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    /**
     * Save a credential securely.
     *
     * @param key Unique identifier for the credential
     * @param value The credential value to store (encrypted)
     */
    fun saveCredential(
        key: String,
        value: String,
    ) {
        encryptedPrefs.edit().putString(key, value).apply()
    }

    /**
     * Retrieve a credential.
     *
     * @param key Unique identifier for the credential
     * @param defaultValue Value to return if credential not found
     * @return The decrypted credential value, or defaultValue if not found
     */
    fun getCredential(
        key: String,
        defaultValue: String? = null,
    ): String? {
        return encryptedPrefs.getString(key, defaultValue)
    }

    /**
     * Check if a credential exists.
     *
     * @param key Unique identifier for the credential
     * @return true if the credential exists
     */
    fun hasCredential(key: String): Boolean {
        return encryptedPrefs.contains(key)
    }

    /**
     * Remove a credential.
     *
     * @param key Unique identifier for the credential
     */
    fun removeCredential(key: String) {
        encryptedPrefs.edit().remove(key).apply()
    }

    /**
     * Clear all stored credentials.
     *
     * WARNING: This will permanently delete all encrypted credentials.
     */
    fun clearAll() {
        encryptedPrefs.edit().clear().apply()
    }

    /**
     * Save VPN profile credentials.
     *
     * @param profileId Unique profile identifier
     * @param username VPN username
     * @param password VPN password
     */
    fun saveProfileCredentials(
        profileId: String,
        username: String,
        password: String,
    ) {
        saveCredential("${KEY_PREFIX_PROFILE}${profileId}_username", username)
        saveCredential("${KEY_PREFIX_PROFILE}${profileId}_password", password)
    }

    /**
     * Get VPN profile username.
     *
     * @param profileId Unique profile identifier
     * @return The username, or null if not found
     */
    fun getProfileUsername(profileId: String): String? {
        return getCredential("${KEY_PREFIX_PROFILE}${profileId}_username")
    }

    /**
     * Get VPN profile password.
     *
     * @param profileId Unique profile identifier
     * @return The password, or null if not found
     */
    fun getProfilePassword(profileId: String): String? {
        return getCredential("${KEY_PREFIX_PROFILE}${profileId}_password")
    }

    /**
     * Remove VPN profile credentials.
     *
     * @param profileId Unique profile identifier
     */
    fun removeProfileCredentials(profileId: String) {
        removeCredential("${KEY_PREFIX_PROFILE}${profileId}_username")
        removeCredential("${KEY_PREFIX_PROFILE}${profileId}_password")
    }

    /**
     * Save a certificate or private key.
     *
     * @param name Certificate/key name
     * @param pemData PEM-encoded certificate or key data
     */
    fun saveCertificate(
        name: String,
        pemData: String,
    ) {
        saveCredential("${KEY_PREFIX_CERT}$name", pemData)
    }

    /**
     * Get a certificate or private key.
     *
     * @param name Certificate/key name
     * @return PEM-encoded data, or null if not found
     */
    fun getCertificate(name: String): String? {
        return getCredential("${KEY_PREFIX_CERT}$name")
    }

    /**
     * Remove a certificate or private key.
     *
     * @param name Certificate/key name
     */
    fun removeCertificate(name: String) {
        removeCredential("${KEY_PREFIX_CERT}$name")
    }

    companion object {
        private const val ENCRYPTED_PREFS_FILE = "secure_vpn_credentials"
        private const val KEY_PREFIX_PROFILE = "profile_"
        private const val KEY_PREFIX_CERT = "cert_"

        /**
         * Credential keys for common VPN settings.
         */
        object Keys {
            const val DEFAULT_USERNAME = "default_username"
            const val DEFAULT_PASSWORD = "default_password"
            const val HUB_PASSWORD = "hub_password"
            const val CLIENT_CERT = "client_certificate"
            const val CLIENT_KEY = "client_private_key"
            const val CA_CERT = "ca_certificate"
        }
    }
}
