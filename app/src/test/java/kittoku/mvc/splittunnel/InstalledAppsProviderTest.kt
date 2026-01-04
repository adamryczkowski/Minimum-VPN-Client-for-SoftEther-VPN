package kittoku.mvc.splittunnel

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for InstalledAppsProvider - provides list of installed apps for split tunneling.
 */
@DisplayName("InstalledAppsProvider")
class InstalledAppsProviderTest {
    private lateinit var packageManagerWrapper: PackageManagerWrapper
    private lateinit var provider: InstalledAppsProvider

    @BeforeEach
    fun setUp() {
        packageManagerWrapper = mockk(relaxed = true)
        provider = InstalledAppsProviderImpl(packageManagerWrapper)
    }

    @Nested
    @DisplayName("getInstalledApps")
    inner class GetInstalledAppsTests {
        @Test
        @DisplayName("should return empty list when no apps installed")
        fun shouldReturnEmptyListWhenNoAppsInstalled() {
            every { packageManagerWrapper.getInstalledApplications() } returns emptyList()

            val apps = provider.getInstalledApps()

            assertTrue(apps.isEmpty())
        }

        @Test
        @DisplayName("should return list of installed apps")
        fun shouldReturnListOfInstalledApps() {
            val appInfo1 = createApplicationInfo("com.example.app1", 0)
            val appInfo2 = createApplicationInfo("com.example.app2", 0)
            every { packageManagerWrapper.getInstalledApplications() } returns listOf(appInfo1, appInfo2)
            every { packageManagerWrapper.getApplicationLabel(appInfo1) } returns "App 1"
            every { packageManagerWrapper.getApplicationLabel(appInfo2) } returns "App 2"

            val apps = provider.getInstalledApps()

            assertEquals(2, apps.size)
            assertEquals("com.example.app1", apps[0].packageName)
            assertEquals("App 1", apps[0].appName)
            assertEquals("com.example.app2", apps[1].packageName)
            assertEquals("App 2", apps[1].appName)
        }

        @Test
        @DisplayName("should filter out system apps when requested")
        fun shouldFilterOutSystemAppsWhenRequested() {
            val userApp = createApplicationInfo("com.example.userapp", 0)
            val systemApp = createApplicationInfo("com.android.system", ApplicationInfo.FLAG_SYSTEM)
            every { packageManagerWrapper.getInstalledApplications() } returns listOf(userApp, systemApp)
            every { packageManagerWrapper.getApplicationLabel(userApp) } returns "User App"
            every { packageManagerWrapper.getApplicationLabel(systemApp) } returns "System App"

            val apps = provider.getInstalledApps(includeSystemApps = false)

            assertEquals(1, apps.size)
            assertEquals("com.example.userapp", apps[0].packageName)
        }

        @Test
        @DisplayName("should include system apps when requested")
        fun shouldIncludeSystemAppsWhenRequested() {
            val userApp = createApplicationInfo("com.example.userapp", 0)
            val systemApp = createApplicationInfo("com.android.system", ApplicationInfo.FLAG_SYSTEM)
            every { packageManagerWrapper.getInstalledApplications() } returns listOf(userApp, systemApp)
            every { packageManagerWrapper.getApplicationLabel(userApp) } returns "User App"
            every { packageManagerWrapper.getApplicationLabel(systemApp) } returns "System App"

            val apps = provider.getInstalledApps(includeSystemApps = true)

            assertEquals(2, apps.size)
        }

        @Test
        @DisplayName("should sort apps alphabetically by name")
        fun shouldSortAppsAlphabeticallyByName() {
            val appZ = createApplicationInfo("com.example.z", 0)
            val appA = createApplicationInfo("com.example.a", 0)
            val appM = createApplicationInfo("com.example.m", 0)
            every { packageManagerWrapper.getInstalledApplications() } returns listOf(appZ, appA, appM)
            every { packageManagerWrapper.getApplicationLabel(appZ) } returns "Zebra App"
            every { packageManagerWrapper.getApplicationLabel(appA) } returns "Alpha App"
            every { packageManagerWrapper.getApplicationLabel(appM) } returns "Middle App"

            val apps = provider.getInstalledApps()

            assertEquals("Alpha App", apps[0].appName)
            assertEquals("Middle App", apps[1].appName)
            assertEquals("Zebra App", apps[2].appName)
        }

        @Test
        @DisplayName("should handle apps with empty labels")
        fun shouldHandleAppsWithEmptyLabels() {
            val appWithEmptyLabel = createApplicationInfo("com.example.nolabel", 0)
            every { packageManagerWrapper.getInstalledApplications() } returns listOf(appWithEmptyLabel)
            every { packageManagerWrapper.getApplicationLabel(appWithEmptyLabel) } returns ""

            val apps = provider.getInstalledApps()

            assertEquals(1, apps.size)
            assertEquals("com.example.nolabel", apps[0].packageName)
            // Should use package name as fallback when label is empty
            assertEquals("com.example.nolabel", apps[0].appName)
        }

        @Test
        @DisplayName("should exclude own app package")
        fun shouldExcludeOwnAppPackage() {
            val ownApp = createApplicationInfo("kittoku.mvc", 0)
            val otherApp = createApplicationInfo("com.example.other", 0)
            every { packageManagerWrapper.getInstalledApplications() } returns listOf(ownApp, otherApp)
            every { packageManagerWrapper.getApplicationLabel(ownApp) } returns "SoftEther Connect"
            every { packageManagerWrapper.getApplicationLabel(otherApp) } returns "Other App"
            every { packageManagerWrapper.getOwnPackageName() } returns "kittoku.mvc"

            val apps = provider.getInstalledApps()

            assertEquals(1, apps.size)
            assertEquals("com.example.other", apps[0].packageName)
        }
    }

