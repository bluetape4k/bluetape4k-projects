package io.bluetape4k.cache.jcache

import io.bluetape4k.coroutines.support.awaitSuspending
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.joinAll
import org.apache.ignite.cache.query.ScanQuery
import org.apache.ignite.client.ClientCache
import javax.cache.configuration.CacheEntryListenerConfiguration

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

    override fun entries(): Flow<SuspendCacheEntry<K, V>> = flow {
        val cursor = cache.query(ScanQuery<K, V>())
        cursor.forEach { emit(SuspendCacheEntry(it.key, it.value)) }
    }

    override suspend fun clear() {
        cache.clearAsync().awaitSuspending()
    }

    override suspend fun close() {
        // ClientCache는 별도 close API가 없습니다.
    }

    override fun isClosed(): Boolean = false

    override suspend fun containsKey(key: K): Boolean =
        cache.containsKeyAsync(key).awaitSuspending()

    override suspend fun get(key: K): V? =
        cache.getAsync(key).awaitSuspending()

    override fun getAll(): Flow<SuspendCacheEntry<K, V>> = entries()

    override fun getAll(keys: Set<K>): Flow<SuspendCacheEntry<K, V>> = flow {
        cache.getAllAsync(keys).awaitSuspending().forEach { (key, value) ->
            emit(SuspendCacheEntry(key, value))
        }
    }

    override suspend fun getAndPut(key: K, value: V): V? =
        cache.getAndPutAsync(key, value).awaitSuspending()

    override suspend fun getAndRemove(key: K): V? =
        cache.getAndRemoveAsync(key).awaitSuspending()

    override suspend fun getAndReplace(key: K, value: V): V? =
        cache.getAndReplaceAsync(key, value).awaitSuspending()

    override suspend fun put(key: K, value: V) {
        cache.putAsync(key, value).awaitSuspending()
    }

    override suspend fun putAll(map: Map<K, V>) {
        cache.putAllAsync(map).awaitSuspending()
    }

    override suspend fun putAllFlow(entries: Flow<Pair<K, V>>) {
        entries
            .map { cache.putAsync(it.first, it.second).asDeferred() }
            .toList()
            .joinAll()
    }

    override suspend fun putIfAbsent(key: K, value: V): Boolean =
        cache.putIfAbsentAsync(key, value).awaitSuspending()

    override suspend fun remove(key: K): Boolean =
        cache.removeAsync(key).awaitSuspending()

    override suspend fun remove(key: K, oldValue: V): Boolean =
        cache.removeAsync(key, oldValue).awaitSuspending()

    override suspend fun removeAll() {
        cache.removeAllAsync().awaitSuspending()
    }

    override suspend fun removeAll(keys: Set<K>) {
        cache.removeAllAsync(keys).awaitSuspending()
    }

    override suspend fun replace(key: K, oldValue: V, newValue: V): Boolean =
        cache.replaceAsync(key, oldValue, newValue).awaitSuspending()

    override suspend fun replace(key: K, value: V): Boolean =
        cache.replaceAsync(key, value).awaitSuspending()

    override fun registerCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        cache.registerCacheEntryListener(configuration)
    }

    override fun deregisterCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        cache.deregisterCacheEntryListener(configuration)
    }
}
