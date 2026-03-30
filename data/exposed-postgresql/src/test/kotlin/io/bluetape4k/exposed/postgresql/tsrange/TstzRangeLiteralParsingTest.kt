package io.bluetape4k.exposed.postgresql.tsrange

import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
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
}
