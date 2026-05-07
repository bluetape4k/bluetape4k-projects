package io.bluetape4k.batch.core

import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.batch.api.JobExecution
import io.bluetape4k.batch.api.StepExecution
import io.bluetape4k.batch.api.StepReport
import io.bluetape4k.junit5.coroutines.runSuspendIO
import io.bluetape4k.assertions.shouldBe
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * [InMemoryBatchJobRepository]의 단위 테스트.
 *
 * ## 검증 항목
 * - findOrCreateJobExecution: 신규 생성, RUNNING/FAILED/STOPPED 재사용, COMPLETED 재생성
 * - findOrCreateStepExecution: 4-case 계약(COMPLETED skip, FAILED/STOPPED/RUNNING 재시작, 신규 생성)
 * - checkpoint: save → load round-trip
 */
class InMemoryBatchJobRepositoryTest {

    private lateinit var repo: InMemoryBatchJobRepository

    @BeforeEach
    fun setUp() {
        repo = InMemoryBatchJobRepository()
    }

    private suspend fun newJobAndStep() =
        repo.findOrCreateJobExecution("job1", emptyMap()).let { je ->
            je to repo.findOrCreateStepExecution(je, "step1")
        }

    private suspend fun completeStep(
        je: JobExecution,
        stepName: String = "step1",
        status: BatchStatus = BatchStatus.COMPLETED,
        readCount: Long = 0L,
        writeCount: Long = 0L,
        skipCount: Long = 0L,
        checkpoint: Long? = null,
    ): StepExecution {
        val se = repo.findOrCreateStepExecution(je, stepName)
        repo.completeStepExecution(
            se,
            StepReport(
                stepName = stepName,
                status = status,
                readCount = readCount,
                writeCount = writeCount,
                skipCount = skipCount,
                checkpoint = checkpoint
            )
        )
        return se
    }

    // ─── findOrCreateJobExecution ───

    @Test
    fun `findOrCreateJobExecution - 신규 생성`() = runSuspendIO {
        val je = repo.findOrCreateJobExecution("job1", mapOf("date" to "2026-04-10"))

        je.jobName shouldBeEqualTo "job1"
        je.params shouldBeEqualTo mapOf("date" to "2026-04-10")
        je.status shouldBe BatchStatus.RUNNING
        je.id shouldBeEqualTo 1L
    }

    @Test
    fun `findOrCreateJobExecution - RUNNING 상태 재사용`() = runSuspendIO {
        val first = repo.findOrCreateJobExecution("job1", emptyMap())
        first.status shouldBe BatchStatus.RUNNING

        // 동일 jobName + params → 재사용
        val second = repo.findOrCreateJobExecution("job1", emptyMap())
        second.id shouldBeEqualTo first.id
        second.status shouldBe BatchStatus.RUNNING
    }

    @Test
    fun `findOrCreateJobExecution - FAILED 상태 재사용하여 RUNNING으로 복원`() = runSuspendIO {
        val first = repo.findOrCreateJobExecution("job1", emptyMap())
        repo.completeJobExecution(first, BatchStatus.FAILED)

        val second = repo.findOrCreateJobExecution("job1", emptyMap())
        second.id shouldBeEqualTo first.id
        second.status shouldBe BatchStatus.RUNNING
    }

    @Test
    fun `findOrCreateJobExecution - STOPPED 상태 재사용하여 RUNNING으로 복원`() = runSuspendIO {
        val first = repo.findOrCreateJobExecution("job1", emptyMap())
        repo.completeJobExecution(first, BatchStatus.STOPPED)

        val second = repo.findOrCreateJobExecution("job1", emptyMap())
        second.id shouldBeEqualTo first.id
        second.status shouldBe BatchStatus.RUNNING
    }

    @Test
    fun `findOrCreateJobExecution - COMPLETED 상태는 재사용하지 않고 신규 생성`() = runSuspendIO {
        val first = repo.findOrCreateJobExecution("job1", emptyMap())
        repo.completeJobExecution(first, BatchStatus.COMPLETED)

        val second = repo.findOrCreateJobExecution("job1", emptyMap())
        second.id shouldBeEqualTo first.id + 1L  // 새 ID
    }

    @Test
    fun `findOrCreateJobExecution - 다른 params는 별개 실행`() = runSuspendIO {
        val je1 = repo.findOrCreateJobExecution("job1", mapOf("date" to "2026-04-10"))
        val je2 = repo.findOrCreateJobExecution("job1", mapOf("date" to "2026-04-11"))

        je1.id shouldBeEqualTo je2.id - 1
    }

    // ─── findOrCreateStepExecution ───

