package io.bluetape4k.cache.memoizer.cache2k

import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.exceptions.BluetapeException
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import org.cache2k.Cache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Cache2k Cache를 이용하여 [AsyncMemoizer]를 생성합니다.
 */
fun <T: Any, R: Any> Cache<T, R>.asyncMemoizer(
    @BuilderInference asyncEvaluator: (T) -> CompletableFuture<R>,
): AsyncMemoizer<T, R> =
    Cache2kAsyncMemoizer(this, asyncEvaluator)

/**
 * Cache2k Cache를 이용하여 [AsyncMemoizer]를 생성합니다.
 */
fun <T: Any, R: Any> ((T) -> CompletableFuture<R>).withMemoizer(cache: Cache<T, R>): AsyncMemoizer<T, R> =
    Cache2kAsyncMemoizer(cache, this)

/**
 * Cache2k Cache를 이용하여 메소드의 실행 결과를 캐시하여, 재 실행 시에 빠르게 응닫할 수 있도록 합니다.
 */
class Cache2kAsyncMemoizer<T: Any, R: Any>(
    private val cache: Cache<T, R>,
    @BuilderInference private val asyncEvaluator: (T) -> CompletableFuture<R>,
): AsyncMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val lock = ReentrantLock()

    override fun invoke(input: T): CompletableFuture<R> {
        val promise = CompletableFuture<R>()

        if (cache.containsKey(input)) {
            promise.complete(cache[input])
        } else {
            asyncEvaluator(input)
                .whenComplete { value, error ->
                    if (value == null || error != null) {
                        promise.completeExceptionally(
                            error ?: BluetapeException("asyncEvaluator returns null. input=$input")
                        )
                        log.warn(error) { "Fail to run `asyncEvaluator` by input=$input" }
                    } else {
                        cache.put(input, value)
                        promise.complete(value)
                        log.debug { "Success to run `asyncEvaluator`. input=$input, result=$value" }
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
