package io.bluetape4k.exposed.bigquery

import org.amshove.kluent.shouldBeEqualTo
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.timestamp
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class BigQueryResultRowTest {

    private object ResultRowTable: Table("result_row_test") {
        val region = varchar("region", 32)
        val amount = decimal("amount", 10, 2).nullable()
        val occurredAt = timestamp("occurred_at")
        val eventType = varchar("event_type", 32).nullable()
    }

    @Test
    fun `컬럼 접근은 BigQuery 문자열 값을 Exposed 타입으로 변환한다`() {
        val row = BigQueryResultRow(
            mapOf(
                "region" to "kr",
                "amount" to "12.34",
                "occurred_at" to "1704067200.0",
            )
        )

        row[ResultRowTable.region] shouldBeEqualTo "kr"
        row[ResultRowTable.amount] shouldBeEqualTo BigDecimal("12.34")
        row[ResultRowTable.occurredAt] shouldBeEqualTo Instant.parse("2024-01-01T00:00:00Z")
    }

    @Test
    fun `원시 이름 접근은 대소문자를 무시한다`() {
        val row = BigQueryResultRow(mapOf("region" to "kr"))

        row["REGION"] shouldBeEqualTo "kr"
    }

    @Test
    fun `nullable 컬럼은 null sentinel 값을 null 로 변환한다`() {
        val row = BigQueryResultRow(
            mapOf(
                "AMOUNT" to "NULL",
                "event_type" to Any(),
            )
        )

        row[ResultRowTable.amount] shouldBeEqualTo null
        row[ResultRowTable.eventType] shouldBeEqualTo null
    }

    @Test
    fun `컬럼 접근은 입력 키 대소문자와 무관하게 동작한다`() {
        val row = BigQueryResultRow(
            mapOf(
                "REGION" to "kr",
                "OCCURRED_AT" to "1704067200.0",
            )
        )

        row[ResultRowTable.region] shouldBeEqualTo "kr"
        row[ResultRowTable.occurredAt] shouldBeEqualTo Instant.parse("2024-01-01T00:00:00Z")
    }
}
