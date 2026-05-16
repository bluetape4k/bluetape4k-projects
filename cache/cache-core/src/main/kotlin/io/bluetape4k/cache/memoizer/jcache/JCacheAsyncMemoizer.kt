package io.bluetape4k.cache.memoizer.jcache

import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * JCache를 이용하는 [JCacheAsyncMemoizer]를 생성합니다.
 *
 * ```kotlin
 * val cachingProvider = Caching.getCachingProvider()
 * val cacheManager = cachingProvider.cacheManager
 * val cache = cacheManager.getCache<String, Int>("myCache")
 * val memo = cache.asyncMemoizer { key -> CompletableFuture.completedFuture(key.length) }
 * val result = memo("hello").join()
 * // result == 5
 * ```
 */
fun <T: Any, R: Any> javax.cache.Cache<T, R>.asyncMemoizer(
    evaluator: (T) -> CompletableFuture<R>,
): JCacheAsyncMemoizer<T, R> =
    JCacheAsyncMemoizer(this, evaluator)

/**
 * Memoizes an async function using a JCache so repeated calls return the cached result.
 *
 * ```kotlin
 * val cachingProvider = Caching.getCachingProvider()
 * val cacheManager = cachingProvider.cacheManager
 * val cache = cacheManager.getCache<String, Int>("myCache")
 * val memo = JCacheAsyncMemoizer(cache) { key -> CompletableFuture.completedFuture(key.length) }
 * val result = memo("hello").join()
 * // result == 5
 * ```
 *
 * ## Thread Safety / Virtual Thread Safety
 * - Uses `putIfAbsent`-based in-flight tracking — no `synchronized` blocks, safe for virtual threads.
 * - A generation counter prevents write-after-clear races: if [clear] is called while an evaluator
 *   is in flight, the stale result is discarded and never written to the cache.
 * - `inFlight` removal uses value-aware two-arg `remove(key, value)` so a freshly installed
 *   promise is not accidentally evicted by a concurrent completion from a previous generation.
 * - The caller's [CompletableFuture] is always completed regardless of the generation check.
 */
class JCacheAsyncMemoizer<in T: Any, R: Any>(
    private val jcache: javax.cache.Cache<@UnsafeVariance T, R>,
    private val evaluator: (T) -> CompletableFuture<R>,
): AsyncMemoizer<T, R> {

    companion object: KLoggingChannel()

    private val inFlight = ConcurrentHashMap<@UnsafeVariance T, CompletableFuture<R>>()
    private val generation = AtomicLong(0)

    override fun invoke(input: T): CompletableFuture<R> {
        jcache.get(input)?.let { return CompletableFuture.completedFuture(it) }

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
                                jcache.put(input, result)
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
        jcache.clear()
    }
}
