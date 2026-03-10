package io.bluetape4k.cache.memoizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Caffeine Cache를 이용하는 [CaffeineAsyncMemoizer]를 생성합니다.
 *
 * @param T cache key type
 * @param R cache value type
 * @param evaluator cache value를 반환하는 메소드
 */
fun <T: Any, R: Any> Cache<T, R>.asyncMemoizer(
    @BuilderInference evaluator: (T) -> CompletableFuture<R>,
): CaffeineAsyncMemoizer<T, R> {
    return CaffeineAsyncMemoizer(this, evaluator)
}

fun <T: Any, R: Any> ((T) -> CompletableFuture<R>).withAsyncMemoizer(
    cache: Cache<T, R>,
): CaffeineAsyncMemoizer<T, R> {
    return CaffeineAsyncMemoizer(cache, this)
}

/**
 * Caffeine Cache를 이용하여 메소드의 실행 결과를 캐시하여, 재 실행 시에 빠르게 응닫할 수 있도록 합니다.
 *
 * @property cache 실행한 값을 저장할 Cache
 * @property evaluator 캐시 값을 생성하는 메소드
 */
class CaffeineAsyncMemoizer<T: Any, R: Any>(
    private val cache: Cache<T, R>,
    @BuilderInference private val evaluator: (T) -> CompletableFuture<R>,
): AsyncMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val lock = ReentrantLock()

    override fun invoke(input: T): CompletableFuture<R> {
        val promise = CompletableFuture<R>()

        val value = cache.getIfPresent(input)
        if (value != null) {
            promise.complete(value)
        } else {
            evaluator(input)
                .whenComplete { result, error ->
                    if (error != null)
                        promise.completeExceptionally(error)
                    else {
                        cache.put(input, result)
                        promise.complete(result)
                    }
                }
        }

        return promise
    }

    override fun clear() {
        lock.withLock {
            cache.cleanUp()
        }
    }
}
