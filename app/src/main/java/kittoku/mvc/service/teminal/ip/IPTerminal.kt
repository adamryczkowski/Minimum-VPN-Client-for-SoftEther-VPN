package kittoku.mvc.service.terminal.ip

import android.content.pm.PackageManager
import android.os.ParcelFileDescriptor
import android.util.Log
import kittoku.mvc.extension.move
import kittoku.mvc.extension.toInetAddress
import kittoku.mvc.service.client.ClientBridge
import kittoku.mvc.service.client.arp.ARPClient
import kittoku.mvc.splittunnel.SplitTunnelApplicator
import kittoku.mvc.unit.ethernet.ETHERNET_HEADER_SIZE
import kittoku.mvc.unit.ethernet.ETHER_TYPE_IPv4
import kittoku.mvc.unit.ip.IPv4_ADDRESS_SIZE
import kittoku.mvc.unit.ip.IPv4_VERSION_AND_HEADER_LENGTH
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Represents a packet waiting for ARP resolution.
 */
internal data class PendingPacket(
    val data: ByteArray,
    val destIp: ByteArray,
    val timestamp: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PendingPacket
        return data.contentEquals(other.data) && destIp.contentEquals(other.destIp)
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + destIp.contentHashCode()
        return result
    }
}

internal class IPTerminal(
    private val bridge: ClientBridge,
    private val splitTunnelApplicator: SplitTunnelApplicator? = null,
) {
    companion object {
        private const val TAG = "IPTerminal"

        /** Offset to destination IP in IPv4 header (after Ethernet header) */
        private const val IPV4_DEST_IP_OFFSET = ETHERNET_HEADER_SIZE + 16

        /** Maximum number of pending packets per destination */
        private const val MAX_PENDING_PACKETS = 10

        /** Timeout for pending packets in milliseconds */
        private const val PENDING_PACKET_TIMEOUT = 5000L
    }

    private lateinit var fd: ParcelFileDescriptor
    private lateinit var inputStream: InputStream
    private lateinit var outputStream: OutputStream

    private lateinit var jobRetrieve: Job
    private lateinit var jobPendingPackets: Job
    private val retrieveChannel = Channel<ByteBuffer>(0)

    private val mutex = Mutex()

    /** ARP client for MAC address resolution */
    internal var arpClient: ARPClient? = null

    /** Queue of packets waiting for ARP resolution */
    private val pendingPackets = ConcurrentLinkedQueue<PendingPacket>()

    internal fun initializeBuilder() {
        val builder = bridge.service.Builder()

        val prefixLength =
            bridge.subnetMask.let {
                val buffer = ByteBuffer.wrap(it)
                buffer.int.countOneBits()
            }

        builder.addAddress(bridge.assignedIpAddress.toInetAddress(), prefixLength)
        builder.addRoute("0.0.0.0", 0)

        bridge.dnsServerIpAddress?.also {
            builder.addDnsServer(it.toInetAddress())
        }

        builder.setBlocking(true)
        builder.setMtu(bridge.internalEthernetMTU)

        // Apply split tunneling configuration
        applySplitTunnel(builder)

        fd = builder.establish()!!
        inputStream = FileInputStream(fd.fileDescriptor)
        outputStream = FileOutputStream(fd.fileDescriptor)
    }

    /**
     * Applies split tunnel configuration to the VPN builder.
     * In INCLUDE mode, only selected apps use the VPN.
     * In EXCLUDE mode, selected apps bypass the VPN.
     */
    private fun applySplitTunnel(builder: android.net.VpnService.Builder) {
        val applicator = splitTunnelApplicator ?: return

        if (!applicator.isSplitTunnelActive()) {
            Log.d(TAG, "Split tunneling is not active")
            return
        }

        // Apply allowed apps (INCLUDE mode)
        val appsToAllow = applicator.getAppsToAllow()
        if (appsToAllow.isNotEmpty()) {
            Log.d(TAG, "Applying INCLUDE mode with ${appsToAllow.size} apps")
            for (packageName in appsToAllow) {
                try {
                    builder.addAllowedApplication(packageName)
                    Log.d(TAG, "Added allowed app: $packageName")
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "App not found, skipping: $packageName")
                }
            }
            // In INCLUDE mode, also add our own app to ensure VPN service works
            try {
                builder.addAllowedApplication(bridge.service.packageName)
                Log.d(TAG, "Added own app to allowed list: ${bridge.service.packageName}")
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(TAG, "Failed to add own app to allowed list")
            }
        }

        // Apply disallowed apps (EXCLUDE mode)
        val appsToDisallow = applicator.getAppsToDisallow()
        if (appsToDisallow.isNotEmpty()) {
            Log.d(TAG, "Applying EXCLUDE mode with ${appsToDisallow.size} apps")
            for (packageName in appsToDisallow) {
                try {
                    builder.addDisallowedApplication(packageName)
                    Log.d(TAG, "Added disallowed app: $packageName")
                } catch (e: PackageManager.NameNotFoundException) {
                    Log.w(TAG, "App not found, skipping: $packageName")
                }
            }
        }
    }

    internal fun launchJobRetrieve() {
        jobRetrieve =
            bridge.scope.launch(bridge.handler) {
                val bufferSize = bridge.internalEthernetMTU + ETHERNET_HEADER_SIZE
                val alpha = ByteBuffer.allocate(bufferSize)
                val beta = ByteBuffer.allocate(bufferSize)

                var isAlphaGo = true

                while (isActive) {
                    if (isAlphaGo) {
                        retrievePacket(alpha)
                        isAlphaGo = false
                    } else {
                        retrievePacket(beta)
                        isAlphaGo = true
                    }
                }
            }

        // Launch job to process pending packets
        jobPendingPackets =
            bridge.scope.launch(bridge.handler) {
                while (isActive) {
                    processPendingPackets()
                    kotlinx.coroutines.delay(100) // Check every 100ms
                }
            }
    }

    /**
     * Retrieves a packet from the TUN interface and prepares it for sending.
     * Uses ARP resolution for on-subnet destinations.
     */
    private suspend fun retrievePacket(buffer: ByteBuffer) {
        while (true) {
            yield()

            buffer.clear()

            // Reserve space for Ethernet header (will be filled after reading IP packet)
            buffer.position(ETHERNET_HEADER_SIZE)

            // Read IP packet from TUN interface
            val readLength = inputStream.read(buffer.array(), buffer.position(), bridge.internalEthernetMTU)
            if (readLength <= 0) continue

            buffer.move(readLength)
            buffer.flip()

            // Check if it's an IPv4 packet
            if (buffer.get(ETHERNET_HEADER_SIZE) != IPv4_VERSION_AND_HEADER_LENGTH) continue

            // Extract destination IP address (at offset 16 in IPv4 header)
            val destIp = ByteArray(IPv4_ADDRESS_SIZE)
            buffer.position(IPV4_DEST_IP_OFFSET)
            buffer.get(destIp)
            buffer.rewind()

            // Determine destination MAC address
            val destMac = getDestinationMac(destIp)

            if (destMac == null) {
                // ARP resolution pending - queue the packet
                queuePendingPacket(buffer, destIp)
                continue
            }

            // Fill in Ethernet header
            buffer.position(0)
            buffer.put(destMac)
            buffer.put(bridge.clientMacAddress)
            buffer.putShort(ETHER_TYPE_IPv4)
            buffer.rewind()
            buffer.limit(ETHERNET_HEADER_SIZE + readLength)

            break
        }

        retrieveChannel.send(buffer)
    }

    /**
     * Gets the destination MAC address for the given IP.
     * - For off-subnet destinations: returns gateway MAC
     * - For on-subnet destinations: checks ARP table or initiates resolution
     *
     * @param destIp The destination IP address
     * @return The MAC address to use, or null if ARP resolution is pending
     */
    private fun getDestinationMac(destIp: ByteArray): ByteArray? {
        val client = arpClient

        // If no ARP client, fall back to gateway MAC (original behavior)
        if (client == null) {
            return bridge.defaultGatewayMacAddress.copyOf()
        }

        return client.getDestinationMac(destIp)
    }

    /**
     * Queues a packet for later sending once ARP resolution completes.
     */
    private fun queuePendingPacket(
        buffer: ByteBuffer,
        destIp: ByteArray,
    ) {
        // Limit pending packets
        if (pendingPackets.size >= MAX_PENDING_PACKETS) {
            pendingPackets.poll() // Remove oldest
        }

        // Copy packet data (excluding Ethernet header space)
        val packetData = ByteArray(buffer.limit() - ETHERNET_HEADER_SIZE)
        buffer.position(ETHERNET_HEADER_SIZE)
        buffer.get(packetData)

        pendingPackets.add(
            PendingPacket(
                data = packetData,
                destIp = destIp.copyOf(),
                timestamp = System.currentTimeMillis(),
            ),
        )

        Log.d(TAG, "Queued packet for ARP resolution, pending count: ${pendingPackets.size}")
    }

    /**
     * Processes pending packets that may now have ARP resolution.
     */
    private suspend fun processPendingPackets() {
        val now = System.currentTimeMillis()
        val iterator = pendingPackets.iterator()

        while (iterator.hasNext()) {
            val pending = iterator.next()

            // Check for timeout
            if (now - pending.timestamp > PENDING_PACKET_TIMEOUT) {
                iterator.remove()
                Log.d(TAG, "Dropped pending packet due to timeout")
                continue
            }

            // Try to get destination MAC
            val destMac = getDestinationMac(pending.destIp)
            if (destMac == null) {
                // Still waiting for ARP resolution
                continue
            }

            // ARP resolved - send the packet
            iterator.remove()

            val bufferSize = ETHERNET_HEADER_SIZE + pending.data.size
            val buffer = ByteBuffer.allocate(bufferSize)

            // Build Ethernet frame
            buffer.put(destMac)
            buffer.put(bridge.clientMacAddress)
            buffer.putShort(ETHER_TYPE_IPv4)
            buffer.put(pending.data)
            buffer.flip()

            // Send via retrieve channel
            retrieveChannel.send(buffer)
            Log.d(TAG, "Sent pending packet after ARP resolution")
        }
    }

    internal suspend fun waitOutgoingPacket() = retrieveChannel.receive()

    internal fun pollOutgoingPacket() = retrieveChannel.tryReceive().getOrNull()

    internal suspend fun feedIncomingPacket(buffer: ByteBuffer) {
        mutex.withLock {
            outputStream.write(buffer.array(), buffer.position(), buffer.remaining())
            outputStream.flush()

            buffer.position(buffer.limit())
        }
    }

    internal fun close() {
        if (::fd.isInitialized) fd.close()
        pendingPackets.clear()
    }
}
