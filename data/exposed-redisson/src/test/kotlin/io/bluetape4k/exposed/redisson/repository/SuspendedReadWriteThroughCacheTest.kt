package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserRecord
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withSuspendedUserCredentialsTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withSuspendedUserTable
import io.bluetape4k.exposed.redisson.repository.scenarios.SuspendedReadThroughScenario
import io.bluetape4k.exposed.redisson.repository.scenarios.SuspendedWriteThroughScenario
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Nested
import java.util.*
import kotlin.coroutines.CoroutineContext

@Suppress("DEPRECATION")
class SuspendedReadWriteThroughCacheTest {

    companion object: KLoggingChannel()

    abstract class SuspendedAutoIncIdReadWriteThrough: AbstractRedissonTest(),
                                                       SuspendedReadThroughScenario<UserRecord, Long>,
                                                       SuspendedWriteThroughScenario<UserRecord, Long> {
        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend JdbcTransaction.() -> Unit,
        ) {
            withSuspendedUserTable(testDB, context, statement)
        }

        override suspend fun getExistingId(): Long = newSuspendedTransaction {
            UserTable
                .select(UserTable.id)
                .limit(1)
                .first()[UserTable.id].value
        }

        override suspend fun getExistingIds(): List<Long> = newSuspendedTransaction {
            UserTable
                .select(UserTable.id)
                .map { it[UserTable.id].value }
        }

        override suspend fun getNonExistentId(): Long = Long.MIN_VALUE

        override suspend fun createNewEntity(): UserRecord = UserSchema.newUserRecord()

        override suspend fun updateEntityEmail(entity: UserRecord): UserRecord =
            entity.copy(email = "updated-${Base58.randomString(8)}@example.com")

        override suspend fun assertSameEntityWithoutAudit(entity1: UserRecord, entity2: UserRecord) {
            entity1 shouldBeEqualTo entity2.copy(createdAt = entity1.createdAt, updatedAt = entity1.updatedAt)
        }
    }

    @Nested
    inner class SuspendedAutoIncIdReadWriteThroughRemoteCache: SuspendedAutoIncIdReadWriteThrough() {
        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH

        override val repository: SuspendedExposedCacheRepository<UserRecord, Long> by lazy {
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

        override val repository: SuspendedExposedCacheRepository<UserRecord, Long> by lazy {
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

        override val repository: SuspendedExposedCacheRepository<UserRecord, Long> by lazy {
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

        override val repository: SuspendedExposedCacheRepository<UserRecord, Long> by lazy {
            SuspendedUserCacheRepository(
                redissonClient,
                "suspended:read-write-through:near:delete-db:users",
                config = cacheConfig
            )
        }
    }


    abstract class SuspendedClientGeneratedIdReadWriteThrough: AbstractRedissonTest(),
                                                               SuspendedReadThroughScenario<UserCredentialsRecord, UUID>,
                                                               SuspendedWriteThroughScenario<UserCredentialsRecord, UUID> {
        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend JdbcTransaction.() -> Unit,
        ) {
            withSuspendedUserCredentialsTable(testDB, context, statement)
        }

        override suspend fun getExistingId() = newSuspendedTransaction {
            UserCredentialsTable
                .select(UserCredentialsTable.id)
                .first()[UserCredentialsTable.id].value
        }

        override suspend fun getExistingIds() = newSuspendedTransaction {
            UserCredentialsTable
                .select(UserCredentialsTable.id)
                .map { it[UserCredentialsTable.id].value }
        }

        override suspend fun getNonExistentId(): UUID = UUID.randomUUID()

        override suspend fun createNewEntity(): UserCredentialsRecord = UserSchema.newUserCredentialsRecord()

        override suspend fun updateEntityEmail(entity: UserCredentialsRecord): UserCredentialsRecord =
            entity.copy(email = "updated.${Base58.randomString(8)}@example.com")

        override suspend fun assertSameEntityWithoutAudit(
            entity1: UserCredentialsRecord,
            entity2: UserCredentialsRecord,
        ) {
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
