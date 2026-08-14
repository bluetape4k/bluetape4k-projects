package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.jcache.SuspendJCache
import io.bluetape4k.cache.jcache.SuspendJCacheEntry
import io.bluetape4k.cache.jcache.SuspendJCacheEntryEventListener
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.info
import io.bluetape4k.logging.trace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import javax.cache.configuration.MutableCacheEntryListenerConfiguration

/**
 * 분산환경에서 front cache, back cache 를 활용하여 빠른 Access 와 분산환경에서의 Data consistency를 만족하는
 * Coroutines 환경하에서 사용하는 Near Cache입니다.
 *
 *
 * @param K Cache entry key type
 * @param V Cache entry value type
 * @property frontCache 로컬 캐시
 * @property backCache 분산환경에서 사용할 원격 캐시
 *
 * 일반 mutation은 back cache를 기준 데이터 원본으로 삼아 back-first 순서로
 * 수행합니다. back mutation이 실패하면 front cache를 변경하지 않으며, back
 * mutation 후 front 반영이 실패하면 front key를 invalidate하여 미커밋 값을
 * 반환하지 않도록 합니다. [kotlinx.coroutines.CancellationException]은
 * fallback이나 retry로 대체하지 않고 호출자에게 재전파합니다.
 * @constructor Create empty Co near cache
 */
