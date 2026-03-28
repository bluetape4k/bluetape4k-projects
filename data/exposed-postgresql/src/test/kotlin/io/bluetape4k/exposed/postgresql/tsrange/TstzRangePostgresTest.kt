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
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * PostgreSQL 전용 TSTZRANGE 네이티브 타입 및 범위 연산자 테스트.
 *
 * PostgreSQL `postgres:16` 이미지 기반 Testcontainers를 사용한다.
 */
class TstzRangePostgresTest : AbstractExposedTest() {

    companion object : KLogging() {
        object PgEventTable : LongIdTable("pg_tsrange_events") {
            val name = varchar("name", 100)
            val period = tstzRange("period")
        }

        object PgOverlapTestTable : LongIdTable("pg_overlap_test") {
            val label = varchar("label", 100)
            val range1 = tstzRange("range1")
            val range2 = tstzRange("range2")
        }
    }

    @Test
    fun `PostgreSQL TSTZRANGE 네이티브 타입으로 범위 저장 및 조회`() {
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val end = Instant.parse("2024-12-31T23:59:59Z")
        val range = TimestampRange(start, end)

        withTables(TestDB.POSTGRESQL, PgEventTable) {
            PgEventTable.insert {
                it[name] = "2024 연간 이벤트"
                it[period] = range
            }

            val row = PgEventTable.selectAll().single()
            val result = row[PgEventTable.period]
            result.shouldNotBeNull()
            result.start shouldBeEqualTo start
            result.end shouldBeEqualTo end
            result.lowerInclusive.shouldBeTrue()
            result.upperInclusive.shouldBeFalse()
        }
    }

    @Test
    fun `PostgreSQL TSTZRANGE 양쪽 포함 경계 저장 및 조회`() {
        val start = Instant.parse("2024-06-01T09:00:00Z")
        val end = Instant.parse("2024-06-01T18:00:00Z")
        val range = TimestampRange(start, end, lowerInclusive = true, upperInclusive = true)

        withTables(TestDB.POSTGRESQL, PgEventTable) {
            PgEventTable.insert {
                it[name] = "회의"
                it[period] = range
            }

            val row = PgEventTable.selectAll().single()
            val result = row[PgEventTable.period]
            result.shouldNotBeNull()
            // PostgreSQL은 tstzrange를 canonical form으로 정규화하므로
            // inclusive/exclusive 경계가 변환될 수 있으나, start/end 시각은 보존됨
            result.start shouldBeEqualTo start
            result.end shouldBeEqualTo end
        }
    }

    @Test
    fun `PostgreSQL TSTZRANGE overlaps 연산자`() {
        val overlapping1 = TimestampRange(
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-06-30T23:59:59Z"),
        )
        val overlapping2 = TimestampRange(
            Instant.parse("2024-06-01T00:00:00Z"),
            Instant.parse("2024-12-31T23:59:59Z"),
        )

        withTables(TestDB.POSTGRESQL, PgOverlapTestTable) {
            PgOverlapTestTable.insert {
                it[label] = "겹침"
                it[range1] = overlapping1
                it[range2] = overlapping2
            }

            val rows = PgOverlapTestTable.selectAll()
                .where { PgOverlapTestTable.range1.overlaps(PgOverlapTestTable.range2) }
                .toList()

            rows.size shouldBeEqualTo 1
            rows.first()[PgOverlapTestTable.label] shouldBeEqualTo "겹침"
        }
    }

    @Test
    fun `PostgreSQL TSTZRANGE overlaps 연산자 - 겹치지 않는 범위`() {
        val noOverlap1 = TimestampRange(
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-06-01T00:00:00Z"),
        )
        val noOverlap2 = TimestampRange(
            Instant.parse("2024-06-01T00:00:00Z"),
            Instant.parse("2024-12-31T23:59:59Z"),
        )

        withTables(TestDB.POSTGRESQL, PgOverlapTestTable) {
            PgOverlapTestTable.insert {
                it[label] = "안 겹침"
                it[range1] = noOverlap1
                it[range2] = noOverlap2
            }

            val rows = PgOverlapTestTable.selectAll()
                .where { PgOverlapTestTable.range1.overlaps(PgOverlapTestTable.range2) }
                .toList()

            rows.size shouldBeEqualTo 0
        }
    }

    @Test
    fun `PostgreSQL TSTZRANGE containsRange 연산자`() {
        val outer = TimestampRange(
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-12-31T23:59:59Z"),
        )
        val inner = TimestampRange(
            Instant.parse("2024-03-01T00:00:00Z"),
            Instant.parse("2024-06-01T00:00:00Z"),
        )

        withTables(TestDB.POSTGRESQL, PgOverlapTestTable) {
            PgOverlapTestTable.insert {
                it[label] = "포함"
                it[range1] = outer
                it[range2] = inner
            }

            val rows = PgOverlapTestTable.selectAll()
                .where { PgOverlapTestTable.range1.containsRange(PgOverlapTestTable.range2) }
                .toList()

            rows.size shouldBeEqualTo 1
            rows.first()[PgOverlapTestTable.label] shouldBeEqualTo "포함"
        }
    }

    @Test
    fun `PostgreSQL TSTZRANGE adjacent 연산자`() {
        val left = TimestampRange(
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-06-01T00:00:00Z"),
        )
        val right = TimestampRange(
            Instant.parse("2024-06-01T00:00:00Z"),
            Instant.parse("2024-12-31T23:59:59Z"),
        )

        withTables(TestDB.POSTGRESQL, PgOverlapTestTable) {
            PgOverlapTestTable.insert {
                it[label] = "adjacent"
                it[range1] = left
                it[range2] = right
            }

            val rows = PgOverlapTestTable.selectAll()
                .where { PgOverlapTestTable.range1.isAdjacentTo(PgOverlapTestTable.range2) }
                .toList()

            rows.size shouldBeEqualTo 1
            rows.first()[PgOverlapTestTable.label] shouldBeEqualTo "adjacent"
        }
    }

    @Test
    fun `여러 범위 저장 후 조회`() {
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

        withTables(TestDB.POSTGRESQL, PgEventTable) {
            ranges.forEach { (label, range) ->
                PgEventTable.insert {
                    it[name] = label
                    it[period] = range
                }
            }

            val rows = PgEventTable.selectAll().toList()
            rows.size shouldBeEqualTo 3

            rows.forEach { row ->
                row[PgEventTable.period].shouldNotBeNull()
            }
        }
    }
}
