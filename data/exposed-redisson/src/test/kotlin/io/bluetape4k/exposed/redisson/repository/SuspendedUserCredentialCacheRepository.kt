package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.repository.UserSchema.toUserCredential
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.redisson.api.RedissonClient
import java.time.Instant

class SuspendedUserCredentialCacheRepository(
    redissonClient: RedissonClient,
    cacheName: String = "exposed:remote:suspended:users",
    config: RedisCacheConfig = RedisCacheConfig.READ_WRITE_THROUGH,
): SuspendedExposedRedisRepository<UserSchema.UserCredentialDTO, String>(redissonClient, cacheName, config) {

    companion object: KLogging()

    override val entityTable: UserSchema.UserCredentialTable = UserSchema.UserCredentialTable
    override fun ResultRow.toEntity(): UserSchema.UserCredentialDTO = toUserCredential()

    override fun doUpdateEntity(
        statement: UpdateStatement,
        entity: UserSchema.UserCredentialDTO,
    ) {
        statement[entityTable.loginId] = entity.loginId
        statement[entityTable.email] = entity.email
        statement[entityTable.lastLoginAt] = entity.lastLoginAt
        statement[entityTable.updatedAt] = Instant.now()
    }

    override fun doBatchInsertEntity(
        statement: BatchInsertStatement,
        entity: UserSchema.UserCredentialDTO,
    ) {
        // NOTE: MapWriter 가 AutoIncremented ID 를 가진 테이블에 대해 INSERT 를 수행하지 않습니다.
        statement[entityTable.id] = entity.id
        statement[entityTable.email] = entity.email
        statement[entityTable.lastLoginAt] = entity.lastLoginAt
    }
}
