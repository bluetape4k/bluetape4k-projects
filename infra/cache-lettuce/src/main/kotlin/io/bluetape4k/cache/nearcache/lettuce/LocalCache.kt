package io.bluetape4k.cache.nearcache.lettuce

/**
 * 로컬(인메모리) 캐시 추상화.
 * Near Cache의 front tier로 사용된다.
 */
interface LocalCache<K : Any, V : Any> : AutoCloseable {

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

    fun stats(): com.github.benmanes.caffeine.cache.stats.CacheStats? = null

    override fun close() {}
}
