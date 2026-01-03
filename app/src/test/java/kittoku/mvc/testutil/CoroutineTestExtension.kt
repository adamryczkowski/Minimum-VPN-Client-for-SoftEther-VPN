package kittoku.mvc.testutil

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

/**
 * JUnit 5 extension for testing coroutines.
 *
 * This extension sets up the Main dispatcher to use a [TestDispatcher],
 * allowing coroutine tests to run synchronously and predictably.
 *
 * Usage with annotation:
 * ```kotlin
 * @ExtendWith(CoroutineTestExtension::class)
 * class MyTest {
 *     @Test
 *     fun `test coroutine`(testScope: TestScope) = testScope.runTest {
 *         // Test code here
 *     }
 * }
 * ```
 *
 * Usage with manual registration:
 * ```kotlin
 * class MyTest {
 *     companion object {
 *         @JvmField
 *         @RegisterExtension
 *         val coroutineExtension = CoroutineTestExtension()
 *     }
 *
 *     @Test
 *     fun `test coroutine`() = coroutineExtension.testScope.runTest {
 *         // Test code here
 *     }
 * }
 * ```
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CoroutineTestExtension(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : BeforeEachCallback, AfterEachCallback, ParameterResolver {
    /**
     * The test scope that can be used for running coroutine tests.
     */
    val testScope: TestScope = TestScope(testDispatcher)

    override fun beforeEach(context: ExtensionContext) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun afterEach(context: ExtensionContext) {
        Dispatchers.resetMain()
    }

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Boolean {
        return parameterContext.parameter.type == TestScope::class.java ||
            parameterContext.parameter.type == TestDispatcher::class.java
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext,
    ): Any {
        return when (parameterContext.parameter.type) {
            TestScope::class.java -> testScope
            TestDispatcher::class.java -> testDispatcher
            else -> throw IllegalArgumentException(
                "Unsupported parameter type: ${parameterContext.parameter.type}",
            )
        }
    }
}

/**
 * Annotation to enable coroutine testing support.
 *
 * Apply this annotation to test classes that need coroutine testing support.
 *
 * Usage:
 * ```kotlin
 * @CoroutineTest
 * class MyCoroutineTest {
 *     @Test
 *     fun `test something`(testScope: TestScope) = testScope.runTest {
 *         // Test code
 *     }
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@org.junit.jupiter.api.extension.ExtendWith(CoroutineTestExtension::class)
annotation class CoroutineTest
