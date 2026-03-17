package io.bluetape4k.exposed.redisson.domain

import io.bluetape4k.exposed.redisson.domain.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.redisson.domain.UserSchema.toUserCredentialsRecord
import io.bluetape4k.exposed.redisson.repository.AbstractSuspendedJdbcRedissonRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient
import java.time.Instant
import java.util.*

class SuspendedUserCredentialCacheRepository(
    redissonClient: RedissonClient,
    cacheName: String = "exposed:remote:suspended:users",
    config: RedissonCacheConfig = RedissonCacheConfig.READ_WRITE_THROUGH,
) : AbstractSuspendedJdbcRedissonRepository<UUID, UserCredentialsRecord>(
        redissonClient,
        cacheName,
        config
    ) {
    companion object : KLoggingChannel()

    override val table: UserCredentialsTable = UserCredentialsTable

    override fun ResultRow.toEntity(): UserCredentialsRecord = toUserCredentialsRecord()

    override fun extractId(entity: UserCredentialsRecord): UUID = entity.id

    override fun doUpdateEntity(
        statement: UpdateStatement,
        entity: UserCredentialsRecord,
    ) {
        statement[table.loginId] = entity.loginId
        statement[table.email] = entity.email
        statement[table.lastLoginAt] = entity.lastLoginAt
        statement[table.updatedAt] = Instant.now()
    }

    override fun doInsertEntity(
        statement: BatchInsertStatement,
        entity: UserCredentialsRecord,
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
