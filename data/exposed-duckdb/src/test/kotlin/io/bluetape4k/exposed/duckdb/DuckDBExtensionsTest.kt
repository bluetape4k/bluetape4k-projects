package io.bluetape4k.exposed.duckdb

import io.bluetape4k.exposed.duckdb.domain.Events
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import io.bluetape4k.assertions.assertFailsWith

class DuckDBExtensionsTest: AbstractDuckDBTest() {

    companion object: KLogging() {
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
    fun `suspendTransaction 은 커스텀 디스패처에서도 올바르게 동작한다`() = runTest {
        val count = suspendTransaction(db, Dispatchers.Default) {
            Events.selectAll().count()
        }

        count shouldBeEqualTo 3L
    }

    @Test
    fun `suspendTransaction 은 트랜잭션 내 예외를 그대로 전파한다`() = runTest {
        assertFailsWith<IllegalStateException> {
            suspendTransaction(db) {
                error("트랜잭션 내부 오류")
            }
        }
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

    @Test
    fun `queryFlow 는 빈 결과를 빈 리스트로 반환한다`() = runTest {
        withEventsTable {}

        val rows = queryFlow(db) {
            Events.selectAll()
                .orderBy(Events.eventId to SortOrder.ASC)
        }.toList()

        rows shouldHaveSize 0
    }

    @Test
    fun `queryFlow 는 where 조건으로 필터된 결과만 emit 한다`() = runTest {
        val rows = queryFlow(db) {
            Events.selectAll().where { Events.region eq "kr" }
        }.toList()

        rows shouldHaveSize 1
        rows.all { it[Events.region] == "kr" }.shouldBeTrue()
    }

    @Test
    fun `queryFlow 는 take 로 일부만 소비할 수 있다`() = runTest {
        val rows = queryFlow(db) {
            Events.selectAll().orderBy(Events.eventId to SortOrder.ASC)
        }.take(2).toList()

        rows shouldHaveSize 2
        rows.map { it[Events.eventId] } shouldBeEqualTo listOf(1L, 2L)
    }

    @Test
    fun `queryFlow 는 커스텀 디스패처에서도 결과를 반환한다`() = runTest {
        val rows = queryFlow(db, Dispatchers.Default) {
            Events.selectAll().orderBy(Events.eventId to SortOrder.ASC)
        }.toList()

        rows shouldHaveSize 3
    }

    @Test
    fun `suspendTransaction 은 삽입 후 카운트가 즉시 반영된다`() = runTest {
        val beforeCount = suspendTransaction(db) { Events.selectAll().count() }

        transaction(db) {
            Events.insert {
                it[eventId] = 99L
                it[userId] = 9999L
                it[eventType] = "VIEW"
                it[region] = "jp"
                it[amount] = null
                it[occurredAt] = OCCURRED_AT
            }
        }

        val afterCount = suspendTransaction(db) { Events.selectAll().count() }
        afterCount shouldBeEqualTo beforeCount + 1L
    }

    @Test
    fun `queryFlow 는 정렬 순서를 보장한다`() = runTest {
        val ascending = queryFlow(db) {
            Events.selectAll().orderBy(Events.eventId to SortOrder.ASC)
        }.toList().map { it[Events.eventId] }

        val descending = queryFlow(db) {
            Events.selectAll().orderBy(Events.eventId to SortOrder.DESC)
        }.toList().map { it[Events.eventId] }

        ascending shouldBeEqualTo listOf(1L, 2L, 3L)
        descending shouldBeEqualTo listOf(3L, 2L, 1L)
        (ascending == descending).shouldBeFalse()
    }
}
