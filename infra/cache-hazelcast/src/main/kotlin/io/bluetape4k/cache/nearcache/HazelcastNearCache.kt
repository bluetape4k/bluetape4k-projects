package io.bluetape4k.cache.nearcache

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import kotlinx.atomicfu.atomic
import java.util.UUID

/**
 * Hazelcast IMap + EntryListener 기반 Near Cache (2-tier cache) - 동기(Blocking) 구현.
 *
 * ## 아키텍처
 * ```
 * Application
 *     |
 * [HazelcastNearCache]
 *     |
 * +--------+--------+-----------+
 * |        |        |           |
 * Front   Back    Listener
 * Caffeine  IMap   EntryListener
 * (local) (remote)  (invalidation)
 * ```
 *
 * ## 동작 방식
 * - Read: front hit → return / front miss → IMap GET → front populate → return
 * - Write: front put + IMap PUT (write-through)
 * - Invalidation: IMap EntryListener → [CaffeineHazelcastLocalCache.invalidate]
 *
 * ## 직렬화 문제 해소
 * JCache `registerCacheEntryListener`는 리스너 factory를 서버로 직렬화해 전송하므로
 * non-serializable 리스너가 실패한다.
 * [IMap.addEntryListener]는 클라이언트 JVM에서 리스너를 실행하므로 직렬화가 불필요하다.
 *
 * @param V 값 타입 (키는 항상 String)
 */
class HazelcastNearCache<V : Any>(
    hazelcastInstance: HazelcastInstance,
    private val config: HazelcastNearCacheConfig = HazelcastNearCacheConfig(),
) : AutoCloseable {

    companion object : KLogging()

    private val closed = atomic(false)
    val isClosed: Boolean by closed

    val cacheName: String get() = config.cacheName

    @Suppress("UNCHECKED_CAST")
    private val imap: IMap<String, V> = hazelcastInstance.getMap(config.cacheName)

    private val frontCache: HazelcastLocalCache<String, V> = CaffeineHazelcastLocalCache(config)

    private val entryListener = HazelcastEntryEventListener(frontCache)
    private val listenerId: UUID = imap.addEntryListener(entryListener, true)

    /**
     * 키에 대한 값을 조회한다.
     * - front hit → return
     * - front miss → IMap GET → front populate → return
     */
    fun get(key: String): V? {
        key.requireNotBlank("key")

        frontCache.get(key)?.let { return it }
        return imap.get(key)?.also { frontCache.put(key, it) }
    }

    /**
     * 여러 키에 대한 값을 한 번에 조회한다.
     */
    fun getAll(keys: Set<String>): Map<String, V> {
        val result = frontCache.getAll(keys).toMutableMap()
        val missedKeys = keys - result.keys

        if (missedKeys.isNotEmpty()) {
            imap.getAll(missedKeys).forEach { (key, value) ->
                result[key] = value
                frontCache.put(key, value)
            }
        }

        return result
    }

    /**
     * key-value를 저장한다 (write-through).
     * front cache + IMap PUT.
     */
    fun put(key: String, value: V) {
        key.requireNotBlank("key")

        frontCache.put(key, value)
        imap.set(key, value)
    }

    /**
     * 여러 key-value를 한 번에 저장한다.
     */
    fun putAll(map: Map<out String, V>) {
        frontCache.putAll(map)
        imap.putAll(map)
    }

    /**
     * 해당 키가 없을 때만 저장한다 (put-if-absent).
     * @return 기존 값(있었으면) 또는 null(새로 저장됨)
     */
    fun putIfAbsent(key: String, value: V): V? {
        key.requireNotBlank("key")

        val existing = get(key)
        if (existing != null) return existing

        val prev = imap.putIfAbsent(key, value)
        return if (prev == null) {
            frontCache.put(key, value)
            null
        } else {
            prev
        }
    }

    /**
     * 키를 제거한다 (front + IMap).
     */
    fun remove(key: String) {
        key.requireNotBlank("key")

        frontCache.remove(key)
        imap.delete(key)
    }

    /**
     * 여러 키를 한 번에 제거한다.
     */
    fun removeAll(keys: Set<String>) {
        frontCache.removeAll(keys)
        keys.forEach { imap.delete(it) }
    }

    /**
     * 기존 값을 새 값으로 교체한다.
     * 키가 존재하는 경우에만 교체한다.
     * @return 교체 성공 여부
     */
    fun replace(key: String, value: V): Boolean {
        key.requireNotBlank("key")

        val replaced = imap.replace(key, value)
        return if (replaced != null) {
            frontCache.put(key, value)
            true
        } else {
            false
        }
    }

    /**
     * 기존 값이 oldValue와 같을 때만 newValue로 교체한다.
     */
    fun replace(key: String, oldValue: V, newValue: V): Boolean {
        key.requireNotBlank("key")

        val replaced = imap.replace(key, oldValue, newValue)
        if (replaced) {
            frontCache.put(key, newValue)
        }
        return replaced
    }

    /**
     * 조회 후 제거한다.
     */
    fun getAndRemove(key: String): V? {
        val value = get(key)
        if (value != null) {
            remove(key)
        }
        return value
    }

    /**
     * 조회 후 교체한다.
     */
    fun getAndReplace(key: String, value: V): V? {
        val existing = get(key) ?: return null
        put(key, value)
        return existing
    }

    /**
     * 해당 키가 캐시에 존재하는지 확인한다 (front or IMap).
     */
    fun containsKey(key: String): Boolean {
        if (frontCache.containsKey(key)) return true
        return imap.containsKey(key)
    }

    /**
     * 로컬 캐시만 비운다 (IMap 유지).
     */
    fun clearLocal() {
        frontCache.clear()
    }

    /**
     * 로컬 캐시 + IMap을 모두 비운다.
     */
    fun clearAll() {
        clearLocal()
        imap.clear()
    }

    /**
     * 로컬 캐시의 추정 크기.
     */
    fun localSize(): Long = frontCache.estimatedSize()

    /**
     * IMap(back-cache)의 크기.
     */
    fun backCacheSize(): Int = imap.size

    /**
     * 모든 리소스를 정리하고 리스너를 제거한다.
     */
    override fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            runCatching { imap.removeEntryListener(listenerId) }
            runCatching { frontCache.close() }
            log.debug { "HazelcastNearCache [${config.cacheName}] closed" }
        }
    }
}
