package io.bluetape4k.redis.redisson.cache

import io.bluetape4k.redis.redisson.codec.RedissonCodecs
import org.redisson.api.map.WriteMode
import org.redisson.api.options.ExMapOptions
import org.redisson.api.options.LocalCachedMapOptions
import org.redisson.api.options.MapOptions
import org.redisson.client.codec.Codec
import java.time.Duration

/**
 * Redis 캐시 설정을 위한 클래스입니다.
 *
 * @property cacheMode 캐시 모드 (읽기 전용, 쓰기 전용, 읽기/쓰기 모두 등)
 * @property writeMode 쓰기 모드 (WRITE_THROUGH, WRITE_BEHIND)
 * @property deleteFromDBOnInvalidate 캐시 무효화 시 DB에서도 삭제할지 여부. 옵션 변환 단계에서는 적용되지 않고 애플리케이션 계층에서 처리해야 합니다.
 * @property ttl 캐시 항목의 기본 만료 시간. `toMapOptions`/`toLocalCachedMapOptions`에서는 직접 지원되지 않으므로 fail-fast 검증에 사용됩니다.
 * @property maxSize 캐시의 최대 크기 (0이면 무제한). `toMapOptions`/`toLocalCachedMapOptions`에서는 직접 지원되지 않으므로 fail-fast 검증에 사용됩니다.
 * @property codec 캐시 항목의 직렬화 및 역직렬화에 사용할 Codec (기본은 [RedissonCodecs.LZ4ForyComposite])
 *
 * @property nearCacheMaxIdleTime 로컬 캐시 항목의 최대 유휴 시간
 * @property nearCacheEnabled 로컬(Near) 캐시 사용 여부
 * @property nearCacheMaxSize 로컬 캐시의 최대 크기
 * @property nearCacheTtl 로컬 캐시 항목의 만료 시간
 * @property nearCacheSyncStrategy 로컬 캐시 무효화 정책
 * @property writeBehindBatchSize WRITE_BEHIND 모드에서 배치 크기
 * @property writeBehindDelay WRITE_BEHIND 모드에서 지연 시간
 * @property writeRetryAttempts 쓰기 실패 시 재시도 횟수
 * @property writeRetryInterval 쓰기 재시도 간격
 */
