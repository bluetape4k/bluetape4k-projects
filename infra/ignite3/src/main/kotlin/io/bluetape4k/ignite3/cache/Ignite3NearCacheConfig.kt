package io.bluetape4k.ignite3.cache

import io.bluetape4k.cache.nearcache.NearCacheConfig
import io.bluetape4k.codec.Base58
import java.util.*
import javax.cache.CacheManager
import javax.cache.configuration.Factory
import javax.cache.configuration.MutableConfiguration
import javax.cache.expiry.AccessedExpiryPolicy
import javax.cache.expiry.Duration

/**
 * Apache Ignite 3.x 기반 [NearCache] / [NearSuspendCache] 설정 클래스입니다.
 *
 * [NearCacheConfig]를 상속하여 Front Cache(Caffeine) 설정을 포함합니다.
 * Ignite 3.x 테이블 이름, 키/값 타입, 컬럼 이름 등 Ignite3 전용 설정을 추가합니다.
 *
 * @param K 캐시 키 타입
 * @param V 캐시 값 타입
 * @property tableName Ignite 3.x 테이블 이름 (Back Cache로 사용)
 * @property keyType 캐시 키의 Java 클래스 타입
 * @property valueType 캐시 값의 Java 클래스 타입
 * @property keyColumn 키 컬럼 이름 (기본값: "ID")
 * @property valueColumn 값 컬럼 이름 (기본값: "DATA")
 */
class IgniteNearCacheConfig<K: Any, V: Any>(
    val tableName: String,
    val keyType: Class<K>,
    val valueType: Class<V>,
    val keyColumn: String = "ID",
    val valueColumn: String = "DATA",
    cacheManagerFactory: Factory<CacheManager> = CaffeineCacheManagerFactory,
    frontCacheName: String = "ignite3-near-front-" + Base58.randomString(8),
    frontCacheConfiguration: MutableConfiguration<K, V> = MutableConfiguration<K, V>().apply {
        setExpiryPolicyFactory { AccessedExpiryPolicy(Duration.THIRTY_MINUTES) }
    },
    isSynchronous: Boolean = false,
    checkExpiryPeriod: Long = DEFAULT_EXPIRY_CHECK_PERIOD,
): NearCacheConfig<K, V>(
    cacheManagerFactory = cacheManagerFactory,
    frontCacheName = frontCacheName,
    frontCacheConfiguration = frontCacheConfiguration,
    isSynchronous = isSynchronous,
    checkExpiryPeriod = checkExpiryPeriod,
) {

    companion object {
        /** 읽기 전용에 최적화된 설정 (TTL 길고 비동기) */
        inline fun <reified K: Any, reified V: Any> readOnly(tableName: String) = IgniteNearCacheConfig<K, V>(
            tableName = tableName,
            keyType = K::class.javaObjectType as Class<K>,
            valueType = V::class.java,
            isSynchronous = false,
            checkExpiryPeriod = 60_000L,
        )

        /** 고성능 쓰기에 최적화된 설정 (짧은 만료 주기, 비동기) */
        inline fun <reified K: Any, reified V: Any> writeOptimized(tableName: String) = IgniteNearCacheConfig<K, V>(
            tableName = tableName,
            keyType = K::class.javaObjectType as Class<K>,
            valueType = V::class.java,
            isSynchronous = false,
            checkExpiryPeriod = 10_000L,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is IgniteNearCacheConfig<*, *> &&
                tableName == other.tableName &&
                keyType == other.keyType &&
                valueType == other.valueType &&
                keyColumn == other.keyColumn &&
                valueColumn == other.valueColumn
    }

    override fun hashCode(): Int = Objects.hash(tableName, keyType, valueType, keyColumn, valueColumn)
}

/**
 * [IgniteNearCacheConfig]를 생성하기 위한 DSL 함수입니다.
 */
inline fun <reified K: Any, reified V: Any> igniteNearCacheConfig(
    tableName: String,
    keyColumn: String = "ID",
    valueColumn: String = "DATA",
    noinline customizer: IgniteNearCacheConfig<K, V>.() -> Unit = {},
): IgniteNearCacheConfig<K, V> =
    IgniteNearCacheConfig(
        tableName = tableName,
        keyType = K::class.javaObjectType as Class<K>,
        valueType = V::class.java,
        keyColumn = keyColumn,
        valueColumn = valueColumn,
    ).apply(customizer)
