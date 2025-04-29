package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.repository.UserSchema.toUserDTO
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.BatchInsertStatement
import org.jetbrains.exposed.sql.statements.UpdateStatement
import org.redisson.api.RedissonClient
import java.time.Instant

class UserCacheRepository(
    redissonClient: RedissonClient,
    cacheName: String = "exposed:users",
    config: RedisCacheConfig = RedisCacheConfig.READ_WRITE_THROUGH,
): ExposedRemoteCacheRepository<UserSchema.UserDTO, Long>(redissonClient, cacheName, config) {

    companion object: KLogging()

    override val entityTable: UserSchema.UserTable = UserSchema.UserTable
    override fun ResultRow.toEntity(): UserSchema.UserDTO = toUserDTO()

    override fun doUpdateEntity(statement: UpdateStatement, entity: UserSchema.UserDTO) {
        statement[entityTable.firstName] = entity.firstName
        statement[entityTable.lastName] = entity.lastName
        statement[entityTable.email] = entity.email
        statement[entityTable.updatedAt] = Instant.now()
    }

    override fun doBatchInsertEntity(statement: BatchInsertStatement, entity: UserSchema.UserDTO) {
        // NOTE: UserTable 은 AutoIncremented ID 이므로, id 를 넣지 않습니다.
        // statement[entityTable.id] = entity.id
        statement[entityTable.firstName] = entity.firstName
        statement[entityTable.lastName] = entity.lastName
        statement[entityTable.email] = entity.email
        statement[entityTable.updatedAt] = Instant.now()
    }
}
