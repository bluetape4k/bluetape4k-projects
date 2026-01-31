package io.bluetape4k.concurrent

import io.bluetape4k.collections.eclipse.stream.toFastList
import io.bluetape4k.collections.eclipse.toUnifiedSet
import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.junit5.coroutines.runSuspendDefault
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.utils.Runtimex
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import kotlin.test.assertFailsWith

class AtomicIntRoundrobinTest {

    companion object: KLogging()

    @Test
    fun `Parallel Stream에서 Atomic 한지 검증합니다`() {
        val size = 10_000
        val atomic = AtomicIntRoundrobin(size)

        val ids = List(size) { it }.parallelStream().map { atomic.next() }.toFastList()
        ids.size shouldBeEqualTo size
        ids.toUnifiedSet().size shouldBeEqualTo size
    }

    @Test
    fun `Round robin 방식으로 안정적으로 증가해야 합니다`() {
        val atomic = AtomicIntRoundrobin(4)

        atomic.get() shouldBeEqualTo 0

        val nums = List(8) { atomic.next() }
        nums shouldBeEqualTo listOf(1, 2, 3, 0, 1, 2, 3, 0)
    }

    @Test
    fun `기존 겂을 새로운 값으로 설정하기`() {
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
    fun `멀티 스레드 환경에서 라운드-로빈 방식으로 값을 증가시킨다`() {
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

        // availableProcessors 을 round robin 하기 때문에, (2*4) 배수이므로 항상 0 이어야 한다
        atomic.get() shouldBeEqualTo 0
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `Virtual Thread 환경에서 라운드-로빈 방식으로 값을 증가시킨다`() {
        val atomic = AtomicIntRoundrobin(Runtimex.availableProcessors)

        StructuredTaskScopeTester()
            .roundsPerTask(4 * Runtimex.availableProcessors * 2)
            .add {
                atomic.next().apply {
                    log.trace { "atomic=$this" }
                }
            }
            .run()

        // availableProcessors 을 round robin 하기 때문에, (2*4) 배수이므로 항상 0 이어야 한다
        atomic.get() shouldBeEqualTo 0
    }

    @Test
    fun `코루틴 멀티 Job 환경에서 라운드-로빈 방식으로 값을 증가시킨다`() = runSuspendDefault {
        val atomic = AtomicIntRoundrobin(Runtimex.availableProcessors)

        SuspendedJobTester()
            .numThreads(Runtimex.availableProcessors * 2)
            .roundsPerJob(Runtimex.availableProcessors * 2 * 4)
            .add {
                atomic.next().apply {
                    log.trace { "atomic=$this" }
                }
            }
            .run()

        // availableProcessors 을 round robin 하기 때문에, (2*4) 배수이므로 항상 0 이어야 한다
        atomic.get() shouldBeEqualTo 0
    }
}
