package io.bluetape4k.ignite3.cache

import io.bluetape4k.cache.jcache.JCache
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.warn
import org.apache.ignite.client.IgniteClient
import org.apache.ignite.table.KeyValueView
import javax.cache.Cache
import javax.cache.CacheManager
import javax.cache.configuration.CacheEntryListenerConfiguration
import javax.cache.configuration.Configuration
import javax.cache.configuration.MutableConfiguration
import javax.cache.integration.CompletionListener
import javax.cache.processor.EntryProcessor
import javax.cache.processor.EntryProcessorException
import javax.cache.processor.EntryProcessorResult

/**
 * Apache Ignite 3.x [KeyValueView]를 [javax.cache.Cache]([JCache]) 인터페이스로 래핑하는 구현체입니다.
 *
 * [NearCache]의 Back Cache로 사용하기 위해 [javax.cache.Cache] 인터페이스를 구현합니다.
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
class Ignite3JCache<K: Any, V: Any> private constructor(
    private val cacheName: String,
    private val client: IgniteClient,
    private val view: KeyValueView<K, V>,
    private val keyColumn: String = "ID",
    private val valueColumn: String = "DATA",
): JCache<K, V> {

    companion object: KLogging() {

        /**
         * [Ignite3JCache] 인스턴스를 생성합니다.
         */
        operator fun <K: Any, V: Any> invoke(
            tableName: String,
            client: IgniteClient,
            keyType: Class<K>,
            valueType: Class<V>,
            keyColumn: String = "ID",
            valueColumn: String = "DATA",
        ): Ignite3JCache<K, V> {
            val table = client.tables().table(tableName)
                ?: error("Ignite 3.x 테이블을 찾을 수 없습니다. tableName=$tableName")
            val view = table.keyValueView(keyType, valueType)
            return Ignite3JCache(tableName, client, view, keyColumn, valueColumn)
        }

        /**
         * reified 타입을 사용하는 [Ignite3JCache] 인스턴스 생성 함수입니다.
         */
        inline operator fun <reified K: Any, reified V: Any> invoke(
            tableName: String,
            client: IgniteClient,
            keyColumn: String = "ID",
            valueColumn: String = "DATA",
        ): Ignite3JCache<K, V> = invoke(tableName, client, K::class.java, V::class.java, keyColumn, valueColumn)
    }

    @Volatile
    private var closed: Boolean = false

    override fun get(key: K): V? = view.get(null, key)

    override fun getAll(keys: MutableSet<out K>): MutableMap<K, V> {
        @Suppress("UNCHECKED_CAST")
        return view.getAll(null, keys as Collection<K>).toMutableMap()
    }

    override fun containsKey(key: K): Boolean = view.contains(null, key)

    override fun loadAll(
        keys: MutableSet<out K>?,
        replaceExistingValues: Boolean,
        completionListener: CompletionListener?,
    ) {
        // Ignite3 JCache에서는 loadAll을 지원하지 않습니다
        completionListener?.onCompletion()
    }

    override fun put(key: K, value: V) {
        view.put(null, key, value)
    }

    override fun getAndPut(key: K, value: V): V? = view.getAndPut(null, key, value)

    override fun putAll(map: MutableMap<out K, out V>) {
        @Suppress("UNCHECKED_CAST")
        view.putAll(null, map as Map<K, V>)
    }

    override fun putIfAbsent(key: K, value: V): Boolean = view.putIfAbsent(null, key, value)

    override fun remove(key: K): Boolean = view.remove(null, key)

    override fun remove(key: K, oldValue: V): Boolean = view.remove(null, key, oldValue)

    override fun getAndRemove(key: K): V? = view.getAndRemove(null, key)

    override fun replace(key: K, oldValue: V, newValue: V): Boolean = view.replace(null, key, oldValue, newValue)

    override fun replace(key: K, value: V): Boolean = view.replace(null, key, value)

    override fun getAndReplace(key: K, value: V): V? = view.getAndReplace(null, key, value)

    override fun removeAll(keys: MutableSet<out K>) {
        @Suppress("UNCHECKED_CAST")
        view.removeAll(null, keys as Collection<K>)
    }

    override fun removeAll() {
        client.sql().execute(null, "DELETE FROM $cacheName").close()
    }

    override fun clear() {
        client.sql().execute(null, "DELETE FROM $cacheName").close()
    }

    override fun <C: Configuration<K, V>> getConfiguration(clazz: Class<C>): C {
        @Suppress("UNCHECKED_CAST")
        return MutableConfiguration<K, V>() as C
    }

    override fun <T: Any> invoke(
        key: K,
        entryProcessor: EntryProcessor<K, V, T>,
        vararg arguments: Any?,
    ): T = throw UnsupportedOperationException("Ignite3JCache는 invoke를 지원하지 않습니다")

    override fun <T: Any> invokeAll(
        keys: MutableSet<out K>,
        entryProcessor: EntryProcessor<K, V, T>,
        vararg arguments: Any?,
    ): MutableMap<K, EntryProcessorResult<T>> =
        throw UnsupportedOperationException("Ignite3JCache는 invokeAll을 지원하지 않습니다")

    override fun getName(): String = cacheName

    override fun getCacheManager(): CacheManager? = null

    override fun close() {
        closed = true
    }

    override fun isClosed(): Boolean = closed

    override fun <T: Any> unwrap(clazz: Class<T>): T? {
        if (clazz.isAssignableFrom(javaClass)) return clazz.cast(this)
        return null
    }

    /**
     * Ignite 3.x 씬 클라이언트는 JCache 이벤트 리스너를 지원하지 않으므로 no-op으로 처리합니다.
     *
     * Redis(Redisson)와 달리 frontCache1 → backCache → frontCache2 이벤트 전파가 불가능합니다.
     * 대신 [NearCache]의 폴링 기반 만료 감지로 최종 일관성을 제공합니다.
     */
    override fun registerCacheEntryListener(cacheEntryListenerConfiguration: CacheEntryListenerConfiguration<K, V>) {
        log.debug { "registerCacheEntryListener - Ignite 3.x 씬 클라이언트는 JCache 이벤트 리스너를 지원하지 않습니다 (no-op). cacheName=$cacheName" }
    }

    override fun deregisterCacheEntryListener(cacheEntryListenerConfiguration: CacheEntryListenerConfiguration<K, V>) {
        log.debug { "deregisterCacheEntryListener - no-op for Ignite 3.x. cacheName=$cacheName" }
    }

    /**
     * SQL 쿼리로 모든 캐시 엔트리를 반환하는 [Iterator]를 제공합니다.
     */
    override fun iterator(): MutableIterator<Cache.Entry<K, V>> {
        val entries = mutableListOf<Cache.Entry<K, V>>()
        try {
            client.sql().execute(null, "SELECT $keyColumn, $valueColumn FROM $cacheName").use { cursor ->
                cursor.forEach { row ->
                    runCatching {
                        @Suppress("UNCHECKED_CAST")
                        val key = row.value<Any>(0) as K
                        @Suppress("UNCHECKED_CAST")
                        val value = row.value<Any>(1) as V
                        entries.add(object: Cache.Entry<K, V> {
                            override fun getKey(): K = key
                            override fun getValue(): V = value
                            override fun <T: Any> unwrap(clazz: Class<T>): T? = null
                        })
                    }.onFailure {
                        log.warn(it) { "캐시 엔트리 조회 중 오류 발생" }
                    }
                }
            }
        } catch (e: Exception) {
            log.warn(e) { "Ignite3JCache iterator 조회 중 오류 발생. cacheName=$cacheName" }
        }
        return entries.toMutableList().iterator()
    }
}
