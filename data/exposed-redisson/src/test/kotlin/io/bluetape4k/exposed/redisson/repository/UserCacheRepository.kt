package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.repository.UserSchema.UserRecord
import io.bluetape4k.exposed.redisson.repository.UserSchema.toUserRecord
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient
import java.time.Instant

class UserCacheRepository(
    redissonClient: RedissonClient,
    cacheName: String = "exposed:remote:users",
    config: RedisCacheConfig = RedisCacheConfig.READ_WRITE_THROUGH,
): AbstractExposedCacheRepository<UserRecord, Long>(redissonClient, cacheName, config) {

    companion object: KLogging()

    override val entityTable: UserSchema.UserTable = UserSchema.UserTable
    override fun ResultRow.toEntity(): UserRecord = toUserRecord()

    override fun doUpdateEntity(statement: UpdateStatement, entity: UserRecord) {
        statement[entityTable.firstName] = entity.firstName
        statement[entityTable.lastName] = entity.lastName
        statement[entityTable.email] = entity.email
        statement[entityTable.updatedAt] = Instant.now()
    }

    override fun doInsertEntity(statement: BatchInsertStatement, entity: UserRecord) {
        // NOTE: MapWriter 가 AutoIncremented ID 를 가진 테이블에 대해 INSERT 를 수행하지 않습니다.
        if (entityTable.id.autoIncColumnType == null) {
            statement[entityTable.id] = entity.id
        }
        statement[entityTable.firstName] = entity.firstName
        statement[entityTable.lastName] = entity.lastName
        statement[entityTable.email] = entity.email
    }
}
