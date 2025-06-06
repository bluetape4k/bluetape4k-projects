package io.bluetape4k.cache.caffeine

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.AsyncLoadingCache
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.CaffeineSpec
import com.github.benmanes.caffeine.cache.LoadingCache
import io.bluetape4k.concurrent.virtualthread.VirtualThreadExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor

/**
 * [CaffeineSpec] 을 빌드합니다.
 *
 * ```
 * val spec = caffeineSpecOf("maximumSize=1000,expireAfterWrite=5m")
 * val cache = spec.toBuilder().build<String, Int>()
 *
 * cache.put("hello", 5)
 * val value = cache.getIfPresent("hello")
 * ```
 *
 * @param specification caffign 환경 설정 정보
 * @return [CaffeineSpec] instance
 */
fun caffeineSpecOf(specification: String): CaffeineSpec =
    CaffeineSpec.parse(specification)

/**
 * [Caffeine]`<Any, Any>` 을 빌드합니다.
 *
 * ```
 * val cache = caffeine {
 *    maximumSize(1000)
 *    expireAfterWrite(5, TimeUnit.MINUTES)
 *    recordStats()
 * }
 * ```
 *
 * @param initializer Caffeine 빌더 메소드
 * @return [caffeine] instance
 */
inline fun caffeine(
    @BuilderInference initializer: Caffeine<Any, Any>.() -> Unit,
) =
    Caffeine.newBuilder().apply(initializer)

/**
 * `executor` 가 `VirtualThreadExecutor`인 [Caffeine]`<Any, Any>` 빌드합니다.
 *
 * @param initializer Caffeine builder method
 * @return [caffeine] instance
 */
inline fun caffeineWithVirtualThread(
    @BuilderInference initializer: Caffeine<Any, Any>.() -> Unit,
) =
    Caffeine.newBuilder()
        .executor(VirtualThreadExecutor)
        .apply(initializer)

/**
 * Caffeine Cache인 [Cache]`<K, V>` 를 생성합니다.
 *
 * ```
 * val caffeine = caffeine<Any, Any> {
 *     maximumSize(1000)
 *     expireAfterWrite(5, TimeUnit.MINUTES)
 * }
 * val cache:Cache<String, Int> = caffeine.cache<String, Int>()
 * ```
 *
 * @param K         Cache key type
 * @param V         Cache value type
 * @return  [Cache]`<K, V>` instance
 */
fun <K: Any, V: Any> Caffeine<Any, Any>.cache(): Cache<K, V> = build()

/**
 * Caffeine [LoadingCache]를 빌드합니다.
 *
 * ```
 * val caffeine = caffeine<Any, Any> {
 *    maximumSize(1000)
 *    expireAfterWrite(5, TimeUnit.MINUTES)
 *    recordStats()
 * }
 * val loadingCache: LoadingCache<String, Int> = caffeine.loadingCache { key -> key.length }
 * ```
 *
 * @param K         Cache key type
 * @param V         Cache value type
 * @param loader    Cache item loader
 * @return [AsyncCache] instance
 */
fun <K: Any, V: Any> Caffeine<Any, Any>.loadingCache(
    @BuilderInference loader: (K) -> V,
): LoadingCache<K, V> = build { key: K -> loader(key) }


/**
 * Caffeine [AsyncCache]를 빌드합니다.
 *
 * ```
 * val caffeine = caffeine<Any, Any> {
 *      maximumSize(1000)
 *      expireAfterWrite(5, TimeUnit.MINUTES)
 *      recordStats()
 *      executor(VirtualThreadExecutor)
 * }
 * val asyncCache: AsyncCache<String, Int> = caffeine.asyncCache()
 * asyncCache.put("hello", 5).join()
 * val value = asyncCache.getIfPresent("hello").join()  // 5
 * ```
 *
 * @param K         Cache key type
 * @param V         Cache value type
 * @return [AsyncCache] instance
 */
fun <K: Any, V: Any> Caffeine<Any, Any>.asyncCache(): AsyncCache<K, V> = buildAsync()


