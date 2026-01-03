package kittoku.mvc.testutil

import kittoku.mvc.extension.read
import kittoku.mvc.service.client.ClientBridge
import kittoku.mvc.service.client.UDPAccelerationConfig
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Factory for creating [ClientBridge] instances configured for testing.
 *
 * This utility provides pre-configured bridges that can be used in unit tests
 * without requiring a live VPN server connection.
 *
 * Usage:
 * ```kotlin
 * val bridge = TestClientBridge.create()
 * // or with custom configuration
 * val bridge = TestClientBridge.create(
 *     hostname = "test.example.com",
 *     port = 443,
 *     username = "testuser",
 *     password = "testpass"
 * )
 * ```
 */
object TestClientBridge {
    /**
     * Default test MAC address used for testing.
     */
    private val DEFAULT_MAC_ADDRESS =
        byteArrayOf(
            0x5E,
            11,
            23,
            58,
            13,
            21,
        )

    /**
     * Creates a [ClientBridge] configured for testing.
     *
     * @param scope The coroutine scope to use. Defaults to IO dispatcher with SupervisorJob.
     * @param hostname The VPN server hostname. Defaults to environment variable or empty string.
     * @param port The VPN server port. Defaults to environment variable or 443.
     * @param username The VPN username. Defaults to environment variable or empty string.
     * @param password The VPN password. Defaults to environment variable or empty string.
     * @param macAddress The client MAC address. Defaults to a fixed test MAC address.
     * @param enableUdpAcceleration Whether to enable UDP acceleration. Defaults to false.
     * @param exceptionHandler Custom exception handler. Defaults to rethrowing exceptions.
     * @return A configured [ClientBridge] instance.
     */
    internal fun create(
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        hostname: String = System.getenv("TEST_HOST") ?: "",
        port: Int = System.getenv("TEST_PORT")?.toIntOrNull() ?: 443,
        username: String = System.getenv("TEST_USERNAME") ?: "",
        password: String = System.getenv("TEST_PASSWORD") ?: "",
        macAddress: ByteArray = DEFAULT_MAC_ADDRESS,
        enableUdpAcceleration: Boolean = false,
        exceptionHandler: CoroutineExceptionHandler =
            CoroutineExceptionHandler { _, exception ->
                throw exception
            },
    ): ClientBridge {
        return ClientBridge(scope, exceptionHandler).also { bridge ->
            bridge.isTest = true
            bridge.serverHostname = hostname
            bridge.serverPort = port
            bridge.clientUsername = username
            bridge.clientPassword = password
            bridge.clientMacAddress.read(macAddress)

            if (enableUdpAcceleration) {
                bridge.udpAccelerationConfig = UDPAccelerationConfig(bridge.random)
            }
        }
    }

    /**
     * Creates a [ClientBridge] with mock-friendly defaults for unit testing.
     *
     * This creates a bridge with minimal configuration that doesn't require
     * any environment variables or network access.
     *
     * @param scope The coroutine scope to use.
     * @return A minimally configured [ClientBridge] instance.
     */
    internal fun createMinimal(
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    ): ClientBridge {
        return create(
            scope = scope,
            hostname = "localhost",
            port = 443,
            username = "test",
            password = "test",
        )
    }

    /**
     * Creates a [ClientBridge] configured from environment variables.
     *
     * This is useful for integration tests that need to connect to a real server.
     * Throws [IllegalStateException] if required environment variables are not set.
     *
     * @param scope The coroutine scope to use.
     * @param enableUdpAcceleration Whether to enable UDP acceleration.
     * @return A configured [ClientBridge] instance.
     * @throws IllegalStateException if TEST_HOST, TEST_USERNAME, or TEST_PASSWORD are not set.
     */
    internal fun createFromEnvironment(
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        enableUdpAcceleration: Boolean = false,
    ): ClientBridge {
        val hostname =
            System.getenv("TEST_HOST")
                ?: throw IllegalStateException("TEST_HOST environment variable not set")
        val username =
            System.getenv("TEST_USERNAME")
                ?: throw IllegalStateException("TEST_USERNAME environment variable not set")
        val password =
            System.getenv("TEST_PASSWORD")
                ?: throw IllegalStateException("TEST_PASSWORD environment variable not set")

        return create(
            scope = scope,
            hostname = hostname,
            port = System.getenv("TEST_PORT")?.toIntOrNull() ?: 443,
            username = username,
            password = password,
            enableUdpAcceleration = enableUdpAcceleration,
        )
    }
}
