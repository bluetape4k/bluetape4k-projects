package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.javatimes.period.ranges.DayOfWeekHourRange
import io.bluetape4k.javatimes.period.ranges.DayRangeInMonth
import io.bluetape4k.javatimes.period.ranges.HourRangeInDay
import io.bluetape4k.javatimes.period.ranges.MonthRangeInYear

interface ICalendarPeriodCollectorFilter: ICalendarVisitorFilter {

    val collectingMonths: MutableList<MonthRangeInYear>

    val collectingDays: MutableList<DayRangeInMonth>

    val collectingHours: MutableList<HourRangeInDay>

    val collectingDayOfWeekHours: MutableList<DayOfWeekHourRange>
}
