package io.bluetape4k.cache.jcache

import io.bluetape4k.coroutines.support.awaitSuspending
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.warn
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.future.asDeferred
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.withTimeout
import org.apache.ignite.cache.query.ContinuousQuery
import org.apache.ignite.cache.query.ScanQuery
import org.apache.ignite.client.ClientCache
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import java.util.concurrent.TimeoutException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
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

    companion object: KLogging() {
        private const val CLIENT_OP_TIMEOUT_MS = 15_000L
        private const val CLIENT_OP_MAX_ATTEMPTS = 3
        private const val CLIENT_OP_RETRY_DELAY_MS = 500L
        private val blockingExecutor = Executors.newVirtualThreadPerTaskExecutor()
    }

    /** ContinuousQuery 기반 리스너 등록 시 반환된 커서 맵. deregister 시 닫기 위해 보관. */
    private val queryCursors = ConcurrentHashMap<CacheEntryListenerConfiguration<K, V>, AutoCloseable>()

    override fun entries(): Flow<SuspendCacheEntry<K, V>> = flow {
        val cursor = cache.query(ScanQuery<K, V>())
        cursor.forEach { emit(SuspendCacheEntry(it.key, it.value)) }
    }

    override suspend fun clear() {
        awaitClientBlocking("clear") { cache.clear() }
    }

    override suspend fun close() {
        // ClientCache는 별도 close API가 없습니다.
    }

    override fun isClosed(): Boolean = false

    override suspend fun containsKey(key: K): Boolean =
        awaitClientAsync("containsKey", key) { cache.containsKeyAsync(key) }

    override suspend fun get(key: K): V? =
        awaitClientAsync("get", key) { cache.getAsync(key) }

    override fun getAll(): Flow<SuspendCacheEntry<K, V>> = entries()

    override fun getAll(keys: Set<K>): Flow<SuspendCacheEntry<K, V>> = flow {
        awaitClientAsync("getAll(keys=${keys.size})") { cache.getAllAsync(keys) }.forEach { (key, value) ->
            emit(SuspendCacheEntry(key, value))
        }
    }

    override suspend fun getAndPut(key: K, value: V): V? =
        awaitClientAsync("getAndPut", key) { cache.getAndPutAsync(key, value) }

    override suspend fun getAndRemove(key: K): V? =
        awaitClientAsync("getAndRemove", key) { cache.getAndRemoveAsync(key) }

    override suspend fun getAndReplace(key: K, value: V): V? =
        awaitClientAsync("getAndReplace", key) { cache.getAndReplaceAsync(key, value) }

    override suspend fun put(key: K, value: V) {
        awaitClientAsync("put", key) { cache.putAsync(key, value) }
    }

    override suspend fun putAll(map: Map<K, V>) {
        awaitClientAsync("putAll(size=${map.size})") { cache.putAllAsync(map) }
    }

    override suspend fun putAllFlow(entries: Flow<Pair<K, V>>) {
        entries
            .map { cache.putAsync(it.first, it.second).asDeferred() }
            .toList()
            .joinAll()
    }

    override suspend fun putIfAbsent(key: K, value: V): Boolean =
        awaitClientAsync("putIfAbsent", key) { cache.putIfAbsentAsync(key, value) }

    override suspend fun remove(key: K): Boolean =
        awaitClientAsync("remove", key) { cache.removeAsync(key) }

    override suspend fun remove(key: K, oldValue: V): Boolean =
        awaitClientAsync("remove(oldValue)", key) { cache.removeAsync(key, oldValue) }

    override suspend fun removeAll() {
        awaitClientAsync("removeAll") { cache.removeAllAsync() }
    }

    override suspend fun removeAll(keys: Set<K>) {
        awaitClientAsync("removeAll(keys=${keys.size})") { cache.removeAllAsync(keys) }
    }

    override suspend fun replace(key: K, oldValue: V, newValue: V): Boolean =
        awaitClientAsync("replace(oldValue)", key) { cache.replaceAsync(key, oldValue, newValue) }

    override suspend fun replace(key: K, value: V): Boolean =
        awaitClientAsync("replace", key) { cache.replaceAsync(key, value) }

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

    private suspend fun <T> awaitClientAsync(
        operation: String,
        key: K? = null,
        futureProvider: () -> Future<T>,
    ): T {
        repeat(CLIENT_OP_MAX_ATTEMPTS) { attempt ->
            try {
                return withTimeout(CLIENT_OP_TIMEOUT_MS) {
                    futureProvider().awaitSuspending()
                }
            } catch (e: TimeoutCancellationException) {
                val timeout = TimeoutException("Ignite thin client timeout: op=$operation, cache=${cache.name}, key=$key, attempt=${attempt + 1}")
                log.warn(timeout) {
                    "Ignite thin client 연산 타임아웃. op=$operation, cache=${cache.name}, key=$key, attempt=${attempt + 1}"
                }
                if (attempt == CLIENT_OP_MAX_ATTEMPTS - 1) {
                    throw timeout
                }
                delay(CLIENT_OP_RETRY_DELAY_MS)
            }
        }
        error("unreachable")
    }

    private suspend fun awaitClientBlocking(
        operation: String,
        key: K? = null,
        block: () -> Unit,
    ) {
        repeat(CLIENT_OP_MAX_ATTEMPTS) { attempt ->
            try {
                withTimeout(CLIENT_OP_TIMEOUT_MS) {
                    CompletableFuture.runAsync(block, blockingExecutor).await()
                }
                return
            } catch (e: TimeoutCancellationException) {
                val timeout = TimeoutException("Ignite thin client timeout: op=$operation, cache=${cache.name}, key=$key, attempt=${attempt + 1}")
                log.warn(timeout) {
                    "Ignite thin client blocking 연산 타임아웃. op=$operation, cache=${cache.name}, key=$key, attempt=${attempt + 1}"
                }
                if (attempt == CLIENT_OP_MAX_ATTEMPTS - 1) {
                    throw timeout
                }
                delay(CLIENT_OP_RETRY_DELAY_MS)
            }
        }
    }
}
