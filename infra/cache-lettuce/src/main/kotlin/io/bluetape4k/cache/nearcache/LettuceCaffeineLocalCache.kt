package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats

/**
 * Caffeine 기반 [LettuceLocalCache] 구현.
 *
 * ```kotlin
 * val config = LettuceNearCacheConfig<String, String>(cacheName = "local", maxLocalSize = 1000)
 * val cache = LettuceCaffeineLocalCache(config)
 * cache.put("hello", "world")
 * val value = cache.get("hello")
 * // value == "world"
 * cache.close()
 * ```
 */
class LettuceCaffeineLocalCache<K: Any, V: Any>(private val config: LettuceNearCacheConfig<K, V>):
    LettuceLocalCache<K, V> {

    private val cache: Cache<K, V> = Caffeine.newBuilder()
        .maximumSize(config.maxLocalSize)
        .expireAfterWrite(config.frontExpireAfterWrite)
        .also { builder ->
            config.frontExpireAfterAccess?.let { builder.expireAfterAccess(it) }
            if (config.recordStats) builder.recordStats()
        }
        .build()

    override fun get(key: K): V? = cache.getIfPresent(key)

    override fun getAll(keys: Set<K>): Map<K, V> = cache.getAllPresent(keys)

    override fun put(key: K, value: V) = cache.put(key, value)

    override fun putAll(map: Map<out K, V>) = cache.putAll(map)

    override fun remove(key: K) = cache.invalidate(key)

    override fun removeAll(keys: Set<K>) = cache.invalidateAll(keys)

    override fun invalidate(key: K) = cache.invalidate(key)

    override fun invalidateAll(keys: Collection<K>) = cache.invalidateAll(keys)

    override fun containsKey(key: K): Boolean = cache.getIfPresent(key) != null

    override fun clear() = cache.invalidateAll()

    override fun estimatedSize(): Long = cache.estimatedSize()

    override fun stats(): CacheStats? = if (config.recordStats) cache.stats() else null

    override fun close() {
        cache.invalidateAll()
        cache.cleanUp()
    }
}
