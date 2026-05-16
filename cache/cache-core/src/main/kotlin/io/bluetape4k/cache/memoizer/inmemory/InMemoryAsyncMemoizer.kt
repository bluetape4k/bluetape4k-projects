package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * InMemory를 이용하여 [InMemoryAsyncMemoizer]를 생성합니다.
 *
 * ```kotlin
 * val memo = ({ key: String -> CompletableFuture.completedFuture(key.length) }).asyncMemoizer()
 * val result = memo("hello").join()
 * // result == 5
 * val result2 = memo("hello").join()  // 캐시에서 즉시 반환
 * // result2 == 5
 * ```
 */
fun <T: Any, R: Any> ((T) -> CompletableFuture<R>).asyncMemoizer(): InMemoryAsyncMemoizer<T, R> =
    InMemoryAsyncMemoizer(this)

/**
 * In-memory [AsyncMemoizer] that stores evaluation results in a local [ConcurrentHashMap].
 *
 * ## Thread Safety / Virtual Thread Safety
 * - Uses `putIfAbsent`-based in-flight tracking — no `synchronized` blocks, safe for virtual threads.
 * - A generation counter prevents write-after-clear races: if [clear] is called while an evaluator
 *   is in flight, the stale result is discarded and never written to the result cache.
 * - `inFlight` removal uses value-aware two-arg `remove(key, value)` so a freshly installed
 *   promise is not accidentally evicted by a concurrent completion from a previous generation.
 * - The caller's [CompletableFuture] is always completed regardless of the generation check.
 *
 * ## Behaviour
 * - Result cache hit → returns an already-completed Future immediately.
 * - In-flight hit → shares the in-progress Future (no duplicate evaluation).
 * - New evaluation → runs the evaluator outside any lock.
 */
class InMemoryAsyncMemoizer<in T: Any, R: Any>(
    private val evaluator: (T) -> CompletableFuture<R>,
): AsyncMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val resultCache = ConcurrentHashMap<@UnsafeVariance T, R>()
    private val inFlight = ConcurrentHashMap<@UnsafeVariance T, CompletableFuture<R>>()
    private val generation = AtomicLong(0)

    override fun invoke(input: T): CompletableFuture<R> {
        resultCache[input]?.let { return CompletableFuture.completedFuture(it) }

        val capturedGen = generation.get()
        val promise = CompletableFuture<R>()
        val existing = inFlight.putIfAbsent(input, promise)
        if (existing != null) return existing

        fun completeExceptionally(error: Throwable) {
            inFlight.remove(input, promise)
            promise.completeExceptionally(error)
        }

        runCatching { evaluator(input) }
            .fold(
                onSuccess = { future ->
                    future.whenComplete { result, error ->
                        if (error != null) {
                            completeExceptionally(error)
                        } else if (result == null) {
                            completeExceptionally(NullPointerException("evaluator returned null for input $input"))
                        } else {
                            inFlight.remove(input, promise)
                            if (generation.get() == capturedGen) {
                                resultCache[input] = result
                            }
                            promise.complete(result)
                        }
                    }
                },
                onFailure = ::completeExceptionally
            )

        return promise
    }

    override fun clear() {
        generation.incrementAndGet()
        inFlight.clear()
        resultCache.clear()
    }
}
