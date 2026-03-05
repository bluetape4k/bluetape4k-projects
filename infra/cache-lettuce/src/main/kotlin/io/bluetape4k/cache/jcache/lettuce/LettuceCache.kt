package io.bluetape4k.cache.jcache.lettuce

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.lettuce.core.HSetExArgs
import io.lettuce.core.api.sync.RedisCommands
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import javax.cache.Cache
import javax.cache.CacheManager
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.configuration.Configuration
import javax.cache.event.CacheEntryCreatedListener
import javax.cache.event.CacheEntryEvent
import javax.cache.event.CacheEntryEventFilter
import javax.cache.event.CacheEntryListener
import javax.cache.event.CacheEntryRemovedListener
import javax.cache.event.CacheEntryUpdatedListener
import javax.cache.event.EventType

/**
 * Lettuce Redis hash를 기반으로 동작하는 JCache [Cache] 구현체입니다.
 *
 * ## 동작/계약
 * - 캐시 항목은 Redis hash(`cacheName`)에 `hset/hget/hdel` 계열 명령으로 저장/조회합니다.
 * - [ttlSeconds]가 지정되면 `hsetex`를 사용해 항목 갱신 시 TTL을 함께 설정합니다.
 * - `close()`는 내부적으로 `clear()`를 수행해 Redis hash 키를 삭제합니다.
 * - 직렬화는 [serializer]로 처리하며, 기본값은 Fory 기반 직렬화입니다.
 */
