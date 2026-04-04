package io.bluetape4k.cache.jcache

import com.github.benmanes.caffeine.cache.AsyncCache
import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.future.await
import java.util.concurrent.CompletableFuture
import javax.cache.configuration.CacheEntryListenerConfiguration

/**
 * Caffeine 기반의 [com.github.benmanes.caffeine.cache.AsyncCache]를 이용하는 Coroutines 기반의 [SuspendJCache] 구현체입니다.
 *
 * ```kotlin
 * val coCache = CaffeineCoCache<String, Any> {
 *     expireAfterWrite(Duration.ofSeconds(60))
 *     maximumSize(100_000)
 * }
 *
 * runBlocking {
 *      coCache.put("key", "value")
 *      val value = coCache.get("key")
 *      println(value)  // value
 * }
 * ```
 *
 * @param K key type
 * @param V value type
 * @property cache caffeine [com.github.benmanes.caffeine.cache.AsyncCache] instance
 */
class CaffeineSuspendJCache<K: Any, V: Any>(private val cache: AsyncCache<K, V>): SuspendJCache<K, V> {

    companion object: KLoggingChannel() {
        /**
         * Caffeine AsyncCache 설정을 DSL로 지정하여 [CaffeineSuspendJCache]를 생성합니다.
         *
         * ```kotlin
         * val cache = CaffeineSuspendJCache<String, Int> {
         *     maximumSize(1000)
         *     expireAfterWrite(Duration.ofMinutes(10))
         * }
         * cache.put("hello", 5)
         * val value = cache.get("hello")
         * // value == 5
         * ```
         */
        @JvmStatic
        inline operator fun <reified K: Any, reified V: Any> invoke(
            builder: Caffeine<Any, Any>.() -> Unit = {},
        ): CaffeineSuspendJCache<K, V> {
            val asyncCache = Caffeine.newBuilder().apply(builder).buildAsync<K, V>()
            return CaffeineSuspendJCache(asyncCache)
        }
    }

    private val closed = atomic(false)

    override fun entries(): Flow<SuspendJCacheEntry<K, V>> = flow {
        cache.asMap().entries.forEach { (key, valueFuture) ->
            emit(SuspendJCacheEntry(key, valueFuture.await()))
        }
    }

    override suspend fun clear() {
        cache.synchronous().invalidateAll()
    }

    override suspend fun close() {
        if (!closed.compareAndSet(false, true)) return
        runCatching { cache.synchronous().invalidateAll() }
        runCatching { cache.synchronous().cleanUp() }
    }

    override fun isClosed(): Boolean = closed.value

    override suspend fun containsKey(key: K): Boolean {
        return cache.getIfPresent(key)?.await() != null
    }

    override suspend fun get(key: K): V? {
        return cache.getIfPresent(key)?.await()
    }

    override fun getAll(): Flow<SuspendJCacheEntry<K, V>> {
        return entries()
    }

    override fun getAll(keys: Set<K>): Flow<SuspendJCacheEntry<K, V>> = flow {
        keys.forEach { keyToLoad ->
            get(keyToLoad)?.let { emit(SuspendJCacheEntry(keyToLoad, it)) }
        }
    }

    override suspend fun getAndPut(key: K, value: V): V? {
        return get(key).apply { put(key, value) }
    }

    override suspend fun getAndRemove(key: K): V? {
        return cache.getIfPresent(key)?.await()
            .also { oldValue -> oldValue?.run { remove(key) } }
    }

    override suspend fun getAndReplace(key: K, value: V): V? {
        return cache.getIfPresent(key)?.await()?.apply { put(key, value) }
    }

    override suspend fun put(key: K, value: V) {
        cache.put(key, CompletableFuture.completedFuture(value))
    }

    override suspend fun putAll(map: Map<K, V>) {
        cache.synchronous().putAll(map)
    }

    override suspend fun putAllFlow(entries: Flow<Pair<K, V>>) {
        entries.onEach { put(it.first, it.second) }.collect()
    }

    override suspend fun putIfAbsent(key: K, value: V): Boolean {
        return cache.synchronous().asMap().putIfAbsent(key, value) == null
    }

    override suspend fun remove(key: K): Boolean {
        if (containsKey(key)) {
            cache.synchronous().invalidate(key)
            return true
        }
        return false
    }

    override suspend fun remove(key: K, oldValue: V): Boolean {
        return cache.synchronous().asMap().remove(key, oldValue)
    }

    override suspend fun removeAll() {
        cache.synchronous().invalidateAll()
    }

    override suspend fun removeAll(keys: Set<K>) {
        cache.synchronous().invalidateAll(keys)
    }

    override suspend fun replace(key: K, oldValue: V, newValue: V): Boolean {
        return cache.synchronous().asMap().replace(key, oldValue, newValue)
    }

    override suspend fun replace(key: K, value: V): Boolean {
        return cache.synchronous().asMap().replace(key, value) != null
    }

    override fun registerCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        log.debug { "Local Cache에서는 Listener가 필요없습니다." }
    }

    override fun deregisterCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        // Nothing to do
    }
}
