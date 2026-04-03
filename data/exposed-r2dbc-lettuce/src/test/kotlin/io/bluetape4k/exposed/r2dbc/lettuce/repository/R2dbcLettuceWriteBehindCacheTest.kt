package io.bluetape4k.exposed.r2dbc.lettuce.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.r2dbc.lettuce.AbstractR2dbcLettuceTest
import io.bluetape4k.exposed.r2dbc.lettuce.domain.R2dbcUserCredentialLettuceRepository
import io.bluetape4k.exposed.r2dbc.lettuce.domain.R2dbcUserLettuceRepository
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.UserTable
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.withUserCredentialsTable
import io.bluetape4k.exposed.r2dbc.lettuce.domain.UserSchema.withUserTable
import io.bluetape4k.exposed.r2dbc.lettuce.repository.scenarios.R2dbcLettuceWriteBehindScenario
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.bluetape4k.redis.lettuce.map.WriteMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Nested
import java.time.Duration
import java.util.*

/**
 * R2DBC Lettuce Write-behind 캐시 통합 테스트.
 *
 * - AutoIncrement Long ID 테이블 ([UserTable]) 과
 * - Client-generated UUID ID 테이블 ([UserCredentialsTable]) 에 대해 각각 검증한다.
 * - `writeBehindDelay = 500ms`로 설정하여 테스트 시 빠른 flush를 유도한다.
 */
class R2dbcLettuceWriteBehindCacheTest {
    companion object: KLoggingChannel()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — UserTable
    // -------------------------------------------------------------------------

    abstract class AutoIncIdWriteBehind:
        AbstractR2dbcLettuceTest(),
        R2dbcLettuceWriteBehindScenario<Long, UserRecord> {
        companion object: KLoggingChannel()

        override val config =
            LettuceCacheConfig(
                writeMode = WriteMode.WRITE_BEHIND,
                writeBehindDelay = Duration.ofMillis(500),
                writeBehindBatchSize = 10,
                keyPrefix = "r2dbc:write-behind:auto-inc:test"
            )

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

        override suspend fun updateEmail(entity: UserRecord): UserRecord =
            entity.copy(email = Base58.randomString(4) + "." + faker.internet().emailAddress())
    }

    @Nested
    inner class AutoIncIdWriteBehindTest: AutoIncIdWriteBehind() {
        override val repository by lazy { R2dbcUserLettuceRepository(redisClient, config) }
    }

    @Nested
    inner class AutoIncIdWriteBehindNearCacheTest: AutoIncIdWriteBehind() {
        override val config =
            LettuceCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE.copy(
                writeBehindDelay = Duration.ofMillis(500),
                writeBehindBatchSize = 10,
                keyPrefix = "r2dbc:write-behind:auto-inc:near:test",
                nearCacheName = "r2dbc-lettuce-users-behind-near"
            )
        override val repository by lazy { R2dbcUserLettuceRepository(redisClient, config) }
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — UserCredentialsTable
    // -------------------------------------------------------------------------

    abstract class ClientGeneratedIdWriteBehind:
        AbstractR2dbcLettuceTest(),
        R2dbcLettuceWriteBehindScenario<UUID, UserCredentialsRecord> {
        companion object: KLoggingChannel()

        override val config =
            LettuceCacheConfig(
                writeMode = WriteMode.WRITE_BEHIND,
                writeBehindDelay = Duration.ofMillis(500),
                writeBehindBatchSize = 10,
                keyPrefix = "r2dbc:write-behind:client-uuid:test"
            )

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

        override suspend fun updateEmail(entity: UserCredentialsRecord): UserCredentialsRecord =
            entity.copy(email = Base58.randomString(4) + "." + faker.internet().emailAddress())
    }

    @Nested
    inner class ClientGeneratedIdWriteBehindTest: ClientGeneratedIdWriteBehind() {
        override val repository by lazy { R2dbcUserCredentialLettuceRepository(redisClient, config) }
    }

    @Nested
    inner class ClientGeneratedIdWriteBehindNearCacheTest: ClientGeneratedIdWriteBehind() {
        override val config =
            LettuceCacheConfig.WRITE_BEHIND_WITH_NEAR_CACHE.copy(
                writeBehindDelay = Duration.ofMillis(500),
                writeBehindBatchSize = 10,
                keyPrefix = "r2dbc:write-behind:client-uuid:near:test",
                nearCacheName = "r2dbc-lettuce-cred-behind-near"
            )
        override val repository by lazy { R2dbcUserCredentialLettuceRepository(redisClient, config) }
    }
}
