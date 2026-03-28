package io.bluetape4k.exposed.duckdb

import io.bluetape4k.exposed.duckdb.domain.Events
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class DuckDBExtensionsTest : AbstractDuckDBTest() {

    companion object : KLogging() {
        private val OCCURRED_AT: Instant = Instant.parse("2024-01-01T00:00:00Z")

        private val FIXTURES = listOf(
            Triple(1L, "kr", BigDecimal("10.00")),
            Triple(2L, "us", BigDecimal("20.00")),
            Triple(3L, "eu", BigDecimal("30.00")),
        )
    }

    @BeforeEach
    fun setUp() {
        withEventsTable {
            transaction(db) {
                FIXTURES.forEach { (id, region, amount) ->
                    Events.insert {
                        it[eventId] = id
                        it[userId] = 1000L + id
                        it[eventType] = "PURCHASE"
                        it[Events.region] = region
                        it[Events.amount] = amount
                        it[occurredAt] = OCCURRED_AT
                    }
                }
            }
        }
    }

    @Test
    fun `suspendTransaction 은 DuckDB 트랜잭션 결과를 반환한다`() = runTest {
        val count = suspendTransaction(db) {
            Events.selectAll().count()
        }

        count shouldBeEqualTo 3L
    }

    @Test
    fun `queryFlow 는 lazy query 를 트랜잭션 안에서 materialize 한다`() = runTest {
        val rows = queryFlow(db) {
            Events.selectAll()
                .orderBy(Events.eventId to SortOrder.ASC)
        }.toList()

        rows shouldHaveSize 3
        rows.map { it[Events.eventId] } shouldBeEqualTo listOf(1L, 2L, 3L)
    }
}
