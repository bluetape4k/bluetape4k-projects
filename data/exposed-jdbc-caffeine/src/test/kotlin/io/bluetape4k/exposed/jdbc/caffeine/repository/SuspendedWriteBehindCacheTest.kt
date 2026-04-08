package io.bluetape4k.exposed.jdbc.caffeine.repository

import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.cache.scenarios.SuspendedJdbcWriteBehindScenario
import io.bluetape4k.exposed.jdbc.caffeine.AbstractJdbcCaffeineTest
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.ActorRecord
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.ActorTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.withSuspendedActorTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSuspendedJdbcCaffeineRepository
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import kotlin.coroutines.CoroutineContext

/**
 * JDBC Caffeine Write-Behind 캐시 suspend 통합 테스트.
 *
 * Auto-increment Long ID 테이블 ([ActorTable])에 대해 검증합니다.
 * 캐시에 먼저 저장하고 DB에는 비동기로 반영되는 패턴을 검증합니다.
 */
@Suppress("DEPRECATION")
class SuspendedWriteBehindCacheTest:
    AbstractJdbcCaffeineTest(),
    SuspendedJdbcWriteBehindScenario<Long, ActorRecord> {

    companion object: KLogging()

    override val cacheWriteMode: CacheWriteMode = CacheWriteMode.WRITE_BEHIND
    override val cacheMode: CacheMode = CacheMode.LOCAL

    private val config = LocalCacheConfig(
        keyPrefix = "jdbc:caffeine:suspended-write-behind:actors",
        writeMode = CacheWriteMode.WRITE_BEHIND,
        writeBehindBatchSize = 10,
        writeBehindQueueCapacity = 1_000,
    )

    override val repository by lazy {
        ActorSuspendedJdbcCaffeineRepository(config)
    }

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
