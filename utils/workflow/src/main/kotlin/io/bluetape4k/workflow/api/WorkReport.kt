package io.bluetape4k.workflow.api

/**
 * 작업 실행 결과를 표현하는 sealed 인터페이스입니다.
 *
 * 실행 흐름 제어는 while 루프에 비유할 수 있습니다:
 * - [Failure] + [ErrorStrategy.CONTINUE] → continue (다음 작업 계속 실행)
 * - [Failure] + [ErrorStrategy.STOP]     → return (중단 후 반환)
 * - [Aborted]                            → break ([ErrorStrategy] 무관, 즉시 전체 워크플로 중단)
 * - [Cancelled]                          → 외부 강제 중단 (timeout, 코루틴 취소)
 *
 * ```kotlin
 * val report = work.execute(context)
 * when (report) {
 *     is WorkReport.Success        -> println("완료: ${report.context}")
 *     is WorkReport.Failure        -> println("실패: ${report.error?.message}")
 *     is WorkReport.PartialSuccess -> println("부분 성공: ${report.failedReports.size}개 실패")
 *     is WorkReport.Aborted        -> println("내부 중단: ${report.reason}")
 *     is WorkReport.Cancelled      -> println("취소: ${report.reason}")
 * }
 * ```
 */
sealed interface WorkReport {

    /** 실행 상태 */
    val status: WorkStatus

    /** 실행 컨텍스트 */
    val context: WorkContext

    /** 실행 중 발생한 에러 (없으면 null) */
    val error: Throwable?

    /** 성공 여부 편의 프로퍼티 */
    val isSuccess: Boolean get() = this is Success

    /** 실패 여부 편의 프로퍼티 */
    val isFailure: Boolean get() = this is Failure

    /**
     * 내부 중단 여부 편의 프로퍼티.
     * [Aborted]이면 true — [ErrorStrategy]와 무관하게 전체 워크플로가 즉시 중단됩니다.
     */
    val isAborted: Boolean get() = this is Aborted

    /** 취소 여부 편의 프로퍼티 */
    val isCancelled: Boolean get() = this is Cancelled

    /**
     * 성공 결과입니다.
     *
     * @property context 실행 컨텍스트
     */
    data class Success(
        override val context: WorkContext,
    ) : WorkReport {
        override val status: WorkStatus = WorkStatus.COMPLETED
        override val error: Throwable? = null
    }

    /**
     * 실패 결과입니다.
     *
     * @property context 실행 컨텍스트
     * @property error 실행 중 발생한 에러 (없으면 null)
     */
    data class Failure(
        override val context: WorkContext,
        override val error: Throwable? = null,
    ) : WorkReport {
        override val status: WorkStatus = WorkStatus.FAILED
    }

    /**
     * CONTINUE 전략으로 실행 완료됐으나 하나 이상의 Work가 실패한 경우입니다.
     *
     * [failedReports]에 실패한 각 Work의 [WorkReport]가 누적됩니다.
     * 마지막 Work가 성공했더라도, 중간 실패가 있으면 이 타입으로 반환됩니다.
     *
     * ```kotlin
     * val report = sequentialFlow.execute(context)
     * if (report is WorkReport.PartialSuccess) {
     *     report.failedReports.forEach { println("실패: ${it.error?.message}") }
     * }
     * ```
     *
     * @property context 실행 컨텍스트
     * @property failedReports 실패한 작업들의 보고서 목록
     */
    data class PartialSuccess(
        override val context: WorkContext,
        val failedReports: List<WorkReport>,
    ) : WorkReport {
        override val status: WorkStatus = WorkStatus.PARTIAL
        override val error: Throwable? = failedReports.firstOrNull()?.error
    }

    /**
     * Work가 내부적으로 전체 워크플로 중단을 결정한 결과입니다.
     *
     * [ErrorStrategy]와 무관하게 해당 워크플로 전체가 즉시 중단됩니다.
     * while 루프의 `break`에 해당합니다.
     *
     * ```kotlin
     * class EarlyExitWork : Work {
     *     override fun execute(context: WorkContext): WorkReport {
     *         if (context.get<Boolean>("abort") == true) {
     *             return WorkReport.aborted(context, "abort flag detected")
     *         }
     *         return WorkReport.success(context)
     *     }
     * }
     * ```
     *
     * @property context 실행 컨텍스트
     * @property reason 중단 사유 (선택)
     */
    data class Aborted(
        override val context: WorkContext,
        val reason: String? = null,
    ) : WorkReport {
        override val status: WorkStatus = WorkStatus.ABORTED
        override val error: Throwable? = null
    }

    /**
     * 취소된 결과입니다. 명시적 취소 또는 timeout 초과 시 반환됩니다.
     *
     * ```kotlin
     * val report = parallelFlow.execute(context)
     * if (report is WorkReport.Cancelled) {
     *     println("취소 사유: ${report.reason}")
     * }
     * ```
     *
     * @property context 실행 컨텍스트
     * @property reason 취소 사유 (선택)
     */
    data class Cancelled(
        override val context: WorkContext,
        val reason: String? = null,
    ) : WorkReport {
        override val status: WorkStatus = WorkStatus.CANCELLED
        override val error: Throwable? = null
    }

    companion object {
        /**
         * 성공 결과를 생성합니다.
         *
         * @param context 실행 컨텍스트
         * @return [Success] 인스턴스
         */
        fun success(context: WorkContext): Success = Success(context)

        /**
         * 실패 결과를 생성합니다.
         *
         * @param context 실행 컨텍스트
         * @param error 발생한 에러 (선택)
         * @return [Failure] 인스턴스
         */
        fun failure(context: WorkContext, error: Throwable? = null): Failure =
            Failure(context, error)

        /**
         * 부분 성공 결과를 생성합니다.
         *
         * @param context 실행 컨텍스트
         * @param failedReports 실패한 작업들의 보고서 목록
         * @return [PartialSuccess] 인스턴스
         */
        fun partialSuccess(context: WorkContext, failedReports: List<WorkReport>): PartialSuccess =
            PartialSuccess(context, failedReports)

        /**
         * 내부 중단 결과를 생성합니다.
         *
         * [ErrorStrategy]와 무관하게 전체 워크플로를 즉시 중단합니다.
         *
         * @param context 실행 컨텍스트
         * @param reason 중단 사유 (선택)
         * @return [Aborted] 인스턴스
         */
        fun aborted(context: WorkContext, reason: String? = null): Aborted =
            Aborted(context, reason)

        /**
         * 취소 결과를 생성합니다.
         *
         * @param context 실행 컨텍스트
         * @param reason 취소 사유 (선택)
         * @return [Cancelled] 인스턴스
         */
        fun cancelled(context: WorkContext, reason: String? = null): Cancelled =
            Cancelled(context, reason)
    }
}
