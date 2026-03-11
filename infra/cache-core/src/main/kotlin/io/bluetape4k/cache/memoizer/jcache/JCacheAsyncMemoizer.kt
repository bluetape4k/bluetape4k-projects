package io.bluetape4k.cache.memoizer.jcache

import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * JCache를 이용하는 [JCacheAsyncMemoizer]를 생성합니다.
 */
fun <T: Any, R: Any> javax.cache.Cache<T, R>.asyncMemoizer(
    evaluator: (T) -> CompletableFuture<R>,
): JCacheAsyncMemoizer<T, R> =
    JCacheAsyncMemoizer(this, evaluator)

/**
 * JCache를 이용하여 메소드의 실행 결과를 캐시하여, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 *
 * ## Virtual Thread 안전성
 * `putIfAbsent` 기반 in-flight 추적을 사용하여 Carrier Thread 고정(pinning) 없이
 * Virtual Thread 환경에서도 안전하게 동작합니다.
 */
class JCacheAsyncMemoizer<in T: Any, R: Any>(
    private val jcache: javax.cache.Cache<@UnsafeVariance T, R>,
    private val evaluator: (T) -> CompletableFuture<R>,
): AsyncMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val inFlight = ConcurrentHashMap<@UnsafeVariance T, CompletableFuture<R>>()

    override fun invoke(input: T): CompletableFuture<R> {
        // 1. 완료된 결과 캐시 hit
        jcache.get(input)?.let { return CompletableFuture.completedFuture(it) }

        // 2. in-flight 확인 또는 신규 등록
        val promise = CompletableFuture<R>()
        val existing = inFlight.putIfAbsent(input, promise)
        if (existing != null) return existing

        // 3. evaluator를 lock 밖에서 실행 (Virtual Thread-safe)
        runCatching { evaluator(input) }
            .fold(
                onSuccess = { future ->
                    future.whenComplete { result, error ->
                        inFlight.remove(input)
                        if (error != null) promise.completeExceptionally(error)
                        else {
                            jcache.put(input, result)
                            promise.complete(result)
                        }
                    }
                },
                onFailure = { error ->
                    inFlight.remove(input)
                    promise.completeExceptionally(error)
                }
            )

        return promise
    }

    override fun clear() {
        inFlight.clear()
        jcache.clear()
    }
}
