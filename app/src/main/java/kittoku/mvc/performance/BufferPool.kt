package kittoku.mvc.performance

import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * A thread-safe pool of reusable ByteBuffers to reduce GC pressure.
 *
 * VPN applications frequently allocate and deallocate buffers for packet processing.
 * This pool maintains a set of pre-allocated buffers that can be borrowed and returned,
 * reducing memory allocation overhead and GC pauses.
 *
 * Usage:
 * ```kotlin
 * val pool = BufferPool(bufferSize = 1600, maxPoolSize = 10)
 * val buffer = pool.acquire()
 * try {
 *     // Use buffer...
 * } finally {
 *     pool.release(buffer)
 * }
 * ```
 *
 * @param bufferSize The size of each buffer in bytes
 * @param maxPoolSize Maximum number of buffers to keep in the pool
 * @param useDirect Whether to use direct ByteBuffers (off-heap memory)
 */
class BufferPool(
    private val bufferSize: Int,
    private val maxPoolSize: Int = DEFAULT_MAX_POOL_SIZE,
    private val useDirect: Boolean = false,
) {
    private val pool = ConcurrentLinkedQueue<ByteBuffer>()
    private val pooledCount = AtomicInteger(0)
    private val allocatedCount = AtomicInteger(0)
    private val acquireCount = AtomicInteger(0)
    private val releaseCount = AtomicInteger(0)
    private val hitCount = AtomicInteger(0)
    private val missCount = AtomicInteger(0)

    /**
     * Acquire a buffer from the pool or create a new one if the pool is empty.
     *
     * The returned buffer is cleared (position = 0, limit = capacity).
     *
     * @return A ByteBuffer ready for use
     */
    fun acquire(): ByteBuffer {
        acquireCount.incrementAndGet()

        val buffer = pool.poll()
        return if (buffer != null) {
            pooledCount.decrementAndGet()
            hitCount.incrementAndGet()
            buffer.clear()
            buffer
        } else {
            missCount.incrementAndGet()
            allocatedCount.incrementAndGet()
            createBuffer()
        }
    }

    /**
     * Return a buffer to the pool for reuse.
     *
     * If the pool is at capacity, the buffer is discarded (eligible for GC).
     *
     * @param buffer The buffer to return
     */
    fun release(buffer: ByteBuffer) {
        releaseCount.incrementAndGet()

        // Only accept buffers of the correct size
        if (buffer.capacity() != bufferSize) {
            return
        }

        // Only pool up to maxPoolSize buffers
        if (pooledCount.get() < maxPoolSize) {
            buffer.clear()
            pool.offer(buffer)
            pooledCount.incrementAndGet()
        }
        // Otherwise, let GC collect it
    }

    /**
     * Clear all buffers from the pool.
     *
     * Call this when the VPN connection is closed to free memory.
     */
    fun clear() {
        pool.clear()
        pooledCount.set(0)
    }

    /**
     * Get current pool statistics.
     */
    fun getStats(): PoolStats {
        return PoolStats(
            bufferSize = bufferSize,
            maxPoolSize = maxPoolSize,
            currentPoolSize = pooledCount.get(),
            totalAllocated = allocatedCount.get(),
            totalAcquired = acquireCount.get(),
            totalReleased = releaseCount.get(),
            hitCount = hitCount.get(),
            missCount = missCount.get(),
            hitRate =
                if (acquireCount.get() > 0) {
                    hitCount.get().toFloat() / acquireCount.get()
                } else {
                    0f
                },
        )
    }

    /**
     * Reset statistics counters.
     */
    fun resetStats() {
        allocatedCount.set(0)
        acquireCount.set(0)
        releaseCount.set(0)
        hitCount.set(0)
        missCount.set(0)
    }

    private fun createBuffer(): ByteBuffer {
        return if (useDirect) {
            ByteBuffer.allocateDirect(bufferSize)
        } else {
            ByteBuffer.allocate(bufferSize)
        }
    }

    /**
     * Statistics about buffer pool usage.
     */
    data class PoolStats(
        val bufferSize: Int,
        val maxPoolSize: Int,
        val currentPoolSize: Int,
        val totalAllocated: Int,
        val totalAcquired: Int,
        val totalReleased: Int,
        val hitCount: Int,
        val missCount: Int,
        val hitRate: Float,
    ) {
        /**
         * Estimated memory saved by reusing buffers (in bytes).
         */
        val memorySaved: Long
            get() = hitCount.toLong() * bufferSize

        override fun toString(): String {
            return buildString {
                appendLine("BufferPool Statistics:")
                appendLine("  Buffer size: $bufferSize bytes")
                appendLine("  Max pool size: $maxPoolSize")
                appendLine("  Current pool size: $currentPoolSize")
                appendLine("  Total allocated: $totalAllocated")
                appendLine("  Total acquired: $totalAcquired")
                appendLine("  Total released: $totalReleased")
                appendLine("  Hit count: $hitCount")
                appendLine("  Miss count: $missCount")
                appendLine("  Hit rate: ${String.format("%.2f", hitRate * 100)}%")
                appendLine("  Memory saved: ${memorySaved / 1024} KB")
            }
        }
    }

    companion object {
        const val DEFAULT_MAX_POOL_SIZE = 16

        /**
         * Standard buffer sizes for VPN packet processing.
         */
        object Sizes {
            /** Standard Ethernet MTU */
            const val ETHERNET_MTU = 1500

            /** Maximum UDP packet size */
            const val UDP_PACKET = 1600

            /** TCP buffer size (matches TCPTerminal) */
            const val TCP_BUFFER = 16384

            /** Small buffer for headers */
            const val HEADER = 256
        }
    }
}
