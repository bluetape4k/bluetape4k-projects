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
 * [NearCache]의 환경 설정 정보를 담는 클래스입니다.
 *
 * NearCache는 로컬 캐시(Front Cache)와 원격 캐시(Back Cache)를 함께 사용하는 2-Tier 캐시 패턴입니다.
 * 이 설정 클래스는 두 캐시 간의 동작 방식을 제어합니다.
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @property cacheManagerFactory Front Cache를 위한 [CacheManager] 팩토리 (기본: Caffeine)
 * @property frontCacheName Front Cache의 고유 이름
 * @property frontCacheConfiguration Front Cache 설정 (만료 시간 등)
 * @property isSynchronous Front-Back 캐시 간 동기화 방식 (true: 동기, false: 비동기)
 * @property checkExpiryPeriod Back Cache 만료 검사 주기 (밀리초)
 * @property syncRemoteTimeout 원격 캐시 동기화 타임아웃 (밀리초)
 *
 * @see NearCache
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
        /** 최소 만료 검사 주기 (1초) */
        const val MIN_EXPIRY_CHECK_PERIOD = 1000L

        /** 기본 만료 검사 주기 (30초) */
        const val DEFAULT_EXPIRY_CHECK_PERIOD = 30_000L

        /** 기본 원격 캐시 동기화 타임아웃 (500ms) */
        const val DEFAULT_SYNC_REMOTE_TIMEOUT = 500L

        /** Caffeine 캐시를 위한 기본 [CacheManager] 팩토리 */
        val CaffeineCacheManagerFactory = Factory { jcacheManager<CaffeineCachingProvider>() }

        /**
         * Front Cache의 기본 설정을 생성합니다.
         *
         * 접근 기준 30분 만료 정책을 사용합니다.
         *
         * @param K 캐시 키 타입
         * @param V 캐시 값 타입
         * @return 기본 [MutableConfiguration] 인스턴스
         */
        fun <K, V> getDefaultFrontCacheConfiguration(): MutableConfiguration<K, V> =
            MutableConfiguration<K, V>().apply {
                setExpiryPolicyFactory { AccessedExpiryPolicy(Duration.THIRTY_MINUTES) }
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

    override fun hashCode(): Int =
        Objects.hash(
            cacheManagerFactory,
            frontCacheName,
            frontCacheConfiguration,
            isSynchronous,
            checkExpiryPeriod,
        )
}
