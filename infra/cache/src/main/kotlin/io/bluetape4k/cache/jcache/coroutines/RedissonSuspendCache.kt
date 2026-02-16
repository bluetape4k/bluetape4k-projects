package io.bluetape4k.cache.jcache.coroutines

import io.bluetape4k.cache.jcache.JCaching
import io.bluetape4k.cache.jcache.getDefaultJCacheConfiguration
import io.bluetape4k.coroutines.flow.extensions.toFastList
import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.joinAll
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.redisson.jcache.JCache
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.configuration.CompleteConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * Redisson 기반의 Coroutines 용 [SuspendCache] 구현체
 *
 * ```
 * val coCache = RedissonCoCache(
 *          "coroutine-cache-" + UUID.randomUUID().encodeBase62(),
 *          redisson,
 *          MutableConfiguration()
 * )
 * runBlocking {
 *   coCache.put("key", "value")
 *   val value = coCache.get("key")
 *   println(value)  // value
 * }
 * ```
 *
 * @param K key type
 * @param V value type
 * @property cache Redisson 기반의 [JCache] instance
 */
class RedissonSuspendCache<K: Any, V: Any>(private val cache: JCache<K, V>): SuspendCache<K, V> {

    companion object: KLoggingChannel() {
        @JvmStatic
        operator fun <K: Any, V: Any> invoke(
            cacheName: String,
            redisson: RedissonClient,
            configuration: Configuration<K, V> = MutableConfiguration(),
        ): RedissonSuspendCache<K, V> {
            cacheName.requireNotBlank("cacheName")
            val jcache = JCaching.Redisson.getOrCreateCache(cacheName, redisson, configuration) as JCache<K, V>
            return RedissonSuspendCache(jcache)
        }

        @JvmStatic
        inline operator fun <reified K: Any, reified V: Any> invoke(
            cacheName: String,
            config: Config,
            configuration: CompleteConfiguration<K, V> = getDefaultJCacheConfiguration(),
        ): RedissonSuspendCache<K, V> {
            cacheName.requireNotBlank("cacheName")
            val jcache = JCaching.Redisson.getOrCreate(cacheName, config, configuration) as JCache<K, V>
            return RedissonSuspendCache(jcache)
        }
    }

    override fun entries(): Flow<SuspendCacheEntry<K, V>> = flow {
        cache.asSequence().forEach {
            emit(SuspendCacheEntry(it.key, it.value))
        }
    }

    override suspend fun clear() {
        cache.clearAsync().awaitSuspending()
    }

    override suspend fun close() {
        cache.close()
    }

    override fun isClosed(): Boolean = cache.isClosed

    override suspend fun containsKey(key: K): Boolean {
        return cache.containsKeyAsync(key).awaitSuspending()
    }

    override suspend fun get(key: K): V? {
        return cache.getAsync(key).awaitSuspending()
    }

    override fun getAll(): Flow<SuspendCacheEntry<K, V>> {
        return entries()
    }

    override fun getAll(keys: Set<K>): Flow<SuspendCacheEntry<K, V>> = flow {
        cache.getAllAsync(keys).awaitSuspending().forEach { (key, value) ->
            emit(SuspendCacheEntry(key, value))
        }
    }

    override suspend fun getAndPut(key: K, value: V): V? {
        return get(key).apply { put(key, value) }
    }

    override suspend fun getAndRemove(key: K): V? {
        return cache.getAndRemoveAsync(key).awaitSuspending()
    }

    override suspend fun getAndReplace(key: K, value: V): V? {
        return cache.getAndReplaceAsync(key, value).awaitSuspending()
    }

    override suspend fun put(key: K, value: V) {
        cache.putAsync(key, value).awaitSuspending()
    }

    override suspend fun putAll(map: Map<K, V>) {
        cache.putAllAsync(map).awaitSuspending()
    }

    override suspend fun putAllFlow(entries: Flow<Pair<K, V>>) {
        entries
            .map { cache.putAsync(it.first, it.second).asDeferred() }
            .toFastList()
            .joinAll()
    }

    override suspend fun putIfAbsent(key: K, value: V): Boolean {
        return cache.putIfAbsentAsync(key, value).awaitSuspending()
    }

    override suspend fun remove(key: K): Boolean {
        return cache.removeAsync(key).awaitSuspending()
    }

    override suspend fun remove(key: K, oldValue: V): Boolean {
        return cache.removeAsync(key, oldValue).awaitSuspending()
    }

    override suspend fun removeAll() {
        cache.removeAll()
    }

    override suspend fun removeAll(keys: Set<K>) {
        cache.removeAllAsync(keys).awaitSuspending()
    }

    override suspend fun replace(key: K, oldValue: V, newValue: V): Boolean {
        return cache.replaceAsync(key, oldValue, newValue).awaitSuspending()
    }

    override suspend fun replace(key: K, value: V): Boolean {
        return cache.replaceAsync(key, value).awaitSuspending()
    }

    override fun registerCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        cache.registerCacheEntryListener(configuration)
    }

    override fun deregisterCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        cache.deregisterCacheEntryListener(configuration)
    }
}
