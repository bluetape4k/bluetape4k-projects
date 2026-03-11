package io.bluetape4k.cache.jcache

import com.hazelcast.cache.ICache
import com.hazelcast.core.HazelcastInstance
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireNotBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
 * Hazelcast JCache를 코루틴 친화적인 [SuspendCache]로 감싼 구현체입니다.
 *
 * ## 동작/계약
 * - `cache.unwrap(ICache::class.java)`가 성공하면 Hazelcast 비동기 API를 우선 사용합니다.
 * - unwrap 실패 시 표준 JCache 동기 API를 `Dispatchers.IO`에서 실행합니다.
 * - 캐시 엔트리 저장소는 외부 `cache` 인스턴스를 그대로 사용하며 새 저장소를 만들지 않습니다.
 *
 * ```kotlin
 * val cache = HazelcastSuspendCache<String, String>("users")
 * cache.put("u:1", "debop")
 * val value = cache.get("u:1")
 * // value == "debop"
 * ```
 */
class HazelcastSuspendCache<K: Any, V: Any>(private val cache: JCache<K, V>): SuspendCache<K, V> {

    companion object: KLoggingChannel() {
        /**
         * 캐시 이름과 구성으로 [HazelcastSuspendCache]를 생성합니다.
         *
         * ## 동작/계약
         * - [cacheName]이 blank면 `requireNotBlank("cacheName")`로 `IllegalArgumentException`이 발생합니다.
         * - 동일 이름 캐시가 있으면 재사용하고, 없으면 [configuration]으로 새 캐시를 생성합니다.
         * - 반환 객체는 JCache 인스턴스를 공유하며 호출마다 새 래퍼를 생성합니다.
         *
         * ```kotlin
         * val cache = HazelcastSuspendCache("users", MutableConfiguration<String, String>())
         * // cache.isClosed() == false
         * ```
         */
        @JvmStatic
        operator fun <K: Any, V: Any> invoke(
            hazelcastInstance: HazelcastInstance,
            cacheName: String,
            configuration: Configuration<K, V> = MutableConfiguration(),
        ): HazelcastSuspendCache<K, V> {
            cacheName.requireNotBlank("cacheName")
            val manager = HazelcastJCaching.cacheManagerOf(hazelcastInstance)
            val jcache = manager.getCache(cacheName, configuration.keyType, configuration.valueType)
                ?: manager.createCache(cacheName, configuration)
            return HazelcastSuspendCache(jcache)
        }

        /**
         * reified 타입으로 키/값 타입이 지정된 [HazelcastSuspendCache]를 생성합니다.
         *
         * ## 동작/계약
         * - [cacheName] blank 입력은 `IllegalArgumentException`을 발생시킵니다.
         * - `MutableConfiguration#setTypes`로 `K`, `V` 타입 정보를 강제합니다.
         * - 기존 캐시가 있으면 재사용, 없으면 타입 정보가 포함된 설정으로 생성합니다.
         *
         * ```kotlin
         * val cache = HazelcastSuspendCache<String, Int>(hazelcastInstance, "scores")
         * cache.put("u1", 10)
         * // cache.get("u1") == 10
         * ```
         */
        @JvmStatic
        inline operator fun <reified K: Any, reified V: Any> invoke(
            hazelcastInstance: HazelcastInstance,
            cacheName: String,
        ): HazelcastSuspendCache<K, V> {
            cacheName.requireNotBlank("cacheName")
            val manager = HazelcastJCaching.cacheManagerOf(hazelcastInstance)
            val config = MutableConfiguration<K, V>().apply {
                setTypes(K::class.java, V::class.java)
            }
            val jcache = manager.getCache(cacheName, K::class.java, V::class.java)
                ?: manager.createCache(cacheName, config)
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
