package io.bluetape4k.exposed.r2dbc.redisson.repository

import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserCredentialDTO
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserCredentialTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.toUserCredentialDTO
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient
import java.time.Instant
import java.util.*

class R2dbcUserCredentialCacheRepository(
    redissonClient: RedissonClient,
    cacheName: String = "exposed:remote:r2dbc:users",
    config: RedisCacheConfig = RedisCacheConfig.READ_WRITE_THROUGH,
): AbstractR2dbcCacheRepository<UserCredentialDTO, UUID>(redissonClient, cacheName, config) {

    companion object: KLoggingChannel()

    override val entityTable: UserCredentialTable = UserCredentialTable
    override suspend fun ResultRow.toEntity(): UserCredentialDTO = toUserCredentialDTO()

    override fun doUpdateEntity(
        statement: UpdateStatement,
        entity: UserCredentialDTO,
    ) {
        statement[entityTable.loginId] = entity.loginId
        statement[entityTable.email] = entity.email
        statement[entityTable.lastLoginAt] = entity.lastLoginAt
        statement[entityTable.updatedAt] = Instant.now()
    }

    override fun doInsertEntity(
        statement: BatchInsertStatement,
        entity: UserCredentialDTO,
    ) {
        // NOTE: MapWriter 가 AutoIncremented ID 를 가진 테이블에 대해 INSERT 를 수행하지 않습니다.
        if (entityTable.id.autoIncColumnType == null) {
            statement[entityTable.id] = entity.id
        }
        statement[entityTable.id] = entity.id
        statement[entityTable.loginId] = entity.loginId
        statement[entityTable.email] = entity.email
        statement[entityTable.lastLoginAt] = entity.lastLoginAt
    }
}
