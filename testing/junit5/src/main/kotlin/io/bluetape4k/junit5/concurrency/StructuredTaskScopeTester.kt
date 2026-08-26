package io.bluetape4k.junit5.concurrency

import io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopes
import io.bluetape4k.junit5.tester.WorkerStressTester
import io.bluetape4k.junit5.tester.StressTester.Companion.DEFAULT_ROUNDS_PER_WORKER
import io.bluetape4k.junit5.tester.StressTester.Companion.MAX_ROUNDS_PER_WORKER
import io.bluetape4k.junit5.tester.StressTester.Companion.MIN_ROUNDS_PER_WORKER
import io.bluetape4k.junit5.tester.WorkerStressTester.Companion.MAX_WORKER_SIZE
import io.bluetape4k.junit5.tester.WorkerStressTester.Companion.MIN_WORKER_SIZE
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import java.time.Instant
import java.util.concurrent.Semaphore
import java.util.concurrent.ThreadFactory
import kotlin.time.Duration

/**
 * Java 21/25 StructuredTaskScope 기반으로 테스트 블록을 병렬 실행합니다.
 *
 * ## Behavior / Contract
 * - `rounds` accepts `1..1_000_000`; values outside that range throw [IllegalArgumentException].
 * - Calling [run] with no registered blocks throws [IllegalStateException].
 * - Uses a virtual thread factory by default; override with [withFactory].
 * - [workers] limits the number of concurrently running test blocks via an internal [Semaphore].
 *   The default is `availableProcessors * 2`.
 * - Exceptions thrown by test blocks are propagated by `throwIfFailed` after the scope joins.
 *
 * ```kotlin
 * val counter = java.util.concurrent.atomic.AtomicInteger()
 * StructuredTaskScopeTester()
 *      .workers(4)
 *      .rounds(3)
 *      .add { counter.incrementAndGet() }
 *      .run()
 * // counter.get() == 3
 * ```
 */
class StructuredTaskScopeTester: WorkerStressTester<StructuredTaskScopeTester> {

    companion object: KLogging() {
        /** Default worker count: twice the number of available processors. */
        val DEFAULT_WORKER_COUNT: Int = Runtime.getRuntime().availableProcessors() * 2
    }

    private var roundsPerWorker: Int = DEFAULT_ROUNDS_PER_WORKER
    private var workerSize: Int = DEFAULT_WORKER_COUNT

    private val testBlocks = mutableListOf<() -> Unit>()
    private var factory: ThreadFactory? = null
    private var timeout: Duration? = null

    /**
     * 실행에 사용할 [ThreadFactory]를 지정합니다.
     *
     * ## 동작/계약
     * - 전달한 factory를 내부 상태에 저장하고 다음 [run] 호출부터 사용합니다.
     * - 기존 설정을 덮어쓰며 즉시 스레드를 생성하지는 않습니다.
     *
     * ```kotlin
     * val factory = Thread.ofVirtual().name("test-vt-", 0).factory()
     * val tester = StructuredTaskScopeTester().withFactory(factory)
     * // run() 시 factory 사용
     * ```
     */
    fun withFactory(factory: ThreadFactory) = apply {
        this.factory = factory
    }

    /**
     * 전체 실행에 적용할 타임아웃을 설정합니다.
     *
     * ## 동작/계약
     * - 설정 시 [run]에서 [io.bluetape4k.concurrent.virtualthread.api.StructuredTaskScopeAll.joinUntil]을 사용합니다.
     * - 타임아웃 초과 시 [java.util.concurrent.TimeoutException]이 발생합니다.
     *
     * ```kotlin
     * StructuredTaskScopeTester()
     *     .rounds(100)
     *     .withTimeout(5.seconds)
     *     .add { heavyWork() }
     *     .run()
     * ```
     */
    fun withTimeout(duration: Duration) = apply {
        this.timeout = duration
    }

    /**
     * [rounds]의 구버전 이름으로 반복 횟수를 설정합니다.
     *
     * ## 동작/계약
     * - 내부적으로 [rounds]를 호출하므로 동일한 범위 검증을 적용합니다.
     * - 설정만 변경하고 실행은 하지 않습니다.
     *
     * ```kotlin
     * val tester = StructuredTaskScopeTester().roundsPerTask(2)
     * // tester.rounds(2)와 동일
     * ```
     */
    @Deprecated(
        message = "Use rounds(value) for consistent naming across testers.",
        replaceWith = ReplaceWith("rounds(value)")
    )
    fun roundsPerTask(value: Int) = apply {
        rounds(value)
    }

