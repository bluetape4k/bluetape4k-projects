package io.bluetape4k.cache.memoizer.ehcache

import io.bluetape4k.cache.memoizer.AsyncMemoizer
import io.bluetape4k.logging.coroutines.KLoggingChannel
import org.ehcache.Cache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Ehcache를 이용하는 [EhCacheAsyncMemoizer]를 생성합니다.
 *
 * ```kotlin
 * val cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true)
 * val cache = cacheManager.createCache(
 *     "myCache",
 *     CacheConfigurationBuilder.newCacheConfigurationBuilder(
 *         String::class.java, Int::class.javaObjectType,
 *         ResourcePoolsBuilder.heap(1000)
 *     )
 * )
 * val memo = cache.asyncMemoizer { key -> CompletableFuture.completedFuture(key.length) }
 * val result = memo("hello").join()
 * // result == 5
 * ```
 */
fun <T: Any, R: Any> Cache<T, R>.asyncMemoizer(
    evaluator: (T) -> CompletableFuture<R>,
): EhCacheAsyncMemoizer<T, R> = EhCacheAsyncMemoizer(this, evaluator)

/**
 * 비동기 함수를 Ehcache 기반 [EhCacheAsyncMemoizer]로 감쌉니다.
 *
 * ```kotlin
 * val cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true)
 * val cache = cacheManager.createCache(
 *     "myCache",
 *     CacheConfigurationBuilder.newCacheConfigurationBuilder(
 *         String::class.java, Int::class.javaObjectType,
 *         ResourcePoolsBuilder.heap(1000)
 *     )
 * )
 * val memo = ({ key: String -> CompletableFuture.completedFuture(key.length) }).withAsyncMemoizer(cache)
 * val result = memo("hello").join()
 * // result == 5
 * ```
 */
fun <T: Any, R: Any> ((T) -> CompletableFuture<R>).withAsyncMemoizer(
    cache: Cache<T, R>,
): EhCacheAsyncMemoizer<T, R> = EhCacheAsyncMemoizer(cache, this)

/**
 * Memoizes an async function using an EhCache [Cache] so repeated calls return the cached result.
 *
 * ```kotlin
 * val cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true)
 * val cache = cacheManager.createCache(
 *     "myCache",
 *     CacheConfigurationBuilder.newCacheConfigurationBuilder(
 *         String::class.java, Int::class.javaObjectType,
 *         ResourcePoolsBuilder.heap(1000)
 *     )
 * )
 * val memo = EhCacheAsyncMemoizer(cache) { key -> CompletableFuture.completedFuture(key.length) }
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
class EhCacheAsyncMemoizer<T: Any, R: Any>(
    private val cache: Cache<T, R>,
    private val evaluator: (T) -> CompletableFuture<R>,
): AsyncMemoizer<T, R> {
    companion object: KLoggingChannel()

    private val inFlight = ConcurrentHashMap<T, CompletableFuture<R>>()
    private val generation = AtomicLong(0)

    override fun invoke(key: T): CompletableFuture<R> {
        cache.get(key)?.let { return CompletableFuture.completedFuture(it) }

        val capturedGen = generation.get()
        val promise = CompletableFuture<R>()
        val existing = inFlight.putIfAbsent(key, promise)
        if (existing != null) return existing

        fun completeExceptionally(error: Throwable) {
            inFlight.remove(key, promise)
            promise.completeExceptionally(error)
        }

        runCatching { evaluator(key) }
            .fold(
                onSuccess = { future ->
                    future.whenComplete { result, error ->
                        if (error != null) {
                            completeExceptionally(error)
                        } else if (result == null) {
                            completeExceptionally(NullPointerException("evaluator returned null for key $key"))
                        } else {
                            inFlight.remove(key, promise)
                            if (generation.get() == capturedGen) {
                                cache.put(key, result)
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
        cache.clear()
    }
}
