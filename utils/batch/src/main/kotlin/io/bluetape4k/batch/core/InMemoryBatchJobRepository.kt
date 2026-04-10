package io.bluetape4k.batch.core

import io.bluetape4k.batch.api.BatchJobRepository
import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.batch.api.JobExecution
import io.bluetape4k.batch.api.StepExecution
import io.bluetape4k.batch.api.StepReport
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 인메모리 [BatchJobRepository] 구현체. 테스트 및 단순 사용에 적합하다.
 *
 * ## 특징
 * - checkpoint를 `Any` 객체 그대로 [ConcurrentHashMap]에 저장
 * - 재시작 시 `RUNNING/FAILED/STOPPED` 상태의 JobExecution을 재사용
 * - thread-safe ([ConcurrentHashMap] + [AtomicLong])
 *
 * ## findOrCreateStepExecution 4-case 계약
 * | 기존 status              | 동작                               |
 * |--------------------------|-----------------------------------|
 * | COMPLETED                | 변경 없이 그대로 반환 (runner skip) |
 * | COMPLETED_WITH_SKIPS     | 변경 없이 그대로 반환 (runner skip) |
 * | FAILED / STOPPED / RUNNING | copy(status=RUNNING) 저장 후 반환  |
 * | 없음                     | 신규 생성 (status=RUNNING)         |
 *
 * ## 사용 예
 * ```kotlin
 * val repository = InMemoryBatchJobRepository()
 * val jobExecution = repository.findOrCreateJobExecution("importOrders", mapOf("date" to "2026-04-10"))
 * val stepExecution = repository.findOrCreateStepExecution(jobExecution, "readStep")
 * ```
 */
class InMemoryBatchJobRepository : BatchJobRepository {

    companion object : KLogging()

    private val idCounter = AtomicLong(0L)
    private val jobExecutions = ConcurrentHashMap<Long, JobExecution>()
    private val stepExecutions = ConcurrentHashMap<Long, StepExecution>()
    private val checkpoints = ConcurrentHashMap<Long, Any>()

    /**
     * jobName + params 조합의 재시작 대상 [JobExecution]을 조회하거나 신규 생성한다.
     *
     * `RUNNING/FAILED/STOPPED` 상태의 기존 실행을 재사용하며 상태를 `RUNNING`으로 복원한다.
     * 해당 상태의 기존 실행이 없으면 신규 [JobExecution]을 생성한다.
     *
     * @param jobName Job 이름 (blank 불가)
     * @param params Job 실행 파라미터
     * @return 재사용하거나 신규 생성한 [JobExecution]
     */
    override suspend fun findOrCreateJobExecution(
        jobName: String,
        params: Map<String, Any>,
    ): JobExecution {
        jobName.requireNotBlank("jobName")

        val existing = jobExecutions.values.firstOrNull { je ->
            je.jobName == jobName &&
                je.params == params &&
                je.status in setOf(BatchStatus.RUNNING, BatchStatus.FAILED, BatchStatus.STOPPED)
        }

        if (existing != null) {
            log.debug { "기존 JobExecution 재사용: jobName=$jobName, id=${existing.id}, status=${existing.status}" }
            val updated = existing.copy(status = BatchStatus.RUNNING)
            jobExecutions[updated.id] = updated
            return updated
        }

        val newId = idCounter.incrementAndGet()
        val newExecution = JobExecution(
            id = newId,
            jobName = jobName,
            params = params,
            status = BatchStatus.RUNNING,
            startTime = Instant.now(),
        )
        jobExecutions[newId] = newExecution
        log.debug { "신규 JobExecution 생성: jobName=$jobName, id=$newId" }
        return newExecution
    }

    /**
     * [JobExecution]을 완료 상태로 갱신한다.
     *
     * @param execution 갱신할 [JobExecution]
     * @param status 최종 상태 (`COMPLETED`, `COMPLETED_WITH_SKIPS`, `FAILED`, `STOPPED` 중 하나)
     */
    override suspend fun completeJobExecution(execution: JobExecution, status: BatchStatus) {
        val updated = execution.copy(status = status, endTime = Instant.now())
        jobExecutions[execution.id] = updated
        log.debug { "JobExecution 완료: id=${execution.id}, status=$status" }
    }

