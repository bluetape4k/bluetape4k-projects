package io.bluetape4k.exposed.r2dbc.caffeine.repository

import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.cache.scenarios.R2dbcReadThroughScenario
import io.bluetape4k.exposed.r2dbc.caffeine.AbstractR2dbcCaffeineTest
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorR2dbcCaffeineRepository
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
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.junit.jupiter.api.Nested
import java.util.*
import kotlin.coroutines.CoroutineContext

/**
 * R2DBC Caffeine Read-Through 캐시 통합 테스트.
 *
 * - AutoIncrement Long ID 테이블 ([ActorTable]) 과
 * - Client-generated UUID ID 테이블 ([CredentialTable]) 에 대해 각각 검증합니다.
 * - Caffeine 로컬 캐시(LOCAL 모드)만 사용하며, Redis/Testcontainers 불필요.
 */
class ReadThroughCacheTest {

    companion object: KLoggingChannel()

    // -------------------------------------------------------------------------
    // AutoIncrement Long ID — ActorTable
    // -------------------------------------------------------------------------

    abstract class AutoIncActorReadThrough:
        AbstractR2dbcCaffeineTest(),
        R2dbcReadThroughScenario<Long, ActorRecord> {

        override val cacheWriteMode: CacheWriteMode = CacheWriteMode.READ_ONLY
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
    }

    @Nested
    inner class AutoIncReadThrough: AutoIncActorReadThrough() {
        private val config = LocalCacheConfig(
            keyPrefix = "r2dbc:caffeine:rt:actor",
            writeMode = CacheWriteMode.READ_ONLY,
        )
        override val repository by lazy { ActorR2dbcCaffeineRepository(config) }
    }

    // -------------------------------------------------------------------------
    // Client-generated UUID ID — CredentialTable
    // -------------------------------------------------------------------------

    abstract class ClientGenIdCredentialReadThrough:
        AbstractR2dbcCaffeineTest(),
        R2dbcReadThroughScenario<UUID, CredentialRecord> {

        override val cacheWriteMode: CacheWriteMode = CacheWriteMode.READ_ONLY
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
    }

    @Nested
    inner class ClientGenIdReadThrough: ClientGenIdCredentialReadThrough() {
        private val config = LocalCacheConfig(
            keyPrefix = "r2dbc:caffeine:rt:credential",
            writeMode = CacheWriteMode.READ_ONLY,
        )
        override val repository by lazy { CredentialR2dbcCaffeineRepository(config) }
    }
}
