package io.bluetape4k.exposed.jdbc.caffeine.repository

import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.cache.scenarios.SuspendedJdbcReadThroughScenario
import io.bluetape4k.exposed.jdbc.caffeine.AbstractJdbcCaffeineTest
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
 * JDBC Caffeine Read-Through 캐시 suspend 통합 테스트.
 *
 * Auto-increment Long ID 테이블 ([ActorTable])에 대해 검증합니다.
 * Caffeine 로컬 캐시(LOCAL 모드)만 사용하며, Redis/Testcontainers 불필요.
 */
@Suppress("DEPRECATION")
class SuspendedReadThroughCacheTest:
    AbstractJdbcCaffeineTest(),
    SuspendedJdbcReadThroughScenario<Long, ActorRecord> {

    companion object: KLogging()

    override val cacheWriteMode: CacheWriteMode = CacheWriteMode.READ_ONLY
    override val cacheMode: CacheMode = CacheMode.LOCAL

    private val config = LocalCacheConfig(
        keyPrefix = "jdbc:caffeine:suspended-read-through:actors",
        writeMode = CacheWriteMode.READ_ONLY,
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
}
