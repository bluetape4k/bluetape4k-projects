package io.bluetape4k.exposed.lettuce.repository

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
 * exposed-jdbc-lettuce Read-through 캐시 통합 테스트 (suspend 버전).
 *
 * - AutoIncrement Long ID 테이블([UserTable])과
 * - Client-generated UUID ID 테이블([UserCredentialsTable])에 대해 각각 검증한다.
 * - 각 ID 유형에 대해 Remote 캐시와 NearCache 두 가지 설정으로 테스트한다.
 */
class SuspendedReadThroughCacheTest {
    companion object: KLoggingChannel()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — UserTable
    // -------------------------------------------------------------------------

    abstract class AutoIncSuspendedReadThrough:
        AbstractJdbcLettuceTest(),
        SuspendedReadThroughScenario<Long, UserRecord> {
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
    }

    @Nested
    inner class AutoIncSuspendedReadThroughRemoteCache: AutoIncSuspendedReadThrough() {
        override val config = LettuceCacheConfig.READ_ONLY
        override val repository by lazy { SuspendedUserRepository(redisClient, config) }
    }

    @Nested
    inner class AutoIncSuspendedReadThroughNearCache: AutoIncSuspendedReadThrough() {
        override val config = LettuceCacheConfig.READ_ONLY_WITH_NEAR_CACHE.copy(
            nearCacheName = "jdbc-lettuce-users-srt-near"
        )
        override val repository by lazy { SuspendedUserRepository(redisClient, config) }
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — UserCredentialsTable
    // -------------------------------------------------------------------------

    abstract class ClientGenIdSuspendedReadThrough:
        AbstractJdbcLettuceTest(),
        SuspendedReadThroughScenario<UUID, UserCredentialsRecord> {
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
    }

    @Nested
    inner class ClientGenIdSuspendedReadThroughRemoteCache: ClientGenIdSuspendedReadThrough() {
        override val config = LettuceCacheConfig.READ_ONLY
        override val repository by lazy { SuspendedUserCredentialRepository(redisClient, config) }
    }

    @Nested
    inner class ClientGenIdSuspendedReadThroughNearCache: ClientGenIdSuspendedReadThrough() {
        override val config = LettuceCacheConfig.READ_ONLY_WITH_NEAR_CACHE.copy(
            nearCacheName = "jdbc-lettuce-cred-srt-near"
        )
        override val repository by lazy { SuspendedUserCredentialRepository(redisClient, config) }
    }
}
