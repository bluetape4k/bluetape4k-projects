package io.bluetape4k.redis.lettuce.atomic

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.bluetape4k.utils.Runtimex
import io.lettuce.core.codec.StringCodec
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LettuceAtomicLongTest: AbstractLettuceTest() {

    companion object: KLogging() {
        private val connection by lazy {
            LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8)
        }
    }

    private lateinit var atomicLong: LettuceAtomicLong

    @BeforeEach
    fun setup() {
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
    fun `incrementAndGet - increment and get`() {
        atomicLong.incrementAndGet() shouldBeEqualTo 1L
        atomicLong.incrementAndGet() shouldBeEqualTo 2L
        atomicLong.incrementAndGet() shouldBeEqualTo 3L
    }

    @Test
    fun `decrementAndGet - decrement and get`() {
        atomicLong.set(5L)
        atomicLong.decrementAndGet() shouldBeEqualTo 4L
        atomicLong.decrementAndGet() shouldBeEqualTo 3L
    }

    @Test
    fun `addAndGet - add and get`() {
        atomicLong.addAndGet(10L) shouldBeEqualTo 10L
        atomicLong.addAndGet(5L) shouldBeEqualTo 15L
        atomicLong.addAndGet(-3L) shouldBeEqualTo 12L
    }

    @Test
    fun `getAndSet - get and set`() {
        atomicLong.set(10L)
        val old = atomicLong.getAndSet(20L)
        old shouldBeEqualTo 10L
        atomicLong.get() shouldBeEqualTo 20L
    }

    @Test
    fun `getAndIncrement - get and increment`() {
        atomicLong.set(5L)
        atomicLong.getAndIncrement() shouldBeEqualTo 5L
        atomicLong.get() shouldBeEqualTo 6L
    }

    @Test
    fun `getAndDecrement - get and decrement`() {
        atomicLong.set(5L)
        atomicLong.getAndDecrement() shouldBeEqualTo 5L
        atomicLong.get() shouldBeEqualTo 4L
    }

    @Test
    fun `getAndAdd - get and add`() {
        atomicLong.set(10L)
        atomicLong.getAndAdd(5L) shouldBeEqualTo 10L
        atomicLong.get() shouldBeEqualTo 15L
    }

    @Test
    fun `compareAndSet - compare and set`() {
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
        val workers = 2 * Runtimex.availableProcessors
        val iterationsPerThread = 100

        MultithreadingTester()
            .workers(workers)
            .rounds(iterationsPerThread)
            .add {
                val counter = LettuceAtomicLong(connection, atomicLong.key)
                counter.incrementAndGet()
            }
            .run()

        atomicLong.get() shouldBeEqualTo (workers * iterationsPerThread).toLong()
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
    fun `incrementAndGetAsync - increment and get asynchronously`() {
        atomicLong.incrementAndGetAsync().get() shouldBeEqualTo 1L
        atomicLong.incrementAndGetAsync().get() shouldBeEqualTo 2L
    }

    @Test
    fun `compareAndSetAsync - compare and set asynchronously`() {
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
        val workers = 2 * Runtimex.availableProcessors
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
        val workers = 2 * Runtimex.availableProcessors
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
