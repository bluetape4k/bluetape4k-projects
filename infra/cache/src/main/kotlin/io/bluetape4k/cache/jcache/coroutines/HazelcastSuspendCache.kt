package io.bluetape4k.cache.jcache.coroutines

import com.hazelcast.cache.ICache
import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.jcache.getDefaultJCacheConfiguration
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.future.await
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withContext
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * Hazelcast JCache 기반 Coroutines용 [SuspendCache] 구현체입니다.
 *
 * `unwrap(ICache::class.java)`에 성공하면 Hazelcast 비동기 API를 활용하고,
 * 실패하면 표준 JCache 동기 API를 `Dispatchers.IO`에서 실행합니다.
 *
 * @param K key type
 * @param V value type
 * @property cache Hazelcast JCache 인스턴스
 */
class HazelcastSuspendCache<K: Any, V: Any>(private val cache: JCache<K, V>): SuspendCache<K, V> {

    companion object: KLoggingChannel() {
        @JvmStatic
        operator fun <K: Any, V: Any> invoke(
            cacheName: String,
            configuration: Configuration<K, V> = MutableConfiguration(),
        ): HazelcastSuspendCache<K, V> {
            cacheName.requireNotBlank("cacheName")
            val manager = JCaching.Hazelcast.cacheManager
            val jcache = manager.getCache(cacheName, configuration.keyType, configuration.valueType)
                ?: manager.createCache(cacheName, configuration)
            return HazelcastSuspendCache(jcache)
        }

        @JvmStatic
        inline operator fun <reified K: Any, reified V: Any> invoke(
            cacheName: String,
        ): HazelcastSuspendCache<K, V> {
            cacheName.requireNotBlank("cacheName")
            val jcache = JCaching.Hazelcast.getOrCreate<K, V>(cacheName, getDefaultJCacheConfiguration())
            return HazelcastSuspendCache(jcache)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val asyncCache: ICache<K, V>? =
        runCatching { cache.unwrap(ICache::class.java) as? ICache<K, V> }.getOrNull()

    override fun entries(): Flow<SuspendCacheEntry<K, V>> = flow {
        cache.asSequence().forEach { emit(SuspendCacheEntry(it.key, it.value)) }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) { cache.clear() }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) { cache.close() }
    }

    override fun isClosed(): Boolean = cache.isClosed

    override suspend fun containsKey(key: K): Boolean =
        withContext(Dispatchers.IO) { cache.containsKey(key) }

    override suspend fun get(key: K): V? =
        asyncCache?.getAsync(key)?.await()
            ?: withContext(Dispatchers.IO) { cache.get(key) }

    override fun getAll(): Flow<SuspendCacheEntry<K, V>> = entries()

    override fun getAll(keys: Set<K>): Flow<SuspendCacheEntry<K, V>> = flow {
        val entries =
            withContext(Dispatchers.IO) { cache.getAll(keys) }
        entries.forEach { (k, v) -> emit(SuspendCacheEntry(k, v)) }
    }

    override suspend fun getAndPut(key: K, value: V): V? =
        get(key).also { put(key, value) }

    override suspend fun getAndRemove(key: K): V? =
        asyncCache?.getAndRemoveAsync(key)?.await()
            ?: withContext(Dispatchers.IO) { cache.getAndRemove(key) }

    override suspend fun getAndReplace(key: K, value: V): V? =
        asyncCache?.getAndReplaceAsync(key, value)?.await()
            ?: withContext(Dispatchers.IO) { cache.getAndReplace(key, value) }

    override suspend fun put(key: K, value: V) {
        asyncCache?.putAsync(key, value)?.await()
            ?: withContext(Dispatchers.IO) { cache.put(key, value) }
    }

    override suspend fun putAll(map: Map<K, V>) {
        withContext(Dispatchers.IO) { cache.putAll(map) }
    }

    override suspend fun putAllFlow(entries: Flow<Pair<K, V>>) {
        if (asyncCache == null) {
            entries.collect { put(it.first, it.second) }
            return
        }

        entries
            .map { asyncCache.putAsync(it.first, it.second).asDeferred() }
            .toList()
            .joinAll()
    }

    override suspend fun putIfAbsent(key: K, value: V): Boolean =
        asyncCache?.putIfAbsentAsync(key, value)?.await()
            ?: withContext(Dispatchers.IO) { cache.putIfAbsent(key, value) }

    override suspend fun remove(key: K): Boolean =
        asyncCache?.removeAsync(key)?.await()
            ?: withContext(Dispatchers.IO) { cache.remove(key) }

    override suspend fun remove(key: K, oldValue: V): Boolean =
        asyncCache?.removeAsync(key, oldValue)?.await()
            ?: withContext(Dispatchers.IO) { cache.remove(key, oldValue) }

    override suspend fun removeAll() {
        withContext(Dispatchers.IO) { cache.removeAll() }
    }

    override suspend fun removeAll(keys: Set<K>) {
        withContext(Dispatchers.IO) { cache.removeAll(keys) }
    }

    override suspend fun replace(key: K, oldValue: V, newValue: V): Boolean =
        asyncCache?.replaceAsync(key, oldValue, newValue)?.await()
            ?: withContext(Dispatchers.IO) { cache.replace(key, oldValue, newValue) }

    override suspend fun replace(key: K, value: V): Boolean =
        asyncCache?.replaceAsync(key, value)?.await()
            ?: withContext(Dispatchers.IO) { cache.replace(key, value) }

    override fun registerCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        cache.registerCacheEntryListener(configuration)
    }

    override fun deregisterCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        cache.deregisterCacheEntryListener(configuration)
    }
}
