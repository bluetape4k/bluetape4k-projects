package io.bluetape4k.exposed.duckdb.query

import io.bluetape4k.exposed.duckdb.AbstractDuckDBTest
import io.bluetape4k.exposed.duckdb.domain.Events
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class SelectTest : AbstractDuckDBTest() {

    companion object : KLogging() {
        private val OCCURRED_AT: Instant = Instant.parse("2024-01-01T00:00:00Z")
        private val DEFAULT_AMOUNT = BigDecimal("10.00")

        private val FIXTURES = listOf(
            Triple(1L, 100L, "kr"),
            Triple(2L, 101L, "kr"),
            Triple(3L, 200L, "us"),
            Triple(4L, 201L, "us"),
            Triple(5L, 300L, "eu"),
        )
    }

    @BeforeEach
    fun setUp() {
        withEventsTable {
            insertFixtures()
        }
    }

    private fun insertFixtures() {
        transaction(db) {
            FIXTURES.forEach { (id, userId, region) ->
                Events.insert {
                    it[eventId] = id
                    it[Events.userId] = userId
                    it[eventType] = "PURCHASE"
                    it[Events.region] = region
                    it[amount] = DEFAULT_AMOUNT
                    it[occurredAt] = OCCURRED_AT
                }
            }
        }
    }

    @Test
    fun `selectAll - 전체 이벤트 조회`() {
        val rows = transaction(db) {
            Events.selectAll().toList()
        }

        rows.shouldNotBeEmpty()
        rows.size shouldBeEqualTo 5
    }

    @Test
    fun `where - 리전 필터`() {
        val rows = transaction(db) {
            Events.selectAll().where { Events.region eq "kr" }.toList()
        }

        rows.size shouldBeEqualTo 2
        rows.all { it[Events.region] == "kr" } shouldBeEqualTo true
    }

    @Test
    fun `orderBy - userId 내림차순 정렬`() {
        val rows = transaction(db) {
            Events.selectAll()
                .orderBy(Events.userId to SortOrder.DESC)
                .limit(1)
                .toList()
        }

        rows.size shouldBeEqualTo 1
        rows[0][Events.userId] shouldBeEqualTo 300L
    }

    @Test
    fun `count - 전체 이벤트 수`() {
        val count = transaction(db) {
            Events.selectAll().count()
        }

        count shouldBeEqualTo 5L
    }

    @Test
    fun `count - 리전별 필터 후 카운트`() {
        val krCount = transaction(db) {
            Events.selectAll().where { Events.region eq "kr" }.count()
        }

        krCount shouldBeEqualTo 2L
    }

    @Test
    fun `sum - kr 리전 amount 합계`() {
        val total = transaction(db) {
            Events.selectAll()
                .where { Events.region eq "kr" }
                .sumOf { it[Events.amount] ?: BigDecimal.ZERO }
        }

        total.compareTo(BigDecimal("20.00")) shouldBeEqualTo 0
    }
}
