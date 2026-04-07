package io.bluetape4k.exposed.r2dbc.redisson.domain

import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.toUserCredentialsRecord
import io.bluetape4k.exposed.r2dbc.redisson.repository.AbstractR2dbcRedissonRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient
import java.time.Instant
import java.util.*

class R2dbcUserCredentialRedissonRepository(
    redissonClient: RedissonClient,
    config: RedissonCacheConfig = RedissonCacheConfig.READ_WRITE_THROUGH.copy(name = "exposed:remote:r2dbc:user-credentials"),
) : AbstractR2dbcRedissonRepository<UUID, UserSchema.UserCredentialsRecord>(
        redissonClient,
        config
    ) {
    companion object : KLoggingChannel()

    override val table: UserSchema.UserCredentialsTable = UserSchema.UserCredentialsTable

    override suspend fun ResultRow.toEntity(): UserSchema.UserCredentialsRecord = toUserCredentialsRecord()

    override fun extractId(entity: UserSchema.UserCredentialsRecord): UUID = entity.id

    override fun doUpdateEntity(
        statement: UpdateStatement,
        entity: UserSchema.UserCredentialsRecord,
    ) {
        statement[table.loginId] = entity.loginId
        statement[table.email] = entity.email
        statement[table.lastLoginAt] = entity.lastLoginAt
        statement[table.updatedAt] = Instant.now()
    }

    override fun doInsertEntity(
        statement: BatchInsertStatement,
        entity: UserSchema.UserCredentialsRecord,
    ) {
        // NOTE: MapWriter 가 AutoIncremented ID 를 가진 테이블에 대해 INSERT 를 수행하지 않습니다.
        if (table.id.autoIncColumnType == null) {
            statement[table.id] = entity.id
        }
        statement[table.id] = entity.id
        statement[table.loginId] = entity.loginId
        statement[table.email] = entity.email
        statement[table.lastLoginAt] = entity.lastLoginAt
    }
}
