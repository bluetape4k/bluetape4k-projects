package io.bluetape4k.cache.jcache

import kotlinx.coroutines.flow.Flow
import javax.cache.configuration.CacheEntryListenerConfiguration

/**
 * 코루틴 환경에서 JCache와 유사한 계약을 제공하는 비동기 캐시 인터페이스입니다.
 *
 * ## 동작/계약
 * - 조회/수정 API는 `suspend`로 정의되어 구현체가 non-blocking 또는 blocking-bridge를 선택할 수 있습니다.
 * - `getAll(vararg)`/`removeAll(vararg)` 기본 구현은 각각 `Set` 변환 뒤 집합 기반 API로 위임합니다.
 * - `unwrap` 기본 구현은 `clazz.isAssignableFrom(javaClass)`일 때만 현재 인스턴스를 반환합니다.
 *
 * ```kotlin
 * suspend fun use(cache: SuspendCache<String, Int>) {
 *   cache.put("a", 1)
 *   val value = cache.get("a")
 *   // value == 1
 * }
 * ```
 */
interface SuspendJCache<K: Any, V: Any> {

    fun entries(): Flow<SuspendJCacheEntry<K, V>>

    suspend fun clear()

    suspend fun close()

    fun isClosed(): Boolean

    suspend fun containsKey(key: K): Boolean

    suspend fun get(key: K): V?

    fun getAll(): Flow<SuspendJCacheEntry<K, V>>
    fun getAll(vararg keys: K): Flow<SuspendJCacheEntry<K, V>> = getAll(keys.toSet())
    fun getAll(keys: Set<K>): Flow<SuspendJCacheEntry<K, V>>

    suspend fun getAndPut(key: K, value: V): V?
    suspend fun getAndRemove(key: K): V?
    suspend fun getAndReplace(key: K, value: V): V?

    suspend fun put(key: K, value: V)
    suspend fun putAll(map: Map<K, V>)
    suspend fun putAllFlow(entries: Flow<Pair<K, V>>)

    suspend fun putIfAbsent(key: K, value: V): Boolean

    suspend fun remove(key: K): Boolean
    suspend fun remove(key: K, oldValue: V): Boolean

    suspend fun removeAll()
    suspend fun removeAll(vararg keys: K) = removeAll(keys.toSet())
    suspend fun removeAll(keys: Set<K>)

    suspend fun replace(key: K, oldValue: V, newValue: V): Boolean
    suspend fun replace(key: K, value: V): Boolean

    fun registerCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>)
    fun deregisterCacheEntryListener(configuration: CacheEntryListenerConfiguration<K, V>)

    fun <T: Any> unwrap(clazz: Class<T>): T? {
        if (clazz.isAssignableFrom(javaClass)) {
            return clazz.cast(this)
        }
        return null
    }
}
