package io.bluetape4k.exposed.r2dbc.lettuce.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.r2dbc.lettuce.AbstractR2dbcLettuceTest
import io.bluetape4k.exposed.r2dbc.lettuce.domain.R2dbcUserCredentialLettuceRepository
import io.bluetape4k.exposed.r2dbc.lettuce.domain.R2dbcUserLettuceRepository
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.withUserCredentialsTable
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.withUserTable
import io.bluetape4k.exposed.r2dbc.lettuce.repository.scenarios.R2dbcLettuceReadThroughScenario
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Nested
import java.util.*

/**
 * R2DBC Lettuce Read-through 캐시 통합 테스트.
 *
 * - AutoIncrement Long ID 테이블 ([UserTable]) 과
 * - Client-generated UUID ID 테이블 ([UserCredentialsTable]) 에 대해 각각 검증한다.
 */
class R2dbcLettuceReadThroughCacheTest {
    companion object: KLoggingChannel()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — UserTable
    // -------------------------------------------------------------------------

    abstract class AutoIncIdReadThrough:
        AbstractR2dbcLettuceTest(),
        R2dbcLettuceReadThroughScenario<Long, UserRecord> {
        companion object: KLoggingChannel()

        override val config = LettuceCacheConfig.READ_ONLY

        override suspend fun withR2dbcEntityTable(
            testDB: TestDB,
            statement: suspend R2dbcTransaction.() -> Unit,
        ) = withUserTable(testDB, statement)

        override suspend fun getExistingId(): Long =
            suspendTransaction {
                UserTable.select(UserTable.id).first()[UserTable.id].value
            }

        override suspend fun getExistingIds(): List<Long> =
            suspendTransaction {
                UserTable.select(UserTable.id).map { it[UserTable.id].value }.toList()
            }

        override fun getNonExistentId(): Long = Long.MIN_VALUE

        override suspend fun buildEntityForId(id: Long): UserRecord =
            UserSchema.newUserRecord().copy(
                id = id,
                email = Base58.randomString(4) + "." + faker.internet().emailAddress()
            )
    }

    @Nested
    inner class AutoIncIdReadThroughTest: AutoIncIdReadThrough() {
        override val repository by lazy { R2dbcUserLettuceRepository(redisClient, config) }
    }

    @Nested
    inner class AutoIncIdReadThroughNearCacheTest: AutoIncIdReadThrough() {
        override val config =
            LettuceCacheConfig.READ_ONLY_WITH_NEAR_CACHE.copy(
                nearCacheName = "r2dbc-lettuce-users-read-near"
            )
        override val repository by lazy { R2dbcUserLettuceRepository(redisClient, config) }
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — UserCredentialsTable
    // -------------------------------------------------------------------------

    abstract class ClientGeneratedIdReadThrough:
        AbstractR2dbcLettuceTest(),
        R2dbcLettuceReadThroughScenario<UUID, UserCredentialsRecord> {
        companion object: KLoggingChannel()

        override val config = LettuceCacheConfig.READ_ONLY

        override suspend fun withR2dbcEntityTable(
            testDB: TestDB,
            statement: suspend R2dbcTransaction.() -> Unit,
        ) = withUserCredentialsTable(testDB, statement)

        override suspend fun getExistingId(): UUID =
            suspendTransaction {
                UserCredentialsTable
                    .select(UserCredentialsTable.id)
                    .first()[UserCredentialsTable.id]
                    .value
            }

        override suspend fun getExistingIds(): List<UUID> =
            suspendTransaction {
                UserCredentialsTable
                    .select(UserCredentialsTable.id)
                    .map { it[UserCredentialsTable.id].value }
                    .toList()
            }

        override fun getNonExistentId(): UUID = UUID.randomUUID()

        override suspend fun buildEntityForId(id: UUID): UserCredentialsRecord =
            UserSchema.newUserCredentialsRecord().copy(
                id = id,
                email = Base58.randomString(4) + "." + faker.internet().emailAddress()
            )
    }

    @Nested
    inner class ClientGeneratedIdReadThroughTest: ClientGeneratedIdReadThrough() {
        override val repository by lazy { R2dbcUserCredentialLettuceRepository(redisClient, config) }
    }

    @Nested
    inner class ClientGeneratedIdReadThroughNearCacheTest: ClientGeneratedIdReadThrough() {
        override val config =
            LettuceCacheConfig.READ_ONLY_WITH_NEAR_CACHE.copy(
                nearCacheName = "r2dbc-lettuce-cred-read-near"
            )
        override val repository by lazy { R2dbcUserCredentialLettuceRepository(redisClient, config) }
    }
}
