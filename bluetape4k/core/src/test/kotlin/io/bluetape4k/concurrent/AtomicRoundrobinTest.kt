package io.bluetape4k.concurrent

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.VirtualthreadTester
import io.bluetape4k.junit5.coroutines.MultijobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class AtomicRoundrobinTest {

    companion object: KLogging()

    @Test
    fun `Parallel Stream에서 Atomic 한가`() {
        val size = 10_000
        val atomic = AtomicIntRoundrobin(size)

        val ids = List(size) { it }.parallelStream().map { atomic.next() }.toList()
        ids.size shouldBeEqualTo size
        ids.toSet().size shouldBeEqualTo size
    }

    @Test
    fun `Round robin 방식으로 증가시키기`() {
        val atomic = AtomicIntRoundrobin(4)

        atomic.get() shouldBeEqualTo 0

        val nums = List(8) { atomic.next() }
        nums shouldBeEqualTo listOf(1, 2, 3, 0, 1, 2, 3, 0)
    }

    @Test
    fun `새로운 값 지정하기`() {
        val atomic = AtomicIntRoundrobin(16)

        atomic.next() shouldBeEqualTo 1
        atomic.next() shouldBeEqualTo 2

        atomic.set(1)

        atomic.get() shouldBeEqualTo 1
        atomic.next() shouldBeEqualTo 2

    }

    @Test
    fun `유효하지 않은 값 지정하면 예외가 발생한다`() {
        val atomic = AtomicIntRoundrobin(16)

        assertFailsWith<IllegalArgumentException> {
            atomic.set(Int.MAX_VALUE)
        }

        assertFailsWith<IllegalArgumentException> {
            atomic.set(Int.MIN_VALUE)
        }
    }

    @Test
    fun `increment round robin in multi-thread`() {
        val atomic = AtomicIntRoundrobin(Runtimex.availableProcessors)

        MultithreadingTester()
            .numThreads(Runtimex.availableProcessors * 2)
            .roundsPerThread(4)
            .add {
                atomic.next().apply {
                    log.trace { "atomic=$this" }
                }
            }
            .run()

        atomic.get() shouldBeEqualTo 0
    }

    @Test
    fun `increment round robin in virtual threads`() {
        val atomic = AtomicIntRoundrobin(Runtimex.availableProcessors)

        VirtualthreadTester()
            .numThreads(Runtimex.availableProcessors * 2)
            .roundsPerThread(4)
            .add {
                atomic.next().apply {
                    log.trace { "atomic=$this" }
                }
            }
            .run()

        atomic.get() shouldBeEqualTo 0
    }

    @Test
    fun `increment round robin in multi jobs`() = runSuspendDefault {
        val atomic = AtomicIntRoundrobin(Runtimex.availableProcessors)

        MultijobTester()
            .numThreads(Runtimex.availableProcessors * 2)
            .roundsPerJob(4)
            .add {
                atomic.next().apply {
                    log.trace { "atomic=$this" }
                }
            }
            .run()

        atomic.get() shouldBeEqualTo 0
    }
}
