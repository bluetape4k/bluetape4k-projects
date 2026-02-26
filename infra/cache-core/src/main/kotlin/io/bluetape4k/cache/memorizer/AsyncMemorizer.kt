package io.bluetape4k.cache.memorizer

import java.util.concurrent.CompletableFuture

/**
 * 비동기 함수 실행 결과를 메모이제이션하는 인터페이스입니다.
 *
 * [CompletableFuture]를 반환하는 비동기 함수의 결과를 캐싱하여,
 * 동일한 입력값에 대해 중복 실행을 방지하고 성능을 향상시킵니다.
 *
 * ```kotlin
 * // 비동기 데이터 조회 함수를 Memorizer로 감싸기
 * val fetchData = { id: String ->
 *     CompletableFuture.supplyAsync { apiClient.fetchUser(id) }
 * }.asyncMemorizer()
 *
 * // 첫 실행 시 API 호출
 * val future1 = fetchData("user-123")
 *
 * // 동일한 키로 호출 시 캐시된 Future 반환
 * val future2 = fetchData("user-123")  // 같은 Future 인스턴스 반환
 * ```
 *
 * @param T 입력값(캐시 키)의 타입
 * @param R 반환값(캐시 값)의 타입
 *
 * @see AsyncInMemoryMemorizer
 * @see AsyncCaffeineMemorizer
 */
interface AsyncMemorizer<in T: Any, R: Any>: (T) -> CompletableFuture<R> {
    /**
     * 저장된 모든 캐시 엔트리를 제거합니다.
     */
    fun clear()
}
