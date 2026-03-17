package io.bluetape4k.exposed.redisson.domain

import io.bluetape4k.exposed.redisson.domain.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.redisson.domain.UserSchema.toUserCredentialsRecord
import io.bluetape4k.exposed.redisson.repository.AbstractJdbcRedissonRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient
import java.time.Instant
import java.util.*

class UserCredentialCacheRepository(
    redissonClient: RedissonClient,
    cacheName: String = "exposed:user-credentials",
    config: RedissonCacheConfig = RedissonCacheConfig.READ_WRITE_THROUGH,
): AbstractJdbcRedissonRepository<UUID, UserCredentialsTable, UserCredentialsRecord>(
    redissonClient,
    cacheName,
    config
) {

    companion object: KLogging()

    override val entityTable: UserCredentialsTable = UserCredentialsTable
    override fun ResultRow.toEntity(): UserCredentialsRecord = toUserCredentialsRecord()

    override fun doUpdateEntity(
        statement: UpdateStatement,
        entity: UserCredentialsRecord,
    ) {
        statement[entityTable.loginId] = entity.loginId
        statement[entityTable.email] = entity.email
        statement[entityTable.lastLoginAt] = entity.lastLoginAt
        statement[entityTable.updatedAt] = Instant.now()
    }

    override fun doInsertEntity(
        statement: BatchInsertStatement,
        entity: UserCredentialsRecord,
    ) {
        // NOTE: MapWriter 가 AutoIncremented ID 를 가진 테이블에 대해 INSERT 를 수행하지 않습니다.
        statement[entityTable.id] = entity.id
        statement[entityTable.loginId] = entity.loginId
        statement[entityTable.email] = entity.email
        statement[entityTable.lastLoginAt] = entity.lastLoginAt
    }
}
