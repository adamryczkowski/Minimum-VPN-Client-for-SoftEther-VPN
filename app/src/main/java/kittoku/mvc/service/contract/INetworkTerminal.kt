package kittoku.mvc.service.contract

import kittoku.mvc.unit.ethernet.EthernetFrame
import kittoku.mvc.unit.http.HttpMessage
import java.nio.ByteBuffer

/**
 * Interface for network terminal operations.
 *
 * This interface abstracts the network I/O layer, allowing for different implementations
 * (TCP, UDP) and enabling mock implementations for testing.
 */
internal interface INetworkTerminal {
    /**
     * Sends an Ethernet frame through the terminal.
     *
     * @param frame The Ethernet frame to send
     */
    suspend fun sendFrame(frame: EthernetFrame)

    /**
     * Consumes incoming frames and processes them with the provided handler.
     *
     * @param handler A suspend function that processes each received frame
     */
    suspend fun consumeFrame(handler: suspend (EthernetFrame) -> Unit)

    /**
     * Closes the terminal and releases all resources.
     */
    fun close()
}

/**
 * Interface for TCP-specific terminal operations.
 *
 * Extends [INetworkTerminal] with HTTP message handling capabilities
 * required for SoftEther protocol negotiation.
 */
internal interface ITcpTerminal : INetworkTerminal {
    /**
     * Sends an HTTP message through the TCP connection.
     *
     * @param message The HTTP message to send
     */
    suspend fun sendHttpMessage(message: HttpMessage)

    /**
     * Receives an HTTP message from the TCP connection.
     *
     * @return The received HTTP message
     */
    suspend fun receiveHttpMessage(): HttpMessage

    /**
     * Consumes incoming IP packet buffers and processes them with the provided handler.
     *
     * @param handler A suspend function that processes each received buffer
     */
    suspend fun consumeIPPacketBuffer(handler: suspend (ByteBuffer) -> Unit)

    /**
     * Loads an outgoing packet into the send buffer.
     *
     * @param buffer The packet buffer to load
     */
    fun loadOutgoingPacket(buffer: ByteBuffer)

    /**
     * Adds an additional outgoing packet to the send buffer.
     *
     * @param buffer The packet buffer to add
     * @return true if the packet was added, false if the buffer is full
     */
    fun addOutGoingPacket(buffer: ByteBuffer): Boolean

    /**
     * Sends all buffered outgoing packets.
     */
    suspend fun sendOutgoingPacket()

    /**
     * Sets the socket timeout for data transfer operations.
     */
    fun setTimeoutForData()

    /**
     * Launches the keep-alive job to maintain the connection.
     */
    fun launchJobKeepAlive()
}

/**
 * Interface for UDP-specific terminal operations.
 *
 * Provides UDP acceleration capabilities for the VPN connection.
 */
internal interface IUdpTerminal : INetworkTerminal {
    /**
     * Receives a UDP packet.
     *
     * @return The received packet as a ByteBuffer
     */
    suspend fun receivePacket(): ByteBuffer

    /**
     * Sends data through the UDP channel.
     *
     * @param buffer The data buffer to send
     */
    suspend fun sendData(buffer: ByteBuffer)

    /**
     * Launches the keep-alive job for UDP.
     */
    fun launchJobKeepAlive()

    /**
     * Launches the NAT-T inquiry job.
     */
    fun launchJobInquireNATT()
}

/**
 * Interface for IP terminal operations.
 *
 * Handles the VPN tunnel interface for reading and writing IP packets.
 */
internal interface IIpTerminal {
    /**
     * Initializes the VPN builder and establishes the tunnel.
     */
    fun initializeBuilder()

    /**
     * Launches the job that retrieves outgoing packets from the tunnel.
     */
    fun launchJobRetrieve()

    /**
     * Waits for and returns the next outgoing packet.
     *
     * @return The outgoing packet buffer
     */
    suspend fun waitOutgoingPacket(): ByteBuffer

    /**
     * Polls for an outgoing packet without blocking.
     *
     * @return The outgoing packet buffer, or null if none available
     */
    fun pollOutgoingPacket(): ByteBuffer?

    /**
     * Feeds an incoming packet to the tunnel.
     *
     * @param buffer The incoming packet buffer
     */
    suspend fun feedIncomingPacket(buffer: ByteBuffer)

    /**
     * Closes the IP terminal and releases resources.
     */
    fun close()
}
