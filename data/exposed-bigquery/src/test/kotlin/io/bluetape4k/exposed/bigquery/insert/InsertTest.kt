package io.bluetape4k.exposed.bigquery.insert

import io.bluetape4k.exposed.bigquery.AbstractBigQueryTest
import io.bluetape4k.exposed.bigquery.domain.Events
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class InsertTest : AbstractBigQueryTest() {

    companion object : KLogging()

    private data class EventFixture(
        val eventId: Long,
        val userId: Long,
        val eventType: String,
        val region: String,
        val amount: BigDecimal?,
        val occurredAt: Instant = Instant.parse("2024-01-01T00:00:00Z"),
    )

    private val fixtures = listOf(
        EventFixture(1L, 100L, "PURCHASE", "kr", BigDecimal("9900.00")),
        EventFixture(2L, 100L, "VIEW", "kr", null),
        EventFixture(3L, 200L, "PURCHASE", "us", BigDecimal("49.99")),
        EventFixture(4L, 200L, "CLICK", "us", null),
        EventFixture(5L, 300L, "PURCHASE", "eu", BigDecimal("19.90")),
    )

    private fun insertFixtures() {
        fixtures.forEach { f ->
            Events.execInsert {
                it[eventId] = f.eventId
                it[userId] = f.userId
                it[eventType] = f.eventType
                it[region] = f.region
                it[amount] = f.amount
                it[occurredAt] = f.occurredAt
            }
        }
    }

    @Test
    fun `execInsert - 이벤트 대량 적재`() {
        withEventsData {
            insertFixtures()

            val rows = Events.selectAll().withBigQuery().toList()
            rows.size shouldBeEqualTo fixtures.size
        }
    }

    @Test
    fun `execInsert - 리전별 집계 검증`() {
        withEventsData {
            insertFixtures()

            val krRows = Events.selectAll()
                .where { Events.region eq "kr" }
                .withBigQuery()
                .toList()
            krRows.size shouldBeEqualTo 2
            krRows.all { it[Events.region] == "kr" }.shouldBeTrue()

            val purchaseRows = Events.selectAll()
                .where { Events.eventType eq "PURCHASE" }
                .withBigQuery()
                .toList()
            purchaseRows.size shouldBeEqualTo 3
        }
    }
}
