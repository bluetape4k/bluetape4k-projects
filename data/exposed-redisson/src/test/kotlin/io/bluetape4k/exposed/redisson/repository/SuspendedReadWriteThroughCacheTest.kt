package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialDTO
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserDTO
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withSuspendedUserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withSuspendedUserTable
import io.bluetape4k.exposed.redisson.repository.scenarios.SuspendedReadThroughScenario
import io.bluetape4k.exposed.redisson.repository.scenarios.SuspendedWriteThroughScenario
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Nested
import java.util.*
import kotlin.coroutines.CoroutineContext

class SuspendedReadWriteThroughCacheTest {

    companion object: KLoggingChannel()

    abstract class SuspendedAutoIncIdReadWriteThrough: AbstractRedissonTest(),
                                                       SuspendedReadThroughScenario<UserDTO, Long>,
                                                       SuspendedWriteThroughScenario<UserDTO, Long> {
        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend JdbcTransaction.() -> Unit,
        ) {
            withSuspendedUserTable(testDB, context, statement)
        }

        override suspend fun getExistingId(): Long = newSuspendedTransaction {
            UserTable.select(UserTable.id).limit(1).first()[UserTable.id].value
        }

        override suspend fun getExistingIds(): List<Long> = newSuspendedTransaction {
            UserTable.selectAll().map { it[UserTable.id].value }
        }

        override suspend fun getNonExistentId(): Long = Long.MIN_VALUE

        override suspend fun createNewEntity(): UserDTO = UserSchema.newUserDTO()

        override suspend fun updateEntityEmail(entity: UserDTO): UserDTO =
            entity.copy(email = "updated-${Base58.randomString(8)}@example.com")

        override suspend fun assertSameEntityWithoutAudit(entity1: UserDTO, entity2: UserDTO) {
            entity1 shouldBeEqualTo entity2.copy(createdAt = entity1.createdAt, updatedAt = entity1.updatedAt)
        }
    }

    @Nested
    inner class SuspendedAutoIncIdReadWriteThroughRemoteCache: SuspendedAutoIncIdReadWriteThrough() {
        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH

        override val repository: SuspendedExposedCacheRepository<UserDTO, Long> by lazy {
            SuspendedUserCacheRepository(
                redissonClient,
                "suspended:read-write-through:remote:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class SuspendedAutoIncIdReadWriteThroughRemoteCacheWithDeleteDB: SuspendedAutoIncIdReadWriteThrough() {
        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH.copy(deleteFromDBOnInvalidate = true)

        override val repository: SuspendedExposedCacheRepository<UserDTO, Long> by lazy {
            SuspendedUserCacheRepository(
                redissonClient,
                "suspended:read-write-through:remote:delete-db:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class SuspendedAutoIncIdReadWriteThroughNearCache: SuspendedAutoIncIdReadWriteThrough() {
        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE

        override val repository: SuspendedExposedCacheRepository<UserDTO, Long> by lazy {
            SuspendedUserCacheRepository(
                redissonClient,
                "suspended:read-write-through:near:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class SuspendedAutoIncIdReadWriteThroughNearCacheWithDeleteDB: SuspendedAutoIncIdReadWriteThrough() {
        override val cacheConfig =
            RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(deleteFromDBOnInvalidate = true)

        override val repository: SuspendedExposedCacheRepository<UserDTO, Long> by lazy {
            SuspendedUserCacheRepository(
                redissonClient,
                "suspended:read-write-through:near:delete-db:users",
                config = cacheConfig
            )
        }
    }


    abstract class SuspendedClientGeneratedIdReadWriteThrough: AbstractRedissonTest(),
                                                               SuspendedReadThroughScenario<UserCredentialDTO, UUID>,
                                                               SuspendedWriteThroughScenario<UserCredentialDTO, UUID> {
        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend JdbcTransaction.() -> Unit,
        ) {
            withSuspendedUserCredentialTable(testDB, context, statement)
        }

        override suspend fun getExistingId() = transaction {
            UserCredentialTable.select(UserCredentialTable.id).first()[UserCredentialTable.id].value
        }

        override suspend fun getExistingIds() = transaction {
            UserCredentialTable.selectAll().map { it[UserCredentialTable.id].value }
        }

        override suspend fun getNonExistentId() = UUID.randomUUID()

        override suspend fun createNewEntity(): UserCredentialDTO = UserSchema.newUserCredentialDTO()

        override suspend fun updateEntityEmail(entity: UserCredentialDTO): UserCredentialDTO =
            entity.copy(email = "updated.${Base58.randomString(8)}@example.com")

        override suspend fun assertSameEntityWithoutAudit(entity1: UserCredentialDTO, entity2: UserCredentialDTO) {
            entity1 shouldBeEqualTo entity2.copy(createdAt = entity1.createdAt, updatedAt = entity1.updatedAt)
        }
    }

    @Nested
    inner class SuspendedClientGeneratedIdReadThroughRemoteCache: SuspendedClientGeneratedIdReadWriteThrough() {

        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH

        override val repository by lazy {
            SuspendedUserCredentialCacheRepository(
                redissonClient,
                "suspended:read-through:remote:user-credentials",
                config = cacheConfig,
            )
        }
    }

    @Nested
    inner class SuspendedClientGeneratedIdReadThroughRemoteCacheWithDeleteDB:
        SuspendedClientGeneratedIdReadWriteThrough() {

        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH.copy(deleteFromDBOnInvalidate = true)

        override val repository by lazy {
            SuspendedUserCredentialCacheRepository(
                redissonClient,
                "suspended:read-through:remote:delete-db:user-credentials",
                config = cacheConfig,
            )
        }
    }


    @Nested
    inner class SuspendedClientGeneratedIdReadThroughNearCache: SuspendedClientGeneratedIdReadWriteThrough() {

        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE

        override val repository by lazy {
            SuspendedUserCredentialCacheRepository(
                redissonClient,
                "suspended:read-through:near:user-credentials",
                config = cacheConfig,
            )
        }
    }

    @Nested
    inner class SuspendedClientGeneratedIdReadThroughNearCacheWithDeleteDB:
        SuspendedClientGeneratedIdReadWriteThrough() {

        override val cacheConfig =
            RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(deleteFromDBOnInvalidate = true)

        override val repository by lazy {
            SuspendedUserCredentialCacheRepository(
                redissonClient,
                "suspended:read-through:near:delete-db:user-credentials",
                config = cacheConfig,
            )
        }
    }
}
