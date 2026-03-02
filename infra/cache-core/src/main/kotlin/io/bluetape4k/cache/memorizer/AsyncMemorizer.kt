package io.bluetape4k.cache.memorizer

import java.util.concurrent.CompletableFuture

/**
 * `CompletableFuture` 기반 비동기 함수 결과를 메모이제이션하는 인터페이스입니다.
 *
 * ## 동작/계약
 * - 동일 입력에 대해 동일한 비동기 계산 결과를 재사용합니다.
 * - 실패/완료 처리 정책(실패 Future 캐싱 여부)은 구현체에 따릅니다.
 * - [clear] 호출 시 저장된 캐시 엔트리를 제거해야 합니다.
 *
 * ```kotlin
 * val memo: AsyncMemorizer<String, Int> =
 *   ({ key -> CompletableFuture.completedFuture(key.length) }).asyncMemorizer()
 * val size = memo("abcd").join()
 * // size == 4
 * ```
 */
interface AsyncMemorizer<in T: Any, R: Any>: (T) -> CompletableFuture<R> {
    /** 저장된 모든 캐시 엔트리를 제거합니다. */
    fun clear()
}
