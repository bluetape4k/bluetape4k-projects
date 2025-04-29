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
@Deprecated("Use `RedisRemoteEntityRepository` instead.")
abstract class AbstractExposedRedisCachedRepository<T: HasIdentifier<ID>, ID: Any>(
    redissonClient: RedissonClient,
    cacheName: String,
    config: RedisCacheConfig = RedisCacheConfig.READ_WRITE_THROUGH,
): AbstractExposedRedisRepository<T, ID>(redissonClient, cacheName, config) {

    companion object: KLogging()


    override val cache: RMapCache<ID, T?> by lazy {
        mapCache(cacheName, redissonClient) {
            if (config.isReadOnly) {
                loader(mapLoader)
            }
            if (config.isReadWrite) {
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

    /**
     * [ExposedEntityMapWriter] 에서 캐시에서 변경된 내용을 Write Through로 DB에 반영하는 함수입니다.
     */
    protected open fun doUpdateEntity(statement: UpdateStatement, entity: T) {
        if (config.isReadWrite) {
            error("MapWriter 에서 변경된 cache item을 DB에 반영할 수 있도록 재정의해주세요. ")
        }
    }

    /**
     * [ExposedEntityMapWriter] 에서 캐시에서 추가된 내용을 Write Through로 DB에 반영하는 함수입니다.
     */
    protected open fun doBatchInsertEntity(statement: BatchInsertStatement, entity: T) {
        if (config.isReadWrite) {
            error("MapWriter 에서 추가된 cache item을 DB에 추가할 수 있도록 재정의해주세요. ")
        }
    }

    /**
     * Write Through 모드라면 [ExposedEntityMapWriter]를 생성하여 제공합니다.
     * Read Through Only 라면 null을 반환합니다.
     */
    override val mapWriter: ExposedEntityMapWriter<ID, T>? by lazy {
        when {
            config.isReadWrite -> ExposedEntityMapWriter(
                entityTable = entityTable,
                toEntity = { toEntity() },
                updateBody = { stmt, entity -> doUpdateEntity(stmt, entity) },
                batchInsertBody = { entity -> doBatchInsertEntity(this, entity) },
                deleteFromDbOnInvalidate = config.deleteFromDbOnInvalidate,
            )
            else -> null
        }
    }
}
