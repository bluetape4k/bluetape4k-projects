package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withSuspendedUserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withSuspendedUserTable
import io.bluetape4k.exposed.redisson.repository.scenarios.SuspendedReadThroughScenario
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Nested
import java.util.*
import kotlin.coroutines.CoroutineContext

class SuspendedReadThroughCacheTest {

    abstract class SuspendedAutoIncIdReadThrough: AbstractRedissonTest(),
                                                  SuspendedReadThroughScenario<UserSchema.UserDTO, Long> {

        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend Transaction.() -> Unit,
        ) {
            withSuspendedUserTable(testDB, context, statement)
        }

        override suspend fun getExistingId() = newSuspendedTransaction {
            UserSchema.UserTable.select(UserSchema.UserTable.id).first()[UserSchema.UserTable.id].value
        }

        override suspend fun getExistingIds() = newSuspendedTransaction {
            UserSchema.UserTable.selectAll().map { it[UserSchema.UserTable.id].value }
        }

        override suspend fun getNonExistentId(): Long = Long.MIN_VALUE
    }

    @Nested
    inner class SuspendedAutoIncIdReadThroughRemteCache: SuspendedAutoIncIdReadThrough() {
        override val cacheConfig: RedisCacheConfig = RedisCacheConfig.READ_ONLY
        override val repository: SuspendedExposedCacheRepository<UserSchema.UserDTO, Long> by lazy {
            SuspendedUserCacheRepository(
                redissonClient,
                "suspended:read-through:remote:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class SuspendedAutoIncIdReadThroughNearCache: SuspendedAutoIncIdReadThrough() {
        override val cacheConfig: RedisCacheConfig = RedisCacheConfig.READ_ONLY_WITH_NEAR_CACHE

        override val repository: SuspendedExposedCacheRepository<UserSchema.UserDTO, Long> by lazy {
            SuspendedUserCacheRepository(
                redissonClient,
                "suspended:read-through:near:users",
                config = cacheConfig
            )
        }
    }

    abstract class SuspendedClientGeneratedIdReadThrough: AbstractRedissonTest(),
                                                          SuspendedReadThroughScenario<UserSchema.UserCredentialDTO, UUID> {

        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend Transaction.() -> Unit,
        ) = withSuspendedUserCredentialTable(testDB, context, statement)

        override suspend fun getExistingId() = newSuspendedTransaction {
            UserCredentialTable.select(UserCredentialTable.id).first()[UserCredentialTable.id].value
        }

        override suspend fun getExistingIds() = newSuspendedTransaction {
            UserCredentialTable.selectAll().map { it[UserCredentialTable.id].value }
        }

        override suspend fun getNonExistentId() = UUID.randomUUID()
    }

    @Nested
    inner class SuspendedClientGeneratedIdReadThroughRemoteCache: SuspendedClientGeneratedIdReadThrough() {
        override val cacheConfig: RedisCacheConfig = RedisCacheConfig.READ_ONLY
        override val repository by lazy {
            SuspendedUserCredentialCacheRepository(
                redissonClient,
                "suspended:read-through:remote:user-credentials",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class SuspendedClientGeneratedIdReadThroughNearCache: SuspendedClientGeneratedIdReadThrough() {
        override val cacheConfig: RedisCacheConfig = RedisCacheConfig.READ_ONLY_WITH_NEAR_CACHE
        override val repository by lazy {
            SuspendedUserCredentialCacheRepository(
                redissonClient,
                "suspended:read-through:near:user-credentials",
                config = cacheConfig
            )
        }
    }
}