    /**
     * jobExecution + stepName 의 [StepExecution]을 조회하거나 신규 생성한다.
     *
     * `COMPLETED`/`COMPLETED_WITH_SKIPS` 상태의 기존 실행은 UPDATE 없이 그대로 반환하며,
     * runner가 해당 상태를 감지하여 즉시 skip 처리한다.
     *
     * @param jobExecution 소속 [JobExecution]
     * @param stepName Step 이름 (blank 불가)
     * @return 기존 또는 신규 [StepExecution]
     */
    override suspend fun findOrCreateStepExecution(
        jobExecution: JobExecution,
        stepName: String,
    ): StepExecution {
        stepName.requireNotBlank("stepName")

        val existing = stepExecutions.values.firstOrNull { se ->
            se.jobExecutionId == jobExecution.id && se.stepName == stepName
        }

        if (existing != null) {
            return when (existing.status) {
                // 완료 상태 — 변경 없이 반환. BatchStepRunner가 즉시 skip 처리
                BatchStatus.COMPLETED,
                BatchStatus.COMPLETED_WITH_SKIPS -> {
                    log.debug { "StepExecution skip (이미 완료): stepName=$stepName, status=${existing.status}" }
                    existing
                }

                // 재시작 대상 — RUNNING으로 복원 후 반환
                BatchStatus.FAILED,
                BatchStatus.STOPPED,
                BatchStatus.RUNNING -> {
                    log.debug { "StepExecution 재시작: stepName=$stepName, 이전 status=${existing.status}" }
                    val updated = existing.copy(status = BatchStatus.RUNNING)
                    stepExecutions[updated.id] = updated
                    updated
                }

                // 그 외 예상치 못한 상태 — 변경 없이 반환
                else -> {
                    log.warn { "StepExecution 예상치 못한 상태: stepName=$stepName, status=${existing.status}" }
                    existing
                }
            }
        }

        val newId = idCounter.incrementAndGet()
        val newExecution = StepExecution(
            id = newId,
            jobExecutionId = jobExecution.id,
            stepName = stepName,
            status = BatchStatus.RUNNING,
            startTime = Instant.now(),
        )
        stepExecutions[newId] = newExecution
        log.debug { "신규 StepExecution 생성: stepName=$stepName, id=$newId" }
        return newExecution
    }

    /**
     * [StepExecution]을 완료 상태로 갱신한다.
     *
     * [StepReport]의 통계(readCount, writeCount, skipCount, checkpoint)를 반영하고
     * 종료 시각을 기록한다.
     *
     * @param execution 갱신할 [StepExecution]
     * @param report Step 실행 결과 보고서
     */
    override suspend fun completeStepExecution(execution: StepExecution, report: StepReport) {
        val updated = execution.copy(
            status = report.status,
            readCount = report.readCount,
            writeCount = report.writeCount,
            skipCount = report.skipCount,
            checkpoint = report.checkpoint,
            endTime = Instant.now(),
        )
        stepExecutions[execution.id] = updated
        log.debug {
            "StepExecution 완료: id=${execution.id}, stepName=${execution.stepName}, " +
                "status=${report.status}, read=${report.readCount}, write=${report.writeCount}, skip=${report.skipCount}"
        }
    }

    /**
     * 체크포인트를 저장한다.
     *
     * 재시작 시 [loadCheckpoint]로 복원하여 중단 지점부터 재개할 수 있다.
     *
     * @param stepExecutionId 대상 [StepExecution] ID
     * @param checkpoint 저장할 체크포인트 값
     */
    override suspend fun saveCheckpoint(stepExecutionId: Long, checkpoint: Any) {
        checkpoints[stepExecutionId] = checkpoint
        log.debug { "체크포인트 저장: stepExecutionId=$stepExecutionId, checkpoint=$checkpoint" }
    }

    /**
     * 저장된 체크포인트를 조회한다.
     *
     * @param stepExecutionId 대상 [StepExecution] ID
     * @return 저장된 체크포인트 값, 없으면 null
     */
    override suspend fun loadCheckpoint(stepExecutionId: Long): Any? =
        checkpoints[stepExecutionId].also { checkpoint ->
            log.debug { "체크포인트 조회: stepExecutionId=$stepExecutionId, checkpoint=$checkpoint" }
        }
}
