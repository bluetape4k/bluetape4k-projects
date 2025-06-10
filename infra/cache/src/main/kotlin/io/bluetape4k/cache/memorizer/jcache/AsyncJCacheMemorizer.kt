package io.bluetape4k.cache.memorizer.jcache

import io.bluetape4k.cache.memorizer.AsyncMemorizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.atomicfu.locks.ReentrantLock
import java.util.concurrent.CompletableFuture
import javax.cache.Cache
import kotlin.concurrent.withLock

/**
 * JCache를 이용하여 메소드의 실행 결과를 캐시하여 , 재 실행 시에 빠르게 응닫할 수 있도록 합니다.
 *
 * @property jcache 실행한 값을 저장할 Cache
 * @property evaluator 캐시 값을 생성하는 메소드
 */
class AsyncJCacheMemorizer<in T: Any, R: Any>(
    private val jcache: Cache<T, R>,
    private val evaluator: (T) -> CompletableFuture<R>,
): AsyncMemorizer<T, R> {

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

/**
 * JCache를 이용하는 [JCacheAsyncMemorizer]를 생성합니다.
 *
 * @param T cache key type
 * @param R cache value type
 * @param evaluator cache value를 반환하는 메소드
 */
fun <T: Any, R: Any> Cache<T, R>.asyncMemorizer(evaluator: (T) -> CompletableFuture<R>): AsyncJCacheMemorizer<T, R> =
    AsyncJCacheMemorizer(this, evaluator)
