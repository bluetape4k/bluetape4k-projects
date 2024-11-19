package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinPeriodTime
import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.SeekDirection
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.TimeRange
import io.bluetape4k.javatimes.period.hasInsideWith
import io.bluetape4k.javatimes.period.hasPureInsideWith
import io.bluetape4k.javatimes.period.overlapWith
import io.bluetape4k.javatimes.period.ranges.DayRange
import io.bluetape4k.javatimes.period.ranges.HourRange
import io.bluetape4k.javatimes.period.ranges.MinuteRange
import io.bluetape4k.javatimes.period.ranges.MonthRange
import io.bluetape4k.javatimes.period.ranges.YearRange
import io.bluetape4k.javatimes.period.ranges.YearRangeCollection
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace


/**
 * 특정 기간에 대한 필터링 정보를 기반으로 기간들을 필터링 할 수 있도록 특정 기간을 탐색하는 Visitor입니다.
 *
 * @see [io.bluetape4k.javatimes.period.calendars.CalendarVisitorFilter]
 * @see [io.bluetape4k.javatimes.period.calendars.seekers.DaySeeker]
 */
abstract class CalendarVisitor<out F: ICalendarVisitorFilter, in C: ICalendarVisitorContext>(
    val filter: F,
    val limits: ITimePeriod,
    val seekDirection: SeekDirection = SeekDirection.FORWARD,
    val calendar: ITimeCalendar = TimeCalendar.Default,
) {

    companion object: KLogging() {
        @JvmField
        val MaxPeriod = TimeRange(MinPeriodTime, MaxPeriodTime.minusYears(1))
    }

    val isForward: Boolean get() = seekDirection.isForward

    protected fun startPeriodVisit(context: C) {
        startPeriodVisit(limits, context)
    }

    protected fun startPeriodVisit(period: ITimePeriod, context: C) {
        log.trace { "기간을 탐색합니다. period=$period, context=$context, seekDir=$seekDirection" }

        if (period.isMoment)
            return

        onVisitStart()

        val years = YearRangeCollection(
            period.start.year,
            period.end.year - period.start.year + 1,
            calendar
        )

        if (onVisitYears(years, context) && enterYears(years, context)) {
            val yearsToVisit = when {
                isForward -> years.yearSequence()
                else      -> years.yearSequence().sortedByDescending { it.end }
            }
            visitYears(yearsToVisit, period, context)
        }
    }

    private fun visitYears(yearsToVisit: Sequence<YearRange>, period: ITimePeriod, context: C) {
        yearsToVisit
            .forEach { year ->
                val canVisit = year.overlapWith(period) && onVisitYear(year, context) && enterMonths(year, context)
                if (canVisit) {
                    val monthsToVisit = when {
                        isForward -> year.monthSequence()
                        else      -> year.monthSequence().sortedByDescending { it.end }
                    }
                    visitMonths(monthsToVisit, period, context)
                }
            }
    }

    private fun visitMonths(monthsToVisit: Sequence<MonthRange>, period: ITimePeriod, context: C) {
        monthsToVisit
            .forEach { m ->
                val canVisit = m.overlapWith(period) && onVisitMonth(m, context) && enterDays(m, context)
                if (canVisit) {
                    val daysToVisit = when {
                        isForward -> m.daySequence()
                        else      -> m.daySequence().sortedByDescending { it.end }
                    }
                    visitDays(daysToVisit, period, context)
                }
            }
    }

    private fun visitDays(daysToVisit: Sequence<DayRange>, period: ITimePeriod, context: C) {
        daysToVisit
            .forEach { day ->
                val canVisit = day.overlapWith(period) && onVisitDay(day, context) && enterHours(day, context)
                if (canVisit) {
                    val hoursToVisit = when {
                        isForward -> day.hourSequence()
                        else      -> day.hourSequence().sortedByDescending { it.end }
                    }
                    visitHours(hoursToVisit, period, context)
                }
            }
    }

    private fun visitHours(hoursToVisit: Sequence<HourRange>, period: ITimePeriod, context: C) {
        hoursToVisit
            .forEach { hour ->
                val canVisit = hour.overlapWith(period) && onVisitHour(hour, context)
                if (canVisit) {
                    enterMinutes(hour, context)
                }
            }
    }

    protected open fun startYearVisit(year: YearRange, context: C, seekDir: SeekDirection): YearRange? {
        onVisitStart()

        var lastVisited: YearRange? = null

        val offset = seekDir.direction
        var current = year

        while (lastVisited == null && MaxPeriod.hasPureInsideWith(current)) {
            if (onVisitYear(current, context)) {
                current = current.addYears(offset)
            } else {
                lastVisited = current
            }
        }

        onVisitEnd()
        log.trace { "Year 단위 탐색 완료. lastVisited=$lastVisited" }

        return lastVisited
    }

    protected open fun startMonthVisit(month: MonthRange, context: C, seekDir: SeekDirection): MonthRange? {
        onVisitStart()

        var lastVisited: MonthRange? = null

        val offset = seekDir.direction
        var current = month

        while (lastVisited == null && MaxPeriod.hasPureInsideWith(current)) {
            if (onVisitMonth(current, context)) {
                current = current.addMonths(offset)
            } else {
                lastVisited = current
            }
        }

        onVisitEnd()
        log.trace { "Month 단위 탐색 완료. lastVisited=$lastVisited" }

        return lastVisited
    }

    protected open fun startDayVisit(day: DayRange, context: C, seekDir: SeekDirection): DayRange? {
        onVisitStart()

        var lastVisited: DayRange? = null

        val offset = seekDir.direction
        var current = day

        while (lastVisited == null && MaxPeriod.hasPureInsideWith(current)) {
            if (onVisitDay(current, context)) {
                current = current.addDays(offset)
            } else {
                lastVisited = current
            }
        }

        onVisitEnd()
        log.trace { "Day 단위 탐색 완료. lastVisited=$lastVisited" }

        return lastVisited
    }

    protected open fun startHourVisit(hour: HourRange, context: C, seekDir: SeekDirection): HourRange? {
        onVisitStart()

        val offset = seekDir.direction
        var current = hour
        var lastVisited: HourRange? = null

        while (lastVisited == null && MaxPeriod.hasPureInsideWith(current)) {
            if (onVisitHour(current, context)) {
                current = current.addHours(offset)
            } else {
                lastVisited = current
            }
        }

        onVisitEnd()
        log.trace { "Hour 단위 탐색 완료.  lastVisited=$lastVisited" }

        return lastVisited
    }

    protected open fun onVisitStart() {
        log.trace { "Start visiting..." }
    }

    protected open fun onVisitEnd() {
        log.trace { "End visiting" }
    }

    protected fun isLimits(target: ITimePeriod): Boolean = limits.hasInsideWith(target)

    protected fun isExcludePeriod(target: ITimePeriod): Boolean =
        filter.excludePeriods.isEmpty() || filter.excludePeriods.overlapPeriods(target).isEmpty()

    protected open fun enterYears(years: YearRangeCollection, context: C): Boolean = true
    protected open fun enterMonths(year: YearRange, context: C): Boolean = true
    protected open fun enterDays(month: MonthRange, context: C): Boolean = true
    protected open fun enterHours(day: DayRange, context: C): Boolean = true
    protected open fun enterMinutes(hour: HourRange, context: C): Boolean = true

    protected open fun onVisitYears(years: YearRangeCollection, context: C): Boolean = true
    protected open fun onVisitYear(year: YearRange, context: C): Boolean = true
    protected open fun onVisitMonth(month: MonthRange, context: C): Boolean = true
    protected open fun onVisitDay(day: DayRange, context: C): Boolean = true
    protected open fun onVisitHour(hour: HourRange, context: C): Boolean = true
    protected open fun onVisitMinutes(minute: MinuteRange, context: C): Boolean = true

    protected open fun isMatchingYear(range: YearRange, context: C): Boolean =
        isExcludePeriod(range) &&
                (filter.years.isEmpty() || filter.years.contains(range.year))

    protected open fun isMatchingMonth(range: MonthRange, context: C): Boolean = when {
        filter.years.isNotEmpty() && !filter.years.contains(range.year)                      -> false
        filter.monthOfYears.isNotEmpty() && !filter.monthOfYears.contains(range.monthOfYear) -> false
        else                                                                                 -> isExcludePeriod(range)
    }

    protected open fun isMatchingDay(range: DayRange, context: C): Boolean = when {
        filter.years.isNotEmpty() && !filter.years.contains(range.year)                      -> false
        filter.monthOfYears.isNotEmpty() && !filter.monthOfYears.contains(range.monthOfYear) -> false
        filter.dayOfMonths.isNotEmpty() && !filter.dayOfMonths.contains(range.dayOfMonth)    -> false
        filter.dayOfWeeks.isNotEmpty() && !filter.dayOfWeeks.contains(range.dayOfWeek)       -> false
        else                                                                                 -> isExcludePeriod(range)
    }

    protected open fun isMatchingHour(range: HourRange, context: C): Boolean = when {
        filter.years.isNotEmpty() && !filter.years.contains(range.year)                      -> false
        filter.monthOfYears.isNotEmpty() && !filter.monthOfYears.contains(range.monthOfYear) -> false
        filter.dayOfMonths.isNotEmpty() && !filter.dayOfMonths.contains(range.dayOfMonth)    -> false
        filter.dayOfWeeks.isNotEmpty() && !filter.dayOfWeeks.contains(range.startDayOfWeek)  -> false
        filter.hourOfDays.isNotEmpty() && !filter.hourOfDays.contains(range.hourOfDay)       -> false
        else                                                                                 -> isExcludePeriod(range)
    }

    protected open fun isMatchingMinute(range: MinuteRange, context: C): Boolean = when {
        filter.years.isNotEmpty() && !filter.years.contains(range.year)                         -> false
        filter.monthOfYears.isNotEmpty() && !filter.monthOfYears.contains(range.monthOfYear)    -> false
        filter.dayOfMonths.isNotEmpty() && !filter.dayOfMonths.contains(range.dayOfMonth)       -> false
        filter.dayOfWeeks.isNotEmpty() && !filter.dayOfWeeks.contains(range.startDayOfWeek)     -> false
        filter.hourOfDays.isNotEmpty() && !filter.hourOfDays.contains(range.hourOfDay)          -> false
        filter.minuteOfHours.isNotEmpty() && !filter.minuteOfHours.contains(range.minuteOfHour) -> false
        else                                                                                    ->
            isExcludePeriod(range)
    }
}
