package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.javatimes.period.ranges.DayOfWeekHourRange
import io.bluetape4k.javatimes.period.ranges.DayRangeInMonth
import io.bluetape4k.javatimes.period.ranges.HourRangeInDay
import io.bluetape4k.javatimes.period.ranges.MonthRangeInYear

/**
 * [CalendarPeriodCollector]가 수집할 때 필터링할 수 있는 인터페이스입니다.
 *
 * ```kotlin
 * val filter = CalendarPeriodCollectorFilter()
 * filter.addWorkingWeekdays()                      // 월~금 요일만 포함
 * filter.collectingHours.add(HourRangeInDay(9, 18)) // 9시~18시 근무시간만 포함
 * filter.addYears(2024)                            // 2024년만 필터링
 * ```
 */
open class CalendarPeriodCollectorFilter: CalendarVisitorFilter(), ICalendarPeriodCollectorFilter {

    override val collectingMonths: MutableList<MonthRangeInYear> = mutableListOf()

    override val collectingDays: MutableList<DayRangeInMonth> = mutableListOf()

    override val collectingHours: MutableList<HourRangeInDay> = mutableListOf()

    override val collectingDayOfWeekHours: MutableList<DayOfWeekHourRange> = mutableListOf()

    override fun clear() {
        super.clear()

        collectingMonths.clear()
        collectingDays.clear()
        collectingHours.clear()
        collectingDayOfWeekHours.clear()
    }


}
