package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.repository.HasIdentifier
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SortOrder

interface CoRedisCacheRepository<T: HasIdentifier<ID>, ID: Any> {
    val cacheName: String

    suspend fun existsById(id: ID): Boolean

    suspend fun findById(id: ID): T
    suspend fun findByIdOrNull(id: ID): T?


    suspend fun findAll(): List<T>
    suspend fun findAll(sortBy: String, order: SortOrder = SortOrder.ASC): List<T>
    suspend fun findAll(where: Op<Boolean>): List<T>
    suspend fun findAll(where: Op<Boolean>, sortBy: String, order: SortOrder = SortOrder.ASC): List<T>

    suspend fun save(entity: T)

    suspend fun delete(entity: T)
    suspend fun deleteById(id: ID)

}

abstract class CoRedisRemoteCacheRepository<T: HasIdentifier<ID>, ID: Any>(
    val redissonClient: org.redisson.api.RedissonClient,
    override val cacheName: String,
    private val config: io.bluetape4k.redis.redisson.cache.RedisCacheConfig,
): CoRedisCacheRepository<T, ID> {

}

abstract class CoRedisNearCacheRepository<T: HasIdentifier<ID>, ID: Any>(
    val redissonClient: org.redisson.api.RedissonClient,
    override val cacheName: String,
    private val config: io.bluetape4k.redis.redisson.cache.RedisCacheConfig,
): CoRedisCacheRepository<T, ID> {

}
