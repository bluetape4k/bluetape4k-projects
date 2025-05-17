package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialDTO
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserDTO
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withUserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withUserTable
import io.bluetape4k.exposed.redisson.repository.scenarios.ReadThroughScenario
import io.bluetape4k.exposed.redisson.repository.scenarios.WriteThroughScenario
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Nested
import java.util.*

class ReadWriteThroughCacheTest {

    companion object: KLogging()

    abstract class AutoIncIdReadWriteThrough: AbstractRedissonTest(),
                                              ReadThroughScenario<UserDTO, Long>,
                                              WriteThroughScenario<UserDTO, Long> {
        override fun withEntityTable(
            testDB: TestDB,
            statement: Transaction.() -> Unit,
        ) = withUserTable(testDB, statement)

        override fun getExistingId() = transaction {
            UserTable.select(UserTable.id).limit(1).first()[UserTable.id].value
        }

        override fun getExistingIds() = transaction {
            UserTable.selectAll().map { it[UserTable.id].value }
        }

        override fun getNonExistentId() = Long.MIN_VALUE

        override fun createNewEntity() = UserSchema.newUserDTO()

        override fun updateEntityEmail(entity: UserDTO) =
            entity.copy(email = "updated-${Base58.randomString(8)}@example.com")

        override fun assertSameEntityWithoutUpdatedAt(entity1: UserDTO, entity2: UserDTO) {
            entity1 shouldBeEqualTo entity2.copy(updatedAt = entity1.updatedAt)
        }

    }

    @Nested
    inner class AutoIncIdReadWriteThroughRemoteCache: AutoIncIdReadWriteThrough() {
        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH

        override val repository by lazy {
            UserCacheRepository(
                redissonClient,
                "read-write-through:remote:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class AutoIncIdReadWriteThroughRemoteCacheWithDeleteDB: AutoIncIdReadWriteThrough() {
        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH.copy(deleteFromDBOnInvalidate = true)

        override val repository by lazy {
            UserCacheRepository(
                redissonClient,
                "read-write-through:remote:delete-db:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class AutoIncIdReadWriteThroughNearCache: AutoIncIdReadWriteThrough() {
        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE

        override val repository by lazy {
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
            RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(deleteFromDBOnInvalidate = true)

        override val repository by lazy {
            UserCacheRepository(
                redissonClient,
                "read-write-through:near:delete-db:users",
                config = cacheConfig
            )
        }
    }


    abstract class ClientGeneratedIdReadWriteThrough: AbstractRedissonTest(),
                                                      ReadThroughScenario<UserCredentialDTO, UUID>,
                                                      WriteThroughScenario<UserCredentialDTO, UUID> {

        override fun withEntityTable(
            testDB: TestDB,
            statement: Transaction.() -> Unit,
        ) = withUserCredentialTable(testDB, statement)

        override fun getExistingId() = transaction {
            UserCredentialTable.select(UserCredentialTable.id).first()[UserCredentialTable.id].value
        }

        override fun getExistingIds() = transaction {
            UserCredentialTable.selectAll().map { it[UserCredentialTable.id].value }
        }

        override fun getNonExistentId() = UUID.randomUUID()

        override fun createNewEntity(): UserCredentialDTO = UserSchema.newUserCredentialDTO()

        override fun updateEntityEmail(entity: UserCredentialDTO): UserCredentialDTO =
            entity.copy(email = "updated.${Base58.randomString(8)}@example.com")

        override fun assertSameEntityWithoutUpdatedAt(entity1: UserCredentialDTO, entity2: UserCredentialDTO) {
            entity1 shouldBeEqualTo entity2.copy(updatedAt = entity1.updatedAt)
        }
    }

    @Nested
    inner class ClientGeneratedIdReadThroughRemoteCache: ClientGeneratedIdReadWriteThrough() {

        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH

        override val repository by lazy {
            UserCredentialCacheRepository(
                redissonClient,
                "read-through:remote:user-credentials",
                config = cacheConfig,
            )
        }
    }

    @Nested
    inner class ClientGeneratedIdReadThroughRemoteCacheWithDeleteDB: ClientGeneratedIdReadWriteThrough() {

        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH.copy(deleteFromDBOnInvalidate = true)

        override val repository by lazy {
            UserCredentialCacheRepository(
                redissonClient,
                "read-through:remote:delete-db:user-credentials",
                config = cacheConfig,
            )
        }
    }


    @Nested
    inner class ClientGeneratedIdReadThroughNearCache: ClientGeneratedIdReadWriteThrough() {

        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE

        override val repository by lazy {
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
            RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(deleteFromDBOnInvalidate = true)

        override val repository by lazy {
            UserCredentialCacheRepository(
                redissonClient,
                "read-through:near:delete-db:user-credentials",
                config = cacheConfig,
            )
        }
    }
}
