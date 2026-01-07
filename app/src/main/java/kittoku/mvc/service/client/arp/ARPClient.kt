package kittoku.mvc.service.client.arp

import android.util.Log
import kittoku.mvc.debug.ErrorCode
import kittoku.mvc.debug.MvcException
import kittoku.mvc.extension.isSame
import kittoku.mvc.extension.read
import kittoku.mvc.extension.toBroadcastAddress
import kittoku.mvc.extension.toHexString
import kittoku.mvc.service.client.ClientBridge
import kittoku.mvc.service.client.ControlMessage
import kittoku.mvc.service.client.dhcp.DHCP_RESEND_MESSAGE_TIMEOUT
import kittoku.mvc.unit.arp.ARPPacket
import kittoku.mvc.unit.arp.ARP_OPCODE_REPLY
import kittoku.mvc.unit.arp.ARP_OPCODE_REQUEST
import kittoku.mvc.unit.ethernet.ETHERNET_BROADCAST_ADDRESS
import kittoku.mvc.unit.ethernet.ETHERNET_UNKNOWN_ADDRESS
import kittoku.mvc.unit.ethernet.ETHER_TYPE_ARP
import kittoku.mvc.unit.ethernet.EthernetFrame
import kittoku.mvc.unit.ip.IPv4_ADDRESS_SIZE
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/** Timeout for ARP resolution requests in milliseconds */
internal const val ARP_RESOLUTION_TIMEOUT: Long = 3000

/** Maximum number of pending ARP resolution requests */
internal const val MAX_PENDING_ARP_REQUESTS = 32

/**
 * Represents a pending ARP resolution request.
 */
internal data class PendingArpRequest(
    val targetIp: ByteArray,
    val requestedAt: Long,
    var retryCount: Int = 0,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as PendingArpRequest
        return targetIp.contentEquals(other.targetIp)
    }

    override fun hashCode(): Int = targetIp.contentHashCode()
}

internal class ARPClient(private val bridge: ClientBridge) {
    companion object {
        private const val TAG = "ARPClient"
        private const val MAX_ARP_RETRIES = 3
    }

    /** Pending ARP resolution requests (IP hex string -> request) */
    private val pendingRequests = ConcurrentHashMap<String, PendingArpRequest>()

    /** Mutex for ARP operations */
    private val arpMutex = Mutex()

    internal fun launchJobInitial() { // resolve default gateway MAC address
        bridge.scope.launch(bridge.handler) {
            while (isActive) {
                val reply = startResolveDefaultGatewaySequence(DHCP_RESEND_MESSAGE_TIMEOUT) ?: continue

                if (!registerArpInformation(reply)) {
                    throw MvcException(ErrorCode.ARP_INVALID_CONFIGURATION_ASSIGNED, null)
                }

                // Also add gateway to ARP table
                bridge.arpTable.update(
                    bridge.defaultGatewayIpAddress,
                    bridge.defaultGatewayMacAddress,
                )

                break
            }

            bridge.controlMailbox.send(ControlMessage.ARP_NEGOTIATION_FINISHED)
        }
    }

    internal fun launchReplyBeacon() {
        bridge.scope.launch(bridge.handler) {
            val packet =
                ARPPacket().also {
                    it.opcode = ARP_OPCODE_REPLY
                    it.senderIp.read(bridge.assignedIpAddress)
                    it.senderMac.read(bridge.clientMacAddress)
                    it.targetIp.read(bridge.assignedIpAddress.toBroadcastAddress(bridge.subnetMask))
                    it.targetMac.read(ETHERNET_BROADCAST_ADDRESS)
                }

            sendAsBroadcast(packet)
        }
    }

    /**
     * Handles an incoming ARP packet.
     * - For ARP requests targeting our IP: send a reply
     * - For ARP replies: update the ARP table
     *
     * @param frame The Ethernet frame containing the ARP packet
     */
    internal fun handleIncomingArpPacket(frame: EthernetFrame) {
        val arpPacket = frame.payloadARPPacket ?: return

        when (arpPacket.opcode) {
            ARP_OPCODE_REQUEST -> handleArpRequest(arpPacket)
            ARP_OPCODE_REPLY -> handleArpReply(arpPacket)
        }
    }

