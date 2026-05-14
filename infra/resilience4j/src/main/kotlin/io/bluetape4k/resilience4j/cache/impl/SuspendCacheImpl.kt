package io.bluetape4k.resilience4j.cache.impl

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.warn
import io.bluetape4k.resilience4j.cache.SuspendCache
import io.bluetape4k.resilience4j.rethrowIfCancellation
import io.github.resilience4j.cache.event.CacheEvent
import io.github.resilience4j.cache.event.CacheOnErrorEvent
import io.github.resilience4j.cache.event.CacheOnHitEvent
import io.github.resilience4j.cache.event.CacheOnMissEvent
import io.github.resilience4j.core.EventConsumer
import io.github.resilience4j.core.EventProcessor
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.cache.Cache

/**
 * Default [SuspendCache] implementation backed by a JCache instance.
 *
 * Contract:
 * - Cache hits and misses are reported through [SuspendCache.metrics] and
 *   [SuspendCache.eventPublisher].
 * - Concurrent misses for the same key are serialized so the suspend loader
 *   runs once per key.
 * - `CancellationException` from the loader or JCache access is rethrown
 *   unchanged and is not published as a cache error event.
 */
class SuspendCacheImpl<K, V>(override val jcache: Cache<K, V>): SuspendCache<K, V> {

    companion object: KLoggingChannel()

    /**
     * 키별 Mutex 풀입니다.
     *
     * 동일 키에 대해 여러 코루틴이 동시에 cache miss를 경험하면 loader()가 중복 호출되는
     * race condition이 발생합니다. 키 단위 Mutex로 직렬화해 loader()가 한 번만 실행되도록
     * 보장합니다. computeIfAbsent 완료 후 Mutex를 제거해 메모리 누수를 방지합니다.
     */
    private val keyLocks = ConcurrentHashMap<Any, Mutex>()

    private fun mutexFor(key: Any): Mutex = keyLocks.computeIfAbsent(key) { Mutex() }

    private fun releaseMutex(key: Any, mutex: Mutex) {
        // lock이 해제된 상태이면 다음 요청자가 없으므로 안전하게 제거합니다.
        if (!mutex.isLocked) {
            keyLocks.remove(key, mutex)
        }
    }

    private val _eventProcessor = SuspendCacheEventProcessor()
    private val _metrics = SuspendCacheMetrics()

    /**
     * the cache name
     */
    override val name: String get() = jcache.name


    /**
     * Returns the Metrics of this Cache.
     */
    override val metrics: SuspendCache.Metrics = _metrics

    /**
     * Returns an EventPublisher which can be used to register event consumers.
     */
    override val eventPublisher: SuspendCache.EventPublisher = _eventProcessor

    /**
     * Returns a cached value or loads and stores a new value on miss.
     *
     * Concurrent misses for the same key are serialized with a key-scoped
     * mutex. The first coroutine runs [loader]; waiters re-check the cache
     * before loading. Cancellation is rethrown before logging or event
     * publication.
     *
     * @param cacheKey cache key; must not be null.
     * @param loader suspend value loader invoked on cache miss.
     * @return cached or loaded value.
     */
    override suspend fun computeIfAbsent(cacheKey: K, loader: suspend () -> V): V {
        val key = requireNotNull(cacheKey) { "cache key must not be null" }

        // 빠른 경로: 이미 캐시된 값이 있으면 Mutex 없이 즉시 반환합니다.
        // hit 메트릭은 getValueFromCache 내부에서 기록됩니다.
        getValueFromCache(key)?.let { return it }

        // 캐시 미스: 키별 Mutex로 직렬화합니다.
        val mutex = mutexFor(key as Any)
        return try {
            mutex.withLock {
                // Mutex 획득 후 재확인 (double-check locking):
                // fast-path에서 miss를 기록한 뒤 대기하는 동안 앞선 코루틴이
                // 이미 값을 채웠을 수 있습니다. 이 때는 miss를 중복 기록하지 않고
                // hit만 기록하기 위해 rawGetWithHit()을 사용합니다.
                rawGetWithHit(key) ?: computeAndPut(key, loader)
            }
        } finally {
            releaseMutex(key, mutex)
        }
    }

    /**
     * JCache에서 값을 직접 조회하고, 값이 있으면 hit 메트릭을 기록합니다.
     *
     * [computeIfAbsent]의 Mutex 내부 double-check 전용입니다.
     *
     * ## 메트릭 정합성
     * fast-path에서 이미 onCacheMiss를 기록했으므로, Mutex 획득 후 재확인 시
     * onCacheMiss가 중복으로 기록되지 않도록 분리된 메서드입니다.
     * 앞선 코루틴이 이미 값을 로드해 캐시에 넣은 경우에는 hit 메트릭을 기록하여
     * miss 1건이 hit 1건으로 보정됩니다.
     */
    private fun rawGetWithHit(cacheKey: K): V? {
        return try {
            if (jcache.containsKey(cacheKey)) {
                // double-check에서 값이 있으면 hit으로 정정합니다.
                // fast-path의 miss 카운트는 이미 기록되었으므로 별도 보정은 하지 않습니다.
                onCacheHit(requireNotNull(cacheKey) { "cache key must not be null" })
                jcache[cacheKey]
            } else {
                null
            }
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
            null
        }
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
                onCacheHit(requireNotNull(cacheKey) { "cache key must not be null" })
                jcache[cacheKey]
            } else {
                onCacheMiss(requireNotNull(cacheKey) { "cache key must not be null" })
                null
            }
        } catch (e: Throwable) {
            e.rethrowIfCancellation()
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
            e.rethrowIfCancellation()
            log.warn(e) { "Fail to put a value into cache [$name], cacheKey=$cacheKey" }
            onError(e)
        }
    }

    private fun onCacheMiss(cacheKey: Any) {
        _metrics.onCacheMiss()
        publicCacheEvent { CacheOnMissEvent(name, cacheKey) }
    }

    private fun onCacheHit(cacheKey: Any) {
        _metrics.onCacheHit()
        publicCacheEvent { CacheOnHitEvent(name, cacheKey) }
    }

    private fun onError(throwable: Throwable) {
        publicCacheEvent { CacheOnErrorEvent(name, throwable) }
    }

    private fun publicCacheEvent(event: () -> CacheEvent) {
        if (_eventProcessor.hasConsumers()) {
            _eventProcessor.processEvent(event())
        }
    }

    private class SuspendCacheEventProcessor: EventProcessor<CacheEvent>(), EventConsumer<CacheEvent>,
                                              SuspendCache.EventPublisher {
        override fun onCacheHit(
            eventConsumer: EventConsumer<CacheEvent>,
        ): SuspendCache.EventPublisher = apply {
            registerConsumer(CacheOnHitEvent::class.simpleName!!, eventConsumer)
        }

        override fun onCacheMiss(
            eventConsumer: EventConsumer<CacheEvent>,
        ): SuspendCache.EventPublisher = apply {
            registerConsumer(CacheOnMissEvent::class.simpleName!!, eventConsumer)
        }

        override fun onError(
            eventConsumer: EventConsumer<CacheEvent>,
        ): SuspendCache.EventPublisher = apply {
            registerConsumer(CacheOnErrorEvent::class.simpleName!!, eventConsumer)
        }

        override fun consumeEvent(event: CacheEvent) {
            super.processEvent(event)
        }
    }

    private class SuspendCacheMetrics: SuspendCache.Metrics {
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
         * Returns the current number of cache misses
         */
        override fun getNumberOfCacheMisses(): Long = cacheMisses.value
    }
}
