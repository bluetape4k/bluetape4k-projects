package io.bluetape4k.exposed.r2dbc.redisson.repository

import io.bluetape4k.exposed.r2dbc.redisson.R2dbcRedissonTestBase
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserCredentialDTO
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserCredentialTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserDTO
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.withUserCredentialTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.withUserTable
import io.bluetape4k.exposed.r2dbc.redisson.scenario.R2dbcReadThroughScenario
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Nested
import java.util.*
import kotlin.coroutines.CoroutineContext

class R2dbcReadThroughCacheTest {

    companion object: KLoggingChannel()

    abstract class R2dbcAutoIncIdReadThrough: R2dbcRedissonTestBase(),
                                              R2dbcReadThroughScenario<UserDTO, Long> {

        companion object: KLoggingChannel()

        override suspend fun withR2dbcEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend R2dbcTransaction.() -> Unit,
        ) = withUserTable(testDB, context = context, statement = statement)

        override suspend fun getExistingId() = suspendTransaction {
            UserTable.select(UserTable.id).first()[UserTable.id].value
        }

        override suspend fun getExistingIds() = suspendTransaction {
            UserTable.select(UserTable.id)
                .map { it[UserTable.id].value }
                .toList()
        }

        override suspend fun getNonExistentId(): Long = Long.MIN_VALUE
    }

    @Nested
    inner class R2dbcAutoIncIdReadThroughRemteCache: R2dbcAutoIncIdReadThrough() {
        override val cacheConfig: RedisCacheConfig = RedisCacheConfig.READ_ONLY
        override val repository: R2dbcCacheRepository<UserDTO, Long> by lazy {
            R2dbcUserCacheRepository(
                redissonClient,
                "r2dbc:read-through:remote:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class R2dbcAutoIncIdReadThroughNearCache: R2dbcAutoIncIdReadThrough() {
        override val cacheConfig: RedisCacheConfig = RedisCacheConfig.READ_ONLY_WITH_NEAR_CACHE

        override val repository: R2dbcCacheRepository<UserDTO, Long> by lazy {
            R2dbcUserCacheRepository(
                redissonClient,
                "r2dbc:read-through:near:users",
                config = cacheConfig
            )
        }
    }

    abstract class R2dbcClientGeneratedIdReadThrough: R2dbcRedissonTestBase(),
                                                      R2dbcReadThroughScenario<UserCredentialDTO, UUID> {

        override suspend fun withR2dbcEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend R2dbcTransaction.() -> Unit,
        ) = withUserCredentialTable(testDB, context, statement)

        override suspend fun getExistingId() = suspendTransaction {
            UserCredentialTable.select(UserCredentialTable.id).first()[UserCredentialTable.id].value
        }

        override suspend fun getExistingIds() = suspendTransaction {
            UserCredentialTable
                .select(UserCredentialTable.id)
                .map { it[UserCredentialTable.id].value }
                .toList()
        }

        override suspend fun getNonExistentId() = UUID.randomUUID()
    }

    @Nested
    inner class R2dbcClientGeneratedIdReadThroughRemoteCache: R2dbcClientGeneratedIdReadThrough() {
        override val cacheConfig: RedisCacheConfig = RedisCacheConfig.READ_ONLY
        override val repository by lazy {
            R2dbcUserCredentialCacheRepository(
                redissonClient,
                "r2dbc:read-through:remote:user-credentials",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class R2dbcClientGeneratedIdReadThroughNearCache: R2dbcClientGeneratedIdReadThrough() {
        override val cacheConfig: RedisCacheConfig = RedisCacheConfig.READ_ONLY_WITH_NEAR_CACHE
        override val repository by lazy {
            R2dbcUserCredentialCacheRepository(
                redissonClient,
                "r2dbc:read-through:near:user-credentials",
                config = cacheConfig
            )
        }
    }
}
