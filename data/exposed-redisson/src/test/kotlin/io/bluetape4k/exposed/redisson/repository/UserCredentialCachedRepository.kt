package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.repository.UserCredentialSchema.UserCredential
import io.bluetape4k.exposed.redisson.repository.UserCredentialSchema.UserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserCredentialSchema.toUserCredential
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.redisson.api.RedissonClient
import java.time.Instant

class UserCredentialCachedRepository(
    redissonClient: RedissonClient,
    cacheName: String = "exposed:users",
    config: RedisCacheConfig = RedisCacheConfig.READ_WRITE_THROUGH,
): AbstractExposedRedisCachedRepository<UserCredential, Long>(redissonClient, cacheName, config) {

    companion object: KLogging()

    override val entityTable = UserCredentialTable

    override fun ResultRow.toEntity(): UserCredential = toUserCredential()


    override fun updateStatement(statement: UpdateStatement, entity: UserCredential) {
        statement[UserCredentialTable.loginId] = entity.loginId
        statement[UserCredentialTable.email] = entity.email
        statement[UserCredentialTable.lastLoginAt] = entity.lastLoginAt
        statement[UserCredentialTable.updatedAt] = Instant.now()
    }

    override fun batchInsertStatement(statement: BatchInsertStatement, entity: UserCredential) {
        statement[UserCredentialTable.id] = entity.id
        statement[UserCredentialTable.loginId] = entity.loginId
        statement[UserCredentialTable.email] = entity.email
        statement[UserCredentialTable.lastLoginAt] = entity.lastLoginAt
        statement[UserCredentialTable.createdAt] = entity.createdAt
    }
}
