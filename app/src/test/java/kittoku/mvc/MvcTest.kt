package kittoku.mvc

import com.google.common.truth.Truth.assertThat
import kittoku.mvc.extension.isSame
import kittoku.mvc.extension.search
import kittoku.mvc.extension.toHexByteArray
import kittoku.mvc.extension.toHexString
import kittoku.mvc.hash.hashSha0
import kittoku.mvc.service.client.ControlClient
import kittoku.mvc.service.terminal.udp.UDP_NATT_IP_REGEX
import kittoku.mvc.service.terminal.udp.UDP_NATT_PORT_REGEX
import kittoku.mvc.testutil.IntegrationTest
import kittoku.mvc.testutil.RequiresVpnTestEnvironment
import kittoku.mvc.testutil.TestClientBridge
import kittoku.mvc.unit.ip.IPv4Packet
import kittoku.mvc.unit.udp.UDPDatagram
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * Unit tests for the MVC (Minimum VPN Client) core functionality.
 *
 * These tests cover:
 * - SHA-0 hash implementation
 * - UDP datagram serialization
 * - IPv4 packet serialization
 * - NAT-T regex parsing
 * - Byte array search utilities
 *
 * Integration tests (testControlClient, testControlClientUDP) require
 * environment variables: TEST_HOST, TEST_PORT, TEST_USERNAME, TEST_PASSWORD
 */
class MvcTest {
    @Nested
    @DisplayName("Integration Tests (require live server)")
    @RequiresVpnTestEnvironment
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class IntegrationTests {
        @BeforeAll
        fun setupSsl() {
            // Install trust-all SSL context to allow connecting to servers with self-signed certs
            TestClientBridge.installTrustAllSslContext()
        }

        @Test
        @IntegrationTest
        @DisplayName("TCP connection to SoftEther server")
        fun testControlClient() {
            // Use a dedicated scope with SupervisorJob to prevent exception propagation
            val caughtException = AtomicReference<Throwable?>(null)
            val exceptionHandler =
                CoroutineExceptionHandler { _, throwable ->
                    caughtException.set(throwable)
                }
            val testJob = SupervisorJob()
            val testScope = CoroutineScope(Dispatchers.IO + testJob + exceptionHandler)

            try {
                runBlocking {
                    val bridge =
                        TestClientBridge.create(
                            scope = testScope,
                            exceptionHandler = exceptionHandler,
                        )
                    val client = ControlClient(bridge)

                    client.run()
                    delay(10_000)
                }
            } finally {
                // Cancel all coroutines and wait for them to complete
                testScope.cancel()
                runBlocking {
                    testJob.children.forEach { it.join() }
                }
            }

            // Check if there was an exception
            caughtException.get()?.let { throw it }
        }

        @Test
        @IntegrationTest
        @DisplayName("UDP acceleration connection to SoftEther server")
        fun testControlClientUDP() {
            // Use a dedicated scope with SupervisorJob to prevent exception propagation
            val caughtException = AtomicReference<Throwable?>(null)
            val exceptionHandler =
                CoroutineExceptionHandler { _, throwable ->
                    caughtException.set(throwable)
                }
            val testJob = SupervisorJob()
            val testScope = CoroutineScope(Dispatchers.IO + testJob + exceptionHandler)

            try {
                runBlocking {
                    val bridge =
                        TestClientBridge.create(
                            scope = testScope,
                            enableUdpAcceleration = true,
                            exceptionHandler = exceptionHandler,
                        )
                    val client = ControlClient(bridge)

                    client.run()
                    delay(10_000)
                }
            } finally {
                // Cancel all coroutines and wait for them to complete
                testScope.cancel()
                runBlocking {
                    testJob.children.forEach { it.join() }
                }
            }

            // Check if there was an exception
            caughtException.get()?.let { throw it }
        }
    }

    @Nested
    @DisplayName("SHA-0 Hash Tests")
    inner class HashTests {
        @Test
        @DisplayName("SHA-0 hash of 'abc' produces correct digest")
        fun testHashSha0Abc() {
            val actual = hashSha0("abc".toByteArray(Charsets.US_ASCII))
            val expected = "0164B8A914CD2A5E74C4F7FF082C4D97F1EDF880".toHexByteArray()

            assertThat(actual.isSame(expected)).isTrue()
        }

        @Test
        @DisplayName("SHA-0 hash of longer string produces correct digest")
        fun testHashSha0Long() {
            val actual = hashSha0("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq".toByteArray(Charsets.US_ASCII))
            val expected = "D2516EE1ACFA5BAF33DFC1C471E438449EF134C8".toHexByteArray()

            assertThat(actual.isSame(expected)).isTrue()
        }
    }

