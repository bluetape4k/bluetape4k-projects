package io.bluetape4k.exposed.postgresql.tsrange

import io.bluetape4k.logging.KLogging
import java.io.Serializable
import java.time.Instant

/**
 * 시작/종료 [Instant]로 표현되는 시간 범위 값 객체.
 *
 * PostgreSQL `TSTZRANGE`에 대응하며, 경계의 inclusive/exclusive 여부를 지원한다.
 * 기본값은 `[start, end)` (하한 포함, 상한 미포함).
 *
 * @property start 범위의 시작 시각
 * @property end 범위의 종료 시각
 * @property lowerInclusive 하한 포함 여부 (`[` = true, `(` = false)
 * @property upperInclusive 상한 포함 여부 (`]` = true, `)` = false)
 */
data class TimestampRange(
    val start: Instant,
    val end: Instant,
    val lowerInclusive: Boolean = true,
    val upperInclusive: Boolean = false,
): Serializable {

    companion object: KLogging() {
        private const val serialVersionUID = 1L
    }

    /**
     * [instant]이 이 범위 안에 포함되는지 확인한다.
     *
     * ```kotlin
     * val range = TimestampRange(Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2024-12-31T23:59:59Z"))
     * val mid = Instant.parse("2024-06-15T12:00:00Z")
     * val before = Instant.parse("2023-12-31T23:59:59Z")
     * // range.contains(mid) == true
     * // range.contains(before) == false
     * ```
     *
     * @param instant 확인할 시각
     * @return 범위에 포함되면 `true`
     */
    fun contains(instant: Instant): Boolean {
        val afterStart = if (lowerInclusive) !instant.isBefore(start) else instant.isAfter(start)
        val beforeEnd = if (upperInclusive) !instant.isAfter(end) else instant.isBefore(end)
        return afterStart && beforeEnd
    }

    /**
     * 이 범위가 [other] 범위와 겹치는지 확인한다.
     *
     * ```kotlin
     * val r1 = TimestampRange(Instant.parse("2024-01-01T00:00:00Z"), Instant.parse("2024-06-30T23:59:59Z"))
     * val r2 = TimestampRange(Instant.parse("2024-04-01T00:00:00Z"), Instant.parse("2024-12-31T23:59:59Z"))
     * val r3 = TimestampRange(Instant.parse("2025-01-01T00:00:00Z"), Instant.parse("2025-12-31T23:59:59Z"))
     * // r1.overlaps(r2) == true
     * // r1.overlaps(r3) == false
     * ```
     *
     * @param other 비교할 다른 범위
     * @return 두 범위가 겹치면 `true`
     */
    fun overlaps(other: TimestampRange): Boolean {
        val thisEndBeforeOtherStart = if (!upperInclusive || !other.lowerInclusive)
            !end.isAfter(other.start) else end.isBefore(other.start)
        val otherEndBeforeThisStart = if (!other.upperInclusive || !lowerInclusive)
            !other.end.isAfter(start) else other.end.isBefore(start)
        return !(thisEndBeforeOtherStart || otherEndBeforeThisStart)
    }
}