/**
 * Caffeine [AsyncLoadingCache]를 빌드합니다.
 *
 * ```
 * val caffeine = caffeine<Any, Any> {
 *    maximumSize(1000)
 *    expireAfterWrite(5, TimeUnit.MINUTES)
 * }
 * val asyncLoadingCache: AsyncLoadingCache<String, Int> =
 *      caffeine.asyncLoadingCache { key ->
 *          CompletableFuture.completedFuture(key.length)
 *      }
 *
 * asyncLoadingCache.put("hello", 5).join()
 * asyncLoadingCache.get("hello").get()  // 5
 * ```
 *
 * @param K         Cache key type
 * @param V         Cache value type
 * @param loader    Cache value loader
 * @return [AsyncLoadingCache] instance
 */
inline fun <K: Any, V: Any> Caffeine<Any, Any>.asyncLoadingCache(
    @BuilderInference crossinline loader: (key: K) -> CompletableFuture<V>,
): AsyncLoadingCache<K, V> = buildAsync { key: K, _: Executor -> loader(key) }

/**
 * Caffeine [AsyncLoadingCache]를 빌드합니다.
 *
 * ```
 * val caffeine = caffeine<Any, Any> {
 *    maximumSize(1000)
 *    expireAfterWrite(5, TimeUnit.MINUTES)
 * }
 * val asyncLoadingCache: AsyncLoadingCache<String, Int> =
 *      caffeine.asyncLoadingCache { key, executor ->
 *          CompletableFuture.asyncSupplier( { ... }, executor)
 *      }
 *
 * asyncLoadingCache.put("hello", 5).join()
 * asyncLoadingCache.get("hello").get()  // 5
 * ```
 *
 * @param K         Cache key type
 * @param V         Cache value type
 * @param loader    Cache value loader
 * @return [AsyncLoadingCache] instance
 */
inline fun <K: Any, V: Any> Caffeine<Any, Any>.asyncLoadingCache(
    @BuilderInference crossinline loader: (key: K, executor: Executor) -> CompletableFuture<V>,
): AsyncLoadingCache<K, V> =
    buildAsync { key: K, executor: Executor -> loader(key, executor) }


/**
 * Coroutines Suspend 함수를 이용하여 비동기로 캐시 값을 로딩하는 [AsyncLoadingCache]를 빌드합니다.
 *
 * ```
 * val caffeine = caffeine<Any, Any> {
 *      maximumSize(1000)
 *      expireAfterWrite(5, TimeUnit.MINUTES)
 * }
 * val asyncLoadingCache: AsyncLoadingCache<String, Int> = caffeine.suspendLoadingCache { key ->
 *     // suspend function
 *     key.length
 * }
 * runBlocking {
 *      asyncLoadingCache.put("hello", 5).join()
 *      asyncLoadingCache.get("hello").await()  // 5
 * }
 * ```
 *
 * @param K         Cache key type
 * @param V         Cache value type
 * @param loader 값을 로딩하는 suspend 함수
 * @return [AsyncLoadingCache] 함수
 */
inline fun <K: Any, V: Any> Caffeine<Any, Any>.suspendLoadingCache(
    @BuilderInference crossinline loader: suspend (key: K) -> V,
): AsyncLoadingCache<K, V> {
    return buildAsync { key, executor: Executor ->
        CoroutineScope(executor.asCoroutineDispatcher()).future {
            loader(key)
        }
    }
}


/**
 * [AsyncCache]에 값이 없으면 [loader]를 이용하여 값을 채우고, 반환합니다.
 *
 * ```
 * val cache = caffeine<Any, Any> {
 *   maximumSize(1000)
 *   expireAfterWrite(5, TimeUnit.MINUTES)
 * }
 * val asyncCache: AsyncCache<String, Int> = cache.asyncCache()
 *
 * runBlocking {
 *      val future:CompletableFuture<Int> = asyncCache.getSuspending("hello") { key ->
 *          // suspend function
 *          delay(10)
 *          key.length
 *      }
 *      future.await()  // 5
 * }
 * ```
 *
 * @param key     cache key
 * @param loader  캐시 값을 Coroutines 환경에서 로딩하는 함수
 * @return [CompletableFuture] for cache value
 */
inline fun <K: Any, V: Any> AsyncCache<K, V>.getSuspending(
    key: K,
    @BuilderInference crossinline loader: suspend (key: K) -> V,
): CompletableFuture<V> {
    return this.get(key) { k: K, executor: Executor ->
        CoroutineScope(executor.asCoroutineDispatcher()).future {
            loader(k)
        }
    }
}
