package io.bluetape4k.exposed.lettuce.repository

import io.bluetape4k.exposed.lettuce.AbstractJdbcLettuceTest
import io.bluetape4k.exposed.lettuce.domain.SuspendedUserRepository
import io.bluetape4k.exposed.lettuce.domain.UserSchema
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserTable
import io.bluetape4k.exposed.lettuce.repository.scenarios.SuspendedWriteThroughScenario
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SuspendedUserWriteThroughCacheTest :
    AbstractJdbcLettuceTest(),
    SuspendedWriteThroughScenario<Long, UserRecord> {
    companion object : KLogging()

    override val config = LettuceCacheConfig.READ_WRITE_THROUGH
    override val repository by lazy { SuspendedUserRepository(redisClient, config) }

    private val testUsers = mutableListOf<UserRecord>()

    @BeforeAll
    fun setupDb() {
        Database.connect(
            url = "jdbc:h2:mem:test-lettuce-swt-scenario;DB_CLOSE_DELAY=-1;MODE=MySQL",
            driver = "org.h2.Driver"
        )
        transaction { SchemaUtils.create(UserTable) }
    }

    @BeforeEach
    fun setupData() {
        transaction { UserTable.deleteAll() }
        testUsers.clear()
        testUsers.addAll((1..3).map { repository.createInDb(UserSchema.newUserRecord()) })
    }

    @AfterEach
    fun tearDown() {
        runTest { repository.clearCache() }
    }

    override suspend fun getExistingId() = testUsers.first().id

    override suspend fun getExistingIds() = testUsers.map { it.id }

    override suspend fun getNonExistentId() = 999_999L

    override suspend fun updateEmail(entity: UserRecord) = entity.copy(email = "swt-updated-${entity.id}@example.com")
}