    /**
     * Handles an incoming ARP request.
     * If the request is for our IP address, we send a reply.
     */
    private fun handleArpRequest(packet: ARPPacket) {
        // Check if this request is for our IP address
        if (!packet.targetIp.isSame(bridge.assignedIpAddress)) {
            return
        }

        Log.d(TAG, "Received ARP request for our IP from ${packet.senderIp.toHexString()}")

        // Learn the sender's MAC address
        if (!packet.senderMac.isSame(ETHERNET_UNKNOWN_ADDRESS) &&
            !packet.senderMac.isSame(ETHERNET_BROADCAST_ADDRESS)
        ) {
            bridge.arpTable.update(packet.senderIp.copyOf(), packet.senderMac.copyOf())
            Log.d(TAG, "Learned MAC ${packet.senderMac.toHexString()} for IP ${packet.senderIp.toHexString()}")
        }

        // Send ARP reply
        bridge.scope.launch(bridge.handler) {
            sendArpReply(packet.senderIp, packet.senderMac)
        }
    }

    /**
     * Handles an incoming ARP reply.
     * Updates the ARP table with the sender's information.
     */
    private fun handleArpReply(packet: ARPPacket) {
        // Validate the reply
        if (packet.senderMac.isSame(ETHERNET_UNKNOWN_ADDRESS) ||
            packet.senderMac.isSame(ETHERNET_BROADCAST_ADDRESS)
        ) {
            return
        }

        Log.d(TAG, "Received ARP reply: ${packet.senderIp.toHexString()} -> ${packet.senderMac.toHexString()}")

        // Update ARP table
        bridge.arpTable.update(packet.senderIp.copyOf(), packet.senderMac.copyOf())

        // Remove from pending requests if present
        val key = packet.senderIp.toHexString()
        pendingRequests.remove(key)
    }

    /**
     * Sends an ARP reply to the specified target.
     */
    private suspend fun sendArpReply(
        targetIp: ByteArray,
        targetMac: ByteArray,
    ) {
        val packet =
            ARPPacket().also {
                it.opcode = ARP_OPCODE_REPLY
                it.senderIp.read(bridge.assignedIpAddress)
                it.senderMac.read(bridge.clientMacAddress)
                it.targetIp.read(targetIp)
                it.targetMac.read(targetMac)
            }

        val frame =
            EthernetFrame().also {
                it.etherType = ETHER_TYPE_ARP
                it.dstMac.read(targetMac)
                it.srcMac.read(bridge.clientMacAddress)
                it.payloadARPPacket = packet
            }

        bridge.controlChannel.send(frame)
        Log.d(TAG, "Sent ARP reply to ${targetIp.toHexString()}")
    }

    /**
     * Resolves the MAC address for the given IP address.
     * First checks the ARP cache, then sends an ARP request if not found.
     *
     * @param targetIp The IP address to resolve (4 bytes)
     * @return The MAC address if found/resolved, null if resolution failed
     */
    suspend fun resolveAddress(targetIp: ByteArray): ByteArray? {
        require(targetIp.size == IPv4_ADDRESS_SIZE) { "Invalid IP address size" }

        // Check if it's our own IP
        if (targetIp.isSame(bridge.assignedIpAddress)) {
            return bridge.clientMacAddress.copyOf()
        }

        // Check if it's the gateway
        if (targetIp.isSame(bridge.defaultGatewayIpAddress)) {
            return bridge.defaultGatewayMacAddress.copyOf()
        }

        // Check ARP cache
        bridge.arpTable.lookup(targetIp)?.let { return it }

        // Need to send ARP request
        return sendArpRequestAndWait(targetIp)
    }

    /**
     * Initiates an ARP request for the given IP address without waiting.
     * The result will be stored in the ARP table when the reply arrives.
     *
     * @param targetIp The IP address to resolve
     */
    fun initiateArpRequest(targetIp: ByteArray) {
        val key = targetIp.toHexString()

        // Check if already pending
        if (pendingRequests.containsKey(key)) {
            return
        }

        // Check if we have too many pending requests
        if (pendingRequests.size >= MAX_PENDING_ARP_REQUESTS) {
            // Remove oldest request
            pendingRequests.entries.minByOrNull { it.value.requestedAt }?.let {
                pendingRequests.remove(it.key)
            }
        }

        // Add to pending
        pendingRequests[key] = PendingArpRequest(targetIp.copyOf(), System.currentTimeMillis())

        // Send ARP request
        bridge.scope.launch(bridge.handler) {
            sendArpRequest(targetIp)
        }
    }

