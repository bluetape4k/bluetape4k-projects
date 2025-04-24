package io.bluetape4k.junit5.coroutines

import io.bluetape4k.junit5.utils.MultiException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.ExecutorCoroutineDispatcher
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
 *    .numThreads(Runtimex.availableProcessors())
 *    .roundsPerJob(4)
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
@Deprecated("Use SuspendedJobTester instead", replaceWith = ReplaceWith("SuspendedJobTester"))
class MultijobTester {

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

    private val workerJobs = mutableListOf<Job>()
    private lateinit var workerDispatcher: ExecutorCoroutineDispatcher

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
        check(numThreads >= suspendBlocks.size) {
            "numThreads: [$numThreads] must be greater than or equal to the number of test blocks: [${suspendBlocks.size}]"
        }

        val me = MultiException()
        try {
            startJobs(me)
            yield()
            awaitJobs()
        } finally {
            shutdownDispatcher()
            me.throwIfNotEmpty()
        }
    }

    private suspend fun startJobs(me: MultiException): Unit = coroutineScope {
        log.trace { "Start multi job testing ..." }

        workerDispatcher = newFixedThreadPoolContext(numThreads, "multi-job")

        val jobs = List(numThreads * roundsPerJob) {
            val block = suspendBlocks[it % suspendBlocks.size]
            launch(workerDispatcher) {
                try {
                    block()
                } catch (e: Throwable) {
                    me.add(e)
                }
            }
        }
        workerJobs.clear()
        workerJobs.addAll(jobs)
    }

    private suspend fun awaitJobs() {
        log.trace { "Await multi jobs ..." }
        runCatching { workerJobs.joinAll() }

    }

    private fun shutdownDispatcher() {
        runCatching { workerDispatcher.close() }
    }
}
