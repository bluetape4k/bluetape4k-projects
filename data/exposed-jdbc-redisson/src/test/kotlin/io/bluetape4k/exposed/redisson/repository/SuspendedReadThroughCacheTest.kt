package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.domain.SuspendedUserCacheRepository
import io.bluetape4k.exposed.redisson.domain.SuspendedUserCredentialCacheRepository
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserTable
import io.bluetape4k.exposed.redisson.domain.UserSchema.withSuspendedUserCredentialsTable
import io.bluetape4k.exposed.redisson.domain.UserSchema.withSuspendedUserTable
import io.bluetape4k.exposed.redisson.repository.scenarios.SuspendedReadThroughScenario
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Nested
import java.util.*
import kotlin.coroutines.CoroutineContext

@Suppress("DEPRECATION")
class SuspendedReadThroughCacheTest {
    companion object: KLoggingChannel()

    abstract class SuspendedAutoIncIdReadThrough:
        AbstractRedissonTest(),
        SuspendedReadThroughScenario<Long, UserRecord> {
        companion object: KLoggingChannel()

        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend JdbcTransaction.() -> Unit,
        ) {
            withSuspendedUserTable(testDB, context, statement)
        }

        override suspend fun getExistingId() =
            newSuspendedTransaction {
                UserTable
                    .select(UserTable.id)
                    .first()[UserTable.id]
                    .value
            }

        override suspend fun getExistingIds() =
            newSuspendedTransaction {
                UserTable
                    .select(UserTable.id)
                    .map { it[UserTable.id].value }
            }

        override suspend fun getNonExistentId(): Long = Long.MIN_VALUE
    }

    @Nested
    inner class SuspendedAutoIncIdReadThroughRemteCache: SuspendedAutoIncIdReadThrough() {
        override val cacheConfig: RedissonCacheConfig =
            RedissonCacheConfig.READ_ONLY.copy(name = "suspended:read-through:remote:users")
        override val repository by lazy {
            SuspendedUserCacheRepository(
                redissonClient,
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class SuspendedAutoIncIdReadThroughNearCache: SuspendedAutoIncIdReadThrough() {
        override val cacheConfig: RedissonCacheConfig =
            RedissonCacheConfig.READ_ONLY_WITH_NEAR_CACHE.copy(name = "suspended:read-through:near:users")

        override val repository by lazy {
            SuspendedUserCacheRepository(
                redissonClient,
                config = cacheConfig
            )
        }
    }

    abstract class SuspendedClientGeneratedIdReadThrough:
        AbstractRedissonTest(),
        SuspendedReadThroughScenario<UUID, UserCredentialsRecord> {
        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend JdbcTransaction.() -> Unit,
        ) = withSuspendedUserCredentialsTable(
            testDB,
            context,
            statement
        )

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
    }

    @Nested
    inner class SuspendedClientGeneratedIdReadThroughRemoteCache: SuspendedClientGeneratedIdReadThrough() {
        override val cacheConfig: RedissonCacheConfig =
            RedissonCacheConfig.READ_ONLY.copy(name = "suspended:read-through:remote:user-credentials")
        override val repository by lazy {
            SuspendedUserCredentialCacheRepository(
                redissonClient,
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class SuspendedClientGeneratedIdReadThroughNearCache: SuspendedClientGeneratedIdReadThrough() {
        override val cacheConfig: RedissonCacheConfig =
            RedissonCacheConfig.READ_ONLY_WITH_NEAR_CACHE.copy(name = "suspended:read-through:near:user-credentials")
        override val repository by lazy {
            SuspendedUserCredentialCacheRepository(
                redissonClient,
                config = cacheConfig
            )
        }
    }
}