    @Nested
    @DisplayName("UDP Datagram Tests")
    inner class UdpDatagramTests {
        private fun writeReadUDPDatagram(datagram: UDPDatagram) {
            val buffer = ByteBuffer.allocate(datagram.length)

            try {
                datagram.write(buffer)
                buffer.flip()
                datagram.read(buffer)
            } catch (e: Exception) {
                println("SRC: ${datagram.srcPort}")
                println("DST: ${datagram.dstPort}")
                println("PAYLOAD: ${datagram.payloadUnknown?.toHexString() ?: ""}")
                println("HEADER: ${accessPseudoIPHeader(datagram).toHexString()}")

                throw e
            }
        }

        private fun accessPseudoIPHeader(datagram: UDPDatagram): ByteArray {
            val property = UDPDatagram::class.memberProperties.find { it.name == "pseudoIPHeader" }

            @Suppress("UNCHECKED_CAST")
            property as KProperty1<UDPDatagram, ByteArray>

            return property.let {
                it.isAccessible = true
                it.get(datagram)
            }
        }

        private fun modifyPseudoIPHeader(
            value: ByteArray,
            datagram: UDPDatagram,
        ) {
            val property = UDPDatagram::class.memberProperties.find { it.name == "pseudoIPHeader" }

            @Suppress("UNCHECKED_CAST")
            property as KMutableProperty1<UDPDatagram, ByteArray>

            property.also {
                it.isAccessible = true
                it.set(datagram, value)
            }
        }

        @Test
        @DisplayName("UDP datagram with minimal values serializes correctly")
        fun testMinimalDatagram() {
            UDPDatagram().also {
                it.srcPort = -8
                it.dstPort = 0
                it.payloadUnknown = ByteArray(0)
                modifyPseudoIPHeader(ByteArray(0), it)

                writeReadUDPDatagram(it)
            }
        }

        @Test
        @DisplayName("UDP datagram with negative ports serializes correctly")
        fun testNegativePortsDatagram() {
            UDPDatagram().also {
                it.srcPort = -549654713
                it.dstPort = -1991713114
                it.payloadUnknown = ByteArray(1).also { array -> array[0] = -100 }
                modifyPseudoIPHeader(ByteArray(0), it)

                writeReadUDPDatagram(it)
            }
        }

        @Test
        @DisplayName("UDP datagram with full payload serializes correctly")
        fun testFullPayloadDatagram() {
            UDPDatagram().also {
                it.srcPort = 65214
                it.dstPort = 33409
                it.payloadUnknown =
                    listOf(
                        "35BBF96E035649A57EB582554A292B9A1932553E70930421BF5D207A6DB421717",
                        "CE65F0A76CDD62C926CA9950EAD9FA9925B84066D7E7724CF39F922BA2593855B",
                        "7FDFD2B434",
                    ).reduce { acc, s -> acc + s }.toHexByteArray()
                modifyPseudoIPHeader("29DB7C1FB0CF03F513C4B712".toHexByteArray(), it)

                writeReadUDPDatagram(it)
            }
        }
    }

    @Nested
    @DisplayName("IPv4 Packet Tests")
    inner class Ipv4PacketTests {
        private fun writeReadIPv4Packet(packet: IPv4Packet) {
            val buffer = ByteBuffer.allocate(packet.length)

            try {
                packet.write(buffer)
                buffer.flip()
                packet.read(buffer)
            } catch (e: Exception) {
                println("SRC: ${packet.srcAddress.toHexString()}")
                println("DST: ${packet.dstAddress.toHexString()}")
                println("PAYLOAD: ${packet.payloadUnknown?.toHexString() ?: ""}")

                throw e
            }
        }

        @Test
        @DisplayName("IPv4 packet with modified source address serializes correctly")
        fun testIpPacket() {
            IPv4Packet().also {
                it.srcAddress[3] = -20
                it.payloadUnknown = ByteArray(0)

                writeReadIPv4Packet(it)
            }
        }
    }

    @Nested
    @DisplayName("NAT-T Regex Tests")
    inner class NattRegexTests {
        @Test
        @DisplayName("NAT-T IP regex extracts IP correctly")
        fun testNattIpRegex() {
            val text = "IP=192.168.0.1,PORT=11235"
            val expectedIP = "IP=192.168.0.1"

            val regexIP = Regex(UDP_NATT_IP_REGEX)
            val match = regexIP.find(text)

            assertThat(match).isNotNull()
            assertThat(match!!.value).isEqualTo(expectedIP)
        }

        @Test
        @DisplayName("NAT-T PORT regex extracts port correctly")
        fun testNattPortRegex() {
            val text = "IP=192.168.0.1,PORT=11235"
            val expectedPort = "PORT=11235"

            val regexPort = Regex(UDP_NATT_PORT_REGEX)
            val match = regexPort.find(text)

            assertThat(match).isNotNull()
            assertThat(match!!.value).isEqualTo(expectedPort)
        }
    }

    @Nested
    @DisplayName("Byte Array Search Tests")
    inner class ByteArraySearchTests {
        @Test
        @DisplayName("Search returns -1 when pattern is longer than array")
        fun testPatternLongerThanArray() {
            val array = "ABCDEFG".toByteArray(Charsets.US_ASCII)
            val pattern = "ABCDEFGH".toByteArray(Charsets.US_ASCII)

            assertThat(array.search(pattern)).isEqualTo(-1)
        }

        @Test
        @DisplayName("Search finds pattern in middle of array")
        fun testPatternInMiddle() {
            val array = "ABCDEFG".toByteArray(Charsets.US_ASCII)
            val pattern = "DEF".toByteArray(Charsets.US_ASCII)

            assertThat(array.search(pattern)).isEqualTo(3)
        }

        @Test
        @DisplayName("Search returns -1 when pattern not found")
        fun testPatternNotFound() {
            val array = "ABCDEFG".toByteArray(Charsets.US_ASCII)
            val pattern = "XYZ".toByteArray(Charsets.US_ASCII)

            assertThat(array.search(pattern)).isEqualTo(-1)
        }

        @Test
        @DisplayName("Search returns -1 when pattern extends beyond array")
        fun testPatternExtendsBeyond() {
            val array = "ABCDEFG".toByteArray(Charsets.US_ASCII)
            val pattern = "GH".toByteArray(Charsets.US_ASCII)

            assertThat(array.search(pattern)).isEqualTo(-1)
        }
    }
}
