package io.bluetape4k.cache.nearcache.ignite

import io.bluetape4k.cache.nearcache.NearCacheConfig
import io.bluetape4k.codec.encodeBase62
import java.util.*
import javax.cache.CacheManager
import javax.cache.configuration.Configuration
import javax.cache.configuration.Factory
import javax.cache.configuration.MutableConfiguration
import javax.cache.expiry.AccessedExpiryPolicy
import javax.cache.expiry.Duration

/**
 * Ignite Back Cache를 사용하는 NearCache 설정 정보입니다.
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @property backCacheConfiguration Back Cache(JCache) 구성
 */
class IgniteNearCacheConfig<K: Any, V: Any>(
    cacheManagerFactory: Factory<CacheManager> = CaffeineCacheManagerFactory,
    frontCacheName: String = "near-front-cache-" + UUID.randomUUID().encodeBase62(),
    frontCacheConfiguration: MutableConfiguration<K, V> = MutableConfiguration<K, V>().apply {
        setExpiryPolicyFactory { AccessedExpiryPolicy(Duration.THIRTY_MINUTES) }
    },
    isSynchronous: Boolean = true,
    checkExpiryPeriod: Long = DEFAULT_EXPIRY_CHECK_PERIOD,
    private val kType: Class<K>,
    private val vType: Class<V>,
    val backCacheConfiguration: MutableConfiguration<K, V> = MutableConfiguration<K, V>().apply {
        setTypes(kType, vType)
    },
): NearCacheConfig<K, V>(
    cacheManagerFactory,
    frontCacheName,
    frontCacheConfiguration,
    isSynchronous,
    checkExpiryPeriod,
),
   Configuration<K, V> {

    override fun getKeyType(): Class<K> = kType
    override fun getValueType(): Class<V> = vType
    override fun isStoreByValue(): Boolean = true
}

/**
 * [IgniteNearCacheConfig]를 생성하는 DSL입니다.
 */
class IgniteNearCacheConfigBuilderDsl<K: Any, V: Any>(
    private val kType: Class<K>,
    private val vType: Class<V>,
) {
    var cacheManagerFactory: Factory<CacheManager> = NearCacheConfig.CaffeineCacheManagerFactory
    var frontCacheName: String = "near-front-cache-" + UUID.randomUUID().encodeBase62()
    var frontCacheConfiguration: MutableConfiguration<K, V> = MutableConfiguration<K, V>().apply {
        setExpiryPolicyFactory { AccessedExpiryPolicy(Duration.THIRTY_MINUTES) }
    }
    var isSynchronous: Boolean = true
    var checkExpiryPeriod: Long = NearCacheConfig.DEFAULT_EXPIRY_CHECK_PERIOD
    var backCacheConfiguration: MutableConfiguration<K, V> = MutableConfiguration<K, V>().apply {
        setTypes(kType, vType)
    }

    fun buildConfig(): IgniteNearCacheConfig<K, V> = IgniteNearCacheConfig(
        cacheManagerFactory,
        frontCacheName,
        frontCacheConfiguration,
        isSynchronous,
        checkExpiryPeriod,
        kType,
        vType,
        backCacheConfiguration,
    )
}

/**
 * [IgniteNearCacheConfig]를 생성하는 유틸리티 함수입니다.
 */
inline fun <reified K: Any, reified V: Any> igniteNearCacheConfigurationOf(
    customizer: IgniteNearCacheConfigBuilderDsl<K, V>.() -> Unit,
): IgniteNearCacheConfig<K, V> = IgniteNearCacheConfigBuilderDsl(K::class.java, V::class.java)
    .apply(customizer)
    .buildConfig()
