package io.bluetape4k.cache.memoizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Caffeine Cache를 이용하는 [CaffeineAsyncMemoizer]를 생성합니다.
 *
 * @param T cache key type
 * @param R cache value type
 * @param evaluator cache value를 반환하는 메소드
 */
fun <T : Any, R : Any> Cache<T, R>.asyncMemoizer(
    @BuilderInference evaluator: (T) -> CompletableFuture<R>,
): CaffeineAsyncMemoizer<T, R> = CaffeineAsyncMemoizer(this, evaluator)

/**
 * 비동기 함수를 Caffeine Cache 기반 [CaffeineAsyncMemoizer]로 감쌉니다.
 */
fun <T : Any, R : Any> ((T) -> CompletableFuture<R>).withAsyncMemoizer(
    cache: Cache<T, R>,
): CaffeineAsyncMemoizer<T, R> = CaffeineAsyncMemoizer(cache, this)

/**
 * Caffeine Cache를 이용하여 메소드의 실행 결과를 캐시하여, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 *
 * ## Virtual Thread 안전성
 * `putIfAbsent` 기반 in-flight 추적을 사용하여 Carrier Thread 고정(pinning) 없이
 * Virtual Thread 환경에서도 안전하게 동작합니다.
 *
 * @property cache 실행한 값을 저장할 Cache
 * @property evaluator 캐시 값을 생성하는 메소드
 */
class CaffeineAsyncMemoizer<T : Any, R : Any>(
    private val cache: Cache<T, R>,
    @BuilderInference private val evaluator: (T) -> CompletableFuture<R>,
) : AsyncMemoizer<T, R> {
    companion object : KLoggingChannel()

    private val inFlight = ConcurrentHashMap<T, CompletableFuture<R>>()

    override fun invoke(input: T): CompletableFuture<R> {
        cache.getIfPresent(input)?.let { return CompletableFuture.completedFuture(it) }

        val promise = CompletableFuture<R>()
        val existing = inFlight.putIfAbsent(input, promise)
        if (existing != null) return existing

        fun completeExceptionally(error: Throwable) {
            inFlight.remove(input)
            promise.completeExceptionally(error)
        }

        runCatching { evaluator(input) }
            .fold(
                onSuccess = { future ->
                    future.whenComplete { result, error ->
                        if (error != null) {
                            completeExceptionally(error)
                        } else {
                            inFlight.remove(input)
                            cache.put(input, result)
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
        cache.invalidateAll()
    }
}
