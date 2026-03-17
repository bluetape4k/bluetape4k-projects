package io.bluetape4k.exposed.lettuce.repository

import io.bluetape4k.exposed.lettuce.domain.UserRepository
import io.bluetape4k.exposed.lettuce.domain.UserSchema
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserTable
import io.bluetape4k.exposed.lettuce.repository.scenarios.ReadThroughScenario
import io.bluetape4k.logging.KLogging
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserReadThroughCacheTest :
    AbstractJdbcLettuceRepositoryTest(),
    ReadThroughScenario<Long, UserRecord> {
    companion object : KLogging()

    override val config = LettuceCacheConfig.READ_ONLY
    override val repository by lazy { UserRepository(redisClient, config) }

    private val testUsers = mutableListOf<UserRecord>()

    @BeforeAll
    fun setupDb() {
        Database.connect(
            url = "jdbc:h2:mem:test-lettuce-rt-scenario;DB_CLOSE_DELAY=-1;MODE=MySQL",
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
        repository.clearCache()
    }

    override fun getExistingId() = testUsers.first().id

    override fun getExistingIds() = testUsers.map { it.id }

    override fun getNonExistentId() = 999_999L

    override fun buildEntityForId(id: Long) = UserSchema.newUserRecord().copy(id = id)
}
