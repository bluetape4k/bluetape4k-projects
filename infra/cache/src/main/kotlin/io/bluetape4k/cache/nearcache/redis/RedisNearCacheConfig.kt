package io.bluetape4k.cache.nearcache.redis

import io.bluetape4k.cache.nearcache.NearCacheConfig
import io.bluetape4k.codec.encodeBase62
import org.redisson.jcache.configuration.RedissonConfiguration
import java.util.*
import javax.cache.CacheManager
import javax.cache.configuration.Configuration
import javax.cache.configuration.Factory
import javax.cache.configuration.MutableConfiguration
import javax.cache.expiry.AccessedExpiryPolicy
import javax.cache.expiry.Duration

class RedisNearCacheConfig<K: Any, V: Any>(
    cacheManagerFactory: Factory<CacheManager> = CaffeineCacheManagerFactory,
    frontCacheName: String = "near-front-cache-" + UUID.randomUUID().encodeBase62(),
    frontCacheConfiguration: MutableConfiguration<K, V> = MutableConfiguration<K, V>().apply {
        setExpiryPolicyFactory {
            AccessedExpiryPolicy(Duration.THIRTY_MINUTES)
        }
    },
    isSynchronous: Boolean = true,
    checkExpiryPeriod: Long = DEFAULT_EXPIRY_CHECK_PERIOD,
    val redissonConfig: RedissonConfiguration<K, V>?,
    private val kType: Class<K>,
    private val vType: Class<V>,
): NearCacheConfig<K, V>(
    cacheManagerFactory,
    frontCacheName,
    frontCacheConfiguration,
    isSynchronous,
    checkExpiryPeriod
),
   Configuration<K, V> {

    override fun getKeyType(): Class<K> = kType
    override fun getValueType(): Class<V> = vType
    override fun isStoreByValue(): Boolean = true

    override fun equals(other: Any?): Boolean {
        return other is RedisNearCacheConfig<*, *> &&
                redissonConfig == other.redissonConfig &&
                kType == other.kType &&
                vType == other.vType
    }

    override fun hashCode(): Int {
        return Objects.hash(super.hashCode(), redissonConfig, kType, vType)
    }
}


class RedisNearCacheConfigBuilderDsl<K: Any, V: Any>(
    private val kType: Class<K>,
    private val vType: Class<V>,

    ) {
    var cacheManagerFactory: Factory<CacheManager> = NearCacheConfig.CaffeineCacheManagerFactory
    var frontCacheName: String = "near-front-cache-" + UUID.randomUUID().encodeBase62()
    var frontCacheConfiguration: MutableConfiguration<K, V> = MutableConfiguration<K, V>().apply {
        setExpiryPolicyFactory {
            AccessedExpiryPolicy(
                Duration.THIRTY_MINUTES
            )
        }
    }
    var isSynchronous: Boolean = true
    var checkExpiryPeriod: Long = NearCacheConfig.DEFAULT_EXPIRY_CHECK_PERIOD
    var redissonConfig: RedissonConfiguration<K, V>? = null


    // org.redisson.jcache.configuration.RedissonConfiguration 은 isStoreValue 를 true 로 강제하고 있다.
    // redis 를 back cache 로 사용한다면 reference 를 캐시하는 의미 자체가 없으므로 isStoreByValue 를 true 로 강제한다.
    fun buildConfig(): RedisNearCacheConfig<K, V> {
        if (false == redissonConfig?.isStoreByValue) {
            throw IllegalArgumentException("RedissonConfig's isStoreByValue should be true")
        }
        return RedisNearCacheConfig(
            cacheManagerFactory,
            frontCacheName,
            frontCacheConfiguration,
            isSynchronous,
            checkExpiryPeriod,
            redissonConfig,
            kType,
            vType
        )
    }
}

/**
 * RedisNearCacheConfig 을 생성하기 위한 유틸리티 함수입니다.
 *
 */
inline fun <reified K: Any, reified V: Any> redisNearCacheConfigurationOf(
    customizer: RedisNearCacheConfigBuilderDsl<K, V>.() -> Unit,
): RedisNearCacheConfig<K, V> {
    return RedisNearCacheConfigBuilderDsl(K::class.java, V::class.java).apply {
        customizer(this)
    }.buildConfig()
}
