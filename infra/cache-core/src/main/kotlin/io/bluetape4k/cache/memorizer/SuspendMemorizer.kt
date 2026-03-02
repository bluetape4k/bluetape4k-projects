package io.bluetape4k.cache.memorizer

/**
 * suspend 함수 결과를 입력 키 기준으로 메모이제이션하는 인터페이스입니다.
 *
 * ## 동작/계약
 * - 동일 입력에 대한 suspend 계산 결과를 재사용합니다.
 * - 동시 호출 병합/경쟁 조건 처리 방식은 구현체에 따릅니다.
 * - [clear] 호출 시 저장된 캐시 엔트리를 제거해야 합니다.
 *
 * ```kotlin
 * val memo: SuspendMemorizer<String, Int> = suspendMemorizer { it.length }
 * val size = memo("abcd")
 * // size == 4
 * ```
 */
interface SuspendMemorizer<in T: Any, out R: Any>: suspend (T) -> R {
    /** 저장된 모든 캐시 엔트리를 제거합니다. */
    suspend fun clear()
}
