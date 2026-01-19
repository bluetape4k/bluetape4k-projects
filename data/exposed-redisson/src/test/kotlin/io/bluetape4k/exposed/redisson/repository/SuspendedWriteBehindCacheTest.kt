package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialDTO
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserDTO
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withSuspendedUserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withSuspendedUserTable
import io.bluetape4k.exposed.redisson.repository.scenarios.SuspendedWriteBehindScenario
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Nested
import java.util.*
import kotlin.coroutines.CoroutineContext

@Suppress("DEPRECATION")
class SuspendedWriteBehindCacheTest {

    companion object: KLoggingChannel()

    abstract class SuspendedAutoIncIdReadWriteBehind: AbstractRedissonTest(),
                                                      SuspendedWriteBehindScenario<UserDTO, Long> {
        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend JdbcTransaction.() -> Unit,
        ) = withSuspendedUserTable(testDB, context, statement)

        override suspend fun getExistingId() = newSuspendedTransaction {
            UserTable
                .select(UserTable.id)
                .limit(1)
                .first()[UserTable.id].value
        }

        override suspend fun getExistingIds() = newSuspendedTransaction {
            UserTable
                .select(UserTable.id)
                .map { it[UserTable.id].value }
        }

        override suspend fun getNonExistentId() = Long.MIN_VALUE
        override suspend fun createNewEntity() = UserSchema.newUserDTO()
    }

    @Nested
    inner class SuspendedAutoIncIdReadWriteBehindRemoteCache: SuspendedAutoIncIdReadWriteBehind() {
        override val cacheConfig = RedisCacheConfig.WRITE_BEHIND

        override val repository by lazy {
            SuspendedUserCacheRepository(
                redissonClient,
                "suspended:write-behind:remote:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class SuspendedAutoIncIdReadWriteBehindNearCache: SuspendedAutoIncIdReadWriteBehind() {
        override val cacheConfig = RedisCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE

        override val repository by lazy {
            SuspendedUserCacheRepository(
                redissonClient,
                "suspended:write-behind:near:users",
                config = cacheConfig
            )
        }
    }

    abstract class SuspendedClientGeneratedIdReadWriteBehind: AbstractRedissonTest(),
                                                              SuspendedWriteBehindScenario<UserCredentialDTO, UUID> {
        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend JdbcTransaction.() -> Unit,
        ) = withSuspendedUserCredentialTable(testDB, context, statement)

        override suspend fun getExistingId() = newSuspendedTransaction {
            UserCredentialTable
                .select(UserCredentialTable.id)
                .first()[UserCredentialTable.id].value
        }

        override suspend fun getExistingIds() = newSuspendedTransaction {
            UserCredentialTable
                .select(UserCredentialTable.id)
                .map { it[UserCredentialTable.id].value }
        }

        override suspend fun getNonExistentId(): UUID = UUID.randomUUID()

        override suspend fun createNewEntity(): UserCredentialDTO = UserSchema.newUserCredentialDTO()
    }

    @Nested
    inner class SuspendedClientGeneratedIdReadBehindRemoteCache: SuspendedClientGeneratedIdReadWriteBehind() {

        override val cacheConfig = RedisCacheConfig.WRITE_BEHIND

        override val repository by lazy {
            SuspendedUserCredentialCacheRepository(
                redissonClient,
                "suspended:write-behind:remote:user-credentials",
                config = cacheConfig,
            )
        }
    }

    @Nested
    inner class SuspendedClientGeneratedIdReadBehindNearCache: SuspendedClientGeneratedIdReadWriteBehind() {

        override val cacheConfig = RedisCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE

        override val repository by lazy {
            SuspendedUserCredentialCacheRepository(
                redissonClient,
                "suspended:write-behind:near:user-credentials",
                config = cacheConfig,
            )
        }
    }
}
