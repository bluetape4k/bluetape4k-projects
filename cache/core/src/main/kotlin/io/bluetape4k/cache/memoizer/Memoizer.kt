package io.bluetape4k.cache.memoizer

/**
 * 동기 함수 결과를 입력 키 기준으로 메모이제이션하는 인터페이스입니다.
 *
 * ## 동작/계약
 * - 동일 입력에 대한 반환값을 재사용해 반복 계산을 줄입니다.
 * - 캐시 저장소 구현(메모리/JCache/Caffeine 등)은 구현체가 결정합니다.
 * - [clear] 호출 시 저장된 캐시 엔트리를 제거해야 합니다.
 *
 * ```kotlin
 * val memo: Memoizer<Int, Int> = { n -> n * n }.memoizer()
 * val a = memo(3)
 * val b = memo(3)
 * // a == 9
 * // b == 9
 * ```
 */
interface Memoizer<in T: Any, out R: Any>: (T) -> R {
    /** 저장된 모든 캐시 엔트리를 제거합니다. */
    fun clear()
}
