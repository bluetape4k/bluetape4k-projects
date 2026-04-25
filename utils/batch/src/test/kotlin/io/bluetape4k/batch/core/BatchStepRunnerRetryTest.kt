package io.bluetape4k.batch.core

import io.bluetape4k.batch.api.BatchReader
import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.batch.api.BatchWriter
import io.bluetape4k.batch.api.JobExecution
import io.bluetape4k.batch.api.SkipPolicy
import io.bluetape4k.batch.api.StepReport
import io.bluetape4k.logging.KLogging
import io.bluetape4k.workflow.api.RetryPolicy
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterOrEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * [BatchStepRunner] 의 retry 정책(`RetryPolicy`) 동작에 대한 테스트.
 *
 * 다음 4 가지 시나리오를 검증한다.
 *
 * 1. retry 가 `maxAttempts` 도달까지 정확히 시도하고 더 이상 시도하지 않는다.
 * 2. exponential backoff(`backoffMultiplier`) 가 누적 delay 에 반영된다.
 * 3. retry 소진 후 (skip 불허) 최종 step status 는 [BatchStatus.FAILED] 이다.
 * 4. retry 도중 N 번째 시도에서 성공하면 `writeCount` 는 정상 아이템 수와 일치한다.
 */
class BatchStepRunnerRetryTest {

    companion object: KLogging()

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

    /** 항상 실패하는 writer. 호출 횟수를 추적한다. */
    private class AlwaysFailWriter<T: Any>: BatchWriter<T> {
        val callCount = AtomicInteger(0)
        override suspend fun write(items: List<T>) {
            callCount.incrementAndGet()
            throw RuntimeException("항상 실패")
        }
    }

    /**
     * 처음 [failCount] 번은 실패하고 그 이후 성공하는 writer.
     * 호출 횟수와 수집된 아이템을 추적한다.
     */
    private class FailThenSucceedWriter<T: Any>(private val failCount: Int): BatchWriter<T> {
        val collected = mutableListOf<T>()
        val callCount = AtomicInteger(0)
        override suspend fun write(items: List<T>) {
            val attempt = callCount.incrementAndGet()
            if (attempt <= failCount) {
                throw RuntimeException("실패 attempt=$attempt")
            }
            collected.addAll(items)
        }
    }

    private fun makeJobExecution(): JobExecution = JobExecution(
        id = 1L,
        jobName = "testJob",
        params = emptyMap(),
        status = BatchStatus.RUNNING,
        startTime = Instant.now(),
    )

    private suspend fun <I: Any, O: Any> runStep(step: BatchStep<I, O>): StepReport {
        val repo = InMemoryBatchJobRepository()
        val je = makeJobExecution()
        return BatchStepRunner(step, je, repo).run()
    }

    // ─── 1. retry 가 maxAttempts 도달까지 시도한다 ────────────────────────────

    /**
     * [RetryPolicy.maxAttempts] = 3 일 때, writer 실패가 지속되면
     * `write()` 가 정확히 3 번 호출되고 그 이상 시도되지 않는다.
     *
     * 검증 대상: [BatchStepRunner.run] 의 writer retry 루프
     */
    @Test
    fun `retry policy 가 maxAttempts 도달까지 재시도한다`() = runTest(timeout = 30.seconds) {
        val reader = ListBatchReader(listOf("a", "b"))
        val writer = AlwaysFailWriter<String>()
        val step = BatchStep<String, String>(
            name = "retryMaxAttempts",
            chunkSize = 2,
            reader = reader,
            writer = writer,
            retryPolicy = RetryPolicy(maxAttempts = 3, delay = 10.milliseconds),
            skipPolicy = SkipPolicy.NONE,
            commitTimeout = 5.seconds,
        )

        val report = runStep(step)

        // writer.write() 가 정확히 maxAttempts(=3) 번 호출되어야 한다.
        writer.callCount.get() shouldBeEqualTo 3
        report.status shouldBe BatchStatus.FAILED
    }

    // ─── 2. exponential backoff 가 적용된다 ───────────────────────────────────

