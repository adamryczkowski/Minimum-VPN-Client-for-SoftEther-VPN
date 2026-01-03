package kittoku.mvc

import com.google.common.truth.Truth.assertThat
import kittoku.mvc.testutil.BaseInstrumentedTest
import org.junit.Test

/**
 * Basic instrumented test to verify the application context is correctly configured.
 *
 * This test runs on an Android device or emulator and verifies:
 * - The application package name is correct
 * - The application context is available
 */
class ApplicationContextTest : BaseInstrumentedTest() {
    @Test
    fun applicationContext_hasCorrectPackageName() {
        assertThat(appContext.packageName).isEqualTo("kittoku.mvc")
    }

    @Test
    fun applicationContext_isNotNull() {
        assertThat(appContext).isNotNull()
    }

    @Test
    fun testContext_isNotNull() {
        assertThat(testContext).isNotNull()
    }
}
