package io.bluetape4k.exposed.r2dbc.redisson.domain

import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.toUserCredentialsRecord
import io.bluetape4k.exposed.r2dbc.redisson.repository.AbstractR2dbcRedissonRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient
import java.time.Instant
import java.util.*

class R2dbcUserCredentialRedissonRepository(
    redissonClient: RedissonClient,
    config: RedissonCacheConfig = RedissonCacheConfig.READ_WRITE_THROUGH.copy(name = "exposed:remote:r2dbc:user-credentials"),
): AbstractR2dbcRedissonRepository<UUID, UserSchema.UserCredentialsRecord>(
    redissonClient,
    config
) {
    companion object: KLoggingChannel()

    override val table: UserSchema.UserCredentialsTable = UserSchema.UserCredentialsTable

    override suspend fun ResultRow.toEntity(): UserSchema.UserCredentialsRecord = toUserCredentialsRecord()

    override fun extractId(entity: UserSchema.UserCredentialsRecord): UUID = entity.id

    override fun UpdateStatement.updateEntity(entity: UserSchema.UserCredentialsRecord) {
        this[table.loginId] = entity.loginId
        this[table.email] = entity.email
        this[table.lastLoginAt] = entity.lastLoginAt
        this[table.updatedAt] = Instant.now()
    }

    override fun BatchInsertStatement.insertEntity(entity: UserSchema.UserCredentialsRecord) {
        // NOTE: MapWriter 가 AutoIncremented ID 를 가진 테이블에 대해 INSERT 를 수행하지 않습니다.
        this[UserSchema.UserCredentialsTable.id] = entity.id
        this[UserSchema.UserCredentialsTable.loginId] = entity.loginId
        this[UserSchema.UserCredentialsTable.email] = entity.email
        this[UserSchema.UserCredentialsTable.lastLoginAt] = entity.lastLoginAt
    }
}
