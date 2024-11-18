package io.bluetape4k.junit5.concurrency

import io.bluetape4k.junit5.utils.MultiException
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.error
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * 멀티스레딩 환경에서 테스트 코드를 실행하는 유틸리티 클래스입니다.
 *
 * ```
 * MultithreadingTester()
 *    .numThreads(Runtimex.availableProcessors())
 *    .roundsPerThread(4)
 *    .add {
 *          println("Hello, World!")
 *     }
 *    .add {
 *          println("Hello, Kotlin!")
 *     }
 *    .run()
 * ```
 *
 * @see [VirtualthreadTester]
 * @see [healingpaper.kommons.junit5.coroutines.MultijobTester]
 */
class MultithreadingTester {

    companion object: KLogging() {
        const val DEFAULT_THREAD_SIZE: Int = 100
        const val DEFAULT_ROUNDS_PER_THREADS: Int = 100

        const val MIN_THREAD_SIZE: Int = 2
        const val MAX_THREAD_SIZE: Int = 2000

        const val MIN_ROUNDS_PER_THREAD: Int = 1
        const val MAX_ROUNDS_PER_THREAD: Int = 100_000
    }

    private var numThreads = DEFAULT_THREAD_SIZE
    private var roundsPerThread = DEFAULT_ROUNDS_PER_THREADS
    private val runnables = mutableListOf<() -> Unit>()

    private lateinit var executor: ExecutorService
    private val futures = CopyOnWriteArrayList<Future<*>>()

    /**
     * 테스트 시에 사용할 Thread 수를 지정합니다.
     */
    fun numThreads(value: Int) = apply {
        require(value in MIN_THREAD_SIZE..MAX_THREAD_SIZE) {
            "Invalid numThreads: [$value] -- must be range in $MIN_THREAD_SIZE..$MAX_THREAD_SIZE"
        }
        numThreads = value
    }

    /**
     * 테스트 코드 블럭을 쓰레드 당 수행할 횟수를 지정합니다.
     */
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
        runnables.add({ testBlock.run() })
    }

    fun addAll(vararg testBlocks: () -> Unit) = apply {
        runnables.addAll(testBlocks)
    }

    fun addAll(testBlocks: Collection<() -> Unit>) = apply {
        runnables.addAll(testBlocks)
    }

    /**
     * 멀티스레딩 환경에서 `add`로 추가한 테스트 코드 블럭을 실행합니다.
     *
     * @see numThreads
     * @see roundsPerThread
     */
    fun run() {
        check(runnables.isNotEmpty()) {
            "실행할 테스트 코드가 등록되지 않았습니다. add() 메소드를 사용하여 테스트 코드를 등록해주세요."
        }

        check(numThreads >= runnables.size) {
            "등록된 테스트 코드 블럭의 수[${runnables.size}]보다 적은 수의 쓰레드[$numThreads]로 테스트를 실행할 수 없습니다."
        }

        val me = MultiException()
        try {
            startWorkerThreads(me)
            Thread.yield()
            runCatching { joinWorkerThreads() }
        } finally {
            shutdownExecutor()
            me.throwIfNotEmpty()
        }
    }

    private fun startWorkerThreads(me: MultiException) {
        log.debug { "Start worker threads ... numThreads=$numThreads" }

        val factory = Thread.ofPlatform().name("multi-thread-tester-", 0).daemon(true).factory()
        executor = Executors.newFixedThreadPool(numThreads, factory)

        val tasks = List(numThreads * roundsPerThread) {
            val runnable = runnables[it % runnables.size]
            executor.submit {
                try {
                    runnable.invoke()
                } catch (t: Throwable) {
                    me.add(t)
                }
            }
        }
        futures.clear()
        futures.addAll(tasks)
    }

    private fun joinWorkerThreads() {
        log.debug { "Join worker threads ..." }

        do {
            var foundAliveFuture = false
            futures.forEach { future ->
                if (!future.isDone) {
                    foundAliveFuture = true
                    try {
                        future.get()
                    } catch (e: Exception) {
                        log.error(e) { "Error occurred while joining worker threads" }
                        throw RuntimeException("Get exception.", e)
                    }
                }
            }
        } while (foundAliveFuture)
    }

    private fun shutdownExecutor() {
        runCatching { executor.shutdownNow() }
    }
}
