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

/**
 * Redis(Redisson) back cache를 사용하는 near cache 설정을 표현합니다.
 *
 * ## 동작/계약
 * - [redissonConfig]가 주어지면 back cache 구성에 해당 설정을 사용합니다.
 * - `isStoreByValue()`는 항상 `true`를 반환합니다.
 * - equals/hashCode 비교에는 `redissonConfig`, `kType`, `vType`가 반영됩니다.
 *
 * ```kotlin
 * val cfg = RedisNearCacheConfig(
 *   redissonConfig = null,
 *   kType = String::class.java,
 *   vType = Int::class.java
 * )
 * // cfg.keyType == String::class.java
 * ```
 */
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

/**
 * [RedisNearCacheConfig] 생성을 위한 DSL 빌더입니다.
 *
 * ## 동작/계약
 * - 기본 front cache 만료 정책은 `AccessedExpiryPolicy(30분)`입니다.
 * - [buildConfig]에서 `redissonConfig.isStoreByValue == false`면 `IllegalArgumentException`이 발생합니다.
 * - [buildConfig]는 현재 빌더 상태로 새 설정 객체를 생성합니다.
 *
 * ```kotlin
 * val cfg = RedisNearCacheConfigBuilderDsl(String::class.java, Int::class.java).buildConfig()
 * // cfg.valueType == Int::class.java
 * ```
 */
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
 * DSL 블록으로 [RedisNearCacheConfig]를 생성합니다.
 *
 * ## 동작/계약
 * - `K/V` reified 타입을 빌더에 전달합니다.
 * - [customizer]에서 설정한 값을 반영해 최종 설정을 생성합니다.
 * - 호출마다 새 설정 객체를 반환합니다.
 *
 * ```kotlin
 * val cfg = redisNearCacheConfigurationOf<String, Int> { isSynchronous = false }
 * // cfg.isSynchronous == false
 * ```
 */
inline fun <reified K: Any, reified V: Any> redisNearCacheConfigurationOf(
    customizer: RedisNearCacheConfigBuilderDsl<K, V>.() -> Unit,
): RedisNearCacheConfig<K, V> {
    return RedisNearCacheConfigBuilderDsl(K::class.java, V::class.java).apply {
        customizer(this)
    }.buildConfig()
}
