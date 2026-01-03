package kittoku.mvc.service.contract

/**
 * Interface for protocol handlers.
 *
 * This interface abstracts protocol-specific negotiation and handling,
 * allowing for different implementations and enabling mock implementations for testing.
 */
internal interface IProtocolHandler {
    /**
     * Launches the negotiation job for this protocol.
     *
     * The negotiation process is protocol-specific and may involve
     * multiple message exchanges with the server.
     */
    fun launchJobNegotiation()
}

/**
 * Interface for SoftEther protocol handling.
 *
 * Handles the SoftEther VPN protocol negotiation including:
 * - Watermark verification
 * - Authentication
 * - UDP acceleration setup
 */
internal interface ISoftEtherHandler : IProtocolHandler {
    // SoftEther-specific methods can be added here if needed
}

/**
 * Interface for DHCP protocol handling.
 *
 * Handles DHCP negotiation to obtain:
 * - IP address assignment
 * - Subnet mask
 * - Default gateway
 * - DNS server
 */
internal interface IDhcpHandler : IProtocolHandler {
    /**
     * Launches the initial DHCP discovery/request process.
     */
    fun launchJobInitial()
}

/**
 * Interface for ARP protocol handling.
 *
 * Handles ARP resolution to obtain:
 * - Gateway MAC address
 * - Secure NAT beacon responses
 */
internal interface IArpHandler : IProtocolHandler {
    /**
     * Launches the initial ARP request process.
     */
    fun launchJobInitial()

    /**
     * Launches a reply to a Secure NAT beacon request.
     */
    fun launchReplyBeacon()
}
