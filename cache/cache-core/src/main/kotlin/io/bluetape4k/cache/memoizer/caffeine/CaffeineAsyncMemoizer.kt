package io.bluetape4k.cache.memoizer.caffeine

import com.github.benmanes.caffeine.cache.Cache
import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 이 Caffeine [Cache]를 backend로 사용하는 [CaffeineAsyncMemoizer]를 생성합니다.
 *
 * ```kotlin
 * val cache = Caffeine.newBuilder().maximumSize(1000).build<String, Int>()
 * val memo = cache.asyncMemoizer { key -> CompletableFuture.completedFuture(key.length) }
 * val result = memo("hello").join()
 * // result == 5
 * ```
 *
 * @param T cache key type
 * @param R cache value type
 * @param evaluator function that produces the cache value asynchronously
 */
fun <T: Any, R: Any> Cache<T, R>.asyncMemoizer(
    evaluator: (T) -> CompletableFuture<R>,
): CaffeineAsyncMemoizer<T, R> = CaffeineAsyncMemoizer(this, evaluator)

/**
 * Wraps an async function with a Caffeine-backed [CaffeineAsyncMemoizer].
 *
 * ```kotlin
 * val cache = Caffeine.newBuilder().maximumSize(1000).build<String, Int>()
 * val memo = ({ key: String -> CompletableFuture.completedFuture(key.length) }).withAsyncMemoizer(cache)
 * val result = memo("hello").join()
 * // result == 5
 * ```
 */
fun <T: Any, R: Any> ((T) -> CompletableFuture<R>).withAsyncMemoizer(
    cache: Cache<T, R>,
): CaffeineAsyncMemoizer<T, R> = CaffeineAsyncMemoizer(cache, this)

/**
 * Memoizes an async function using a Caffeine [Cache] so repeated calls return the cached result.
 *
 * ```kotlin
 * val cache = Caffeine.newBuilder().maximumSize(1000).build<String, Int>()
 * val memo = CaffeineAsyncMemoizer(cache) { key -> CompletableFuture.completedFuture(key.length) }
 * val result = memo("hello").join()
 * // result == 5
 * ```
 *
 * ## Thread Safety / Virtual Thread Safety
 * - Uses `putIfAbsent`-based in-flight tracking — no `synchronized` blocks, safe for virtual threads.
 * - A generation counter prevents write-after-clear races: if [clear] is called while an evaluator
 *   is in flight, the stale result is discarded and never written to the cache.
 * - `inFlight` removal uses a value-aware two-arg `remove(key, value)` so a freshly installed
 *   promise is not accidentally evicted by a concurrent completion from a previous generation.
 * - The caller's [CompletableFuture] is always completed (success or exceptionally) regardless of
 *   the generation check, so no caller ever hangs.
 *
 * @property cache Caffeine cache that stores computed values
 * @property evaluator function that computes the value for a given input asynchronously
 */
class CaffeineAsyncMemoizer<T: Any, R: Any>(
    private val cache: Cache<T, R>,
    private val evaluator: (T) -> CompletableFuture<R>,
): AsyncMemoizer<T, R> {
    companion object: KLoggingChannel()

    private val inFlight = ConcurrentHashMap<T, CompletableFuture<R>>()
    private val generation = AtomicLong(0)

    override fun invoke(input: T): CompletableFuture<R> {
        cache.getIfPresent(input)?.let { return CompletableFuture.completedFuture(it) }

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
                                cache.put(input, result)
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
        cache.invalidateAll()
    }
}