class SuspendNearJCache<K: Any, V: Any> internal constructor(
    private val frontCache: SuspendJCache<K, V>,
    private val backCache: SuspendJCache<K, V>,
): SuspendJCache<K, V> by backCache {

    companion object: KLoggingChannel() {
        const val DEFAULT_EXPIRY_CHECK_PERIOD = 30_000L

        operator fun <K: Any, V: Any> invoke(
            frontCache: SuspendJCache<K, V>,
            backCache: SuspendJCache<K, V>,
        ): SuspendNearJCache<K, V> {
            log.info { "Back cache의 event 를 수신하는 listener를 생성합니다..." }

            val cacheEntryEventListenerCfg = MutableCacheEntryListenerConfiguration(
                { SuspendJCacheEntryEventListener(frontCache) },
                null,
                false,
                false
            )
            log.info { "back cache의 이벤트를 수신할 수 있도록 listener 등록. listenerCfg=$cacheEntryEventListenerCfg" }
            backCache.registerCacheEntryListener(cacheEntryEventListenerCfg)

            log.info { "Create SuspendNearCache instance." }
            return SuspendNearJCache(frontCache, backCache)
        }

        /**
         * back cache에 listener를 등록하지 않고 [SuspendNearJCache]를 생성합니다.
         *
         * Hazelcast client JCache 처럼 listener를 클러스터에 직렬화해서 전파해야 하는 환경에서
         * non-serializable listener 등록이 실패할 때 사용합니다.
         *
         * @param K 캐시 키 타입
         * @param V 캐시 값 타입
         * @param frontCache 로컬 front cache
         * @param backCache 원격 back cache
         * @return [SuspendNearJCache] 인스턴스
         */
        fun <K: Any, V: Any> withoutListener(
            frontCache: SuspendJCache<K, V>,
            backCache: SuspendJCache<K, V>,
        ): SuspendNearJCache<K, V> {
            log.info { "listener 없이 SuspendNearJCache를 생성합니다." }
            return SuspendNearJCache(frontCache, backCache)
        }
    }

    override fun entries(): Flow<SuspendJCacheEntry<K, V>> = frontCache.entries()

    override suspend fun clear() {
        log.debug { "Front cache를 Clear합니다." }
        frontCache.clear()
    }

    /**
     * Front Cache와 Back Cache 모두 비웁니다.
     *
     * 단, Back Cache를 공유한 다른 NearCache에는 전파되지 않습니다.
     * 전파가 필요한 경우 `removeAll()`을 사용하세요.
     *
     * ```kotlin
     * val nearCache = SuspendNearJCache(frontCache, backCache)
     * nearCache.put("hello", 5)
     * nearCache.clearAll()
     * val value = nearCache.getDeeply("hello")
     * // value == null
     * ```
     */
    suspend fun clearAll() {
        log.info {
            "front cache, back cache 모두 clear 합니다. 단 back cache 를 공유한 다른 near cache에는 전파되지 않습니다. " +
                    "전파를 위해서는 removeAll을 사용하세요"
        }
        frontCache.clear()
        runCatching { backCache.clear() }

        log.info { "front cache, back cache 모두 clear 완료." }
    }

    override suspend fun close() {
        log.info { "Near Cache 의 Front Cache를 Close 합니다." }
        runCatching { frontCache.close() }
    }

    override fun isClosed(): Boolean = frontCache.isClosed()

    override suspend fun containsKey(key: K): Boolean {
        return frontCache.containsKey(key) || backCache.containsKey(key)
    }

    override suspend fun get(key: K): V? = getDeeply(key)

    /**
     * Front Cache에서 값을 우선 조회하고, 없으면 Back Cache까지 조회합니다.
     *
     * Back Cache에서 값을 찾은 경우 Front Cache에 채워 넣어 이후 조회를 빠르게 처리합니다.
     *
     * ```kotlin
     * val nearCache = SuspendNearJCache(frontCache, backCache)
     * nearCache.put("hello", 5)
     * nearCache.clear()  // front만 비움
     * val value = nearCache.getDeeply("hello")
     * // value == 5  (back cache에서 조회 후 front에 채워 넣음)
     * ```
     *
     * @param key 조회할 캐시 키
     * @return 조회된 값, 없으면 `null`
     */
    suspend fun getDeeply(key: K): V? {
        return frontCache.get(key)
            ?: backCache.get(key)?.also { value -> frontCache.put(key, value) }
    }

    override fun getAll(): Flow<SuspendJCacheEntry<K, V>> {
        return frontCache.getAll()
    }

    override fun getAll(vararg keys: K): Flow<SuspendJCacheEntry<K, V>> =
        getAll(keys.toSet())

    override fun getAll(keys: Set<K>): Flow<SuspendJCacheEntry<K, V>> {
        return frontCache.getAll(keys)
    }

    /**
     * Back Cache의 원자 compound 연산 결과를 기준으로 Front Cache를 동기화합니다.
     * Front Cache만 먼저 조회하거나 `get` 후 변경하는 조합은 front miss와
     * 동시 호출에서 JCache 반환값·정합성을 보장하지 못하므로 사용하지 않습니다.
     */
    override suspend fun getAndPut(key: K, value: V): V? {
        val oldValue = backCache.getAndPut(key, value)
        frontCache.put(key, value)
        return oldValue
    }

    override suspend fun getAndRemove(key: K): V? {
        log.trace { "get and remove if exists cache entry. key=$key" }
        val oldValue = backCache.getAndRemove(key)
        frontCache.remove(key)
        return oldValue
    }

    override suspend fun getAndReplace(key: K, value: V): V? {
        log.trace { "get entry, and put new value if exists. key=$key, new value=$value" }
        val oldValue = backCache.getAndReplace(key, value)
        if (oldValue == null) {
            frontCache.remove(key)
        } else {
            frontCache.put(key, value)
        }
        return oldValue
    }

    override suspend fun put(key: K, value: V) {
        backCache.put(key, value)
        syncFrontAfterBack({ frontCache.put(key, value) }, { invalidateFront(key) })
    }

    override suspend fun putAll(map: Map<K, V>) {
        backCache.putAll(map)
        syncFrontAfterBack({ frontCache.putAll(map) }) { invalidateFront(map.keys) }
    }

    override suspend fun putAllFlow(entries: Flow<Pair<K, V>>) {
        entries.onEach { put(it.first, it.second) }.collect()
    }

    override suspend fun putIfAbsent(key: K, value: V): Boolean {
        val inserted = backCache.putIfAbsent(key, value)
        if (inserted) {
            syncFrontAfterBack({ frontCache.put(key, value) }, { invalidateFront(key) })
        } else {
            // Back가 이미 보유한 값이 front의 stale 값보다 우선하므로 miss로 되돌립니다.
            syncFrontAfterBack({ frontCache.remove(key) }) { invalidateFront(key) }
        }
        return inserted
    }

    override suspend fun remove(key: K): Boolean {
        val removed = backCache.remove(key)
        syncFrontAfterBack({ frontCache.remove(key) }) { invalidateFront(key) }
        return removed
    }

    override suspend fun remove(key: K, oldValue: V): Boolean {
        val removed = backCache.remove(key, oldValue)
        syncFrontAfterBack({ frontCache.remove(key) }) { invalidateFront(key) }
        return removed
    }

    override suspend fun removeAll() {
        frontCache.removeAll()
        // NOTE: Redisson에서는 bulk operation 의 경우 REMOVED event 가 발생하지 않습니다!!!
        backCache.entries()
            .map { backCache.remove(it.key) }
            .collect()
    }

    override suspend fun removeAll(vararg keys: K) {
        removeAll(keys.toSet())
    }

    override suspend fun removeAll(keys: Set<K>) {
        frontCache.removeAll(keys)
        // NOTE: Redisson에서는 bulk operation 의 경우 REMOVED event 가 발생하지 않습니다!!!
        keys.map { remove(it) }
    }

    override suspend fun replace(key: K, oldValue: V, newValue: V): Boolean {
        val replaced = backCache.replace(key, oldValue, newValue)
        if (replaced) {
            syncFrontAfterBack({ frontCache.put(key, newValue) }, { invalidateFront(key) })
        } else {
            syncFrontAfterBack({ frontCache.remove(key) }) { invalidateFront(key) }
        }
        return replaced
    }

    override suspend fun replace(key: K, value: V): Boolean {
        val replaced = backCache.replace(key, value)
        if (replaced) {
            syncFrontAfterBack({ frontCache.put(key, value) }, { invalidateFront(key) })
        } else {
            syncFrontAfterBack({ frontCache.remove(key) }) { invalidateFront(key) }
        }
        return replaced
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun syncFrontAfterBack(
        sync: suspend () -> Unit,
        invalidate: suspend () -> Unit,
    ) {
        try {
            sync()
        } catch (failure: Throwable) {
            try {
                invalidate()
            } catch (invalidateFailure: Throwable) {
                if (invalidateFailure !== failure) {
                    failure.addSuppressed(invalidateFailure)
                }
            }
            throw failure
        }
    }

    private suspend fun invalidateFront(key: K) {
        frontCache.remove(key)
    }

    private suspend fun invalidateFront(keys: Set<K>) {
        if (keys.isNotEmpty()) {
            frontCache.removeAll(keys)
        }
    }
}
