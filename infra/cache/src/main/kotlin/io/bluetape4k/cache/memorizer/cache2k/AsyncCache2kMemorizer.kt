package io.bluetape4k.cache.memorizer.cache2k

import io.bluetape4k.cache.memorizer.AsyncMemorizer
import io.bluetape4k.exceptions.BluetapeException
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import kotlinx.atomicfu.locks.ReentrantLock
import org.cache2k.Cache
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.withLock


/**
 * Cache2k Cache를 이용하여 [AsyncMemorizer]를 생성합니다.
 *
 * @param T cache key type
 * @param R cache value type
 * @param asyncEvaluator cache value를 반환하는 메소드
 */
fun <T: Any, R: Any> Cache<T, R>.asyncMemorizer(asyncEvaluator: (T) -> CompletableFuture<R>): AsyncMemorizer<T, R> =
    Cache2kAsyncMemorizer(this, asyncEvaluator)

/**
 * Caffeine Cache를 이용하여 [AsyncMemorizer]를 생성합니다.
 *
 * @param T cache key type
 * @param R cache value type
 * @param cache 실행 결과를 캐싱하는 캐시
 */
fun <T: Any, R: Any> ((T) -> CompletableFuture<R>).withMemorizer(cache: Cache<T, R>): AsyncMemorizer<T, R> =
    Cache2kAsyncMemorizer(cache, this)

/**
 * Cache2k Cache 이용하여 메소드의 실행 결과를 캐시하여 , 재 실행 시에 빠르게 응닫할 수 있도록 합니다.
 *
 * @property cache 실행한 값을 저장할 Cache
 * @property asyncEvaluator 캐시 값을 생성하는 메소드
 *

 */
class Cache2kAsyncMemorizer<T: Any, R: Any>(
    private val cache: Cache<T, R>,
    private val asyncEvaluator: (T) -> CompletableFuture<R>,
): AsyncMemorizer<T, R> {

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
