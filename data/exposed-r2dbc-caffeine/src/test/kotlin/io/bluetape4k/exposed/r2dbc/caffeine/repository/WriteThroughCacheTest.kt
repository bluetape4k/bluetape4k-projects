package io.bluetape4k.exposed.r2dbc.caffeine.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.cache.scenarios.R2dbcReadThroughScenario
import io.bluetape4k.exposed.cache.scenarios.R2dbcWriteThroughScenario
import io.bluetape4k.exposed.r2dbc.caffeine.AbstractR2dbcCaffeineTest
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorR2dbcCaffeineRepository
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.ActorRecord
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.ActorTable
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.CredentialRecord
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.CredentialTable
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.withActorTable
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.withCredentialTable
import io.bluetape4k.exposed.r2dbc.caffeine.domain.CredentialR2dbcCaffeineRepository
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Nested
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * R2DBC Caffeine Write-Through 캐시 통합 테스트.
 *
 * - AutoIncrement Long ID 테이블 ([ActorTable]) 과
 * - Client-generated UUID ID 테이블 ([CredentialTable]) 에 대해 각각 검증합니다.
 * - 캐시에 저장하면 DB에도 동시 반영되는 패턴을 검증합니다.
 */
class WriteThroughCacheTest {

    companion object: KLoggingChannel()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — ActorTable
    // -------------------------------------------------------------------------

    abstract class AutoIncActorWriteThrough:
        AbstractR2dbcCaffeineTest(),
        R2dbcReadThroughScenario<Long, ActorRecord>,
        R2dbcWriteThroughScenario<Long, ActorRecord> {

        override val cacheWriteMode: CacheWriteMode = CacheWriteMode.WRITE_THROUGH
        override val cacheMode: CacheMode = CacheMode.LOCAL

        override suspend fun withR2dbcEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend R2dbcTransaction.() -> Unit,
        ) = withActorTable(testDB, statement)

        override suspend fun getExistingId(): Long =
            suspendTransaction {
                ActorTable.select(ActorTable.id).first()[ActorTable.id].value
            }

        override suspend fun getExistingIds(): List<Long> =
            suspendTransaction {
                ActorTable.select(ActorTable.id).map { it[ActorTable.id].value }.toList()
            }

        override suspend fun getNonExistentId(): Long = Long.MIN_VALUE

        override suspend fun createNewEntity(): ActorRecord =
            ActorSchema.newActorRecord()

        override suspend fun updateEntityEmail(entity: ActorRecord): ActorRecord =
            entity.copy(email = Base58.randomString(4) + ".wt-updated@example.com")

        override suspend fun assertSameEntityWithoutAudit(entity1: ActorRecord, entity2: ActorRecord) {
            entity1.firstName shouldBeEqualTo entity2.firstName
            entity1.lastName shouldBeEqualTo entity2.lastName
            entity1.email shouldBeEqualTo entity2.email
        }
    }

    @Nested
    inner class AutoIncWriteThrough: AutoIncActorWriteThrough() {
        private val config = LocalCacheConfig(
            keyPrefix = "r2dbc:caffeine:wt:actor",
            writeMode = CacheWriteMode.WRITE_THROUGH,
        )
        override val repository by lazy { ActorR2dbcCaffeineRepository(config) }
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — CredentialTable
    // -------------------------------------------------------------------------

    abstract class ClientGenIdCredentialWriteThrough:
        AbstractR2dbcCaffeineTest(),
        R2dbcReadThroughScenario<UUID, CredentialRecord>,
        R2dbcWriteThroughScenario<UUID, CredentialRecord> {

        override val cacheWriteMode: CacheWriteMode = CacheWriteMode.WRITE_THROUGH
        override val cacheMode: CacheMode = CacheMode.LOCAL

        override suspend fun withR2dbcEntityTable(
            testDB: TestDB,
            context: CoroutineContext,
            statement: suspend R2dbcTransaction.() -> Unit,
        ) = withCredentialTable(testDB, statement)

        override suspend fun getExistingId(): UUID =
            suspendTransaction {
                CredentialTable.select(CredentialTable.id).first()[CredentialTable.id].value
            }

        override suspend fun getExistingIds(): List<UUID> =
            suspendTransaction {
                CredentialTable.select(CredentialTable.id).map { it[CredentialTable.id].value }.toList()
            }

        override suspend fun getNonExistentId(): UUID = UUID.randomUUID()

        override suspend fun createNewEntity(): CredentialRecord =
            ActorSchema.newCredentialRecord()

        override suspend fun updateEntityEmail(entity: CredentialRecord): CredentialRecord =
            entity.copy(email = Base58.randomString(4) + ".wt-updated@example.com")

        override suspend fun assertSameEntityWithoutAudit(entity1: CredentialRecord, entity2: CredentialRecord) {
            entity1.loginId shouldBeEqualTo entity2.loginId
            entity1.email shouldBeEqualTo entity2.email
        }
    }

    @Nested
    inner class ClientGenIdWriteThrough: ClientGenIdCredentialWriteThrough() {
        private val config = LocalCacheConfig(
            keyPrefix = "r2dbc:caffeine:wt:credential",
            writeMode = CacheWriteMode.WRITE_THROUGH,
        )
        override val repository by lazy { CredentialR2dbcCaffeineRepository(config) }
    }
}
