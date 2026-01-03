package kittoku.mvc.testutil

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before

/**
 * Base class for instrumented tests that run on an Android device or emulator.
 *
 * Provides common setup and utilities for Android instrumented tests:
 * - Access to application and instrumentation contexts
 * - Common test lifecycle hooks
 *
 * Usage:
 * ```kotlin
 * class MyInstrumentedTest : BaseInstrumentedTest() {
 *     @Test
 *     fun testSomething() {
 *         // Use context, appContext, or instrumentation
 *         assertThat(appContext.packageName).isEqualTo("kittoku.mvc")
 *     }
 * }
 * ```
 */
abstract class BaseInstrumentedTest {
    /**
     * The application context.
     */
    protected lateinit var appContext: Context

    /**
     * The instrumentation context (test APK context).
     */
    protected lateinit var testContext: Context

    /**
     * The instrumentation instance.
     */
    protected val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    @Before
    open fun baseSetUp() {
        appContext = ApplicationProvider.getApplicationContext()
        testContext = instrumentation.context
    }

    /**
     * Runs the given block on the UI thread and waits for it to complete.
     */
    protected fun runOnUiThread(block: () -> Unit) {
        instrumentation.runOnMainSync(block)
    }

    /**
     * Waits for the UI to become idle.
     */
    protected fun waitForIdle() {
        instrumentation.waitForIdleSync()
    }
}