    @Nested
    @DisplayName("getAppInfo")
    inner class GetAppInfoTests {
        @Test
        @DisplayName("should return app info for valid package name")
        fun shouldReturnAppInfoForValidPackageName() {
            val appInfo = createApplicationInfo("com.example.app", 0)
            every { packageManagerWrapper.getApplicationInfo("com.example.app") } returns appInfo
            every { packageManagerWrapper.getApplicationLabel(appInfo) } returns "Test App"

            val result = provider.getAppInfo("com.example.app")

            assertNotNull(result)
            assertEquals("com.example.app", result?.packageName)
            assertEquals("Test App", result?.appName)
        }

        @Test
        @DisplayName("should return null for non-existent package")
        fun shouldReturnNullForNonExistentPackage() {
            every {
                packageManagerWrapper.getApplicationInfo("com.nonexistent.app")
            } throws PackageManager.NameNotFoundException()

            val result = provider.getAppInfo("com.nonexistent.app")

            assertEquals(null, result)
        }
    }

    @Nested
    @DisplayName("isAppInstalled")
    inner class IsAppInstalledTests {
        @Test
        @DisplayName("should return true for installed app")
        fun shouldReturnTrueForInstalledApp() {
            val appInfo = createApplicationInfo("com.example.app", 0)
            every { packageManagerWrapper.getApplicationInfo("com.example.app") } returns appInfo

            assertTrue(provider.isAppInstalled("com.example.app"))
        }

        @Test
        @DisplayName("should return false for non-installed app")
        fun shouldReturnFalseForNonInstalledApp() {
            every {
                packageManagerWrapper.getApplicationInfo("com.nonexistent.app")
            } throws PackageManager.NameNotFoundException()

            assertFalse(provider.isAppInstalled("com.nonexistent.app"))
        }
    }

    @Nested
    @DisplayName("getAppIcon")
    inner class GetAppIconTests {
        @Test
        @DisplayName("should return icon for valid package")
        fun shouldReturnIconForValidPackage() {
            val mockDrawable = mockk<Drawable>()
            every { packageManagerWrapper.getApplicationIcon("com.example.app") } returns mockDrawable

            val icon = provider.getAppIcon("com.example.app")

            assertEquals(mockDrawable, icon)
        }

        @Test
        @DisplayName("should return null for non-existent package")
        fun shouldReturnNullForNonExistentPackage() {
            every {
                packageManagerWrapper.getApplicationIcon("com.nonexistent.app")
            } throws PackageManager.NameNotFoundException()

            val icon = provider.getAppIcon("com.nonexistent.app")

            assertEquals(null, icon)
        }
    }

    /**
     * Creates a real ApplicationInfo object with the given package name and flags.
     * We use real objects instead of mocks because ApplicationInfo.packageName is a field,
     * not a property, and MockK cannot mock fields.
     */
    private fun createApplicationInfo(
        packageName: String,
        flags: Int,
    ): ApplicationInfo {
        return ApplicationInfo().apply {
            this.packageName = packageName
            this.flags = flags
        }
    }
}
