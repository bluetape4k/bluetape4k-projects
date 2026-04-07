package io.bluetape4k.exposed.jdbc.caffeine.repository

import io.bluetape4k.exposed.cache.CacheMode
import io.bluetape4k.exposed.cache.CacheWriteMode
import io.bluetape4k.exposed.cache.LocalCacheConfig
import io.bluetape4k.exposed.cache.scenarios.JdbcReadThroughScenario
import io.bluetape4k.exposed.jdbc.caffeine.AbstractJdbcCaffeineTest
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorJdbcCaffeineRepository
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.ActorRecord
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.ActorTable
import io.bluetape4k.exposed.jdbc.caffeine.domain.ActorSchema.withActorTable
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * JDBC Caffeine Read-Through 캐시 통합 테스트.
 *
 * Auto-increment Long ID 테이블 ([ActorTable])에 대해 검증합니다.
 * Caffeine 로컬 캐시(LOCAL 모드)만 사용하며, Redis/Testcontainers 불필요.
 */
class ReadThroughCacheTest:
    AbstractJdbcCaffeineTest(),
    JdbcReadThroughScenario<Long, ActorRecord> {

    companion object: KLogging()

    override val cacheWriteMode: CacheWriteMode = CacheWriteMode.READ_ONLY
    override val cacheMode: CacheMode = CacheMode.LOCAL

    private val config = LocalCacheConfig(
        keyPrefix = "jdbc:caffeine:read-through:actors",
        writeMode = CacheWriteMode.READ_ONLY,
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
}
