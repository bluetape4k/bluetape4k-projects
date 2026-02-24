package io.bluetape4k.ignite.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.ignite.client.ClientCache
import org.apache.ignite.client.IgniteClient
import java.util.concurrent.TimeUnit

/**
 * Caffeine을 Front Cache로, Apache Ignite 2.x [ClientCache]를 Back Cache로 사용하는 2-Tier Suspend NearCache입니다.
 *
 * Ignite 2.x 씬 클라이언트는 Near Cache를 지원하지 않으므로,
 * Caffeine으로 로컬 캐시 레이어를 추가합니다.
 * `ClientCache`의 동기 API는 `withContext(Dispatchers.IO)`를 통해 non-blocking으로 호출합니다.
 *
 * ```kotlin
 * val nearCache = IgniteClientSuspendNearCache<String, Order>(
 *     client = igniteClient,
 *     config = IgniteNearCacheConfig(cacheName = "ORDERS"),
 * )
 * nearCache.put("order-1", order)
 * val cached = nearCache.get("order-1")
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @property backCache Ignite 2.x [ClientCache] (원격 캐시)
 * @property frontCache Caffeine [Cache] (로컬 캐시)
 */
class IgniteClientSuspendNearCache<K: Any, V: Any> private constructor(
    val backCache: ClientCache<K, V>,
    val frontCache: Cache<K, V>,
) {
    companion object: KLogging() {

        /**
         * [IgniteClientSuspendNearCache] 인스턴스를 생성합니다.
         *
         * @param client Ignite 2.x 씬 클라이언트
         * @param config Near Cache 설정
         * @return [IgniteClientSuspendNearCache] 인스턴스
         */
        operator fun <K: Any, V: Any> invoke(
            client: IgniteClient,
            config: IgniteNearCacheConfig,
        ): IgniteClientSuspendNearCache<K, V> {
            val backCache = client.getOrCreateCache<K, V>(config.cacheName)

            val caffeineBuilder = Caffeine.newBuilder()
                .maximumSize(config.frontCacheMaxSize)
            if (config.frontCacheTtlSeconds > 0) {
                caffeineBuilder.expireAfterWrite(config.frontCacheTtlSeconds, TimeUnit.SECONDS)
            }
            val frontCache = caffeineBuilder.build<K, V>()

            return IgniteClientSuspendNearCache(backCache, frontCache)
        }
    }

    /** 캐시 이름 */
    val name: String get() = backCache.name

    /**
     * Front Cache(Caffeine) → Back Cache(Ignite 2.x) 순으로 조회합니다.
     *
     * @param key 캐시 키
     * @return 캐시 값 또는 null
     */
    suspend fun get(key: K): V? {
        frontCache.getIfPresent(key)?.let { return it }
        log.debug { "Front Cache 미스 - Ignite 2.x에서 조회. cache=${backCache.name}, key=$key" }
        val value = withContext(Dispatchers.IO) { backCache.get(key) }
        if (value != null) {
            frontCache.put(key, value)
        }
        return value
    }

    /**
     * Front Cache(Caffeine)와 Back Cache(Ignite 2.x) 모두에 값을 저장합니다.
     *
     * @param key 캐시 키
     * @param value 저장할 값
     */
    suspend fun put(key: K, value: V) {
        frontCache.put(key, value)
        log.debug { "캐시 저장. cache=${backCache.name}, key=$key" }
        withContext(Dispatchers.IO) { backCache.put(key, value) }
    }

    /**
     * 여러 항목을 한 번에 Front Cache와 Back Cache에 저장합니다.
     *
     * @param entries 저장할 키-값 쌍
     */
    suspend fun putAll(entries: Map<K, V>) {
        log.debug { "캐시 일괄 저장. cache=${backCache.name}, size=${entries.size}" }
        entries.forEach { (k, v) -> frontCache.put(k, v) }
        withContext(Dispatchers.IO) { backCache.putAll(entries) }
    }

    /**
     * 키에 해당하는 항목을 Front Cache와 Back Cache에서 삭제합니다.
     *
     * @param key 삭제할 캐시 키
     * @return 삭제 성공 여부
     */
    suspend fun remove(key: K): Boolean {
        frontCache.invalidate(key)
        log.debug { "캐시 삭제. cache=${backCache.name}, key=$key" }
        return withContext(Dispatchers.IO) { backCache.remove(key) }
    }

    /**
     * 키가 Front Cache 또는 Back Cache에 존재하는지 확인합니다.
     *
     * @param key 확인할 캐시 키
     * @return 존재 여부
     */
    suspend fun containsKey(key: K): Boolean {
        if (frontCache.getIfPresent(key) != null) return true
        return withContext(Dispatchers.IO) { backCache.containsKey(key) }
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
            log.debug { "Front Cache 미스 - Ignite 2.x 일괄 조회. missed=${missedKeys.size}개" }
            withContext(Dispatchers.IO) { backCache.getAll(missedKeys) }.forEach { (k, v) ->
                result[k] = v
                frontCache.put(k, v)
            }
        }
        return result
    }

    /**
     * Front Cache(Caffeine)만 초기화합니다. Back Cache(Ignite)는 변경하지 않습니다.
     */
    fun clearFrontCache() {
        log.debug { "Front Cache(Caffeine) 초기화. cache=${backCache.name}" }
        frontCache.invalidateAll()
    }
}
