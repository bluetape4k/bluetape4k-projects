package io.bluetape4k.batch.core

import io.bluetape4k.batch.api.BatchProcessor
import io.bluetape4k.batch.api.BatchReader
import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.batch.api.BatchWriter
import io.bluetape4k.batch.api.JobExecution
import io.bluetape4k.batch.api.SkipPolicy
import io.bluetape4k.batch.api.StepReport
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.workflow.api.RetryPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * [BatchStepRunner]의 chunk 루프 엣지 케이스 테스트.
 *
 * 11가지 시나리오:
 * 1. 정상 경로: read → process → write → COMPLETED
 * 2. 빈 리더 → COMPLETED (0 items)
 * 3. Processor null 필터 → skipCount 증가 없이 항목 제외
 * 4. Processor 예외 + skipPolicy.ALL → COMPLETED_WITH_SKIPS
 * 5. Processor 예외 + skipPolicy.NONE → FAILED
 * 6. Writer 재시도 성공: 1회 실패 후 성공 → COMPLETED
 * 7. Writer retry 소진 + skip 허용 → COMPLETED_WITH_SKIPS
 * 8. Writer retry 소진 + skip 불허 → FAILED
 * 9. CancellationException → STOPPED 저장 후 재던짐
 * 10. 이미 COMPLETED StepExecution → reader/writer open 없이 즉시 skip 반환
 * 11. 이미 COMPLETED_WITH_SKIPS → 즉시 skip 반환
 */
class BatchStepRunnerTest {

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /** 리스트 기반 fake reader. 아이템을 순서대로 반환하고 EOF에서 null 반환. */
    class ListBatchReader<T : Any>(items: List<T>) : BatchReader<T> {
        private val queue = ArrayDeque(items)
        private val openCount = AtomicInteger(0)
        private val closeCount = AtomicInteger(0)
        var lastCheckpoint: Long = -1L
            private set

        val wasOpened: Boolean get() = openCount.get() > 0
        val wasClosed: Boolean get() = closeCount.get() > 0

        override suspend fun open() { openCount.incrementAndGet() }
        override suspend fun close() { closeCount.incrementAndGet() }
        override suspend fun read(): T? = queue.removeFirstOrNull()
        override suspend fun checkpoint(): Any? = if (lastCheckpoint >= 0) lastCheckpoint else null
        override suspend fun onChunkCommitted() { lastCheckpoint++ }
    }

    /** 아이템 수집 fake writer. */
    class CollectingWriter<T : Any> : BatchWriter<T> {
        val collected = mutableListOf<T>()
        val openCount = AtomicInteger(0)
        val closeCount = AtomicInteger(0)
        val wasOpened: Boolean get() = openCount.get() > 0
        val wasClosed: Boolean get() = closeCount.get() > 0

        override suspend fun open() { openCount.incrementAndGet() }
        override suspend fun close() { closeCount.incrementAndGet() }
        override suspend fun write(items: List<T>) { collected.addAll(items) }
    }

    /** n번 실패 후 성공하는 writer. */
    class FailThenSucceedWriter<T : Any>(private val failCount: Int) : BatchWriter<T> {
        val collected = mutableListOf<T>()
        private val attempts = AtomicInteger(0)

        override suspend fun write(items: List<T>) {
            if (attempts.incrementAndGet() <= failCount) {
                throw RuntimeException("write 실패 (attempt=${attempts.get()})")
            }
            collected.addAll(items)
        }
    }

    /** 항상 실패하는 writer. */
    class AlwaysFailWriter<T : Any> : BatchWriter<T> {
        override suspend fun write(items: List<T>) {
            throw RuntimeException("항상 실패")
        }
    }

    /** 지정된 지연 후 write를 완료하는 writer (타임아웃 테스트용). */
    class SlowWriter<T : Any>(private val writeDelay: Duration) : BatchWriter<T> {
        val collected = mutableListOf<T>()
        override suspend fun write(items: List<T>) {
            delay(writeDelay)
            collected.addAll(items)
        }
    }

