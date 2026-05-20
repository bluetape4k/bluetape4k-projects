package io.bluetape4k.redis.lettuce.atomic

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.AbstractLettuceTest
import io.bluetape4k.redis.lettuce.LettuceClients
import io.bluetape4k.redis.lettuce.LettuceTestUtils
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.codec.StringCodec
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class LettuceSuspendAtomicLongTest: AbstractLettuceTest() {

    companion object: KLoggingChannel() {
        private val connection by lazy { LettuceClients.connect(LettuceTestUtils.client, StringCodec.UTF8) }
    }

    private lateinit var atomicLong: LettuceAtomicLong

    @BeforeEach
    fun setup() {
        atomicLong = LettuceAtomicLong(connection, randomName(), initialValue = 0L)
    }

    private fun suspendAtomicLong(): LettuceSuspendAtomicLong =
        LettuceSuspendAtomicLong(connection, atomicLong.key)

    // =========================================================================
    // 코루틴 기본 테스트
    // =========================================================================

    @Test
    fun `get and set`() = runSuspendIO {
        val counter = suspendAtomicLong()
        counter.set(42L)
        counter.get() shouldBeEqualTo 42L
    }

    @Test
    fun `incrementAndGetSuspending`() = runSuspendIO {
        val counter = suspendAtomicLong()
        counter.incrementAndGet() shouldBeEqualTo 1L
        counter.incrementAndGet() shouldBeEqualTo 2L
    }

    @Test
    fun `decrementAndGetSuspending`() = runSuspendIO {
        val counter = suspendAtomicLong()
        counter.set(5L)
        counter.decrementAndGet() shouldBeEqualTo 4L
    }

    @Test
    fun `addAndGetSuspending`() = runSuspendIO {
        val counter = suspendAtomicLong()
        counter.addAndGet(10L) shouldBeEqualTo 10L
        counter.addAndGet(5L) shouldBeEqualTo 15L
    }

    @Test
    fun `getAndSetSuspending`() = runSuspendIO {
        val counter = suspendAtomicLong()
        counter.set(10L)
        counter.getAndSet(20L) shouldBeEqualTo 10L
        counter.get() shouldBeEqualTo 20L
    }

    @Test
    fun `compareAndSetSuspending - 성공`() = runSuspendIO {
        val counter = suspendAtomicLong()
        counter.set(10L)
        counter.compareAndSet(10L, 20L).shouldBeTrue()
        counter.get() shouldBeEqualTo 20L
    }

    @Test
    fun `코루틴 동시성 - 여러 코루틴에서 incrementAndGetSuspending`() = runSuspendIO {
        val count = 100

        val jobs = List(count) {
            async {
                val counter = LettuceSuspendAtomicLong(connection, atomicLong.key)
                counter.incrementAndGet()
            }
        }
        jobs.awaitAll()

        suspendAtomicLong().get() shouldBeEqualTo count.toLong()
    }

    // =========================================================================
    // SuspendedJobTester 동시성 테스트
    // =========================================================================

    @Test
    fun `SuspendedJobTester - 코루틴 동시 incrementAndGet 원자성 검증`() = runSuspendIO {
        val workers = 8
        val rounds = 50

        SuspendedJobTester()
            .workers(workers)
            .rounds(rounds)
            .add {
                val counter = LettuceSuspendAtomicLong(connection, atomicLong.key)
                counter.incrementAndGet()
            }
            .run()

        // SuspendedJobTester: 총 실행 횟수 = rounds * blockCount (workers는 병렬도만 제어)
        suspendAtomicLong().get() shouldBeEqualTo rounds.toLong()
    }

    @Test
    fun `SuspendedJobTester - 코루틴 동시 addAndGet 원자성 검증`() = runSuspendIO {
        val workers = 5
        val rounds = 20
        val delta = 3L

        SuspendedJobTester()
            .workers(workers)
            .rounds(rounds)
            .add {
                val counter = LettuceSuspendAtomicLong(connection, atomicLong.key)
                counter.addAndGet(delta)
            }
            .run()

        // SuspendedJobTester: 총 실행 횟수 = rounds * blockCount (workers는 병렬도만 제어)
        suspendAtomicLong().get() shouldBeEqualTo (rounds * delta)
    }
}
