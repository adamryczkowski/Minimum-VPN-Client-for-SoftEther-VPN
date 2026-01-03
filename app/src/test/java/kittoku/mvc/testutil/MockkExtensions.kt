@file:Suppress("unused")

package kittoku.mvc.testutil

import android.net.VpnService
import android.os.ParcelFileDescriptor
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream

// MockK extensions and utilities for testing VPN-related components.
// These utilities help create mock objects for Android VPN components
// that are difficult to instantiate in unit tests.

/**
 * Creates a mock [VpnService] configured for testing.
 *
 * The mock is configured with sensible defaults for common operations:
 * - [VpnService.Builder] returns a mock builder
 * - [VpnService.protect] returns true
 *
 * Usage:
 * ```kotlin
 * val mockService = createMockVpnService()
 * bridge.service = mockService
 * ```
 *
 * @return A configured mock [VpnService] instance.
 */
fun createMockVpnService(): VpnService {
    val mockBuilder = createMockVpnBuilder()

    return mockk<VpnService>(relaxed = true) {
        every { Builder() } returns mockBuilder
        every { protect(any<Int>()) } returns true
        every { protect(any<java.net.Socket>()) } returns true
        every { protect(any<java.net.DatagramSocket>()) } returns true
    }
}

/**
 * Creates a mock [VpnService.Builder] configured for testing.
 *
 * The mock builder is configured to:
 * - Accept all configuration methods (setSession, addAddress, etc.)
 * - Return a mock [ParcelFileDescriptor] from establish()
 *
 * @return A configured mock [VpnService.Builder] instance.
 */
fun createMockVpnBuilder(): VpnService.Builder {
    val mockPfd = createMockParcelFileDescriptor()

    return mockk<VpnService.Builder>(relaxed = true) {
        every { setSession(any()) } returns this@mockk
        every { addAddress(any<String>(), any()) } returns this@mockk
        every { addRoute(any<String>(), any()) } returns this@mockk
        every { addDnsServer(any<String>()) } returns this@mockk
        every { addSearchDomain(any()) } returns this@mockk
        every { setMtu(any()) } returns this@mockk
        every { setBlocking(any()) } returns this@mockk
        every { establish() } returns mockPfd
    }
}

/**
 * Creates a mock [ParcelFileDescriptor] for testing.
 *
 * This is useful for testing code that interacts with the VPN tunnel
 * file descriptor.
 *
 * @return A configured mock [ParcelFileDescriptor] instance.
 */
fun createMockParcelFileDescriptor(): ParcelFileDescriptor {
    return mockk<ParcelFileDescriptor>(relaxed = true) {
        every { fd } returns 42
        every { fileDescriptor } returns FileDescriptor()
        every { close() } returns Unit
        every { detachFd() } returns 42
    }
}

/**
 * Creates a mock [FileInputStream] for testing.
 *
 * @param data The data to return when reading from the stream.
 * @return A configured mock [FileInputStream] instance.
 */
fun createMockFileInputStream(data: ByteArray = ByteArray(0)): FileInputStream {
    val position = slot<Int>()

    return mockk<FileInputStream>(relaxed = true) {
        every { read(any<ByteArray>()) } answers {
            val buffer = firstArg<ByteArray>()
            val bytesToRead = minOf(buffer.size, data.size)
            data.copyInto(buffer, 0, 0, bytesToRead)
            if (bytesToRead == 0) -1 else bytesToRead
        }
        every { read(any<ByteArray>(), any(), any()) } answers {
            val buffer = firstArg<ByteArray>()
            val offset = secondArg<Int>()
            val length = thirdArg<Int>()
            val bytesToRead = minOf(length, data.size)
            data.copyInto(buffer, offset, 0, bytesToRead)
            if (bytesToRead == 0) -1 else bytesToRead
        }
        every { close() } returns Unit
    }
}

/**
 * Creates a mock [FileOutputStream] for testing.
 *
 * @param captureBuffer Optional buffer to capture written data.
 * @return A configured mock [FileOutputStream] instance.
 */
fun createMockFileOutputStream(captureBuffer: MutableList<ByteArray>? = null): FileOutputStream {
    return mockk<FileOutputStream>(relaxed = true) {
        every { write(any<ByteArray>()) } answers {
            captureBuffer?.add(firstArg<ByteArray>().copyOf())
            Unit
        }
        every { write(any<ByteArray>(), any(), any()) } answers {
            val buffer = firstArg<ByteArray>()
            val offset = secondArg<Int>()
            val length = thirdArg<Int>()
            captureBuffer?.add(buffer.copyOfRange(offset, offset + length))
            Unit
        }
        every { close() } returns Unit
        every { flush() } returns Unit
    }
}

/**
 * Captures method calls on a mock for later verification.
 *
 * Usage:
 * ```kotlin
 * val calls = mutableListOf<String>()
 * val mock = mockk<SomeClass> {
 *     captureCall(calls, "methodName") { methodName() }
 * }
 * ```
 */
inline fun <T> T.captureCall(
    calls: MutableList<String>,
    name: String,
    block: T.() -> Unit,
): T {
    block()
    calls.add(name)
    return this
}
