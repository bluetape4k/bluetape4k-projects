package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats

/**
 * Redisson RESP3 NearCache의 로컬(인메모리) 캐시 추상화.
 * Near Cache의 front tier로 사용된다.
 *
 * @param K 키 타입
 * @param V 값 타입
 */
interface RedissonLocalCache<K : Any, V : Any> : AutoCloseable {

    fun get(key: K): V?

    fun getAll(keys: Set<K>): Map<K, V>

    fun put(key: K, value: V)

    fun putAll(map: Map<out K, V>)

    fun remove(key: K)

    fun removeAll(keys: Set<K>)

    /**
     * Redis back-cache로의 전파 없이 로컬 캐시에서만 제거.
     */
    fun invalidate(key: K)

    fun invalidateAll(keys: Collection<K>)

    fun containsKey(key: K): Boolean

    fun clear()

    fun estimatedSize(): Long

    fun stats(): CacheStats? = null

    override fun close() {}
}

/**
 * Caffeine 기반 [RedissonLocalCache] 구현.
 * [RedissonResp3NearCacheConfig] 설정을 사용해 Caffeine 캐시를 초기화한다.
 *
 * @param V 값 타입 (키는 항상 String)
 */
class CaffeineRedissonLocalCache<V : Any>(
    private val config: RedissonResp3NearCacheConfig,
) : RedissonLocalCache<String, V> {

    private val cache: Cache<String, V> = Caffeine.newBuilder()
        .maximumSize(config.maxLocalSize)
        .expireAfterWrite(config.frontExpireAfterWrite)
        .also { builder ->
            config.frontExpireAfterAccess?.let { builder.expireAfterAccess(it) }
            if (config.recordStats) builder.recordStats()
        }
        .build()

    override fun get(key: String): V? = cache.getIfPresent(key)

    override fun getAll(keys: Set<String>): Map<String, V> = cache.getAllPresent(keys)

    override fun put(key: String, value: V) = cache.put(key, value)

    override fun putAll(map: Map<out String, V>) = cache.putAll(map)

    override fun remove(key: String) = cache.invalidate(key)

    override fun removeAll(keys: Set<String>) = cache.invalidateAll(keys)

    override fun invalidate(key: String) = cache.invalidate(key)

    override fun invalidateAll(keys: Collection<String>) = cache.invalidateAll(keys)

    override fun containsKey(key: String): Boolean = cache.getIfPresent(key) != null

    override fun clear() = cache.invalidateAll()

    override fun estimatedSize(): Long = cache.estimatedSize()

    override fun stats(): CacheStats? = if (config.recordStats) cache.stats() else null

    override fun close() {
        cache.invalidateAll()
        cache.cleanUp()
    }
}
