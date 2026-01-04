package kittoku.mvc.autoconnect

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Tests for NetworkMonitor - monitors network connectivity changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("NetworkMonitor")
class NetworkMonitorTest {
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkMonitor: NetworkMonitor

    @BeforeEach
    fun setUp() {
        connectivityManager = mockk(relaxed = true)
        networkMonitor = NetworkMonitor(connectivityManager)
    }

    @Nested
    @DisplayName("Network state")
    inner class NetworkStateTests {
        @Test
        @DisplayName("should emit disconnected state initially")
        fun shouldEmitDisconnectedStateInitially() =
            runTest {
                val state = networkMonitor.networkState.first()
                assertEquals(NetworkState.Disconnected, state)
            }

        @Test
        @DisplayName("should emit connected state when network becomes available")
        fun shouldEmitConnectedStateWhenNetworkAvailable() =
            runTest {
                val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
                every {
                    connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot))
                } answers {}

                networkMonitor.startMonitoring()

                val network = mockk<Network>()
                callbackSlot.captured.onAvailable(network)

                val state = networkMonitor.networkState.first()
                assertTrue(state is NetworkState.Connected)
            }

        @Test
        @DisplayName("should emit disconnected state when network is lost")
        fun shouldEmitDisconnectedStateWhenNetworkLost() =
            runTest {
                val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
                every {
                    connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot))
                } answers {}

                networkMonitor.startMonitoring()

                val network = mockk<Network>()
                callbackSlot.captured.onAvailable(network)
                callbackSlot.captured.onLost(network)

                val state = networkMonitor.networkState.first()
                assertEquals(NetworkState.Disconnected, state)
            }
    }

    @Nested
    @DisplayName("Network type detection")
    inner class NetworkTypeDetectionTests {
        @Test
        @DisplayName("should detect WiFi network")
        fun shouldDetectWifiNetwork() =
            runTest {
                val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
                every {
                    connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot))
                } answers {}

                networkMonitor.startMonitoring()

                val network = mockk<Network>()
                val capabilities = mockk<NetworkCapabilities>()
                every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
                every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
                every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false

                callbackSlot.captured.onAvailable(network)
                callbackSlot.captured.onCapabilitiesChanged(network, capabilities)

                val state = networkMonitor.networkState.first()
                assertTrue(state is NetworkState.Connected)
                assertEquals(NetworkType.WIFI, (state as NetworkState.Connected).type)
            }

        @Test
        @DisplayName("should detect cellular network")
        fun shouldDetectCellularNetwork() =
            runTest {
                val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
                every {
                    connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot))
                } answers {}

                networkMonitor.startMonitoring()

                val network = mockk<Network>()
                val capabilities = mockk<NetworkCapabilities>()
                every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
                every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
                every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false

                callbackSlot.captured.onAvailable(network)
                callbackSlot.captured.onCapabilitiesChanged(network, capabilities)

                val state = networkMonitor.networkState.first()
                assertTrue(state is NetworkState.Connected)
                assertEquals(NetworkType.CELLULAR, (state as NetworkState.Connected).type)
            }

        @Test
        @DisplayName("should detect ethernet network")
        fun shouldDetectEthernetNetwork() =
            runTest {
                val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
                every {
                    connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot))
                } answers {}

                networkMonitor.startMonitoring()

                val network = mockk<Network>()
                val capabilities = mockk<NetworkCapabilities>()
                every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
                every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
                every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns true

                callbackSlot.captured.onAvailable(network)
                callbackSlot.captured.onCapabilitiesChanged(network, capabilities)

                val state = networkMonitor.networkState.first()
                assertTrue(state is NetworkState.Connected)
                assertEquals(NetworkType.ETHERNET, (state as NetworkState.Connected).type)
            }

        @Test
        @DisplayName("should detect unknown network type")
        fun shouldDetectUnknownNetworkType() =
            runTest {
                val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
                every {
                    connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot))
                } answers {}

                networkMonitor.startMonitoring()

                val network = mockk<Network>()
                val capabilities = mockk<NetworkCapabilities>()
                every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
                every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
                every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false

                callbackSlot.captured.onAvailable(network)
                callbackSlot.captured.onCapabilitiesChanged(network, capabilities)

                val state = networkMonitor.networkState.first()
                assertTrue(state is NetworkState.Connected)
                assertEquals(NetworkType.UNKNOWN, (state as NetworkState.Connected).type)
            }
    }

    @Nested
    @DisplayName("Monitoring lifecycle")
    inner class MonitoringLifecycleTests {
        @Test
        @DisplayName("should register callback when starting monitoring")
        fun shouldRegisterCallbackWhenStartingMonitoring() {
            networkMonitor.startMonitoring()

            verify { connectivityManager.registerDefaultNetworkCallback(any()) }
        }

        @Test
        @DisplayName("should unregister callback when stopping monitoring")
        fun shouldUnregisterCallbackWhenStoppingMonitoring() {
            val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
            every {
                connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot))
            } answers {}

            networkMonitor.startMonitoring()
            networkMonitor.stopMonitoring()

            verify { connectivityManager.unregisterNetworkCallback(callbackSlot.captured) }
        }

        @Test
        @DisplayName("should not register callback twice")
        fun shouldNotRegisterCallbackTwice() {
            networkMonitor.startMonitoring()
            networkMonitor.startMonitoring()

            verify(exactly = 1) { connectivityManager.registerDefaultNetworkCallback(any()) }
        }

        @Test
        @DisplayName("should handle stop without start gracefully")
        fun shouldHandleStopWithoutStartGracefully() {
            // Should not throw
            networkMonitor.stopMonitoring()

            verify(exactly = 0) { connectivityManager.unregisterNetworkCallback(any<ConnectivityManager.NetworkCallback>()) }
        }
    }

    @Nested
    @DisplayName("isConnected")
    inner class IsConnectedTests {
        @Test
        @DisplayName("should return false when disconnected")
        fun shouldReturnFalseWhenDisconnected() {
            assertFalse(networkMonitor.isConnected())
        }

        @Test
        @DisplayName("should return true when connected")
        fun shouldReturnTrueWhenConnected() =
            runTest {
                val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
                every {
                    connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot))
                } answers {}

                networkMonitor.startMonitoring()

                val network = mockk<Network>()
                callbackSlot.captured.onAvailable(network)

                assertTrue(networkMonitor.isConnected())
            }
    }

    @Nested
    @DisplayName("getCurrentNetworkType")
    inner class GetCurrentNetworkTypeTests {
        @Test
        @DisplayName("should return null when disconnected")
        fun shouldReturnNullWhenDisconnected() {
            assertNull(networkMonitor.getCurrentNetworkType())
        }

        @Test
        @DisplayName("should return network type when connected")
        fun shouldReturnNetworkTypeWhenConnected() =
            runTest {
                val callbackSlot = slot<ConnectivityManager.NetworkCallback>()
                every {
                    connectivityManager.registerDefaultNetworkCallback(capture(callbackSlot))
                } answers {}

                networkMonitor.startMonitoring()

                val network = mockk<Network>()
                val capabilities = mockk<NetworkCapabilities>()
                every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
                every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
                every { capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false

                callbackSlot.captured.onAvailable(network)
                callbackSlot.captured.onCapabilitiesChanged(network, capabilities)

                assertEquals(NetworkType.WIFI, networkMonitor.getCurrentNetworkType())
            }
    }
}
