package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.cache.stats.CacheStats

/**
 * 로컬(인메모리) 캐시 추상화.
 * Near Cache의 front tier로 사용된다.
 *
 * ```kotlin
 * val localCache: LettuceLocalCache<String, String> = LettuceCaffeineLocalCache(config)
 * localCache.put("key1", "value1")
 * val value = localCache.get("key1")
 * // value == "value1"
 * localCache.close()
 * ```
 */
interface LettuceLocalCache<K: Any, V: Any>: AutoCloseable {

    /**
     * 키에 대한 값을 조회합니다.
     *
     * ```kotlin
     * localCache.put("k", "v")
     * val result = localCache.get("k")
     * // result == "v"
     * ```
     */
    fun get(key: K): V?

    /**
     * 여러 키에 대한 값을 한 번에 조회합니다.
     *
     * ```kotlin
     * localCache.putAll(mapOf("a" to "1", "b" to "2"))
     * val results = localCache.getAll(setOf("a", "b"))
     * // results == mapOf("a" to "1", "b" to "2")
     * ```
     */
    fun getAll(keys: Set<K>): Map<K, V>

    /**
     * 키-값을 저장합니다.
     *
     * ```kotlin
     * localCache.put("hello", "world")
     * // localCache.get("hello") == "world"
     * ```
     */
    fun put(key: K, value: V)

    /**
     * 여러 키-값을 한 번에 저장합니다.
     *
     * ```kotlin
     * localCache.putAll(mapOf("x" to "1", "y" to "2"))
     * // localCache.get("x") == "1"
     * ```
     */
    fun putAll(map: Map<out K, V>)

    /**
     * 키를 제거합니다.
     *
     * ```kotlin
     * localCache.put("key", "value")
     * localCache.remove("key")
     * // localCache.get("key") == null
     * ```
     */
    fun remove(key: K)

    /**
     * 여러 키를 한 번에 제거합니다.
     *
     * ```kotlin
     * localCache.putAll(mapOf("a" to "1", "b" to "2"))
     * localCache.removeAll(setOf("a", "b"))
     * // localCache.get("a") == null
     * ```
     */
    fun removeAll(keys: Set<K>)

    /**
     * Redis back-cache로의 전파 없이 로컬 캐시에서만 제거.
     *
     * ```kotlin
     * localCache.put("key", "value")
     * localCache.invalidate("key")
     * // localCache.get("key") == null
     * ```
     */
    fun invalidate(key: K)

    /**
     * 여러 키를 로컬 캐시에서만 무효화합니다.
     *
     * ```kotlin
     * localCache.putAll(mapOf("a" to "1", "b" to "2"))
     * localCache.invalidateAll(listOf("a", "b"))
     * // localCache.get("a") == null
     * ```
     */
    fun invalidateAll(keys: Collection<K>)

    /**
     * 키가 존재하는지 확인합니다.
     *
     * ```kotlin
     * localCache.put("present", "yes")
     * val exists = localCache.containsKey("present")
     * // exists == true
     * val missing = localCache.containsKey("missing")
     * // missing == false
     * ```
     */
    fun containsKey(key: K): Boolean

    /**
     * 모든 항목을 제거합니다.
     *
     * ```kotlin
     * localCache.put("a", "1")
     * localCache.clear()
     * // localCache.estimatedSize() == 0L
     * ```
     */
    fun clear()

    /**
     * 현재 캐시의 추정 크기를 반환합니다.
     *
     * ```kotlin
     * localCache.put("a", "1")
     * val size = localCache.estimatedSize()
     * // size >= 1L
     * ```
     */
    fun estimatedSize(): Long

    /**
     * Caffeine 캐시 통계를 반환합니다. [LettuceNearCacheConfig.recordStats]가 true일 때만 유효합니다.
     *
     * ```kotlin
     * val stats = localCache.stats()
     * // stats == null (recordStats=false) 또는 stats.hitCount() >= 0
     * ```
     */
    fun stats(): CacheStats? = null

    override fun close() {}
}
