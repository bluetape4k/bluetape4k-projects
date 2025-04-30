package io.bluetape4k.redis.redisson.cache

import io.bluetape4k.redis.redisson.RedissonCodecs
import org.redisson.api.map.WriteMode
import org.redisson.api.options.LocalCachedMapOptions
import org.redisson.api.options.MapOptions
import org.redisson.client.codec.Codec
import java.time.Duration

/**
 * Redis 캐시 설정을 위한 클래스입니다.
 *
 * @property cacheMode 캐시 모드 (읽기 전용, 쓰기 전용, 읽기/쓰기 모두 등)
 * @property writeMode 쓰기 모드 (WRITE_THROUGH, WRITE_BEHIND)
 * @property deleteFromDBOnInvalidate 캐시 무효화 시 DB에서도 삭제할지 여부
 * @property ttl 캐시 항목의 기본 만료 시간
 * @property maxSize 캐시의 최대 크기 (0이면 무제한)
 * @property codec 캐시 항목의 직렬화 및 역직렬화에 사용할 Codec (기본은 [RedissonCodecs.LZ4Fury])
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
data class RedisCacheConfig(
    val cacheMode: CacheMode = CacheMode.READ_WRITE,
    val writeMode: WriteMode = WriteMode.WRITE_THROUGH,
    val deleteFromDBOnInvalidate: Boolean = false,
    val ttl: Duration = Duration.ZERO,
    val maxSize: Int = 0,
    val codec: Codec = RedissonCodecs.LZ4FuryComposite,

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
        val READ_ONLY = RedisCacheConfig(
            cacheMode = CacheMode.READ_ONLY,
            nearCacheEnabled = false
        )

        /**
         * 읽기 전용 캐시 설정으로, 로컬 캐시를 사용합니다.
         * 자주 조회되는 데이터에 대해 성능을 향상시킵니다.
         */
        val READ_ONLY_WITH_NEAR_CACHE = RedisCacheConfig(
            cacheMode = CacheMode.READ_ONLY,
            nearCacheEnabled = true
        )

        /**
         * 읽기-쓰기 통과(Read-Write Through) 캐시 설정입니다.
         * 캐시와 DB 간의 데이터 일관성을 보장합니다.
         */
        val READ_WRITE_THROUGH = RedisCacheConfig(
            cacheMode = CacheMode.READ_WRITE,
            writeMode = WriteMode.WRITE_THROUGH,
            nearCacheEnabled = false
        )

        /**
         * 읽기-쓰기 통과 캐시 설정으로, 로컬 캐시를 사용합니다.
         */
        val READ_WRITE_THROUGH_WITH_NEAR_CACHE = RedisCacheConfig(
            cacheMode = CacheMode.READ_WRITE,
            writeMode = WriteMode.WRITE_THROUGH,
            nearCacheEnabled = true
        )

        /**
         * 쓰기 지연(Write Behind) 캐시 설정입니다.
         * 높은 쓰기 처리량이 필요한 경우 적합합니다.
         */
        val WRITE_BEHIND = RedisCacheConfig(
            cacheMode = CacheMode.READ_WRITE,
            writeMode = WriteMode.WRITE_BEHIND,
            nearCacheEnabled = false,
            writeBehindBatchSize = 50,
            writeBehindDelay = 1000
        )

        /**
         * 쓰기 지연 캐시 설정으로, 로컬 캐시를 사용합니다.
         */
        val WRITE_BEHIND_WITH_NEAR_CACHE = RedisCacheConfig(
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
        READ_WRITE
    }

    /**
     * 이 설정으로 기본 MapOptions 객체를 생성합니다.
     */
    fun <K: Any, V> toMapOptions(name: String): MapOptions<K, V> {
        return MapOptions.name<K, V>(name).apply {
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
    }

    /**
     * 이 설정으로 LocalCachedMapOptions 객체를 생성합니다.
     */
    fun <K: Any, V> toLocalCachedMapOptions(name: String): LocalCachedMapOptions<K, V> {
        return if (nearCacheEnabled) {
            LocalCachedMapOptions.name<K, V>(name).apply {
                if (cacheMode == CacheMode.READ_WRITE) {
                    writeMode(writeMode)

                    if (writeMode == WriteMode.WRITE_BEHIND) {
                        writeBehindBatchSize(writeBehindBatchSize)
                        writeBehindDelay(writeBehindDelay)
                    }

                    writeRetryAttempts(writeRetryAttempts)
                    writeRetryInterval(writeRetryInterval)
                }

                evictionPolicy(LocalCachedMapOptions.EvictionPolicy.LRU)
                cacheSize(nearCacheMaxSize)
                timeToLive(nearCacheTtl)
                syncStrategy(nearCacheSyncStrategy)
            }
        } else {
            LocalCachedMapOptions.name<K, V>(name).apply {
                if (cacheMode == CacheMode.READ_WRITE) {
                    writeMode(writeMode)

                    if (writeMode == WriteMode.WRITE_BEHIND) {
                        writeBehindBatchSize(writeBehindBatchSize)
                        writeBehindDelay(writeBehindDelay)
                    }

                    writeRetryAttempts(writeRetryAttempts)
                    writeRetryInterval(writeRetryInterval)
                }

                // NearCache 비활성화 - 기본 설정 사용
                cacheSize(0)
            }
        }
    }
}
