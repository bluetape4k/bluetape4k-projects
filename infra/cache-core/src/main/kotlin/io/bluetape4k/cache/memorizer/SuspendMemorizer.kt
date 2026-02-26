package io.bluetape4k.cache.memorizer

/**
 * 코루틴 환경에서 함수 실행 결과를 메모이제이션하는 인터페이스입니다.
 *
 * suspend 함수의 결과를 캐싱하여 동일한 입력값에 대해 중복 실행을 방지합니다.
 *
 * ```kotlin
 * // suspend 함수를 Memorizer로 감싸기
 * val fetchUser = suspendMemorizer { userId: String ->
 *     userRepository.findById(userId)  // DB 조회
 * }
 *
 * // 코루틴에서 사용
 * coroutineScope {
 *     val user1 = async { fetchUser("user-123") }  // DB 조회
 *     val user2 = async { fetchUser("user-123") }  // 캐시에서 조회
 * }
 * ```
 *
 * @param T 입력값(캐시 키)의 타입
 * @param R 반환값(캐시 값)의 타입
 *
 * @see SuspendInMemoryMemorizer
 * @see SuspendCaffeineMemorizer
 */
interface SuspendMemorizer<in T: Any, out R: Any>: suspend (T) -> R {
    /**
     * 저장된 모든 캐시 엔트리를 제거합니다.
     */
    suspend fun clear()
}
