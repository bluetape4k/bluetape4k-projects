package io.bluetape4k.exposed.redisson.domain

import io.bluetape4k.exposed.redisson.domain.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.redisson.domain.UserSchema.toUserCredentialsRecord
import io.bluetape4k.exposed.redisson.repository.AbstractSuspendedJdbcRedissonRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient
import java.time.Instant
import java.util.*

class SuspendedUserCredentialCacheRepository(
    redissonClient: RedissonClient,
    config: RedissonCacheConfig = RedissonCacheConfig.READ_WRITE_THROUGH.copy(name = "exposed:remote:suspended:user-credentials"),
): AbstractSuspendedJdbcRedissonRepository<UUID, UserCredentialsRecord>(
    redissonClient,
    config
) {
    companion object: KLoggingChannel()

    override val table: UserCredentialsTable = UserCredentialsTable

    override fun ResultRow.toEntity(): UserCredentialsRecord = toUserCredentialsRecord()

    override fun extractId(entity: UserCredentialsRecord): UUID = entity.id

    override fun UpdateStatement.updateEntity(entity: UserCredentialsRecord) {
        this[table.loginId] = entity.loginId
        this[table.email] = entity.email
        this[table.lastLoginAt] = entity.lastLoginAt
        this[table.updatedAt] = Instant.now()
    }

    override fun BatchInsertStatement.insertEntity(entity: UserCredentialsRecord) {
        // NOTE: MapWriter 가 AutoIncremented ID 를 가진 테이블에 대해 INSERT 를 수행하지 않습니다.
        this[UserCredentialsTable.id] = entity.id
        this[UserCredentialsTable.loginId] = entity.loginId
        this[UserCredentialsTable.email] = entity.email
        this[UserCredentialsTable.lastLoginAt] = entity.lastLoginAt
    }
}
