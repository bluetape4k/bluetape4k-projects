package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.domain.UserCacheRepository
import io.bluetape4k.exposed.redisson.domain.UserCredentialCacheRepository
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.redisson.domain.UserSchema.UserTable
import io.bluetape4k.exposed.redisson.domain.UserSchema.withUserCredentialsTable
import io.bluetape4k.exposed.redisson.domain.UserSchema.withUserTable
import io.bluetape4k.exposed.redisson.repository.scenarios.ReadThroughScenario
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.cache.RedissonCacheConfig
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Nested
import java.util.*

class ReadThroughCacheTest {
    companion object: KLogging()

    abstract class AutoIncIdReadThrough: AbstractRedissonTest(),
                                         ReadThroughScenario<Long, UserRecord> {
        override fun withEntityTable(
            testDB: TestDB,
            statement: JdbcTransaction.() -> Unit,
        ) = withUserTable(testDB, statement)

        override fun getExistingId() =
            transaction {
                UserTable
                    .select(UserTable.id)
                    .limit(1)
                    .first()[UserTable.id]
                    .value
            }

        override fun getExistingIds() =
            transaction {
                UserTable
                    .select(UserTable.id)
                    .map { it[UserTable.id].value }
            }

        override fun getNonExistentId() = Long.MIN_VALUE
    }

    @Nested
    inner class AutoIncIdReadThroughRemoteCache: AutoIncIdReadThrough() {
        override val cacheConfig: RedissonCacheConfig = RedissonCacheConfig.READ_ONLY
        override val repository by lazy {
            UserCacheRepository(
                redissonClient,
                "read-through:remote:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class AutoIncIdReadThroughNearCache: AutoIncIdReadThrough() {
        override val cacheConfig: RedissonCacheConfig = RedissonCacheConfig.READ_ONLY_WITH_NEAR_CACHE
        override val repository by lazy {
            UserCacheRepository(
                redissonClient,
                "read-through:near:users",
                config = cacheConfig
            )
        }
    }

    abstract class ClientGeneratedIdReadThrough:
        AbstractRedissonTest(),
        ReadThroughScenario<UUID, UserCredentialsRecord> {
        override fun withEntityTable(
            testDB: TestDB,
            statement: JdbcTransaction.() -> Unit,
        ) = withUserCredentialsTable(testDB, statement)

        override fun getExistingId() =
            transaction {
                UserCredentialsTable
                    .select(UserCredentialsTable.id)
                    .limit(1)
                    .first()[UserCredentialsTable.id]
                    .value
            }

        override fun getExistingIds() =
            transaction {
                UserCredentialsTable
                    .select(UserCredentialsTable.id)
                    .map { it[UserCredentialsTable.id].value }
            }

        override fun getNonExistentId(): UUID = UUID.randomUUID()
    }

    @Nested
    inner class ClientGeneratedIdReadThroughRemoteCache: ClientGeneratedIdReadThrough() {
        override val cacheConfig: RedissonCacheConfig = RedissonCacheConfig.READ_ONLY
        override val repository by lazy {
            UserCredentialCacheRepository(
                redissonClient,
                "read-through:remote:user-credentials",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class ClientGeneratedIdReadThroughNearCache: ClientGeneratedIdReadThrough() {
        override val cacheConfig: RedissonCacheConfig = RedissonCacheConfig.READ_ONLY_WITH_NEAR_CACHE
        override val repository by lazy {
            UserCredentialCacheRepository(
                redissonClient,
                "read-through:near:user-credentials",
                config = cacheConfig
            )
        }
    }
}
