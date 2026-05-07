package io.bluetape4k.exposed.core

import io.bluetape4k.exposed.tests.AbstractExposedTest
import io.bluetape4k.exposed.tests.TestDB
import io.bluetape4k.exposed.tests.withTables
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterOrEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldNotBeNull
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.date
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.javatime.timestamp
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import io.bluetape4k.assertions.assertFailsWith

/**
 * [ResultRow] 확장 함수 중 시간 타입 변환 함수에 대한 단위 테스트.
 *
 * `getDate`, `getTimestamp`, `getInstant`, `getLocalDate`, `getLocalDateTime` 등의
 * 변환 함수의 정상 동작과 null/예외 처리를 검증한다.
 */
class ResultRowTimeExtensionsTest: AbstractExposedTest() {

    companion object: KLogging()

    object TimeTable: Table("result_row_time_test") {
        val localDateCol = date("local_date_col")
        val localDateTimeCol = datetime("local_datetime_col")
        val instantCol = timestamp("instant_col")
        val nullableInstant = timestamp("nullable_instant_col").nullable()
        val nullableLocalDate = date("nullable_local_date_col").nullable()
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getLocalDate는 LocalDate 값을 올바르게 반환한다`(testDB: TestDB) {
        val today = LocalDate.of(2024, 6, 15)
        withTables(testDB, TimeTable) {
            TimeTable.insert {
                it[localDateCol] = today
                it[localDateTimeCol] = today.atStartOfDay()
                it[instantCol] = Instant.parse("2024-06-15T00:00:00Z")
                it[nullableInstant] = null
                it[nullableLocalDate] = null
            }

            val row = TimeTable.selectAll().single()
            row.getLocalDate(TimeTable.localDateCol) shouldBeEqualTo today
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getLocalDateTime는 LocalDateTime 값을 올바르게 반환한다`(testDB: TestDB) {
        val now = LocalDateTime.of(2024, 6, 15, 12, 30, 45)
        withTables(testDB, TimeTable) {
            TimeTable.insert {
                it[localDateCol] = now.toLocalDate()
                it[localDateTimeCol] = now
                it[instantCol] = now.toInstant(java.time.ZoneOffset.UTC)
                it[nullableInstant] = null
                it[nullableLocalDate] = null
            }

            val row = TimeTable.selectAll().single()
            row.getLocalDateTime(TimeTable.localDateTimeCol) shouldBeEqualTo now
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getInstant는 Instant 값을 올바르게 반환한다`(testDB: TestDB) {
        val instant = Instant.parse("2024-06-15T10:20:30Z")
        withTables(testDB, TimeTable) {
            TimeTable.insert {
                it[localDateCol] = LocalDate.of(2024, 6, 15)
                it[localDateTimeCol] = LocalDateTime.of(2024, 6, 15, 10, 20, 30)
                it[instantCol] = instant
                it[nullableInstant] = null
                it[nullableLocalDate] = null
            }

            val row = TimeTable.selectAll().single()
            row.getInstant(TimeTable.instantCol).shouldNotBeNull()
            row.getInstant(TimeTable.instantCol).epochSecond shouldBeGreaterOrEqualTo 0L
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getInstantOrNull은 nullable 컬럼에서 null을 반환한다`(testDB: TestDB) {
        withTables(testDB, TimeTable) {
            TimeTable.insert {
                it[localDateCol] = LocalDate.of(2024, 1, 1)
                it[localDateTimeCol] = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
                it[instantCol] = Instant.parse("2024-01-01T00:00:00Z")
                it[nullableInstant] = null
                it[nullableLocalDate] = null
            }

            val row = TimeTable.selectAll().single()
            row.getInstantOrNull(TimeTable.nullableInstant).shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getLocalDateOrNull은 nullable 컬럼에서 null을 반환한다`(testDB: TestDB) {
        withTables(testDB, TimeTable) {
            TimeTable.insert {
                it[localDateCol] = LocalDate.of(2024, 1, 1)
                it[localDateTimeCol] = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
                it[instantCol] = Instant.parse("2024-01-01T00:00:00Z")
                it[nullableInstant] = null
                it[nullableLocalDate] = null
            }

            val row = TimeTable.selectAll().single()
            row.getLocalDateOrNull(TimeTable.nullableLocalDate).shouldBeNull()
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getInstant는 null 컬럼에서 예외를 던진다`(testDB: TestDB) {
        withTables(testDB, TimeTable) {
            TimeTable.insert {
                it[localDateCol] = LocalDate.of(2024, 1, 1)
                it[localDateTimeCol] = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
                it[instantCol] = Instant.parse("2024-01-01T00:00:00Z")
                it[nullableInstant] = null
                it[nullableLocalDate] = null
            }

            val row = TimeTable.selectAll().single()
            assertFailsWith<IllegalStateException> {
                row.getInstant(TimeTable.nullableInstant)
            }
        }
    }

    @ParameterizedTest
    @MethodSource(ENABLE_DIALECTS_METHOD)
    fun `getLocalDate는 null 컬럼에서 예외를 던진다`(testDB: TestDB) {
        withTables(testDB, TimeTable) {
            TimeTable.insert {
                it[localDateCol] = LocalDate.of(2024, 1, 1)
                it[localDateTimeCol] = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
                it[instantCol] = Instant.parse("2024-01-01T00:00:00Z")
                it[nullableInstant] = null
                it[nullableLocalDate] = null
            }

            val row = TimeTable.selectAll().single()
            assertFailsWith<IllegalStateException> {
                row.getLocalDate(TimeTable.nullableLocalDate)
            }
        }
    }
}
