package io.bluetape4k.batch.core

import io.bluetape4k.batch.api.BatchJobRepository
import io.bluetape4k.batch.api.BatchReader
import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.batch.api.BatchWriter
import io.bluetape4k.batch.api.JobExecution
import io.bluetape4k.batch.api.SkipPolicy
import io.bluetape4k.batch.api.StepExecution
import io.bluetape4k.batch.api.StepReport
import io.bluetape4k.logging.KLogging
import io.bluetape4k.workflow.api.RetryPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import io.bluetape4k.assertions.shouldBe
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Instant
import io.bluetape4k.assertions.assertFailsWith
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * [BatchStepRunner]의 commitTimeout / cancellation 처리 테스트.
 *
 * ## 검증 시나리오
 * 1. writer commitTimeout 초과 + retry=0 + `SkipPolicy.NONE` → step status `FAILED`.
 * 2. writer commitTimeout 초과 + `SkipPolicy.maxSkips(>= chunkSize)` → `COMPLETED_WITH_SKIPS` + skipCount.
 * 3. 외부 cancellation 시 [CancellationException]이 재던져진다 (절대 삼키지 않음).
 * 4. 외부 cancellation 시 repository에 STOPPED 상태가 저장된다.
 *
 * ## 핵심 동작 메모
 * - [WriteTimeoutException]은 [CancellationException] 서브타입이 **아니다** —
 *   retry/skip 경로로 흘러 들어간다.
 * - 외부 [CancellationException] (또는 [kotlinx.coroutines.TimeoutCancellationException])은
 *   runner가 STOPPED 상태를 저장한 뒤 즉시 재던진다.
 */
class BatchStepRunnerTimeoutTest {

