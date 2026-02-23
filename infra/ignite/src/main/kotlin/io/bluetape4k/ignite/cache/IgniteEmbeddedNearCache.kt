package io.bluetape4k.ignite.cache

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.apache.ignite.Ignite
import org.apache.ignite.IgniteCache

/**
 * Apache Ignite 2.x 임베디드 노드의 [IgniteCache]를 활용한 NearCache 래퍼입니다.
 *
 * Ignite 2.x 임베디드 모드에서 [org.apache.ignite.configuration.NearCacheConfiguration]을 적용하면
 * Ignite가 자동으로 로컬 Near Cache를 관리합니다.
 *
 * **주의**: Near Cache는 임베디드 노드(`Ignition.start()`)에서만 지원됩니다.
 * 씬 클라이언트 모드는 [IgniteClientNearCache]를 사용하세요.
 *
 * ```kotlin
 * val ignite = igniteEmbedded { igniteInstanceName = "my-node" }
 * val nearCache = IgniteEmbeddedNearCache<String, Order>(
 *     ignite = ignite,
 *     config = IgniteNearCacheConfig(cacheName = "ORDERS"),
 * )
 * nearCache.put("order-1", order)
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @property cache Near Cache가 활성화된 [IgniteCache]
 */
class IgniteEmbeddedNearCache<K: Any, V: Any> private constructor(
    val cache: IgniteCache<K, V>,
) {
    companion object: KLogging() {

        /**
         * [IgniteEmbeddedNearCache] 인스턴스를 생성합니다.
         *
         * @param ignite Ignite 임베디드 노드
         * @param config Near Cache 설정
         * @return [IgniteEmbeddedNearCache] 인스턴스
         */
        operator fun <K: Any, V: Any> invoke(
            ignite: Ignite,
            config: IgniteNearCacheConfig,
        ): IgniteEmbeddedNearCache<K, V> {
            val cacheCfg = config.toCacheConfiguration<K, V>()
            val nearCacheCfg = config.toNearCacheConfiguration<K, V>()

            // 캐시가 없으면 생성, 있으면 Near Cache 활성화
            val cache = if (ignite.cacheNames().contains(config.cacheName)) {
                ignite.getOrCreateNearCache(config.cacheName, nearCacheCfg)
            } else {
                ignite.getOrCreateCache(cacheCfg)
                ignite.getOrCreateNearCache(config.cacheName, nearCacheCfg)
            }
            return IgniteEmbeddedNearCache(cache)
        }
    }

    val name: String get() = cache.name

    fun get(key: K): V? {
        log.debug { "캐시 조회. cache=${cache.name}, key=$key" }
        return cache.get(key)
    }

    fun put(key: K, value: V) {
        log.debug { "캐시 저장. cache=${cache.name}, key=$key" }
        cache.put(key, value)
    }

    fun putAll(entries: Map<K, V>) {
        log.debug { "캐시 일괄 저장. cache=${cache.name}, size=${entries.size}" }
        cache.putAll(entries)
    }

    fun remove(key: K): Boolean {
        log.debug { "캐시 삭제. cache=${cache.name}, key=$key" }
        return cache.remove(key)
    }

    fun removeAll(keys: Set<K>) {
        log.debug { "캐시 일괄 삭제. cache=${cache.name}, keys=${keys.size}개" }
        cache.removeAll(keys)
    }

    fun getAll(keys: Set<K>): Map<K, V> {
        log.debug { "캐시 일괄 조회. cache=${cache.name}, keys=${keys.size}개" }
        return cache.getAll(keys)
    }

    fun containsKey(key: K): Boolean = cache.containsKey(key)

    fun clear() = cache.clear()
}
