package io.bluetape4k.junit5.concurrency

import io.bluetape4k.junit5.tester.StressTester
import io.bluetape4k.junit5.tester.StressTester.Companion.DEFAULT_ROUNDS_PER_WORKER
import io.bluetape4k.junit5.tester.StressTester.Companion.MAX_ROUNDS_PER_WORKER
import io.bluetape4k.junit5.tester.StressTester.Companion.MIN_ROUNDS_PER_WORKER
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import java.util.concurrent.StructuredTaskScope
import java.util.concurrent.ThreadFactory

/**
 * Java 21의 Virtual threads 환경에서 테스트 코드를 실행하는 유틸리티 클래스입니다.
 *
 * ```
 * StructuredTaskScopeTester()
 *   .rounds(4 * Runtime.getRuntime().availableProcessors())
 *   .add {
 *      println("Hello, World!")
 *   }
 *   .add {
 *      println("Hello, Kotlin!")
 *   }
 *   .run()
 * ```
 *
 * @see [MultithreadingTester]
 * @see [io.bluetape4k.junit5.coroutines.SuspendedJobTester]
 */
class StructuredTaskScopeTester: StressTester<StructuredTaskScopeTester> {

    companion object: KLogging()

    private var roundsPerWorker: Int = DEFAULT_ROUNDS_PER_WORKER

    private val testBlocks = mutableListOf<() -> Unit>()
    private var factory: ThreadFactory? = null

    fun withFactory(factory: ThreadFactory) = apply {
        this.factory = factory
    }

    @Deprecated(
        message = "Use rounds(value) for consistent naming across testers.",
        replaceWith = ReplaceWith("rounds(value)")
    )
    fun roundsPerTask(value: Int) = apply {
        rounds(value)
    }

    /**
     * 공통 설정명: task 실행 라운드 수를 지정합니다.
     */
    override fun rounds(value: Int) = apply {
        require(value in MIN_ROUNDS_PER_WORKER..MAX_ROUNDS_PER_WORKER) {
            "Invalid roundsPerTask: [$value] -- must be range in $MIN_ROUNDS_PER_WORKER..$MAX_ROUNDS_PER_WORKER"
        }
        roundsPerWorker = value
    }

    fun add(testBlock: () -> Unit) = apply {
        testBlocks.add(testBlock)
    }

    fun addAll(vararg testBlocks: () -> Unit) = apply {
        this.testBlocks.addAll(testBlocks)
    }

    fun run() {
        check(testBlocks.isNotEmpty()) {
            "No test blocks added. Please add test blocks using add() method."
        }

        val factory = this.factory ?: Thread.ofVirtual().factory()
        StructuredTaskScope.ShutdownOnFailure("a", factory).use { scope ->
            repeat(roundsPerWorker) {
                testBlocks.forEach { testBlocks ->
                    scope.fork {
                        testBlocks.invoke()
                    }
                }
            }

            scope.join().throwIfFailed {
                log.error(it) { "Test blocks failed with exception." }
                throw it
            }
        }
    }
}