    /** open() 시 예외를 던지는 reader (초기화 실패 테스트용). */
    class FailOnOpenReader<T : Any> : BatchReader<T> {
        override suspend fun open() { throw RuntimeException("reader.open() 실패") }
        override suspend fun read(): T? = null
    }

    /** close() 시 예외를 던지는 writer (close 예외 테스트용). */
    class FailOnCloseWriter<T : Any> : BatchWriter<T> {
        val collected = mutableListOf<T>()
        override suspend fun write(items: List<T>) { collected.addAll(items) }
        override suspend fun close() { throw RuntimeException("writer.close() 실패") }
    }

    /** 기본 JobExecution 팩토리. */
    private fun makeJobExecution(): JobExecution = JobExecution(
        id = 1L,
        jobName = "testJob",
        params = emptyMap(),
        status = BatchStatus.RUNNING,
        startTime = Instant.now(),
    )

    /** 기본 BatchStep 팩토리 (I=String, O=String). */
    private fun <I : Any, O : Any> makeStep(
        name: String = "step1",
        chunkSize: Int = 3,
        reader: BatchReader<I>,
        writer: BatchWriter<O>,
        processor: BatchProcessor<I, O>? = null,
        skipPolicy: SkipPolicy = SkipPolicy.NONE,
        retryPolicy: RetryPolicy = RetryPolicy.NONE,
    ): BatchStep<I, O> = BatchStep(
        name = name,
        chunkSize = chunkSize,
        reader = reader,
        writer = writer,
        processor = processor,
        skipPolicy = skipPolicy,
        retryPolicy = retryPolicy,
        commitTimeout = 5.seconds,
    )

    // ─── 1. 정상 경로 ─────────────────────────────────────────────────────────

    @Test
    fun `1 정상 경로 - read, process, write 후 COMPLETED`() = runSuspendIO {
        val reader = ListBatchReader(listOf("a", "b", "c", "d", "e"))
        val writer = CollectingWriter<String>()
        val repo = InMemoryBatchJobRepository()
        val step = makeStep<String, String>(chunkSize = 2, reader = reader, writer = writer)
        val runner = BatchStepRunner(step, makeJobExecution(), repo)

        val report = runner.run()

        report.status shouldBe BatchStatus.COMPLETED
        report.readCount shouldBeEqualTo 5L
        report.writeCount shouldBeEqualTo 5L
        report.skipCount shouldBeEqualTo 0L
        writer.collected shouldBeEqualTo listOf("a", "b", "c", "d", "e")
        reader.wasOpened shouldBe true
        reader.wasClosed shouldBe true
        writer.wasClosed shouldBe true
    }

    // ─── 2. 빈 리더 ──────────────────────────────────────────────────────────

    @Test
    fun `2 빈 리더 - EOF 즉시 반환 COMPLETED, 0 items`() = runSuspendIO {
        val reader = ListBatchReader<String>(emptyList())
        val writer = CollectingWriter<String>()
        val repo = InMemoryBatchJobRepository()
        val step = makeStep<String, String>(reader = reader, writer = writer)
        val runner = BatchStepRunner(step, makeJobExecution(), repo)

        val report = runner.run()

        report.status shouldBe BatchStatus.COMPLETED
        report.readCount shouldBeEqualTo 0L
        report.writeCount shouldBeEqualTo 0L
        writer.collected.isEmpty() shouldBe true
    }

    // ─── 3. Processor null 필터 ──────────────────────────────────────────────

    @Test
    fun `3 Processor null 필터 - skipCount 증가 없이 항목 제외`() = runSuspendIO {
        val reader = ListBatchReader(listOf(1, 2, 3, 4, 5))
        val writer = CollectingWriter<Int>()
        val repo = InMemoryBatchJobRepository()
        // 홀수만 통과
        val processor = BatchProcessor<Int, Int> { if (it % 2 != 0) it else null }
        val step = makeStep(chunkSize = 5, reader = reader, writer = writer, processor = processor)
        val runner = BatchStepRunner(step, makeJobExecution(), repo)

        val report = runner.run()

        report.status shouldBe BatchStatus.COMPLETED
        report.readCount shouldBeEqualTo 5L
        report.writeCount shouldBeEqualTo 3L
        report.skipCount shouldBeEqualTo 0L  // null 필터는 skip이 아님
        writer.collected shouldBeEqualTo listOf(1, 3, 5)
    }

