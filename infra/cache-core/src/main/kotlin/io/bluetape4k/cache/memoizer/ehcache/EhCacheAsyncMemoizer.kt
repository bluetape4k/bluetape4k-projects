package io.bluetape4k.cache.memoizer.ehcache

import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.ehcache.Cache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Ehcache를 이용하는 [EhCacheAsyncMemoizer]를 생성합니다.
 */
fun <T: Any, R: Any> Cache<T, R>.asyncMemoizer(
    @BuilderInference evaluator: (T) -> CompletableFuture<R>,
): EhCacheAsyncMemoizer<T, R> =
    EhCacheAsyncMemoizer(this, evaluator)

fun <T: Any, R: Any> ((T) -> CompletableFuture<R>).withAsyncMemoizer(
    cache: Cache<T, R>,
): EhCacheAsyncMemoizer<T, R> =
    EhCacheAsyncMemoizer(cache, this)

/**
 * Ehcache를 이용하여 메소드의 실행 결과를 캐시하여, 재 실행 시에 빠르게 응닫할 수 있도록 합니다.
 */
class EhCacheAsyncMemoizer<T: Any, R: Any>(
    private val cache: Cache<T, R>,
    @BuilderInference private val evaluator: (T) -> CompletableFuture<R>,
): AsyncMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val lock = ReentrantLock()

    override fun invoke(key: T): CompletableFuture<R> {
        val promise = CompletableFuture<R>()

        val value = cache.get(key)
        if (value != null) {
            promise.complete(value)
        } else {
            evaluator(key)
                .whenComplete { result, error ->
                    if (error != null)
                        promise.completeExceptionally(error)
                    else {
                        cache.put(key, result)
                        promise.complete(result)
                    }
                }
        }

        return promise
    }

    override fun clear() {
        lock.withLock {
            cache.clear()
        }
    }
}
