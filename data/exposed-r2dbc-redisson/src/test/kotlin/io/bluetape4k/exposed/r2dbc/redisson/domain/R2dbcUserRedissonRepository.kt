package io.bluetape4k.exposed.r2dbc.redisson.domain

import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.toUserRecord
import io.bluetape4k.exposed.r2dbc.redisson.repository.AbstractR2dbcRedissonRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient
import java.time.Instant

class R2dbcUserRedissonRepository(
    redissonClient: RedissonClient,
    config: RedissonCacheConfig = RedissonCacheConfig.READ_WRITE_THROUGH.copy(name = "exposed:remote:r2dbc:users"),
): AbstractR2dbcRedissonRepository<Long, UserSchema.UserRecord>(
    redissonClient,
    config
) {
    companion object: KLoggingChannel()

    override val table: UserSchema.UserTable = UserSchema.UserTable

    override suspend fun ResultRow.toEntity(): UserSchema.UserRecord = toUserRecord()

    override fun extractId(entity: UserSchema.UserRecord): Long = entity.id

    override fun UpdateStatement.updateEntity(entity: UserSchema.UserRecord) {
        this[table.firstName] = entity.firstName
        this[table.lastName] = entity.lastName
        this[table.email] = entity.email
        this[table.updatedAt] = Instant.now()
    }

    override fun BatchInsertStatement.insertEntity(entity: UserSchema.UserRecord) {
        // NOTE: MapWriter 가 AutoIncremented ID 를 가진 테이블에 대해 INSERT 를 수행하지 않습니다.
        if (UserSchema.UserTable.id.autoIncColumnType == null) {
            this[UserSchema.UserTable.id] = entity.id
        }
        this[UserSchema.UserTable.firstName] = entity.firstName
        this[UserSchema.UserTable.lastName] = entity.lastName
        this[UserSchema.UserTable.email] = entity.email
    }
}
