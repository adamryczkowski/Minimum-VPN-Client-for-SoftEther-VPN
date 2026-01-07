package kittoku.mvc.service.client.arp

import kittoku.mvc.extension.toHexString
import kittoku.mvc.unit.ethernet.ETHERNET_MAC_ADDRESS_SIZE
import kittoku.mvc.unit.ip.IPv4_ADDRESS_SIZE
import java.util.concurrent.ConcurrentHashMap

/**
 * Represents an entry in the ARP cache.
 *
 * @param macAddress The MAC address associated with the IP
 * @param expiresAt The timestamp (in milliseconds) when this entry expires
 */
internal data class ArpEntry(
    val macAddress: ByteArray,
    val expiresAt: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ArpEntry
        return macAddress.contentEquals(other.macAddress) && expiresAt == other.expiresAt
    }

    override fun hashCode(): Int {
        var result = macAddress.contentHashCode()
        result = 31 * result + expiresAt.hashCode()
        return result
    }
}

/**
 * Thread-safe ARP table for caching IP-to-MAC address mappings.
 *
 * This table is used to resolve MAC addresses for hosts on the same
 * VPN subnet, enabling direct client-to-client communication.
 */
internal class ArpTable {
    companion object {
        /** Default TTL for ARP entries in seconds */
        const val DEFAULT_TTL_SECONDS = 300L // 5 minutes

        /** Maximum number of entries in the cache */
        const val MAX_ENTRIES = 256
    }

    private val cache = ConcurrentHashMap<String, ArpEntry>()

    /**
     * Converts an IP address byte array to a string key for the cache.
     */
    private fun ipToKey(ipAddress: ByteArray): String {
        require(ipAddress.size == IPv4_ADDRESS_SIZE) { "Invalid IP address size" }
        return ipAddress.toHexString()
    }

    /**
     * Looks up a MAC address for the given IP address.
     *
     * @param ipAddress The IP address to look up (4 bytes)
     * @return The MAC address if found and not expired, null otherwise
     */
    fun lookup(ipAddress: ByteArray): ByteArray? {
        val key = ipToKey(ipAddress)
        val entry = cache[key] ?: return null

        // Check if entry has expired
        if (System.currentTimeMillis() > entry.expiresAt) {
            cache.remove(key)
            return null
        }

        return entry.macAddress.copyOf()
    }

    /**
     * Updates or adds an entry in the ARP cache.
     *
     * @param ipAddress The IP address (4 bytes)
     * @param macAddress The MAC address (6 bytes)
     * @param ttlSeconds Time-to-live in seconds (default: 5 minutes)
     */
    fun update(
        ipAddress: ByteArray,
        macAddress: ByteArray,
        ttlSeconds: Long = DEFAULT_TTL_SECONDS,
    ) {
        require(ipAddress.size == IPv4_ADDRESS_SIZE) { "Invalid IP address size" }
        require(macAddress.size == ETHERNET_MAC_ADDRESS_SIZE) { "Invalid MAC address size" }

        // Enforce max entries limit
        if (cache.size >= MAX_ENTRIES) {
            pruneExpiredEntries()
            // If still at limit, remove oldest entry
            if (cache.size >= MAX_ENTRIES) {
                cache.entries.minByOrNull { it.value.expiresAt }?.let {
                    cache.remove(it.key)
                }
            }
        }

        val key = ipToKey(ipAddress)
        val expiresAt = System.currentTimeMillis() + (ttlSeconds * 1000)
        cache[key] = ArpEntry(macAddress.copyOf(), expiresAt)
    }

    /**
     * Removes an entry from the ARP cache.
     *
     * @param ipAddress The IP address to remove
     */
    fun remove(ipAddress: ByteArray) {
        val key = ipToKey(ipAddress)
        cache.remove(key)
    }

    /**
     * Clears all entries from the ARP cache.
     */
    fun clear() {
        cache.clear()
    }

    /**
     * Returns the number of entries in the cache.
     */
    fun size(): Int = cache.size

    /**
     * Checks if an entry exists for the given IP address (even if expired).
     */
    fun contains(ipAddress: ByteArray): Boolean {
        val key = ipToKey(ipAddress)
        return cache.containsKey(key)
    }

    /**
     * Removes all expired entries from the cache.
     */
    fun pruneExpiredEntries() {
        val now = System.currentTimeMillis()
        cache.entries.removeIf { it.value.expiresAt < now }
    }

    /**
     * Checks if the given IP address is on the same subnet.
     *
     * @param destIp The destination IP address
     * @param localIp The local IP address
     * @param subnetMask The subnet mask
     * @return true if destIp is on the same subnet as localIp
     */
    fun isOnSameSubnet(
        destIp: ByteArray,
        localIp: ByteArray,
        subnetMask: ByteArray,
    ): Boolean {
        require(destIp.size == IPv4_ADDRESS_SIZE) { "Invalid destination IP size" }
        require(localIp.size == IPv4_ADDRESS_SIZE) { "Invalid local IP size" }
        require(subnetMask.size == IPv4_ADDRESS_SIZE) { "Invalid subnet mask size" }

        for (i in 0 until IPv4_ADDRESS_SIZE) {
            val destNetwork = destIp[i].toInt() and subnetMask[i].toInt()
            val localNetwork = localIp[i].toInt() and subnetMask[i].toInt()
            if (destNetwork != localNetwork) {
                return false
            }
        }
        return true
    }
}
