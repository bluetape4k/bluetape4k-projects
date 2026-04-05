package io.bluetape4k.workflow.api

/**
 * 워크플로 실행 중 에러 발생 시 동작을 결정하는 전략입니다.
 *
 * ```kotlin
 * val flow = sequentialFlow {
 *     errorStrategy = ErrorStrategy.CONTINUE
 *     execute(work1)
 *     execute(work2)
 * }
 * ```
 */
enum class ErrorStrategy {
    /** 에러 발생 시 즉시 중단하고 Failure를 반환 (기본값) */
    STOP,

    /** 에러를 누적하고 다음 작업을 계속 실행. 실패가 있으면 PartialSuccess 반환 */
    CONTINUE,
}
