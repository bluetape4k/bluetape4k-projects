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
import io.bluetape4k.exposed.lettuce.repository.scenarios.SuspendedWriteBehindScenario
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import io.bluetape4k.redis.lettuce.map.LettuceCacheConfig
import io.bluetape4k.redis.lettuce.map.WriteMode
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Nested
import java.time.Duration
import java.util.*
import kotlin.coroutines.CoroutineContext

@Suppress("DEPRECATION")
/**
 * exposed-jdbc-lettuce Write-behind 캐시 통합 테스트 (suspend 버전).
 *
 * - AutoIncrement Long ID 테이블([UserTable])과
 * - Client-generated UUID ID 테이블([UserCredentialsTable])에 대해 각각 검증한다.
 * - 각 ID 유형에 대해 Remote 캐시와 NearCache 두 가지 설정으로 테스트한다.
 * - `writeBehindDelay = 300ms`로 설정하여 테스트 시 빠른 flush를 유도한다.
 */
class SuspendedWriteBehindCacheTest {
    companion object : KLoggingChannel()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — UserTable
    // -------------------------------------------------------------------------

    abstract class AutoIncSuspendedWriteBehind :
        AbstractJdbcLettuceTest(),
        SuspendedWriteBehindScenario<Long, UserRecord> {
        companion object : KLoggingChannel()

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

        override suspend fun updateEmail(entity: UserRecord): UserRecord =
            entity.copy(email = Base58.randomString(4) + "." + faker.internet().emailAddress())

        override suspend fun createNewEntity(): UserRecord = UserSchema.newUserRecord()
    }

    @Nested
    inner class AutoIncSuspendedWriteBehindRemoteCache : AutoIncSuspendedWriteBehind() {
        override val config = LettuceCacheConfig(
            writeMode = WriteMode.WRITE_BEHIND,
            writeBehindDelay = Duration.ofMillis(300),
            writeBehindBatchSize = 50,
            keyPrefix = "jdbc-swb-auto-remote"
        )
        override val repository by lazy { SuspendedUserRepository(redisClient, config) }
    }

    @Nested
    inner class AutoIncSuspendedWriteBehindNearCache : AutoIncSuspendedWriteBehind() {
        override val config = LettuceCacheConfig(
            writeMode = WriteMode.WRITE_BEHIND,
            writeBehindDelay = Duration.ofMillis(300),
            writeBehindBatchSize = 50,
            keyPrefix = "jdbc-swb-auto-near",
            nearCacheEnabled = true,
            nearCacheName = "jdbc-lettuce-users-swb-near"
        )
        override val repository by lazy { SuspendedUserRepository(redisClient, config) }
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — UserCredentialsTable
    // -------------------------------------------------------------------------

    abstract class ClientGenIdSuspendedWriteBehind :
        AbstractJdbcLettuceTest(),
        SuspendedWriteBehindScenario<UUID, UserCredentialsRecord> {
        companion object : KLoggingChannel()

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

        override suspend fun updateEmail(entity: UserCredentialsRecord): UserCredentialsRecord =
            entity.copy(email = Base58.randomString(4) + "." + faker.internet().emailAddress())

        override suspend fun createNewEntity(): UserCredentialsRecord = UserSchema.newUserCredentialsRecord()
    }

    @Nested
    inner class ClientGenIdSuspendedWriteBehindRemoteCache : ClientGenIdSuspendedWriteBehind() {
        override val config = LettuceCacheConfig(
            writeMode = WriteMode.WRITE_BEHIND,
            writeBehindDelay = Duration.ofMillis(300),
            writeBehindBatchSize = 50,
            keyPrefix = "jdbc-swb-client-remote"
        )
        override val repository by lazy { SuspendedUserCredentialRepository(redisClient, config) }
    }

    @Nested
    inner class ClientGenIdSuspendedWriteBehindNearCache : ClientGenIdSuspendedWriteBehind() {
        override val config = LettuceCacheConfig(
            writeMode = WriteMode.WRITE_BEHIND,
            writeBehindDelay = Duration.ofMillis(300),
            writeBehindBatchSize = 50,
            keyPrefix = "jdbc-swb-client-near",
            nearCacheEnabled = true,
            nearCacheName = "jdbc-lettuce-cred-swb-near"
        )
        override val repository by lazy { SuspendedUserCredentialRepository(redisClient, config) }
    }
}
