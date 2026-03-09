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
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 등록한 블록을 고정 스레드 풀에서 반복 실행해 멀티스레드 안정성을 검증합니다.
 *
 * ## 동작/계약
 * - `workers`는 `1..2000`, `rounds`는 `1..1_000_000` 범위를 벗어나면 [IllegalArgumentException]이 발생합니다.
 * - `run` 호출 시 블록이 하나도 없거나 worker 수가 블록 수보다 작으면 [IllegalStateException]이 발생합니다.
 * - 테스트 블록 내부 예외는 [MultiException]에 수집되며 실행 종료 시 재던집니다.
 * - 수신 객체 설정(`workers`, `rounds`, `add*`)은 내부 상태를 변경하며, 실행 시 worker 고정 개수로
 *   `workers * rounds` 실행 단위를 순차 할당합니다(대량 라운드에서도 상수 크기 worker 유지).
 *
 * ```kotlin
 * val counter = java.util.concurrent.atomic.AtomicInteger()
 * MultithreadingTester()
 *      .workers(2)
 *      .rounds(3)
 *      .add { counter.incrementAndGet() }
 *      .run()
 * // counter.get() == 6
 * ```
 */
class MultithreadingTester: WorkerStressTester<MultithreadingTester> {

    companion object: KLogging()

    private var workers = DEFAULT_WORKER_SIZE
    private var roundsPerWorker = DEFAULT_ROUNDS_PER_WORKER
    private val runnables = CopyOnWriteArrayList<() -> Unit>()
    private val futures = mutableListOf<Future<*>>()
    private var executor: ExecutorService? = null

    /**
     * `workers`의 구버전 이름으로 worker(thread) 수를 설정합니다.
     *
     * ## 동작/계약
     * - 값이 `1..2000` 범위를 벗어나면 [IllegalArgumentException]이 발생합니다.
     * - 내부 `workers` 값을 변경하고 자기 자신을 반환합니다.
     *
     * ```kotlin
     * val tester = MultithreadingTester().numThreads(4)
     * // tester.workers(4)와 동일한 설정 효과
     * ```
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
     * 실행 worker(thread) 수를 설정합니다.
     *
     * ## 동작/계약
     * - 값이 `1..2000` 범위를 벗어나면 [IllegalArgumentException]이 발생합니다.
     * - 내부 설정만 변경하며 즉시 실행하지 않습니다.
     *
     * ```kotlin
     * val tester = MultithreadingTester().workers(8)
     * // 이후 run()에서 최대 8개 worker 사용
     * ```
     */
    override fun workers(value: Int) = apply {
        require(value in MIN_WORKER_SIZE..MAX_WORKER_SIZE) {
            "Invalid workers: [$value] -- must be range in $MIN_WORKER_SIZE..$MAX_WORKER_SIZE"
        }
        workers = value
    }

    /**
     * `rounds`의 구버전 이름으로 worker당 반복 횟수를 설정합니다.
     *
     * ## 동작/계약
     * - 내부적으로 [rounds]를 호출하므로 동일한 검증/예외 규칙을 따릅니다.
     * - 설정만 변경하고 테스트 실행은 하지 않습니다.
     *
     * ```kotlin
     * val tester = MultithreadingTester().roundsPerThread(5)
     * // tester.rounds(5)와 동일
     * ```
     */
    @Deprecated(
        message = "Use rounds(value) for consistent naming across testers.",
        replaceWith = ReplaceWith("rounds(value)")
    )
    fun roundsPerThread(value: Int) = apply {
        rounds(value)
    }

    /**
     * worker당 실행 라운드 수를 설정합니다.
     *
     * ## 동작/계약
     * - 값이 `1..1_000_000` 범위를 벗어나면 [IllegalArgumentException]이 발생합니다.
     * - 내부 설정만 변경하며 실행은 [run] 호출 시 시작됩니다.
     *
     * ```kotlin
     * val tester = MultithreadingTester().rounds(3)
     * // 각 worker가 등록 블록을 3회씩 실행
     * ```
     */
    override fun rounds(value: Int) = apply {
        require(value in MIN_ROUNDS_PER_WORKER..MAX_ROUNDS_PER_WORKER) {
            "Invalid rounds: [$value] -- must be range in $MIN_ROUNDS_PER_WORKER..$MAX_ROUNDS_PER_WORKER"
        }
        roundsPerWorker = value
    }

