package io.bluetape4k.exposed.jdbc.caffeine.repository

import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.cache.scenarios.SuspendedJdbcWriteBehindScenario
import io.bluetape4k.exposed.jdbc.caffeine.AbstractJdbcCaffeineTest
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.ActorRecord
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.ActorTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.CredentialRecord
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.CredentialTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.withSuspendedActorTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.withSuspendedCredentialTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSuspendedJdbcCaffeineRepository
import io.bluetape4k.exposed.jdbc.caffeine.domain.CredentialSuspendedJdbcCaffeineRepository
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.Nested
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * JDBC Caffeine Write-Behind 캐시 suspend 통합 테스트.
 *
 * - AutoIncrement Long ID 테이블 ([ActorTable]) 과
 * - Client-generated UUID ID 테이블 ([CredentialTable]) 에 대해 각각 검증합니다.
 * - 캐시에 먼저 저장하고 DB에는 비동기로 반영되는 패턴을 검증합니다.
 */
class SuspendedWriteBehindCacheTest {

    companion object: KLogging()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — ActorTable
    // -------------------------------------------------------------------------

    @Suppress("DEPRECATION")
    abstract class AutoIncActorSuspendedWriteBehind:
        AbstractJdbcCaffeineTest(),
        SuspendedJdbcWriteBehindScenario<Long, ActorRecord> {

        override val cacheWriteMode: CacheWriteMode = CacheWriteMode.WRITE_BEHIND
        override val cacheMode: CacheMode = CacheMode.LOCAL

        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend JdbcTransaction.() -> Unit,
        ) = withSuspendedActorTable(testDB, context, statement)

        override suspend fun getExistingId(): Long =
            newSuspendedTransaction {
                ActorTable.select(ActorTable.id).first()[ActorTable.id].value
            }

        override suspend fun getExistingIds(): List<Long> =
            newSuspendedTransaction {
                ActorTable.select(ActorTable.id).map { it[ActorTable.id].value }
            }

        override suspend fun getNonExistentId(): Long = Long.MIN_VALUE

        override suspend fun createNewEntity(): ActorRecord =
            ActorSchema.newActorRecord()
    }

    @Nested
    inner class AutoIncSuspendedWriteBehind: AutoIncActorSuspendedWriteBehind() {
        private val config = LocalCacheConfig(
            keyPrefix = "jdbc:caffeine:s-wb:actor",
            writeMode = CacheWriteMode.WRITE_BEHIND,
            writeBehindBatchSize = 10,
            writeBehindQueueCapacity = 1_000,
        )
        override val repository by lazy { ActorSuspendedJdbcCaffeineRepository(config) }
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — CredentialTable
    // -------------------------------------------------------------------------

    @Suppress("DEPRECATION")
    abstract class ClientGenIdCredentialSuspendedWriteBehind:
        AbstractJdbcCaffeineTest(),
        SuspendedJdbcWriteBehindScenario<UUID, CredentialRecord> {

        override val cacheWriteMode: CacheWriteMode = CacheWriteMode.WRITE_BEHIND
        override val cacheMode: CacheMode = CacheMode.LOCAL

        override suspend fun withSuspendedEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend JdbcTransaction.() -> Unit,
        ) = withSuspendedCredentialTable(testDB, context, statement)

        override suspend fun getExistingId(): UUID =
            newSuspendedTransaction {
                CredentialTable.select(CredentialTable.id).first()[CredentialTable.id].value
            }

        override suspend fun getExistingIds(): List<UUID> =
            newSuspendedTransaction {
                CredentialTable.select(CredentialTable.id).map { it[CredentialTable.id].value }
            }

        override suspend fun getNonExistentId(): UUID = UUID.randomUUID()

        override suspend fun createNewEntity(): CredentialRecord =
            ActorSchema.newCredentialRecord()
    }

    @Nested
    inner class ClientGenIdSuspendedWriteBehind: ClientGenIdCredentialSuspendedWriteBehind() {
        private val config = LocalCacheConfig(
            keyPrefix = "jdbc:caffeine:s-wb:credential",
            writeMode = CacheWriteMode.WRITE_BEHIND,
            writeBehindBatchSize = 10,
            writeBehindQueueCapacity = 1_000,
        )
        override val repository by lazy { CredentialSuspendedJdbcCaffeineRepository(config) }
    }
}
