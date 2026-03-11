package io.bluetape4k.redis.lettuce.atomic

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.codec.StringCodec
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class LettuceAtomicLongTest: AbstractLettuceTest() {

    companion object: KLogging()

    private lateinit var atomicLong: LettuceAtomicLong

    @BeforeEach
    fun setup() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        atomicLong = LettuceAtomicLong(connection, randomName(), initialValue = 0L)
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
                val counter = LettuceAtomicLong(connection, atomicLong.key)
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
    // MultithreadingTester 동시성 테스트
    // =========================================================================

    @Test
    fun `MultithreadingTester - 동시 incrementAndGet 원자성 검증`() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val workers = 8
        val rounds = 50

        MultithreadingTester()
            .workers(workers)
            .rounds(rounds)
            .add {
                val counter = LettuceAtomicLong(connection, atomicLong.key)
                counter.incrementAndGet()
            }
            .run()

        atomicLong.get() shouldBeEqualTo (workers * rounds).toLong()
    }

    @Test
    fun `MultithreadingTester - 동시 addAndGet 원자성 검증`() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val workers = 5
        val rounds = 20
        val delta = 3L

        MultithreadingTester()
            .workers(workers)
            .rounds(rounds)
            .add {
                val counter = LettuceAtomicLong(connection, atomicLong.key)
                counter.addAndGet(delta)
            }
            .run()

        atomicLong.get() shouldBeEqualTo (workers * rounds * delta)
    }

    // =========================================================================
    // StructuredTaskScopeTester 동시성 테스트
    // =========================================================================

    @Test
    fun `StructuredTaskScopeTester - 동시 incrementAndGet 원자성 검증`() {
        val connection = LettuceClients.connect(client, StringCodec.UTF8)
        val rounds = 100

        StructuredTaskScopeTester()
            .rounds(rounds)
            .add {
                val counter = LettuceAtomicLong(connection, atomicLong.key)
                counter.incrementAndGet()
            }
            .run()

        atomicLong.get() shouldBeEqualTo rounds.toLong()
    }
}
