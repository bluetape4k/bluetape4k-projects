package io.bluetape4k.exposed.postgresql.tsrange

import io.bluetape4k.logging.KLogging
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.core.vendors.currentDialect
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

/**
 * PostgreSQL `TSTZRANGE` 컬럼 타입.
 *
 * PostgreSQL에서는 네이티브 `TSTZRANGE` 타입을 사용하고,
 * 그 외 DB(H2 등)에서는 `VARCHAR(120)`로 fallback한다.
 *
 * range literal 포맷: `[2024-01-01T00:00:00Z,2024-12-31T23:59:59Z)`
 * - `[` = lowerInclusive=true, `(` = lowerInclusive=false
 * - `]` = upperInclusive=true, `)` = upperInclusive=false
 *
 * ```kotlin
 * object EventTable: LongIdTable("events") {
 *     val period = tstzRange("period")
 * }
 * val range = TimestampRange(
 *     Instant.parse("2024-01-01T00:00:00Z"),
 *     Instant.parse("2024-12-31T23:59:59Z")
 * )
 * val id = EventTable.insertAndGetId { it[period] = range }
 * val row = EventTable.selectAll().where { EventTable.id eq id }.single()
 * // row[EventTable.period].start == Instant.parse("2024-01-01T00:00:00Z")
 * ```
 */
class TstzRangeColumnType: ColumnType<TimestampRange>() {

    companion object: KLogging() {
        /**
         * PostgreSQL JDBC 드라이버가 반환하는 timestamp+tz 포맷 파서.
         *
         * `"2024-01-01 00:00:00+00"` 또는 `"2024-01-01 00:00:00.123456+00"` 등을 파싱한다.
         */
        private val PG_TIMESTAMP_FORMATTER: DateTimeFormatter = DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd HH:mm:ss")
            .optionalStart()
            .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
            .optionalEnd()
            .appendPattern("X")
            .toFormatter()
    }

    /**
     * DB SQL 타입을 반환한다.
     *
     * @return PostgreSQL이면 `"TSTZRANGE"`, 그 외는 `"VARCHAR(120)"`
     */
    override fun sqlType(): String = when (currentDialect) {
        is PostgreSQLDialect -> "TSTZRANGE"
        else -> "VARCHAR(120)"
    }

    /**
     * [TimestampRange] 값을 DB에 저장할 형태로 변환한다.
     *
     * @param value 저장할 [TimestampRange] 객체
     * @return range literal 문자열 (예: `"[2024-01-01T00:00:00Z,2024-12-31T23:59:59Z)"`)
     */
    override fun notNullValueToDB(value: TimestampRange): Any {
        return toRangeLiteral(value)
    }

    /**
     * DB에서 읽은 값을 [TimestampRange]로 변환한다.
     *
     * PostgreSQL JDBC 드라이버의 `PGobject.value` 또는 H2의 VARCHAR 문자열을 파싱한다.
     *
     * @param value DB에서 읽은 값
     * @return 파싱된 [TimestampRange] 객체
     */
    override fun valueFromDB(value: Any): TimestampRange {
        return parseRangeLiteral(value.toString())
    }

    /**
     * PostgreSQL에서 `VARCHAR`를 `TSTZRANGE` 컬럼에 바인딩할 때 타입 캐스트를 추가한다.
     *
     * @return PostgreSQL이면 `"?::tstzrange"`, 그 외는 `"?"`
     */
    override fun parameterMarker(value: TimestampRange?): String = when (currentDialect) {
        is PostgreSQLDialect -> "?::tstzrange"
        else -> "?"
    }

    /**
     * [TimestampRange]를 range literal 문자열로 변환한다.
     */
    private fun toRangeLiteral(range: TimestampRange): String {
        val lower = if (range.lowerInclusive) "[" else "("
        val upper = if (range.upperInclusive) "]" else ")"
        return "$lower${range.start},${range.end}$upper"
    }

    /**
     * range literal 문자열을 [TimestampRange]로 파싱한다.
     *
     * ISO-8601 포맷(`2024-01-01T00:00:00Z`)과
     * PostgreSQL JDBC 드라이버 포맷(`"2024-01-01 00:00:00+00"`, `"2024-01-01 00:00:00.123456+00"`)을 모두 지원한다.
     */
    private fun parseRangeLiteral(literal: String): TimestampRange {
        val trimmed = literal.trim()
        val lowerInclusive = trimmed.startsWith("[")
        val upperInclusive = trimmed.endsWith("]")

        // 경계 문자 제거
        val inner = trimmed.substring(1, trimmed.length - 1)

        // PostgreSQL 드라이버는 따옴표로 감쌀 수 있다: ["2024-01-01 00:00:00+00","2024-12-31 23:59:59+00")
        // 따옴표 제거 후 '","' 또는 ',' 로 분리
        val cleaned = inner.replace("\"", "")
        val commaIdx = findSplitComma(cleaned)
        val startStr = cleaned.substring(0, commaIdx).trim()
        val endStr = cleaned.substring(commaIdx + 1).trim()

        val start = parseInstant(startStr)
        val end = parseInstant(endStr)

        return TimestampRange(start, end, lowerInclusive, upperInclusive)
    }

    /**
     * 두 timestamp 사이의 구분 콤마 위치를 찾는다.
     *
     * ISO-8601의 'T' 뒤에 콤마가 없으므로, 단순히 첫 번째 콤마를 찾으면 된다.
     * 단, PostgreSQL 포맷(`2024-01-01 00:00:00+00,2024-12-31 23:59:59+00`)도 지원.
     */
    private fun findSplitComma(s: String): Int {
        // ISO-8601: "2024-01-01T00:00:00Z,2024-12-31T23:59:59Z"
        // PG JDBC:  "2024-01-01 00:00:00+00,2024-12-31 23:59:59+00"
        // 첫 번째 콤마가 아닌 timestamp 구분 콤마를 찾기 위해 뒤에서부터 'T' 또는 '+' 이후의 콤마를 사용
        // 가장 단순한 방법: 첫 번째 콤마 이후에 숫자(날짜 시작)가 나오는 콤마를 찾는다
        for (i in s.indices) {
            if (s[i] == ',' && i + 1 < s.length && (s[i + 1].isDigit() || s[i + 1] == ' ')) {
                return i
            }
        }
        // fallback: 첫 번째 콤마
        return s.indexOf(',')
    }

    /**
     * timestamp 문자열을 [Instant]로 파싱한다.
     *
     * ISO-8601(`2024-01-01T00:00:00Z`)과 PostgreSQL JDBC(`2024-01-01 00:00:00+00`) 모두 지원.
     */
    private fun parseInstant(s: String): Instant {
        return try {
            Instant.parse(s)
        } catch (e: Exception) {
            // PostgreSQL JDBC 포맷 시도
            val temporal = PG_TIMESTAMP_FORMATTER.parse(s)
            LocalDateTime.from(temporal).toInstant(ZoneOffset.from(temporal))
        }
    }
}
