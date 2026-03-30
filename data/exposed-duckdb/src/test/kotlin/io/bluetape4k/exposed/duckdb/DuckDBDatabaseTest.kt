package io.bluetape4k.exposed.duckdb

import io.bluetape4k.exposed.duckdb.domain.Events
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class DuckDBDatabaseTest {

    @Test
    fun `inMemory 는 트랜잭션 사이에서 상태를 공유하지 않는다`() {
        val db = DuckDBDatabase.inMemory()

        transaction(db) {
            SchemaUtils.create(Events)
            Events.insert {
                it[eventId] = 1L
                it[userId] = 100L
                it[eventType] = "PURCHASE"
                it[region] = "kr"
                it[amount] = BigDecimal("10.00")
                it[occurredAt] = Instant.parse("2024-01-01T00:00:00Z")
            }
            Events.selectAll().count() shouldBeEqualTo 1L
        }

        val failure = runCatching {
            transaction(db) {
                Events.selectAll().count()
            }
        }.exceptionOrNull()

        failure.shouldNotBeNull()
    }
}