    /**
     * 실행할 테스트 블록 하나를 추가합니다.
     *
     * ## 동작/계약
     * - 전달한 블록 참조를 내부 목록에 그대로 보관합니다.
     * - 호출할 때마다 목록 크기가 1 증가하며 기존 항목은 유지됩니다.
     *
     * ```kotlin
     * val tester = MultithreadingTester().add { /* noop */ }
     * // run() 대상 블록 1개 등록
     * ```
     */
    fun add(testBlock: () -> Unit) = apply {
        runnables.add(testBlock)
    }

    /**
     * [Runnable] 기반 테스트 블록을 추가합니다.
     *
     * ## 동작/계약
     * - 전달한 [Runnable]은 람다로 감싸 내부 목록에 추가됩니다.
     * - 변환 과정에서 추가 예외를 만들지 않고, 실제 예외는 실행 시점에 수집됩니다.
     *
     * ```kotlin
     * val task = Runnable { Thread.sleep(1) }
     * MultithreadingTester().add(task)
     * // run() 시 task.run() 실행
     * ```
     */
    fun add(testBlock: Runnable) = apply {
        runnables.add({ testBlock.run() })
    }

    /**
     * 테스트 블록을 가변 인자로 한 번에 추가합니다.
     *
     * ## 동작/계약
     * - 인자 순서대로 내부 목록 뒤에 append합니다.
     * - 빈 인자를 전달해도 예외 없이 아무 변경 없이 반환합니다.
     *
     * ```kotlin
     * MultithreadingTester().addAll({ }, { })
     * // 등록된 블록 수 +2
     * ```
     */
    fun addAll(vararg testBlocks: () -> Unit) = apply {
        runnables.addAll(testBlocks)
    }

    /**
     * 컬렉션으로 전달한 테스트 블록을 한 번에 추가합니다.
     *
     * ## 동작/계약
     * - 전달한 컬렉션의 반복 순서대로 내부 목록에 추가합니다.
     * - 컬렉션 자체는 변경하지 않습니다.
     *
     * ```kotlin
     * val blocks = listOf<() -> Unit>({ }, { })
     * MultithreadingTester().addAll(blocks)
     * // blocks는 그대로 유지
     * ```
     */
    fun addAll(testBlocks: Collection<() -> Unit>) = apply {
        runnables.addAll(testBlocks)
    }

    /**
     * 등록된 테스트 블록을 멀티스레드로 실행합니다.
     *
     * ## 동작/계약
     * - 블록 미등록 또는 `workers < 블록 수` 조건이면 [IllegalStateException]이 발생합니다.
     * - 내부적으로 고정 스레드 풀을 만들고 모든 Future를 대기한 뒤 풀을 종료합니다.
     * - 실행 중 수집된 예외가 있으면 마지막에 [MultiException.throwIfNotEmpty]로 재던집니다.
     *
     * ```kotlin
     * val counter = java.util.concurrent.atomic.AtomicInteger()
     * MultithreadingTester().workers(2).rounds(2).add { counter.incrementAndGet() }.run()
     * // counter.get() == 4
     * ```
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
            joinWorkerThreads()
        } finally {
            shutdownExecutor()
            me.throwIfNotEmpty()
        }
    }

    private fun startWorkerThreads(me: MultiException) {
        log.debug { "Start worker threads ... workers=$workers" }

        val factory = Thread.ofPlatform().name("multi-thread-tester-", 0).daemon(true).factory()
        val pool = Executors.newFixedThreadPool(workers, factory)
        executor = pool

        val totalRuns = workers * roundsPerWorker
        val index = AtomicInteger(0)
        val tasks = List(workers) {
            pool.submit {
                while (true) {
                    val taskIndex = index.getAndIncrement()
                    if (taskIndex >= totalRuns) {
                        break
                    }

                    val runnable = runnables[taskIndex % runnables.size]
                    try {
                        runnable.invoke()
                    } catch (t: Throwable) {
                        me.add(t)
                    }
                }
            }
        }
        futures.clear()
        futures.addAll(tasks)
    }

    private fun joinWorkerThreads() {
        log.debug { "Join worker threads ..." }

        futures.forEach { future ->
            try {
                future.get()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                log.error(e) { "Interrupted while joining worker threads" }
                throw RuntimeException("Interrupted while waiting worker completion.", e)
            } catch (e: ExecutionException) {
                log.error(e) { "Error occurred while joining worker threads" }
                throw RuntimeException("Get exception.", e.cause ?: e)
            }
        }
    }

    private fun shutdownExecutor() {
        executor?.let { pool ->
            pool.shutdown()
            try {
                if (!pool.awaitTermination(3, TimeUnit.SECONDS)) {
                    pool.shutdownNow()
                }
            } catch (e: InterruptedException) {
                pool.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
    }
}
