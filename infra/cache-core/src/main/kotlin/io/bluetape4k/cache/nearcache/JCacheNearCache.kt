package io.bluetape4k.cache.nearcache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.support.asyncRunWithTimeout
import kotlinx.atomicfu.atomic
import java.util.concurrent.atomic.AtomicLong
import javax.cache.configuration.MutableCacheEntryListenerConfiguration

/**
 * JCache 기반 [NearCacheOperations] 구현체.
 *
 * Caffeine 등 JCache 호환 로컬 캐시(front)와 Redis 등 JCache 호환 원격 캐시(back)를
 * 2-tier로 조합합니다. Back cache의 변경 이벤트를 수신하여 front cache를 자동 동기화합니다.
 *
 * ```kotlin
 * val cache = jcacheNearCacheOf<String>(backCache, config)
 * cache.put("key", "value")
 * cache.get("key") // front에서 먼저 조회
 * ```
 *
 * @param V 캐시 값 타입 (키는 String 고정)
 * @see NearCacheOperations
 */
class JCacheNearCache<V : Any> private constructor(
    private val frontCache: JCache<String, V>,
    private val backCache: JCache<String, V>,
    private val config: NearCacheConfig<String, V>,
) : NearCacheOperations<V> {
    companion object : KLogging() {
        /**
         * [JCacheNearCache] 인스턴스를 생성합니다.
         *
         * @param V 캐시 값 타입
         * @param nearCacheCfg Front Cache 설정
         * @param backCache 원격 캐시 인스턴스
         * @return [JCacheNearCache] 인스턴스
         */
        operator fun <V : Any> invoke(
            nearCacheCfg: NearCacheConfig<String, V>,
            backCache: JCache<String, V>,
        ): JCacheNearCache<V> {
            val frontCacheManager = nearCacheCfg.cacheManagerFactory.create()

            log.info { "front cache 생성. name=${nearCacheCfg.frontCacheName}" }
            val frontCache =
                frontCacheManager.createCache(nearCacheCfg.frontCacheName, nearCacheCfg.frontCacheConfiguration)

            // back cache의 event를 받아 front cache에 반영
            val listenerCfg =
                MutableCacheEntryListenerConfiguration(
                    { CacheEntryEventListener(frontCache) },
                    null,
                    false,
                    nearCacheCfg.isSynchronous
                )
            log.info { "back cache 이벤트 리스너 등록. listenerCfg=$listenerCfg" }
            backCache.registerCacheEntryListener(listenerCfg)

            return JCacheNearCache(frontCache, backCache, nearCacheCfg)
        }
    }

    override val cacheName: String get() = config.frontCacheName

    private val closed = atomic(false)
    override val isClosed: Boolean by closed

    private val backHitCount = AtomicLong(0)
    private val backMissCount = AtomicLong(0)

    // -- Read --

    override fun get(key: String): V? {
        val frontValue = frontCache.get(key)
        if (frontValue != null) return frontValue

        // front miss → back 조회
        val backValue = backCache.get(key)
        if (backValue != null) {
            backHitCount.incrementAndGet()
            runCatching { frontCache.put(key, backValue) }
        } else {
            backMissCount.incrementAndGet()
        }
        return backValue
    }

    override fun getAll(keys: Set<String>): Map<String, V> {
        val result = mutableMapOf<String, V>()
        val missingKeys = mutableSetOf<String>()

        // front에서 먼저 조회
        keys.forEach { key ->
            val value = frontCache.get(key)
            if (value != null) {
                result[key] = value
            } else {
                missingKeys.add(key)
            }
        }

        // 미스된 키만 back에서 조회
        if (missingKeys.isNotEmpty()) {
            val backValues = backCache.getAll(missingKeys)
            backValues.forEach { (key, value) ->
                backHitCount.incrementAndGet()
                result[key] = value
                runCatching { frontCache.put(key, value) }
            }
            val backMissKeys = missingKeys - backValues.keys
            backMissCount.addAndGet(backMissKeys.size.toLong())
        }

        return result
    }

    override fun containsKey(key: String): Boolean = frontCache.containsKey(key) || backCache.containsKey(key)

    // -- Write --

    override fun put(
        key: String,
        value: V,
    ) {
        frontCache.put(key, value)
        syncBackCache { backCache.put(key, value) }
    }

    override fun putAll(entries: Map<String, V>) {
        frontCache.putAll(entries)
        syncBackCache { backCache.putAll(entries) }
    }

    override fun putIfAbsent(
        key: String,
        value: V,
    ): V? {
        // 기존 값이 있으면 반환, 없으면 저장 후 null 반환
        val existing = get(key)
        if (existing != null) return existing

        frontCache.put(key, value)
        syncBackCache { backCache.putIfAbsent(key, value) }
        return null
    }

    override fun replace(
        key: String,
        value: V,
    ): Boolean {
        val replaced = frontCache.replace(key, value)
        if (replaced) {
            syncBackCache {
                if (backCache.containsKey(key)) {
                    backCache.put(key, value)
                }
            }
        }
        return replaced
    }

    override fun replace(
        key: String,
        oldValue: V,
        newValue: V,
    ): Boolean {
        val replaced = frontCache.replace(key, oldValue, newValue)
        if (replaced) {
            syncBackCache {
                if (backCache.containsKey(key) && backCache.get(key) == oldValue) {
                    backCache.put(key, newValue)
                }
            }
        }
        return replaced
    }

    // -- Delete --

    override fun remove(key: String) {
        frontCache.remove(key)
        syncBackCache { backCache.remove(key) }
    }

    override fun removeAll(keys: Set<String>) {
        frontCache.removeAll(keys)
        syncBackCache {
            keys.forEach { runCatching { backCache.remove(it) } }
        }
    }

    override fun getAndRemove(key: String): V? {
        val value = get(key)
        if (value != null) {
            remove(key)
        }
        return value
    }

    override fun getAndReplace(
        key: String,
        value: V,
    ): V? {
        if (!containsKey(key)) return null
        val oldValue = get(key)
        put(key, value)
        return oldValue
    }

    // -- Cache Management --

    override fun clearLocal() {
        log.debug { "front cache를 clear합니다." }
        runCatching { frontCache.clear() }
    }

    override fun clearAll() {
        log.debug { "front cache, back cache 모두 clear합니다." }
        runCatching { frontCache.clear() }
        runCatching { backCache.clear() }
    }

    override fun localCacheSize(): Long {
        var count = 0L
        frontCache.forEach { count++ }
        return count
    }

    override fun backCacheSize(): Long {
        var count = 0L
        backCache.forEach { count++ }
        return count
    }

    // -- Statistics --

    override fun stats(): NearCacheStatistics =
        DefaultNearCacheStatistics(
            localHits = 0, // JCache는 Caffeine처럼 세밀한 stats를 제공하지 않음
            localMisses = 0,
            localSize = localCacheSize(),
            localEvictions = 0,
            backHits = backHitCount.get(),
            backMisses = backMissCount.get()
        )

    // -- Lifecycle --

    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            log.debug { "JCacheNearCache를 종료합니다. cacheName=$cacheName" }
            runCatching { frontCache.close() }
        }
    }

    private inline fun syncBackCache(crossinline syncTask: () -> Unit) {
        if (config.isSynchronous) {
            runCatching { syncTask() }
        } else {
            val timeoutMillis = config.syncRemoteTimeout.coerceAtLeast(NearCacheConfig.DEFAULT_SYNC_REMOTE_TIMEOUT)
            asyncRunWithTimeout(timeoutMillis) {
                runCatching { syncTask() }
            }
        }
    }
}

/**
 * JCache 기반 [NearCacheOperations]를 생성합니다.
 *
 * @param V 캐시 값 타입
 * @param backCache JCache 호환 원격 캐시
 * @param config NearCache 설정
 * @return [NearCacheOperations] 인스턴스
 */
fun <V : Any> jcacheNearCacheOf(
    backCache: JCache<String, V>,
    config: NearCacheConfig<String, V> = NearCacheConfig(),
): NearCacheOperations<V> = JCacheNearCache(config, backCache)
