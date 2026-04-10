package io.bluetape4k.batch.core

import io.bluetape4k.batch.api.BatchProcessor
import io.bluetape4k.batch.api.BatchReader
import io.bluetape4k.batch.api.BatchReport
import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.batch.api.BatchWriter
import io.bluetape4k.batch.api.SkipPolicy
import io.bluetape4k.batch.core.BatchStepRunnerTest.CollectingWriter
import io.bluetape4k.batch.core.BatchStepRunnerTest.ListBatchReader
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.workflow.api.WorkContext
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test

/**
 * [BatchJob]의 통합 테스트.
 *
 * ## 검증 항목
 * - 단일 Step 성공 → BatchReport.Success
 * - 다중 Step 순차 실행
 * - Step FAILED → BatchReport.Failure (후속 Step 미실행)
 * - Skip 발생 → BatchReport.PartiallyCompleted
 * - CancellationException 전파
 * - SuspendWork.execute() 매핑
 */
class BatchJobTest {

    private fun simpleStep(
        name: String,
        items: List<String>,
        writer: CollectingWriter<String> = CollectingWriter(),
        skipPolicy: SkipPolicy = SkipPolicy.NONE,
    ): BatchStep<String, String> = BatchStep(
        name = name,
        chunkSize = 10,
        reader = ListBatchReader(items),
        writer = writer,
        skipPolicy = skipPolicy,
    )

    // ─── 단일 Step 성공 ───────────────────────────────────────────────────────

    @Test
    fun `단일 Step 성공 - BatchReport Success 반환`() = runSuspendIO {
        val writer = CollectingWriter<String>()
        val step = simpleStep("step1", listOf("a", "b", "c"), writer)

        val job = BatchJob(
            name = "testJob",
            params = emptyMap(),
            steps = listOf(step),
            repository = InMemoryBatchJobRepository(),
        )

        val report = job.run()

        report shouldBeInstanceOf BatchReport.Success::class
        report.stepReports.size shouldBeEqualTo 1
        report.stepReports[0].status shouldBe BatchStatus.COMPLETED
        report.stepReports[0].writeCount shouldBeEqualTo 3L
        writer.collected shouldBeEqualTo listOf("a", "b", "c")
    }

    // ─── 다중 Step 순차 실행 ─────────────────────────────────────────────────

    @Test
    fun `다중 Step 순차 실행 - 모두 성공 → Success`() = runSuspendIO {
        val writer1 = CollectingWriter<String>()
        val writer2 = CollectingWriter<String>()
        val step1 = simpleStep("step1", listOf("a", "b"), writer1)
        val step2 = simpleStep("step2", listOf("c", "d"), writer2)

        val job = BatchJob(
            name = "multiStepJob",
            params = emptyMap(),
            steps = listOf(step1, step2),
            repository = InMemoryBatchJobRepository(),
        )

        val report = job.run()

        report shouldBeInstanceOf BatchReport.Success::class
        report.stepReports.size shouldBeEqualTo 2
        report.stepReports[0].stepName shouldBeEqualTo "step1"
        report.stepReports[1].stepName shouldBeEqualTo "step2"
        writer1.collected shouldBeEqualTo listOf("a", "b")
        writer2.collected shouldBeEqualTo listOf("c", "d")
    }

    // ─── Step FAILED → 후속 미실행 ───────────────────────────────────────────

    @Test
    fun `Step FAILED - BatchReport Failure 반환, 후속 Step 미실행`() = runSuspendIO {
        val writer2 = CollectingWriter<String>()
        val failingReader = object : BatchReader<String> {
            override suspend fun read(): String? = throw RuntimeException("step 1 read 실패")
        }
        val step1 = BatchStep(
            name = "failStep",
            chunkSize = 1,
            reader = failingReader,
            writer = CollectingWriter(),
        )
        val step2 = simpleStep("step2", listOf("x"), writer2)

        val job = BatchJob(
            name = "failJob",
            params = emptyMap(),
            steps = listOf(step1, step2),
            repository = InMemoryBatchJobRepository(),
        )

        val report = job.run()

        report shouldBeInstanceOf BatchReport.Failure::class
        val failure = report as BatchReport.Failure
        failure.error.shouldNotBeNull()
        failure.stepReports.size shouldBeEqualTo 1  // step2 미실행
        failure.stepReports[0].stepName shouldBeEqualTo "failStep"
        writer2.collected.isEmpty() shouldBe true  // step2 미실행
    }

    // ─── Skip 발생 → PartiallyCompleted ─────────────────────────────────────

    @Test
    fun `Skip 발생 - BatchReport PartiallyCompleted 반환`() = runSuspendIO {
        val writer = CollectingWriter<String>()
        val step = BatchStep(
            name = "skipStep",
            chunkSize = 5,
            reader = ListBatchReader(listOf("ok", "bad", "ok2")),
            processor = BatchProcessor<String, String> { item ->
                if (item == "bad") throw IllegalArgumentException("skip me") else item
            },
            writer = writer,
            skipPolicy = SkipPolicy.ALL,
        )

        val job = BatchJob(
            name = "skipJob",
            params = emptyMap(),
            steps = listOf(step),
            repository = InMemoryBatchJobRepository(),
        )

        val report = job.run()

        report shouldBeInstanceOf BatchReport.PartiallyCompleted::class
        report.stepReports[0].skipCount shouldBeEqualTo 1L
    }

