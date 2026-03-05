package io.bluetape4k.cache.nearcache

import io.bluetape4k.codec.encodeBase62
import java.util.*
import javax.cache.CacheManager
import javax.cache.configuration.Configuration
import javax.cache.configuration.Factory
import javax.cache.configuration.MutableConfiguration
import javax.cache.expiry.AccessedExpiryPolicy
import javax.cache.expiry.Duration

/**
 * Ignite back cache를 사용하는 near cache 설정을 표현합니다.
 *
 * ## 동작/계약
 * - front cache 설정은 [NearCacheConfig] 기반 속성을 그대로 상속합니다.
 * - back cache 설정은 [backCacheConfiguration]에 `K/V` 타입이 고정된 `MutableConfiguration`으로 유지됩니다.
 * - `isStoreByValue()`는 항상 `true`를 반환합니다.
 *
 * ```kotlin
 * val cfg = IgniteNearCacheConfig(kType = String::class.java, vType = Int::class.java)
 * // cfg.valueType == Int::class.java
 * ```
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
 * [IgniteNearCacheConfig] 생성을 위한 DSL 빌더입니다.
 *
 * ## 동작/계약
 * - 기본 front cache 만료 정책은 `AccessedExpiryPolicy(30분)`입니다.
 * - 기본 back cache 설정은 `setTypes(kType, vType)`가 적용됩니다.
 * - [buildConfig] 호출 시 현재 빌더 상태로 새 설정 객체를 생성합니다.
 *
 * ```kotlin
 * val cfg = IgniteNearCacheConfigBuilderDsl(String::class.java, Int::class.java).buildConfig()
 * // cfg.keyType == String::class.java
 * ```
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
 * DSL 블록으로 [IgniteNearCacheConfig]를 생성합니다.
 *
 * ## 동작/계약
 * - `K/V` reified 타입을 빌더에 전달합니다.
 * - [customizer]에서 변경한 값을 반영해 최종 설정을 만듭니다.
 * - 호출마다 새 설정 객체를 반환합니다.
 *
 * ```kotlin
 * val cfg = igniteNearCacheConfigurationOf<String, Int> { checkExpiryPeriod = 500L }
 * // cfg.checkExpiryPeriod == 500L
 * ```
 */
inline fun <reified K: Any, reified V: Any> igniteNearCacheConfigurationOf(
    customizer: IgniteNearCacheConfigBuilderDsl<K, V>.() -> Unit,
): IgniteNearCacheConfig<K, V> = IgniteNearCacheConfigBuilderDsl(K::class.java, V::class.java)
    .apply(customizer)
    .buildConfig()
