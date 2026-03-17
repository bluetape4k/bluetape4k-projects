package io.bluetape4k.exposed.redisson.domain

import io.bluetape4k.exposed.redisson.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserTable
import io.bluetape4k.exposed.redisson.domain.UserSchema.toUserRecord
import io.bluetape4k.exposed.redisson.repository.AbstractSuspendedJdbcRedissonRepository
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient
import java.time.Instant

class SuspendedUserCacheRepository(
    redissonClient: RedissonClient,
    cacheName: String = "exposed:remote:suspended:users",
    config: RedissonCacheConfig = RedissonCacheConfig.READ_WRITE_THROUGH,
) : AbstractSuspendedJdbcRedissonRepository<Long, UserRecord>(
        redissonClient,
        cacheName,
        config
    ) {
    companion object : KLoggingChannel()

    override val table: UserTable = UserTable

    override fun ResultRow.toEntity(): UserRecord = toUserRecord()

    override fun extractId(entity: UserRecord): Long = entity.id

    override fun doUpdateEntity(
        statement: UpdateStatement,
        entity: UserRecord,
    ) {
        statement[table.firstName] = entity.firstName
        statement[table.lastName] = entity.lastName
        statement[table.email] = entity.email
        statement[table.updatedAt] = Instant.now()
    }

    override fun doInsertEntity(
        statement: BatchInsertStatement,
        entity: UserRecord,
    ) {
        // NOTE: MapWriter 가 AutoIncremented ID 를 가진 테이블에 대해 INSERT 를 수행하지 않습니다.
        statement[table.id] = entity.id
        statement[table.firstName] = entity.firstName
        statement[table.lastName] = entity.lastName
        statement[table.email] = entity.email
    }
}
