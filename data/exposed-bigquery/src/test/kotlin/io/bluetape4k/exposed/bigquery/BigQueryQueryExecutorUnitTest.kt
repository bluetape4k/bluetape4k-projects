package io.bluetape4k.exposed.bigquery

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeInstanceOf
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * [BigQueryResultRow] 단위 테스트 — 에뮬레이터 없이 타입 변환 로직만 검증.
 */
class BigQueryQueryExecutorUnitTest {

    @Test
    fun `BigQueryResultRow - 키가 없으면 null을 반환한다`() {
        val row = BigQueryResultRow(mapOf("region" to "kr"))

        row["nonexistent"].shouldBeNull()
    }

    @Test
    fun `BigQueryResultRow - toString은 내부 맵 표현을 반환한다`() {
        val row = BigQueryResultRow(mapOf("region" to "kr"))
        val str = row.toString()

        str.shouldNotBeNull()
        (str.contains("region") && str.contains("kr")).shouldBeEqualTo(true)
    }

    @Test
    fun `BigQueryResultRow - 키는 대소문자를 구분하지 않는다`() {
        val row = BigQueryResultRow(mapOf("Region" to "kr"))

        row["region"].shouldBeEqualTo("kr")
        row["REGION"].shouldBeEqualTo("kr")
        row["Region"].shouldBeEqualTo("kr")
    }

    @Test
    fun `BigQueryResultRow - null 원시값은 null을 반환한다`() {
        val row = BigQueryResultRow(mapOf("region" to null))

        row["region"].shouldBeNull()
    }

    @Test
    fun `BigQueryQueryException - 기본 생성자는 메시지를 보존한다`() {
        val msg = "BigQuery 쿼리 오류: 테이블 없음"
        val ex = BigQueryQueryException(msg)

        ex.message.shouldBeEqualTo(msg)
        ex.cause.shouldBeNull()
    }

    @Test
    fun `BigQueryQueryException - cause 포함 생성자는 원인을 보존한다`() {
        val cause = RuntimeException("원본 오류")
        val ex = BigQueryQueryException("래핑된 오류", cause)

        ex.message.shouldBeEqualTo("래핑된 오류")
        ex.cause.shouldBeEqualTo(cause)
    }

    @Test
    fun `BigQueryQueryException은 RuntimeException 하위 타입이다`() {
        val ex = BigQueryQueryException("테스트")

        ex.shouldBeInstanceOf<RuntimeException>()
    }
}
