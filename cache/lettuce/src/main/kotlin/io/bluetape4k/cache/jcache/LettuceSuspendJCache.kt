package io.bluetape4k.cache.jcache

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.requireNotBlank
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration
import javax.cache.event.CacheEntryListener

@OptIn(ExperimentalLettuceCoroutinesApi::class)
/**
 * Lettuce Redis hash를 기반으로 동작하는 [SuspendJCache] 구현체입니다.
 *
 * ## 동작/계약
 * - 캐시 항목은 Redis hash(`cacheName`)에 `hset/hget/hdel` 계열 명령으로 저장/조회합니다.
 * - `configuration.ttlSeconds` 가 지정되면 `supportsHSetEx` 여부에 따라 `HSETEX + EXPIRE`(Redis 8+) 또는 `HSET/HMSET + EXPIRE`(Redis 7 이하)로 동작합니다. 분기는 초기화 시점에 1회 결정됩니다.
 * - `close()`는 내부적으로 `clear()`를 수행해 Redis hash 키를 삭제합니다.
 * - [CacheEntryListener] 등록을 지원하며, 등록된 리스너에게 [Channel] 기반 비동기 이벤트를 전파합니다.
 *
 * ```kotlin
 * val cache = LettuceSuspendCache("users", commands, ttlSeconds = 60, cacheManager = manager)
 * cache.put("u:1", "debop")
 * val value = cache.get("u:1")
 * // value == "debop"
 * ```
 */
class LettuceSuspendJCache<V: Any>(private val cache: LettuceJCache<String, V>): SuspendJCache<String, V> {

    companion object: KLoggingChannel() {
        /**
         * [RedisClient] 인스턴스로 [LettuceSuspendJCache]를 생성합니다.
         *
         * ## 동작/계약
         * - [cacheName]이 blank면 `requireNotBlank("cacheName")`로 `IllegalArgumentException`이 발생합니다.
         * - 기존 캐시가 있으면 재사용하고 없으면 [configuration]으로 새 [LettuceJCache]를 생성합니다.
         * - 반환 객체는 동일 캐시 이름을 공유하는 [LettuceJCache] 인스턴스를 래핑합니다.
         *
         * ```kotlin
         * val cache = LettuceSuspendJCache<String>("users", redisClient, MutableConfiguration<String, String>())
         * // cache.isClosed() == false
         * ```
         */
        @JvmStatic
        inline operator fun <reified V: Any> invoke(
            cacheName: String,
            redisClient: RedisClient,
            configuration: Configuration<String, V> = MutableConfiguration(),
        ): LettuceSuspendJCache<V> {
            cacheName.requireNotBlank("cacheName")
            val jcache = LettuceJCaching.getOrCreate<String, V>(
                redisClient,
                cacheName,
                configuration
            ) as LettuceJCache<String, V>
            return LettuceSuspendJCache(jcache)
        }
    }

    val name: String get() = cache.name

    override fun entries(): Flow<SuspendJCacheEntry<String, V>> = flow {
        cache.asSequence().forEach {
            emit(SuspendJCacheEntry(it.key, it.value))
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.IO) { cache.clear() }
    }

    override suspend fun close() {
        withContext(Dispatchers.IO) { cache.close() }
    }

    override fun isClosed(): Boolean = cache.isClosed

    override suspend fun containsKey(key: String): Boolean =
        withContext(Dispatchers.IO) { cache.containsKey(key) }

    override suspend fun get(key: String): V? =
        withContext(Dispatchers.IO) { cache.get(key) }

    override fun getAll(): Flow<SuspendJCacheEntry<String, V>> = entries()

    override fun getAll(keys: Set<String>): Flow<SuspendJCacheEntry<String, V>> = flow {
        cache.getAll(keys).forEach { (key, value) ->
            emit(SuspendJCacheEntry(key, value))
        }
    }

    override suspend fun getAndPut(key: String, value: V): V? =
        get(key).also { put(key, value) }

    override suspend fun getAndRemove(key: String): V? =
        withContext(Dispatchers.IO) { cache.getAndRemove(key) }

    override suspend fun getAndReplace(key: String, value: V): V? =
        withContext(Dispatchers.IO) { cache.getAndReplace(key, value) }

    override suspend fun put(key: String, value: V) {
        withContext(Dispatchers.IO) { cache.put(key, value) }
    }

    override suspend fun putAll(map: Map<String, V>) {
        withContext(Dispatchers.IO) { cache.putAll(map) }
    }

    override suspend fun putAllFlow(entries: Flow<Pair<String, V>>) {
        entries.collect { put(it.first, it.second) }
    }

    override suspend fun putIfAbsent(key: String, value: V): Boolean =
        withContext(Dispatchers.IO) { cache.putIfAbsent(key, value) }

    override suspend fun remove(key: String): Boolean =
        withContext(Dispatchers.IO) { cache.remove(key) }

    override suspend fun remove(key: String, oldValue: V): Boolean =
        withContext(Dispatchers.IO) { cache.remove(key, oldValue) }

    override suspend fun removeAll() {
        withContext(Dispatchers.IO) { cache.removeAll() }
    }

    override suspend fun removeAll(keys: Set<String>) {
        withContext(Dispatchers.IO) { cache.removeAll(keys) }
    }

    override suspend fun replace(key: String, oldValue: V, newValue: V): Boolean =
        withContext(Dispatchers.IO) { cache.replace(key, oldValue, newValue) }

    override suspend fun replace(key: String, value: V): Boolean =
        withContext(Dispatchers.IO) { cache.replace(key, value) }

    override fun registerCacheEntryListener(configuration: CacheEntryListenerConfiguration<String, V>) {
        cache.registerCacheEntryListener(configuration)
    }

    override fun deregisterCacheEntryListener(configuration: CacheEntryListenerConfiguration<String, V>) {
        cache.deregisterCacheEntryListener(configuration)
    }
}
