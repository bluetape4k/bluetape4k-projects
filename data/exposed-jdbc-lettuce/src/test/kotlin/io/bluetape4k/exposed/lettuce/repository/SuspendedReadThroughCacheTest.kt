package io.bluetape4k.exposed.lettuce.repository

import io.bluetape4k.exposed.lettuce.AbstractJdbcLettuceTest
import io.bluetape4k.exposed.lettuce.domain.SuspendedUserCredentialRepository
import io.bluetape4k.exposed.lettuce.domain.SuspendedUserRepository
import io.bluetape4k.exposed.lettuce.domain.UserSchema
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserTable
import io.bluetape4k.exposed.lettuce.repository.scenarios.SuspendedReadThroughScenario
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import java.util.*

/**
 * exposed-jdbc-lettuce Read-through 캐시 통합 테스트.
 *
 * - AutoIncrement Long ID 테이블 ([UserTable]) 과
 * - Client-generated UUID ID 테이블 ([UserCredentialsTable]) 에 대해 각각 검증한다.
 */
class SuspendedReadThroughCacheTest {
    companion object : KLoggingChannel()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — UserTable
    // -------------------------------------------------------------------------

    abstract class AutoIncIdReadThrough :
        AbstractJdbcLettuceTest(),
        SuspendedReadThroughScenario<Long, UserRecord> {
        companion object : KLoggingChannel()

        override val config = LettuceCacheConfig.READ_ONLY

        protected abstract val dbUrl: String

        private val testUsers = mutableListOf<UserRecord>()

        @BeforeAll
        fun setupDb() {
            Database.connect(url = dbUrl, driver = "org.h2.Driver")
            transaction { SchemaUtils.create(UserTable) }
        }

        @BeforeEach
        fun setupData() {
            val users = mutableListOf<UserRecord>()
            transaction {
                UserTable.deleteAll()
                repeat(3) {
                    val record = UserSchema.newUserRecord()
                    val id =
                        UserTable
                            .insertAndGetId {
                                it[UserTable.firstName] = record.firstName
                                it[UserTable.lastName] = record.lastName
                                it[UserTable.email] = record.email
                            }.value
                    users.add(record.copy(id = id))
                }
            }
            testUsers.clear()
            testUsers.addAll(users)
        }

        @AfterEach
        fun tearDown() {
            runTest { repository.clearCache() }
        }

        override suspend fun getExistingId() = testUsers.first().id

        override suspend fun getExistingIds() = testUsers.map { it.id }

        override suspend fun getNonExistentId() = 999_999L

        override suspend fun buildEntityForId(id: Long) = UserSchema.newUserRecord().copy(id = id)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AutoIncIdReadThroughTest : AutoIncIdReadThrough() {
        override val dbUrl = "jdbc:h2:mem:lettuce-rt-long;DB_CLOSE_DELAY=-1;MODE=MySQL"
        override val repository by lazy { SuspendedUserRepository(redisClient, config) }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AutoIncIdReadThroughNearCacheTest : AutoIncIdReadThrough() {
        override val dbUrl = "jdbc:h2:mem:lettuce-rt-long-near;DB_CLOSE_DELAY=-1;MODE=MySQL"
        override val config =
            LettuceCacheConfig.READ_ONLY_WITH_NEAR_CACHE.copy(
                nearCacheName = "jdbc-lettuce-users-rt-near"
            )
        override val repository by lazy { SuspendedUserRepository(redisClient, config) }
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — UserCredentialsTable
    // -------------------------------------------------------------------------

    abstract class ClientGeneratedIdReadThrough :
        AbstractJdbcLettuceTest(),
        SuspendedReadThroughScenario<UUID, UserCredentialsRecord> {
        companion object : KLoggingChannel()

        override val config = LettuceCacheConfig.READ_ONLY

        protected abstract val dbUrl: String

        private val testCredentials = mutableListOf<UserCredentialsRecord>()

        @BeforeAll
        fun setupDb() {
            Database.connect(url = dbUrl, driver = "org.h2.Driver")
            transaction { SchemaUtils.create(UserCredentialsTable) }
        }

        @BeforeEach
        fun setupData() {
            val creds = mutableListOf<UserCredentialsRecord>()
            transaction {
                UserCredentialsTable.deleteAll()
                repeat(3) {
                    val record = UserSchema.newUserCredentialsRecord()
                    UserCredentialsTable.insertAndGetId {
                        it[UserCredentialsTable.id] = record.id
                        it[UserCredentialsTable.loginId] = record.loginId
                        it[UserCredentialsTable.email] = record.email
                        it[UserCredentialsTable.lastLoginAt] = record.lastLoginAt
                    }
                    creds.add(record)
                }
            }
            testCredentials.clear()
            testCredentials.addAll(creds)
        }

        @AfterEach
        fun tearDown() {
            runTest { repository.clearCache() }
        }

        override suspend fun getExistingId() = testCredentials.first().id

        override suspend fun getExistingIds() = testCredentials.map { it.id }

        override suspend fun getNonExistentId(): UUID = UUID.randomUUID()

        override suspend fun buildEntityForId(id: UUID) = UserSchema.newUserCredentialsRecord().copy(id = id)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ClientGeneratedIdReadThroughTest : ClientGeneratedIdReadThrough() {
        override val dbUrl = "jdbc:h2:mem:lettuce-rt-uuid;DB_CLOSE_DELAY=-1;MODE=MySQL"
        override val repository by lazy { SuspendedUserCredentialRepository(redisClient, config) }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ClientGeneratedIdReadThroughNearCacheTest : ClientGeneratedIdReadThrough() {
        override val dbUrl = "jdbc:h2:mem:lettuce-rt-uuid-near;DB_CLOSE_DELAY=-1;MODE=MySQL"
        override val config =
            LettuceCacheConfig.READ_ONLY_WITH_NEAR_CACHE.copy(
                nearCacheName = "jdbc-lettuce-cred-rt-near"
            )
        override val repository by lazy { SuspendedUserCredentialRepository(redisClient, config) }
    }
}
