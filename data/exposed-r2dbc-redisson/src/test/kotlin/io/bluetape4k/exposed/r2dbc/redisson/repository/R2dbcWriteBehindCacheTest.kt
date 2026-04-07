package io.bluetape4k.exposed.r2dbc.redisson.repository

import io.bluetape4k.exposed.r2dbc.redisson.AbstractR2dbcRedissonTest
import io.bluetape4k.exposed.r2dbc.redisson.domain.R2dbcUserCredentialRedissonRepository
import io.bluetape4k.exposed.r2dbc.redisson.domain.R2dbcUserRedissonRepository
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.withUserCredentialsTable
import io.bluetape4k.exposed.r2dbc.redisson.domain.UserSchema.withUserTable
import io.bluetape4k.exposed.r2dbc.redisson.repository.scenario.R2dbcWriteBehindScenario
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Nested
import java.util.*
import kotlin.coroutines.CoroutineContext

class R2dbcWriteBehindCacheTest {
    companion object : KLoggingChannel()

    abstract class R2dbcAutoIncIdReadWriteBehind :
        AbstractR2dbcRedissonTest(),
        R2dbcWriteBehindScenario<Long, UserRecord> {
        override suspend fun withR2dbcEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend R2dbcTransaction.() -> Unit,
        ) = withUserTable(testDB, context, statement)

        override suspend fun getExistingId() =
            suspendTransaction {
                UserTable
                    .select(UserTable.id)
                    .limit(1)
                    .first()[UserTable.id]
                    .value
            }

        override suspend fun getExistingIds() =
            suspendTransaction {
                UserTable
                    .selectAll()
                    .map { it[UserTable.id].value }
                    .toList()
            }

        override suspend fun getNonExistentId(): Long = Long.MIN_VALUE

        override suspend fun createNewEntity(): UserRecord = UserSchema.newUserRecord()
    }

    @Nested
    inner class R2dbcAutoIncIdReadWriteBehindRemoteCache : R2dbcAutoIncIdReadWriteBehind() {
        override val cacheConfig = RedissonCacheConfig.WRITE_BEHIND

        override val repository by lazy {
            R2dbcUserRedissonRepository(
                redissonClient,
                config = cacheConfig.copy(name = "r2dbc:write-behind:remote:users")
            )
        }
    }

    @Nested
    inner class R2dbcAutoIncIdReadWriteBehindNearCache : R2dbcAutoIncIdReadWriteBehind() {
        override val cacheConfig = RedissonCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE

        override val repository by lazy {
            R2dbcUserRedissonRepository(
                redissonClient,
                config = cacheConfig.copy(name = "r2dbc:write-behind:near:users")
            )
        }
    }

    abstract class R2dbcClientGeneratedIdReadWriteBehind :
        AbstractR2dbcRedissonTest(),
        R2dbcWriteBehindScenario<UUID, UserCredentialsRecord> {
        override suspend fun withR2dbcEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend R2dbcTransaction.() -> Unit,
        ) = withUserCredentialsTable(testDB, context, statement)

        override suspend fun getExistingId() =
            suspendTransaction {
                UserCredentialsTable
                    .select(UserCredentialsTable.id)
                    .first()[UserCredentialsTable.id]
                    .value
            }

        override suspend fun getExistingIds() =
            suspendTransaction {
                UserCredentialsTable
                    .select(UserCredentialsTable.id)
                    .map { it[UserCredentialsTable.id].value }
                    .toList()
            }

        override suspend fun getNonExistentId(): UUID = UUID.randomUUID()

        override suspend fun createNewEntity(): UserCredentialsRecord = UserSchema.newUserCredentialsRecord()
    }

    @Nested
    inner class R2dbcClientGeneratedIdReadBehindRemoteCache : R2dbcClientGeneratedIdReadWriteBehind() {
        override val cacheConfig = RedissonCacheConfig.WRITE_BEHIND

        override val repository by lazy {
            R2dbcUserCredentialRedissonRepository(
                redissonClient,
                config = cacheConfig.copy(name = "r2dbc:write-behind:remote:user-credentials")
            )
        }
    }

    @Nested
    inner class R2dbcClientGeneratedIdReadBehindNearCache : R2dbcClientGeneratedIdReadWriteBehind() {
        override val cacheConfig = RedissonCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE

        override val repository by lazy {
            R2dbcUserCredentialRedissonRepository(
                redissonClient,
                config = cacheConfig.copy(name = "r2dbc:write-behind:near:user-credentials")
            )
        }
    }
}
