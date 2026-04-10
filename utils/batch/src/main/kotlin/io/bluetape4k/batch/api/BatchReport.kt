package io.bluetape4k.batch.api

/**
 * Job 실행 전체 결과 보고서.
 *
 * ## 타입별 의미
 * - [Success]: 모든 Step 성공, skipCount = 0
 * - [PartiallyCompleted]: 모든 Step 완료, 일부 skip 발생 (workflow에서는 success로 처리)
 * - [Failure]: 하나 이상의 Step FAILED
 *
 * ```kotlin
 * val report = jobRunner.run(job)
 * when (report) {
 *     is BatchReport.Success           -> println("Job 완료: ${report.stepReports.size}개 Step")
 *     is BatchReport.PartiallyCompleted -> println("부분 완료: skip 발생")
 *     is BatchReport.Failure           -> println("Job 실패: ${report.error.message}")
 * }
 * ```
 */
sealed interface BatchReport {
    /** Job 실행 컨텍스트 */
    val jobExecution: JobExecution

    /** 각 Step 실행 결과 목록 */
    val stepReports: List<StepReport>

    /**
     * 모든 Step이 성공하고 skip이 없는 경우의 결과.
     *
     * @property jobExecution Job 실행 컨텍스트
     * @property stepReports Step 실행 결과 목록
     */
    data class Success(
        override val jobExecution: JobExecution,
        override val stepReports: List<StepReport>,
    ) : BatchReport

    /**
     * 모든 Step이 완료됐으나 일부 아이템이 skip된 경우의 결과.
     * workflow에서는 success로 처리한다.
     *
     * @property jobExecution Job 실행 컨텍스트
     * @property stepReports Step 실행 결과 목록
     */
    data class PartiallyCompleted(
        override val jobExecution: JobExecution,
        override val stepReports: List<StepReport>,
    ) : BatchReport

    /**
     * 하나 이상의 Step이 FAILED 상태로 종료된 경우의 결과.
     *
     * @property jobExecution Job 실행 컨텍스트
     * @property stepReports Step 실행 결과 목록
     * @property error Job 실패 원인 예외
     */
    data class Failure(
        override val jobExecution: JobExecution,
        override val stepReports: List<StepReport>,
        val error: Throwable,
    ) : BatchReport
}
