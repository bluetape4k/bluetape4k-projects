package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import io.bluetape4k.redis.redisson.cache.localCachedMap
import io.bluetape4k.support.requireNotNull
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RedissonClient
import org.redisson.api.map.WriteMode
import java.time.Duration

/**
 * ExposedRedisRepository는 Exposed와 Redisson을 사용하여 Redis에 데이터를 캐싱하는 Repository입니다.
 *
 * @param T Entity Type      Exposed 용 엔티티는 Redis 저장 시 Serializer 때문에 문제가 됩니다. 꼭 Serializable DTO를 사용해 주세요.
 * @param ID Entity ID Type
 *
 * @param redissonClient Redisson Client
 * @param cacheName Redis Cache Name
 * @param config ExposedRedisCacheConfig
 */
abstract class AbstractExposedRedisLocalCachedRepository<T: HasIdentifier<ID>, ID: Any>(
    redissonClient: RedissonClient,
    cacheName: String,
    config: RedisCacheConfig = RedisCacheConfig.READ_ONLY,
): AbstractExposedRedisRepository<T, ID>(redissonClient, cacheName, config) {

    companion object: KLogging()

    override val cache: RLocalCachedMap<ID, T?> by lazy {
        localCachedMap(cacheName, redissonClient) {
            if (config.canRead) {
                loader(mapLoader)
            }
            if (config.canWrite) {
                mapWriter.requireNotNull("mapWriter")
                writer(mapWriter)
                writeMode(WriteMode.WRITE_THROUGH)
            }

            codec(config.codec)
            syncStrategy(config.nearCacheSyncStrategy)
            writeRetryAttempts(config.writeRetryAttempts)
            writeRetryInterval(config.writeRetryInterval)
            timeToLive(config.ttl)
            if (config.nearCacheMaxIdleTime > Duration.ZERO) {
                maxIdle(config.nearCacheMaxIdleTime)
            }
        }
    }

    fun clearLocalCache() {
        cache.clearLocalCache()
    }
}