    // ─── 4. Processor 예외 + skipPolicy.ALL ──────────────────────────────────

    @Test
    fun `4 Processor 예외 - skipPolicy ALL → COMPLETED_WITH_SKIPS`() = runSuspendIO {
        val reader = ListBatchReader(listOf("ok", "bad", "ok2"))
        val writer = CollectingWriter<String>()
        val repo = InMemoryBatchJobRepository()
        val processor = BatchProcessor<String, String> { item ->
            if (item == "bad") throw IllegalArgumentException("bad item") else item
        }
        val step = makeStep(
            chunkSize = 3,
            reader = reader,
            writer = writer,
            processor = processor,
            skipPolicy = SkipPolicy.ALL,
        )
        val runner = BatchStepRunner(step, makeJobExecution(), repo)

        val report = runner.run()

        report.status shouldBe BatchStatus.COMPLETED_WITH_SKIPS
        report.readCount shouldBeEqualTo 3L
        report.writeCount shouldBeEqualTo 2L
        report.skipCount shouldBeEqualTo 1L
        writer.collected shouldBeEqualTo listOf("ok", "ok2")
    }

    // ─── 5. Processor 예외 + skipPolicy.NONE ─────────────────────────────────

    @Test
    fun `5 Processor 예외 - skipPolicy NONE → FAILED`() = runSuspendIO {
        val reader = ListBatchReader(listOf("ok", "bad"))
        val writer = CollectingWriter<String>()
        val repo = InMemoryBatchJobRepository()
        val processor = BatchProcessor<String, String> { item ->
            if (item == "bad") throw RuntimeException("fatal") else item
        }
        val step = makeStep(
            chunkSize = 5,
            reader = reader,
            writer = writer,
            processor = processor,
            skipPolicy = SkipPolicy.NONE,
        )
        val runner = BatchStepRunner(step, makeJobExecution(), repo)

        val report = runner.run()

        report.status shouldBe BatchStatus.FAILED
        report.error.shouldNotBeNull()
        report.error shouldBeInstanceOf RuntimeException::class
    }

    // ─── 6. Writer 재시도 성공 ────────────────────────────────────────────────

    @Test
    fun `6 Writer retry 성공 - 1회 실패 후 성공 → COMPLETED`() = runTest {
        val reader = ListBatchReader(listOf("a", "b"))
        val writer = FailThenSucceedWriter<String>(failCount = 1)
        val repo = InMemoryBatchJobRepository()
        val step = makeStep(
            chunkSize = 2,
            reader = reader,
            writer = writer,
            retryPolicy = RetryPolicy(maxAttempts = 2, delay = 10.milliseconds),
        )
        val runner = BatchStepRunner(step, makeJobExecution(), repo)

        val report = runner.run()

        report.status shouldBe BatchStatus.COMPLETED
        report.writeCount shouldBeEqualTo 2L
        writer.collected shouldBeEqualTo listOf("a", "b")
    }

    // ─── 7. Writer retry 소진 + skip 허용 ────────────────────────────────────

    @Test
    fun `7 Writer retry 소진 - skip 허용 → COMPLETED_WITH_SKIPS`() = runTest {
        val reader = ListBatchReader(listOf("a", "b", "c"))
        val writer = AlwaysFailWriter<String>()
        val repo = InMemoryBatchJobRepository()
        val step = makeStep(
            chunkSize = 3,
            reader = reader,
            writer = writer,
            retryPolicy = RetryPolicy(maxAttempts = 2, delay = 10.milliseconds),
            skipPolicy = SkipPolicy.ALL,
        )
        val runner = BatchStepRunner(step, makeJobExecution(), repo)

        val report = runner.run()

        report.status shouldBe BatchStatus.COMPLETED_WITH_SKIPS
        report.skipCount shouldBeEqualTo 3L  // chunk.size
        report.writeCount shouldBeEqualTo 0L
    }

