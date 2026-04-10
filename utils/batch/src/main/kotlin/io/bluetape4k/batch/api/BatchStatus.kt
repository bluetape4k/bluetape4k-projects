package io.bluetape4k.batch.api

/**
 * 배치 Job/Step 실행 상태.
 *
 * ## 상태 전이
 * ```
 * STARTING → RUNNING → COMPLETED
 *                    → COMPLETED_WITH_SKIPS
 *                    → FAILED
 *                    → STOPPED   (취소)
 * ```
 *
 * **주의**: COMPLETED / COMPLETED_WITH_SKIPS 상태의 StepExecution은
 * `findOrCreateStepExecution`에서 UPDATE 없이 그대로 반환되어 runner가 skip 처리한다.
 *
 * ```kotlin
 * when (stepExecution.status) {
 *     BatchStatus.COMPLETED,
 *     BatchStatus.COMPLETED_WITH_SKIPS -> skipStep()
 *     BatchStatus.FAILED               -> retryStep()
 *     else                             -> runStep()
 * }
 * ```
 */
enum class BatchStatus {
    /** 시작 중 */
    STARTING,

    /** 실행 중 */
    RUNNING,

    /** 성공적으로 완료됨 (skip 없음) */
    COMPLETED,

    /** 완료됐으나 일부 아이템이 skip됨 */
    COMPLETED_WITH_SKIPS,

    /** 실행 실패 */
    FAILED,

    /** 중단됨 (취소) */
    STOPPED;

    /**
     * 최종 상태 여부.
     *
     * `true`이면 재시작 없이 종료된 상태이다.
     * `false`이면 STARTING/RUNNING 등 진행 중 상태이다.
     */
    val isTerminal: Boolean
        get() = this in setOf(COMPLETED, COMPLETED_WITH_SKIPS, FAILED, STOPPED)
}
