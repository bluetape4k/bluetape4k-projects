package io.bluetape4k.exposed.jdbc.caffeine.repository

import io.bluetape4k.codec.Base58
import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.cache.scenarios.SuspendedJdbcReadThroughScenario
import io.bluetape4k.exposed.cache.scenarios.SuspendedJdbcWriteThroughScenario
import io.bluetape4k.exposed.jdbc.caffeine.AbstractJdbcCaffeineTest
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.ActorRecord
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.ActorTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.withSuspendedActorTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSuspendedJdbcCaffeineRepository
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import kotlin.coroutines.CoroutineContext

/**
 * JDBC Caffeine Write-Through 캐시 suspend 통합 테스트.
 *
 * Auto-increment Long ID 테이블 ([ActorTable])에 대해 검증합니다.
 * 캐시에 저장하면 DB에도 동시 반영되는 패턴을 검증합니다.
 */
@Suppress("DEPRECATION")
class SuspendedWriteThroughCacheTest:
    AbstractJdbcCaffeineTest(),
    SuspendedJdbcReadThroughScenario<Long, ActorRecord>,
    SuspendedJdbcWriteThroughScenario<Long, ActorRecord> {

    companion object: KLogging()

    override val cacheWriteMode: CacheWriteMode = CacheWriteMode.WRITE_THROUGH
    override val cacheMode: CacheMode = CacheMode.LOCAL

    private val config = LocalCacheConfig(
        keyPrefix = "jdbc:caffeine:suspended-write-through:actors",
        writeMode = CacheWriteMode.WRITE_THROUGH,
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

    override suspend fun updateEntityEmail(entity: ActorRecord): ActorRecord =
        entity.copy(email = Base58.randomString(4) + "." + faker.internet().emailAddress())

    override suspend fun assertSameEntityWithoutAudit(entity1: ActorRecord, entity2: ActorRecord) {
        entity1.firstName shouldBeEqualTo entity2.firstName
        entity1.lastName shouldBeEqualTo entity2.lastName
        entity1.email shouldBeEqualTo entity2.email
    }
}