    // ─── 8. Writer retry 소진 + skip 불허 ────────────────────────────────────

    @Test
    fun `8 Writer retry 소진 - skip 불허 → FAILED`() = runTest {
        val reader = ListBatchReader(listOf("a"))
        val writer = AlwaysFailWriter<String>()
        val repo = InMemoryBatchJobRepository()
        val step = makeStep(
            chunkSize = 1,
            reader = reader,
            writer = writer,
            retryPolicy = RetryPolicy(maxAttempts = 2, delay = 10.milliseconds),
            skipPolicy = SkipPolicy.NONE,
        )
        val runner = BatchStepRunner(step, makeJobExecution(), repo)

        val report = runner.run()

        report.status shouldBe BatchStatus.FAILED
        report.error.shouldNotBeNull()
    }

    // ─── 9. CancellationException ────────────────────────────────────────────

    @Test
    fun `9 CancellationException - STOPPED 저장 후 재던짐`() = runSuspendIO {
        val reader = object : BatchReader<String> {
            override suspend fun read(): String? = throw CancellationException("테스트 취소")
        }
        val writer = CollectingWriter<String>()
        val repo = InMemoryBatchJobRepository()
        val step = makeStep<String, String>(reader = reader, writer = writer)

        val je = makeJobExecution()
        val runner = BatchStepRunner(step, je, repo)

        var thrown: Throwable? = null
        try {
            runner.run()
        } catch (e: CancellationException) {
            thrown = e
        }

        thrown.shouldNotBeNull()
        thrown shouldBeInstanceOf CancellationException::class

        // StepExecution이 STOPPED로 저장되어야 함
        val je2 = repo.findOrCreateJobExecution(je.jobName, je.params)
        val se = repo.findOrCreateStepExecution(je2, step.name)
        // STOPPED 상태이므로 findOrCreate가 RUNNING으로 복원
        se.status shouldBe BatchStatus.RUNNING
    }

    // ─── 10. 이미 COMPLETED StepExecution ────────────────────────────────────

    @Test
    fun `10 이미 COMPLETED StepExecution - reader open 없이 즉시 skip`() = runSuspendIO {
        val reader = ListBatchReader(listOf("a"))
        val writer = CollectingWriter<String>()
        val repo = InMemoryBatchJobRepository()
        val step = makeStep<String, String>(reader = reader, writer = writer)
        val je = makeJobExecution()

        // StepExecution을 미리 COMPLETED 상태로 만들기
        val se = repo.findOrCreateStepExecution(je, step.name)
        repo.completeStepExecution(
            se,
            StepReport(
                stepName = step.name,
                status = BatchStatus.COMPLETED,
                readCount = 50L,
                writeCount = 50L,
            ),
        )

        val runner = BatchStepRunner(step, je, repo)
        val report = runner.run()

        report.status shouldBe BatchStatus.COMPLETED
        report.readCount shouldBeEqualTo 50L  // 기존 통계 반환
        reader.wasOpened shouldBe false       // open 호출 없음
        writer.collected.isEmpty() shouldBe true
    }

    // ─── 12. commitTimeout 초과 → WriteTimeoutException → skip 허용 ──────────

    @Test
    fun `12 commitTimeout 초과 - WriteTimeoutException → skip 허용 시 COMPLETED_WITH_SKIPS`() = runTest {
        val reader = ListBatchReader(listOf("a", "b"))
        val writer = SlowWriter<String>(writeDelay = 1.seconds)
        val repo = InMemoryBatchJobRepository()
        val step = BatchStep(
            name = "timeoutStep",
            chunkSize = 2,
            reader = reader,
            writer = writer,
            skipPolicy = SkipPolicy.ALL,
            retryPolicy = RetryPolicy.NONE,
            commitTimeout = 10.milliseconds,  // 슬로우 writer보다 훨씬 짧음
        )
        val runner = BatchStepRunner(step, makeJobExecution(), repo)

        val report = runner.run()

        // WriteTimeoutException이 발생 → skip 허용 → COMPLETED_WITH_SKIPS
        report.status shouldBe BatchStatus.COMPLETED_WITH_SKIPS
        report.skipCount shouldBeEqualTo 2L
        report.writeCount shouldBeEqualTo 0L
    }

