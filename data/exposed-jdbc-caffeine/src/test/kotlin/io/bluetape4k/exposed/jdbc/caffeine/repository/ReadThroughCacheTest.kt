package io.bluetape4k.exposed.jdbc.caffeine.repository

import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.cache.scenarios.JdbcReadThroughScenario
import io.bluetape4k.exposed.jdbc.caffeine.AbstractJdbcCaffeineTest
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorJdbcCaffeineRepository
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
 * JDBC Caffeine Read-Through 캐시 통합 테스트.
 *
 * - AutoIncrement Long ID 테이블 ([ActorTable]) 과
 * - Client-generated UUID ID 테이블 ([CredentialTable]) 에 대해 각각 검증합니다.
 * - Caffeine 로컬 캐시(LOCAL 모드)만 사용하며, Redis/Testcontainers 불필요.
 */
class ReadThroughCacheTest {

    companion object: KLogging()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — ActorTable
    // -------------------------------------------------------------------------

    abstract class AutoIncActorReadThrough:
        AbstractJdbcCaffeineTest(),
        JdbcReadThroughScenario<Long, ActorRecord> {

        override val cacheWriteMode: CacheWriteMode = CacheWriteMode.READ_ONLY
        override val cacheMode: CacheMode = CacheMode.LOCAL

        override fun withEntityTable(testDB: TestDB, statement: JdbcTransaction.() -> Unit) =
            withActorTable(testDB, statement)

        override fun getExistingId(): Long =
            transaction {
                ActorTable.select(ActorTable.id).limit(1).first()[ActorTable.id].value
            }

        override fun getExistingIds(): List<Long> =
            transaction {
                ActorTable.select(ActorTable.id).map { it[ActorTable.id].value }
            }

        override fun getNonExistentId(): Long = Long.MIN_VALUE
    }

    @Nested
    inner class AutoIncReadThrough: AutoIncActorReadThrough() {
        private val config = LocalCacheConfig(
            keyPrefix = "jdbc:caffeine:rt:actor",
            writeMode = CacheWriteMode.READ_ONLY,
        )
        override val repository by lazy { ActorJdbcCaffeineRepository(config) }
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — CredentialTable
    // -------------------------------------------------------------------------

    abstract class ClientGenIdCredentialReadThrough:
        AbstractJdbcCaffeineTest(),
        JdbcReadThroughScenario<UUID, CredentialRecord> {

        override val cacheWriteMode: CacheWriteMode = CacheWriteMode.READ_ONLY
        override val cacheMode: CacheMode = CacheMode.LOCAL

        override fun withEntityTable(testDB: TestDB, statement: JdbcTransaction.() -> Unit) =
            withCredentialTable(testDB, statement)

        override fun getExistingId(): UUID =
            transaction {
                CredentialTable.select(CredentialTable.id).limit(1).first()[CredentialTable.id].value
            }

        override fun getExistingIds(): List<UUID> =
            transaction {
                CredentialTable.select(CredentialTable.id).map { it[CredentialTable.id].value }
            }

        override fun getNonExistentId(): UUID = UUID.randomUUID()
    }

    @Nested
    inner class ClientGenIdReadThrough: ClientGenIdCredentialReadThrough() {
        private val config = LocalCacheConfig(
            keyPrefix = "jdbc:caffeine:rt:credential",
            writeMode = CacheWriteMode.READ_ONLY,
        )
        override val repository by lazy { CredentialJdbcCaffeineRepository(config) }
    }
}
