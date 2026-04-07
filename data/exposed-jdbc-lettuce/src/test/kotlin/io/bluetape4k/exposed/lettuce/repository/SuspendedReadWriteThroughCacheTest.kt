package io.bluetape4k.exposed.lettuce.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.lettuce.AbstractJdbcLettuceTest
import io.bluetape4k.exposed.lettuce.domain.SuspendedUserCredentialRepository
import io.bluetape4k.exposed.lettuce.domain.SuspendedUserRepository
import io.bluetape4k.exposed.lettuce.domain.UserSchema
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserCredentialsRecord
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserCredentialsTable
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserRecord
import io.bluetape4k.exposed.lettuce.domain.UserSchema.UserTable
import io.bluetape4k.exposed.lettuce.domain.UserSchema.withSuspendedUserCredentialsTable
import io.bluetape4k.exposed.lettuce.domain.UserSchema.withSuspendedUserTable
import io.bluetape4k.exposed.lettuce.repository.scenarios.SuspendedReadThroughScenario
import io.bluetape4k.exposed.lettuce.repository.scenarios.SuspendedWriteThroughScenario
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Nested
import java.util.*
import kotlin.coroutines.CoroutineContext

@Suppress("DEPRECATION")
/**
 * exposed-jdbc-lettuce Read/Write-through 캐시 통합 테스트 (suspend 버전).
 *
 * - AutoIncrement Long ID 테이블([UserTable])과
 * - Client-generated UUID ID 테이블([UserCredentialsTable])에 대해 각각 검증한다.
 * - 각 ID 유형에 대해 Remote 캐시와 NearCache 두 가지 설정으로 테스트한다.
 */
class SuspendedReadWriteThroughCacheTest {
    companion object: KLoggingChannel()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — UserTable
    // -------------------------------------------------------------------------

    abstract class AutoIncSuspendedReadWriteThrough:
        AbstractJdbcLettuceTest(),
        SuspendedReadThroughScenario<Long, UserRecord>,
        SuspendedWriteThroughScenario<Long, UserRecord> {
        companion object: KLoggingChannel()

        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend JdbcTransaction.() -> Unit,
        ) = withSuspendedUserTable(testDB, context, statement)

        override suspend fun getExistingId(): Long =
            newSuspendedTransaction {
                UserTable
                    .select(UserTable.id)
                    .limit(1)
                    .first()[UserTable.id]
                    .value
            }

        override suspend fun getExistingIds(): List<Long> =
            newSuspendedTransaction {
                UserTable
                    .select(UserTable.id)
                    .map { it[UserTable.id].value }
            }

        override suspend fun getNonExistentId(): Long = Long.MIN_VALUE

        override suspend fun buildEntityForId(id: Long): UserRecord =
            UserSchema.newUserRecord().copy(id = id)

        override suspend fun updateEmail(entity: UserRecord): UserRecord =
            entity.copy(email = Base58.randomString(4) + "." + faker.internet().emailAddress())

        override suspend fun createNewEntity(): UserRecord = UserSchema.newUserRecord()
    }

    @Nested
    inner class AutoIncSuspendedReadWriteThroughRemoteCache: AutoIncSuspendedReadWriteThrough() {
        override val config = LettuceCacheConfig.READ_WRITE_THROUGH
        override val repository by lazy { SuspendedUserRepository(redisClient, config) }
    }

    @Nested
    inner class AutoIncSuspendedReadWriteThroughNearCache: AutoIncSuspendedReadWriteThrough() {
        override val config = LettuceCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(
            nearCacheName = "jdbc-lettuce-users-srwt-near"
        )
        override val repository by lazy { SuspendedUserRepository(redisClient, config) }
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — UserCredentialsTable
    // -------------------------------------------------------------------------

    abstract class ClientGenIdSuspendedReadWriteThrough:
        AbstractJdbcLettuceTest(),
        SuspendedReadThroughScenario<UUID, UserCredentialsRecord>,
        SuspendedWriteThroughScenario<UUID, UserCredentialsRecord> {
        companion object: KLoggingChannel()

        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend JdbcTransaction.() -> Unit,
        ) = withSuspendedUserCredentialsTable(testDB, context, statement)

        override suspend fun getExistingId(): UUID =
            newSuspendedTransaction {
                UserCredentialsTable
                    .select(UserCredentialsTable.id)
                    .limit(1)
                    .first()[UserCredentialsTable.id]
                    .value
            }

        override suspend fun getExistingIds(): List<UUID> =
            newSuspendedTransaction {
                UserCredentialsTable
                    .select(UserCredentialsTable.id)
                    .map { it[UserCredentialsTable.id].value }
            }

        override suspend fun getNonExistentId(): UUID = UUID.randomUUID()

        override suspend fun buildEntityForId(id: UUID): UserCredentialsRecord =
            UserSchema.newUserCredentialsRecord().copy(id = id)

        override suspend fun updateEmail(entity: UserCredentialsRecord): UserCredentialsRecord =
            entity.copy(email = Base58.randomString(4) + "." + faker.internet().emailAddress())

        override suspend fun createNewEntity(): UserCredentialsRecord = UserSchema.newUserCredentialsRecord()
    }

    @Nested
    inner class ClientGenIdSuspendedReadWriteThroughRemoteCache: ClientGenIdSuspendedReadWriteThrough() {
        override val config = LettuceCacheConfig.READ_WRITE_THROUGH
        override val repository by lazy { SuspendedUserCredentialRepository(redisClient, config) }
    }

    @Nested
    inner class ClientGenIdSuspendedReadWriteThroughNearCache: ClientGenIdSuspendedReadWriteThrough() {
        override val config = LettuceCacheConfig.READ_WRITE_THROUGH_WITH_NEAR_CACHE.copy(
            nearCacheName = "jdbc-lettuce-cred-srwt-near"
        )
        override val repository by lazy { SuspendedUserCredentialRepository(redisClient, config) }
    }
}
