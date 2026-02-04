package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.toUserCredentialsRecord
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient
import java.time.Instant
import java.util.*

class UserCredentialCacheRepository(
    redissonClient: RedissonClient,
    cacheName: String = "exposed:user-credentials",
    config: RedisCacheConfig = RedisCacheConfig.READ_WRITE_THROUGH,
): AbstractExposedCacheRepository<UserCredentialsRecord, UUID>(redissonClient, cacheName, config) {

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
        entity: UserSchema.UserCredentialsRecord,
    ) {
        // NOTE: MapWriter 가 AutoIncremented ID 를 가진 테이블에 대해 INSERT 를 수행하지 않습니다.
        statement[entityTable.id] = entity.id
        statement[entityTable.loginId] = entity.loginId
        statement[entityTable.email] = entity.email
        statement[entityTable.lastLoginAt] = entity.lastLoginAt
    }
}
