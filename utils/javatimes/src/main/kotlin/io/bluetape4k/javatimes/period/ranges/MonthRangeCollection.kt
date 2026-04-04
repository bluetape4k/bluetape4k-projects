package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.todayZonedDateTime
import io.bluetape4k.javatimes.zonedDateTimeOf
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * 월 단위의 범위를 나타내는 [MonthRange]의 컬렉션
 *
 * ```kotlin
 * val months = MonthRangeCollection(2024, 1, 6) // 2024년 1월부터 6개월
 * months.months().size // 6
 * ```
 */
open class MonthRangeCollection(
    startTime: ZonedDateTime = todayZonedDateTime(),
    monthCount: Int = 1,
    calendar: ITimeCalendar = TimeCalendar.Default,
): MonthTimeRange(startTime, monthCount, calendar) {

    /**
     * [year], [monthOfYear] 기준의 컬렉션을 생성합니다. 기본 zone은 UTC입니다.
     */
    constructor(year: Int, monthOfYear: Int, monthCount: Int = 1, calendar: ITimeCalendar = TimeCalendar.Default)
            : this(zonedDateTimeOf(year, monthOfYear), monthCount, calendar)

    constructor(
        year: Int,
        monthOfYear: Int,
        monthCount: Int = 1,
        zoneId: ZoneId,
        calendar: ITimeCalendar = TimeCalendar.Default,
    ): this(zonedDateTimeOf(year, monthOfYear, zoneId = zoneId), monthCount, calendar)

    /**
     * 컬렉션에 포함된 월 범위를 [Sequence]로 반환합니다.
     *
     * ```kotlin
     * val months = MonthRangeCollection(2024, 1, 3) // 1월, 2월, 3월
     * months.monthSequence().toList().size // 3
     * ```
     */
    fun monthSequence(): Sequence<MonthRange> = monthRanges(startDayOfStart, monthCount, calendar)

    /**
     * 컬렉션에 포함된 월 범위를 [List]로 반환합니다.
     *
     * ```kotlin
     * val months = MonthRangeCollection(2024, 1, 4)
     * months.months().map { it.monthOfYear } // [1, 2, 3, 4]
     * ```
     */
    fun months(): List<MonthRange> = monthSequence().toList()
}
