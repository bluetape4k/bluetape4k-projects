package io.bluetape4k.workflow.api

/**
 * 병렬 워크플로 실행 정책.
 *
 * [java.util.concurrent.StructuredTaskScope] 의 두 가지 정책에 대응합니다:
 * - [ALL]: ShutdownOnFailure — 모든 작업이 완료되어야 하며, 하나라도 실패 시 나머지 취소
 * - [ANY]: ShutdownOnSuccess — 첫 번째 성공한 작업 결과를 반환하고 나머지 취소
 *
 * ```kotlin
 * val flow = ParallelWorkFlow(
 *     works = listOf(work1, work2),
 *     policy = ParallelPolicy.ALL,   // 모두 완료 대기
 * )
 *
 * val raceFlow = ParallelWorkFlow(
 *     works = listOf(work1, work2),
 *     policy = ParallelPolicy.ANY,   // 첫 성공 즉시 반환
 * )
 * ```
 */
enum class ParallelPolicy {
    /** 모든 작업이 완료되어야 함. 하나라도 실패 시 나머지 취소. while의 `&&` 조건. */
    ALL,

    /** 첫 번째 성공한 작업 결과를 반환하고 나머지 취소. while의 `||` 조건. */
    ANY,
}
