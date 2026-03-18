package io.bluetape4k.cache.jcache

import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.trace
import io.bluetape4k.support.asLong
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.HSetExArgs
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import java.lang.reflect.Proxy
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import javax.cache.Cache
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.event.CacheEntryCreatedListener
import javax.cache.event.CacheEntryEvent
import javax.cache.event.CacheEntryEventFilter
import javax.cache.event.CacheEntryListener
import javax.cache.event.CacheEntryRemovedListener
import javax.cache.event.CacheEntryUpdatedListener
import javax.cache.event.EventType

@OptIn(ExperimentalLettuceCoroutinesApi::class)
/**
 * Lettuce Redis hash를 기반으로 동작하는 [SuspendJCache] 구현체입니다.
 *
 * ## 동작/계약
 * - 캐시 항목은 Redis hash(`cacheName`)에 `hset/hget/hdel` 계열 명령으로 저장/조회합니다.
 * - [ttlSeconds]가 지정되면 [supportsHSetEx] 여부에 따라 `HSETEX + EXPIRE`(Redis 8+) 또는 `HSET/HMSET + EXPIRE`(Redis 7 이하)로 동작합니다. 분기는 초기화 시점에 1회 결정됩니다.
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
class LettuceSuspendJCache<V: Any>(
    val cacheName: String,
    val commands: RedisCoroutinesCommands<String, V>,
    val ttlSeconds: Long? = null,
    val cacheManager: LettuceSuspendCacheManager,
    val supportsHSetEx: Boolean = false,
    private val closeResource: () -> Unit = {},
): SuspendJCache<String, V> {

    companion object: KLoggingChannel()

    private val closed = atomic(false)

    private val ttlDuration: Duration? by lazy { ttlSeconds?.let(Duration::ofSeconds) }

    // ── CacheEntryListener 지원 ──

    private val listeners =
        ConcurrentHashMap<CacheEntryListenerConfiguration<String, V>, CacheEntryListener<String, V>>()

    /**
     * 이벤트 전달용 Channel — UNLIMITED 버퍼로 send가 suspend되지 않습니다.
     */
    private val eventChannel = Channel<CacheEventData<V>>(Channel.UNLIMITED)

    /**
     * 이벤트 소비 전용 CoroutineScope — close() 시 취소됩니다.
     */
    private val eventScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * [CacheEntryEvent] 생성 시 필요한 JCache [Cache] 소스 프록시입니다.
     * [SuspendCacheEntryEventListener]는 source를 로깅용으로만 참조하므로 최소 프록시로 충분합니다.
     */
    @Suppress("UNCHECKED_CAST")
    private val eventSource: Cache<String, V> by lazy {
        Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(Cache::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getName" -> cacheName
                "toString" -> "LettuceSuspendCache($cacheName)"
                else -> null
            }
        } as Cache<String, V>
    }

    init {
        // Channel에서 이벤트를 읽어 등록된 리스너에게 전달
        eventScope.launch {
            for (event in eventChannel) {
                dispatchToListeners(event)
            }
        }
    }

    // ── SuspendCache 구현 ──

    override fun entries(): Flow<SuspendJCacheEntry<String, V>> = channelFlow {
        commands
            .hkeys(cacheName)
            .chunked(100)
            .collect { keys ->
                commands
                    .hmget(cacheName, *keys.toTypedArray())
                    .collect {
                        if (it.hasValue()) send(SuspendJCacheEntry(it.key, it.value))
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
            eventChannel.close()
            eventScope.cancel()
            clear()
            runCatching { closeResource() }
        }
    }

    override fun isClosed(): Boolean = closed.value

    override suspend fun containsKey(key: String): Boolean {
        return commands.hexists(cacheName, key) ?: false
    }

    override suspend fun get(key: String): V? = commands.hget(cacheName, key)

    override fun getAll(): Flow<SuspendJCacheEntry<String, V>> = entries()

    override fun getAll(keys: Set<String>): Flow<SuspendJCacheEntry<String, V>> = flow {
        keys
            .chunked(100)
            .map { chunkedKeys ->
                commands.hmget(cacheName, *chunkedKeys.toTypedArray())
            }.forEach { entries ->
                emitAll(entries.map { SuspendJCacheEntry(it.key, it.value) })
            }
    }

    override suspend fun getAndPut(key: String, value: V): V? {
        val cachedValue = commands.hget(cacheName, key)
        writeEntry(key, value)
        dispatchEvent(if (cachedValue != null) EventType.UPDATED else EventType.CREATED, key, value)
        return cachedValue
    }

    override suspend fun getAndRemove(key: String): V? {
        val cachedValue = commands.hget(cacheName, key) ?: return null
        commands.hdel(cacheName, key)
        dispatchEvent(EventType.REMOVED, key, null)
        return cachedValue
    }

    override suspend fun getAndReplace(key: String, value: V): V? {
        val cachedValue = commands.hget(cacheName, key) ?: return null
        writeEntry(key, value)
        dispatchEvent(EventType.UPDATED, key, value)
        return cachedValue
    }

    override suspend fun put(key: String, value: V) {
        writeEntry(key, value)
        dispatchEvent(EventType.CREATED, key, value)
    }

    override suspend fun putAll(map: Map<String, V>) {
        if (map.isEmpty()) return
        writeEntries(map)
        map.forEach { (key, value) ->
            dispatchEvent(EventType.CREATED, key, value)
        }
    }

    override suspend fun putAllFlow(entries: Flow<Pair<String, V>>) {
        putAll(entries.toList().toMap())
    }

    override suspend fun putIfAbsent(key: String, value: V): Boolean {
        val added = commands.hsetnx(cacheName, key, value) ?: false
        if (added) {
            if (ttlSeconds != null) {
                commands.expire(cacheName, ttlSeconds)
            }
            dispatchEvent(EventType.CREATED, key, value)
        }
        return added
    }

    override suspend fun remove(key: String): Boolean {
        val removed = commands.hdel(cacheName, key).asLong() > 0L
        if (removed) {
            dispatchEvent(EventType.REMOVED, key, null)
        }
        return removed
    }

    override suspend fun remove(key: String, oldValue: V): Boolean {
        val cacheValue = commands.hget(cacheName, key)
        return if (containsKey(key) && cacheValue == oldValue) {
            val removed = commands.hdel(cacheName, key).asLong() > 0L
            if (removed) {
                dispatchEvent(EventType.REMOVED, key, null)
            }
            removed
        } else {
            false
        }
    }

    override suspend fun removeAll() {
        commands.hkeys(cacheName)
            .chunked(100)
            .collect { keys ->
                commands.hdel(cacheName, *keys.toTypedArray())
                keys.forEach { key ->
                    dispatchEvent(EventType.REMOVED, key, null)
                }
            }
    }

    override suspend fun removeAll(keys: Set<String>) {
        commands.hdel(cacheName, *keys.toTypedArray())
        keys.forEach { key ->
            dispatchEvent(EventType.REMOVED, key, null)
        }
    }

    override suspend fun replace(key: String, oldValue: V, newValue: V): Boolean {
        val cacheValue = commands.hget(cacheName, key)
        return if (containsKey(key) && cacheValue == oldValue) {
            runCatching {
                writeEntry(key, newValue)
                dispatchEvent(EventType.UPDATED, key, newValue)
            }.isSuccess
        } else {
            false
        }
    }

    override suspend fun replace(key: String, value: V): Boolean {
        if (!containsKey(key)) return false
        writeEntry(key, value)
        dispatchEvent(EventType.UPDATED, key, value)
        return true
    }

    override fun registerCacheEntryListener(configuration: CacheEntryListenerConfiguration<String, V>) {
        val listener = configuration.cacheEntryListenerFactory?.create() ?: return
        @Suppress("UNCHECKED_CAST")
        listeners[configuration] = listener as CacheEntryListener<String, V>
        log.trace { "CacheEntryListener 등록 완료. cacheName=$cacheName, listener=${listener.javaClass.simpleName}" }
    }

    override fun deregisterCacheEntryListener(configuration: CacheEntryListenerConfiguration<String, V>) {
        listeners.remove(configuration)
    }

    // ── 이벤트 전파 ──

    private fun dispatchEvent(type: EventType, key: String, value: V?) {
        if (listeners.isNotEmpty()) {
            eventChannel.trySend(CacheEventData(type, key, value))
        }
    }

    private fun dispatchToListeners(event: CacheEventData<V>) {
        val cacheEntryEvent = object: CacheEntryEvent<String, V>(eventSource, event.type) {
            override fun getKey(): String = event.key
            override fun getValue(): V? = event.value
            override fun getOldValue(): V? = null
            override fun isOldValueAvailable(): Boolean = false
            override fun <T: Any?> unwrap(clazz: Class<T>): T = clazz.cast(this)
        }

        listeners.forEach { (cfg, listener) ->
            @Suppress("UNCHECKED_CAST")
            val filter = cfg.cacheEntryEventFilterFactory?.create() as? CacheEntryEventFilter<String, V>
            if (filter == null || filter.evaluate(cacheEntryEvent)) {
                runCatching {
                    when (event.type) {
                        EventType.CREATED -> (listener as? CacheEntryCreatedListener<String, V>)?.onCreated(
                            listOf(
                                cacheEntryEvent
                            )
                        )
                        EventType.UPDATED -> (listener as? CacheEntryUpdatedListener<String, V>)?.onUpdated(
                            listOf(
                                cacheEntryEvent
                            )
                        )
                        EventType.REMOVED -> (listener as? CacheEntryRemovedListener<String, V>)?.onRemoved(
                            listOf(
                                cacheEntryEvent
                            )
                        )
                        else              -> {}
                    }
                }
            }
        }
    }

    // ── 내부 유틸 ──

    private suspend fun writeEntry(key: String, value: V) {
        val ttl = ttlDuration
        if (ttl == null) {
            commands.hset(cacheName, key, value)
            return
        }

        if (supportsHSetEx) {
            commands.hsetex(cacheName, HSetExArgs.Builder.ex(ttl), mapOf(key to value))
        } else {
            commands.hset(cacheName, key, value)
        }
        commands.expire(cacheName, ttl.seconds)
    }

    private suspend fun writeEntries(map: Map<String, V>) {
        val ttl = ttlDuration
        if (ttl == null) {
            commands.hmset(cacheName, map)
            return
        }

        if (supportsHSetEx) {
            commands.hsetex(cacheName, HSetExArgs.Builder.ex(ttl), map)
        } else {
            commands.hmset(cacheName, map)
        }
        commands.expire(cacheName, ttl.seconds)
    }

    /**
     * Channel을 통해 전달되는 캐시 이벤트 데이터입니다.
     */
    private data class CacheEventData<V>(val type: EventType, val key: String, val value: V?)
}
