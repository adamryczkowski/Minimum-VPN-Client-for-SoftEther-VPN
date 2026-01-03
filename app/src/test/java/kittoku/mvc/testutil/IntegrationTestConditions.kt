package kittoku.mvc.testutil

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit 5 condition that enables tests only when VPN test environment is configured.
 *
 * Tests annotated with [@RequiresVpnTestEnvironment][RequiresVpnTestEnvironment] will only run
 * when the following environment variables are set:
 * - TEST_HOST
 * - TEST_USERNAME
 * - TEST_PASSWORD
 *
 * This allows integration tests to be skipped in CI/CD environments where
 * a live VPN server is not available.
 */
class VpnTestEnvironmentCondition : ExecutionCondition {
    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val host = System.getenv("TEST_HOST")
        val username = System.getenv("TEST_USERNAME")
        val password = System.getenv("TEST_PASSWORD")

        return if (!host.isNullOrBlank() && !username.isNullOrBlank() && !password.isNullOrBlank()) {
            ConditionEvaluationResult.enabled("VPN test environment is configured")
        } else {
            val missing =
                buildList {
                    if (host.isNullOrBlank()) add("TEST_HOST")
                    if (username.isNullOrBlank()) add("TEST_USERNAME")
                    if (password.isNullOrBlank()) add("TEST_PASSWORD")
                }
            ConditionEvaluationResult.disabled(
                "VPN test environment not configured. Missing: ${missing.joinToString(", ")}. " +
                    "Set environment variables or copy .env.example to .env",
            )
        }
    }
}

/**
 * Annotation to mark tests that require a live VPN server connection.
 *
 * Tests with this annotation will be skipped if the VPN test environment
 * is not configured (i.e., TEST_HOST, TEST_USERNAME, TEST_PASSWORD are not set).
 *
 * Usage:
 * ```kotlin
 * @RequiresVpnTestEnvironment
 * @Test
 * fun `test VPN connection`() {
 *     // This test will only run when env vars are set
 * }
 * ```
 *
 * Can also be applied at the class level:
 * ```kotlin
 * @RequiresVpnTestEnvironment
 * class VpnIntegrationTests {
 *     @Test
 *     fun `test connection`() { ... }
 * }
 * ```
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(VpnTestEnvironmentCondition::class)
annotation class RequiresVpnTestEnvironment

/**
 * Tag for integration tests.
 *
 * Use this tag to categorize tests that require external resources
 * (like a live VPN server) and should be run separately from unit tests.
 *
 * Usage:
 * ```kotlin
 * @IntegrationTest
 * @Test
 * fun `test full connection flow`() { ... }
 * ```
 *
 * Run only integration tests:
 * ```bash
 * ./gradlew test --tests "*IntegrationTest*"
 * ```
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@org.junit.jupiter.api.Tag("integration")
annotation class IntegrationTest
