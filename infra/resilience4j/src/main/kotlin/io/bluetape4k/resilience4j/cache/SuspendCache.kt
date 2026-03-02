package io.bluetape4k.resilience4j.cache

import io.bluetape4k.resilience4j.cache.impl.SuspendCacheImpl
import io.github.resilience4j.cache.event.CacheEvent
import io.github.resilience4j.core.EventConsumer
import javax.cache.Cache

/**
 * Resilience4j 캐시를 코루틴 suspend 함수와 함께 사용할 수 있게 하는 추상화입니다.
 *
 * ## 동작/계약
 * - 내부 저장소는 [jcache]를 사용하며 캐시 hit/miss 이벤트는 Resilience4j 이벤트로 발행됩니다.
 * - `decorateSuspendSupplier/decorateSuspendFunction` 계열은 loader 실행 결과를 키 기준으로 캐시합니다.
 * - [computeIfAbsent]는 캐시 miss일 때만 loader를 실행해야 합니다.
 *
 * ```kotlin
 * val cache = SuspendCache.of(jcache)
 * val value = cache.computeIfAbsent("u:1") { "debop" }
 * // value == "debop"
 * ```
 */
interface SuspendCache<K, V> {

    companion object {
        /**
         * JCache 인스턴스를 [SuspendCache] 구현으로 감쌉니다.
         *
         * ## 동작/계약
         * - [jcache] 참조를 그대로 보관하는 [SuspendCacheImpl]을 반환합니다.
         * - 호출마다 새 래퍼 인스턴스를 생성합니다.
         *
         * ```kotlin
         * val cache = SuspendCache.of(jcache)
         * // cache.jcache === jcache
         * ```
         */
        @JvmStatic
        fun <K, V> of(jcache: Cache<K, V>): SuspendCache<K, V> {
            return SuspendCacheImpl(jcache)
        }

        /**
         * keyless suspend supplier를 키 기반 함수로 감쌉니다.
         *
         * ## 동작/계약
         * - loader 결과는 전달된 [cache]를 통해 키별로 캐시됩니다.
         * - 동일 키 재호출 시 loader 대신 캐시 값이 반환됩니다.
         *
         * ```kotlin
         * val fn = SuspendCache.decorateSuspendedSupplier(cache) { "v" }
         * // fn("k1") == "v"
         * ```
         */
        @JvmStatic
        fun <K, V> decorateSuspendedSupplier(
            cache: SuspendCache<K, V>,
            loader: suspend () -> V,
        ): suspend (K) -> V {
            return cache.decorateSuspendSupplier(loader)
        }

        /**
         * key 기반 suspend loader를 캐시 데코레이터로 감쌉니다.
         *
         * ## 동작/계약
         * - 전달된 loader는 cache miss일 때만 실행됩니다.
         * - 캐시된 값이 있으면 loader를 호출하지 않습니다.
         *
         * ```kotlin
         * val fn = SuspendCache.decorateSuspendedFunction(cache) { key: String -> key.length }
         * // fn("abcd") == 4
         * ```
         */
        @JvmStatic
        fun <K, V> decorateSuspendedFunction(
            cache: SuspendCache<K, V>,
            loader: suspend (K) -> V,
        ): suspend (K) -> V {
            return cache.decorateSuspendFunction(loader)
        }
    }

    /**
     * the cache name
     */
    val name: String

    /**
     * Jcache
     */
    val jcache: Cache<K, V>

    /**
     * Returns the Metrics of this Cache.
     */
    val metrics: Metrics

    /**
     * Returns an EventPublisher which can be used to register event consumers.
     */
    val eventPublisher: EventPublisher

    /**
     * 캐시된 정보를 로드합니다. 만약 캐시에 없을 시에는 [loader]를 이용하여 정보를 얻어 캐시에 저장하고, 반환합니다.
     *
     * @param cacheKey       Cache Key
     * @param loader    Value loader
     * @return cached value
     */
    suspend fun computeIfAbsent(cacheKey: K, @BuilderInference loader: suspend () -> V): V

    /**
     * 해당 키의 캐시 정보가 존재하는지 여부
     *
     * @param cacheKey Cache Key
     * @return Cache된 정보가 있으면 true, 아니면 false
     */
    fun containsKey(cacheKey: K): Boolean

    interface Metrics {

        /**
         * Returns the current number of cache hits
         */
        fun getNumberOfCacheHits(): Long

        /**
         * Retruns the current number of cache misses
         */
        fun getNumberOfCacheMisses(): Long
    }

    /**
     * An EventPublisher which can be used to register event consumers.
     */
    interface EventPublisher: io.github.resilience4j.core.EventPublisher<CacheEvent> {

        fun onCacheHit(eventConsumer: EventConsumer<CacheEvent>): EventPublisher

        fun onCacheMiss(eventConsumer: EventConsumer<CacheEvent>): EventPublisher

        fun onError(eventConsumer: EventConsumer<CacheEvent>): EventPublisher
    }
}
