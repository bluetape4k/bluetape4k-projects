package io.bluetape4k.cache.jcache

import io.bluetape4k.io.serializer.BinarySerializer
import io.bluetape4k.io.serializer.BinarySerializers
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.redis.lettuce.map.LettuceMap
import java.util.concurrent.ConcurrentHashMap
import java.time.Duration
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
import javax.cache.integration.CompletionListener
import javax.cache.processor.EntryProcessor
import javax.cache.processor.EntryProcessorException
import javax.cache.processor.EntryProcessorResult
import javax.cache.processor.MutableEntry

/**
 * Lettuce Redis hash를 기반으로 동작하는 JCache [javax.cache.Cache] 구현체입니다.
 *
 * ## 동작/계약
 * - 캐시 항목은 [LettuceMap]을 통해 Redis hash에 `hset/hget/hdel` 계열 명령으로 저장/조회합니다.
 * - [ttlSeconds]가 지정되면 Redis 8+에서는 `HSETEX` 후 hash key `EXPIRE`를 함께 갱신하고, 미지원 서버에서는 `HSET/HMSET + EXPIRE`로 fallback 합니다.
 * - `close()`는 내부적으로 `clear()`를 수행해 Redis hash 키를 삭제합니다.
 * - 직렬화는 [serializer]로 처리하며, 기본값은 Fory 기반 직렬화입니다.
 */
