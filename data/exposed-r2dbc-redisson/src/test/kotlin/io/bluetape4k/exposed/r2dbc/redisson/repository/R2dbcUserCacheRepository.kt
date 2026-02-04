package io.bluetape4k.exposed.r2dbc.redisson.repository

import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.toUserRecord
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient
import java.time.Instant

class R2dbcUserCacheRepository(
    redissonClient: RedissonClient,
    cacheName: String = "exposed:remote:r2dbc:users",
    config: RedisCacheConfig = RedisCacheConfig.READ_WRITE_THROUGH,
): AbstractR2dbcCacheRepository<UserRecord, Long>(redissonClient, cacheName, config) {

    companion object: KLoggingChannel()

    override val entityTable: UserTable = UserTable
    override suspend fun ResultRow.toEntity(): UserRecord = toUserRecord()

    override fun doUpdateEntity(
        statement: UpdateStatement,
        entity: UserRecord,
    ) {
        statement[entityTable.firstName] = entity.firstName
        statement[entityTable.lastName] = entity.lastName
        statement[entityTable.email] = entity.email
        statement[entityTable.updatedAt] = Instant.now()
    }

    override fun doInsertEntity(
        statement: BatchInsertStatement,
        entity: UserRecord,
    ) {
        // NOTE: MapWriter 가 AutoIncremented ID 를 가진 테이블에 대해 INSERT 를 수행하지 않습니다.
        statement[entityTable.id] = entity.id
        statement[entityTable.firstName] = entity.firstName
        statement[entityTable.lastName] = entity.lastName
        statement[entityTable.email] = entity.email
    }
}
