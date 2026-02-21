package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.startOfYear
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * 여러 해 (years) 를 범위로 나타내는 클래스입니다.
 */
open class YearRangeCollection protected constructor(
    startTime: ZonedDateTime,
    yearCount: Int,
    calendar: ITimeCalendar = TimeCalendar.Default,
): YearTimeRange(startTime.startOfYear(), yearCount, calendar) {

    companion object {
        @JvmStatic
        operator fun invoke(
            year: Int,
            yearCount: Int,
            calendar: ITimeCalendar = TimeCalendar.Default,
        ): YearRangeCollection {
            // 연도 기반 생성은 UTC 기준으로 고정하여 환경 의존성을 줄입니다.
            return YearRangeCollection(startOfYear(year, ZoneOffset.UTC), yearCount, calendar)
        }

        @JvmStatic
        operator fun invoke(
            year: Int,
            yearCount: Int,
            zoneId: ZoneId,
            calendar: ITimeCalendar = TimeCalendar.Default,
        ): YearRangeCollection {
            return YearRangeCollection(startOfYear(year, zoneId), yearCount, calendar)
        }

        @JvmStatic
        operator fun invoke(
            time: ZonedDateTime,
            yearCount: Int,
            calendar: ITimeCalendar = TimeCalendar.Default,
        ): YearRangeCollection {
            return YearRangeCollection(time, yearCount, calendar)
        }
    }

    fun yearSequence(): Sequence<YearRange> = yearRanges(startYearOfStart, yearCount, calendar)

    fun years(): List<YearRange> = yearSequence().toList()
}
