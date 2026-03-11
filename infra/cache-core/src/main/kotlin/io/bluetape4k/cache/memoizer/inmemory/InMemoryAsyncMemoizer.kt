package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * InMemory를 이용하여 [InMemoryAsyncMemoizer]를 생성합니다.
 */
fun <T: Any, R: Any> ((T) -> CompletableFuture<R>).asyncMemoizer(): InMemoryAsyncMemoizer<T, R> =
    InMemoryAsyncMemoizer(this)

/**
 * 로컬 메모리에 [evaluator] 실행 결과를 저장하는 [AsyncMemoizer] 구현체.
 *
 * ## Virtual Thread 안전성
 * `putIfAbsent` 기반 in-flight 추적을 사용하여 `ConcurrentHashMap.computeIfAbsent`의
 * bin lock 장기 보유 문제를 피합니다. Carrier Thread 고정(pinning) 없이
 * Virtual Thread 환경에서도 안전하게 동작합니다.
 *
 * ## 동작 방식
 * - 결과 캐시 hit → 즉시 완료된 Future 반환
 * - in-flight 있음 → 진행 중인 Future 공유 (중복 평가 방지)
 * - 신규 평가 → lock 밖에서 evaluator 실행
 */
class InMemoryAsyncMemoizer<in T: Any, R: Any>(
    @BuilderInference private val evaluator: (T) -> CompletableFuture<R>,
): AsyncMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val resultCache = ConcurrentHashMap<@UnsafeVariance T, R>()
    private val inFlight = ConcurrentHashMap<@UnsafeVariance T, CompletableFuture<R>>()

    override fun invoke(input: T): CompletableFuture<R> {
        resultCache[input]?.let { return CompletableFuture.completedFuture(it) }

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
                            resultCache[input] = result
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
        resultCache.clear()
    }
}
