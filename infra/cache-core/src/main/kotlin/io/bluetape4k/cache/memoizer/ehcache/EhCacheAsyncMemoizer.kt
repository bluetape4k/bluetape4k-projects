package io.bluetape4k.cache.memoizer.ehcache

import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.ehcache.Cache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Ehcache를 이용하는 [EhCacheAsyncMemoizer]를 생성합니다.
 */
fun <T : Any, R : Any> Cache<T, R>.asyncMemoizer(
    evaluator: (T) -> CompletableFuture<R>,
): EhCacheAsyncMemoizer<T, R> = EhCacheAsyncMemoizer(this, evaluator)

/**
 * 비동기 함수를 Ehcache 기반 [EhCacheAsyncMemoizer]로 감쌉니다.
 */
fun <T : Any, R : Any> ((T) -> CompletableFuture<R>).withAsyncMemoizer(
    cache: Cache<T, R>,
): EhCacheAsyncMemoizer<T, R> = EhCacheAsyncMemoizer(cache, this)

/**
 * Ehcache를 이용하여 메소드의 실행 결과를 캐시하여, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 *
 * ## Virtual Thread 안전성
 * `putIfAbsent` 기반 in-flight 추적을 사용하여 Carrier Thread 고정(pinning) 없이
 * Virtual Thread 환경에서도 안전하게 동작합니다.
 */
class EhCacheAsyncMemoizer<T : Any, R : Any>(
    private val cache: Cache<T, R>,
    private val evaluator: (T) -> CompletableFuture<R>,
) : AsyncMemoizer<T, R> {
    companion object : KLoggingChannel()

    private val inFlight = ConcurrentHashMap<T, CompletableFuture<R>>()

    override fun invoke(key: T): CompletableFuture<R> {
        cache.get(key)?.let { return CompletableFuture.completedFuture(it) }

        val promise = CompletableFuture<R>()
        val existing = inFlight.putIfAbsent(key, promise)
        if (existing != null) return existing

        fun completeExceptionally(error: Throwable) {
            inFlight.remove(key)
            promise.completeExceptionally(error)
        }

        runCatching { evaluator(key) }
            .fold(
                onSuccess = { future ->
                    future.whenComplete { result, error ->
                        if (error != null) {
                            completeExceptionally(error)
                        } else {
                            inFlight.remove(key)
                            cache.put(key, result)
                            promise.complete(result)
                        }
                    }
                },
                onFailure = ::completeExceptionally
            )

        return promise
    }

    override fun clear() {
        inFlight.clear()
        cache.clear()
    }
}