    /**
     * delay = 10ms, backoffMultiplier = 2.0, maxAttempts = 3 일 때,
     * 두 번의 retry 사이의 누적 대기 시간은 최소 (10ms + 20ms) = 30ms 이상이어야 한다.
     *
     * 가상 시간 스케줄러의 [kotlinx.coroutines.test.TestCoroutineScheduler.currentTime] 으로
     * 누적 가상 시간(ms)을 검증한다.
     *
     * 검증 대상: [BatchStepRunner.run] 의 backoff 누적 로직
     */
    @Test
    fun `retry policy 의 exponential backoff 가 적용된다`() = runTest(timeout = 30.seconds) {
        val reader = ListBatchReader(listOf("a"))
        val writer = AlwaysFailWriter<String>()
        val step = BatchStep<String, String>(
            name = "retryBackoff",
            chunkSize = 1,
            reader = reader,
            writer = writer,
            retryPolicy = RetryPolicy(
                maxAttempts = 3,
                delay = 10.milliseconds,
                backoffMultiplier = 2.0,
            ),
            skipPolicy = SkipPolicy.NONE,
            commitTimeout = 5.seconds,
        )

        val startTime = testScheduler.currentTime
        runStep(step)
        val elapsed = testScheduler.currentTime - startTime

        // 첫 번째 실패 후 10ms, 두 번째 실패 후 20ms 대기 → 합 30ms 이상.
        elapsed shouldBeGreaterOrEqualTo 30L
    }

    // ─── 3. retry 후 최종 실패 시 step status 는 FAILED ──────────────────────

    /**
     * retry 가 모두 소진되고 [SkipPolicy.NONE] 인 경우 step 의 최종 상태는 [BatchStatus.FAILED] 이다.
     *
     * 검증 대상: [BatchStepRunner.run] 의 retry 소진 후 skip 평가 분기
     */
    @Test
    fun `retry 후 최종 실패 시 step status 는 FAILED`() = runTest(timeout = 30.seconds) {
        val reader = ListBatchReader(listOf("a", "b"))
        val writer = AlwaysFailWriter<String>()
        val step = BatchStep<String, String>(
            name = "retryFailed",
            chunkSize = 2,
            reader = reader,
            writer = writer,
            retryPolicy = RetryPolicy(maxAttempts = 3, delay = 10.milliseconds),
            skipPolicy = SkipPolicy.NONE,
            commitTimeout = 5.seconds,
        )

        val report = runStep(step)

        report.status shouldBe BatchStatus.FAILED
        report.error.shouldNotBeNull()
        report.writeCount shouldBeEqualTo 0L
    }

    // ─── 4. retry N 번째 시도에서 성공 시 writeCount 정상 ───────────────────

    /**
     * 처음 2 번은 실패하지만 3 번째 시도에서 성공하는 writer 의 경우,
     * [RetryPolicy.maxAttempts] = 3 안에서 성공하므로 step 은 [BatchStatus.COMPLETED] 가 되고
     * `writeCount` 는 입력 아이템 수와 같다.
     *
     * 검증 대상: [BatchStepRunner.run] 의 retry 성공 시 break 분기
     */
    @Test
    fun `retry 성공 (N번째 시도) 시 writeCount 는 정상`() = runTest(timeout = 30.seconds) {
        val items = listOf("a", "b", "c")
        val reader = ListBatchReader(items)
        val writer = FailThenSucceedWriter<String>(failCount = 2)
        val step = BatchStep<String, String>(
            name = "retrySuccess",
            chunkSize = 3,
            reader = reader,
            writer = writer,
            retryPolicy = RetryPolicy(maxAttempts = 3, delay = 10.milliseconds),
            skipPolicy = SkipPolicy.NONE,
            commitTimeout = 5.seconds,
        )

        val report = runStep(step)

        report.status shouldBe BatchStatus.COMPLETED
        report.writeCount shouldBeEqualTo items.size.toLong()
        writer.callCount.get() shouldBeEqualTo 3 // 2 failures + 1 success
        writer.collected shouldBeEqualTo items
    }
}
