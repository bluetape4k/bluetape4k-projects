package io.bluetape4k.cache.jcache

import io.bluetape4k.coroutines.support.awaitSuspending
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.apache.ignite.cache.query.ContinuousQuery
import org.apache.ignite.cache.query.ScanQuery
import org.apache.ignite.client.ClientCache
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.event.CacheEntryCreatedListener
import javax.cache.event.CacheEntryExpiredListener
import javax.cache.event.CacheEntryRemovedListener
import javax.cache.event.CacheEntryUpdatedListener
import javax.cache.event.EventType

/**
 * Ignite 2.x thin client [org.apache.ignite.client.ClientCache]를 코루틴용 [SuspendCache]로 감싼 구현체입니다.
 *
 * ## 동작/계약
 * - 모든 연산은 `ClientCache#*Async`를 사용하고 `awaitSuspending()`으로 완료를 대기합니다.
 * - `close()`는 no-op이며 `ClientCache` 수명주기는 외부 클라이언트가 관리합니다.
 * - `entries()`는 `ScanQuery`로 전체 엔트리를 순회하므로 엔트리 수에 비례해 동작합니다.
 *
 * ```kotlin
 * val cache = Ignite2ClientSuspendCache(client.cache("users"))
 * cache.put("u:1", "debop")
 * val value = cache.get("u:1")
 * // value == "debop"
 * ```
 */
class IgniteClientSuspendCache<K: Any, V: Any>(
    private val cache: ClientCache<K, V>,
): SuspendCache<K, V> {

    companion object {
        /**
         * Ignite 연산 최대 대기 시간 (ms).
         *
         * ARM64 환경에서 IgniteClientFuture가 완료되지 않아 무한 대기하는 현상 방지.
         * [awaitSuspending]이 내부적으로 폴링 기반 [FutureToCompletableFutureWrapper]를 사용하므로
         * Future가 완료되지 않으면 영구 hang 가능 → 30초 상한으로 보호합니다.
         */
        const val OPERATION_TIMEOUT_MS = 30_000L
    }

    /** ContinuousQuery 기반 리스너 등록 시 반환된 커서 맵. deregister 시 닫기 위해 보관. */
    private val queryCursors = ConcurrentHashMap<CacheEntryListenerConfiguration<K, V>, AutoCloseable>()

    override fun entries(): Flow<SuspendCacheEntry<K, V>> = flow {
        val cursor = cache.query(ScanQuery<K, V>())
        cursor.forEach { emit(SuspendCacheEntry(it.key, it.value)) }
    }

    override suspend fun clear() {
        cache.clearAsync().awaitOrTimeout()
    }

    override suspend fun close() {
        // ClientCache는 별도 close API가 없습니다.
    }

    override fun isClosed(): Boolean = false

    override suspend fun containsKey(key: K): Boolean =
        cache.containsKeyAsync(key).awaitOrTimeout()

    override suspend fun get(key: K): V? =
        cache.getAsync(key).awaitOrTimeout()

    override fun getAll(): Flow<SuspendCacheEntry<K, V>> = entries()

    override fun getAll(keys: Set<K>): Flow<SuspendCacheEntry<K, V>> = flow {
        cache.getAllAsync(keys).awaitOrTimeout().forEach { (key, value) ->
            emit(SuspendCacheEntry(key, value))
        }
    }

    override suspend fun getAndPut(key: K, value: V): V? =
        cache.getAndPutAsync(key, value).awaitOrTimeout()

    override suspend fun getAndRemove(key: K): V? =
        cache.getAndRemoveAsync(key).awaitOrTimeout()

    override suspend fun getAndReplace(key: K, value: V): V? =
        cache.getAndReplaceAsync(key, value).awaitOrTimeout()

    override suspend fun put(key: K, value: V) {
        cache.putAsync(key, value).awaitOrTimeout()
    }

    override suspend fun putAll(map: Map<K, V>) {
        cache.putAllAsync(map).awaitOrTimeout()
    }

    override suspend fun putAllFlow(entries: Flow<Pair<K, V>>) {
        coroutineScope {
            entries.collect { (key, value) ->
                launch { cache.putAsync(key, value).awaitOrTimeout() }
            }
        }
    }

    override suspend fun putIfAbsent(key: K, value: V): Boolean =
        cache.putIfAbsentAsync(key, value).awaitOrTimeout()

    override suspend fun remove(key: K): Boolean =
        cache.removeAsync(key).awaitOrTimeout()

    override suspend fun remove(key: K, oldValue: V): Boolean =
        cache.removeAsync(key, oldValue).awaitOrTimeout()

    override suspend fun removeAll() {
        cache.removeAllAsync().awaitOrTimeout()
    }

    override suspend fun removeAll(keys: Set<K>) {
        cache.removeAllAsync(keys).awaitOrTimeout()
    }

    override suspend fun replace(key: K, oldValue: V, newValue: V): Boolean =
        cache.replaceAsync(key, oldValue, newValue).awaitOrTimeout()

    override suspend fun replace(key: K, value: V): Boolean =
        cache.replaceAsync(key, value).awaitOrTimeout()

    /**
     * ContinuousQuery의 localListener를 사용해 캐시 이벤트 리스너를 등록합니다.
     *
     * JCache 표준 `registerCacheEntryListener`는 리스너 factory를 서버로 직렬화해 전송하므로
     * non-serializable 리스너(Caffeine front cache 등을 캡처한 람다)에서 실패합니다.
     * ContinuousQuery의 `setLocalListener`는 클라이언트 JVM에서 실행되어 직렬화가 불필요합니다.
     */
    override fun registerCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        val listener = configuration.cacheEntryListenerFactory.create()

        val cq = ContinuousQuery<K, V>()
        cq.setLocalListener { events ->
            val created = events.filter { it.eventType == EventType.CREATED }
            val updated = events.filter { it.eventType == EventType.UPDATED }
            val removed = events.filter { it.eventType == EventType.REMOVED }
            val expired = events.filter { it.eventType == EventType.EXPIRED }

            @Suppress("UNCHECKED_CAST")
            if (listener is CacheEntryCreatedListener<*, *> && created.isNotEmpty())
                (listener as CacheEntryCreatedListener<K, V>).onCreated(created)
            @Suppress("UNCHECKED_CAST")
            if (listener is CacheEntryUpdatedListener<*, *> && updated.isNotEmpty())
                (listener as CacheEntryUpdatedListener<K, V>).onUpdated(updated)
            @Suppress("UNCHECKED_CAST")
            if (listener is CacheEntryRemovedListener<*, *> && removed.isNotEmpty())
                (listener as CacheEntryRemovedListener<K, V>).onRemoved(removed)
            @Suppress("UNCHECKED_CAST")
            if (listener is CacheEntryExpiredListener<*, *> && expired.isNotEmpty())
                (listener as CacheEntryExpiredListener<K, V>).onExpired(expired)
        }

        val cursor = cache.query(cq)
        queryCursors[configuration] = cursor
    }

    override fun deregisterCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        queryCursors.remove(configuration)?.close()
    }

    /**
     * ARM64에서 [IgniteClientFuture]가 완료되지 않아 무한 대기하는 현상 방지를 위해
     * [awaitSuspending] 호출에 [OPERATION_TIMEOUT_MS] 상한을 적용합니다.
     */
    private suspend fun <T> Future<T>.awaitOrTimeout(): T =
        withTimeout(OPERATION_TIMEOUT_MS) { awaitSuspending() }
}
