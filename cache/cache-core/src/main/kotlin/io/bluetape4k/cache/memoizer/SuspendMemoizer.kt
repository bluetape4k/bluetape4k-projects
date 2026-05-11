package io.bluetape4k.cache.memoizer

/**
 * suspend 함수 결과를 입력 키 기준으로 메모이제이션하는 인터페이스입니다.
 *
 * ## 동작/계약
 * - 동일 입력에 대한 suspend 계산 결과를 재사용합니다.
 * - 동시 호출 병합/경쟁 조건 처리 방식은 구현체에 따릅니다.
 * - evaluator 실패 또는 코루틴 취소는 성공 결과처럼 캐시하면 안 됩니다.
 *   다음 호출은 같은 실패를 재사용하지 않고 새 계산을 시도해야 합니다.
 * - [clear] 호출 시 저장된 캐시 엔트리를 제거해야 합니다.
 *
 * ```kotlin
 * val memo: SuspendMemoizer<String, Int> = suspendMemoizer { it.length }
 * val size = memo("abcd")
 * // size == 4
 *
 * // 일시적 실패는 in-flight 항목에 고정되지 않아야 합니다.
 * // 이후 같은 키 호출은 새 계산으로 복구할 수 있어야 합니다.
 * ```
 */
interface SuspendMemoizer<in T: Any, out R: Any>: suspend (T) -> R {
    /** 저장된 모든 캐시 엔트리를 제거합니다. */
    suspend fun clear()
}
