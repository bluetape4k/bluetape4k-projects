package io.bluetape4k.exposed.r2dbc.redisson.repository

import io.bluetape4k.coroutines.flow.extensions.toFastList
import io.bluetape4k.exposed.r2dbc.redisson.R2dbcRedissonTestBase
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserCredentialDTO
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserCredentialTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserDTO
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.withUserCredentialTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.withUserTable
import io.bluetape4k.exposed.r2dbc.redisson.scenario.R2dbcWriteBehindScenario
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Nested
import java.util.*
import kotlin.coroutines.CoroutineContext

class R2dbcWriteBehindCacheTest {

    companion object: KLoggingChannel()

    abstract class R2dbcAutoIncIdReadWriteBehind: R2dbcRedissonTestBase(),
                                                  R2dbcWriteBehindScenario<UserDTO, Long> {
        override suspend fun withR2dbcEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend R2dbcTransaction.() -> Unit,
        ) = withUserTable(testDB, context, statement)

        override suspend fun getExistingId() = suspendTransaction {
            UserTable
                .select(UserTable.id)
                .limit(1)
                .first()[UserTable.id].value
        }

        override suspend fun getExistingIds() = suspendTransaction {
            UserTable
                .selectAll()
                .map { it[UserTable.id].value }
                .toFastList()
        }

        override suspend fun getNonExistentId(): Long = Long.MIN_VALUE
        override suspend fun createNewEntity(): UserDTO = UserSchema.newUserDTO()
    }

    @Nested
    inner class R2dbcAutoIncIdReadWriteBehindRemoteCache: R2dbcAutoIncIdReadWriteBehind() {
        override val cacheConfig = RedisCacheConfig.WRITE_BEHIND

        override val repository by lazy {
            R2dbcUserCacheRepository(
                redissonClient,
                "r2dbc:write-behind:remote:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class R2dbcAutoIncIdReadWriteBehindNearCache: R2dbcAutoIncIdReadWriteBehind() {
        override val cacheConfig = RedisCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE

        override val repository by lazy {
            R2dbcUserCacheRepository(
                redissonClient,
                "r2dbc:write-behind:near:users",
                config = cacheConfig
            )
        }
    }

    abstract class R2dbcClientGeneratedIdReadWriteBehind: R2dbcRedissonTestBase(),
                                                          R2dbcWriteBehindScenario<UserCredentialDTO, UUID> {
        override suspend fun withR2dbcEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend R2dbcTransaction.() -> Unit,
        ) = withUserCredentialTable(testDB, context, statement)

        override suspend fun getExistingId() = suspendTransaction {
            UserCredentialTable
                .select(UserCredentialTable.id)
                .first()[UserCredentialTable.id].value
        }

        override suspend fun getExistingIds() = suspendTransaction {
            UserCredentialTable
                .select(UserCredentialTable.id)
                .map { it[UserCredentialTable.id].value }
                .toFastList()
        }

        override suspend fun getNonExistentId(): UUID = UUID.randomUUID()

        override suspend fun createNewEntity(): UserCredentialDTO = UserSchema.newUserCredentialDTO()
    }

    @Nested
    inner class R2dbcClientGeneratedIdReadBehindRemoteCache: R2dbcClientGeneratedIdReadWriteBehind() {

        override val cacheConfig = RedisCacheConfig.WRITE_BEHIND

        override val repository by lazy {
            R2dbcUserCredentialCacheRepository(
                redissonClient,
                "r2dbc:write-behind:remote:user-credentials",
                config = cacheConfig,
            )
        }
    }

    @Nested
    inner class R2dbcClientGeneratedIdReadBehindNearCache: R2dbcClientGeneratedIdReadWriteBehind() {

        override val cacheConfig = RedisCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE

        override val repository by lazy {
            R2dbcUserCredentialCacheRepository(
                redissonClient,
                "r2dbc:write-behind:near:user-credentials",
                config = cacheConfig,
            )
        }
    }
}
