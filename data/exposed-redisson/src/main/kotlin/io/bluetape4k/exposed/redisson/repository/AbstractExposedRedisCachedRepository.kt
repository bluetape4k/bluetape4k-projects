package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.ExposedEntityMapWriter
import io.bluetape4k.exposed.repository.HasIdentifier
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import io.bluetape4k.redis.redisson.cache.mapCache
import io.bluetape4k.support.requireNotNull
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.redisson.api.EvictionMode
import org.redisson.api.RMapCache
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
abstract class AbstractExposedRedisCachedRepository<T: HasIdentifier<ID>, ID: Any>(
    redissonClient: RedissonClient,
    cacheName: String,
    config: RedisCacheConfig = RedisCacheConfig.READ_WRITE_THROUGH,
): AbstractExposedRedisRepository<T, ID>(redissonClient, cacheName, config) {

    companion object: KLogging()


    override val cache: RMapCache<ID, T?> by lazy {
        mapCache(cacheName, redissonClient) {
            if (config.canRead) {
                loader(mapLoader)
            }
            if (config.canWrite) {
                mapWriter.requireNotNull("mapWriter")
                writer(mapWriter)
                writeMode(WriteMode.WRITE_THROUGH)
            }
            codec(config.codec)
            writeRetryAttempts(config.writeRetryAttempts)
            writeRetryInterval(config.writeRetryInterval)


        }.apply {
            if (config.nearCacheMaxSize > 0) {
                setMaxSize(config.nearCacheMaxSize, EvictionMode.LRU)
            }
        }
    }

    protected open fun updateStatement(statement: UpdateStatement, entity: T) {
        if (config.canWrite) {
            error("MapWriter 에서 변경된 cache item을 DB에 반영할 수 있도록 재정의해주세요. ")
        }
    }

    protected open fun batchInsertStatement(statement: BatchInsertStatement, entity: T) {
        if (config.canWrite) {
            error("MapWriter 에서 추가된 cache item을 DB에 추가할 수 있도록 재정의해주세요. ")
        }
    }

    override val mapWriter: ExposedEntityMapWriter<ID, T>? by lazy {
        when {
            config.canWrite -> ExposedEntityMapWriter(
                entityTable = entityTable,
                toEntity = { toEntity() },
                updateBody = { stmt, entity -> updateStatement(stmt, entity) },
                batchInsertBody = { entity -> batchInsertStatement(this, entity) },
                deleteFromDbOnInvalidate = config.deleteFromDbOnInvalidate,
            )
            else -> null
        }
    }
}
