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
        fun <K, V> of(jcache: Cache<K, V>): SuspendCache<K, V> = SuspendCacheImpl(jcache)

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
        ): suspend (K) -> V = cache.decorateSuspendSupplier(loader)

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
        ): suspend (K) -> V = cache.decorateSuspendFunction(loader)
    }

    /** 캐시 이름 */
    val name: String

    /** 내부 JCache 인스턴스 */
    val jcache: Cache<K, V>

    /** 이 캐시의 히트/미스 메트릭을 반환합니다. */
    val metrics: Metrics

    /** 캐시 이벤트(히트, 미스, 에러) 소비자를 등록할 수 있는 EventPublisher를 반환합니다. */
    val eventPublisher: EventPublisher

    /**
     * 캐시된 정보를 로드합니다. 만약 캐시에 없을 시에는 [loader]를 이용하여 정보를 얻어 캐시에 저장하고, 반환합니다.
     *
     * @param cacheKey       Cache Key
     * @param loader    Value loader
     * @return cached value
     */
    suspend fun computeIfAbsent(
        cacheKey: K,
        loader: suspend () -> V,
    ): V

    /**
     * 해당 키의 캐시 정보가 존재하는지 여부
     *
     * @param cacheKey Cache Key
     * @return Cache된 정보가 있으면 true, 아니면 false
     */
    fun containsKey(cacheKey: K): Boolean

    interface Metrics {
        /**
         * 현재까지의 캐시 히트 횟수를 반환합니다.
         */
        fun getNumberOfCacheHits(): Long

        /**
         * 현재까지의 캐시 미스 횟수를 반환합니다.
         */
        fun getNumberOfCacheMisses(): Long
    }

    /**
     * 캐시 이벤트 소비자를 등록할 수 있는 EventPublisher입니다.
     */
    interface EventPublisher: io.github.resilience4j.core.EventPublisher<CacheEvent> {
        /** 캐시 히트 이벤트 소비자를 등록합니다. */
        fun onCacheHit(eventConsumer: EventConsumer<CacheEvent>): EventPublisher

        /** 캐시 미스 이벤트 소비자를 등록합니다. */
        fun onCacheMiss(eventConsumer: EventConsumer<CacheEvent>): EventPublisher

        /** 캐시 에러 이벤트 소비자를 등록합니다. */
        fun onError(eventConsumer: EventConsumer<CacheEvent>): EventPublisher
    }
}
