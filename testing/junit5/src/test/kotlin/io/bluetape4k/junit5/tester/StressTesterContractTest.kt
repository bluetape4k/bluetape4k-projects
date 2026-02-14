package io.bluetape4k.junit5.tester

import io.bluetape4k.junit5.concurrency.MultithreadingTester
import io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledOnJre
import org.junit.jupiter.api.condition.JRE
import java.util.concurrent.atomic.AtomicInteger

class StressTesterContractTest {

    @Test
    fun `멀티스레드 테스터는 WorkerStressTester 계약으로 실행된다`() {
        val counter = AtomicInteger(0)
        val tester = configureWorkerTester(MultithreadingTester(), workers = 2, rounds = 3)

        tester
            .add { counter.incrementAndGet() }
            .run()

        counter.get() shouldBeEqualTo 6
    }

    @EnabledOnJre(JRE.JAVA_21)
    @Test
    fun `버추얼스레드 테스터는 StressTester 계약으로 실행된다`() {
        val counter = AtomicInteger(0)
        val tester = configureRoundsTester(StructuredTaskScopeTester(), rounds = 4)

        tester
            .add { counter.incrementAndGet() }
            .run()

        counter.get() shouldBeEqualTo 4
    }

    @Test
    fun `코루틴 테스터는 WorkerStressTester 계약으로 실행된다`() = runTest {
        val counter = AtomicInteger(0)
        val tester = configureWorkerTester(SuspendedJobTester(), workers = 2, rounds = 5)

        tester
            .add { counter.incrementAndGet() }
            .run()

        counter.get() shouldBeEqualTo 5
    }

    private fun <T: StressTester<T>> configureRoundsTester(tester: T, rounds: Int): T =
        tester.rounds(rounds)

    private fun <T: WorkerStressTester<T>> configureWorkerTester(tester: T, workers: Int, rounds: Int): T =
        tester.workers(workers).rounds(rounds)
}
