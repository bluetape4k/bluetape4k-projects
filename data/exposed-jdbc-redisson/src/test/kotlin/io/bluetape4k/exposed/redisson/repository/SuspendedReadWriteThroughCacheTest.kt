package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.domain.SuspendedUserCacheRepository
import io.bluetape4k.exposed.redisson.domain.SuspendedUserCredentialCacheRepository
import io.bluetape4k.exposed.redisson.domain.UserSchema
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserTable
import io.bluetape4k.exposed.redisson.domain.UserSchema.withSuspendedUserCredentialsTable
import io.bluetape4k.exposed.redisson.domain.UserSchema.withSuspendedUserTable
import io.bluetape4k.exposed.redisson.repository.scenarios.SuspendedReadThroughScenario
import io.bluetape4k.exposed.redisson.repository.scenarios.SuspendedWriteThroughScenario
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Nested
import java.util.*
import kotlin.coroutines.CoroutineContext

@Suppress("DEPRECATION")
class SuspendedReadWriteThroughCacheTest {
    companion object : KLoggingChannel()

    abstract class SuspendedAutoIncIdReadWriteThrough :
        AbstractRedissonTest(),
        SuspendedReadThroughScenario<Long, UserRecord>,
        SuspendedWriteThroughScenario<Long, UserRecord> {
        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend JdbcTransaction.() -> Unit,
        ) {
            withSuspendedUserTable(testDB, context, statement)
        }

        override suspend fun getExistingId(): Long =
            newSuspendedTransaction {
                UserTable
                    .select(UserTable.id)
                    .limit(1)
                    .first()[UserTable.id]
                    .value
            }

        override suspend fun getExistingIds(): List<Long> =
            newSuspendedTransaction {
                UserTable
                    .select(UserTable.id)
                    .map { it[UserTable.id].value }
            }

        override suspend fun getNonExistentId(): Long = Long.MIN_VALUE

        override suspend fun createNewEntity(): UserRecord = UserSchema.newUserRecord()

        override suspend fun updateEntityEmail(entity: UserRecord): UserRecord =
            entity.copy(email = "updated-${Base58.randomString(8)}@example.com")

        override suspend fun assertSameEntityWithoutAudit(
            entity1: UserRecord,
            entity2: UserRecord,
        ) {
            entity1 shouldBeEqualTo entity2.copy(createdAt = entity1.createdAt, updatedAt = entity1.updatedAt)
        }
    }

    @Nested
    inner class SuspendedAutoIncIdReadWriteThroughRemoteCache : SuspendedAutoIncIdReadWriteThrough() {
        override val cacheConfig = RedissonCacheConfig.READ_WRITE_THROUGH.copy(name = "suspended:read-write-through:remote:users")

        override val repository by lazy {
            SuspendedUserCacheRepository(
                redissonClient,
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class SuspendedAutoIncIdReadWriteThroughRemoteCacheWithDeleteDB : SuspendedAutoIncIdReadWriteThrough() {
        override val cacheConfig = RedissonCacheConfig.READ_WRITE_THROUGH.copy(name = "suspended:read-write-through:remote:delete-db:users", deleteFromDBOnInvalidate = true)

        override val repository by lazy {
            SuspendedUserCacheRepository(
                redissonClient,
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class SuspendedAutoIncIdReadWriteThroughNearCache : SuspendedAutoIncIdReadWriteThrough() {
        override val cacheConfig = RedissonCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(name = "suspended:read-write-through:near:users")

        override val repository by lazy {
            SuspendedUserCacheRepository(
                redissonClient,
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class SuspendedAutoIncIdReadWriteThroughNearCacheWithDeleteDB : SuspendedAutoIncIdReadWriteThrough() {
        override val cacheConfig =
            RedissonCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(name = "suspended:read-write-through:near:delete-db:users", deleteFromDBOnInvalidate = true)

        override val repository by lazy {
            SuspendedUserCacheRepository(
                redissonClient,
                config = cacheConfig
            )
        }
    }

    abstract class SuspendedClientGeneratedIdReadWriteThrough :
        AbstractRedissonTest(),
        SuspendedReadThroughScenario<UUID, UserCredentialsRecord>,
        SuspendedWriteThroughScenario<UUID, UserCredentialsRecord> {
        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend JdbcTransaction.() -> Unit,
        ) {
            withSuspendedUserCredentialsTable(testDB, context, statement)
        }

        override suspend fun getExistingId() =
            newSuspendedTransaction {
                UserCredentialsTable
                    .select(UserCredentialsTable.id)
                    .first()[UserCredentialsTable.id]
                    .value
            }

        override suspend fun getExistingIds() =
            newSuspendedTransaction {
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
    inner class SuspendedClientGeneratedIdReadThroughRemoteCache : SuspendedClientGeneratedIdReadWriteThrough() {
        override val cacheConfig = RedissonCacheConfig.READ_WRITE_THROUGH.copy(name = "suspended:read-through:remote:user-credentials")

        override val repository by lazy {
            SuspendedUserCredentialCacheRepository(
                redissonClient,
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class SuspendedClientGeneratedIdReadThroughRemoteCacheWithDeleteDB :
        SuspendedClientGeneratedIdReadWriteThrough() {
        override val cacheConfig = RedissonCacheConfig.READ_WRITE_THROUGH.copy(name = "suspended:read-through:remote:delete-db:user-credentials", deleteFromDBOnInvalidate = true)

        override val repository by lazy {
            SuspendedUserCredentialCacheRepository(
                redissonClient,
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class SuspendedClientGeneratedIdReadThroughNearCache : SuspendedClientGeneratedIdReadWriteThrough() {
        override val cacheConfig = RedissonCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(name = "suspended:read-through:near:user-credentials")

        override val repository by lazy {
            SuspendedUserCredentialCacheRepository(
                redissonClient,
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class SuspendedClientGeneratedIdReadThroughNearCacheWithDeleteDB :
        SuspendedClientGeneratedIdReadWriteThrough() {
        override val cacheConfig =
            RedissonCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(name = "suspended:read-through:near:delete-db:user-credentials", deleteFromDBOnInvalidate = true)

        override val repository by lazy {
            SuspendedUserCredentialCacheRepository(
                redissonClient,
                config = cacheConfig
            )
        }
    }
}
