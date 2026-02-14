package io.bluetape4k.junit5.concurrency

import io.bluetape4k.junit5.tester.StressTester.Companion.DEFAULT_ROUNDS_PER_WORKER
import io.bluetape4k.junit5.tester.StressTester.Companion.MAX_ROUNDS_PER_WORKER
import io.bluetape4k.junit5.tester.StressTester.Companion.MIN_ROUNDS_PER_WORKER
import io.bluetape4k.junit5.tester.WorkerStressTester
import io.bluetape4k.junit5.tester.WorkerStressTester.Companion.DEFAULT_WORKER_SIZE
import io.bluetape4k.junit5.tester.WorkerStressTester.Companion.MAX_WORKER_SIZE
import io.bluetape4k.junit5.tester.WorkerStressTester.Companion.MIN_WORKER_SIZE
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
 *    .workers(Runtimex.availableProcessors())
 *    .rounds(4)
 *    .add {
 *          println("Hello, World!")
 *     }
 *    .add {
 *          println("Hello, Kotlin!")
 *     }
 *    .run()
 * ```
 *
 * @see [StructuredTaskScopeTester]
 * @see [io.bluetape4k.junit5.coroutines.SuspendedJobTester]
 */
class MultithreadingTester: WorkerStressTester<MultithreadingTester> {

    companion object: KLogging()

    private var workers = DEFAULT_WORKER_SIZE
    private var roundsPerWorker = DEFAULT_ROUNDS_PER_WORKER
    private val runnables = CopyOnWriteArrayList<() -> Unit>()
    private val futures = CopyOnWriteArrayList<Future<*>>()
    private var executor: ExecutorService? = null

    /**
     * 테스트 시에 사용할 Thread 수를 지정합니다.
     */
    @Deprecated(
        message = "Use workers(value) for consistent naming across testers.",
        replaceWith = ReplaceWith("workers(value)")
    )
    fun numThreads(value: Int) = apply {
        require(value in MIN_WORKER_SIZE..MAX_WORKER_SIZE) {
            "Invalid threads: [$value] -- must be range in $MIN_WORKER_SIZE..$MAX_WORKER_SIZE"
        }
        workers = value
    }

    /**
     * 공통 설정명: 실행 worker(thread) 수를 지정합니다.
     */
    override fun workers(value: Int) = apply {
        require(value in MIN_WORKER_SIZE..MAX_WORKER_SIZE) {
            "Invalid workers: [$value] -- must be range in $MIN_WORKER_SIZE..$MAX_WORKER_SIZE"
        }
        workers = value
    }

    /**
     * 테스트 코드 블럭을 쓰레드 당 수행할 횟수를 지정합니다.
     */
    @Deprecated(
        message = "Use rounds(value) for consistent naming across testers.",
        replaceWith = ReplaceWith("rounds(value)")
    )
    fun roundsPerThread(value: Int) = apply {
        rounds(value)
    }

    /**
     * 공통 설정명: worker당 실행 라운드 수를 지정합니다.
     */
    override fun rounds(value: Int) = apply {
        require(value in MIN_ROUNDS_PER_WORKER..MAX_ROUNDS_PER_WORKER) {
            "Invalid rounds: [$value] -- must be range in $MIN_ROUNDS_PER_WORKER..$MAX_ROUNDS_PER_WORKER"
        }
        roundsPerWorker = value
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
     * @see workers
     * @see roundsPerWorker
     */
    fun run() {
        check(runnables.isNotEmpty()) {
            "실행할 테스트 코드가 등록되지 않았습니다. add() 메소드를 사용하여 테스트 코드를 등록해주세요."
        }

        check(workers >= runnables.size) {
            "등록된 테스트 코드 블럭의 수[${runnables.size}]보다 적은 수의 Worker[$workers]로 테스트를 실행할 수 없습니다."
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
        log.debug { "Start worker threads ... workers=$workers" }

        val factory = Thread.ofPlatform().name("multi-thread-tester-", 0).daemon(true).factory()
        executor = Executors.newFixedThreadPool(workers, factory)

        val tasks = List(workers * roundsPerWorker) {
            val runnable = runnables[it % runnables.size]
            executor!!.submit {
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
        runCatching { executor!!.shutdownNow() }
    }
}
