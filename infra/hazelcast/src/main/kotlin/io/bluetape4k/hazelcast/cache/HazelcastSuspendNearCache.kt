package io.bluetape4k.hazelcast.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.hazelcast.map.IMap
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Caffeine을 Front Cache로, Hazelcast [IMap]을 Back Cache로 사용하는 2-Tier Suspend NearCache입니다.
 *
 * Hazelcast 5.x `IMap`의 비동기 API(`getAsync`, `putAsync`, `removeAsync`)를
 * 코루틴에서 non-blocking으로 활용합니다.
 *
 * ```kotlin
 * val nearCache = HazelcastSuspendNearCache<String, Order>(
 *     client = hazelcastClient,
 *     config = HazelcastNearCacheConfig("orders"),
 * )
 * nearCache.put("order-1", order)
 * val cached = nearCache.get("order-1")
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @property backCache Hazelcast [IMap] (원격 캐시)
 * @property frontCache Caffeine [Cache] (로컬 캐시)
 */
class HazelcastSuspendNearCache<K: Any, V: Any> private constructor(
    val backCache: IMap<K, V>,
    val frontCache: Cache<K, V>,
) {
    companion object: KLogging() {

        /**
         * [HazelcastSuspendNearCache] 인스턴스를 생성합니다.
         *
         * @param map Near Cache가 활성화된 Hazelcast [IMap]
         * @param config Near Cache 설정
         * @return [HazelcastSuspendNearCache] 인스턴스
         */
        operator fun <K: Any, V: Any> invoke(
            map: IMap<K, V>,
            config: HazelcastNearCacheConfig,
        ): HazelcastSuspendNearCache<K, V> {
            val caffeineBuilder = Caffeine.newBuilder()
                .maximumSize(config.maxSize.toLong())
            if (config.timeToLiveSeconds > 0) {
                caffeineBuilder.expireAfterWrite(config.timeToLiveSeconds.toLong(), TimeUnit.SECONDS)
            }
            if (config.maxIdleSeconds > 0) {
                caffeineBuilder.expireAfterAccess(config.maxIdleSeconds.toLong(), TimeUnit.SECONDS)
            }
            val frontCache = caffeineBuilder.build<K, V>()
            return HazelcastSuspendNearCache(map, frontCache)
        }
    }

    /** 캐시 이름 */
    val name: String get() = backCache.name

    /** 캐시에 저장된 항목 수 (Back Cache 기준) */
    val size: Int get() = backCache.size

    /**
     * Front Cache(Caffeine) → Back Cache(Hazelcast IMap) 순으로 조회합니다.
     *
     * @param key 캐시 키
     * @return 캐시 값 또는 null
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun get(key: K): V? {
        frontCache.getIfPresent(key)?.let { return it }
        log.debug { "Front Cache 미스 - Hazelcast IMap에서 조회. map=${backCache.name}, key=$key" }
        val value = (backCache.getAsync(key) as java.util.concurrent.CompletableFuture<V?>).await()
        if (value != null) {
            frontCache.put(key, value)
        }
        return value
    }

    /**
     * Front Cache(Caffeine)와 Back Cache(Hazelcast IMap) 모두에 값을 저장합니다.
     *
     * @param key 캐시 키
     * @param value 저장할 값
     */
    suspend fun put(key: K, value: V) {
        frontCache.put(key, value)
        log.debug { "캐시 저장. map=${backCache.name}, key=$key" }
        backCache.putAsync(key, value).await()
    }

    /**
     * 여러 항목을 한 번에 Front Cache와 Back Cache에 저장합니다.
     *
     * @param entries 저장할 키-값 쌍
     */
    suspend fun putAll(entries: Map<K, V>) {
        log.debug { "캐시 일괄 저장. map=${backCache.name}, size=${entries.size}" }
        entries.forEach { (k, v) -> frontCache.put(k, v) }
        withContext(Dispatchers.IO) { backCache.putAll(entries) }
    }

    /**
     * 키에 해당하는 항목을 Front Cache와 Back Cache에서 삭제합니다.
     *
     * @param key 삭제할 캐시 키
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun remove(key: K) {
        frontCache.invalidate(key)
        log.debug { "캐시 삭제. map=${backCache.name}, key=$key" }
        (backCache.removeAsync(key) as java.util.concurrent.CompletableFuture<V?>).await()
    }

    /**
     * 키가 Front Cache 또는 Back Cache에 존재하는지 확인합니다.
     *
     * @param key 확인할 캐시 키
     * @return 존재 여부
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun containsKey(key: K): Boolean {
        if (frontCache.getIfPresent(key) != null) return true
        return (backCache.getAsync(key) as java.util.concurrent.CompletableFuture<V?>).await() != null
    }

    /**
     * Front Cache 히트 항목과 Back Cache에서 조회한 나머지 항목을 병합하여 반환합니다.
     *
     * @param keys 조회할 캐시 키 집합
     * @return 키-값 맵 (존재하지 않는 키는 포함되지 않음)
     */
    suspend fun getAll(keys: Set<K>): Map<K, V> {
        val result = mutableMapOf<K, V>()
        val missedKeys = mutableSetOf<K>()
        keys.forEach { key ->
            frontCache.getIfPresent(key)?.let { result[key] = it } ?: missedKeys.add(key)
        }
        if (missedKeys.isNotEmpty()) {
            log.debug { "Front Cache 미스 - Hazelcast IMap 일괄 조회. missed=${missedKeys.size}개" }
            withContext(Dispatchers.IO) { backCache.getAll(missedKeys) }.forEach { (k, v) ->
                result[k] = v
                frontCache.put(k, v)
            }
        }
        return result
    }

    /**
     * Front Cache(Caffeine)만 초기화합니다. Back Cache(Hazelcast)는 변경하지 않습니다.
     */
    fun clear() {
        log.debug { "Front Cache(Caffeine) 초기화. map=${backCache.name}" }
        frontCache.invalidateAll()
    }

    /**
     * Front Cache와 Back Cache 모두 초기화합니다.
     */
    suspend fun clearAll() {
        log.debug { "Front Cache와 Back Cache 모두 초기화. map=${backCache.name}" }
        frontCache.invalidateAll()
        withContext(Dispatchers.IO) { backCache.clear() }
    }
}
