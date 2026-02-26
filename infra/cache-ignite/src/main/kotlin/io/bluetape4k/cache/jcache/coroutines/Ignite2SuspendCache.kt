package io.bluetape4k.cache.jcache.coroutines

import io.bluetape4k.cache.jcache.JCache
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
import org.apache.ignite.cache.CachingProvider as IgniteCachingProvider
import org.apache.ignite.IgniteCache
import org.apache.ignite.lang.IgniteFuture
import java.util.concurrent.CompletableFuture
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * Apache Ignite 2.x JCache 기반 Coroutines용 [SuspendCache] 구현체입니다.
 *
 * `unwrap(IgniteCache::class.java)`에 성공하면 Ignite 비동기 API를 사용하고,
 * 실패하면 표준 JCache 동기 API를 `Dispatchers.IO`에서 실행합니다.
 *
 * @param K key type
 * @param V value type
 * @property cache Ignite 2.x JCache 인스턴스
 */
class Ignite2SuspendCache<K: Any, V: Any>(private val cache: JCache<K, V>): SuspendCache<K, V> {

    companion object: KLoggingChannel() {
        @JvmStatic
        operator fun <K: Any, V: Any> invoke(
            cacheName: String,
            configuration: Configuration<K, V> = MutableConfiguration(),
        ): Ignite2SuspendCache<K, V> {
            cacheName.requireNotBlank("cacheName")
            val manager = IgniteCachingProvider().cacheManager
            val jcache = manager.getCache(cacheName, configuration.keyType, configuration.valueType)
                ?: manager.createCache(cacheName, configuration)
            return Ignite2SuspendCache(jcache)
        }

        @JvmStatic
        inline operator fun <reified K: Any, reified V: Any> invoke(
            cacheName: String,
        ): Ignite2SuspendCache<K, V> {
            cacheName.requireNotBlank("cacheName")
            val manager = IgniteCachingProvider().cacheManager
            val config = MutableConfiguration<K, V>().apply {
                setTypes(K::class.java, V::class.java)
            }
            val jcache = manager.getCache(cacheName, K::class.java, V::class.java)
                ?: manager.createCache(cacheName, config)
            return Ignite2SuspendCache(jcache)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private val igniteCache: IgniteCache<K, V>? =
        runCatching { cache.unwrap(IgniteCache::class.java) as? IgniteCache<K, V> }.getOrNull()

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
        igniteCache?.getAsync(key)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.get(key) }

    override fun getAll(): Flow<SuspendCacheEntry<K, V>> = entries()

    override fun getAll(keys: Set<K>): Flow<SuspendCacheEntry<K, V>> = flow {
        val entries =
            igniteCache?.getAllAsync(keys)?.awaitIgnite()
                ?: withContext(Dispatchers.IO) { cache.getAll(keys) }
        entries.forEach { (k, v) -> emit(SuspendCacheEntry(k, v)) }
    }

    override suspend fun getAndPut(key: K, value: V): V? =
        igniteCache?.getAndPutAsync(key, value)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.getAndPut(key, value) }

    override suspend fun getAndRemove(key: K): V? =
        igniteCache?.getAndRemoveAsync(key)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.getAndRemove(key) }

    override suspend fun getAndReplace(key: K, value: V): V? =
        igniteCache?.getAndReplaceAsync(key, value)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.getAndReplace(key, value) }

    override suspend fun put(key: K, value: V) {
        igniteCache?.putAsync(key, value)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.put(key, value) }
    }

    override suspend fun putAll(map: Map<K, V>) {
        igniteCache?.putAllAsync(map)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.putAll(map) }
    }

    override suspend fun putAllFlow(entries: Flow<Pair<K, V>>) {
        if (igniteCache != null) {
            entries
                .map { igniteCache.putAsync(it.first, it.second).toCompletableFuture() }
                .toList()
                .map { it.asDeferred() }
                .joinAll()
            return
        }
        entries.collect { put(it.first, it.second) }
    }

    override suspend fun putIfAbsent(key: K, value: V): Boolean =
        igniteCache?.putIfAbsentAsync(key, value)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.putIfAbsent(key, value) }

    override suspend fun remove(key: K): Boolean =
        igniteCache?.removeAsync(key)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.remove(key) }

    override suspend fun remove(key: K, oldValue: V): Boolean =
        igniteCache?.removeAsync(key, oldValue)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.remove(key, oldValue) }

    override suspend fun removeAll() {
        igniteCache?.removeAllAsync()?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.removeAll() }
    }

    override suspend fun removeAll(keys: Set<K>) {
        igniteCache?.removeAllAsync(keys)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.removeAll(keys) }
    }

    override suspend fun replace(key: K, oldValue: V, newValue: V): Boolean =
        igniteCache?.replaceAsync(key, oldValue, newValue)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.replace(key, oldValue, newValue) }

    override suspend fun replace(key: K, value: V): Boolean =
        igniteCache?.replaceAsync(key, value)?.awaitIgnite()
            ?: withContext(Dispatchers.IO) { cache.replace(key, value) }

    override fun registerCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        cache.registerCacheEntryListener(configuration)
    }

    override fun deregisterCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        cache.deregisterCacheEntryListener(configuration)
    }
}

private fun <T> IgniteFuture<T>.toCompletableFuture(): CompletableFuture<T> {
    val cf = CompletableFuture<T>()
    listen { f ->
        try {
            cf.complete(f.get())
        } catch (e: Throwable) {
            cf.completeExceptionally(e.cause ?: e)
        }
    }
    return cf
}

private suspend fun <T> IgniteFuture<T>.awaitIgnite(): T = toCompletableFuture().await()
