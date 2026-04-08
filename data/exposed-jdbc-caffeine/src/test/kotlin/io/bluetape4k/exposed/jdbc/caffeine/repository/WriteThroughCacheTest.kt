package io.bluetape4k.exposed.jdbc.caffeine.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.cache.scenarios.JdbcReadThroughScenario
import io.bluetape4k.exposed.cache.scenarios.JdbcWriteThroughScenario
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
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Nested
import java.util.*

/**
 * JDBC Caffeine Write-Through 캐시 통합 테스트.
 *
 * - AutoIncrement Long ID 테이블 ([ActorTable]) 과
 * - Client-generated UUID ID 테이블 ([CredentialTable]) 에 대해 각각 검증합니다.
 * - 캐시에 저장하면 DB에도 동시 반영되는 패턴을 검증합니다.
 */
class WriteThroughCacheTest {

    companion object: KLogging()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — ActorTable
    // -------------------------------------------------------------------------

    abstract class AutoIncActorWriteThrough:
        AbstractJdbcCaffeineTest(),
        JdbcReadThroughScenario<Long, ActorRecord>,
        JdbcWriteThroughScenario<Long, ActorRecord> {

        override val cacheWriteMode: CacheWriteMode = CacheWriteMode.WRITE_THROUGH
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

        override fun createNewEntity(): ActorRecord =
            ActorSchema.newActorRecord()

        override fun updateEntityEmail(entity: ActorRecord): ActorRecord =
            entity.copy(email = Base58.randomString(4) + ".wt-updated@example.com")

        override fun assertSameEntityWithoutUpdatedAt(entity1: ActorRecord, entity2: ActorRecord) {
            entity1.firstName shouldBeEqualTo entity2.firstName
            entity1.lastName shouldBeEqualTo entity2.lastName
            entity1.email shouldBeEqualTo entity2.email
        }
    }

    @Nested
    inner class AutoIncWriteThrough: AutoIncActorWriteThrough() {
        private val config = LocalCacheConfig(
            keyPrefix = "jdbc:caffeine:wt:actor",
            writeMode = CacheWriteMode.WRITE_THROUGH,
        )
        override val repository by lazy { ActorJdbcCaffeineRepository(config) }
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — CredentialTable
    // -------------------------------------------------------------------------

    abstract class ClientGenIdCredentialWriteThrough:
        AbstractJdbcCaffeineTest(),
        JdbcReadThroughScenario<UUID, CredentialRecord>,
        JdbcWriteThroughScenario<UUID, CredentialRecord> {

        override val cacheWriteMode: CacheWriteMode = CacheWriteMode.WRITE_THROUGH
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

        override fun createNewEntity(): CredentialRecord =
            ActorSchema.newCredentialRecord()

        override fun updateEntityEmail(entity: CredentialRecord): CredentialRecord =
            entity.copy(email = Base58.randomString(4) + ".wt-updated@example.com")

        override fun assertSameEntityWithoutUpdatedAt(entity1: CredentialRecord, entity2: CredentialRecord) {
            entity1.loginId shouldBeEqualTo entity2.loginId
            entity1.email shouldBeEqualTo entity2.email
        }
    }

    @Nested
    inner class ClientGenIdWriteThrough: ClientGenIdCredentialWriteThrough() {
        private val config = LocalCacheConfig(
            keyPrefix = "jdbc:caffeine:wt:credential",
            writeMode = CacheWriteMode.WRITE_THROUGH,
        )
        override val repository by lazy { CredentialJdbcCaffeineRepository(config) }
    }
}
