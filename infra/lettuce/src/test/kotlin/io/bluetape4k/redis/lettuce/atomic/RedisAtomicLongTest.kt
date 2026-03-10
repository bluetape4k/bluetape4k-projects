package io.bluetape4k.redis.lettuce.atomic

import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisAtomicLongTest: AbstractLettuceTest() {

    companion object: KLoggingChannel()

    private lateinit var atomicLong: RedisAtomicLong

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        atomicLong = RedisAtomicLong(connection, randomName(), initialValue = 0L)
    }

    // =========================================================================
    // 동기 테스트
    // =========================================================================

    @Test
    fun `초기값 확인`() {
        atomicLong.get() shouldBeEqualTo 0L
    }

    @Test
    fun `set and get`() {
        atomicLong.set(42L)
        atomicLong.get() shouldBeEqualTo 42L
    }

    @Test
    fun `incrementAndGet`() {
        atomicLong.incrementAndGet() shouldBeEqualTo 1L
        atomicLong.incrementAndGet() shouldBeEqualTo 2L
        atomicLong.incrementAndGet() shouldBeEqualTo 3L
    }

    @Test
    fun `decrementAndGet`() {
        atomicLong.set(5L)
        atomicLong.decrementAndGet() shouldBeEqualTo 4L
        atomicLong.decrementAndGet() shouldBeEqualTo 3L
    }

    @Test
    fun `addAndGet`() {
        atomicLong.addAndGet(10L) shouldBeEqualTo 10L
        atomicLong.addAndGet(5L) shouldBeEqualTo 15L
        atomicLong.addAndGet(-3L) shouldBeEqualTo 12L
    }

    @Test
    fun `getAndSet`() {
        atomicLong.set(10L)
        val old = atomicLong.getAndSet(20L)
        old shouldBeEqualTo 10L
        atomicLong.get() shouldBeEqualTo 20L
    }

    @Test
    fun `getAndIncrement`() {
        atomicLong.set(5L)
        atomicLong.getAndIncrement() shouldBeEqualTo 5L
        atomicLong.get() shouldBeEqualTo 6L
    }

    @Test
    fun `getAndDecrement`() {
        atomicLong.set(5L)
        atomicLong.getAndDecrement() shouldBeEqualTo 5L
        atomicLong.get() shouldBeEqualTo 4L
    }

    @Test
    fun `getAndAdd`() {
        atomicLong.set(10L)
        atomicLong.getAndAdd(5L) shouldBeEqualTo 10L
        atomicLong.get() shouldBeEqualTo 15L
    }

    @Test
    fun `compareAndSet - 성공`() {
        atomicLong.set(10L)
        atomicLong.compareAndSet(10L, 20L).shouldBeTrue()
        atomicLong.get() shouldBeEqualTo 20L
    }

    @Test
    fun `compareAndSet - 실패 (값 불일치)`() {
        atomicLong.set(10L)
        atomicLong.compareAndSet(5L, 20L).shouldBeFalse()
        atomicLong.get() shouldBeEqualTo 10L
    }

    @Test
    fun `동시성 - 여러 스레드에서 incrementAndGet`() {
        val threadCount = 10
        val iterationsPerThread = 100
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        val connection = LettuceClients.connect(client, StringCodec.UTF8)

        repeat(threadCount) {
            executor.submit {
                val counter = RedisAtomicLong(connection, atomicLong.key)
                repeat(iterationsPerThread) {
                    counter.incrementAndGet()
                }
                latch.countDown()
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        executor.shutdown()
        atomicLong.get() shouldBeEqualTo (threadCount * iterationsPerThread).toLong()
    }

    // =========================================================================
    // 비동기 테스트
    // =========================================================================

    @Test
    fun `getAsync and setAsync`() {
        atomicLong.setAsync(100L).get()
        atomicLong.getAsync().get() shouldBeEqualTo 100L
    }

    @Test
    fun `incrementAndGetAsync`() {
        atomicLong.incrementAndGetAsync().get() shouldBeEqualTo 1L
        atomicLong.incrementAndGetAsync().get() shouldBeEqualTo 2L
    }

    @Test
    fun `compareAndSetAsync`() {
        atomicLong.setAsync(10L).get()
        atomicLong.compareAndSetAsync(10L, 20L).get().shouldBeTrue()
        atomicLong.compareAndSetAsync(10L, 30L).get().shouldBeFalse()
        atomicLong.getAsync().get() shouldBeEqualTo 20L
    }

    // =========================================================================
    // 코루틴 테스트
    // =========================================================================

    @Test
    fun `getSuspending and setSuspending`() = runSuspendIO {
        atomicLong.setSuspending(42L)
        atomicLong.getSuspending() shouldBeEqualTo 42L
    }

    @Test
    fun `incrementAndGetSuspending`() = runSuspendIO {
        atomicLong.incrementAndGetSuspending() shouldBeEqualTo 1L
        atomicLong.incrementAndGetSuspending() shouldBeEqualTo 2L
    }

    @Test
    fun `decrementAndGetSuspending`() = runSuspendIO {
        atomicLong.setSuspending(5L)
        atomicLong.decrementAndGetSuspending() shouldBeEqualTo 4L
    }

    @Test
    fun `addAndGetSuspending`() = runSuspendIO {
        atomicLong.addAndGetSuspending(10L) shouldBeEqualTo 10L
        atomicLong.addAndGetSuspending(5L) shouldBeEqualTo 15L
    }

    @Test
    fun `getAndSetSuspending`() = runSuspendIO {
        atomicLong.setSuspending(10L)
        atomicLong.getAndSetSuspending(20L) shouldBeEqualTo 10L
        atomicLong.getSuspending() shouldBeEqualTo 20L
    }

    @Test
    fun `compareAndSetSuspending - 성공`() = runSuspendIO {
        atomicLong.setSuspending(10L)
        atomicLong.compareAndSetSuspending(10L, 20L).shouldBeTrue()
        atomicLong.getSuspending() shouldBeEqualTo 20L
    }

    @Test
    fun `코루틴 동시성 - 여러 코루틴에서 incrementAndGetSuspending`() = runSuspendIO {
        val count = 100
        val connection = LettuceClients.connect(client, StringCodec.UTF8)

        val jobs = List(count) {
            async {
                val counter = RedisAtomicLong(connection, atomicLong.key)
                counter.incrementAndGetSuspending()
            }
        }
        jobs.awaitAll()

        atomicLong.getSuspending() shouldBeEqualTo count.toLong()
    }
}