    /**
     * Sends an ARP request and waits for the reply.
     *
     * @param targetIp The IP address to resolve
     * @return The MAC address if resolved, null if timeout
     */
    private suspend fun sendArpRequestAndWait(targetIp: ByteArray): ByteArray? {
        val key = targetIp.toHexString()

        return arpMutex.withLock {
            // Double-check cache after acquiring lock
            bridge.arpTable.lookup(targetIp)?.let { return@withLock it }

            // Send request and wait for reply
            for (attempt in 0 until MAX_ARP_RETRIES) {
                sendArpRequest(targetIp)

                // Wait for reply with timeout
                val result =
                    withTimeoutOrNull(ARP_RESOLUTION_TIMEOUT) {
                        while (isActive) {
                            // Check if reply arrived
                            bridge.arpTable.lookup(targetIp)?.let { return@withTimeoutOrNull it }
                            kotlinx.coroutines.delay(50)
                        }
                        null
                    }

                if (result != null) {
                    pendingRequests.remove(key)
                    return@withLock result
                }

                Log.d(TAG, "ARP request timeout for ${targetIp.toHexString()}, attempt ${attempt + 1}/$MAX_ARP_RETRIES")
            }

            pendingRequests.remove(key)
            Log.w(TAG, "Failed to resolve MAC for ${targetIp.toHexString()} after $MAX_ARP_RETRIES attempts")
            null
        }
    }

    /**
     * Sends an ARP request for the given IP address.
     */
    private suspend fun sendArpRequest(targetIp: ByteArray) {
        val packet =
            ARPPacket().also {
                it.opcode = ARP_OPCODE_REQUEST
                it.senderIp.read(bridge.assignedIpAddress)
                it.senderMac.read(bridge.clientMacAddress)
                it.targetIp.read(targetIp)
                // targetMac is left as zeros for request
            }

        sendAsBroadcast(packet)
        Log.d(TAG, "Sent ARP request for ${targetIp.toHexString()}")
    }

    /**
     * Checks if the given IP is on the same subnet as our assigned IP.
     */
    fun isOnSameSubnet(destIp: ByteArray): Boolean {
        return bridge.arpTable.isOnSameSubnet(
            destIp,
            bridge.assignedIpAddress,
            bridge.subnetMask,
        )
    }

    /**
     * Gets the MAC address for a destination IP.
     * - For on-subnet destinations: uses ARP table or initiates resolution
     * - For off-subnet destinations: returns gateway MAC
     *
     * @param destIp The destination IP address
     * @return The MAC address to use, or null if on-subnet and not yet resolved
     */
    fun getDestinationMac(destIp: ByteArray): ByteArray? {
        // Check if destination is on the same subnet
        if (!isOnSameSubnet(destIp)) {
            // Off-subnet: use gateway MAC
            return bridge.defaultGatewayMacAddress.copyOf()
        }

        // On-subnet: check ARP table
        bridge.arpTable.lookup(destIp)?.let { return it }

        // Not in cache, initiate resolution
        initiateArpRequest(destIp)
        return null
    }

    private fun extractMessageToMe(frame: EthernetFrame): ARPPacket? {
        val isBroadcastFrame = frame.dstMac.isSame(ETHERNET_BROADCAST_ADDRESS)
        val isToMeFrame = frame.dstMac.isSame(bridge.clientMacAddress)
        if (!(isBroadcastFrame || isToMeFrame)) {
            return null
        }

        return frame.payloadARPPacket!!
    }

    private suspend fun sendAsBroadcast(packet: ARPPacket) {
        val frame =
            EthernetFrame().also {
                it.etherType = ETHER_TYPE_ARP
                it.dstMac.read(ETHERNET_BROADCAST_ADDRESS)
                it.srcMac.read(bridge.clientMacAddress)
                it.payloadARPPacket = packet
            }

        bridge.controlChannel.send(frame)
    }

    private suspend fun expectReplyPacket(): ARPPacket? {
        while (true) {
            val reply = extractMessageToMe(bridge.arpChannel.receive()) ?: continue

            return if (reply.opcode == ARP_OPCODE_REPLY) {
                reply
            } else {
                null
            }
        }
    }

    private suspend fun startResolveDefaultGatewaySequence(timeout: Long): ARPPacket? {
        return withTimeoutOrNull(timeout) {
            val packet =
                ARPPacket().also {
                    it.opcode = ARP_OPCODE_REQUEST
                    it.senderIp.read(bridge.assignedIpAddress)
                    it.senderMac.read(bridge.clientMacAddress)
                    it.targetIp.read(bridge.defaultGatewayIpAddress)
                }

            sendAsBroadcast(packet)
            expectReplyPacket()
        }
    }

    private fun registerArpInformation(reply: ARPPacket): Boolean {
        if (!reply.targetIp.isSame(bridge.assignedIpAddress)) return false
        if (!reply.targetMac.isSame(bridge.clientMacAddress)) return false
        if (!reply.senderIp.isSame(bridge.defaultGatewayIpAddress)) return false
        if (reply.senderMac.isSame(ETHERNET_UNKNOWN_ADDRESS)) return false
        if (reply.senderMac.isSame(ETHERNET_BROADCAST_ADDRESS)) return false

        bridge.defaultGatewayMacAddress.read(reply.senderMac)

        return true
    }
}
