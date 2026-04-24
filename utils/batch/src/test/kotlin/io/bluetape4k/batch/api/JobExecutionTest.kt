package io.bluetape4k.batch.api

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * [JobExecution] 데이터 클래스 + 상태 전이 검증 테스트.
 */
class JobExecutionTest {

    companion object : KLogging()

    // ─── 1. 기본 생성 ────────────────────────────────────────────────────────

    @Test
    fun `기본 생성 - 필수 필드 검증`() {
        val exec = JobExecution(id = 1L, jobName = "importJob")

        exec.id shouldBeEqualTo 1L
        exec.jobName shouldBeEqualTo "importJob"
        exec.params shouldBeEqualTo emptyMap()
        exec.status shouldBeEqualTo BatchStatus.STARTING
        exec.startTime.shouldNotBeNull()
        exec.endTime.shouldBeNull()
    }

    @Test
    fun `params 포함 생성`() {
        val params = mapOf("date" to "2026-04-10", "env" to "prod")
        val exec = JobExecution(
            id = 2L,
            jobName = "exportJob",
            params = params,
        )

        exec.params["date"] shouldBeEqualTo "2026-04-10"
        exec.params["env"] shouldBeEqualTo "prod"
    }

    // ─── 2. data class equality & copy ──────────────────────────────────────

    @Test
    fun `equality - 동일 필드로 생성한 두 인스턴스는 동등`() {
        val t = Instant.now()
        val exec1 = JobExecution(id = 1L, jobName = "job", startTime = t)
        val exec2 = JobExecution(id = 1L, jobName = "job", startTime = t)

        exec1 shouldBeEqualTo exec2
    }

    @Test
    fun `copy - status만 변경`() {
        val original = JobExecution(id = 1L, jobName = "job")
        val running = original.copy(status = BatchStatus.RUNNING)

        running.id shouldBeEqualTo original.id
        running.jobName shouldBeEqualTo original.jobName
        running.status shouldBeEqualTo BatchStatus.RUNNING
    }

    @Test
    fun `copy - endTime 설정`() {
        val original = JobExecution(id = 1L, jobName = "job")
        val end = Instant.now()
        val completed = original.copy(status = BatchStatus.COMPLETED, endTime = end)

        completed.endTime shouldBeEqualTo end
        completed.status shouldBeEqualTo BatchStatus.COMPLETED
    }

    // ─── 3. 상태 전이 시뮬레이션 ────────────────────────────────────────────

    @Test
    fun `상태 전이 - STARTING → RUNNING → COMPLETED`() {
        val exec = JobExecution(id = 1L, jobName = "job", status = BatchStatus.STARTING)

        val running = exec.copy(status = BatchStatus.RUNNING)
        running.status shouldBeEqualTo BatchStatus.RUNNING

        val completed = running.copy(status = BatchStatus.COMPLETED, endTime = Instant.now())
        completed.status shouldBeEqualTo BatchStatus.COMPLETED
        completed.endTime.shouldNotBeNull()
    }

    @Test
    fun `상태 전이 - RUNNING → FAILED`() {
        val exec = JobExecution(id = 1L, jobName = "job", status = BatchStatus.RUNNING)
        val failed = exec.copy(status = BatchStatus.FAILED, endTime = Instant.now())

        failed.status shouldBeEqualTo BatchStatus.FAILED
        failed.endTime.shouldNotBeNull()
    }

    @Test
    fun `상태 전이 - RUNNING → STOPPED`() {
        val exec = JobExecution(id = 1L, jobName = "job", status = BatchStatus.RUNNING)
        val stopped = exec.copy(status = BatchStatus.STOPPED, endTime = Instant.now())

        stopped.status shouldBeEqualTo BatchStatus.STOPPED
    }

    // ─── 4. isTerminal 기반 분기 ─────────────────────────────────────────────

    @Test
    fun `STARTING status - isTerminal은 false`() {
        val exec = JobExecution(id = 1L, jobName = "job", status = BatchStatus.STARTING)
        exec.status.isTerminal shouldBe false
    }

    @Test
    fun `RUNNING status - isTerminal은 false`() {
        val exec = JobExecution(id = 1L, jobName = "job", status = BatchStatus.RUNNING)
        exec.status.isTerminal shouldBe false
    }

    @Test
    fun `COMPLETED status - isTerminal은 true`() {
        val exec = JobExecution(id = 1L, jobName = "job", status = BatchStatus.COMPLETED)
        exec.status.isTerminal shouldBe true
    }

    @Test
    fun `FAILED status - isTerminal은 true`() {
        val exec = JobExecution(id = 1L, jobName = "job", status = BatchStatus.FAILED)
        exec.status.isTerminal shouldBe true
    }

    // ─── 5. Serializable 보장 ────────────────────────────────────────────────

    @Test
    fun `JobExecution은 Serializable을 구현한다`() {
        val exec = JobExecution(id = 1L, jobName = "job")
        (exec is java.io.Serializable) shouldBe true
    }
}
