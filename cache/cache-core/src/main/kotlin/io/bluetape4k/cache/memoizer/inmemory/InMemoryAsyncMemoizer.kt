package io.bluetape4k.cache.memoizer.inmemory

import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.cache.memoizer.SingleFlight
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * 이 [CompletableFuture] 기반 evaluator에 대한 [InMemoryAsyncMemoizer]를 생성합니다.
 *
 * ```kotlin
 * val memo = ({ key: String -> CompletableFuture.completedFuture(key.length) }).asyncMemoizer()
 * val result = memo("hello").join()
 * // result == 5
 * val result2 = memo("hello").join()  // returned immediately from the cache
 * // result2 == 5
 * ```
 */
fun <T: Any, R: Any> ((T) -> CompletableFuture<R>).asyncMemoizer(): InMemoryAsyncMemoizer<T, R> =
    InMemoryAsyncMemoizer(this)

/**
 * In-memory [AsyncMemoizer] that stores evaluation results in a local [ConcurrentHashMap].
 *
 * ## Thread Safety / Virtual Thread Safety
 * - Uses [SingleFlight] for same-key in-flight tracking with no `synchronized` blocks.
 * - A generation token prevents write-after-clear races: if [clear] is called while an evaluator
 *   is in flight, the stale result is discarded and never written to the result cache.
 * - The caller's [CompletableFuture] is always completed regardless of the generation check.
 * - A Java future that completes with `null` completes exceptionally and is not cached.
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
    private val singleFlight = SingleFlight<@UnsafeVariance T, R>()

    override fun invoke(input: T): CompletableFuture<R> {
        resultCache[input]?.let { return CompletableFuture.completedFuture(it) }

        return singleFlight.runAsync(input) { token ->
            evaluator(input).thenApply { result ->
                if (result == null) {
                    throw NullPointerException("evaluator returned null for input $input")
                }
                if (singleFlight.isCurrent(token)) {
                    resultCache[input] = result
                }
                result
            }
        }
    }

    override fun clear() {
        singleFlight.clear()
        resultCache.clear()
    }
}
