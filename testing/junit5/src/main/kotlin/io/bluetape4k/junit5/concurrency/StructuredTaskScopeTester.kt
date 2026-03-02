package io.bluetape4k.junit5.concurrency

import io.bluetape4k.concurrent.virtualthread.StructuredTaskScopes
import io.bluetape4k.junit5.tester.StressTester
import io.bluetape4k.junit5.tester.StressTester.Companion.DEFAULT_ROUNDS_PER_WORKER
import io.bluetape4k.junit5.tester.StressTester.Companion.MAX_ROUNDS_PER_WORKER
import io.bluetape4k.junit5.tester.StressTester.Companion.MIN_ROUNDS_PER_WORKER
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.error
import java.util.concurrent.ThreadFactory

/**
 * Java 21/25 StructuredTaskScope 기반으로 테스트 블록을 병렬 실행합니다.
 *
 * ## 동작/계약
 * - `rounds` 값은 `1..1_000_000`만 허용하며 벗어나면 [IllegalArgumentException]이 발생합니다.
 * - 실행할 블록이 없을 때 [run]을 호출하면 [IllegalStateException]이 발생합니다.
 * - 기본은 virtual thread factory를 사용하고, [withFactory]로 사용자 factory를 지정할 수 있습니다.
 * - 실행 중 예외가 발생하면 scope 종료 시 `throwIfFailed` 경로로 전파됩니다.
 *
 * ```kotlin
 * val counter = java.util.concurrent.atomic.AtomicInteger()
 * StructuredTaskScopeTester()
 *      .rounds(3)
 *      .add { counter.incrementAndGet() }
 *      .run()
 * // counter.get() == 3
 * ```
 */
class StructuredTaskScopeTester: StressTester<StructuredTaskScopeTester> {

    companion object: KLogging()

    private var roundsPerWorker: Int = DEFAULT_ROUNDS_PER_WORKER

    private val testBlocks = mutableListOf<() -> Unit>()
    private var factory: ThreadFactory? = null

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
     * 등록된 블록을 StructuredTaskScope로 실행합니다.
     *
     * ## 동작/계약
     * - 블록이 하나도 없으면 [IllegalStateException]이 발생합니다.
     * - `rounds` 횟수만큼 모든 블록을 fork하고, join 후 실패를 전파합니다.
     * - 호출이 끝나면 scope가 닫히며 스레드 자원은 정리됩니다.
     *
     * ```kotlin
     * val counter = java.util.concurrent.atomic.AtomicInteger()
     * StructuredTaskScopeTester().rounds(2).add { counter.incrementAndGet() }.run()
     * // counter.get() == 2
     * ```
     */
    fun run() {
        check(testBlocks.isNotEmpty()) {
            "No test blocks added. Please add test blocks using add() method."
        }

        val factory = this.factory ?: Thread.ofVirtual().factory()
        StructuredTaskScopes.all("stressTester", factory) { scope ->
            repeat(roundsPerWorker) {
                testBlocks.forEach { block ->
                    scope.fork { block() }

                }
            }
            scope.join().throwIfFailed {
                log.error(it) { "Test blocks failed with exception." }
            }
        }
    }
}