    // ─── CancellationException 전파 ──────────────────────────────────────────

    @Test
    fun `CancellationException - STOPPED 영속화 후 재던짐`() = runSuspendIO {
        val cancellingReader = object : BatchReader<String> {
            override suspend fun read(): String = throw kotlinx.coroutines.CancellationException("외부 취소")
        }
        val step = BatchStep(
            name = "step1",
            chunkSize = 1,
            reader = cancellingReader,
            writer = CollectingWriter(),
        )

        val job = BatchJob(
            name = "cancelJob",
            params = emptyMap(),
            steps = listOf(step),
            repository = InMemoryBatchJobRepository(),
        )

        var thrown: Throwable? = null
        try {
            job.run()
        } catch (e: kotlinx.coroutines.CancellationException) {
            thrown = e
        }

        thrown.shouldNotBeNull()
        thrown shouldBeInstanceOf kotlinx.coroutines.CancellationException::class
    }

    // ─── 재시작: FAILED 잡 재시작 시 COMPLETED Step skip ────────────────────

    @Test
    fun `재시작 - FAILED 잡 재시작 시 COMPLETED Step은 skip되고 나머지만 실행`() = runSuspendIO {
        val writer1 = CollectingWriter<String>()
        val repo = InMemoryBatchJobRepository()

        // 1차 실행: step1 완료, step2 실패 → BatchReport.Failure
        val failingReader = object : BatchReader<String> {
            override suspend fun read(): String? = throw RuntimeException("step2 강제 실패")
        }
        val job1 = BatchJob(
            name = "restartJob",
            params = emptyMap(),
            steps = listOf(
                simpleStep("step1", listOf("a", "b"), writer1),
                BatchStep(name = "step2", chunkSize = 1, reader = failingReader, writer = CollectingWriter()),
            ),
            repository = repo,
        )
        val report1 = job1.run()
        report1 shouldBeInstanceOf BatchReport.Failure::class
        writer1.collected shouldBeEqualTo listOf("a", "b")  // step1은 성공

        // 2차 실행: FAILED JobExecution 재사용 → step1 COMPLETED(skip), step2 재실행
        val writer2b = CollectingWriter<String>()
        val job2 = BatchJob(
            name = "restartJob",
            params = emptyMap(),
            steps = listOf(
                simpleStep("step1", listOf("x", "y"), writer1),  // step1 skip → writer1에 추가 없음
                simpleStep("step2", listOf("c", "d"), writer2b),
            ),
            repository = repo,
        )
        val report2 = job2.run()

        report2 shouldBeInstanceOf BatchReport.Success::class
        writer1.collected shouldBeEqualTo listOf("a", "b")  // step1 재실행 안 됨 (skip)
        writer2b.collected shouldBeEqualTo listOf("c", "d")  // step2 정상 실행
    }

    // ─── SuspendWork.execute() 매핑 ─────────────────────────────────────────

    @Test
    fun `execute - Success → WorkReport success`() = runSuspendIO {
        val step = simpleStep("step1", listOf("a"))
        val job = BatchJob(
            name = "workJob",
            params = emptyMap(),
            steps = listOf(step),
            repository = InMemoryBatchJobRepository(),
        )

        val context = WorkContext()
        val workReport = job.execute(context)

        workReport.status shouldBe io.bluetape4k.workflow.api.WorkStatus.COMPLETED
        context.contains("batch.workJob.report") shouldBe true
    }

    @Test
    fun `execute - Failure → WorkReport failure`() = runSuspendIO {
        val failingStep = BatchStep(
            name = "step1",
            chunkSize = 1,
            reader = object : BatchReader<String> {
                override suspend fun read(): String? = throw RuntimeException("실패")
            },
            writer = CollectingWriter(),
        )

        val job = BatchJob(
            name = "failWorkJob",
            params = emptyMap(),
            steps = listOf(failingStep),
            repository = InMemoryBatchJobRepository(),
        )

        val context = WorkContext()
        val workReport = job.execute(context)

        workReport.status shouldBe io.bluetape4k.workflow.api.WorkStatus.FAILED
    }

    @Test
    fun `execute - PartiallyCompleted → WorkReport success`() = runSuspendIO {
        val step = BatchStep(
            name = "step1",
            chunkSize = 5,
            reader = ListBatchReader(listOf("ok", "bad")),
            processor = BatchProcessor<String, String> { item ->
                if (item == "bad") throw IllegalArgumentException() else item
            },
            writer = CollectingWriter(),
            skipPolicy = SkipPolicy.ALL,
        )

        val job = BatchJob(
            name = "partialJob",
            params = emptyMap(),
            steps = listOf(step),
            repository = InMemoryBatchJobRepository(),
        )

        val context = WorkContext()
        val workReport = job.execute(context)

        workReport.status shouldBe io.bluetape4k.workflow.api.WorkStatus.COMPLETED
        context.get<Long>("batch.partialJob.skipCount") shouldBeEqualTo 1L
    }
}
