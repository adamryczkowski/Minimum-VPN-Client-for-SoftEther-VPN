package kittoku.mvc.performance

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

/**
 * Unit tests for [BufferPool].
 */
@DisplayName("BufferPool")
class BufferPoolTest {
    private lateinit var pool: BufferPool

    @BeforeEach
    fun setUp() {
        pool = BufferPool(bufferSize = 1024, maxPoolSize = 4)
    }

    @Nested
    @DisplayName("acquire()")
    inner class Acquire {
        @Test
        @DisplayName("returns buffer of correct size")
        fun returnsBufferOfCorrectSize() {
            val buffer = pool.acquire()

            assertThat(buffer.capacity()).isEqualTo(1024)
        }

        @Test
        @DisplayName("returns cleared buffer")
        fun returnsClearedBuffer() {
            val buffer = pool.acquire()

            assertThat(buffer.position()).isEqualTo(0)
            assertThat(buffer.limit()).isEqualTo(buffer.capacity())
        }

        @Test
        @DisplayName("increments acquire count")
        fun incrementsAcquireCount() {
            pool.acquire()
            pool.acquire()
            pool.acquire()

            val stats = pool.getStats()
            assertThat(stats.totalAcquired).isEqualTo(3)
        }

        @Test
        @DisplayName("records miss when pool is empty")
        fun recordsMissWhenPoolEmpty() {
            pool.acquire()

            val stats = pool.getStats()
            assertThat(stats.missCount).isEqualTo(1)
            assertThat(stats.hitCount).isEqualTo(0)
        }

        @Test
        @DisplayName("records hit when buffer available in pool")
        fun recordsHitWhenBufferAvailable() {
            val buffer = pool.acquire()
            pool.release(buffer)
            pool.acquire()

            val stats = pool.getStats()
            assertThat(stats.hitCount).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("release()")
    inner class Release {
        @Test
        @DisplayName("increments release count")
        fun incrementsReleaseCount() {
            val buffer = pool.acquire()
            pool.release(buffer)

            val stats = pool.getStats()
            assertThat(stats.totalReleased).isEqualTo(1)
        }

        @Test
        @DisplayName("adds buffer to pool")
        fun addsBufferToPool() {
            val buffer = pool.acquire()
            pool.release(buffer)

            val stats = pool.getStats()
            assertThat(stats.currentPoolSize).isEqualTo(1)
        }

        @Test
        @DisplayName("respects max pool size")
        fun respectsMaxPoolSize() {
            // Acquire and release more buffers than max pool size
            val buffers = (1..10).map { pool.acquire() }
            buffers.forEach { pool.release(it) }

            val stats = pool.getStats()
            assertThat(stats.currentPoolSize).isAtMost(4)
        }

        @Test
        @DisplayName("rejects buffers of wrong size")
        fun rejectsBuffersOfWrongSize() {
            val wrongSizeBuffer = ByteBuffer.allocate(512)
            pool.release(wrongSizeBuffer)

            val stats = pool.getStats()
            assertThat(stats.currentPoolSize).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("clear()")
    inner class Clear {
        @Test
        @DisplayName("removes all buffers from pool")
        fun removesAllBuffers() {
            val buffers = (1..4).map { pool.acquire() }
            buffers.forEach { pool.release(it) }

            pool.clear()

            val stats = pool.getStats()
            assertThat(stats.currentPoolSize).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("getStats()")
    inner class GetStats {
        @Test
        @DisplayName("returns correct buffer size")
        fun returnsCorrectBufferSize() {
            val stats = pool.getStats()

            assertThat(stats.bufferSize).isEqualTo(1024)
        }

        @Test
        @DisplayName("returns correct max pool size")
        fun returnsCorrectMaxPoolSize() {
            val stats = pool.getStats()

            assertThat(stats.maxPoolSize).isEqualTo(4)
        }

        @Test
        @DisplayName("calculates hit rate correctly")
        fun calculatesHitRateCorrectly() {
            // 2 misses (initial acquires)
            val buffer1 = pool.acquire()
            val buffer2 = pool.acquire()

            // Release and reacquire (2 hits)
            pool.release(buffer1)
            pool.release(buffer2)
            pool.acquire()
            pool.acquire()

            val stats = pool.getStats()
            // 4 total acquires, 2 hits = 50% hit rate
            assertThat(stats.hitRate).isWithin(0.01f).of(0.5f)
        }

        @Test
        @DisplayName("calculates memory saved correctly")
        fun calculatesMemorySavedCorrectly() {
            val buffer = pool.acquire()
            pool.release(buffer)

            // Reuse buffer 3 times
            repeat(3) {
                val reused = pool.acquire()
                pool.release(reused)
            }

            val stats = pool.getStats()
            // 3 hits * 1024 bytes = 3072 bytes saved
            assertThat(stats.memorySaved).isEqualTo(3 * 1024L)
        }
    }

    @Nested
    @DisplayName("resetStats()")
    inner class ResetStats {
        @Test
        @DisplayName("resets all counters")
        fun resetsAllCounters() {
            // Generate some stats
            val buffer = pool.acquire()
            pool.release(buffer)
            pool.acquire()

            pool.resetStats()

            val stats = pool.getStats()
            assertThat(stats.totalAllocated).isEqualTo(0)
            assertThat(stats.totalAcquired).isEqualTo(0)
            assertThat(stats.totalReleased).isEqualTo(0)
            assertThat(stats.hitCount).isEqualTo(0)
            assertThat(stats.missCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("Sizes companion object")
    inner class SizesCompanionObject {
        @Test
        @DisplayName("defines standard buffer sizes")
        fun definesStandardBufferSizes() {
            assertThat(BufferPool.Companion.Sizes.ETHERNET_MTU).isEqualTo(1500)
            assertThat(BufferPool.Companion.Sizes.UDP_PACKET).isEqualTo(1600)
            assertThat(BufferPool.Companion.Sizes.TCP_BUFFER).isEqualTo(16384)
            assertThat(BufferPool.Companion.Sizes.HEADER).isEqualTo(256)
        }
    }

    @Nested
    @DisplayName("Thread safety")
    inner class ThreadSafety {
        @Test
        @DisplayName("handles concurrent access")
        fun handlesConcurrentAccess() {
            val pool = BufferPool(bufferSize = 256, maxPoolSize = 8)
            val threads =
                (1..10).map { threadId ->
                    Thread {
                        repeat(100) {
                            val buffer = pool.acquire()
                            // Simulate some work
                            buffer.putInt(threadId)
                            Thread.sleep(1)
                            pool.release(buffer)
                        }
                    }
                }

            threads.forEach { it.start() }
            threads.forEach { it.join() }

            val stats = pool.getStats()
            assertThat(stats.totalAcquired).isEqualTo(1000)
            assertThat(stats.totalReleased).isEqualTo(1000)
        }
    }
}
