package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredential
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserDTO
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withUserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withUserTable
import io.bluetape4k.exposed.redisson.repository.scenarios.ReadWriteThroughScenario
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.redis.redisson.RedissonCodecs
import io.bluetape4k.redis.redisson.RedissonCodecs.LZ4Fury
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Nested
import org.redisson.codec.CompositeCodec

class ReadWriteThroughCacheTest: AbstractRedissonTest() {

    abstract class AutoIncIdReadWriteThrough: ReadWriteThroughScenario<UserDTO, Long>() {

        override fun withEntityTable(
            testDB: TestDB,
            statement: Transaction.() -> Unit,
        ) = withUserTable(testDB, statement)

        override fun getExistingId(): Long = transaction {
            UserTable.select(UserTable.id).limit(1).first()[UserTable.id].value
        }

        override fun getExistingIds(): List<Long> = transaction {
            UserTable.selectAll().map { it[UserTable.id].value }
        }

        override fun getNonExistentId(): Long = Long.MIN_VALUE

        override fun updateEntityEmail(entity: UserDTO): UserDTO =
            entity.copy(email = "updated@example.com")

        override fun assertSameEntityWithoutUpdatedAt(entity1: UserDTO, entity2: UserDTO) {
            entity1 shouldBeEqualTo entity2.copy(updatedAt = entity1.updatedAt)
        }
    }

    @Nested
    inner class AutoIncIdReadWriteThroughRemteCache: AutoIncIdReadWriteThrough() {
        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH.copy(
            codec = CompositeCodec(RedissonCodecs.Long, LZ4Fury, LZ4Fury)
        )

        override val repository: ExposedCacheRepository<UserDTO, Long> by lazy {
            UserCacheRepository(
                redissonClient,
                "read-write-through:remote:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class AutoIncIdReadWriteThroughRemteCacheWithDeleteDB: AutoIncIdReadWriteThrough() {
        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH.copy(
            deleteFromDBOnInvalidate = true,
            codec = CompositeCodec(RedissonCodecs.Long, LZ4Fury, LZ4Fury)
        )

        override val repository: ExposedCacheRepository<UserDTO, Long> by lazy {
            UserCacheRepository(
                redissonClient,
                "read-write-through:remote:delete-db:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class AutoIncIdReadWriteThroughNearCache: AutoIncIdReadWriteThrough() {
        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(
            codec = CompositeCodec(RedissonCodecs.Long, LZ4Fury, LZ4Fury)
        )

        override val repository: ExposedCacheRepository<UserDTO, Long> by lazy {
            UserCacheRepository(
                redissonClient,
                "read-write-through:near:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class AutoIncIdReadWriteThroughNearCacheWithDeleteDB: AutoIncIdReadWriteThrough() {
        override val cacheConfig =
            RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(
                deleteFromDBOnInvalidate = true,
                codec = CompositeCodec(RedissonCodecs.Long, LZ4Fury, LZ4Fury)
            )

        override val repository: ExposedCacheRepository<UserDTO, Long> by lazy {
            UserCacheRepository(
                redissonClient,
                "read-write-through:near:delete-db:users",
                config = cacheConfig
            )
        }
    }


    abstract class ClientGeneratedIdReadWriteThrough: ReadWriteThroughScenario<UserCredential, String>() {

        override fun withEntityTable(
            testDB: TestDB,
            statement: Transaction.() -> Unit,
        ) = withUserCredentialTable(testDB, statement)

        override fun getExistingId(): String = transaction {
            UserCredentialTable.select(UserCredentialTable.id).first()[UserCredentialTable.id].value
        }

        override fun getExistingIds(): List<String> = transaction {
            UserCredentialTable.selectAll().map { it[UserCredentialTable.id].value }
        }

        override fun getNonExistentId(): String = Base58.randomString(4)

        override fun updateEntityEmail(entity: UserCredential): UserCredential =
            entity.copy(email = "updated@example.com")

        override fun assertSameEntityWithoutUpdatedAt(entity1: UserCredential, entity2: UserCredential) {
            entity1 shouldBeEqualTo entity2.copy(updatedAt = entity1.updatedAt)
        }

    }

    @Nested
    inner class ClientGeneratedIdReadThroughRemoteCache: ClientGeneratedIdReadWriteThrough() {

        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH.copy(
            codec = CompositeCodec(RedissonCodecs.String, LZ4Fury, LZ4Fury)
        )

        override val repository: ExposedCacheRepository<UserCredential, String> by lazy {
            UserCredentialCacheRepository(
                redissonClient,
                "read-through:remote:user-credentials",
                config = cacheConfig,
            )
        }
    }

    @Nested
    inner class ClientGeneratedIdReadThroughRemoteCacheWithDeleteDB: ClientGeneratedIdReadWriteThrough() {

        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH.copy(
            deleteFromDBOnInvalidate = true,
            codec = CompositeCodec(RedissonCodecs.String, LZ4Fury, LZ4Fury)
        )

        override val repository: ExposedCacheRepository<UserCredential, String> by lazy {
            UserCredentialCacheRepository(
                redissonClient,
                "read-through:remote:delete-db:user-credentials",
                config = cacheConfig,
            )
        }
    }


    @Nested
    inner class ClientGeneratedIdReadThroughNearCache: ClientGeneratedIdReadWriteThrough() {

        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(
            codec = CompositeCodec(RedissonCodecs.String, LZ4Fury, LZ4Fury)
        )

        override val repository: ExposedCacheRepository<UserCredential, String> by lazy {
            UserCredentialCacheRepository(
                redissonClient,
                "read-through:near:user-credentials",
                config = cacheConfig,
            )
        }
    }

    @Nested
    inner class ClientGeneratedIdReadThroughNearCacheWithDeleteDB: ClientGeneratedIdReadWriteThrough() {

        override val cacheConfig =
            RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(
                deleteFromDBOnInvalidate = true,
                codec = CompositeCodec(RedissonCodecs.String, LZ4Fury, LZ4Fury)
            )

        override val repository: ExposedCacheRepository<UserCredential, String> by lazy {
            UserCredentialCacheRepository(
                redissonClient,
                "read-through:near:delete-db:user-credentials",
                config = cacheConfig,
            )
        }
    }
}
