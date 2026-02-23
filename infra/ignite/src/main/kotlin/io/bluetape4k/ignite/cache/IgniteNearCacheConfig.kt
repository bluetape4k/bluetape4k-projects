package io.bluetape4k.ignite.cache

import org.apache.ignite.cache.CacheMode
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicyFactory
import org.apache.ignite.configuration.CacheConfiguration
import org.apache.ignite.configuration.NearCacheConfiguration
import java.io.Serializable

/**
 * Apache Ignite 2.x Near Cache 설정 클래스입니다.
 *
 * **임베디드 모드**에서는 [org.apache.ignite.configuration.NearCacheConfiguration]을 통해
 * Ignite 고유의 Near Cache를 사용할 수 있습니다.
 *
 * **씬 클라이언트 모드**에서는 Caffeine + [org.apache.ignite.client.ClientCache] 2-Tier 방식을 사용합니다.
 *
 * @property cacheName Ignite 캐시 이름
 * @property cacheMode 캐시 모드 (PARTITIONED, REPLICATED, LOCAL)
 * @property nearMaxSize Near Cache 로컬 영역 최대 항목 수
 * @property frontCacheMaxSize 씬 클라이언트용 Caffeine Front Cache 최대 항목 수
 * @property frontCacheTtlSeconds 씬 클라이언트용 Caffeine TTL (초, 0이면 무제한)
 */
data class IgniteNearCacheConfig(
    val cacheName: String,
    val cacheMode: CacheMode = CacheMode.PARTITIONED,
    val nearMaxSize: Int = 10_000,
    val frontCacheMaxSize: Long = 10_000L,
    val frontCacheTtlSeconds: Long = 600L,
): Serializable {

    /**
     * Ignite 2.x 임베디드용 [NearCacheConfiguration]을 생성합니다.
     */
    fun <K: Any, V: Any> toNearCacheConfiguration(): NearCacheConfiguration<K, V> =
        NearCacheConfiguration<K, V>().apply {
            setNearEvictionPolicyFactory(LruEvictionPolicyFactory<K, V>().apply {
                setMaxSize(nearMaxSize)
            })
            setNearStartSize(minOf(nearMaxSize / 10, 100))
        }

    /**
     * Ignite 2.x 임베디드용 [CacheConfiguration]을 생성합니다.
     */
    fun <K: Any, V: Any> toCacheConfiguration(): CacheConfiguration<K, V> =
        CacheConfiguration<K, V>(cacheName).apply {
            cacheMode = this@IgniteNearCacheConfig.cacheMode
        }

    companion object {
        /** 읽기 전용에 최적화된 설정 */
        fun readOnly(cacheName: String) = IgniteNearCacheConfig(
            cacheName = cacheName,
            cacheMode = CacheMode.REPLICATED,
            frontCacheTtlSeconds = 3600L,
        )
    }
}
