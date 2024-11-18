package io.bluetape4k.resilience4j.cache.impl

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import io.bluetape4k.resilience4j.cache.CoCache
import io.github.resilience4j.cache.event.CacheEvent
import io.github.resilience4j.cache.event.CacheOnErrorEvent
import io.github.resilience4j.cache.event.CacheOnHitEvent
import io.github.resilience4j.cache.event.CacheOnMissEvent
import io.github.resilience4j.core.EventConsumer
import io.github.resilience4j.core.EventProcessor
import kotlinx.atomicfu.atomic

/**
 * [CoCache]의 기본 구현체입니다.
 * jCache의 저장소를 사용합니다.
 */
class CoroutinesCacheImpl<K, V>(override val jcache: javax.cache.Cache<K, V>): CoCache<K, V> {

    companion object: KLogging()

    private val _eventProcessor = CoCacheEventProcessor()
    private val _metrics = CoCacheMetrics()

    /**
     * the cache name
     */
    override val name: String get() = jcache.name


    /**
     * Returns the Metrics of this Cache.
     */
    override val metrics: CoCache.Metrics = _metrics

    /**
     * Returns an EventPublisher which can be used to register event consumers.
     */
    override val eventPublisher: CoCache.EventPublisher = _eventProcessor

    /**
     * 캐시된 정보를 로드합니다. 만약 캐시에 없을 시에는 [loader]를 이용하여 정보를 얻어 캐시에 저장하고, 반환합니다.
     *
     * @param cacheKey  Cache Key
     * @param loader    Value loader
     * @return cached value
     */
    override suspend fun computeIfAbsent(cacheKey: K, loader: suspend () -> V): V {
        return getValueFromCache(cacheKey) ?: computeAndPut(cacheKey, loader)
    }

    /**
     * 해당 키의 캐시 정보가 존재하는지 여부
     *
     * @param cacheKey Cache Key
     * @return    Cache된 정보가 있으면 true, 아니면 false
     */
    override fun containsKey(cacheKey: K): Boolean {
        return jcache.containsKey(cacheKey)
    }

    private suspend fun computeAndPut(cacheKey: K, loader: suspend () -> V): V {
        return loader().apply { putValueIntoCache(cacheKey, this) }
    }

    private fun getValueFromCache(cacheKey: K): V? {
        return try {
            if (jcache.containsKey(cacheKey)) {
                onCacheHit(cacheKey)
                jcache[cacheKey]
            } else {
                onCacheMiss(cacheKey)
                null
            }
        } catch (e: Throwable) {
            log.warn(e) { "Fail to get a value from Cache[$name], cacheKey=$cacheKey" }
            onError(e)
            null
        }
    }

    private fun putValueIntoCache(cacheKey: K, value: V?) {
        try {
            if (value != null) {
                jcache.put(cacheKey, value)
            }
        } catch (e: Throwable) {
            log.warn(e) { "Fail to put a value into cache [$name], cacheKey=$cacheKey" }
            onError(e)
        }
    }

    private fun onCacheMiss(cacheKey: K) {
        _metrics.onCacheMiss()
        publicCacheEvent { CacheOnMissEvent(name, cacheKey!!) }
    }

    private fun onCacheHit(cacheKey: K) {
        _metrics.onCacheHit()
        publicCacheEvent { CacheOnHitEvent(name, cacheKey!!) }
    }

    private fun onError(throwable: Throwable) {
        publicCacheEvent { CacheOnErrorEvent(name, throwable) }
    }

    private fun publicCacheEvent(event: () -> CacheEvent) {
        if (_eventProcessor.hasConsumers()) {
            _eventProcessor.processEvent(event())
        }
    }

    private class CoCacheEventProcessor: EventProcessor<CacheEvent>(), EventConsumer<CacheEvent>,
                                         CoCache.EventPublisher {
        override fun onCacheHit(eventConsumer: EventConsumer<CacheEvent>): CoCache.EventPublisher = apply {
            registerConsumer(CacheOnHitEvent::class.simpleName!!, eventConsumer)
        }

        override fun onCacheMiss(eventConsumer: EventConsumer<CacheEvent>): CoCache.EventPublisher = apply {
            registerConsumer(CacheOnMissEvent::class.simpleName!!, eventConsumer)
        }

        override fun onError(eventConsumer: EventConsumer<CacheEvent>): CoCache.EventPublisher = apply {
            registerConsumer(CacheOnErrorEvent::class.simpleName!!, eventConsumer)
        }

        override fun consumeEvent(event: CacheEvent) {
            super.processEvent(event)
        }
    }

    private class CoCacheMetrics: CoCache.Metrics {
        private val cacheMisses = atomic(0L)
        private val cacheHits = atomic(0L)

        fun onCacheMiss() {
            cacheMisses.incrementAndGet()
        }

        fun onCacheHit() {
            cacheHits.incrementAndGet()
        }

        /**
         * Returns the current number of cache hits
         */
        override fun getNumberOfCacheHits(): Long = cacheHits.value

        /**
         * Retruns the current number of cache misses
         */
        override fun getNumberOfCacheMisses(): Long = cacheMisses.value
    }
}