class LettuceCache<K: Any, V: Any>(
    private val map: LettuceMap<ByteArray>,
    private val keyCodec: (K) -> String = { it.toString() },
    private val keyDecoder: ((String) -> K)? = null,
    private val serializer: BinarySerializer = BinarySerializers.Fory,
    private val ttlSeconds: Long? = null,
    private val cacheManager: LettuceCacheManager,
    private val configuration: Configuration<K, V>,
    private val closeResource: () -> Unit = {},
): Cache<K, V> {

    companion object: KLogging()

    private val cacheName: String get() = map.mapKey

    private var closed = false

    private val listeners = ConcurrentHashMap<CacheEntryListenerConfiguration<K, V>, CacheEntryListener<K, V>>()

    private val ttlDuration: Duration? by lazy { ttlSeconds?.let(Duration::ofSeconds) }

    private fun checkNotClosed() {
        check(!closed) { "LettuceCache[$cacheName]가 이미 닫혀 있습니다." }
    }

    private fun encodeKey(key: K): String = keyCodec(key)

    private fun decodeKey(field: String): K {
        keyDecoder?.let { return it(field) }
        if (configuration.keyType == String::class.java) {
            @Suppress("UNCHECKED_CAST")
            return field as K
        }
        throw javax.cache.CacheException(
            "LettuceCache는 non-String key iterator/entry 탐색을 위해 keyDecoder가 필요합니다. keyType=${configuration.keyType.name}"
        )
    }

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
        val bytes = map.get(encodeKey(key)) ?: return null
        return decodeValue(bytes)
    }

    override fun getAll(keys: MutableSet<out K>): MutableMap<K, V> {
        checkNotClosed()
        val result = mutableMapOf<K, V>()
        keys.chunked(100).forEach { chunk ->
            val fields = chunk.map { encodeKey(it) }
            val fetched = map.getAll(fields)
            chunk.forEach { k ->
                val bytes = fetched[encodeKey(k)]
                if (bytes != null) result[k] = decodeValue(bytes)
            }
        }
        return result
    }

    override fun containsKey(key: K): Boolean {
        checkNotClosed()
        return map.containsKey(encodeKey(key))
    }

    override fun put(key: K, value: V) {
        checkNotClosed()
        log.debug { "put: cacheName=$cacheName, key=$key" }
        val field = encodeKey(key)
        val bytes = encodeValue(value)
        val existed = map.containsKey(field)

        map.putTtl(field, bytes, ttlDuration)

        val eventType = if (existed) EventType.UPDATED else EventType.CREATED
        dispatchEvent(eventType, key, value)
    }

    override fun getAndPut(key: K, value: V): V? {
        checkNotClosed()
        val field = encodeKey(key)
        val oldBytes = map.get(field)
        val existed = oldBytes != null
        val bytes = encodeValue(value)

        map.putTtl(field, bytes, ttlDuration)

        val eventType = if (existed) EventType.UPDATED else EventType.CREATED
        dispatchEvent(eventType, key, value)

        return oldBytes?.let { decodeValue(it) }
    }

    override fun putAll(map: MutableMap<out K, out V>) {
        checkNotClosed()
        if (map.isEmpty()) return
        val encodedMap = map.entries.associate { (k, v) -> encodeKey(k) to encodeValue(v) }
        this.map.putAllTtl(encodedMap, ttlDuration)
        map.forEach { (k, v) -> dispatchEvent(EventType.CREATED, k, v) }
    }

    override fun putIfAbsent(key: K, value: V): Boolean {
        checkNotClosed()
        val field = encodeKey(key)
        val bytes = encodeValue(value)
        val added = map.putIfAbsentTtl(field, bytes, ttlDuration)
        if (added) {
            dispatchEvent(EventType.CREATED, key, value)
        }
        return added
    }

    override fun remove(key: K): Boolean {
        checkNotClosed()
        val field = encodeKey(key)
        val removed = map.remove(field) > 0L
        if (removed) {
            dispatchEvent(EventType.REMOVED, key, null)
        }
        return removed
    }

    override fun remove(key: K, oldValue: V): Boolean {
        checkNotClosed()
        val field = encodeKey(key)
        val currentBytes = map.get(field) ?: return false
        val currentValue = decodeValue(currentBytes)
        return if (currentValue == oldValue) {
            val removed = map.remove(field) > 0L
            if (removed) dispatchEvent(EventType.REMOVED, key, null)
            removed
        } else {
            false
        }
    }

    override fun getAndRemove(key: K): V? {
        checkNotClosed()
        val field = encodeKey(key)
        val bytes = map.get(field) ?: return null
        map.remove(field)
        dispatchEvent(EventType.REMOVED, key, null)
        return decodeValue(bytes)
    }

    override fun replace(key: K, oldValue: V, newValue: V): Boolean {
        checkNotClosed()
        val field = encodeKey(key)
        val currentBytes = map.get(field) ?: return false
        val currentValue = decodeValue(currentBytes)
        return if (currentValue == oldValue) {
            val bytes = encodeValue(newValue)
            map.putTtl(field, bytes, ttlDuration)
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
        map.putTtl(field, bytes, ttlDuration)
        dispatchEvent(EventType.UPDATED, key, value)
        return true
    }

    override fun getAndReplace(key: K, value: V): V? {
        checkNotClosed()
        val field = encodeKey(key)
        val oldBytes = map.get(field) ?: return null
        val bytes = encodeValue(value)
        map.putTtl(field, bytes, ttlDuration)
        dispatchEvent(EventType.UPDATED, key, value)
        return decodeValue(oldBytes)
    }

    override fun removeAll(keys: MutableSet<out K>) {
        checkNotClosed()
        if (keys.isEmpty()) return
        keys.forEach { key ->
            map.remove(encodeKey(key))
            dispatchEvent(EventType.REMOVED, key, null)
        }
    }

    override fun removeAll() {
        checkNotClosed()
        map.clear()
    }

    override fun clear() {
        checkNotClosed()
        map.clear()
    }

    override fun iterator(): MutableIterator<Cache.Entry<K, V>> {
        checkNotClosed()
        val allEntries = map.entries()
        val entries = mutableListOf<Cache.Entry<K, V>>()
        allEntries.forEach { (field, bytes) ->
            val key = decodeKey(field)
            val value = decodeValue(bytes)
            entries.add(object: Cache.Entry<K, V> {
                override fun getKey(): K = key
                override fun getValue(): V = value
                override fun <T: Any?> unwrap(clazz: Class<T>): T = clazz.cast(this)
            })
        }
        return entries.iterator()
    }

    override fun loadAll(
        keys: MutableSet<out K>?,
        replaceExistingValues: Boolean,
        completionListener: CompletionListener?,
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
        entryProcessor: EntryProcessor<K, V, T>,
        vararg arguments: Any?,
    ): T {
        checkNotClosed()
        val field = encodeKey(key)
        val entry = MutableEntryImpl(key, field)
        val result = try {
            entryProcessor.process(entry, *arguments)
        } catch (e: Exception) {
            throw EntryProcessorException(e)
        }
        entry.commit()
        return result
    }

    override fun <T: Any> invokeAll(
        keys: MutableSet<out K>,
        entryProcessor: EntryProcessor<K, V, T>,
        vararg arguments: Any?,
    ): MutableMap<K, EntryProcessorResult<T>> {
        checkNotClosed()
        val results = linkedMapOf<K, EntryProcessorResult<T>>()
        keys.forEach { key ->
            try {
                val value = invoke(key, entryProcessor, *arguments)
                results[key] = EntryProcessorResult { value }
            } catch (e: Exception) {
                results[key] = EntryProcessorResult { throw EntryProcessorException(e) }
            }
        }
        return results
    }

    override fun <T: Any?> unwrap(clazz: Class<T>): T {
        if (clazz.isAssignableFrom(javaClass)) return clazz.cast(this)
        throw IllegalArgumentException("Can't unwrap to $clazz")
    }

    override fun isClosed(): Boolean = closed

    override fun close() {
        if (closed) return
        closed = true
        runCatching { map.clear() }
        cacheManager.closeCache(this)
        runCatching { closeResource() }
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
                    else              -> {}
                }
            }
        }
    }

    private inner class MutableEntryImpl(
        private val key: K,
        private val field: String,
    ): MutableEntry<K, V> {
        private var originalLoaded = false
        private var originalValue: V? = null
        private var exists = false
        private var updatedValue: V? = null
        private var removeRequested = false

        override fun getKey(): K = key

        override fun getValue(): V? {
            ensureLoaded()
            return when {
                removeRequested -> null
                updatedValue != null -> updatedValue
                else -> originalValue
            }
        }

        override fun exists(): Boolean {
            ensureLoaded()
            return !removeRequested && (updatedValue != null || exists)
        }

        override fun remove() {
            ensureLoaded()
            updatedValue = null
            removeRequested = true
        }

        override fun setValue(value: V) {
            ensureLoaded()
            updatedValue = value
            removeRequested = false
        }

        override fun <T : Any?> unwrap(clazz: Class<T>): T = clazz.cast(this)

        fun commit() {
            ensureLoaded()
            when {
                removeRequested && exists -> this@LettuceCache.remove(key)
                removeRequested -> Unit
                updatedValue != null -> this@LettuceCache.put(key, updatedValue!!)
            }
        }

        private fun ensureLoaded() {
            if (!originalLoaded) {
                val bytes = map.get(field)
                originalValue = bytes?.let(::decodeValue)
                exists = bytes != null
                originalLoaded = true
            }
        }
    }
}
