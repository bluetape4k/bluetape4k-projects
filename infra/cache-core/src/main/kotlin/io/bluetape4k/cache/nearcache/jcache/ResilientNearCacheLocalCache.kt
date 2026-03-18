package io.bluetape4k.cache.nearcache.jcache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats
import java.time.Duration

/**
 * [ResilientNearCache] 및 [ResilientSuspendNearCache]의 front tier 로컬 캐시 추상화.
 *
 * JCache front와 별도로 순수 Caffeine 기반으로 구현하여
 * JCache 오버헤드 없이 고성능 로컬 캐싱을 제공한다.
 *
 * @param K 키 타입
 * @param V 값 타입
 */
interface ResilientLocalCache<K: Any, V: Any> : AutoCloseable {

    fun get(key: K): V?

    fun getAll(keys: Set<K>): Map<K, V>

    fun put(key: K, value: V)

    fun putAll(map: Map<K, V>)

    fun remove(key: K)

    fun removeAll(keys: Set<K>)

    fun containsKey(key: K): Boolean

    fun clear()

    fun estimatedSize(): Long

    fun stats(): CacheStats? = null

    override fun close() {}
}

/**
 * [ResilientNearCacheConfig]를 사용하는 Caffeine 기반 [ResilientLocalCache] 구현.
 *
 * @param K 키 타입
 * @param V 값 타입
 */
class CaffeineResilientLocalCache<K: Any, V: Any>(
    maxLocalSize: Long = 10_000,
    frontExpireAfterWrite: Duration = Duration.ofMinutes(30),
    frontExpireAfterAccess: Duration? = null,
    recordStats: Boolean = false,
) : ResilientLocalCache<K, V> {

    private val cache: Cache<K, V> = Caffeine.newBuilder()
        .maximumSize(maxLocalSize)
        .expireAfterWrite(frontExpireAfterWrite)
        .also { builder ->
            frontExpireAfterAccess?.let { builder.expireAfterAccess(it) }
            if (recordStats) builder.recordStats()
        }
        .build()

    override fun get(key: K): V? = cache.getIfPresent(key)

    override fun getAll(keys: Set<K>): Map<K, V> = cache.getAllPresent(keys)

    override fun put(key: K, value: V) = cache.put(key, value)

    override fun putAll(map: Map<K, V>) = cache.putAll(map)

    override fun remove(key: K) = cache.invalidate(key)

    override fun removeAll(keys: Set<K>) = cache.invalidateAll(keys)

    override fun containsKey(key: K): Boolean = cache.getIfPresent(key) != null

    override fun clear() = cache.invalidateAll()

    override fun estimatedSize(): Long = cache.estimatedSize()

    override fun stats(): CacheStats? = cache.stats().takeIf { it.requestCount() > 0 || it.hitCount() > 0 }

    override fun close() {
        cache.invalidateAll()
        cache.cleanUp()
    }
}
