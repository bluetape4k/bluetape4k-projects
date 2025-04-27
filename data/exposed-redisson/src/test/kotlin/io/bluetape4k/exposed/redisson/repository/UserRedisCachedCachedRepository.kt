package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.CacheSchema.UserDTO
import io.bluetape4k.exposed.redisson.CacheSchema.UserTable
import io.bluetape4k.exposed.redisson.CacheSchema.toUserDTO
import io.bluetape4k.exposed.redisson.ExposedRedisCacheConfig
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.sql.ResultRow
import org.redisson.api.RedissonClient

class UserRedisCachedCachedRepository(
    redissonClient: RedissonClient,
    cacheName: String = "exposed:users",
    config: ExposedRedisCacheConfig = ExposedRedisCacheConfig.READ_THROUGH,
): ExposedRedisCachedRepository<UserDTO, Long>(redissonClient, cacheName, config) {

    companion object: KLogging()

    override val table = UserTable

    override fun ResultRow.toEntity(): UserDTO = toUserDTO()

}
