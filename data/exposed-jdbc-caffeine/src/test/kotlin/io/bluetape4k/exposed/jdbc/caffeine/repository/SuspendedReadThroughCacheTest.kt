package io.bluetape4k.exposed.jdbc.caffeine.repository

import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.cache.scenarios.SuspendedJdbcReadThroughScenario
import io.bluetape4k.exposed.jdbc.caffeine.AbstractJdbcCaffeineTest
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
 * JDBC Caffeine Read-Through 캐시 suspend 통합 테스트.
 *
 * - AutoIncrement Long ID 테이블 ([ActorTable]) 과
 * - Client-generated UUID ID 테이블 ([CredentialTable]) 에 대해 각각 검증합니다.
 * - Caffeine 로컬 캐시(LOCAL 모드)만 사용하며, Redis/Testcontainers 불필요.
 */
class SuspendedReadThroughCacheTest {

    companion object: KLogging()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — ActorTable
    // -------------------------------------------------------------------------

    @Suppress("DEPRECATION")
    abstract class AutoIncActorSuspendedReadThrough:
        AbstractJdbcCaffeineTest(),
        SuspendedJdbcReadThroughScenario<Long, ActorRecord> {

        override val cacheWriteMode: CacheWriteMode = CacheWriteMode.READ_ONLY
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
    }

    @Nested
    inner class AutoIncSuspendedReadThrough: AutoIncActorSuspendedReadThrough() {
        private val config = LocalCacheConfig(
            keyPrefix = "jdbc:caffeine:s-rt:actor",
            writeMode = CacheWriteMode.READ_ONLY,
        )
        override val repository by lazy { ActorSuspendedJdbcCaffeineRepository(config) }
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — CredentialTable
    // -------------------------------------------------------------------------

    @Suppress("DEPRECATION")
    abstract class ClientGenIdCredentialSuspendedReadThrough:
        AbstractJdbcCaffeineTest(),
        SuspendedJdbcReadThroughScenario<UUID, CredentialRecord> {

        override val cacheWriteMode: CacheWriteMode = CacheWriteMode.READ_ONLY
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
    }

    @Nested
    inner class ClientGenIdSuspendedReadThrough: ClientGenIdCredentialSuspendedReadThrough() {
        private val config = LocalCacheConfig(
            keyPrefix = "jdbc:caffeine:s-rt:credential",
            writeMode = CacheWriteMode.READ_ONLY,
        )
        override val repository by lazy { CredentialSuspendedJdbcCaffeineRepository(config) }
    }
}
