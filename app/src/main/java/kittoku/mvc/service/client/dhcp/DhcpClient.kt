package kittoku.mvc.service.client.dhcp

import android.util.Log
import kittoku.mvc.debug.ErrorCode
import kittoku.mvc.debug.MvcException
import kittoku.mvc.extension.copy
import kittoku.mvc.extension.isSame
import kittoku.mvc.extension.read
import kittoku.mvc.service.client.ClientBridge
import kittoku.mvc.service.client.ControlMessage
import kittoku.mvc.unit.dhcp.*
import kittoku.mvc.unit.ethernet.ETHERNET_BROADCAST_ADDRESS
import kittoku.mvc.unit.ethernet.ETHER_TYPE_IPv4
import kittoku.mvc.unit.ethernet.EthernetFrame
import kittoku.mvc.unit.ip.IP_PROTOCOL_UDP
import kittoku.mvc.unit.ip.IPv4Packet
import kittoku.mvc.unit.ip.IPv4_BROADCAST_ADDRESS
import kittoku.mvc.unit.ip.IPv4_UNKNOWN_ADDRESS
import kittoku.mvc.unit.udp.UDPDatagram
import kittoku.mvc.unit.udp.UDP_PORT_DHCP_CLIENT
import kittoku.mvc.unit.udp.UDP_PORT_DHCP_SEVER
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal class DhcpClient(private val bridge: ClientBridge) {
    internal fun launchJobInitial() {
        bridge.scope.launch(bridge.handler) {
            while (isActive) {
                val offer = startDiscoverOfferSequence(DHCP_RESEND_MESSAGE_TIMEOUT) ?: continue
                val ack = startRequestAckSequence(offer, DHCP_RESEND_MESSAGE_TIMEOUT) ?: continue

                if (!registerDhcpInformation(ack)) {
                    throw MvcException(ErrorCode.DHCP_INVALID_CONFIGURATION_ASSIGNED, null)
                }

                break
            }

            bridge.controlMailbox.send(ControlMessage.DHCP_NEGOTIATION_FINISHED)
        }
    }

    private fun prepareBasicOptionsParameters(): ByteArray {
        val parameters = mutableListOf<Byte>()

        parameters.add(DHCP_OPTION_SUBNET_MASK)
        parameters.add(DHCP_OPTION_ROUTER_ADDRESS)
        parameters.add(DHCP_OPTION_DNS_SERVER_ADDRESS)

        return parameters.toByteArray()
    }

    private fun extractMessageToMe(frame: EthernetFrame): DhcpMessage? {
        val isBroadcastFrame = frame.dstMac.isSame(ETHERNET_BROADCAST_ADDRESS)
        val isToMeFrame = frame.dstMac.isSame(bridge.clientMacAddress)
        if (!(isBroadcastFrame || isToMeFrame)) {
            return null
        }

        val packet = frame.payloadIPv4Packet!!
        val isBroadcastPacket = packet.dstAddress.isSame(IPv4_BROADCAST_ADDRESS)
        val isToMePacket = packet.dstAddress.isSame(bridge.assignedIpAddress)
        if (!(isBroadcastPacket || isToMePacket)) {
            return null
        }

        val datagram = packet.payloadUDPDatagram!!
        if (datagram.dstPort != UDP_PORT_DHCP_CLIENT || datagram.srcPort != UDP_PORT_DHCP_SEVER) {
            return null
        }

        val message = datagram.payloadDhcpMessage!!
        if (!message.clientMacAddress.isSame(bridge.clientMacAddress)) {
            return null
        }

        return datagram.payloadDhcpMessage!!
    }

    private suspend fun sendAsBroadcast(message: DhcpMessage) {
        val datagram =
            UDPDatagram().also {
                it.dstPort = UDP_PORT_DHCP_SEVER
                it.srcPort = UDP_PORT_DHCP_CLIENT
                it.payloadDhcpMessage = message
            }

        val packet =
            IPv4Packet().also {
                it.protocol = IP_PROTOCOL_UDP
                it.identification = bridge.random.nextInt().toShort()
                it.dstAddress.read(IPv4_BROADCAST_ADDRESS)
                it.srcAddress.read(IPv4_UNKNOWN_ADDRESS)
                it.payloadUDPDatagram = datagram

                datagram.importIPv4Header(it)
            }

        val frame =
            EthernetFrame().also {
                it.etherType = ETHER_TYPE_IPv4
                it.dstMac.read(ETHERNET_BROADCAST_ADDRESS)
                it.srcMac.read(bridge.clientMacAddress)
                it.payloadIPv4Packet = packet
            }

        bridge.controlChannel.send(frame)
    }

    private suspend fun expectOfferMessage(transactionId: Int): DhcpMessage? {
        while (true) {
            val received = bridge.dhcpChannel.receive()
            val reply = extractMessageToMe(received) ?: continue
            if (reply.transactionId != transactionId) continue

            return if (reply.options.messageType == DHCP_MESSAGE_TYPE_OFFER) {
                reply
            } else {
                null
            }
        }
    }

    private suspend fun expectAckMessage(transactionId: Int): DhcpMessage? {
        while (true) {
            val reply = extractMessageToMe(bridge.dhcpChannel.receive()) ?: continue
            if (reply.transactionId != transactionId) continue

            return if (reply.options.messageType == DHCP_MESSAGE_TYPE_ACK) {
                reply
            } else {
                null
            }
        }
    }

    private suspend fun startDiscoverOfferSequence(timeout: Long): DhcpMessage? {
        return withTimeoutOrNull(timeout) {
            val transactionId = bridge.random.nextInt()

            val options =
                OptionPack().also {
                    it.messageType = DHCP_MESSAGE_TYPE_DISCOVER
                    it.optionParameterList =
                        DhcpOptionParameterList().also { option ->
                            option.value = prepareBasicOptionsParameters()
                        }
                }

            val message =
                DhcpMessage().also {
                    it.opcode = DHCP_OPCODE_BOOT_REQUEST
                    it.transactionId = transactionId
                    it.flags = DhcpMessage.FLAG_BROADCAST // Request broadcast response
                    it.clientMacAddress.read(bridge.clientMacAddress)
                    it.options = options
                }

            sendAsBroadcast(message)
            expectOfferMessage(transactionId)
        }
    }

    private suspend fun startRequestAckSequence(
        offer: DhcpMessage,
        timeout: Long,
    ): DhcpMessage? {
        return withTimeoutOrNull(timeout) {
            val options =
                OptionPack().also {
                    it.messageType = DHCP_MESSAGE_TYPE_REQUEST
                    it.optionRequestedAddress =
                        DhcpOptionRequestedAddress().also { option ->
                            option.address.read(offer.yourIpAddress)
                        }
                    it.optionDhcpServerAddress = offer.options.optionDhcpServerAddress
                    it.optionParameterList =
                        DhcpOptionParameterList().also { option ->
                            option.value = prepareBasicOptionsParameters()
                        }
                }

            val message =
                DhcpMessage().also {
                    it.opcode = DHCP_OPCODE_BOOT_REQUEST
                    it.transactionId = offer.transactionId
                    it.flags = DhcpMessage.FLAG_BROADCAST // Request broadcast response
                    it.clientMacAddress.read(bridge.clientMacAddress)
                    it.options = options
                }

            sendAsBroadcast(message)
            expectAckMessage(offer.transactionId)
        }
    }

    private fun formatIpAddress(bytes: ByteArray): String {
        return bytes.joinToString(".") { (it.toInt() and 0xFF).toString() }
    }

    private fun registerDhcpInformation(ack: DhcpMessage): Boolean {
        Log.d(TAG, "registerDhcpInformation: Starting DHCP ACK validation")
        Log.d(TAG, "  yourIpAddress: ${formatIpAddress(ack.yourIpAddress)}")
        Log.d(TAG, "  optionSubnetMask: ${ack.options.optionSubnetMask?.let { formatIpAddress(it.address) } ?: "null"}")
        Log.d(TAG, "  optionRouterAddress: ${ack.options.optionRouterAddress?.let { formatIpAddress(it.address) } ?: "null"}")
        Log.d(TAG, "  optionDhcpServerAddress: ${ack.options.optionDhcpServerAddress?.let { formatIpAddress(it.address) } ?: "null"}")
        Log.d(TAG, "  optionDnsServerAddress: ${ack.options.optionDnsServerAddress?.let { formatIpAddress(it.address) } ?: "null"}")
        Log.d(TAG, "  optionLeaseTime: ${ack.options.optionLeaseTime?.length ?: "null"}")
        Log.d(TAG, "  unknownOptionTags: ${ack.options.unknownOptionTags.map { it.toInt() and 0xFF }}")

        if (ack.yourIpAddress.isSame(IPv4_UNKNOWN_ADDRESS)) {
            Log.e(TAG, "DHCP validation failed: yourIpAddress is unknown/zero")
            return false
        }
        bridge.assignedIpAddress.read(ack.yourIpAddress)

        val subnetMask = ack.options.optionSubnetMask?.address
        if (subnetMask == null) {
            Log.e(TAG, "DHCP validation failed: optionSubnetMask is null")
            return false
        }
        if (subnetMask.isSame(IPv4_UNKNOWN_ADDRESS)) {
            Log.e(TAG, "DHCP validation failed: optionSubnetMask is unknown/zero")
            return false
        }
        bridge.subnetMask.read(subnetMask)

        // Get router address (optional - some VPN servers like SoftEther don't provide it)
        val routerAddress = ack.options.optionRouterAddress?.address
        // Get DHCP server address (optional but usually provided)
        val dhcpServerAddress = ack.options.optionDhcpServerAddress?.address

        // Determine the default gateway - prefer router address, fallback to DHCP server
        val defaultGatewayAddress: ByteArray? =
            when {
                routerAddress != null && !routerAddress.isSame(IPv4_UNKNOWN_ADDRESS) -> {
                    Log.d(TAG, "Using router address as gateway: ${formatIpAddress(routerAddress)}")
                    routerAddress
                }
                dhcpServerAddress != null && !dhcpServerAddress.isSame(IPv4_UNKNOWN_ADDRESS) -> {
                    Log.d(TAG, "Router address not provided, using DHCP server as gateway: ${formatIpAddress(dhcpServerAddress)}")
                    dhcpServerAddress
                }
                else -> {
                    Log.e(TAG, "DHCP validation failed: neither router nor DHCP server address available")
                    null
                }
            }

        if (defaultGatewayAddress == null) {
            return false
        }
        bridge.defaultGatewayIpAddress.read(defaultGatewayAddress)

        // Set DHCP server address - prefer explicit, fallback to gateway
        if (dhcpServerAddress != null && !dhcpServerAddress.isSame(IPv4_UNKNOWN_ADDRESS)) {
            bridge.dhcpServerIpAddress.read(dhcpServerAddress)
            Log.d(TAG, "DHCP server address set: ${formatIpAddress(dhcpServerAddress)}")
        } else {
            bridge.dhcpServerIpAddress.read(defaultGatewayAddress)
            Log.d(TAG, "DHCP server address not provided, using gateway: ${formatIpAddress(defaultGatewayAddress)}")
        }

        ack.options.optionDnsServerAddress?.also {
            if (it.address.isSame(IPv4_UNKNOWN_ADDRESS)) {
                Log.e(TAG, "DHCP validation failed: optionDnsServerAddress is unknown/zero")
                return false
            }
            bridge.dnsServerIpAddress = it.address.copy()
        }

        ack.options.optionLeaseTime?.also {
            if (it.length < 0) {
                Log.e(TAG, "DHCP validation failed: optionLeaseTime is negative")
                return false
            }
            bridge.leaseTime = it.length.toLong() * 1_000 // as millisecond
        }

        Log.d(TAG, "DHCP validation successful!")
        return true
    }

    companion object {
        private const val TAG = "DhcpClient"
    }
}
