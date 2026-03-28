package io.bluetape4k.exposed.postgresql.tsrange

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.time.Instant

/**
 * TstzRangeColumnType 통합 테스트.
 *
 * H2 및 PostgreSQL 다이얼렉트에서 TimestampRange 저장/조회를 검증한다.
 */
class TstzRangeColumnTypeTest : AbstractExposedTest() {

    companion object : KLogging() {
        object EventTable : LongIdTable("tsrange_events") {
            val name = varchar("name", 100)
            val period = tstzRange("period")
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `범위 저장 및 조회 - 기본 경계`(testDB: TestDB) {
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val end = Instant.parse("2024-12-31T23:59:59Z")
        val range = TimestampRange(start, end)

        withTables(testDB, EventTable) {
            EventTable.insert {
                it[name] = "2024 연간 이벤트"
                it[period] = range
            }

            val row = EventTable.selectAll().single()
            val result = row[EventTable.period]
            result.shouldNotBeNull()
            result.start shouldBeEqualTo start
            result.end shouldBeEqualTo end
            result.lowerInclusive.shouldBeTrue()
            result.upperInclusive.shouldBeFalse()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `범위 저장 및 조회 - 양쪽 포함 경계`(testDB: TestDB) {
        val start = Instant.parse("2024-06-01T09:00:00Z")
        val end = Instant.parse("2024-06-01T18:00:00Z")
        val range = TimestampRange(start, end, lowerInclusive = true, upperInclusive = true)

        withTables(testDB, EventTable) {
            EventTable.insert {
                it[name] = "회의"
                it[period] = range
            }

            val row = EventTable.selectAll().single()
            val result = row[EventTable.period]
            result.shouldNotBeNull()
            result.start shouldBeEqualTo start
            result.end shouldBeEqualTo end
            result.lowerInclusive.shouldBeTrue()
            result.upperInclusive.shouldBeTrue()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `범위 저장 및 조회 - 양쪽 미포함 경계`(testDB: TestDB) {
        val start = Instant.parse("2024-03-01T00:00:00Z")
        val end = Instant.parse("2024-03-31T23:59:59Z")
        val range = TimestampRange(start, end, lowerInclusive = false, upperInclusive = false)

        withTables(testDB, EventTable) {
            EventTable.insert {
                it[name] = "3월 이벤트"
                it[period] = range
            }

            val row = EventTable.selectAll().single()
            val result = row[EventTable.period]
            result.shouldNotBeNull()
            result.start shouldBeEqualTo start
            result.end shouldBeEqualTo end
            result.lowerInclusive.shouldBeFalse()
            result.upperInclusive.shouldBeFalse()
        }
    }

    @Test
    fun `TimestampRange contains - 범위 내 시각`() {
        val range = TimestampRange(
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-12-31T23:59:59Z"),
        )

        range.contains(Instant.parse("2024-06-15T12:00:00Z")).shouldBeTrue()
        range.contains(Instant.parse("2024-01-01T00:00:00Z")).shouldBeTrue()
        range.contains(Instant.parse("2024-12-31T23:59:59Z")).shouldBeFalse()
        range.contains(Instant.parse("2023-12-31T23:59:59Z")).shouldBeFalse()
        range.contains(Instant.parse("2025-01-01T00:00:00Z")).shouldBeFalse()
    }

    @Test
    fun `TimestampRange overlaps - 겹치는 범위`() {
        val range1 = TimestampRange(
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-06-30T23:59:59Z"),
        )
        val range2 = TimestampRange(
            Instant.parse("2024-06-01T00:00:00Z"),
            Instant.parse("2024-12-31T23:59:59Z"),
        )

        range1.overlaps(range2).shouldBeTrue()
        range2.overlaps(range1).shouldBeTrue()
    }

    @Test
    fun `TimestampRange overlaps - 겹치지 않는 범위`() {
        val range1 = TimestampRange(
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-06-01T00:00:00Z"),
        )
        val range2 = TimestampRange(
            Instant.parse("2024-06-01T00:00:00Z"),
            Instant.parse("2024-12-31T23:59:59Z"),
        )

        range1.overlaps(range2).shouldBeFalse()
    }

    @Test
    fun `TstzRangeColumnType 은 PostgreSQL JDBC literal 을 파싱한다`() {
        val columnType = TstzRangeColumnType()
        val literal = "[\"2024-01-01 00:00:00+00\",\"2024-12-31 23:59:59+00\")"

        val result = columnType.valueFromDB(literal)

        result.start shouldBeEqualTo Instant.parse("2024-01-01T00:00:00Z")
        result.end shouldBeEqualTo Instant.parse("2024-12-31T23:59:59Z")
        result.lowerInclusive.shouldBeTrue()
        result.upperInclusive.shouldBeFalse()
    }

    @Test
    fun `TstzRangeColumnType 은 dialect 에 따라 sqlType 과 parameterMarker 를 선택한다`() {
        val columnType = TstzRangeColumnType()
        val sample = TimestampRange(
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-01-02T00:00:00Z"),
        )

        withDb(TestDB.H2) {
            columnType.sqlType() shouldBeEqualTo "VARCHAR(120)"
            columnType.parameterMarker(sample) shouldBeEqualTo "?"
        }

        withDb(TestDB.POSTGRESQL) {
            columnType.sqlType() shouldBeEqualTo "TSTZRANGE"
            columnType.parameterMarker(sample) shouldBeEqualTo "?::tstzrange"
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `여러 범위 저장 및 조회`(testDB: TestDB) {
        val ranges = listOf(
            "Q1" to TimestampRange(
                Instant.parse("2024-01-01T00:00:00Z"),
                Instant.parse("2024-04-01T00:00:00Z"),
            ),
            "Q2" to TimestampRange(
                Instant.parse("2024-04-01T00:00:00Z"),
                Instant.parse("2024-07-01T00:00:00Z"),
            ),
            "Q3" to TimestampRange(
                Instant.parse("2024-07-01T00:00:00Z"),
                Instant.parse("2024-10-01T00:00:00Z"),
            ),
        )

        withTables(testDB, EventTable) {
            ranges.forEach { (n, range) ->
                EventTable.insert {
                    it[name] = n
                    it[period] = range
                }
            }

            val rows = EventTable.selectAll().toList()
            rows.size shouldBeEqualTo 3
        }
    }

    private fun withDb(testDB: TestDB, block: () -> Unit) {
        transaction(db = testDB.connect()) {
            block()
        }
    }
}
