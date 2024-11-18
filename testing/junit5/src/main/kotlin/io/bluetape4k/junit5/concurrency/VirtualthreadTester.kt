package io.bluetape4k.junit5.concurrency

import io.bluetape4k.junit5.utils.MultiException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import java.util.concurrent.Callable
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Virtual threads 환경에서 테스트 코드를 실행하는 유틸리티 클래스입니다.
 *
 * ```
 * VirtualthreadTester()
 *   .numThreads(Runtime.getRuntime().availableProcessors())
 *   .roundsPerThread(4)
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
 * @see [io.bluetape4k.junit5.coroutines.MultijobTester]
 */
class VirtualthreadTester {

    companion object: KLogging() {
        const val DEFAULT_THREAD_SIZE: Int = 100
        const val DEFAULT_ROUNDS_PER_THREAD: Int = 100

        const val MIN_THREAD_SIZE: Int = 2
        const val MAX_THREAD_SIZE: Int = 2000

        const val MIN_ROUNDS_PER_THREAD: Int = 1
        const val MAX_ROUNDS_PER_THREAD: Int = 100_000
    }

    private var numThreads = DEFAULT_THREAD_SIZE
    private var roundsPerThread = DEFAULT_ROUNDS_PER_THREAD
    private val runnables = mutableListOf<Runnable>()

    private val futures = mutableListOf<Future<Unit>>()
    private val executor by lazy { TestingExecutors.newVirtualThreadPerTaskExecutor() }

    fun numThreads(value: Int) = apply {
        require(value in MIN_THREAD_SIZE..MAX_THREAD_SIZE) {
            "Invalid numThreads: [$value] -- must be range in $MIN_THREAD_SIZE..$MAX_THREAD_SIZE"
        }
        numThreads = value
    }

    fun roundsPerThread(value: Int) = apply {
        require(value in MIN_ROUNDS_PER_THREAD..MAX_ROUNDS_PER_THREAD) {
            "Invalid roundsPerThread: [$value] -- must be range in $MIN_ROUNDS_PER_THREAD..$MAX_ROUNDS_PER_THREAD"
        }
        roundsPerThread = value
    }

    fun add(testBlock: () -> Unit) = apply {
        runnables.add(testBlock)
    }

    fun add(testBlock: Runnable) = apply {
        runnables.add(testBlock)
    }

    fun addAll(vararg testBlocks: () -> Unit) = apply {
        runnables.addAll(testBlocks.map { Runnable { it.invoke() } })
    }

    fun addAll(testBlocks: Collection<() -> Unit>) = apply {
        runnables.addAll(testBlocks.map { Runnable { it.invoke() } })
    }

    fun run() {
        check(runnables.isNotEmpty()) {
            "No test blocks added. Please add test blocks using add() method."
        }
        check(numThreads >= runnables.size) {
            "Number of threads[$numThreads] must be greater than or equal to the number of test blocks[${runnables.size}]."
        }

        val me = MultiException()

        try {
            startWorkerThreads(me)
            Thread.yield()
            joinWorkerThreads()
        } finally {
            shutdownExecutor()
            me.throwIfNotEmpty()
        }
    }

    private fun startWorkerThreads(me: MultiException) {
        log.trace { "Start virtual threads ... numThreads=$numThreads" }

        val tasks = List(numThreads * roundsPerThread) {
            Callable {
                try {
                    val runnable = runnables[it % runnables.size]
                    runnable.run()
                } catch (t: Throwable) {
                    me.add(t)
                }
            }
        }

        futures.clear()
        futures.addAll(executor.invokeAll(tasks))
    }

    private fun joinWorkerThreads() {
        log.trace { "Join virtual threads ..." }

        do {
            var foundAliveFuture = false
            futures.forEach { future ->
                try {
                    future.get(100, TimeUnit.MILLISECONDS)
                    foundAliveFuture = !future.isDone
                } catch (e: InterruptedException) {
                    throw RuntimeException("Get interrupted", e)
                }
            }
        } while (foundAliveFuture)
    }

    private fun shutdownExecutor() {
        runCatching { executor.shutdownNow() }
    }
}
