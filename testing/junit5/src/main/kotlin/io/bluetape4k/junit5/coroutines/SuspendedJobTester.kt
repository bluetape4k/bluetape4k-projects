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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.yield

/**
 * 코루틴 블록을 다수의 Job으로 병렬 실행해 비동기 코드의 경쟁 조건을 검증합니다.
 *
 * ## 동작/계약
 * - `workers`는 `1..2000`, `rounds`는 `1..1_000_000` 범위를 벗어나면 [IllegalArgumentException]이 발생합니다.
 * - [run] 호출 시 블록이 비어 있으면 [IllegalStateException]이 발생합니다.
 * - 각 블록 예외는 [MultiException]에 모아졌다가 실행 완료 후 재던져집니다.
 * - 내부적으로 `newFixedThreadPoolContext`를 생성해 사용 후 즉시 닫습니다.
 *
 * ```kotlin
 * val counter = java.util.concurrent.atomic.AtomicInteger()
 * SuspendedJobTester()
 *      .workers(2)
 *      .rounds(3)
 *      .add { counter.incrementAndGet() }
 *      .run()
 * // counter.get() == 6
 * ```
 */
class SuspendedJobTester: WorkerStressTester<SuspendedJobTester> {

    companion object: KLoggingChannel()

    private var numWorkers = DEFAULT_WORKER_SIZE
    private var roundPerWorker = DEFAULT_ROUNDS_PER_WORKER
    private val suspendBlocks = mutableListOf<suspend () -> Unit>()

    /**
     * [workers]의 구버전 이름으로 worker 수를 설정합니다.
     *
     * ## 동작/계약
     * - 값이 `1..2000` 범위를 벗어나면 [IllegalArgumentException]이 발생합니다.
     * - 설정만 바꾸고 즉시 실행하지 않습니다.
     *
     * ```kotlin
     * val tester = SuspendedJobTester().numThreads(4)
     * // tester.workers(4)와 동일
     * ```
     */
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
     * 실행 worker 수를 설정합니다.
     *
     * ## 동작/계약
     * - 값이 `1..2000` 범위를 벗어나면 [IllegalArgumentException]이 발생합니다.
     * - 내부 상태만 변경하며 실행은 [run]에서 시작됩니다.
     *
     * ```kotlin
     * val tester = SuspendedJobTester().workers(8)
     * // run() 시 최대 8 worker context 사용
     * ```
     */
    override fun workers(value: Int) = apply {
        applyNumWorks(value)
    }

    /**
     * [rounds]의 구버전 이름으로 worker당 반복 횟수를 설정합니다.
     *
     * ## 동작/계약
     * - 내부적으로 [rounds]와 동일한 검증 로직을 적용합니다.
     * - 설정만 변경하고 실행은 하지 않습니다.
     *
     * ```kotlin
     * val tester = SuspendedJobTester().roundsPerJob(2)
     * // tester.rounds(2)와 동일
     * ```
     */
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
     * worker당 실행 라운드 수를 설정합니다.
     *
     * ## 동작/계약
     * - 값이 `1..1_000_000` 범위를 벗어나면 [IllegalArgumentException]이 발생합니다.
     * - 실행량은 `workers * rounds * 등록 블록 수`에 비례합니다.
     *
     * ```kotlin
     * val tester = SuspendedJobTester().rounds(5)
     * // run() 시 각 worker 라운드 5회 반복
     * ```
     */
    override fun rounds(value: Int) = apply {
        applyRoundsPerJob(value)
    }

    /**
     * 실행할 suspend 블록을 하나 추가합니다.
     *
     * ## 동작/계약
     * - 전달한 블록 참조를 내부 목록에 append합니다.
     * - 블록은 [run] 호출 전까지 실행되지 않습니다.
     *
     * ```kotlin
     * val tester = SuspendedJobTester().add { kotlinx.coroutines.delay(1) }
     * // 등록 블록 수 +1
     * ```
     */
    fun add(testBlock: suspend () -> Unit) = apply {
        suspendBlocks.add(testBlock)
    }

    /**
     * suspend 블록을 가변 인자로 한 번에 추가합니다.
     *
     * ## 동작/계약
     * - 입력 순서를 유지해 내부 목록에 추가합니다.
     * - 빈 입력이면 아무 작업 없이 반환합니다.
     *
     * ```kotlin
     * SuspendedJobTester().addAll({ }, { })
     * // 등록 블록 수 +2
     * ```
     */
    fun addAll(vararg testBlocks: suspend () -> Unit) = apply {
        suspendBlocks.addAll(testBlocks)
    }

    /**
     * 컬렉션으로 전달한 suspend 블록을 추가합니다.
     *
     * ## 동작/계약
     * - 컬렉션 내용을 복사해 내부 목록에 append합니다.
     * - 입력 컬렉션 객체 자체는 변경하지 않습니다.
     *
     * ```kotlin
     * val blocks = listOf<suspend () -> Unit>({ }, { })
     * SuspendedJobTester().addAll(blocks)
     * // blocks는 그대로 유지
     * ```
     */
    fun addAll(testBlocks: Collection<suspend () -> Unit>) = apply {
        suspendBlocks.addAll(testBlocks)
    }

    /**
     * 등록된 suspend 블록을 병렬 실행합니다.
     *
     * ## 동작/계약
     * - 블록 미등록 상태면 [IllegalStateException]이 발생합니다.
     * - 고정 스레드 dispatcher 위에서 모든 Job을 생성하고 `joinAll`로 완료까지 대기합니다.
     * - 수집된 예외가 있으면 [MultiException.throwIfNotEmpty]가 예외를 던집니다.
     *
     * ```kotlin
     * val counter = java.util.concurrent.atomic.AtomicInteger()
     * SuspendedJobTester().workers(2).rounds(2).add { counter.incrementAndGet() }.run()
     * // counter.get() == 4
     * ```
     */
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
                    } catch (e: CancellationException) {
                        // CancellationException은 코루틴 구조화된 동시성의 정상 취소 신호이므로 재전파합니다.
                        throw e
                    } catch (e: Throwable) {
                        me.add(e)
                    }
                }
            }
        }.flatten()
    }
}