    companion object: KLogging() {
        private const val JOB_NAME = "timeoutTestJob"
        private const val STEP_NAME = "timeoutStep"
        private const val CHUNK_SIZE = 3
        private val SHORT_COMMIT_TIMEOUT: Duration = 10.milliseconds
        private val SLOW_WRITER_DELAY: Duration = 200.milliseconds
        private val LONG_COMMIT_TIMEOUT: Duration = 10.seconds
        private val EXTERNAL_TIMEOUT: Duration = 50.milliseconds
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /** 리스트 기반 fake reader. */
    private class ListBatchReader<T: Any>(items: List<T>): BatchReader<T> {
        private val queue = ArrayDeque(items)
        override suspend fun open() {}
        override suspend fun close() {}
        override suspend fun read(): T? = queue.removeFirstOrNull()
        override suspend fun checkpoint(): Any? = null
        override suspend fun onChunkCommitted() {}
    }

    /**
     * 지정한 시간만큼 [delay] 한 뒤에 아이템을 수집하는 writer.
     *
     * commitTimeout 보다 더 오래 지연시켜 [WriteTimeoutException] 발생 또는
     * 외부 cancellation 검증에 사용한다.
     */
    private class SlowWriter<T: Any>(private val writeDelay: Duration): BatchWriter<T> {
        val collected = mutableListOf<T>()
        override suspend fun open() {}
        override suspend fun close() {}
        override suspend fun write(items: List<T>) {
            delay(writeDelay)
            collected.addAll(items)
        }
    }

    /**
     * 마지막 [completeStepExecution] 호출의 [StepReport]를 캡처하는 리포지토리 래퍼.
     *
     * `findOrCreateStepExecution`은 STOPPED → RUNNING으로 상태를 복원해버리므로,
     * runner가 저장한 최종 StepReport를 직접 검사할 수 있도록 보조한다.
     */
    private class RecordingRepository(
        private val delegate: InMemoryBatchJobRepository = InMemoryBatchJobRepository(),
    ): BatchJobRepository {

        @Volatile
        var lastReport: StepReport? = null
            private set

        @Volatile
        var lastStepExecution: StepExecution? = null
            private set

        override suspend fun findOrCreateJobExecution(
            jobName: String,
            params: Map<String, Any>,
        ): JobExecution = delegate.findOrCreateJobExecution(jobName, params)

        override suspend fun completeJobExecution(execution: JobExecution, status: BatchStatus) {
            delegate.completeJobExecution(execution, status)
        }

        override suspend fun findOrCreateStepExecution(
            jobExecution: JobExecution,
            stepName: String,
        ): StepExecution = delegate.findOrCreateStepExecution(jobExecution, stepName)

        override suspend fun completeStepExecution(execution: StepExecution, report: StepReport) {
            lastStepExecution = execution
            lastReport = report
            delegate.completeStepExecution(execution, report)
        }

        override suspend fun saveCheckpoint(stepExecutionId: Long, checkpoint: Any) {
            delegate.saveCheckpoint(stepExecutionId, checkpoint)
        }

        override suspend fun loadCheckpoint(stepExecutionId: Long): Any? =
            delegate.loadCheckpoint(stepExecutionId)
    }

    /** 기본 JobExecution 팩토리. */
    private fun makeJobExecution(): JobExecution = JobExecution(
        id = 1L,
        jobName = JOB_NAME,
        params = emptyMap(),
        status = BatchStatus.RUNNING,
        startTime = Instant.now(),
    )

    /** 동일 설정의 BatchStep 팩토리. */
    private fun <I: Any, O: Any> makeStep(
        reader: BatchReader<I>,
        writer: BatchWriter<O>,
        skipPolicy: SkipPolicy = SkipPolicy.NONE,
        retryPolicy: RetryPolicy = RetryPolicy.NONE,
        commitTimeout: Duration = SHORT_COMMIT_TIMEOUT,
        chunkSize: Int = CHUNK_SIZE,
    ): BatchStep<I, O> = BatchStep(
        name = STEP_NAME,
        chunkSize = chunkSize,
        reader = reader,
        writer = writer,
        skipPolicy = skipPolicy,
        retryPolicy = retryPolicy,
        commitTimeout = commitTimeout,
    )

    // ─── 1. writer timeout + retry=0 + SkipPolicy.NONE → FAILED ───────────────

    /**
     * `commitTimeout` 보다 오래 걸리는 writer + retry=0 + `SkipPolicy.NONE` 시나리오.
     *
     * 동작:
     * - writer가 `SLOW_WRITER_DELAY` (200ms) 동안 [delay] → `commitTimeout` 10ms 초과
     * - [writeWithTimeout]가 [WriteTimeoutException]을 throw
     * - [RetryPolicy.NONE].maxAttempts == 1 이므로 추가 시도 없이 chunk-level skip 평가
     * - [SkipPolicy.NONE]은 false 반환 → runner가 예외를 다시 throw → 외부 catch에서 FAILED
     *
     * 기대 결과:
     * - status = `FAILED`
     * - skipCount = 0
     * - error not null ([WriteTimeoutException])
     */
    @Test
    fun `writer timeout + retry=0 + SkipPolicy NONE 이면 step status FAILED`() = runTest(timeout = 30.seconds) {
        val items = (1..CHUNK_SIZE).map { "item-$it" }
        val reader = ListBatchReader(items)
        val writer = SlowWriter<String>(writeDelay = SLOW_WRITER_DELAY)
        val repo = RecordingRepository()
        val je = makeJobExecution()

        val step = makeStep(
            reader = reader,
            writer = writer,
            skipPolicy = SkipPolicy.NONE,
            retryPolicy = RetryPolicy.NONE,
            commitTimeout = SHORT_COMMIT_TIMEOUT,
        )

        val report = BatchStepRunner(step, je, repo).run()

        report.status shouldBe BatchStatus.FAILED
        report.skipCount shouldBeEqualTo 0L
        report.writeCount shouldBeEqualTo 0L
        report.error.shouldNotBeNull()
    }

    // ─── 2. writer timeout + maxSkips(>=chunkSize) → COMPLETED_WITH_SKIPS ────

    /**
     * `commitTimeout` 초과 + `SkipPolicy.maxSkips(chunkSize)` 시나리오.
     *
     * 동작:
     * - 첫 청크: writer 타임아웃 → retry 소진 → skipPolicy `shouldSkip(e, 0) = (0 < CHUNK_SIZE)` true → chunk skip
     * - skipCount += chunk.size = CHUNK_SIZE 누적
     * - reader EOF → 정상 종료
     *
     * 기대 결과:
     * - status = `COMPLETED_WITH_SKIPS`
     * - skipCount == CHUNK_SIZE
     * - writeCount == 0
     * - readCount == CHUNK_SIZE
     */
    @Test
    fun `writer timeout + SkipPolicy maxSkips chunkSize 이상이면 COMPLETED_WITH_SKIPS`() =
        runTest(timeout = 30.seconds) {
            val items = (1..CHUNK_SIZE).map { "row-$it" }
            val reader = ListBatchReader(items)
            val writer = SlowWriter<String>(writeDelay = SLOW_WRITER_DELAY)
            val repo = RecordingRepository()
            val je = makeJobExecution()

            val step = makeStep(
                reader = reader,
                writer = writer,
                skipPolicy = SkipPolicy.maxSkips(CHUNK_SIZE.toLong()),
                retryPolicy = RetryPolicy.NONE,
                commitTimeout = SHORT_COMMIT_TIMEOUT,
            )

            val report = BatchStepRunner(step, je, repo).run()

            report.status shouldBe BatchStatus.COMPLETED_WITH_SKIPS
            report.skipCount shouldBeEqualTo CHUNK_SIZE.toLong()
            report.writeCount shouldBeEqualTo 0L
            report.readCount shouldBeEqualTo CHUNK_SIZE.toLong()
        }

    // ─── 3. 외부 cancellation 시 CancellationException 재던짐 ─────────────────

    /**
     * 외부 [withTimeout] 으로 runner 코루틴을 취소하는 시나리오.
     *
     * 동작:
     * - writer가 SLOW_WRITER_DELAY (200ms) 동안 [delay] 중인데
     * - 외부 `withTimeout(EXTERNAL_TIMEOUT = 50ms)` 가 코루틴을 취소
     * - [kotlinx.coroutines.TimeoutCancellationException] (= [CancellationException] 서브타입) 발생
     * - runner는 catch (e: CancellationException) 분기로 진입 → STOPPED 저장 후 **재던짐**
     *
     * 기대 결과:
     * - 호출자에게 [CancellationException] 이 전파된다 (절대 삼키지 않음).
     */
    @Test
    fun `coroutine cancellation 시 CancellationException 재던짐`() = runTest(timeout = 30.seconds) {
        val items = listOf("a", "b", "c")
        val reader = ListBatchReader(items)
        val writer = SlowWriter<String>(writeDelay = SLOW_WRITER_DELAY)
        val repo = RecordingRepository()
        val je = makeJobExecution()

        val step = makeStep(
            reader = reader,
            writer = writer,
            skipPolicy = SkipPolicy.NONE,
            retryPolicy = RetryPolicy.NONE,
            // commitTimeout 은 외부 timeout 보다 길게 두어 내부 WriteTimeoutException 이 아닌
            // 외부 cancellation 이 우선 트리거되도록 한다.
            commitTimeout = LONG_COMMIT_TIMEOUT,
        )

        assertFailsWith<CancellationException> {
            withTimeout(EXTERNAL_TIMEOUT) {
                BatchStepRunner(step, je, repo).run()
            }
        }
    }

    // ─── 4. 외부 cancellation 시 STOPPED 상태 저장 ────────────────────────────

    /**
     * 외부 cancellation 시 runner가 STOPPED 상태로 [BatchJobRepository.completeStepExecution] 호출하는지 검증.
     *
     * 동작은 위 시나리오와 동일하나, [RecordingRepository]에 캡처된 마지막 [StepReport]를 직접 검사한다.
     *
     * 기대 결과:
     * - lastReport.status == `STOPPED`
     * - 예외는 호출자에게 전파됨
     */
    @Test
    fun `cancel 후 step status 는 STOPPED`() = runTest(timeout = 30.seconds) {
        val items = listOf("a", "b", "c")
        val reader = ListBatchReader(items)
        val writer = SlowWriter<String>(writeDelay = SLOW_WRITER_DELAY)
        val repo = RecordingRepository()
        val je = makeJobExecution()

        val step = makeStep(
            reader = reader,
            writer = writer,
            skipPolicy = SkipPolicy.NONE,
            retryPolicy = RetryPolicy.NONE,
            commitTimeout = LONG_COMMIT_TIMEOUT,
        )

        assertFailsWith<CancellationException> {
            withTimeout(EXTERNAL_TIMEOUT) {
                BatchStepRunner(step, je, repo).run()
            }
        }

        val lastReport = repo.lastReport
        lastReport.shouldNotBeNull()
        lastReport.status shouldBe BatchStatus.STOPPED
        lastReport.stepName shouldBeEqualTo STEP_NAME
    }
}
