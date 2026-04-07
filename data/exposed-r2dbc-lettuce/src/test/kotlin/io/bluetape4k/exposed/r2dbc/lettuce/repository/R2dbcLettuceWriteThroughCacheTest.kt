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
import io.bluetape4k.exposed.r2dbc.lettuce.repository.scenarios.R2dbcLettuceWriteThroughScenario
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
 * R2DBC Lettuce Write-through 캐시 통합 테스트.
 *
 * - AutoIncrement Long ID 테이블 ([UserTable]) 과
 * - Client-generated UUID ID 테이블 ([UserCredentialsTable]) 에 대해 각각 검증한다.
 * - Remote 캐시와 NearCache 변형을 포함한 4-variant @Nested 구조.
 */
class R2dbcLettuceWriteThroughCacheTest {
    companion object: KLoggingChannel()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — UserTable
    // -------------------------------------------------------------------------

    /**
     * Auto-increment Long ID ([UserTable]) 기반 Read+Write-through 테스트 추상 클래스.
     */
    abstract class R2dbcAutoIncIdReadWriteThrough:
        AbstractR2dbcLettuceTest(),
        R2dbcLettuceReadThroughScenario<Long, UserRecord>,
        R2dbcLettuceWriteThroughScenario<Long, UserRecord> {
        companion object: KLoggingChannel()

        override val config = LettuceCacheConfig.READ_WRITE_THROUGH

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

        override suspend fun updateEmail(entity: UserRecord): UserRecord =
            entity.copy(email = Base58.randomString(4) + "." + faker.internet().emailAddress())
    }

    /**
     * Auto-increment Long ID — Remote 캐시 (NearCache 미사용).
     */
    @Nested
    inner class R2dbcAutoIncIdReadWriteThroughRemoteCache: R2dbcAutoIncIdReadWriteThrough() {
        override val config =
            LettuceCacheConfig.READ_WRITE_THROUGH.copy(keyPrefix = "r2dbc:write-through:remote:users")
        override val repository by lazy { R2dbcUserLettuceRepository(redisClient, config) }
    }

    /**
     * Auto-increment Long ID — NearCache 활성화.
     */
    @Nested
    inner class R2dbcAutoIncIdReadWriteThroughNearCache: R2dbcAutoIncIdReadWriteThrough() {
        override val config =
            LettuceCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(
                nearCacheName = "r2dbc-lettuce-users-write-near"
            )
        override val repository by lazy { R2dbcUserLettuceRepository(redisClient, config) }
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — UserCredentialsTable
    // -------------------------------------------------------------------------

    /**
     * Client-generated UUID ID ([UserCredentialsTable]) 기반 Read+Write-through 테스트 추상 클래스.
     */
    abstract class R2dbcClientGeneratedIdReadWriteThrough:
        AbstractR2dbcLettuceTest(),
        R2dbcLettuceReadThroughScenario<UUID, UserCredentialsRecord>,
        R2dbcLettuceWriteThroughScenario<UUID, UserCredentialsRecord> {
        companion object: KLoggingChannel()

        override val config = LettuceCacheConfig.READ_WRITE_THROUGH

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

        override suspend fun updateEmail(entity: UserCredentialsRecord): UserCredentialsRecord =
            entity.copy(email = Base58.randomString(4) + "." + faker.internet().emailAddress())
    }

    /**
     * Client-generated UUID ID — Remote 캐시 (NearCache 미사용).
     */
    @Nested
    inner class R2dbcClientGeneratedIdReadWriteThroughRemoteCache: R2dbcClientGeneratedIdReadWriteThrough() {
        override val config =
            LettuceCacheConfig.READ_WRITE_THROUGH.copy(keyPrefix = "r2dbc:write-through:remote:user-credentials")
        override val repository by lazy { R2dbcUserCredentialLettuceRepository(redisClient, config) }
    }

    /**
     * Client-generated UUID ID — NearCache 활성화.
     */
    @Nested
    inner class R2dbcClientGeneratedIdReadWriteThroughNearCache: R2dbcClientGeneratedIdReadWriteThrough() {
        override val config =
            LettuceCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(
                nearCacheName = "r2dbc-lettuce-cred-write-near"
            )
        override val repository by lazy { R2dbcUserCredentialLettuceRepository(redisClient, config) }
    }
}
