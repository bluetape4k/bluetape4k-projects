package io.bluetape4k.cache.memoizer.cache2k

import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.exceptions.BluetapeException
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import org.cache2k.Cache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

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
 * Cache2k Cache를 이용하여 메소드의 실행 결과를 캐시하여, 재 실행 시에 빠르게 응답할 수 있도록 합니다.
 *
 * ## Virtual Thread 안전성
 * `putIfAbsent` 기반 in-flight 추적을 사용하여 Carrier Thread 고정(pinning) 없이
 * Virtual Thread 환경에서도 안전하게 동작합니다.
 */
class Cache2kAsyncMemoizer<T: Any, R: Any>(
    private val cache: Cache<T, R>,
    @BuilderInference private val asyncEvaluator: (T) -> CompletableFuture<R>,
): AsyncMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val inFlight = ConcurrentHashMap<T, CompletableFuture<R>>()

    override fun invoke(input: T): CompletableFuture<R> {
        // 1. 완료된 결과 캐시 hit
        if (cache.containsKey(input)) {
            return CompletableFuture.completedFuture(cache[input])
        }

        // 2. in-flight 확인 또는 신규 등록
        val promise = CompletableFuture<R>()
        val existing = inFlight.putIfAbsent(input, promise)
        if (existing != null) return existing

        // 3. evaluator를 lock 밖에서 실행 (Virtual Thread-safe)
        runCatching { asyncEvaluator(input) }
            .fold(
                onSuccess = { future ->
                    future.whenComplete { value, error ->
                        inFlight.remove(input)
                        if (value == null || error != null) {
                            val ex = error ?: BluetapeException("asyncEvaluator returns null. input=$input")
                            log.warn(error) { "Fail to run `asyncEvaluator` by input=$input" }
                            promise.completeExceptionally(ex)
                        } else {
                            cache.put(input, value)
                            promise.complete(value)
                            log.debug { "Success to run `asyncEvaluator`. input=$input, result=$value" }
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
        cache.clear()
    }
}
