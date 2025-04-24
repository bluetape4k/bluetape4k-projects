package io.bluetape4k.junit5.coroutines

import io.bluetape4k.junit5.utils.MultiException
import io.bluetape4k.logging.KLogging
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
 * MultijobTester()
 *    .numThreads(Runtimex.availableProcessors())      // 테스트 코드들이 실행될 Thread 수
 *    .roundsPerJob(4)                                 // 테스트 코드마다 4번씩 실행
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
 * @see [io.bluetape4k.junit5.concurrency.VirtualthreadTester]
 */
class SuspendedJobTester {

    companion object: KLogging() {
        const val DEFAULT_JOB_SIZE: Int = 64
        const val DEFAULT_ROUNDS_PER_JOB: Int = 100

        const val MIN_JOB_SIZE: Int = 2
        const val MAX_JOB_SIZE: Int = 2000

        const val MIN_ROUNDS_PER_JOB: Int = 1
        const val MAX_ROUNDS_PER_JOB: Int = 100_000
    }

    private var numThreads = DEFAULT_JOB_SIZE
    private var roundsPerJob = DEFAULT_ROUNDS_PER_JOB
    private val suspendBlocks = mutableListOf<suspend () -> Unit>()

    fun numThreads(value: Int) = apply {
        require(value in MIN_JOB_SIZE..MAX_JOB_SIZE) {
            "Invalid numJobs: [$value] -- must be range in $MIN_JOB_SIZE..$MAX_JOB_SIZE"
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
