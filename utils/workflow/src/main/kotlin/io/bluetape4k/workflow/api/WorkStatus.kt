package io.bluetape4k.workflow.api

/**
 * 워크플로 작업의 실행 상태를 나타내는 열거형입니다.
 *
 * ```kotlin
 * val report = work.execute(context)
 * when (report.status) {
 *     WorkStatus.COMPLETED  -> println("성공")
 *     WorkStatus.FAILED     -> println("실패")
 *     WorkStatus.PARTIAL    -> println("부분 성공")
 *     WorkStatus.ABORTED    -> println("내부 중단")
 *     WorkStatus.CANCELLED  -> println("취소됨")
 * }
 * ```
 */
enum class WorkStatus {
    /** 작업이 성공적으로 완료됨 */
    COMPLETED,

    /** 작업 실행 중 오류 발생 */
    FAILED,

    /** CONTINUE 전략으로 일부 작업 성공, 일부 실패 */
    PARTIAL,

    /**
     * Work가 내부적으로 전체 워크플로 중단을 결정함.
     * [ErrorStrategy]와 무관하게 즉시 중단.
     * while 루프의 break에 해당.
     */
    ABORTED,

    /** 취소된 작업 (명시적 취소 또는 timeout 초과) */
    CANCELLED,
}
