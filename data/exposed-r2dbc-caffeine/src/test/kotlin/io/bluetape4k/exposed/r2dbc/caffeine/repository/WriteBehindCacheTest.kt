package io.bluetape4k.exposed.r2dbc.caffeine.repository

import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.cache.scenarios.R2dbcWriteBehindScenario
import io.bluetape4k.exposed.r2dbc.caffeine.AbstractR2dbcCaffeineTest
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorR2dbcCaffeineRepository
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.ActorRecord
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.ActorTable
import io.bluetape4k.exposed.r2dbc.caffeine.domain.ActorSchema.withActorTable
import io.bluetape4k.exposed.r2dbc.tests.TestDB
import io.bluetape4k.logging.coroutines.KLoggingChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import kotlin.coroutines.CoroutineContext

/**
 * R2DBC Caffeine Write-Behind 캐시 통합 테스트.
 *
 * Auto-increment Long ID 테이블 ([ActorTable])에 대해 검증합니다.
 * 캐시에 먼저 저장하고 DB에는 비동기로 반영되는 패턴을 검증합니다.
 */
class WriteBehindCacheTest:
    AbstractR2dbcCaffeineTest(),
    R2dbcWriteBehindScenario<Long, ActorRecord> {

    companion object: KLoggingChannel()

    override val cacheWriteMode: CacheWriteMode = CacheWriteMode.WRITE_BEHIND
    override val cacheMode: CacheMode = CacheMode.LOCAL

    private val config = LocalCacheConfig(
        keyPrefix = "r2dbc:caffeine:write-behind:actors",
        writeMode = CacheWriteMode.WRITE_BEHIND,
        writeBehindBatchSize = 10,
        writeBehindQueueCapacity = 1_000,
    )

    override val repository by lazy {
        ActorR2dbcCaffeineRepository(config)
    }

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
}