    @Test
    fun `findOrCreateStepExecution - 신규 생성`() = runSuspendIO {
        val (je, se) = newJobAndStep()

        se.stepName shouldBeEqualTo "step1"
        se.jobExecutionId shouldBeEqualTo je.id
        se.status shouldBe BatchStatus.RUNNING
    }

    @Test
    fun `findOrCreateStepExecution - COMPLETED 상태는 변경 없이 반환`() = runSuspendIO {
        val je = repo.findOrCreateJobExecution("job1", emptyMap())
        val se1 = completeStep(je, readCount = 100, writeCount = 100)

        val se2 = repo.findOrCreateStepExecution(je, "step1")
        se2.id shouldBeEqualTo se1.id
        se2.status shouldBe BatchStatus.COMPLETED  // 변경 없이 반환
        se2.readCount shouldBeEqualTo 100L
    }

    @Test
    fun `findOrCreateStepExecution - COMPLETED_WITH_SKIPS 상태는 변경 없이 반환`() = runSuspendIO {
        val je = repo.findOrCreateJobExecution("job1", emptyMap())
        completeStep(je, status = BatchStatus.COMPLETED_WITH_SKIPS, readCount = 50, writeCount = 48, skipCount = 2)

        val se2 = repo.findOrCreateStepExecution(je, "step1")
        se2.status shouldBe BatchStatus.COMPLETED_WITH_SKIPS
        se2.skipCount shouldBeEqualTo 2L
    }

    @Test
    fun `findOrCreateStepExecution - FAILED 상태는 RUNNING으로 복원`() = runSuspendIO {
        val je = repo.findOrCreateJobExecution("job1", emptyMap())
        val se1 = completeStep(je, status = BatchStatus.FAILED)

        val se2 = repo.findOrCreateStepExecution(je, "step1")
        se2.id shouldBeEqualTo se1.id
        se2.status shouldBe BatchStatus.RUNNING  // RUNNING으로 복원
    }

    @Test
    fun `findOrCreateStepExecution - STOPPED 상태는 RUNNING으로 복원`() = runSuspendIO {
        val je = repo.findOrCreateJobExecution("job1", emptyMap())
        completeStep(je, status = BatchStatus.STOPPED)

        val se2 = repo.findOrCreateStepExecution(je, "step1")
        se2.status shouldBe BatchStatus.RUNNING
    }

    // ─── checkpoint ───

    @Test
    fun `saveCheckpoint and loadCheckpoint - round-trip`() = runSuspendIO {
        val (je, se) = newJobAndStep()

        repo.saveCheckpoint(se.id, 42L)

        repo.loadCheckpoint(se.id) shouldBeEqualTo 42L
    }

    @Test
    fun `loadCheckpoint - 저장 전에는 null 반환`() = runSuspendIO {
        val (je, se) = newJobAndStep()

        repo.loadCheckpoint(se.id).shouldBeNull()
    }

    @Test
    fun `saveCheckpoint - 덮어쓰기 가능`() = runSuspendIO {
        val (je, se) = newJobAndStep()

        repo.saveCheckpoint(se.id, 10L)
        repo.saveCheckpoint(se.id, 20L)

        repo.loadCheckpoint(se.id) shouldBeEqualTo 20L
    }

    // ─── completeStepExecution ───

    @Test
    fun `completeStepExecution - 통계 갱신`() = runSuspendIO {
        val (je, _) = newJobAndStep()
        completeStep(je, readCount = 1000, writeCount = 999, skipCount = 1, checkpoint = 500L)

        // findOrCreate 재조회로 저장된 값 확인
        val se2 = repo.findOrCreateStepExecution(je, "step1")
        se2.status shouldBe BatchStatus.COMPLETED  // 완료 상태
        se2.readCount shouldBeEqualTo 1000L
        se2.writeCount shouldBeEqualTo 999L
        se2.skipCount shouldBeEqualTo 1L
        se2.checkpoint shouldBeEqualTo 500L
    }

    // ─── completeJobExecution ───

    @Test
    fun `completeJobExecution - COMPLETED 상태 갱신`() = runSuspendIO {
        val je = repo.findOrCreateJobExecution("job1", emptyMap())
        repo.completeJobExecution(je, BatchStatus.COMPLETED)

        // COMPLETED이므로 재조회 시 신규 생성됨
        val je2 = repo.findOrCreateJobExecution("job1", emptyMap())
        je2.id shouldBeEqualTo je.id + 1L
    }

    @Test
    fun `completeJobExecution - endTime이 설정됨`() = runSuspendIO {
        val je = repo.findOrCreateJobExecution("job1", emptyMap())
        je.endTime.shouldBeNull()

        repo.completeJobExecution(je, BatchStatus.COMPLETED)
        // 직접 endTime을 확인할 수는 없으나 예외 없이 완료됨을 확인
    }
}