    /**
     * task 실행 라운드 수를 설정합니다.
     *
     * ## 동작/계약
     * - 값이 `1..1_000_000` 범위를 벗어나면 [IllegalArgumentException]이 발생합니다.
     * - 설정만 갱신하며 실제 task 생성은 [run]에서 수행합니다.
     *
     * ```kotlin
     * val tester = StructuredTaskScopeTester().rounds(4)
     * // run() 시 4 라운드 반복
     * ```
     */
    override fun rounds(value: Int) = apply {
        require(value in MIN_ROUNDS_PER_WORKER..MAX_ROUNDS_PER_WORKER) {
            "Invalid roundsPerTask: [$value] -- must be range in $MIN_ROUNDS_PER_WORKER..$MAX_ROUNDS_PER_WORKER"
        }
        roundsPerWorker = value
    }

    /**
     * Sets the maximum number of concurrently running test blocks.
     *
     * ## Behavior / Contract
     * - Accepts values in `1..2000`; values outside that range throw [IllegalArgumentException].
     * - An internal [Semaphore] created at [run] time limits live executions to this count.
     * - Defaults to `availableProcessors * 2` when not called.
     *
     * ```kotlin
     * StructuredTaskScopeTester()
     *     .workers(4)
     *     .rounds(100)
     *     .add { heavyWork() }
     *     .run()
     * // at most 4 blocks execute concurrently
     * ```
     */
    override fun workers(value: Int) = apply {
        require(value in MIN_WORKER_SIZE..MAX_WORKER_SIZE) {
            "Invalid workers: [$value] -- must be range in $MIN_WORKER_SIZE..$MAX_WORKER_SIZE"
        }
        workerSize = value
    }

    /**
     * 실행할 테스트 블록을 하나 추가합니다.
     *
     * ## 동작/계약
     * - 블록 참조를 내부 목록에 append합니다.
     * - 호출 순서가 실행 순서 기준 목록 순서로 유지됩니다.
     *
     * ```kotlin
     * val tester = StructuredTaskScopeTester().add { /* task */ }
     * // 등록된 블록 수 +1
     * ```
     */
    fun add(testBlock: () -> Unit) = apply {
        testBlocks.add(testBlock)
    }

    /**
     * 테스트 블록 여러 개를 한 번에 추가합니다.
     *
     * ## 동작/계약
     * - 가변 인자 순서대로 내부 목록에 추가합니다.
     * - 빈 입력이면 변경 없이 반환합니다.
     *
     * ```kotlin
     * StructuredTaskScopeTester().addAll({ }, { })
     * // 등록된 블록 수 +2
     * ```
     */
    fun addAll(vararg testBlocks: () -> Unit) = apply {
        this.testBlocks.addAll(testBlocks)
    }

    /**
     * Runs all registered blocks using a StructuredTaskScope.
     *
     * ## Behavior / Contract
     * - Throws [IllegalStateException] when no blocks are registered.
     * - Forks every block for each round; at most [workers] blocks run concurrently
     *   (controlled by an internal [Semaphore]).
     * - Propagates the first failure via `throwIfFailed` after the scope joins.
     * - The scope is closed and all thread resources are released when this method returns.
     *
     * ```kotlin
     * val counter = java.util.concurrent.atomic.AtomicInteger()
     * StructuredTaskScopeTester().workers(4).rounds(2).add { counter.incrementAndGet() }.run()
     * // counter.get() == 2
     * ```
     */
    fun run() {
        check(testBlocks.isNotEmpty()) {
            "No test blocks added. Please add test blocks using add() method."
        }

        val factory = this.factory ?: Thread.ofVirtual().factory()
        // Compute deadline before the fork loop so that fork overhead does not eat into the timeout.
        val deadline = timeout?.let { Instant.now().plusMillis(it.inWholeMilliseconds) }
        val semaphore = Semaphore(workerSize)
        StructuredTaskScopes.failFast("stressTester", factory) { scope ->
            repeat(roundsPerWorker) {
                testBlocks.forEach { block ->
                    scope.fork {
                        semaphore.acquire()
                        try {
                            block()
                        } finally {
                            semaphore.release()
                        }
                    }
                }
            }
            val joined = deadline?.let { scope.joinUntil(it) } ?: scope.join()
            joined.throwIfFailed {
                log.error(it) { "Test blocks failed with exception." }
            }
        }
    }
}
