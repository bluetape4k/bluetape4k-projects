package io.bluetape4k.cache.memorizer

/**
 * 함수 실행 결과를 메모이제이션(Memoization)하여 캐싱하는 인터페이스입니다.
 *
 * 동일한 입력값에 대해 함수를 재실행하지 않고 캐시된 결과를 반환하여 성능을 향상시킵니다.
 *
 * ```kotlin
 * // 팩토리얼 함수를 Memorizer로 감싸기
 * val factorial = { n: Int ->
 *     if (n <= 1) 1 else n * factorial(n - 1)
 * }.memorizer()
 *
 * // 첫 실행 시 계산
 * println(factorial(5))  // 120 (계산됨)
 *
 * // 캐시된 값 반환
 * println(factorial(5))  // 120 (캐시에서 조회)
 * ```
 *
 * @param T 입력값(캐시 키)의 타입
 * @param R 반환값(캐시 값)의 타입
 *
 * @see InMemoryMemorizer
 * @see CaffeineMemorizer
 */
interface Memorizer<in T: Any, out R: Any>: (T) -> R {
    /**
     * 저장된 모든 캐시 엔트리를 제거합니다.
     */
    fun clear()
}
