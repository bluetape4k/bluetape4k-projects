package io.bluetape4k.cache.memoizer.jcache

import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * JCache를 이용하는 [AsyncJCacheMemoizer]를 생성합니다.
 */
fun <T: Any, R: Any> javax.cache.Cache<T, R>.asyncMemoizer(
    evaluator: (T) -> CompletableFuture<R>,
): AsyncJCacheMemoizer<T, R> =
    AsyncJCacheMemoizer(this, evaluator)

/**
 * JCache를 이용하여 메소드의 실행 결과를 캐시하여, 재 실행 시에 빠르게 응닫할 수 있도록 합니다.
 */
class AsyncJCacheMemoizer<in T: Any, R: Any>(
    private val jcache: javax.cache.Cache<T, R>,
    private val evaluator: (T) -> CompletableFuture<R>,
): AsyncMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val lock = ReentrantLock()

    override fun invoke(input: T): CompletableFuture<R> {
        val promise = CompletableFuture<R>()
        try {
            val value = jcache.get(input)
            if (value != null) {
                promise.complete(value)
            } else {
                evaluator(input)
                    .whenComplete { result, error ->
                        if (error != null)
                            promise.completeExceptionally(error)
                        else {
                            jcache.put(input, result)
                            promise.complete(result)
                        }
                    }
            }
        } catch (e: Throwable) {
            promise.completeExceptionally(e)
        }

        return promise
    }

    override fun clear() {
        lock.withLock {
            jcache.clear()
        }
    }
}
