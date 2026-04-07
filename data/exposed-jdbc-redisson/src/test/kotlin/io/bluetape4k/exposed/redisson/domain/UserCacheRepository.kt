package io.bluetape4k.exposed.redisson.domain

import io.bluetape4k.exposed.redisson.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserTable
import io.bluetape4k.exposed.redisson.domain.UserSchema.toUserRecord
import io.bluetape4k.exposed.redisson.repository.AbstractJdbcRedissonRepository
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.autoIncColumnType
import org.jetbrains.exposed.v1.core.statements.BatchInsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.redisson.api.RedissonClient
import java.time.Instant

class UserCacheRepository(
    redissonClient: RedissonClient,
    config: RedissonCacheConfig = RedissonCacheConfig.READ_WRITE_THROUGH.copy(name = "exposed:remote:users"),
) : AbstractJdbcRedissonRepository<Long, UserRecord>(
        redissonClient,
        config
    ) {
    companion object : KLogging()

    override val table: UserTable = UserTable

    override fun ResultRow.toEntity(): UserRecord = toUserRecord()

    override fun extractId(entity: UserRecord): Long = entity.id

    override fun UpdateStatement.updateEntity(entity: UserRecord) {
        this[table.firstName] = entity.firstName
        this[table.lastName] = entity.lastName
        this[table.email] = entity.email
        this[table.updatedAt] = Instant.now()
    }

    override fun BatchInsertStatement.insertEntity(entity: UserRecord) {
        // NOTE: MapWriter 가 AutoIncremented ID 를 가진 테이블에 대해 INSERT 를 수행하지 않습니다.
        if (UserTable.id.autoIncColumnType == null) {
            this[UserTable.id] = entity.id
        }
        this[UserTable.firstName] = entity.firstName
        this[UserTable.lastName] = entity.lastName
        this[UserTable.email] = entity.email
    }
}