    // ─── 13. commitTimeout = ZERO (타임아웃 미적용) ───────────────────────────

    @Test
    fun `13 commitTimeout ZERO - 타임아웃 미적용, 정상 완료`() = runSuspendIO {
        val reader = ListBatchReader(listOf("x", "y"))
        val writer = CollectingWriter<String>()
        val repo = InMemoryBatchJobRepository()
        val step = BatchStep(
            name = "noTimeoutStep",
            chunkSize = 2,
            reader = reader,
            writer = writer,
            commitTimeout = Duration.ZERO,  // 타임아웃 비활성화
        )
        val runner = BatchStepRunner(step, makeJobExecution(), repo)

        val report = runner.run()

        report.status shouldBe BatchStatus.COMPLETED
        report.writeCount shouldBeEqualTo 2L
        writer.collected shouldBeEqualTo listOf("x", "y")
    }

    // ─── 14. reader.open() throws → step FAILED ──────────────────────────────

    @Test
    fun `14 reader open 실패 - step FAILED 반환`() = runSuspendIO {
        val reader = FailOnOpenReader<String>()
        val writer = CollectingWriter<String>()
        val repo = InMemoryBatchJobRepository()
        val step = makeStep<String, String>(reader = reader, writer = writer)
        val runner = BatchStepRunner(step, makeJobExecution(), repo)

        val report = runner.run()

        report.status shouldBe BatchStatus.FAILED
        report.error.shouldNotBeNull()
        report.readCount shouldBeEqualTo 0L
        writer.collected.isEmpty() shouldBe true
    }

    // ─── 15. writer.close() throws → 주 결과 마스킹 없음 ────────────────────

    @Test
    fun `15 writer close 실패 - COMPLETED 결과를 마스킹하지 않음`() = runSuspendIO {
        val reader = ListBatchReader(listOf("a", "b", "c"))
        val writer = FailOnCloseWriter<String>()
        val repo = InMemoryBatchJobRepository()
        val step = makeStep<String, String>(reader = reader, writer = writer)
        val runner = BatchStepRunner(step, makeJobExecution(), repo)

        // writer.close()에서 예외가 발생하더라도 COMPLETED 결과를 반환해야 함
        val report = runner.run()

        report.status shouldBe BatchStatus.COMPLETED
        report.writeCount shouldBeEqualTo 3L
        writer.collected shouldBeEqualTo listOf("a", "b", "c")
    }

    // ─── 11. 이미 COMPLETED_WITH_SKIPS ───────────────────────────────────────

    @Test
    fun `11 이미 COMPLETED_WITH_SKIPS - 즉시 skip 반환`() = runSuspendIO {
        val reader = ListBatchReader(listOf("a"))
        val writer = CollectingWriter<String>()
        val repo = InMemoryBatchJobRepository()
        val step = makeStep<String, String>(reader = reader, writer = writer)
        val je = makeJobExecution()

        val se = repo.findOrCreateStepExecution(je, step.name)
        repo.completeStepExecution(
            se,
            StepReport(
                stepName = step.name,
                status = BatchStatus.COMPLETED_WITH_SKIPS,
                readCount = 10L,
                writeCount = 8L,
                skipCount = 2L,
            ),
        )

        val runner = BatchStepRunner(step, je, repo)
        val report = runner.run()

        report.status shouldBe BatchStatus.COMPLETED_WITH_SKIPS
        report.skipCount shouldBeEqualTo 2L
        reader.wasOpened shouldBe false
    }
}
