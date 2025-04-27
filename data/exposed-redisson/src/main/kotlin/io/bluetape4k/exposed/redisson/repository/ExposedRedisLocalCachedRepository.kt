package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.ExposedEntityMapLoader
import io.bluetape4k.exposed.redisson.ExposedEntityMapWriter
import io.bluetape4k.exposed.redisson.ExposedRedisCacheConfig
import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.localCachedMap
import io.bluetape4k.support.requireNotNull
import org.redisson.api.RLocalCachedMap
import org.redisson.api.RedissonClient
import org.redisson.api.map.WriteMode

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
abstract class ExposedRedisLocalCachedRepository<T: HasIdentifier<ID>, ID: Any>(
    private val redissonClient: RedissonClient,
    private val cacheName: String,
    private val config: ExposedRedisCacheConfig = ExposedRedisCacheConfig.Companion.READ_THROUGH,
): ExposedRedisRepository<T, ID> {

    companion object: KLogging()

    open val mapLoader: ExposedEntityMapLoader<ID, T> by lazy { ExposedEntityMapLoader(table) { toEntity() } }
    open val mapWriter: ExposedEntityMapWriter<ID, T>? =
        null  // TODO: DTO -> Entity 로 변환 내지는 UpdateStatement 를 제공해야 한다 = ExposedEntityMapWriter<ID, T>(table) { map -> }

    override val cache: RLocalCachedMap<ID, T?> by lazy {
        localCachedMap(cacheName, redissonClient) {
            if (config.readThrough) {
                loader(mapLoader)
            }
            if (config.writeThrough) {
                mapWriter.requireNotNull("mapWriter")
                writer(mapWriter)
                writeMode(WriteMode.WRITE_THROUGH)
            }
            retryAttempts(config.retryAttempts)
            retryInterval(config.retryInterval)
            timeToLive(config.timeToLive)
            codec(config.codec)
        }
    }
}
