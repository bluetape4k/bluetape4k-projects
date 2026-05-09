package io.bluetape4k.cache.jcache

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
import org.redisson.api.RedissonClient
import org.redisson.config.Config
import org.redisson.jcache.JCache
import org.redisson.jcache.JCachingProvider
import org.redisson.jcache.configuration.RedissonConfiguration
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.configuration.CompleteConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration

/**
 * Redisson JCache를 코루틴 기반 [SuspendJCache]로 감싼 구현체입니다.
 *
 * ## 동작/계약
 * - 조회/갱신 연산은 Redisson `*Async()` API를 호출한 뒤 `await()`으로 대기합니다.
 * - `putAllFlow`는 각 put 비동기 작업을 모아 `joinAll()`로 완료를 보장합니다.
 * - 캐시 데이터 저장소는 외부 [cache] 인스턴스를 그대로 사용합니다.
 *
 * ```kotlin
 * val cache = RedissonSuspendCache<String, String>("users", redisson)
 * cache.put("u:1", "debop")
 * val value = cache.get("u:1")
 * // value == "debop"
 * ```
 */
class RedissonSuspendJCache<K: Any, V: Any>(private val cache: JCache<K, V>): SuspendJCache<K, V> {

    companion object: KLoggingChannel() {
        /**
         * RedissonClient 인스턴스로 [RedissonSuspendJCache]를 생성합니다.
         *
         * ## 동작/계약
         * - [cacheName]이 blank면 `requireNotBlank("cacheName")`로 `IllegalArgumentException`이 발생합니다.
         * - 기존 캐시가 있으면 재사용하고 없으면 [configuration]을 감싼 Redisson 설정으로 생성합니다.
         * - 반환 객체는 동일 캐시 이름을 공유하는 JCache 인스턴스를 래핑합니다.
         *
         * ```kotlin
         * val cache = RedissonSuspendCache("users", redisson, MutableConfiguration<String, String>())
         * // cache.isClosed() == false
         * ```
         */
        @JvmStatic
        operator fun <K: Any, V: Any> invoke(
            cacheName: String,
            redisson: RedissonClient,
            configuration: Configuration<K, V> = MutableConfiguration(),
        ): RedissonSuspendJCache<K, V> {
            cacheName.requireNotBlank("cacheName")
            val manager = jcacheManager<JCachingProvider>()
            val redissonCfg = RedissonConfiguration.fromInstance(redisson, configuration)
            val jcache = (manager.getCache(cacheName, configuration.keyType, configuration.valueType)
                ?: manager.createCache(cacheName, redissonCfg)) as JCache<K, V>
            return RedissonSuspendJCache(jcache)
        }

        /**
         * Redisson [Config]로 캐시를 생성하거나 재사용합니다.
         *
         * ## 동작/계약
         * - [cacheName] blank 입력은 `IllegalArgumentException`을 발생시킵니다.
         * - [configuration]의 키/값 타입 정보를 기반으로 캐시를 조회/생성합니다.
         * - 같은 이름 캐시가 이미 존재하면 해당 캐시를 재사용합니다.
         *
         * ```kotlin
         * val cache = RedissonSuspendCache<String, Int>("scores", config)
         * cache.put("u1", 10)
         * // cache.get("u1") == 10
         * ```
         */
        @JvmStatic
        inline operator fun <reified K: Any, reified V: Any> invoke(
            cacheName: String,
            config: Config,
            configuration: CompleteConfiguration<K, V> = getDefaultJCacheConfiguration(),
        ): RedissonSuspendJCache<K, V> {
            cacheName.requireNotBlank("cacheName")
            val manager = jcacheManager<JCachingProvider>()
            val redissonCfg = RedissonConfiguration.fromConfig(config, configuration)
            val jcache = (manager.getCache(cacheName, K::class.java, V::class.java)
                ?: manager.createCache(cacheName, redissonCfg)) as JCache<K, V>
            return RedissonSuspendJCache(jcache)
        }
    }

    override fun entries(): Flow<SuspendJCacheEntry<K, V>> = flow {
        cache.asSequence().forEach {
            emit(SuspendJCacheEntry(it.key, it.value))
        }
    }

    override suspend fun clear() {
        cache.clearAsync().await()
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) { runCatching { cache.close() } }
    }

    override fun isClosed(): Boolean = cache.isClosed

    override suspend fun containsKey(key: K): Boolean {
        return cache.containsKeyAsync(key).await()
    }

    override suspend fun get(key: K): V? {
        return cache.getAsync(key).await()
    }

    override fun getAll(): Flow<SuspendJCacheEntry<K, V>> {
        return entries()
    }

    override fun getAll(keys: Set<K>): Flow<SuspendJCacheEntry<K, V>> = flow {
        cache.getAllAsync(keys).await().forEach { (key, value) ->
            emit(SuspendJCacheEntry(key, value))
        }
    }

    /**
     * [key]의 현재 값을 반환하고 [value]로 교체합니다.
     *
     * **참고**: Redisson JCache는 `getAndPutAsync()`를 제공하지 않습니다.
     * 현재 구현은 get → put 두 단계로 동작하므로 원자적이지 않습니다.
     * 엄격한 원자성이 필요하면 Redisson `RMap.getAndSetAsync()`를 직접 사용하세요.
     */
    override suspend fun getAndPut(key: K, value: V): V? {
        return get(key).apply { put(key, value) }
    }

    override suspend fun getAndRemove(key: K): V? {
        return cache.getAndRemoveAsync(key).await()
    }

    override suspend fun getAndReplace(key: K, value: V): V? {
        return cache.getAndReplaceAsync(key, value).await()
    }

    override suspend fun put(key: K, value: V) {
        cache.putAsync(key, value).await()
    }

    override suspend fun putAll(map: Map<K, V>) {
        cache.putAllAsync(map).await()
    }

    override suspend fun putAllFlow(entries: Flow<Pair<K, V>>) {
        entries
            .map { cache.putAsync(it.first, it.second).asDeferred() }
            .toList()
            .joinAll()
    }

    override suspend fun putIfAbsent(key: K, value: V): Boolean {
        return cache.putIfAbsentAsync(key, value).await()
    }

    override suspend fun remove(key: K): Boolean {
        return cache.removeAsync(key).await()
    }

    override suspend fun remove(key: K, oldValue: V): Boolean {
        return cache.removeAsync(key, oldValue).await()
    }

    override suspend fun removeAll() {
        withContext(Dispatchers.IO) { cache.removeAll() }
    }

    override suspend fun removeAll(keys: Set<K>) {
        cache.removeAllAsync(keys).await()
    }

    override suspend fun replace(key: K, oldValue: V, newValue: V): Boolean {
        return cache.replaceAsync(key, oldValue, newValue).await()
    }

    override suspend fun replace(key: K, value: V): Boolean {
        return cache.replaceAsync(key, value).await()
    }

    override fun registerCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        cache.registerCacheEntryListener(configuration)
    }

    override fun deregisterCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        cache.deregisterCacheEntryListener(configuration)
    }
}