class LettuceCache<K: Any, V: Any>(
    private val cacheName: String,
    private val commands: RedisCommands<String, ByteArray>,
    private val keyCodec: (K) -> String = { it.toString() },
    private val serializer: BinarySerializer = BinarySerializers.Fory,
    private val ttlSeconds: Long? = null,
    private val cacheManager: LettuceCacheManager,
    private val configuration: Configuration<K, V>,
): Cache<K, V> {

    companion object: KLogging()

    private var closed = false

    private val listeners = ConcurrentHashMap<CacheEntryListenerConfiguration<K, V>, CacheEntryListener<K, V>>()

    private val hsetExArgs: HSetExArgs? by lazy {
        ttlSeconds?.let { HSetExArgs.Builder.ex(Duration.ofSeconds(it)) }
    }

    private fun checkNotClosed() {
        check(!closed) { "LettuceCache[$cacheName]가 이미 닫혀 있습니다." }
    }

    private fun encodeKey(key: K): String = keyCodec(key)

    private fun encodeValue(value: V): ByteArray = serializer.serialize(value)

    @Suppress("UNCHECKED_CAST")
    private fun decodeValue(bytes: ByteArray): V = serializer.deserialize<V>(bytes)!!

    override fun getName(): String = cacheName

    override fun getCacheManager(): CacheManager = cacheManager

    override fun <C: Configuration<K, V>> getConfiguration(clazz: Class<C>): C {
        @Suppress("UNCHECKED_CAST")
        return configuration as C
    }

    override fun get(key: K): V? {
        checkNotClosed()
        log.debug { "get: cacheName=$cacheName, key=$key" }
        val bytes = commands.hget(cacheName, encodeKey(key)) ?: return null
        return decodeValue(bytes)
    }

    override fun getAll(keys: MutableSet<out K>): MutableMap<K, V> {
        checkNotClosed()
        val result = mutableMapOf<K, V>()
        keys.chunked(100).forEach { chunk ->
            val fields = chunk.map { encodeKey(it) }.toTypedArray()
            commands.hmget(cacheName, *fields).forEach { kv ->
                if (!kv.isEmpty) {
                    val originalKey = chunk.firstOrNull { encodeKey(it) == kv.key }
                    if (originalKey != null) {
                        result[originalKey] = decodeValue(kv.value)
                    }
                }
            }
        }
        return result
    }

    override fun containsKey(key: K): Boolean {
        checkNotClosed()
        return commands.hexists(cacheName, encodeKey(key)) ?: false
    }

    override fun put(key: K, value: V) {
        checkNotClosed()
        log.debug { "put: cacheName=$cacheName, key=$key" }
        val field = encodeKey(key)
        val bytes = encodeValue(value)
        val existed = commands.hexists(cacheName, field) ?: false

        if (hsetExArgs != null) {
            commands.hsetex(cacheName, hsetExArgs!!, mapOf(field to bytes))
        } else {
            commands.hset(cacheName, field, bytes)
        }

        val eventType = if (existed) EventType.UPDATED else EventType.CREATED
        dispatchEvent(eventType, key, value)
    }

    override fun getAndPut(key: K, value: V): V? {
        checkNotClosed()
        val field = encodeKey(key)
        val oldBytes = commands.hget(cacheName, field)
        val existed = oldBytes != null
        val bytes = encodeValue(value)

        if (hsetExArgs != null) {
            commands.hsetex(cacheName, hsetExArgs!!, mapOf(field to bytes))
        } else {
            commands.hset(cacheName, field, bytes)
        }

        val eventType = if (existed) EventType.UPDATED else EventType.CREATED
        dispatchEvent(eventType, key, value)

        return oldBytes?.let { decodeValue(it) }
    }

    override fun putAll(map: MutableMap<out K, out V>) {
        checkNotClosed()
        if (map.isEmpty()) return
        val encodedMap = map.entries.associate { (k, v) -> encodeKey(k) to encodeValue(v) }
        if (hsetExArgs != null) {
            commands.hsetex(cacheName, hsetExArgs!!, encodedMap)
        } else {
            commands.hmset(cacheName, encodedMap)
        }
        map.forEach { (k, v) -> dispatchEvent(EventType.CREATED, k, v) }
    }

    override fun putIfAbsent(key: K, value: V): Boolean {
        checkNotClosed()
        val field = encodeKey(key)
        val bytes = encodeValue(value)
        val added = commands.hsetnx(cacheName, field, bytes) ?: false
        if (added) {
            dispatchEvent(EventType.CREATED, key, value)
        }
        return added
    }

    override fun remove(key: K): Boolean {
        checkNotClosed()
        val field = encodeKey(key)
        val removed = (commands.hdel(cacheName, field) ?: 0L) > 0L
        if (removed) {
            dispatchEvent(EventType.REMOVED, key, null)
        }
        return removed
    }

    override fun remove(key: K, oldValue: V): Boolean {
        checkNotClosed()
        val field = encodeKey(key)
        val currentBytes = commands.hget(cacheName, field) ?: return false
        val currentValue = decodeValue(currentBytes)
        return if (currentValue == oldValue) {
            val removed = (commands.hdel(cacheName, field) ?: 0L) > 0L
            if (removed) dispatchEvent(EventType.REMOVED, key, null)
            removed
        } else {
            false
        }
    }

    override fun getAndRemove(key: K): V? {
        checkNotClosed()
        val field = encodeKey(key)
        val bytes = commands.hget(cacheName, field) ?: return null
        commands.hdel(cacheName, field)
        dispatchEvent(EventType.REMOVED, key, null)
        return decodeValue(bytes)
    }

    override fun replace(key: K, oldValue: V, newValue: V): Boolean {
        checkNotClosed()
        val field = encodeKey(key)
        val currentBytes = commands.hget(cacheName, field) ?: return false
        val currentValue = decodeValue(currentBytes)
        return if (currentValue == oldValue) {
            val bytes = encodeValue(newValue)
            if (hsetExArgs != null) {
                commands.hsetex(cacheName, hsetExArgs!!, mapOf(field to bytes))
            } else {
                commands.hset(cacheName, field, bytes)
            }
            dispatchEvent(EventType.UPDATED, key, newValue)
            true
        } else {
            false
        }
    }

    override fun replace(key: K, value: V): Boolean {
        checkNotClosed()
        if (!containsKey(key)) return false
        val field = encodeKey(key)
        val bytes = encodeValue(value)
        if (hsetExArgs != null) {
            commands.hsetex(cacheName, hsetExArgs!!, mapOf(field to bytes))
        } else {
            commands.hset(cacheName, field, bytes)
        }
        dispatchEvent(EventType.UPDATED, key, value)
        return true
    }

    override fun getAndReplace(key: K, value: V): V? {
        checkNotClosed()
        val field = encodeKey(key)
        val oldBytes = commands.hget(cacheName, field) ?: return null
        val bytes = encodeValue(value)
        if (hsetExArgs != null) {
            commands.hsetex(cacheName, hsetExArgs!!, mapOf(field to bytes))
        } else {
            commands.hset(cacheName, field, bytes)
        }
        dispatchEvent(EventType.UPDATED, key, value)
        return decodeValue(oldBytes)
    }

    override fun removeAll(keys: MutableSet<out K>) {
        checkNotClosed()
        if (keys.isEmpty()) return
        val fields = keys.map { encodeKey(it) }.toTypedArray()
        commands.hdel(cacheName, *fields)
        keys.forEach { dispatchEvent(EventType.REMOVED, it, null) }
    }

    override fun removeAll() {
        checkNotClosed()
        commands.del(cacheName)
    }

    override fun clear() {
        checkNotClosed()
        commands.del(cacheName)
    }

    override fun iterator(): MutableIterator<Cache.Entry<K, V>> {
        checkNotClosed()
        val allFields = commands.hkeys(cacheName) ?: emptyList()
        val entries = mutableListOf<Cache.Entry<K, V>>()
        allFields.chunked(100).forEach { chunk ->
            val fields = chunk.toTypedArray()
            commands.hmget(cacheName, *fields).forEach { kv ->
                if (!kv.isEmpty) {
                    @Suppress("UNCHECKED_CAST")
                    val key = kv.key as K
                    val value = decodeValue(kv.value)
                    entries.add(object: Cache.Entry<K, V> {
                        override fun getKey(): K = key
                        override fun getValue(): V = value
                        override fun <T: Any?> unwrap(clazz: Class<T>): T = clazz.cast(this)
                    })
                }
            }
        }
        return entries.iterator()
    }

    override fun loadAll(
        keys: MutableSet<out K>?,
        replaceExistingValues: Boolean,
        completionListener: javax.cache.integration.CompletionListener?,
    ) {
        completionListener?.onCompletion()
    }

    override fun registerCacheEntryListener(cacheEntryListenerConfiguration: CacheEntryListenerConfiguration<K, V>) {
        checkNotClosed()
        val listener = cacheEntryListenerConfiguration.cacheEntryListenerFactory?.create()
        if (listener != null) {
            @Suppress("UNCHECKED_CAST")
            listeners[cacheEntryListenerConfiguration] = listener as CacheEntryListener<K, V>
        }
    }

    override fun deregisterCacheEntryListener(cacheEntryListenerConfiguration: CacheEntryListenerConfiguration<K, V>) {
        listeners.remove(cacheEntryListenerConfiguration)
    }

    override fun <T: Any> invoke(
        key: K,
        entryProcessor: javax.cache.processor.EntryProcessor<K, V, T>,
        vararg arguments: Any?,
    ): T {
        throw UnsupportedOperationException("invoke는 지원하지 않습니다.")
    }

    override fun <T: Any> invokeAll(
        keys: MutableSet<out K>,
        entryProcessor: javax.cache.processor.EntryProcessor<K, V, T>,
        vararg arguments: Any?,
    ): MutableMap<K, javax.cache.processor.EntryProcessorResult<T>> {
        throw UnsupportedOperationException("invokeAll은 지원하지 않습니다.")
    }

    override fun <T: Any?> unwrap(clazz: Class<T>): T {
        if (clazz.isAssignableFrom(javaClass)) return clazz.cast(this)
        throw IllegalArgumentException("Can't unwrap to $clazz")
    }

    override fun isClosed(): Boolean = closed

    override fun close() {
        if (closed) return
        closed = true
        runCatching { commands.del(cacheName) }
        cacheManager.closeCache(this)
    }

    private fun dispatchEvent(eventType: EventType, key: K, value: V?) {
        if (listeners.isEmpty()) return
        listeners.forEach { (cfg, listener) ->
            val event = object: CacheEntryEvent<K, V>(this, eventType) {
                override fun getKey(): K = key
                override fun getValue(): V? = value
                override fun getOldValue(): V? = null
                override fun isOldValueAvailable(): Boolean = false
                override fun <T: Any?> unwrap(clazz: Class<T>): T = clazz.cast(this)
            }
            @Suppress("UNCHECKED_CAST")
            val filter = cfg.cacheEntryEventFilterFactory?.create() as? CacheEntryEventFilter<K, V>
            if (filter == null || filter.evaluate(event)) {
                when (eventType) {
                    EventType.CREATED -> (listener as? CacheEntryCreatedListener<K, V>)?.onCreated(listOf(event))
                    EventType.UPDATED -> (listener as? CacheEntryUpdatedListener<K, V>)?.onUpdated(listOf(event))
                    EventType.REMOVED -> (listener as? CacheEntryRemovedListener<K, V>)?.onRemoved(listOf(event))
                    else -> {}
                }
            }
        }
    }
}
