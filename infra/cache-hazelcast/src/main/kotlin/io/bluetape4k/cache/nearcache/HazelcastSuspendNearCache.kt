package io.bluetape4k.cache.nearcache

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.support.requireNotBlank
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Hazelcast IMap + EntryListener 기반 Near Cache (2-tier cache) - Coroutine(Suspend) 구현.
 *
 * ## 아키텍처
 * ```
 * Application (suspend)
 *     |
 * [HazelcastSuspendNearCache]
 *     |
 * +--------+--------+-----------+
 * |        |        |           |
 * Front   Back    Listener
 * Caffeine  IMap   EntryListener
 * (local) (remote)  (invalidation)
 * ```
 *
 * ## 동작 방식
 * - Read: front hit → return / front miss → IMap getAsync → front populate → return
 * - Write: front put + IMap setAsync (write-through)
 * - Invalidation: IMap EntryListener → [CaffeineHazelcastLocalCache.invalidate]
 *
 * @param V 값 타입 (키는 항상 String)
 */
class HazelcastSuspendNearCache<V : Any>(
    hazelcastInstance: HazelcastInstance,
    private val config: HazelcastNearCacheConfig = HazelcastNearCacheConfig(),
) : SuspendNearCacheOperations<V> {
    companion object : KLoggingChannel()

    private val closed = atomic(false)
    override val isClosed: Boolean by closed

    override val cacheName: String get() = config.cacheName

    @Suppress("UNCHECKED_CAST")
    private val imap: IMap<String, V> = hazelcastInstance.getMap(config.cacheName)

    private val frontCache: HazelcastLocalCache<String, V> = CaffeineHazelcastLocalCache(config)

    private val entryListener = HazelcastEntryEventListener(frontCache)
    private val listenerId: UUID = imap.addEntryListener(entryListener, true)

    // 백엔드 조회 통계 카운터
    private val backHitCount = atomic(0L)
    private val backMissCount = atomic(0L)

    /**
     * 키에 대한 값을 조회한다.
     * - front hit → return
     * - front miss → IMap getAsync → front populate → return
     */
    override suspend fun get(key: String): V? {
        key.requireNotBlank("key")

        frontCache.get(key)?.let { return it }
        val backValue = imap.getAsync(key).await()
        return if (backValue != null) {
            backHitCount.incrementAndGet()
            frontCache.put(key, backValue)
            backValue
        } else {
            backMissCount.incrementAndGet()
            null
        }
    }

    /**
     * 여러 키에 대한 값을 한 번에 조회한다.
     */
    override suspend fun getAll(keys: Set<String>): Map<String, V> {
        val result = frontCache.getAll(keys).toMutableMap()
        val missedKeys = keys - result.keys

        if (missedKeys.isNotEmpty()) {
            missedKeys.forEach { key ->
                val backValue = imap.getAsync(key).await()
                if (backValue != null) {
                    result[key] = backValue
                    frontCache.put(key, backValue)
                    backHitCount.incrementAndGet()
                } else {
                    backMissCount.incrementAndGet()
                }
            }
        }

        return result
    }

    /**
     * key-value를 저장한다 (write-through).
     * front cache + IMap setAsync.
     */
    override suspend fun put(
        key: String,
        value: V,
    ) {
        key.requireNotBlank("key")

        frontCache.put(key, value)
        imap.setAsync(key, value).await()
    }

    /**
     * 여러 key-value를 한 번에 저장한다.
     */
    override suspend fun putAll(entries: Map<String, V>) {
        frontCache.putAll(entries)
        val futures = entries.map { (key, value) -> imap.setAsync(key, value) }
        futures.forEach { it.await() }
    }

    /**
     * 해당 키가 없을 때만 저장한다 (put-if-absent).
     * @return 기존 값(있었으면) 또는 null(새로 저장됨)
     */
    override suspend fun putIfAbsent(
        key: String,
        value: V,
    ): V? {
        key.requireNotBlank("key")

        val existing = get(key)
        if (existing != null) return existing

        val prev = withContext(Dispatchers.IO) { imap.putIfAbsent(key, value) }
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
    override suspend fun remove(key: String) {
        key.requireNotBlank("key")

        frontCache.remove(key)
        imap.deleteAsync(key).await()
    }

    /**
     * 여러 키를 한 번에 제거한다.
     */
    override suspend fun removeAll(keys: Set<String>) {
        frontCache.removeAll(keys)
        val futures = keys.map { imap.deleteAsync(it) }
        futures.forEach { it.await() }
    }

    /**
     * 기존 값을 새 값으로 교체한다.
     * 키가 존재하는 경우에만 교체한다.
     * @return 교체 성공 여부
     */
    override suspend fun replace(
        key: String,
        value: V,
    ): Boolean {
        key.requireNotBlank("key")

        val replaced = withContext(Dispatchers.IO) { imap.replace(key, value) }
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
    override suspend fun replace(
        key: String,
        oldValue: V,
        newValue: V,
    ): Boolean {
        key.requireNotBlank("key")

        val replaced = withContext(Dispatchers.IO) { imap.replace(key, oldValue, newValue) }
        if (replaced) {
            frontCache.put(key, newValue)
        }
        return replaced
    }

    /**
     * 조회 후 제거한다.
     */
    override suspend fun getAndRemove(key: String): V? {
        val value = get(key)
        if (value != null) {
            remove(key)
        }
        return value
    }

    /**
     * 조회 후 교체한다.
     */
    override suspend fun getAndReplace(
        key: String,
        value: V,
    ): V? {
        val existing = get(key) ?: return null
        put(key, value)
        return existing
    }

    /**
     * 해당 키가 캐시에 존재하는지 확인한다 (front or IMap).
     */
    override suspend fun containsKey(key: String): Boolean {
        if (frontCache.containsKey(key)) return true
        return withContext(Dispatchers.IO) { imap.containsKey(key) }
    }

    /**
     * 로컬 캐시만 비운다 (IMap 유지).
     */
    override fun clearLocal() {
        frontCache.clear()
    }

    /**
     * 로컬 캐시 + IMap을 모두 비운다.
     */
    override suspend fun clearAll() {
        clearLocal()
        withContext(Dispatchers.IO) { imap.clear() }
    }

    /**
     * 로컬 캐시의 추정 크기.
     */
    override fun localCacheSize(): Long = frontCache.estimatedSize()

    /**
     * IMap(back-cache)의 크기.
     */
    override suspend fun backCacheSize(): Long = withContext(Dispatchers.IO) { imap.size.toLong() }

    /**
     * 캐시 통계를 반환한다.
     * Caffeine 로컬 통계와 백엔드 hit/miss 카운터를 합산한다.
     */
    override fun stats(): NearCacheStatistics {
        val caffeineStats = frontCache.stats()
        return DefaultNearCacheStatistics(
            localHits = caffeineStats?.hitCount() ?: 0L,
            localMisses = caffeineStats?.missCount() ?: 0L,
            localSize = frontCache.estimatedSize(),
            localEvictions = caffeineStats?.evictionCount() ?: 0L,
            backHits = backHitCount.value,
            backMisses = backMissCount.value
        )
    }

    /**
     * 모든 리소스를 정리하고 리스너를 제거한다.
     */
    override suspend fun close() {
        if (closed.compareAndSet(expect = false, update = true)) {
            runCatching { imap.removeEntryListener(listenerId) }
            runCatching { frontCache.close() }
            log.debug { "HazelcastSuspendNearCache [${config.cacheName}] closed" }
        }
    }
}
