package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import java.time.ZonedDateTime

/**
 * 여러 해 (years) 를 범위로 나타내는 클래스입니다.
 */
open class YearRangeCollection protected constructor(
    year: Int,
    yearCount: Int,
    calendar: ITimeCalendar = TimeCalendar.Default,
): YearTimeRange(year, yearCount, calendar) {

    companion object {
        @JvmStatic
        operator fun invoke(
            year: Int,
            yearCount: Int,
            calendar: ITimeCalendar = TimeCalendar.Default,
        ): YearRangeCollection {
            return YearRangeCollection(year, yearCount, calendar)
        }

        @JvmStatic
        operator fun invoke(
            time: ZonedDateTime,
            yearCount: Int,
            calendar: ITimeCalendar = TimeCalendar.Default,
        ): YearRangeCollection {
            return invoke(time.year, yearCount, calendar)
        }
    }

    fun yearSequence(): Sequence<YearRange> = yearRanges(year, yearCount, calendar)

    fun years(): List<YearRange> = yearSequence().toFastList()
}
