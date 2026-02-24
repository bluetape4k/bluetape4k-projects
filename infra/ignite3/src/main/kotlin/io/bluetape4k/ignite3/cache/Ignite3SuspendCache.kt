package io.bluetape4k.ignite3.cache

import io.bluetape4k.cache.jcache.coroutines.SuspendCache
import io.bluetape4k.cache.jcache.coroutines.SuspendCacheEntry
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.future.await
import org.apache.ignite.client.IgniteClient
import org.apache.ignite.table.KeyValueView
import javax.cache.configuration.CacheEntryListenerConfiguration

/**
 * Apache Ignite 3.x [KeyValueView]의 비동기 API를 사용하는 [SuspendCache] 구현체입니다.
 *
 * [NearSuspendCache]의 Back Cache로 사용하기 위해 [SuspendCache] 인터페이스를 구현합니다.
 * Ignite 3.x 씬 클라이언트는 JCache 이벤트 리스너를 지원하지 않으므로,
 * [registerCacheEntryListener] / [deregisterCacheEntryListener]는 no-op으로 처리됩니다.
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @property cacheName 테이블 이름 (= 캐시 이름)
 * @property client Ignite 3.x 씬 클라이언트
 * @property view Ignite 3.x [KeyValueView] 인스턴스
 * @property keyColumn 키 컬럼 이름
 * @property valueColumn 값 컬럼 이름
 */
class Ignite3SuspendCache<K: Any, V: Any> private constructor(
    private val cacheName: String,
    private val client: IgniteClient,
    private val view: KeyValueView<K, V>,
    private val keyColumn: String = "ID",
    private val valueColumn: String = "DATA",
): SuspendCache<K, V> {

    companion object: KLoggingChannel() {

        /**
         * [Ignite3SuspendCache] 인스턴스를 생성합니다.
         */
        operator fun <K: Any, V: Any> invoke(
            tableName: String,
            client: IgniteClient,
            keyType: Class<K>,
            valueType: Class<V>,
            keyColumn: String = "ID",
            valueColumn: String = "DATA",
        ): Ignite3SuspendCache<K, V> {
            val table = client.tables().table(tableName)
                ?: error("Ignite 3.x 테이블을 찾을 수 없습니다. tableName=$tableName")
            val view = table.keyValueView(keyType, valueType)
            return Ignite3SuspendCache(tableName, client, view, keyColumn, valueColumn)
        }

        /**
         * reified 타입을 사용하는 [Ignite3SuspendCache] 인스턴스 생성 함수입니다.
         */
        inline operator fun <reified K: Any, reified V: Any> invoke(
            tableName: String,
            client: IgniteClient,
            keyColumn: String = "ID",
            valueColumn: String = "DATA",
        ): Ignite3SuspendCache<K, V> = invoke(tableName, client, K::class.java, V::class.java, keyColumn, valueColumn)
    }

    @Volatile
    private var closed: Boolean = false

    override fun entries(): Flow<SuspendCacheEntry<K, V>> = flow {
        try {
            client.sql().execute(null, "SELECT $keyColumn, $valueColumn FROM $cacheName").use { cursor ->
                cursor.forEach { row ->
                    runCatching {
                        @Suppress("UNCHECKED_CAST")
                        val key = row.value<Any>(0) as K
                        @Suppress("UNCHECKED_CAST")
                        val value = row.value<Any>(1) as V
                        emit(SuspendCacheEntry(key, value))
                    }.onFailure {
                        log.warn(it) { "캐시 엔트리 조회 중 오류 발생" }
                    }
                }
            }
        } catch (e: Exception) {
            log.warn(e) { "Ignite3SuspendCache entries() 조회 중 오류 발생. cacheName=$cacheName" }
        }
    }

    override suspend fun clear() {
        client.sql().execute(null, "DELETE FROM $cacheName").close()
    }

    override suspend fun close() {
        closed = true
    }

    override fun isClosed(): Boolean = closed

    override suspend fun containsKey(key: K): Boolean =
        view.containsAsync(null, key).await()

    override suspend fun get(key: K): V? =
        view.getAsync(null, key).await()

    override fun getAll(): Flow<SuspendCacheEntry<K, V>> = entries()

    override fun getAll(keys: Set<K>): Flow<SuspendCacheEntry<K, V>> = flow {
        view.getAllAsync(null, keys).await().forEach { (k, v) ->
            emit(SuspendCacheEntry(k, v))
        }
    }

    override suspend fun getAndPut(key: K, value: V): V? =
        view.getAndPutAsync(null, key, value).await()

    override suspend fun getAndRemove(key: K): V? =
        view.getAndRemoveAsync(null, key).await()

    override suspend fun getAndReplace(key: K, value: V): V? =
        view.getAndReplaceAsync(null, key, value).await()

    override suspend fun put(key: K, value: V) {
        view.putAsync(null, key, value).await()
    }

    override suspend fun putAll(map: Map<K, V>) {
        view.putAllAsync(null, map).await()
    }

    override suspend fun putAllFlow(entries: Flow<Pair<K, V>>) {
        val map = mutableMapOf<K, V>()
        entries.collect { (k, v) -> map[k] = v }
        if (map.isNotEmpty()) {
            view.putAllAsync(null, map).await()
        }
    }

    override suspend fun putIfAbsent(key: K, value: V): Boolean =
        view.putIfAbsentAsync(null, key, value).await()

    override suspend fun remove(key: K): Boolean =
        view.removeAsync(null, key).await()

    override suspend fun remove(key: K, oldValue: V): Boolean =
        view.removeAsync(null, key, oldValue).await()

    override suspend fun removeAll() {
        client.sql().execute(null, "DELETE FROM $cacheName").close()
    }

    override suspend fun removeAll(keys: Set<K>) {
        view.removeAllAsync(null, keys).await()
    }

    override suspend fun replace(key: K, oldValue: V, newValue: V): Boolean =
        view.replaceAsync(null, key, oldValue, newValue).await()

    override suspend fun replace(key: K, value: V): Boolean =
        view.replaceAsync(null, key, value).await()

    /**
     * Ignite 3.x 씬 클라이언트는 JCache 이벤트 리스너를 지원하지 않으므로 no-op으로 처리합니다.
     *
     * Redis(Redisson)와 달리 frontCache1 → backCache → frontCache2 이벤트 전파가 불가능합니다.
     * 대신 [NearSuspendCache]의 캐시-어사이드 패턴으로 front miss 시 back에서 조회합니다.
     */
    override fun registerCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        log.debug { "registerCacheEntryListener - Ignite 3.x 씬 클라이언트는 JCache 이벤트 리스너를 지원하지 않습니다 (no-op). cacheName=$cacheName" }
    }

    override fun deregisterCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>) {
        log.debug { "deregisterCacheEntryListener - no-op for Ignite 3.x. cacheName=$cacheName" }
    }
}
