package io.bluetape4k.exposed.r2dbc.redisson.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.r2dbc.redisson.R2dbcRedissonTestBase
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserCredentialDTO
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserCredentialTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserDTO
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.withUserCredentialTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.withUserTable
import io.bluetape4k.exposed.r2dbc.redisson.scenario.R2dbcReadThroughScenario
import io.bluetape4k.exposed.r2dbc.redisson.scenario.R2dbcWriteThroughScenario
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
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
                                                   R2dbcReadThroughScenario<UserDTO, Long>,
                                                   R2dbcWriteThroughScenario<UserDTO, Long> {
        override suspend fun withR2dbcEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend R2dbcTransaction.() -> Unit,
        ) {
            withUserTable(testDB, context, statement)
        }

        override suspend fun getExistingId(): Long = suspendTransaction {
            UserTable.select(UserTable.id)
                .limit(1)
                .first()[UserTable.id].value
        }

        override suspend fun getExistingIds(): List<Long> = suspendTransaction {
            UserTable
                .select(UserTable.id)
                .map { it[UserTable.id].value }
                .toList()
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
    inner class R2dbcAutoIncIdReadWriteThroughRemoteCache: R2dbcAutoIncIdReadWriteThrough() {
        override val cacheConfig = RedisCacheConfig.READ_WRITE_THROUGH

        override val repository: R2dbcCacheRepository<UserDTO, Long> by lazy {
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

        override val repository: R2dbcCacheRepository<UserDTO, Long> by lazy {
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

        override val repository: R2dbcCacheRepository<UserDTO, Long> by lazy {
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

        override val repository: R2dbcCacheRepository<UserDTO, Long> by lazy {
            R2dbcUserCacheRepository(
                redissonClient,
                "r2dbc:read-write-through:near:delete-db:users",
                config = cacheConfig
            )
        }
    }


    abstract class R2dbcClientGeneratedIdReadWriteThrough: R2dbcRedissonTestBase(),
                                                           R2dbcReadThroughScenario<UserCredentialDTO, UUID>,
                                                           R2dbcWriteThroughScenario<UserCredentialDTO, UUID> {
        override suspend fun withR2dbcEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend R2dbcTransaction.() -> Unit,
        ) = withUserCredentialTable(testDB, context, statement)

        override suspend fun getExistingId(): UUID = suspendTransaction {
            UserCredentialTable
                .select(UserCredentialTable.id)
                .limit(1)
                .first()[UserCredentialTable.id].value
        }

        override suspend fun getExistingIds(): List<UUID> = suspendTransaction {
            UserCredentialTable
                .select(UserCredentialTable.id)
                .map { it[UserCredentialTable.id].value }
                .toList()
        }

        override suspend fun getNonExistentId(): UUID = UUID.randomUUID()

        override suspend fun createNewEntity(): UserCredentialDTO = UserSchema.newUserCredentialDTO()

        override suspend fun updateEntityEmail(entity: UserCredentialDTO): UserCredentialDTO =
            entity.copy(email = "updated.${Base58.randomString(8)}@example.com")

        override suspend fun assertSameEntityWithoutAudit(entity1: UserCredentialDTO, entity2: UserCredentialDTO) {
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
