package io.bluetape4k.batch.core

import io.bluetape4k.batch.api.BatchJobRepository
import io.bluetape4k.batch.api.BatchReport
import io.bluetape4k.batch.api.BatchStatus
import io.bluetape4k.batch.api.StepReport
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.warn
import io.bluetape4k.support.requireNotBlank
import io.bluetape4k.support.requireNotEmpty
import io.bluetape4k.workflow.api.SuspendWork
import io.bluetape4k.workflow.api.WorkContext
import io.bluetape4k.workflow.api.WorkReport
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * 배치 Job 실행기. 여러 [BatchStep]을 순차적으로 실행하며 재시작을 지원합니다.
 *
 * ## 재시작 계약
 * - `RUNNING/FAILED/STOPPED` 상태의 기존 JobExecution을 재사용합니다.
 * - 이미 `COMPLETED/COMPLETED_WITH_SKIPS`인 StepExecution은 내부적으로 skip됩니다.
 *
 * ## Workflow 통합
 * [SuspendWork] 구현으로 Workflow DSL 안에 임베딩할 수 있습니다.
 * `PartiallyCompleted`는 skip이 의도된 동작이므로 [WorkReport.success]로 매핑합니다.
 *
 * ```kotlin
 * val job = batchJob("importOrders") {
 *     repository(myRepository)
 *     params("date" to "2026-04-10")
 *     step<Order, OrderEntity>("loadStep") {
 *         reader(orderReader)
 *         writer(orderWriter)
 *         chunkSize(500)
 *     }
 * }
 * val report = job.run()
 * ```
 */
class BatchJob(
    val name: String,
    val params: Map<String, Any> = emptyMap(),
    val steps: List<BatchStep<*, *>>,
    val repository: BatchJobRepository,
) : SuspendWork {

    companion object : KLoggingChannel()

    init {
        name.requireNotBlank("name")
        steps.requireNotEmpty("steps")
    }

    /**
     * Job을 실행하고 [BatchReport]를 반환합니다.
     *
     * ## 동작
     * 1. [BatchJobRepository.findOrCreateJobExecution]으로 JobExecution 생성/재사용
     * 2. 각 Step을 [BatchStepRunner]로 실행 — COMPLETED Step은 내부적으로 skip
     * 3. Step FAILED → [BatchReport.Failure] 반환 (Job 전체 중단)
     * 4. 모든 Step 성공 → skip 여부에 따라 [BatchReport.Success] 또는 [BatchReport.PartiallyCompleted] 반환
     *
     * ## 취소 처리
     * - 외부 코루틴 취소([CancellationException]) → STOPPED 영속화 후 **반드시 재던짐**
     * - 치명적 예외([Throwable]) → FAILED 영속화 후 [BatchReport.Failure] 반환
     *
     * @return [BatchReport.Success], [BatchReport.PartiallyCompleted], 또는 [BatchReport.Failure]
     */
    suspend fun run(): BatchReport {
        val jobExecution = repository.findOrCreateJobExecution(name, params)
        val stepReports = mutableListOf<StepReport>()

        try {
            for (step in steps) {
                @Suppress("UNCHECKED_CAST")
                val runner = BatchStepRunner(
                    step = step as BatchStep<Any, Any>,
                    jobExecution = jobExecution,
                    repository = repository,
                )
                val report = runner.run()
                stepReports += report

                if (report.status == BatchStatus.FAILED) {
                    throw report.error
                        ?: IllegalStateException("Step '${step.name}' FAILED without error")
                }
            }

            val hasSkips = stepReports.any { it.skipCount > 0 }
            val finalStatus = if (hasSkips) BatchStatus.COMPLETED_WITH_SKIPS else BatchStatus.COMPLETED
            repository.completeJobExecution(jobExecution, finalStatus)

            return if (hasSkips) {
                BatchReport.PartiallyCompleted(
                    jobExecution.copy(status = BatchStatus.COMPLETED_WITH_SKIPS),
                    stepReports,
                )
            } else {
                BatchReport.Success(
                    jobExecution.copy(status = BatchStatus.COMPLETED),
                    stepReports,
                )
            }

        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                runCatching { repository.completeJobExecution(jobExecution, BatchStatus.STOPPED) }
                    .onFailure { log.warn(it) { "STOPPED 상태 저장 실패 — job=$name" } }
            }
            throw e

        } catch (e: Throwable) {
            withContext(NonCancellable) {
                runCatching { repository.completeJobExecution(jobExecution, BatchStatus.FAILED) }
                    .onFailure { log.warn(it) { "FAILED 상태 저장 실패 — job=$name" } }
            }
            return BatchReport.Failure(
                jobExecution.copy(status = BatchStatus.FAILED),
                stepReports,
                e,
            )
        }
    }

    /**
     * [SuspendWork] 구현 — Workflow DSL 안에 [BatchJob]을 임베딩합니다.
     *
     * 매핑 규칙:
     * - [BatchReport.Success]            → [WorkReport.success]
     * - [BatchReport.PartiallyCompleted] → [WorkReport.success] + `context["batch.{name}.skipCount"]`
     * - [BatchReport.Failure]            → [WorkReport.failure]
     * - 외부 취소([CancellationException]) → 재던짐 (Workflow 전체 취소 전파)
     *
     * @param context 워크플로 실행 컨텍스트
     * @return [WorkReport]
     */
    override suspend fun execute(context: WorkContext): WorkReport {
        context["batch.${name}.startTime"] = Instant.now()
        return try {
            when (val report = run()) {
                is BatchReport.Success -> {
                    context["batch.${name}.report"] = report
                    WorkReport.success(context)
                }
                is BatchReport.PartiallyCompleted -> {
                    context["batch.${name}.skipCount"] = report.stepReports.sumOf { it.skipCount }
                    context["batch.${name}.report"] = report
                    WorkReport.success(context)
                }
                is BatchReport.Failure -> {
                    WorkReport.failure(context, report.error)
                }
            }
        } catch (e: CancellationException) {
            throw e
        }
    }
}
