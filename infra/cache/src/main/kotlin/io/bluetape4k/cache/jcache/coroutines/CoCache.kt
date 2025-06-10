package io.bluetape4k.cache.jcache.coroutines

import kotlinx.coroutines.flow.Flow
import javax.cache.configuration.CacheEntryListenerConfiguration

/**
 * Coroutines 환경에서 [javax.cache.Cache]와 같은 기능을 제공하는 Cache 입니다.
 *
 * @param K key type
 * @param V value type
 */
@Deprecated("Use SuspendCache instead", ReplaceWith("SuspendCache"))
interface CoCache<K: Any, V: Any> {

    fun entries(): Flow<CoCacheEntry<K, V>>

    suspend fun clear()

    suspend fun close()

    fun isClosed(): Boolean

    suspend fun containsKey(key: K): Boolean

    suspend fun get(key: K): V?

    fun getAll(): Flow<CoCacheEntry<K, V>>
    fun getAll(vararg keys: K): Flow<CoCacheEntry<K, V>> = getAll(keys.toSet())
    fun getAll(keys: Set<K>): Flow<CoCacheEntry<K, V>>

    suspend fun getAndPut(key: K, value: V): V?
    suspend fun getAndRemove(key: K): V?
    suspend fun getAndReplace(key: K, value: V): V?

    suspend fun put(key: K, value: V)
    suspend fun putAll(map: Map<K, V>)
    suspend fun putAllFlow(entries: Flow<Pair<K, V>>)

    suspend fun putIfAbsent(key: K, value: V): Boolean

    suspend fun remove(key: K): Boolean
    suspend fun remove(key: K, oldValue: V): Boolean

    suspend fun removeAll()
    suspend fun removeAll(vararg keys: K) = removeAll(keys.toSet())
    suspend fun removeAll(keys: Set<K>)

    suspend fun replace(key: K, oldValue: V, newValue: V): Boolean
    suspend fun replace(key: K, value: V): Boolean

    fun registerCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>)
    fun deregisterCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>)

    fun <T: Any> unwrap(clazz: Class<T>): T? {
        if (clazz.isAssignableFrom(javaClass)) {
            return clazz.cast(this)
        }
        return null
    }
}
