package io.bluetape4k.exposed.lettuce.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.lettuce.AbstractJdbcLettuceTest
import io.bluetape4k.exposed.lettuce.domain.UserCredentialRepository
import io.bluetape4k.exposed.lettuce.domain.UserRepository
import io.bluetape4k.exposed.lettuce.domain.UserSchema
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserTable
import io.bluetape4k.exposed.lettuce.domain.UserSchema.withUserCredentialsTable
import io.bluetape4k.exposed.lettuce.domain.UserSchema.withUserTable
import io.bluetape4k.exposed.lettuce.repository.scenarios.WriteThroughScenario
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Nested
import java.util.*

/**
 * exposed-jdbc-lettuce Write-through 캐시 통합 테스트.
 *
 * - AutoIncrement Long ID 테이블 ([UserTable]) 과
 * - Client-generated UUID ID 테이블 ([UserCredentialsTable]) 에 대해 각각 검증한다.
 * - Remote Cache 및 Near Cache 두 가지 설정을 모두 테스트한다.
 */
class WriteThroughCacheTest {
    companion object: KLogging()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — UserTable
    // -------------------------------------------------------------------------

    abstract class AutoIncIdWriteThrough:
        AbstractJdbcLettuceTest(),
        WriteThroughScenario<Long, UserRecord> {

        override fun withEntityTable(testDB: TestDB, statement: JdbcTransaction.() -> Unit) =
            withUserTable(testDB, statement)

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
        override fun updateEmail(entity: UserRecord) =
            entity.copy(email = Base58.randomString(4) + ".wt-updated@example.com")

        override fun createNewEntity(): UserRecord = UserSchema.newUserRecord()
    }

    @Nested
    inner class AutoIncIdWriteThroughRemoteCache: AutoIncIdWriteThrough() {
        override val config = LettuceCacheConfig.READ_WRITE_THROUGH
        override val repository by lazy { UserRepository(redisClient, config) }
    }

    @Nested
    inner class AutoIncIdWriteThroughNearCache: AutoIncIdWriteThrough() {
        override val config = LettuceCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(
            nearCacheName = "jdbc-lettuce-users-wt-near"
        )
        override val repository by lazy { UserRepository(redisClient, config) }
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — UserCredentialsTable
    // -------------------------------------------------------------------------

    abstract class ClientGeneratedIdWriteThrough:
        AbstractJdbcLettuceTest(),
        WriteThroughScenario<UUID, UserCredentialsRecord> {

        override fun withEntityTable(testDB: TestDB, statement: JdbcTransaction.() -> Unit) =
            withUserCredentialsTable(testDB, statement)

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
        override fun updateEmail(entity: UserCredentialsRecord) =
            entity.copy(email = Base58.randomString(4) + ".wt-updated@example.com")

        override fun createNewEntity(): UserCredentialsRecord = UserSchema.newUserCredentialsRecord()
    }

    @Nested
    inner class ClientGeneratedIdWriteThroughRemoteCache: ClientGeneratedIdWriteThrough() {
        override val config = LettuceCacheConfig.READ_WRITE_THROUGH
        override val repository by lazy { UserCredentialRepository(redisClient, config) }
    }

    @Nested
    inner class ClientGeneratedIdWriteThroughNearCache: ClientGeneratedIdWriteThrough() {
        override val config = LettuceCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(
            nearCacheName = "jdbc-lettuce-cred-wt-near"
        )
        override val repository by lazy { UserCredentialRepository(redisClient, config) }
    }
}
