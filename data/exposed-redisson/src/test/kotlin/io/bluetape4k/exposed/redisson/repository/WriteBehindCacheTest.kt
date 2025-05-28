package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialDTO
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserDTO
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withUserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withUserTable
import io.bluetape4k.exposed.redisson.repository.scenarios.WriteBehindScenario
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Nested
import java.util.*

class WriteBehindCacheTest {

    companion object: KLogging()

    abstract class AutoIncIdReadWriteBehind: AbstractRedissonTest(),
                                             WriteBehindScenario<UserDTO, Long> {
        override fun withEntityTable(
            testDB: TestDB,
            statement: JdbcTransaction.() -> Unit,
        ) = withUserTable(testDB, statement)

        override fun getExistingId() = transaction {
            UserTable
                .select(UserTable.id)
                .limit(1)
                .first()[UserTable.id].value
        }

        override fun getExistingIds() = transaction {
            UserTable
                .select(UserTable.id)
                .map { it[UserTable.id].value }
        }

        override fun getNonExistentId() = Long.MIN_VALUE
        override fun createNewEntity() = UserSchema.newUserDTO()
    }

    @Nested
    inner class AutoIncIdReadWriteBehindRemoteCache: AutoIncIdReadWriteBehind() {
        override val cacheConfig = RedisCacheConfig.WRITE_BEHIND

        override val repository by lazy {
            UserCacheRepository(
                redissonClient,
                "write-behind:remote:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class AutoIncIdReadWriteBehindNearCache: AutoIncIdReadWriteBehind() {
        override val cacheConfig = RedisCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE

        override val repository by lazy {
            UserCacheRepository(
                redissonClient,
                "write-behind:near:users",
                config = cacheConfig
            )
        }
    }

    abstract class ClientGeneratedIdReadWriteBehind: AbstractRedissonTest(),
                                                     WriteBehindScenario<UserCredentialDTO, UUID> {
        override fun withEntityTable(
            testDB: TestDB,
            statement: JdbcTransaction.() -> Unit,
        ) = withUserCredentialTable(testDB, statement)

        override fun getExistingId() = transaction {
            UserCredentialTable
                .select(UserCredentialTable.id)
                .limit(1)
                .first()[UserCredentialTable.id].value
        }

        override fun getExistingIds() = transaction {
            UserCredentialTable
                .select(UserCredentialTable.id)
                .map { it[UserCredentialTable.id].value }
        }

        override fun getNonExistentId() = UUID.randomUUID()

        override fun createNewEntity(): UserCredentialDTO = UserSchema.newUserCredentialDTO()
    }

    @Nested
    inner class ClientGeneratedIdReadBehindRemoteCache: ClientGeneratedIdReadWriteBehind() {

        override val cacheConfig = RedisCacheConfig.WRITE_BEHIND

        override val repository by lazy {
            UserCredentialCacheRepository(
                redissonClient,
                "write-behind:remote:user-credentials",
                config = cacheConfig,
            )
        }
    }

    @Nested
    inner class ClientGeneratedIdReadBehindNearCache: ClientGeneratedIdReadWriteBehind() {

        override val cacheConfig = RedisCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE

        override val repository by lazy {
            UserCredentialCacheRepository(
                redissonClient,
                "write-behind:near:user-credentials",
                config = cacheConfig,
            )
        }
    }
}
