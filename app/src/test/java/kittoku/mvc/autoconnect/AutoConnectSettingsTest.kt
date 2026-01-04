package kittoku.mvc.autoconnect

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for AutoConnectSettings - manages auto-connect preferences.
 */
@DisplayName("AutoConnectSettings")
class AutoConnectSettingsTest {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var settings: AutoConnectSettings

    @BeforeEach
    fun setUp() {
        sharedPreferences = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        every { sharedPreferences.edit() } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putStringSet(any(), any()) } returns editor
        settings = AutoConnectSettings(sharedPreferences)
    }

    @Nested
    @DisplayName("Connect on boot")
    inner class ConnectOnBootTests {
        @Test
        @DisplayName("should return false by default")
        fun shouldReturnFalseByDefault() {
            every {
                sharedPreferences.getBoolean(
                    AutoConnectSettings.KEY_CONNECT_ON_BOOT,
                    false,
                )
            } returns false

            assertFalse(settings.isConnectOnBootEnabled())
        }

        @Test
        @DisplayName("should return true when enabled")
        fun shouldReturnTrueWhenEnabled() {
            every {
                sharedPreferences.getBoolean(
                    AutoConnectSettings.KEY_CONNECT_ON_BOOT,
                    false,
                )
            } returns true

            assertTrue(settings.isConnectOnBootEnabled())
        }

        @Test
        @DisplayName("should save connect on boot setting")
        fun shouldSaveConnectOnBootSetting() {
            settings.setConnectOnBootEnabled(true)

            verify { editor.putBoolean(AutoConnectSettings.KEY_CONNECT_ON_BOOT, true) }
            verify { editor.apply() }
        }
    }

    @Nested
    @DisplayName("Default profile")
    inner class DefaultProfileTests {
        @Test
        @DisplayName("should return null when no default profile is set")
        fun shouldReturnNullWhenNoDefaultProfile() {
            every {
                sharedPreferences.getLong(
                    AutoConnectSettings.KEY_DEFAULT_PROFILE_ID,
                    -1L,
                )
            } returns -1L

            assertNull(settings.getDefaultProfileId())
        }

        @Test
        @DisplayName("should return profile ID when set")
        fun shouldReturnProfileIdWhenSet() {
            every {
                sharedPreferences.getLong(
                    AutoConnectSettings.KEY_DEFAULT_PROFILE_ID,
                    -1L,
                )
            } returns 42L

            assertEquals(42L, settings.getDefaultProfileId())
        }

        @Test
        @DisplayName("should save default profile ID")
        fun shouldSaveDefaultProfileId() {
            settings.setDefaultProfileId(42L)

            verify { editor.putLong(AutoConnectSettings.KEY_DEFAULT_PROFILE_ID, 42L) }
            verify { editor.apply() }
        }

        @Test
        @DisplayName("should clear default profile ID when set to null")
        fun shouldClearDefaultProfileId() {
            settings.setDefaultProfileId(null)

            verify { editor.putLong(AutoConnectSettings.KEY_DEFAULT_PROFILE_ID, -1L) }
            verify { editor.apply() }
        }
    }

    @Nested
    @DisplayName("Auto-reconnect")
    inner class AutoReconnectTests {
        @Test
        @DisplayName("should return false by default for auto-reconnect")
        fun shouldReturnFalseByDefaultForAutoReconnect() {
            every {
                sharedPreferences.getBoolean(
                    AutoConnectSettings.KEY_AUTO_RECONNECT,
                    false,
                )
            } returns false

            assertFalse(settings.isAutoReconnectEnabled())
        }

        @Test
        @DisplayName("should return true when auto-reconnect is enabled")
        fun shouldReturnTrueWhenAutoReconnectEnabled() {
            every {
                sharedPreferences.getBoolean(
                    AutoConnectSettings.KEY_AUTO_RECONNECT,
                    false,
                )
            } returns true

            assertTrue(settings.isAutoReconnectEnabled())
        }

        @Test
        @DisplayName("should save auto-reconnect setting")
        fun shouldSaveAutoReconnectSetting() {
            settings.setAutoReconnectEnabled(true)

            verify { editor.putBoolean(AutoConnectSettings.KEY_AUTO_RECONNECT, true) }
            verify { editor.apply() }
        }
    }

    @Nested
    @DisplayName("Reconnect after update")
    inner class ReconnectAfterUpdateTests {
        @Test
        @DisplayName("should return false by default")
        fun shouldReturnFalseByDefault() {
            every {
                sharedPreferences.getBoolean(
                    AutoConnectSettings.KEY_RECONNECT_AFTER_UPDATE,
                    false,
                )
            } returns false

            assertFalse(settings.isReconnectAfterUpdateEnabled())
        }

        @Test
        @DisplayName("should return true when enabled")
        fun shouldReturnTrueWhenEnabled() {
            every {
                sharedPreferences.getBoolean(
                    AutoConnectSettings.KEY_RECONNECT_AFTER_UPDATE,
                    false,
                )
            } returns true

            assertTrue(settings.isReconnectAfterUpdateEnabled())
        }

        @Test
        @DisplayName("should save reconnect after update setting")
        fun shouldSaveReconnectAfterUpdateSetting() {
            settings.setReconnectAfterUpdateEnabled(true)

            verify { editor.putBoolean(AutoConnectSettings.KEY_RECONNECT_AFTER_UPDATE, true) }
            verify { editor.apply() }
        }
    }

    @Nested
    @DisplayName("Trusted networks")
    inner class TrustedNetworksTests {
        @Test
        @DisplayName("should return empty set by default")
        fun shouldReturnEmptySetByDefault() {
            every {
                sharedPreferences.getStringSet(
                    AutoConnectSettings.KEY_TRUSTED_NETWORKS,
                    emptySet(),
                )
            } returns emptySet()

            assertTrue(settings.getTrustedNetworks().isEmpty())
        }

        @Test
        @DisplayName("should return trusted networks when set")
        fun shouldReturnTrustedNetworksWhenSet() {
            val networks = setOf("HomeWiFi", "OfficeWiFi")
            every {
                sharedPreferences.getStringSet(
                    AutoConnectSettings.KEY_TRUSTED_NETWORKS,
                    emptySet(),
                )
            } returns networks

            assertEquals(networks, settings.getTrustedNetworks())
        }

        @Test
        @DisplayName("should save trusted networks")
        fun shouldSaveTrustedNetworks() {
            val networks = setOf("HomeWiFi", "OfficeWiFi")
            settings.setTrustedNetworks(networks)

            verify { editor.putStringSet(AutoConnectSettings.KEY_TRUSTED_NETWORKS, networks) }
            verify { editor.apply() }
        }

        @Test
        @DisplayName("should add trusted network")
        fun shouldAddTrustedNetwork() {
            val existingNetworks = setOf("HomeWiFi")
            every {
                sharedPreferences.getStringSet(
                    AutoConnectSettings.KEY_TRUSTED_NETWORKS,
                    emptySet(),
                )
            } returns existingNetworks

            settings.addTrustedNetwork("OfficeWiFi")

            verify {
                editor.putStringSet(
                    AutoConnectSettings.KEY_TRUSTED_NETWORKS,
                    setOf("HomeWiFi", "OfficeWiFi"),
                )
            }
            verify { editor.apply() }
        }

        @Test
        @DisplayName("should remove trusted network")
        fun shouldRemoveTrustedNetwork() {
            val existingNetworks = setOf("HomeWiFi", "OfficeWiFi")
            every {
                sharedPreferences.getStringSet(
                    AutoConnectSettings.KEY_TRUSTED_NETWORKS,
                    emptySet(),
                )
            } returns existingNetworks

            settings.removeTrustedNetwork("OfficeWiFi")

            verify {
                editor.putStringSet(
                    AutoConnectSettings.KEY_TRUSTED_NETWORKS,
                    setOf("HomeWiFi"),
                )
            }
            verify { editor.apply() }
        }

        @Test
        @DisplayName("should check if network is trusted")
        fun shouldCheckIfNetworkIsTrusted() {
            val networks = setOf("HomeWiFi", "OfficeWiFi")
            every {
                sharedPreferences.getStringSet(
                    AutoConnectSettings.KEY_TRUSTED_NETWORKS,
                    emptySet(),
                )
            } returns networks

            assertTrue(settings.isNetworkTrusted("HomeWiFi"))
            assertTrue(settings.isNetworkTrusted("OfficeWiFi"))
            assertFalse(settings.isNetworkTrusted("CoffeeShopWiFi"))
        }
    }

    @Nested
    @DisplayName("Trusted networks enabled")
    inner class TrustedNetworksEnabledTests {
        @Test
        @DisplayName("should return false by default")
        fun shouldReturnFalseByDefault() {
            every {
                sharedPreferences.getBoolean(
                    AutoConnectSettings.KEY_TRUSTED_NETWORKS_ENABLED,
                    false,
                )
            } returns false

            assertFalse(settings.isTrustedNetworksEnabled())
        }

        @Test
        @DisplayName("should return true when enabled")
        fun shouldReturnTrueWhenEnabled() {
            every {
                sharedPreferences.getBoolean(
                    AutoConnectSettings.KEY_TRUSTED_NETWORKS_ENABLED,
                    false,
                )
            } returns true

            assertTrue(settings.isTrustedNetworksEnabled())
        }

        @Test
        @DisplayName("should save trusted networks enabled setting")
        fun shouldSaveTrustedNetworksEnabledSetting() {
            settings.setTrustedNetworksEnabled(true)

            verify { editor.putBoolean(AutoConnectSettings.KEY_TRUSTED_NETWORKS_ENABLED, true) }
            verify { editor.apply() }
        }
    }
}
