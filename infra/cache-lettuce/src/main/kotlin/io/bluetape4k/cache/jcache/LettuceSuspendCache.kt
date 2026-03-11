package io.bluetape4k.cache.jcache

import io.bluetape4k.exceptions.NotSupportedException
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.support.asLong
import io.bluetape4k.support.ifTrue
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.HSetExArgs
import io.lettuce.core.RedisCommandExecutionException
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import java.time.Duration
import javax.cache.configuration.CacheEntryListenerConfiguration

@OptIn(ExperimentalLettuceCoroutinesApi::class)
/**
 * Lettuce Redis hash를 기반으로 동작하는 [SuspendCache] 구현체입니다.
 *
 * ## 동작/계약
 * - 캐시 항목은 Redis hash(`cacheName`)에 `hset/hget/hdel` 계열 명령으로 저장/조회합니다.
 * - [ttlSeconds]가 지정되면 Redis 8+에서는 `HSETEX` 후 hash key `EXPIRE`를 함께 갱신하고, 미지원 서버에서는 `HSET/HMSET + EXPIRE`로 fallback 합니다.
 * - `close()`는 내부적으로 `clear()`를 수행해 Redis hash 키를 삭제합니다.
 * - CacheEntryListener 등록 API는 지원하지 않으며 호출 시 [NotSupportedException]을 발생시킵니다.
 *
 * ```kotlin
 * val cache = LettuceSuspendCache("users", commands, ttlSeconds = 60, cacheManager = manager)
 * cache.put("u:1", "debop")
 * val value = cache.get("u:1")
 * // value == "debop"
 * ```
 */
class LettuceSuspendCache<V: Any>(
    val cacheName: String,
    val commands: RedisCoroutinesCommands<String, V>,
    val ttlSeconds: Long? = null,
    val cacheManager: LettuceSuspendCacheManager,
    private val closeResource: () -> Unit = {},
): SuspendCache<String, V> {

    companion object: KLoggingChannel()

    private val closed = atomic(false)

    private val ttlDuration: Duration? by lazy { ttlSeconds?.let(Duration::ofSeconds) }

    override fun entries(): Flow<SuspendCacheEntry<String, V>> = channelFlow {
        commands
            .hkeys(cacheName)
            .chunked(100)
            .collect { keys ->
                commands
                    .hmget(cacheName, *keys.toTypedArray())
                    .collect {
                        send(SuspendCacheEntry(it.key, it.value))
                    }
            }
    }

    override suspend fun clear() {
        commands.del(cacheName)
    }

    override suspend fun close() {
        if (isClosed()) {
            return
        }

        if (closed.compareAndSet(expect = false, update = true)) {
            clear()
            runCatching { closeResource() }
        }
    }

    override fun isClosed(): Boolean = closed.value

    override suspend fun containsKey(key: String): Boolean {
        return commands.hexists(cacheName, key) ?: false
    }

    override suspend fun get(key: String): V? = commands.hget(cacheName, key)

    override fun getAll(): Flow<SuspendCacheEntry<String, V>> = entries()

    override fun getAll(keys: Set<String>): Flow<SuspendCacheEntry<String, V>> = flow {
        keys
            .chunked(100)
            .map { chunkedKeys ->
                commands.hmget(cacheName, *chunkedKeys.toTypedArray())
            }.forEach { entries ->
                emitAll(entries.map { SuspendCacheEntry(it.key, it.value) })
            }
    }

    override suspend fun getAndPut(key: String, value: V): V? {
        val cachedValue = commands.hget(cacheName, key)

        writeEntry(key, value)
        return cachedValue
    }

    override suspend fun getAndRemove(key: String): V? {
        val cachedValue = commands.hget(cacheName, key)
        commands.hdel(cacheName, key)
        return cachedValue
    }

    override suspend fun getAndReplace(key: String, value: V): V? {
        val cachedValue = commands.hget(cacheName, key)
        if (containsKey(key)) {
            writeEntry(key, value)
        }
        return cachedValue
    }

    override suspend fun put(key: String, value: V) {
        writeEntry(key, value)
    }

    override suspend fun putAll(map: Map<String, V>) {
        if (map.isEmpty()) return
        writeEntries(map)
    }

    override suspend fun putAllFlow(entries: Flow<Pair<String, V>>) {
        putAll(entries.toList().toMap())
    }

    override suspend fun putIfAbsent(key: String, value: V): Boolean {
        val added = commands.hsetnx(cacheName, key, value) ?: false
        if (added && ttlSeconds != null) {
            commands.expire(cacheName, ttlSeconds)
        }
        return added
    }

    override suspend fun remove(key: String): Boolean {
        return commands.hdel(cacheName, key).asLong() > 0L
    }

    override suspend fun remove(key: String, oldValue: V): Boolean {
        val cacheValue = commands.hget(cacheName, key)
        return if (containsKey(key) && cacheValue == oldValue) {
            commands.hdel(cacheName, key).asLong() > 0L
        } else {
            false
        }
    }

    override suspend fun removeAll() {
        commands.hkeys(cacheName)
            .chunked(100)
            .collect { keys ->
                commands.hdel(cacheName, *keys.toTypedArray())
            }
    }

    override suspend fun removeAll(keys: Set<String>) {
        commands.hdel(cacheName, *keys.toTypedArray())
    }

    override suspend fun replace(key: String, oldValue: V, newValue: V): Boolean {
        val cacheValue = commands.hget(cacheName, key)
        return if (containsKey(key) && cacheValue == oldValue) {
            runCatching {
                writeEntry(key, newValue)
            }.isSuccess
        } else {
            false
        }
    }

    override suspend fun replace(key: String, value: V): Boolean {
        return containsKey(key).ifTrue {
            runCatching {
                writeEntry(key, value)
            }.isSuccess
        } ?: false
    }

    override fun registerCacheEntryListener(configuration: CacheEntryListenerConfiguration<String, V>) {
        throw NotSupportedException("Lettuce 에서는 CacheEntryListener 를 등록할 수 없습니다.")
    }

    override fun deregisterCacheEntryListener(configuration: CacheEntryListenerConfiguration<String, V>) {
        throw NotSupportedException("Lettuce 에서는 CacheEntryListener 를 등록할 수 없습니다.")
    }

    private suspend fun writeEntry(key: String, value: V) {
        val ttl = ttlDuration
        if (ttl == null) {
            commands.hset(cacheName, key, value)
            return
        }

        val ttlArgs = HSetExArgs.Builder.ex(ttl)
        runCatching {
            commands.hsetex(cacheName, ttlArgs, mapOf(key to value))
            commands.expire(cacheName, ttl.seconds)
        }.recoverCatching { error ->
            if (error is RedisCommandExecutionException && error.message?.contains("unknown command", ignoreCase = true) == true) {
                commands.hset(cacheName, key, value)
                commands.expire(cacheName, ttl.seconds)
            } else {
                throw error
            }
        }.getOrThrow()
    }

    private suspend fun writeEntries(map: Map<String, V>) {
        val ttl = ttlDuration
        if (ttl == null) {
            commands.hmset(cacheName, map)
            return
        }

        val ttlArgs = HSetExArgs.Builder.ex(ttl)
        runCatching {
            commands.hsetex(cacheName, ttlArgs, map)
            commands.expire(cacheName, ttl.seconds)
        }.recoverCatching { error ->
            if (error is RedisCommandExecutionException && error.message?.contains("unknown command", ignoreCase = true) == true) {
                commands.hmset(cacheName, map)
                commands.expire(cacheName, ttl.seconds)
            } else {
                throw error
            }
        }.getOrThrow()
    }

}
