package io.bluetape4k.junit5.coroutines

import io.bluetape4k.junit5.tester.StressTester.Companion.DEFAULT_ROUNDS_PER_WORKER
import io.bluetape4k.junit5.tester.StressTester.Companion.MAX_ROUNDS_PER_WORKER
import io.bluetape4k.junit5.tester.StressTester.Companion.MIN_ROUNDS_PER_WORKER
import io.bluetape4k.junit5.tester.WorkerStressTester
import io.bluetape4k.junit5.tester.WorkerStressTester.Companion.DEFAULT_WORKER_SIZE
import io.bluetape4k.junit5.tester.WorkerStressTester.Companion.MAX_WORKER_SIZE
import io.bluetape4k.junit5.tester.WorkerStressTester.Companion.MIN_WORKER_SIZE
import io.bluetape4k.junit5.utils.MultiException
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.yield

/**
 * Coroutines 를 사용하여 테스트 코드들을 많은 수의 Job으로 만들어 Async/Non-Blocking 방식으로 실행합니다.
 * 이렇게 하므로서, 테스트 코드들이 서로 영향을 주지 않고 동시에 실행될 수 있는지 확인합니다.
 *
 * ```
 * SuspendedJobTester()
 *    .workers(Runtimex.availableProcessors())                    // 테스트 코드들이 실행될 Thread 수
 *    .rounds(4 * Runtime.getRuntime().availableProcessors())  // 테스트 코드마다 4 * CPU core 번씩 실행
 *    .add {
 *          // 테스트 코드 1
 *    }
 *    .add {
 *          // 테스트 코드 2
 *    }
 *    .run()
 * ```
 *
 * @see [io.bluetape4k.junit5.concurrency.MultithreadingTester]
 * @see [io.bluetape4k.junit5.concurrency.StructuredTaskScopeTester]
 */
class SuspendedJobTester: WorkerStressTester<SuspendedJobTester> {

    companion object: KLoggingChannel()

    private var numWorkers = DEFAULT_WORKER_SIZE
    private var roundPerWorker = DEFAULT_ROUNDS_PER_WORKER
    private val suspendBlocks = mutableListOf<suspend () -> Unit>()

    @Deprecated(
        message = "Use workers(value) for consistent naming across testers.",
        replaceWith = ReplaceWith("workers(value)")
    )
    fun numThreads(value: Int) = apply {
        applyNumWorks(value)
    }

    private fun applyNumWorks(value: Int) {
        require(value in MIN_WORKER_SIZE..MAX_WORKER_SIZE) {
            "Invalid numJobs: [$value] -- must be range in $MIN_WORKER_SIZE..$MAX_WORKER_SIZE"
        }
        numWorkers = value
    }

    /**
     * 공통 설정명: 실행 worker(thread) 수를 지정합니다.
     */
    override fun workers(value: Int) = apply {
        applyNumWorks(value)
    }

    @Deprecated(
        message = "Use rounds(value) for consistent naming across testers.",
        replaceWith = ReplaceWith("rounds(value)")
    )
    fun roundsPerJob(value: Int) = apply {
        applyRoundsPerJob(value)
    }

    private fun applyRoundsPerJob(value: Int) {
        require(value in MIN_ROUNDS_PER_WORKER..MAX_ROUNDS_PER_WORKER) {
            "Invalid roundsPerJob: [$value] -- must be range in $MIN_ROUNDS_PER_WORKER..$MAX_ROUNDS_PER_WORKER"
        }
        roundPerWorker = value
    }

    /**
     * 공통 설정명: worker당 실행 라운드 수를 지정합니다.
     */
    override fun rounds(value: Int) = apply {
        applyRoundsPerJob(value)
    }

    fun add(testBlock: suspend () -> Unit) = apply {
        suspendBlocks.add(testBlock)
    }

    fun addAll(vararg testBlocks: suspend () -> Unit) = apply {
        suspendBlocks.addAll(testBlocks)
    }

    fun addAll(testBlocks: Collection<suspend () -> Unit>) = apply {
        suspendBlocks.addAll(testBlocks)
    }

    suspend fun run() {
        check(suspendBlocks.isNotEmpty()) { "실행할 코드가 없습니다. add 로 추가해주세요." }

        val me = MultiException()
        newFixedThreadPoolContext(numWorkers, "multi-job").use { dispatcher ->
            val jobs = launchJobs(dispatcher, me)
            yield()
            jobs.joinAll()
        }
        me.throwIfNotEmpty()
    }

    private suspend fun launchJobs(
        dispatcher: CoroutineDispatcher,
        me: MultiException,
    ): List<Job> = coroutineScope {
        log.trace { "Start multi job testing ..." }

        List(roundPerWorker) {
            suspendBlocks.map { block ->
                launch(dispatcher) {
                    try {
                        block.invoke()
                    } catch (e: Throwable) {
                        me.add(e)
                    }
                }
            }
        }.flatten()
    }
}
