package io.bluetape4k.junit5.coroutines

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
 *    .numThreads(Runtimex.availableProcessors())                    // 테스트 코드들이 실행될 Thread 수
 *    .roundsPerJob(4 * Runtime.getRuntime().availableProcessors())  // 테스트 코드마다 4 * CPU core 번씩 실행
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
class SuspendedJobTester {

    companion object: KLoggingChannel() {
        const val DEFAULT_NUM_THREADS: Int = 16
        const val MIN_NUM_THREADS: Int = 2
        const val MAX_NUM_THREADS: Int = 2000

        const val DEFAULT_ROUNDS_PER_JOB: Int = 100
        const val MIN_ROUNDS_PER_JOB: Int = 1
        const val MAX_ROUNDS_PER_JOB: Int = 1_000_000
    }

    private var numThreads = DEFAULT_NUM_THREADS
    private var roundsPerJob = DEFAULT_ROUNDS_PER_JOB
    private val suspendBlocks = mutableListOf<suspend () -> Unit>()

    fun numThreads(value: Int) = apply {
        require(value in MIN_NUM_THREADS..MAX_NUM_THREADS) {
            "Invalid numJobs: [$value] -- must be range in $MIN_NUM_THREADS..$MAX_NUM_THREADS"
        }
        numThreads = value
    }

    fun roundsPerJob(value: Int) = apply {
        require(value in MIN_ROUNDS_PER_JOB..MAX_ROUNDS_PER_JOB) {
            "Invalid roundsPerJob: [$value] -- must be range in $MIN_ROUNDS_PER_JOB..$MAX_ROUNDS_PER_JOB"
        }
        roundsPerJob = value
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
        newFixedThreadPoolContext(numThreads, "multi-job").use { dispatcher ->
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

        List(roundsPerJob) {
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
