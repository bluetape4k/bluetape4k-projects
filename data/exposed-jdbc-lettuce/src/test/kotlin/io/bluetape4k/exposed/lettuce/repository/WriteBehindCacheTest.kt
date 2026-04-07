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
import io.bluetape4k.exposed.lettuce.repository.scenarios.WriteBehindScenario
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.bluetape4k.redis.lettuce.map.WriteMode
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Nested
import java.time.Duration
import java.util.*

/**
 * exposed-jdbc-lettuce Write-behind 캐시 통합 테스트.
 *
 * - AutoIncrement Long ID 테이블 ([UserTable]) 과
 * - Client-generated UUID ID 테이블 ([UserCredentialsTable]) 에 대해 각각 검증한다.
 * - Remote Cache 및 Near Cache 두 가지 설정을 모두 테스트한다.
 * - `writeBehindDelay = 300ms`로 설정하여 테스트 시 빠른 flush를 유도한다.
 */
class WriteBehindCacheTest {
    companion object : KLogging()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — UserTable
    // -------------------------------------------------------------------------

    abstract class AutoIncIdWriteBehind :
        AbstractJdbcLettuceTest(),
        WriteBehindScenario<Long, UserRecord> {

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
            entity.copy(email = Base58.randomString(4) + ".wb-updated@example.com")

        override fun createNewEntity(): UserRecord = UserSchema.newUserRecord()
    }

    @Nested
    inner class AutoIncIdWriteBehindRemoteCache : AutoIncIdWriteBehind() {
        override val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            writeBehindDelay = Duration.ofMillis(300),
            writeBehindBatchSize = 50
        )
        override val repository by lazy { UserRepository(redisClient, config) }
    }

    @Nested
    inner class AutoIncIdWriteBehindNearCache : AutoIncIdWriteBehind() {
        override val config = LettuceCacheConfig(
            writeMode = WriteMode.WRITE_BEHIND,
            writeBehindDelay = Duration.ofMillis(300),
            writeBehindBatchSize = 50,
            keyPrefix = "jdbc-wb-auto-inc-near",
            nearCacheEnabled = true,
            nearCacheName = "jdbc-lettuce-users-wb-near"
        )
        override val repository by lazy { UserRepository(redisClient, config) }
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — UserCredentialsTable
    // -------------------------------------------------------------------------

    abstract class ClientGeneratedIdWriteBehind :
        AbstractJdbcLettuceTest(),
        WriteBehindScenario<UUID, UserCredentialsRecord> {

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
            entity.copy(email = Base58.randomString(4) + ".wb-updated@example.com")

        override fun createNewEntity(): UserCredentialsRecord = UserSchema.newUserCredentialsRecord()
    }

    @Nested
    inner class ClientGeneratedIdWriteBehindRemoteCache : ClientGeneratedIdWriteBehind() {
        override val config = LettuceCacheConfig.WRITE_BEHIND.copy(
            writeBehindDelay = Duration.ofMillis(300),
            writeBehindBatchSize = 50
        )
        override val repository by lazy { UserCredentialRepository(redisClient, config) }
    }

    @Nested
    inner class ClientGeneratedIdWriteBehindNearCache : ClientGeneratedIdWriteBehind() {
        override val config = LettuceCacheConfig(
            writeMode = WriteMode.WRITE_BEHIND,
            writeBehindDelay = Duration.ofMillis(300),
            writeBehindBatchSize = 50,
            keyPrefix = "jdbc-wb-client-uuid-near",
            nearCacheEnabled = true,
            nearCacheName = "jdbc-lettuce-cred-wb-near2"
        )
        override val repository by lazy { UserCredentialRepository(redisClient, config) }
    }
}
