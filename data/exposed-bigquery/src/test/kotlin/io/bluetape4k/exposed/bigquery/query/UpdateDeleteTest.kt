package io.bluetape4k.exposed.bigquery.query

import io.bluetape4k.exposed.bigquery.AbstractBigQueryTest
import io.bluetape4k.exposed.bigquery.domain.Events
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeEmpty
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

/**
 * [io.bluetape4k.exposed.bigquery.BigQueryContext.execUpdate],
 * [io.bluetape4k.exposed.bigquery.BigQueryContext.execDelete],
 * [io.bluetape4k.exposed.bigquery.BigQueryContext.execDeleteAll] 통합 테스트.
 */
class UpdateDeleteTest: AbstractBigQueryTest() {

    companion object: KLogging()

    private fun insertEvent(id: Long, region: String, eventType: String = "PURCHASE") {
        Events.execInsert {
            it[eventId] = id
            it[userId] = id * 10
            it[Events.eventType] = eventType
            it[Events.region] = region
            it[amount] = BigDecimal("10.00")
            it[occurredAt] = Instant.parse("2024-01-01T00:00:00Z")
        }
    }

    @Test
    fun `execUpdate - 조건에 맞는 행만 갱신된다`() {
        withEventsData {
            insertEvent(1L, "kr")
            insertEvent(2L, "us")

            Events.execUpdate(Events.region eq "kr") { it[eventType] = "UPDATED_KR" }

            val updated = Events.selectAll()
                .where { Events.eventId eq 1L }
                .withBigQuery()
                .singleOrNull()
            updated?.get(Events.eventType) shouldBeEqualTo "UPDATED_KR"

            val unchanged = Events.selectAll()
                .where { Events.eventId eq 2L }
                .withBigQuery()
                .singleOrNull()
            unchanged?.get(Events.eventType) shouldBeEqualTo "PURCHASE"
        }
    }

    @Test
    fun `execDelete - 조건에 맞는 행만 삭제된다`() {
        withEventsData {
            insertEvent(1L, "kr")
            insertEvent(2L, "us")

            Events.execDelete(Events.region eq "us")

            val rows = Events.selectAll().withBigQuery().toList()
            rows.size shouldBeEqualTo 1
            rows[0][Events.region] shouldBeEqualTo "kr"
        }
    }

    @Test
    fun `execDeleteAll - 테이블 전체 행이 삭제된다`() {
        withEventsData {
            insertEvent(1L, "kr")
            insertEvent(2L, "us")
            insertEvent(3L, "eu")

            with(bqContext) { Events.execDeleteAll() }

            val rows = Events.selectAll().withBigQuery().toList()
            rows.size shouldBeEqualTo 0
        }
    }

    @Test
    fun `execUpdateSuspending - 비동기 갱신 후 변경이 반영된다`() = runTest {
        withEventsDataSuspending {
            insertEvent(10L, "kr")

            with(bqContext) {
                Events.execUpdateSuspending(Events.eventId eq 10L) { it[eventType] = "ASYNC_UPDATE" }
            }

            val row = Events.selectAll()
                .where { Events.eventId eq 10L }
                .withBigQuery()
                .singleOrNull()
            row?.get(Events.eventType) shouldBeEqualTo "ASYNC_UPDATE"
        }
    }

    @Test
    fun `execDeleteSuspending - 비동기 삭제 후 행이 사라진다`() = runTest {
        withEventsDataSuspending {
            insertEvent(20L, "kr")

            with(bqContext) {
                Events.execDeleteSuspending(Events.eventId eq 20L)
            }

            val row = Events.selectAll()
                .where { Events.eventId eq 20L }
                .withBigQuery()
                .singleOrNull()
            row.shouldBeNull()
        }
    }

    @Test
    fun `singleOrNull - 결과가 없으면 null을 반환한다`() {
        withEventsData {
            val row = Events.selectAll()
                .where { Events.eventId eq 9999L }
                .withBigQuery()
                .singleOrNull()
            row.shouldBeNull()
        }
    }

    @Test
    fun `firstOrNull - 결과가 없으면 null을 반환한다`() {
        withEventsData {
            val row = Events.selectAll()
                .where { Events.eventId eq 9999L }
                .withBigQuery()
                .firstOrNull()
            row.shouldBeNull()
        }
    }
}
