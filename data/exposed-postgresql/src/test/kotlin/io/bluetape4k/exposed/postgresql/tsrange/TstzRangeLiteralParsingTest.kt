package io.bluetape4k.exposed.postgresql.tsrange

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class TstzRangeLiteralParsingTest {

    private val columnType = TstzRangeColumnType()

    @Test
    fun `PostgreSQL JDBC fractional seconds literal 을 Instant 로 파싱한다`() {
        val literal = "[\"2024-01-01 00:00:00.123456+00\",\"2024-01-01 01:02:03.987654+00\")"

        val result = columnType.valueFromDB(literal)

        result.start shouldBeEqualTo Instant.parse("2024-01-01T00:00:00.123456Z")
        result.end shouldBeEqualTo Instant.parse("2024-01-01T01:02:03.987654Z")
        result.lowerInclusive.shouldBeTrue()
        result.upperInclusive shouldBeEqualTo false
    }

    @Test
    fun `하한 미포함 경계 literal 을 파싱한다`() {
        // (start, end) 형태 — 양쪽 exclusive
        val literal = "(2024-03-01T00:00:00Z,2024-03-31T23:59:59Z)"

        val result = columnType.valueFromDB(literal)

        result.start shouldBeEqualTo Instant.parse("2024-03-01T00:00:00Z")
        result.end shouldBeEqualTo Instant.parse("2024-03-31T23:59:59Z")
        result.lowerInclusive.shouldBeFalse()
        result.upperInclusive.shouldBeFalse()
    }

    @Test
    fun `양쪽 포함 경계 literal 을 파싱한다`() {
        // [start, end] 형태 — 양쪽 inclusive
        val literal = "[2024-06-01T09:00:00Z,2024-06-01T18:00:00Z]"

        val result = columnType.valueFromDB(literal)

        result.lowerInclusive.shouldBeTrue()
        result.upperInclusive.shouldBeTrue()
    }

    @Test
    fun `ISO-8601 literal 을 파싱한다`() {
        val literal = "[2024-01-01T00:00:00Z,2024-12-31T23:59:59Z)"

        val result = columnType.valueFromDB(literal)

        result.start shouldBeEqualTo Instant.parse("2024-01-01T00:00:00Z")
        result.end shouldBeEqualTo Instant.parse("2024-12-31T23:59:59Z")
        result.lowerInclusive.shouldBeTrue()
        result.upperInclusive.shouldBeFalse()
    }

    @Test
    fun `빈 문자열 literal 은 IllegalArgumentException 을 던진다`() {
        assertThrows<IllegalArgumentException> {
            columnType.valueFromDB("")
        }
    }

    @Test
    fun `공백만 포함된 literal 은 IllegalArgumentException 을 던진다`() {
        assertThrows<IllegalArgumentException> {
            columnType.valueFromDB("   ")
        }
    }

    @Test
    fun `지원하지 않는 timestamp 포맷은 IllegalArgumentException 을 던진다`() {
        // 완전히 잘못된 timestamp 포맷
        assertThrows<IllegalArgumentException> {
            columnType.valueFromDB("[NOT_A_DATE,ALSO_NOT_A_DATE)")
        }
    }
}