data class RedissonCacheConfig(
    val cacheMode: CacheMode = CacheMode.READ_WRITE,
    val writeMode: WriteMode = WriteMode.WRITE_THROUGH,
    val deleteFromDBOnInvalidate: Boolean = false,
    val ttl: Duration = Duration.ZERO,
    val maxSize: Int = 0,
    val codec: Codec = RedissonCodecs.LZ4ForyComposite, // mapKey Codec은 String, mapValue Codec은 LZ4Fury
    // NearCache 관련 설정
    val nearCacheMaxIdleTime: Duration = Duration.ZERO,
    val nearCacheEnabled: Boolean = false,
    val nearCacheMaxSize: Int = 1000,
    val nearCacheTtl: Duration = Duration.ofMinutes(10),
    val nearCacheSyncStrategy: LocalCachedMapOptions.SyncStrategy = LocalCachedMapOptions.SyncStrategy.UPDATE,
    // Write Behind 관련 설정
    val writeBehindBatchSize: Int = 50,
    val writeBehindDelay: Int = 1000,
    val writeRetryAttempts: Int = 3,
    val writeRetryInterval: Duration = Duration.ofMillis(100),
) {
    init {
        require(maxSize >= 0) { "maxSize must be greater than or equal to 0. maxSize=$maxSize" }
        require(nearCacheMaxSize >= 0) {
            "nearCacheMaxSize must be greater than or equal to 0. nearCacheMaxSize=$nearCacheMaxSize"
        }
        require(writeBehindBatchSize > 0) {
            "writeBehindBatchSize must be greater than 0. writeBehindBatchSize=$writeBehindBatchSize"
        }
        require(writeBehindDelay >= 0) {
            "writeBehindDelay must be greater than or equal to 0. writeBehindDelay=$writeBehindDelay"
        }
        require(writeRetryAttempts >= 0) {
            "writeRetryAttempts must be greater than or equal to 0. writeRetryAttempts=$writeRetryAttempts"
        }
        require(!writeRetryInterval.isNegative) {
            "writeRetryInterval must be greater than or equal to 0. writeRetryInterval=$writeRetryInterval"
        }
        require(!ttl.isNegative) { "ttl must be greater than or equal to 0. ttl=$ttl" }
        require(
            !nearCacheTtl.isNegative
        ) { "nearCacheTtl must be greater than or equal to 0. nearCacheTtl=$nearCacheTtl" }
        require(!nearCacheMaxIdleTime.isNegative) {
            "nearCacheMaxIdleTime must be greater than or equal to 0. nearCacheMaxIdleTime=$nearCacheMaxIdleTime"
        }
    }

    val isReadOnly: Boolean
        get() = cacheMode == CacheMode.READ_ONLY

    val isReadWrite: Boolean
        get() = cacheMode == CacheMode.READ_WRITE

    val isWriteBehind: Boolean
        get() = writeMode == WriteMode.WRITE_BEHIND

    val isNearCacheEnabled: Boolean
        get() = nearCacheEnabled

    companion object {
        /**
         * 읽기 전용 캐시 설정입니다.
         * DB에서 데이터를 로드하지만, 캐시에 변경사항을 저장하지 않습니다.
         */
        val READ_ONLY =
            RedissonCacheConfig(
                cacheMode = CacheMode.READ_ONLY,
                nearCacheEnabled = false
            )

        /**
         * 읽기 전용 캐시 설정으로, 로컬 캐시를 사용합니다.
         * 자주 조회되는 데이터에 대해 성능을 향상시킵니다.
         */
        val READ_ONLY_WITH_NEAR_CACHE =
            RedissonCacheConfig(
                cacheMode = CacheMode.READ_ONLY,
                nearCacheEnabled = true
            )

        /**
         * 읽기-쓰기 통과(Read-Write Through) 캐시 설정입니다.
         * 캐시와 DB 간의 데이터 일관성을 보장합니다.
         */
        val READ_WRITE_THROUGH =
            RedissonCacheConfig(
                cacheMode = CacheMode.READ_WRITE,
                writeMode = WriteMode.WRITE_THROUGH,
                nearCacheEnabled = false
            )

        /**
         * 읽기-쓰기 통과 캐시 설정으로, 로컬 캐시를 사용합니다.
         */
        val READ_WRITE_THROUGH_WITH_NEAR_CACHE =
            RedissonCacheConfig(
                cacheMode = CacheMode.READ_WRITE,
                writeMode = WriteMode.WRITE_THROUGH,
                nearCacheEnabled = true
            )

        /**
         * 쓰기 지연(Write Behind) 캐시 설정입니다.
         * 높은 쓰기 처리량이 필요한 경우 적합합니다.
         */
        val WRITE_BEHIND =
            RedissonCacheConfig(
                cacheMode = CacheMode.READ_WRITE,
                writeMode = WriteMode.WRITE_BEHIND,
                nearCacheEnabled = false,
                writeBehindBatchSize = 50,
                writeBehindDelay = 1000
            )

        /**
         * 쓰기 지연 캐시 설정으로, 로컬 캐시를 사용합니다.
         */
        val WRITE_BEHIND_WITH_NEAR_CACHE =
            RedissonCacheConfig(
                cacheMode = CacheMode.READ_WRITE,
                writeMode = WriteMode.WRITE_BEHIND,
                nearCacheEnabled = true,
                writeBehindBatchSize = 50,
                writeBehindDelay = 1000
            )
    }

    /**
     * 캐시 모드를 나타내는 열거형입니다.
     */
    enum class CacheMode {
        /**
         * 읽기 전용 캐시 모드입니다.
         * 데이터는 DB에서 읽어오지만 캐시에 대한 수정은 DB에 반영되지 않습니다.
         */
        READ_ONLY,

        /**
         * 읽기 및 쓰기 모드입니다.
         * 캐시 수정이 DB에 반영되며, 설정에 따라 WRITE_THROUGH 또는 WRITE_BEHIND 모드로 작동합니다.
         */
        READ_WRITE,
    }

    /**
     * 이 설정을 기반으로 [MapOptions] 객체를 생성합니다.
     *
     * ## 주의사항
     * - [ttl], [maxSize], [deleteFromDBOnInvalidate]는 Redisson [MapOptions]에서 직접 지원되지 않습니다.
     *   이 값들이 기본값이 아닌 경우 [IllegalArgumentException]이 발생합니다.
     * - [ttl]이 필요한 경우 엔트리별 만료 API(`RMap.put(key, value, ttl, unit)`)를 사용하세요.
     * - [maxSize]가 필요한 경우 로컬 캐시 크기 제한([nearCacheMaxSize])이나 별도 정책을 적용하세요.
     *
     * @param name Redis 맵 이름
     * @return 설정이 적용된 [MapOptions] 인스턴스
     * @throws IllegalArgumentException [ttl], [maxSize], [deleteFromDBOnInvalidate]가 기본값이 아닌 경우
     */
    fun <K: Any, V> toMapOptions(name: String): MapOptions<K, V> {
        validateUnsupportedMapSettings()

        return MapOptions
            .name<K, V>(name)
            .codec(codec)
            .applyWriteOptions()
    }

    /**
     * 이 설정을 기반으로 [LocalCachedMapOptions] 객체를 생성합니다.
     *
     * ## 주의사항
     * - [ttl], [maxSize], [deleteFromDBOnInvalidate]는 Redisson [LocalCachedMapOptions]에서 직접 지원되지 않습니다.
     *   이 값들이 기본값이 아닌 경우 [IllegalArgumentException]이 발생합니다.
     * - 로컬 캐시 만료는 [nearCacheTtl], [nearCacheMaxIdleTime]으로 제어하세요.
     * - 로컬 캐시 크기는 [nearCacheMaxSize]로 제어하세요.
     * - [nearCacheEnabled]가 `false`인 경우 로컬 캐시 크기가 0으로 설정되어 로컬 캐싱이 비활성화됩니다.
     *
     * @param name Redis 맵 이름
     * @return 설정이 적용된 [LocalCachedMapOptions] 인스턴스
     * @throws IllegalArgumentException [ttl], [maxSize], [deleteFromDBOnInvalidate]가 기본값이 아닌 경우
     */
    fun <K: Any, V> toLocalCachedMapOptions(name: String): LocalCachedMapOptions<K, V> {
        validateUnsupportedMapSettings()

        return LocalCachedMapOptions
            .name<K, V>(name)
            .codec(codec)
            .applyWriteOptions()
            .applyNearCacheOptions()
    }

    private fun validateUnsupportedMapSettings() {
        require(ttl.isZero) {
            "ttl is not applied by Redisson MapOptions/LocalCachedMapOptions. Use per-entry expiration or map cache APIs instead. ttl=$ttl"
        }
        require(maxSize == 0) {
            "maxSize is not applied by Redisson MapOptions/LocalCachedMapOptions. Use a bounded local cache or custom eviction policy instead. maxSize=$maxSize"
        }
        require(!deleteFromDBOnInvalidate) {
            "deleteFromDBOnInvalidate is an application-level invalidation policy and is not applied by Redisson MapOptions/LocalCachedMapOptions."
        }
    }

    private fun <K: Any, V, T: ExMapOptions<T, K, V>> T.applyWriteOptions(): T =
        apply {
            if (cacheMode == CacheMode.READ_WRITE) {
                writeMode(writeMode)

                if (writeMode == WriteMode.WRITE_BEHIND) {
                    writeBehindBatchSize(writeBehindBatchSize)
                    writeBehindDelay(writeBehindDelay)
                }

                writeRetryAttempts(writeRetryAttempts)
                writeRetryInterval(writeRetryInterval)
            }
        }

    private fun <K: Any, V> LocalCachedMapOptions<K, V>.applyNearCacheOptions(): LocalCachedMapOptions<K, V> =
        apply {
            if (nearCacheEnabled) {
                evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LRU)
                cacheSize(nearCacheMaxSize)
                timeToLive(nearCacheTtl)
                maxIdle(nearCacheMaxIdleTime)
                syncStrategy(nearCacheSyncStrategy)
            } else {
                cacheSize(0)
                timeToLive(Duration.ZERO)
                maxIdle(Duration.ZERO)
            }
        }
}
