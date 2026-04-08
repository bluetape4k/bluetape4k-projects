package io.bluetape4k.exposed.jdbc.caffeine.repository

import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.cache.scenarios.JdbcWriteBehindScenario
import io.bluetape4k.exposed.jdbc.caffeine.AbstractJdbcCaffeineTest
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorJdbcCaffeineRepository
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.ActorRecord
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.ActorTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.CredentialRecord
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.CredentialTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.withActorTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.withCredentialTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.CredentialJdbcCaffeineRepository
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Nested
import java.util.*

/**
 * JDBC Caffeine Write-Behind 캐시 통합 테스트.
 *
 * - AutoIncrement Long ID 테이블 ([ActorTable]) 과
 * - Client-generated UUID ID 테이블 ([CredentialTable]) 에 대해 각각 검증합니다.
 * 캐시에 먼저 저장하고 DB에는 비동기로 반영되는 패턴을 검증합니다.
 */
class WriteBehindCacheTest {

    companion object: KLogging()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — ActorTable
    // -------------------------------------------------------------------------

    @Nested
    inner class AutoIncActorWriteBehind:
        AbstractJdbcCaffeineTest(),
        JdbcWriteBehindScenario<Long, ActorRecord> {

        override val cacheWriteMode: CacheWriteMode = CacheWriteMode.WRITE_BEHIND
        override val cacheMode: CacheMode = CacheMode.LOCAL

        private val config = LocalCacheConfig(
            keyPrefix = "jdbc:caffeine:write-behind:actors",
            writeMode = CacheWriteMode.WRITE_BEHIND,
            writeBehindBatchSize = 10,
            writeBehindQueueCapacity = 1_000,
        )

        override val repository by lazy {
            ActorJdbcCaffeineRepository(config)
        }

        override fun withEntityTable(
            testDB: TestDB,
            statement: JdbcTransaction.() -> Unit,
        ) = withActorTable(testDB, statement)

        override fun getExistingId(): Long =
            transaction {
                ActorTable.select(ActorTable.id).first()[ActorTable.id].value
            }

        override fun getExistingIds(): List<Long> =
            transaction {
                ActorTable.select(ActorTable.id).map { it[ActorTable.id].value }
            }

        override fun getNonExistentId(): Long = Long.MIN_VALUE

        override fun createNewEntity(): ActorRecord =
            ActorSchema.newActorRecord()
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — CredentialTable
    // -------------------------------------------------------------------------

    @Nested
    inner class ClientGenIdCredentialWriteBehind:
        AbstractJdbcCaffeineTest(),
        JdbcWriteBehindScenario<UUID, CredentialRecord> {

        override val cacheWriteMode: CacheWriteMode = CacheWriteMode.WRITE_BEHIND
        override val cacheMode: CacheMode = CacheMode.LOCAL

        private val config = LocalCacheConfig(
            keyPrefix = "jdbc:caffeine:write-behind:credentials",
            writeMode = CacheWriteMode.WRITE_BEHIND,
            writeBehindBatchSize = 10,
            writeBehindQueueCapacity = 1_000,
        )

        override val repository by lazy {
            CredentialJdbcCaffeineRepository(config)
        }

        override fun withEntityTable(
            testDB: TestDB,
            statement: JdbcTransaction.() -> Unit,
        ) = withCredentialTable(testDB, statement)

        override fun getExistingId(): UUID =
            transaction {
                CredentialTable.select(CredentialTable.id).first()[CredentialTable.id].value
            }

        override fun getExistingIds(): List<UUID> =
            transaction {
                CredentialTable.select(CredentialTable.id).map { it[CredentialTable.id].value }
            }

        override fun getNonExistentId(): UUID = UUID.randomUUID()

        override fun createNewEntity(): CredentialRecord =
            ActorSchema.newCredentialRecord()
    }
}
