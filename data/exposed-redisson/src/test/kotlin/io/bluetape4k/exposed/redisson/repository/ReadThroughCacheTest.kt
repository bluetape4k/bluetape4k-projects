package io.bluetape4k.exposed.redisson.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.redisson.AbstractRedissonTest
import io.bluetape4k.exposed.redisson.repository.UserSchema.UserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withUserCredentialTable
import io.bluetape4k.exposed.redisson.repository.UserSchema.withUserTable
import io.bluetape4k.exposed.redisson.repository.scenarios.ReadThroughScenario
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.redis.redisson.cache.RedisCacheConfig
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.selectAll
import org.junit.jupiter.api.Nested

class ReadThroughCacheTest {

    abstract class AutoIncIdReadThrough: AbstractRedissonTest(),
                                         ReadThroughScenario<UserSchema.UserDTO, Long> {

        override fun withEntityTable(
            testDB: TestDB,
            statement: Transaction.() -> Unit,
        ) = withUserTable(testDB, statement)

        override fun getExistingId(): Long =
            UserSchema.UserTable.select(UserSchema.UserTable.id).first()[UserSchema.UserTable.id].value

        override fun getExistingIds(): List<Long> =
            UserSchema.UserTable.selectAll().map { it[UserSchema.UserTable.id].value }

        override fun getNonExistentId(): Long = Long.MIN_VALUE

    }

    @Nested
    inner class AutoIncIdReadThroughRemteCache: AutoIncIdReadThrough() {
        override val cacheConfig: RedisCacheConfig = RedisCacheConfig.READ_ONLY
        override val repository: ExposedCacheRepository<UserSchema.UserDTO, Long> by lazy {
            UserCacheRepository(
                redissonClient,
                "read-through:remote:users",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class AutoIncIdReadThroughNearCache: AutoIncIdReadThrough() {
        override val cacheConfig: RedisCacheConfig = RedisCacheConfig.READ_ONLY_WITH_NEAR_CACHE

        override val repository: ExposedCacheRepository<UserSchema.UserDTO, Long> by lazy {
            UserCacheRepository(
                redissonClient,
                "read-through:near:users",
                config = cacheConfig
            )
        }
    }

    abstract class ClientGeneratedIdReadThrough: AbstractRedissonTest(),
                                                 ReadThroughScenario<UserSchema.UserCredentialDTO, String> {

        override fun withEntityTable(
            testDB: TestDB,
            statement: Transaction.() -> Unit,
        ) = withUserCredentialTable(testDB, statement)

        override fun getExistingId(): String =
            UserCredentialTable.select(UserCredentialTable.id).first()[UserCredentialTable.id].value

        override fun getExistingIds(): List<String> =
            UserCredentialTable.selectAll().map { it[UserCredentialTable.id].value }

        override fun getNonExistentId(): String = Base58.randomString(4)
    }

    @Nested
    inner class ClientGeneratedIdReadThroughRemoteCache: ClientGeneratedIdReadThrough() {
        override val cacheConfig: RedisCacheConfig = RedisCacheConfig.READ_ONLY
        override val repository: ExposedCacheRepository<UserSchema.UserCredentialDTO, String> by lazy {
            UserCredentialCacheRepository(
                redissonClient,
                "read-through:remote:user-credentials",
                config = cacheConfig
            )
        }
    }

    @Nested
    inner class ClientGeneratedIdReadThroughNearCache: ClientGeneratedIdReadThrough() {
        override val cacheConfig: RedisCacheConfig = RedisCacheConfig.READ_ONLY_WITH_NEAR_CACHE
        override val repository: ExposedCacheRepository<UserSchema.UserCredentialDTO, String> by lazy {
            UserCredentialCacheRepository(
                redissonClient,
                "read-through:near:user-credentials",
                config = cacheConfig
            )
        }
    }
}
