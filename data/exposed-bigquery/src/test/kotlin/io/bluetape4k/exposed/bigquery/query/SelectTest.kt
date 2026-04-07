package io.bluetape4k.exposed.bigquery.query

import io.bluetape4k.exposed.bigquery.AbstractBigQueryTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SelectTest: AbstractBigQueryTest() {

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
    fun `selectAll - 전체 이벤트 조회`() {
        withEventsData {
            insertFixtures()

            val response = runRawQuery("SELECT * FROM events")
            response.rows.shouldNotBeEmpty()
            response.rows.size shouldBeEqualTo 5
        }
    }

    @Test
    fun `where - 리전 필터`() {
        withEventsData {
            insertFixtures()

            val response = runRawQuery("SELECT COUNT(*) as cnt FROM events WHERE region = 'kr'")
            val count = response.rows.first().f.first().v.toString().toLong()
            count shouldBeEqualTo 2L
        }
    }

    @Test
    fun `orderBy - userId 내림차순 정렬`() {
        withEventsData {
            insertFixtures()

            val response = runRawQuery("SELECT user_id FROM events ORDER BY user_id DESC LIMIT 1")
            val userId = response.rows.first().f.first().v.toString().toLong()
            userId shouldBeEqualTo 300L
        }
    }

    @Test
    fun `count - 리전별 이벤트 수`() {
        withEventsData {
            insertFixtures()

            val response = runRawQuery(
                "SELECT region, COUNT(*) as cnt FROM events GROUP BY region ORDER BY region"
            )
            response.rows.shouldNotBeEmpty()

            val krRow = response.rows.find { it.f[0].v.toString() == "kr" }!!
            krRow.f[1].v.toString().toLong() shouldBeEqualTo 2L
        }
    }

    @Test
    fun `sum - 리전별 매출 합계`() {
        withEventsData {
            insertFixtures()

            val response = runRawQuery(
                "SELECT region, SUM(amount) as total FROM events GROUP BY region"
            )
            response.rows.shouldNotBeEmpty()

            val krRow = response.rows.find { it.f[0].v.toString() == "kr" }!!
            BigDecimal(krRow.f[1].v.toString()).compareTo(BigDecimal("20.00")) shouldBeEqualTo 0
        }
    }
}
