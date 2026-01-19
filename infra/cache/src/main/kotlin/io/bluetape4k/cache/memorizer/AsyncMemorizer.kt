package io.bluetape4k.cache.memorizer

import java.util.concurrent.CompletableFuture

/**
 * 비동기 함수 결과를 기록하여, 재시도 시에는 빠르게 결과를 응답할 수 있도록 해주는 Memorizer 입니다.
 */
interface AsyncMemorizer<in T: Any, R: Any>: (T) -> CompletableFuture<R> {

    /**
     * 저장된 기존 수행 결과를 clear 합니다.
     */
    fun clear()
}
