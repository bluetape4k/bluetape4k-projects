package io.bluetape4k.ignite.cache

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.ignite.client.ClientCache
import org.apache.ignite.client.IgniteClient
import java.util.concurrent.TimeUnit

/**
 * Apache Ignite 2.x 씬 클라이언트([ClientCache])를 Back Cache로,
 * Caffeine을 Front Cache로 사용하는 2-Tier NearCache입니다.
 *
 * Ignite 2.x 씬 클라이언트는 Near Cache를 지원하지 않으므로,
 * Caffeine으로 로컬 캐시 레이어를 추가합니다.
 * 임베디드 모드에서 진정한 Near Cache를 원한다면 [IgniteEmbeddedNearCache]를 사용하세요.
 *
 * ```kotlin
 * val client = igniteClient("localhost:10800")
 * val nearCache = IgniteClientNearCache<String, Order>(
 *     client = client,
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
class IgniteClientNearCache<K: Any, V: Any> private constructor(
    val backCache: ClientCache<K, V>,
    val frontCache: Cache<K, V>,
) {
    companion object: KLogging() {

        /**
         * [IgniteClientNearCache] 인스턴스를 생성합니다.
         *
         * @param client Ignite 2.x 씬 클라이언트
         * @param config Near Cache 설정
         * @return [IgniteClientNearCache] 인스턴스
         */
        operator fun <K: Any, V: Any> invoke(
            client: IgniteClient,
            config: IgniteNearCacheConfig,
        ): IgniteClientNearCache<K, V> {
            val backCache = client.getOrCreateCache<K, V>(config.cacheName)

            val caffeineBuilder = Caffeine.newBuilder()
                .maximumSize(config.frontCacheMaxSize)
            if (config.frontCacheTtlSeconds > 0) {
                caffeineBuilder.expireAfterWrite(config.frontCacheTtlSeconds, TimeUnit.SECONDS)
            }
            val frontCache = caffeineBuilder.build<K, V>()

            return IgniteClientNearCache(backCache, frontCache)
        }
    }

    val name: String get() = backCache.name

    /**
     * Front Cache(Caffeine) → Back Cache(Ignite 2.x) 순으로 조회합니다.
     */
    fun get(key: K): V? {
        frontCache.getIfPresent(key)?.let { return it }
        log.debug { "Front Cache 미스 - Ignite 2.x에서 조회. cache=${backCache.name}, key=$key" }
        return backCache.get(key)?.also { frontCache.put(key, it) }
    }

    fun put(key: K, value: V) {
        backCache.put(key, value)
        frontCache.put(key, value)
    }

    fun putAll(entries: Map<K, V>) {
        backCache.putAll(entries)
        frontCache.putAll(entries)
    }

    fun remove(key: K): Boolean {
        frontCache.invalidate(key)
        return backCache.remove(key)
    }

    fun removeAll(keys: Set<K>) {
        frontCache.invalidateAll(keys)
        backCache.removeAll(keys)
    }

    fun getAll(keys: Set<K>): Map<K, V> {
        val result = mutableMapOf<K, V>()
        val missedKeys = mutableSetOf<K>()
        keys.forEach { key ->
            frontCache.getIfPresent(key)?.let { result[key] = it } ?: missedKeys.add(key)
        }
        if (missedKeys.isNotEmpty()) {
            log.debug { "Front Cache 미스 - Ignite 2.x 일괄 조회. missed=${missedKeys.size}개" }
            backCache.getAll(missedKeys).forEach { (k, v) ->
                result[k] = v
                frontCache.put(k, v)
            }
        }
        return result
    }

    fun containsKey(key: K): Boolean =
        frontCache.getIfPresent(key) != null || backCache.containsKey(key)

    /** Front Cache(Caffeine)만 초기화합니다. Back Cache(Ignite)는 변경하지 않습니다. */
    fun clearFrontCache() = frontCache.invalidateAll()
}
