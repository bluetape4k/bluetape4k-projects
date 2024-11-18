package io.bluetape4k.cache.nearcache

import com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider
import io.bluetape4k.cache.jcache.jcacheManager
import io.bluetape4k.codec.Base58
import java.io.Serializable
import java.util.*
import javax.cache.CacheManager
import javax.cache.configuration.Factory
import javax.cache.configuration.MutableConfiguration
import javax.cache.expiry.AccessedExpiryPolicy
import javax.cache.expiry.Duration


/**
 * [NearCache] 의 환경설정 정보
 *
 * @param K
 * @param V
 * @property cacheManagerFactory JCache [CacheManager]의 Factory
 * @property frontCacheName back cache의 event 를 받아 proactive하게 반영하는 local cache
 * @property frontCacheConfiguration event cache 환경설정 정보 (expiry를 상대적으로 길게 가져가도 무방합니다)
 * @property isSynchronous front cache와 back cache 간의 연동을 동기방식으로 반영할 것인가?,
 * (동기인 경우 latency가 느려지지만, 데이터 일관성이 유지되고, 비동기인 경우 처리속도가 빨라지지만 data consistency가 틀어질 경우가 있다)
 */
open class NearCacheConfig<K: Any, V: Any>(
    val cacheManagerFactory: Factory<CacheManager> = CaffeineCacheManagerFactory,
    val frontCacheName: String = "front-cache-" + Base58.randomString(8),
    val frontCacheConfiguration: MutableConfiguration<K, V> = getDefaultFrontCacheConfiguration(),
    val isSynchronous: Boolean = false,
    val checkExpiryPeriod: Long = DEFAULT_EXPIRY_CHECK_PERIOD,
    val syncRemoteTimeout: Long = NearCacheConfig.DEFAULT_SYNC_REMOTE_TIMEOUT,
): Serializable {

    companion object {
        const val MIN_EXPIRY_CHECK_PERIOD = 1000L
        const val DEFAULT_EXPIRY_CHECK_PERIOD = 30_000L
        const val DEFAULT_SYNC_REMOTE_TIMEOUT = 500L
        val CaffeineCacheManagerFactory = Factory { jcacheManager<CaffeineCachingProvider>() }

        fun <K, V> getDefaultFrontCacheConfiguration(): MutableConfiguration<K, V> {
            return MutableConfiguration<K, V>().apply {
                setExpiryPolicyFactory { AccessedExpiryPolicy(Duration.THIRTY_MINUTES) }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        return other is NearCacheConfig<*, *> &&
                cacheManagerFactory == other.cacheManagerFactory &&
                frontCacheName == other.frontCacheName &&
                frontCacheConfiguration == other.frontCacheConfiguration &&
                isSynchronous == other.isSynchronous &&
                checkExpiryPeriod == other.checkExpiryPeriod
    }

    override fun hashCode(): Int {
        return Objects.hash(
            cacheManagerFactory,
            frontCacheName,
            frontCacheConfiguration,
            isSynchronous,
            checkExpiryPeriod
        )
    }
}
