package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.stats.CacheStats

/**
 * Hazelcast IMap NearCache의 로컬(인메모리) 캐시 추상화.
 * Near Cache의 front tier로 사용된다.
 *
 * ```kotlin
 * val localCache: HazelcastLocalCache<String, String> = CaffeineHazelcastLocalCache(config)
 * localCache.put("key", "value")
 * val value = localCache.get("key")
 * // value == "value"
 * ```
 *
 * @param K 키 타입
 * @param V 값 타입
 */
interface HazelcastLocalCache<K : Any, V : Any> : AutoCloseable {

    /**
     * [key]에 해당하는 값을 반환합니다. 없으면 null을 반환합니다.
     *
     * ```kotlin
     * localCache.put("k", "v")
     * val v = localCache.get("k")
     * // v == "v"
     * ```
     */
    fun get(key: K): V?

    /**
     * [keys]에 해당하는 값을 일괄 조회합니다. 없는 키는 결과에서 제외됩니다.
     *
     * ```kotlin
     * localCache.put("k1", "v1")
     * localCache.put("k2", "v2")
     * val map = localCache.getAll(setOf("k1", "k2", "k3"))
     * // map.size == 2
     * ```
     */
    fun getAll(keys: Set<K>): Map<K, V>

    /**
     * [key]-[value] 쌍을 저장합니다.
     *
     * ```kotlin
     * localCache.put("k", "v")
     * // localCache.get("k") == "v"
     * ```
     */
    fun put(key: K, value: V)

    /**
     * 여러 [map] 엔트리를 일괄 저장합니다.
     *
     * ```kotlin
     * localCache.putAll(mapOf("k1" to "v1", "k2" to "v2"))
     * // localCache.estimatedSize() >= 2
     * ```
     */
    fun putAll(map: Map<out K, V>)

    /**
     * [key]를 캐시에서 제거하고 IMap에도 전파합니다.
     *
     * ```kotlin
     * localCache.put("k", "v")
     * localCache.remove("k")
     * // localCache.get("k") == null
     * ```
     */
    fun remove(key: K)

    /**
     * 여러 [keys]를 캐시에서 일괄 제거합니다.
     *
     * ```kotlin
     * localCache.removeAll(setOf("k1", "k2"))
     * // localCache.get("k1") == null
     * ```
     */
    fun removeAll(keys: Set<K>)

    /**
     * IMap back-cache로의 전파 없이 로컬 캐시에서만 제거합니다.
     *
     * ```kotlin
     * localCache.put("k", "v")
     * localCache.invalidate("k")
     * // localCache.get("k") == null
     * ```
     */
    fun invalidate(key: K)

    /**
     * IMap back-cache로의 전파 없이 여러 [keys]를 로컬 캐시에서만 제거합니다.
     *
     * ```kotlin
     * localCache.invalidateAll(listOf("k1", "k2"))
     * // localCache.get("k1") == null
     * ```
     */
    fun invalidateAll(keys: Collection<K>)

    /**
     * [key]가 로컬 캐시에 존재하는지 확인합니다.
     *
     * ```kotlin
     * localCache.put("k", "v")
     * val exists = localCache.containsKey("k")
     * // exists == true
     * ```
     */
    fun containsKey(key: K): Boolean

    /**
     * 로컬 캐시의 모든 항목을 비웁니다.
     *
     * ```kotlin
     * localCache.put("k", "v")
     * localCache.clear()
     * // localCache.estimatedSize() == 0
     * ```
     */
    fun clear()

    /**
     * 로컬 캐시의 예상 항목 수를 반환합니다.
     *
     * ```kotlin
     * localCache.put("k", "v")
     * val size = localCache.estimatedSize()
     * // size >= 1
     * ```
     */
    fun estimatedSize(): Long

    /**
     * Caffeine 캐시 통계 스냅샷을 반환합니다.
     * `recordStats = true`인 경우에만 실제 통계를 반환하고 아니면 null을 반환합니다.
     */
    fun stats(): CacheStats? = null

    override fun close() {}
}

/**
 * Caffeine 기반 [HazelcastLocalCache] 구현.
 * [HazelcastNearCacheConfig] 설정을 사용해 Caffeine 캐시를 초기화한다.
 *
 * ```kotlin
 * val config = HazelcastNearCacheConfig(cacheName = "users", maxLocalSize = 500)
 * val localCache = CaffeineHazelcastLocalCache<String>(config)
 * localCache.put("k", "v")
 * val value = localCache.get("k")
 * // value == "v"
 * ```
 *
 * @param V 값 타입 (키는 항상 String)
 */
class CaffeineHazelcastLocalCache<V : Any>(
    private val config: HazelcastNearCacheConfig,
) : HazelcastLocalCache<String, V> {

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
