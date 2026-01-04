package kittoku.mvc.security

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Unit tests for [SecureCredentialStorage].
 *
 * These tests verify the credential storage constants and key naming conventions.
 * Integration tests with actual EncryptedSharedPreferences require Android
 * instrumentation tests.
 */
@DisplayName("SecureCredentialStorage")
class SecureCredentialStorageTest {
    @Nested
    @DisplayName("Keys companion object")
    inner class KeysCompanionObject {
        @Test
        @DisplayName("DEFAULT_USERNAME key is defined correctly")
        fun defaultUsernameKey_isCorrect() {
            assertThat(SecureCredentialStorage.Companion.Keys.DEFAULT_USERNAME)
                .isEqualTo("default_username")
        }

        @Test
        @DisplayName("DEFAULT_PASSWORD key is defined correctly")
        fun defaultPasswordKey_isCorrect() {
            assertThat(SecureCredentialStorage.Companion.Keys.DEFAULT_PASSWORD)
                .isEqualTo("default_password")
        }

        @Test
        @DisplayName("HUB_PASSWORD key is defined correctly")
        fun hubPasswordKey_isCorrect() {
            assertThat(SecureCredentialStorage.Companion.Keys.HUB_PASSWORD)
                .isEqualTo("hub_password")
        }

        @Test
        @DisplayName("CLIENT_CERT key is defined correctly")
        fun clientCertKey_isCorrect() {
            assertThat(SecureCredentialStorage.Companion.Keys.CLIENT_CERT)
                .isEqualTo("client_certificate")
        }

        @Test
        @DisplayName("CLIENT_KEY key is defined correctly")
        fun clientKeyKey_isCorrect() {
            assertThat(SecureCredentialStorage.Companion.Keys.CLIENT_KEY)
                .isEqualTo("client_private_key")
        }

        @Test
        @DisplayName("CA_CERT key is defined correctly")
        fun caCertKey_isCorrect() {
            assertThat(SecureCredentialStorage.Companion.Keys.CA_CERT)
                .isEqualTo("ca_certificate")
        }
    }

    @Nested
    @DisplayName("Key naming conventions")
    inner class KeyNamingConventions {
        @Test
        @DisplayName("All keys use snake_case naming")
        fun allKeys_useSnakeCase() {
            val keys =
                listOf(
                    SecureCredentialStorage.Companion.Keys.DEFAULT_USERNAME,
                    SecureCredentialStorage.Companion.Keys.DEFAULT_PASSWORD,
                    SecureCredentialStorage.Companion.Keys.HUB_PASSWORD,
                    SecureCredentialStorage.Companion.Keys.CLIENT_CERT,
                    SecureCredentialStorage.Companion.Keys.CLIENT_KEY,
                    SecureCredentialStorage.Companion.Keys.CA_CERT,
                )

            keys.forEach { key ->
                // Verify snake_case: lowercase with underscores, no uppercase
                assertThat(key.matches(Regex("[a-z_]+"))).isTrue()
            }
        }

        @Test
        @DisplayName("All keys are unique")
        fun allKeys_areUnique() {
            val keys =
                listOf(
                    SecureCredentialStorage.Companion.Keys.DEFAULT_USERNAME,
                    SecureCredentialStorage.Companion.Keys.DEFAULT_PASSWORD,
                    SecureCredentialStorage.Companion.Keys.HUB_PASSWORD,
                    SecureCredentialStorage.Companion.Keys.CLIENT_CERT,
                    SecureCredentialStorage.Companion.Keys.CLIENT_KEY,
                    SecureCredentialStorage.Companion.Keys.CA_CERT,
                )

            val uniqueKeys = keys.toSet()
            assertThat(uniqueKeys.size).isEqualTo(keys.size)
        }

        @Test
        @DisplayName("Certificate keys contain 'certificate' or 'key' suffix")
        fun certificateKeys_haveProperSuffix() {
            assertThat(SecureCredentialStorage.Companion.Keys.CLIENT_CERT).contains("certificate")
            assertThat(SecureCredentialStorage.Companion.Keys.CLIENT_KEY).contains("key")
            assertThat(SecureCredentialStorage.Companion.Keys.CA_CERT).contains("certificate")
        }

        @Test
        @DisplayName("Password keys contain 'password' suffix")
        fun passwordKeys_haveProperSuffix() {
            assertThat(SecureCredentialStorage.Companion.Keys.DEFAULT_PASSWORD).contains("password")
            assertThat(SecureCredentialStorage.Companion.Keys.HUB_PASSWORD).contains("password")
        }

        @Test
        @DisplayName("Username keys contain 'username' suffix")
        fun usernameKeys_haveProperSuffix() {
            assertThat(SecureCredentialStorage.Companion.Keys.DEFAULT_USERNAME).contains("username")
        }
    }
}
