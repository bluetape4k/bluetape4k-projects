package io.bluetape4k.exposed.bigquery.query

import io.bluetape4k.exposed.bigquery.AbstractBigQueryTest
import io.bluetape4k.exposed.bigquery.BigQueryContext
import io.bluetape4k.exposed.bigquery.domain.Events
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEmpty
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.sum
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

/**
 * Exposed Query 객체를 [withBigQuery]에 전달하여 BigQuery 에뮬레이터에서 실행하는 테스트.
 *
 * H2(PostgreSQL 모드)로 SQL을 생성한 뒤 REST API로 에뮬레이터에 전달합니다.
 * 결과는 [io.bluetape4k.exposed.bigquery.BigQueryResultRow]로 반환되며 Column 참조로 타입 안전하게 접근합니다.
 */
class SelectQueryTest: AbstractBigQueryTest() {

    companion object: KLogging()

    private fun insertFixtures() {
        val fixtures = listOf(
            Triple(1L, 100L, "kr"),
            Triple(2L, 101L, "kr"),
            Triple(3L, 200L, "us"),
            Triple(4L, 201L, "us"),
            Triple(5L, 300L, "eu"),
        )
        fixtures.forEach { (id, userId, region) ->
            runRawQuery(
                """
                INSERT INTO events (event_id, user_id, event_type, region, amount, occurred_at)
                VALUES ($id, $userId, 'PURCHASE', '$region', 10.00, TIMESTAMP '2024-01-01 00:00:00 UTC')
                """
            )
        }
    }

    @Test
    fun `selectAll - withBigQuery로 전체 이벤트 조회`() {
        withEventsData {
            insertFixtures()

            val rows = Events.selectAll().withBigQuery().toList()

            rows.shouldNotBeEmpty()
            rows.size shouldBeEqualTo 5
        }
    }

    @Test
    fun `where - withBigQuery로 리전 필터 후 Column 접근`() {
        withEventsData {
            insertFixtures()

            val rows = Events.selectAll()
                .where { Events.region eq "kr" }
                .withBigQuery()
                .toList()

            rows.size shouldBeEqualTo 2
            rows.all { it[Events.region] == "kr" }.shouldBeTrue()
        }
    }

    @Test
    fun `orderBy - withBigQuery로 userId 내림차순 정렬`() {
        withEventsData {
            insertFixtures()

            val rows = Events.selectAll()
                .orderBy(Events.userId, SortOrder.DESC)
                .withBigQuery()
                .toList()

            rows.first()[Events.userId] shouldBeEqualTo 300L
        }
    }

    @Test
    fun `count - withBigQuery로 리전별 이벤트 수`() {
        withEventsData {
            insertFixtures()

            val countExpr = Events.eventId.count()
            val response = runQuery(
                Events.select(Events.region, countExpr)
                    .groupBy(Events.region)
                    .orderBy(Events.region)
            )
            response.rows.shouldNotBeEmpty()
            val krRow = response.rows.find { it.f[0].v.toString() == "kr" }!!
            krRow.f[1].v.toString().toLong() shouldBeEqualTo 2L
        }
    }

    @Test
    fun `sum - withBigQuery로 리전별 매출 합계 후 Column 접근`() {
        withEventsData {
            insertFixtures()

            val sumExpr = Events.amount.sum()
            val response = runQuery(
                Events.select(Events.region, sumExpr)
                    .groupBy(Events.region)
            )
            response.rows.shouldNotBeEmpty()
            val krRow = response.rows.find { it.f[0].v.toString() == "kr" }!!
            BigDecimal(krRow.f[1].v.toString()).compareTo(BigDecimal("20.00")) shouldBeEqualTo 0
        }
    }

    @Test
    fun `singleOrNull 과 firstOrNull - public 조회 헬퍼 검증`() {
        withEventsData {
            insertFixtures()

            val singleRow = Events.selectAll()
                .where { Events.eventId eq 1L }
                .withBigQuery()
                .singleOrNull()
            singleRow.shouldNotBeNull()
            singleRow[Events.region] shouldBeEqualTo "kr"

            val firstRow = Events.selectAll()
                .orderBy(Events.userId, SortOrder.DESC)
                .withBigQuery()
                .firstOrNull()
            firstRow.shouldNotBeNull()
            firstRow[Events.userId] shouldBeEqualTo 300L
        }
    }

    @Test
    fun `toListSuspending 과 toFlow - suspend 조회 및 타입 변환 검증`() = runTest {
        withEventsDataSuspending {
            runRawQuery(
                """
                INSERT INTO events (event_id, user_id, event_type, region, amount, occurred_at)
                VALUES (10, 999, 'PURCHASE', 'kr', 12.34, TIMESTAMP '2024-01-01 00:00:00 UTC')
                """
            )
            runRawQuery(
                """
                INSERT INTO events (event_id, user_id, event_type, region, amount, occurred_at)
                VALUES (11, 1000, 'VIEW', 'us', NULL, TIMESTAMP '2024-01-02 00:00:00 UTC')
                """
            )

            val rows = Events.selectAll()
                .orderBy(Events.eventId)
                .withBigQuery()
                .toListSuspending()

            rows.size shouldBeEqualTo 2
            rows[0][Events.amount] shouldBeEqualTo BigDecimal("12.34")
            rows[0][Events.occurredAt] shouldBeEqualTo Instant.parse("2024-01-01T00:00:00Z")
            rows[1][Events.amount] shouldBeEqualTo null

            val streamed = Events.selectAll()
                .orderBy(Events.eventId)
                .withBigQuery()
                .toFlow()
                .toList()

            streamed.size shouldBeEqualTo 2
            streamed[1][Events.occurredAt] shouldBeEqualTo Instant.parse("2024-01-02T00:00:00Z")
        }
    }

    @Test
    fun `create 팩토리 와 suspend DML - 권장 진입점 검증`() = runTest {
        val context = BigQueryContext.create(
            bigquery = bqContext.bigquery,
            projectId = bqContext.projectId,
            datasetId = bqContext.datasetId,
        )

        withEventsDataSuspending {
            with(context) {
                Events.execDeleteAll()
                Events.execInsertSuspending {
                    it[eventId] = 21L
                    it[userId] = 2100L
                    it[eventType] = "PURCHASE"
                    it[region] = "kr"
                    it[amount] = BigDecimal("77.77")
                    it[occurredAt] = Instant.parse("2024-02-01T00:00:00Z")
                }
                Events.execUpdateSuspending(Events.eventId eq 21L) {
                    it[eventType] = "UPDATED"
                }

                val updated = Events.selectAll()
                    .where { Events.eventId eq 21L }
                    .withBigQuery()
                    .toListSuspending()
                    .single()

                updated[Events.eventType] shouldBeEqualTo "UPDATED"

                Events.execDeleteSuspending(Events.eventId eq 21L)
                Events.selectAll()
                    .where { Events.eventId eq 21L }
                    .withBigQuery()
                    .singleOrNull() shouldBeEqualTo null
            }
        }
    }
}
