package io.bluetape4k.cache.nearcache.jcache

import io.bluetape4k.cache.jcache.jcacheManagerOf
import io.bluetape4k.codec.Base58
import java.io.IOException
import java.io.ObjectInputStream
import java.io.Serializable
import javax.cache.CacheManager
import javax.cache.configuration.Factory
import javax.cache.configuration.MutableConfiguration
import javax.cache.expiry.AccessedExpiryPolicy
import javax.cache.expiry.Duration

/**
 * [NearJCache]의 환경 설정 정보를 담는 클래스입니다.
 *
 * NearCache는 로컬 캐시(Front Cache)와 원격 캐시(Back Cache)를 함께 사용하는 2-Tier 캐시 패턴입니다.
 * 이 설정 클래스는 두 캐시 간의 동작 방식을 제어합니다.
 *
 * ```kotlin
 * val config = NearJCacheConfig<String, Int>(
 *     cacheName = "my-near-cache",
 *     isSynchronous = true
 * )
 * val nearCache = NearJCache(backCache, config)
 * nearCache.put("hello", 5)
 * val value = nearCache.get("hello")
 * // value == 5
 * ```
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @property cacheManagerFactory Front Cache를 위한 [CacheManager] 팩토리 (기본: Caffeine)
 * @property cacheName Front Cache의 고유 이름
 * @property frontCacheConfiguration Front Cache 설정 (만료 시간 등)
 * @property isSynchronous Front-Back 캐시 간 동기화 방식 (true: 동기, false: 비동기)
 * @property syncRemoteTimeout 원격 캐시 동기화 타임아웃 (밀리초)
 * @property syncRemoteRetryCount 비동기 원격 캐시 write-through의 bounded retry 횟수
 *
 * @see NearJCache
 */
data class NearJCacheConfig<K: Any, V: Any>(
    val cacheManagerFactory: Factory<CacheManager> = CaffeineCacheManagerFactory,
    val cacheName: String = "near-jcache-" + Base58.randomString(8),
    val frontCacheConfiguration: MutableConfiguration<K, V> = getDefaultFrontCacheConfiguration(),
    val isSynchronous: Boolean = false,
    val syncRemoteTimeout: Long = NearJCacheConfig.DEFAULT_SYNC_REMOTE_TIMEOUT,
    val syncRemoteRetryCount: Int = NearJCacheConfig.DEFAULT_SYNC_REMOTE_RETRY_COUNT,
): Serializable {

    /**
     * prior-release의 5-인자 JVM constructor descriptor를 유지합니다.
     */
    constructor(
        cacheManagerFactory: Factory<CacheManager>,
        cacheName: String,
        frontCacheConfiguration: MutableConfiguration<K, V>,
        isSynchronous: Boolean,
        syncRemoteTimeout: Long,
    ) : this(
        cacheManagerFactory = cacheManagerFactory,
        cacheName = cacheName,
        frontCacheConfiguration = frontCacheConfiguration,
        isSynchronous = isSynchronous,
        syncRemoteTimeout = syncRemoteTimeout,
        syncRemoteRetryCount = DEFAULT_SYNC_REMOTE_RETRY_COUNT,
    )

    /** prior-release의 5-인자 JVM copy descriptor를 유지합니다. */
    fun copy(
        cacheManagerFactory: Factory<CacheManager>,
        cacheName: String,
        frontCacheConfiguration: MutableConfiguration<K, V>,
        isSynchronous: Boolean,
        syncRemoteTimeout: Long,
    ): NearJCacheConfig<K, V> = NearJCacheConfig(
        cacheManagerFactory = cacheManagerFactory,
        cacheName = cacheName,
        frontCacheConfiguration = frontCacheConfiguration,
        isSynchronous = isSynchronous,
        syncRemoteTimeout = syncRemoteTimeout,
        syncRemoteRetryCount = DEFAULT_SYNC_REMOTE_RETRY_COUNT,
    )

    /**
     * Java serialization은 constructor를 호출하지 않으므로, 기존 stream의 누락 필드를
     * [ObjectInputStream.GetField.defaulted]로 구분해 새 정책 기본값을 복원합니다.
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(input: ObjectInputStream) {
        val fields = input.readFields()
        setSerializedField("cacheManagerFactory", fields.get("cacheManagerFactory", null) as Factory<CacheManager>)
        setSerializedField("cacheName", fields.get("cacheName", null) as String)
        setSerializedField(
            "frontCacheConfiguration",
            fields.get("frontCacheConfiguration", null) as MutableConfiguration<K, V>,
        )
        setSerializedField("isSynchronous", fields.get("isSynchronous", false))
        setSerializedField("syncRemoteTimeout", fields.get("syncRemoteTimeout", DEFAULT_SYNC_REMOTE_TIMEOUT))
        setSerializedField("syncRemoteRetryCount", if (fields.defaulted("syncRemoteRetryCount")) {
            DEFAULT_SYNC_REMOTE_RETRY_COUNT
        } else {
            fields.get("syncRemoteRetryCount", DEFAULT_SYNC_REMOTE_RETRY_COUNT)
        })
    }

    private fun setSerializedField(name: String, value: Any?) {
        javaClass.getDeclaredField(name).apply {
            isAccessible = true
            set(this@NearJCacheConfig, value)
        }
    }

    companion object {
        private const val serialVersionUID: Long = 1L

        /** 최소 만료 검사 주기 (1초) */
        const val MIN_EXPIRY_CHECK_PERIOD = 1000L

        /** 기본 만료 검사 주기 (30초) */
        const val DEFAULT_EXPIRY_CHECK_PERIOD = 30_000L

        /** 기본 원격 캐시 동기화 타임아웃 (500ms) */
        const val DEFAULT_SYNC_REMOTE_TIMEOUT = 500L

        /** 비동기 원격 캐시 write-through의 기본 재시도 횟수 */
        const val DEFAULT_SYNC_REMOTE_RETRY_COUNT = 1

        /** 원격 캐시 write-through 재시도 횟수 상한 */
        const val MAX_SYNC_REMOTE_RETRY_COUNT = 3

        /** Caffeine 캐시를 위한 기본 [CacheManager] 팩토리 */
        val CaffeineCacheManagerFactory = Factory {
            jcacheManagerOf("com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider")
        }

        /**
         * Front Cache의 기본 설정을 생성합니다.
         *
         * 접근 기준 30분 만료 정책을 사용합니다.
         *
         * ```kotlin
         * val frontConfig = NearJCacheConfig.getDefaultFrontCacheConfiguration<String, Int>()
         * val config = NearJCacheConfig<String, Int>(frontCacheConfiguration = frontConfig)
         * ```
         *
         * @param K 캐시 키 타입
         * @param V 캐시 값 타입
         * @return 기본 [MutableConfiguration] 인스턴스
         */
        fun <K, V> getDefaultFrontCacheConfiguration(): MutableConfiguration<K, V> =
            MutableConfiguration<K, V>().apply {
                setStoreByValue(false)
                setExpiryPolicyFactory { AccessedExpiryPolicy(Duration.THIRTY_MINUTES) }
            }
    }
}
