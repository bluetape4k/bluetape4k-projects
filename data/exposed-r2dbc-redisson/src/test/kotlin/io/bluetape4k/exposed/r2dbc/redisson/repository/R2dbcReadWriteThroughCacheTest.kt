package io.bluetape4k.exposed.r2dbc.redisson.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.coroutines.flow.extensions.toFastList
import io.bluetape4k.exposed.r2dbc.redisson.R2dbcRedissonTestBase
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.withUserCredentialsTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.withUserTable
import io.bluetape4k.exposed.r2dbc.redisson.scenario.R2dbcReadThroughScenario
import io.bluetape4k.exposed.r2dbc.redisson.scenario.R2dbcWriteThroughScenario
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Nested
import java.util.*
import kotlin.coroutines.CoroutineContext

class R2dbcReadWriteThroughCacheTest {

    companion object: KLoggingChannel()

    abstract class R2dbcAutoIncIdReadWriteThrough: R2dbcRedissonTestBase(),
                                                   R2dbcReadThroughScenario<UserRecord, Long>,
                                                   R2dbcWriteThroughScenario<UserRecord, Long> {
        override suspend fun withR2dbcEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend R2dbcTransaction.() -> Unit,
        ) {
            withUserTable(testDB, context, statement)
        }

        override suspend fun getExistingId() = suspendTransaction {
            UserTable.select(UserTable.id)
                .limit(1)
                .first()[UserTable.id].value
        }

        override suspend fun getExistingIds() = suspendTransaction {
            UserTable
                .select(UserTable.id)
                .map { it[UserTable.id].value }
                .toFastList()
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
    inner class R2dbcAutoIncIdReadWriteThroughRemoteCache: R2dbcAutoIncIdReadWriteThrough() {
        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH

        override val repository: R2dbcCacheRepository<UserRecord, Long> by lazy {
            R2dbcUserCacheRepository(
                redissonClient,
                "r2dbc:read-write-through:remote:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class R2dbcAutoIncIdReadWriteThroughRemoteCacheWithDeleteDB: R2dbcAutoIncIdReadWriteThrough() {
        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH.copy(deleteFromDBOnInvalidate = true)

        override val repository: R2dbcCacheRepository<UserRecord, Long> by lazy {
            R2dbcUserCacheRepository(
                redissonClient,
                "r2dbc:read-write-through:remote:delete-db:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class R2dbcAutoIncIdReadWriteThroughNearCache: R2dbcAutoIncIdReadWriteThrough() {
        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE

        override val repository: R2dbcCacheRepository<UserRecord, Long> by lazy {
            R2dbcUserCacheRepository(
                redissonClient,
                "r2dbc:read-write-through:near:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class R2dbcAutoIncIdReadWriteThroughNearCacheWithDeleteDB: R2dbcAutoIncIdReadWriteThrough() {
        override val cacheConfig =
            RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(deleteFromDBOnInvalidate = true)

        override val repository: R2dbcCacheRepository<UserRecord, Long> by lazy {
            R2dbcUserCacheRepository(
                redissonClient,
                "r2dbc:read-write-through:near:delete-db:users",
                config = cacheConfig
            )
        }
    }


    abstract class R2dbcClientGeneratedIdReadWriteThrough: R2dbcRedissonTestBase(),
                                                           R2dbcReadThroughScenario<UserCredentialsRecord, UUID>,
                                                           R2dbcWriteThroughScenario<UserCredentialsRecord, UUID> {
        override suspend fun withR2dbcEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend R2dbcTransaction.() -> Unit,
        ) = withUserCredentialsTable(testDB, context, statement)

        override suspend fun getExistingId() = suspendTransaction {
            UserCredentialsTable
                .select(UserCredentialsTable.id)
                .limit(1)
                .first()[UserCredentialsTable.id].value
        }

        override suspend fun getExistingIds() = suspendTransaction {
            UserCredentialsTable
                .select(UserCredentialsTable.id)
                .map { it[UserCredentialsTable.id].value }
                .toFastList()
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
    inner class R2dbcClientGeneratedIdReadThroughRemoteCache: R2dbcClientGeneratedIdReadWriteThrough() {

        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH

        override val repository by lazy {
            R2dbcUserCredentialCacheRepository(
                redissonClient,
                "r2dbc:read-through:remote:user-credentials",
                config = cacheConfig,
            )
        }
    }

    @Nested
    inner class R2dbcClientGeneratedIdReadThroughRemoteCacheWithDeleteDB:
        R2dbcClientGeneratedIdReadWriteThrough() {

        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH.copy(deleteFromDBOnInvalidate = true)

        override val repository by lazy {
            R2dbcUserCredentialCacheRepository(
                redissonClient,
                "r2dbc:read-through:remote:delete-db:user-credentials",
                config = cacheConfig,
            )
        }
    }


    @Nested
    inner class R2dbcClientGeneratedIdReadThroughNearCache: R2dbcClientGeneratedIdReadWriteThrough() {

        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE

        override val repository by lazy {
            R2dbcUserCredentialCacheRepository(
                redissonClient,
                "r2dbc:read-through:near:user-credentials",
                config = cacheConfig,
            )
        }
    }

    @Nested
    inner class R2dbcClientGeneratedIdReadThroughNearCacheWithDeleteDB:
        R2dbcClientGeneratedIdReadWriteThrough() {

        override val cacheConfig =
            RedisCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(deleteFromDBOnInvalidate = true)

        override val repository by lazy {
            R2dbcUserCredentialCacheRepository(
                redissonClient,
                "r2dbc:read-through:near:delete-db:user-credentials",
                config = cacheConfig,
            )
        }
    }
}
