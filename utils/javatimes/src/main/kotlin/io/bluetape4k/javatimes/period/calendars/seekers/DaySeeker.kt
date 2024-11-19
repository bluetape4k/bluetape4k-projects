package io.bluetape4k.javatimes.period.calendars.seekers

import io.bluetape4k.javatimes.period.ITimeCalendar
import io.bluetape4k.javatimes.period.SeekDirection
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.TimeRange
import io.bluetape4k.javatimes.period.calendars.CalendarVisitor
import io.bluetape4k.javatimes.period.calendars.CalendarVisitorFilter
import io.bluetape4k.javatimes.period.ranges.DayRange
import io.bluetape4k.javatimes.period.ranges.MonthRange
import io.bluetape4k.javatimes.period.ranges.YearRange
import io.bluetape4k.javatimes.period.ranges.YearRangeCollection
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace

/**
 * [filter]를 이용하여 기간에서 원하는 날짜를 찾습니다.
 *
 * ```
 * val start = DayRange(2011, 2, 15)
 * val filter = CalendarVisitorFilter().apply {
 *     addWorkingWeekdays()
 *     // 14 days -> week 9 and week 10
 *     excludePeriods.add(
 *         DayRangeCollection(
 *             zonedDateTimeOf(2011, 2, 27),
 *             14
 *         )
 *     )
 * }
 * val daySeeker = DaySeeker(filter)
 * val day1 = daySeeker.findDay(start, 3)
 * day1 shouldBeEqualTo DayRange(2011, 2, 18)
 * val day2 = daySeeker.findDay(start, 4)   // 주말 (19, 20) 제외
 * day2 shouldBeEqualTo DayRange(2011, 2, 21)
 * val day3 = daySeeker.findDay(start, 10)   // 주말 (19, 20) 제외, 2.27부터 14일간 휴가
 * day3 shouldBeEqualTo DayRange(2011, 3, 15)
 * ```
 *
 * @param filter 칼렌더 탐색 필터 (예외 기간 등을 건너뛰도록 합니다)
 * @param seekDir 탐색 방향
 * @param calendar 달력
 */
open class DaySeeker private constructor(
    filter: CalendarVisitorFilter,
    seekDir: SeekDirection,
    calendar: ITimeCalendar,
): CalendarVisitor<CalendarVisitorFilter, DaySeekerContext>(filter, TimeRange.AnyTime, seekDir, calendar) {

    companion object: KLogging() {
        @JvmStatic
        operator fun invoke(
            filter: CalendarVisitorFilter = CalendarVisitorFilter(),
            seekDir: SeekDirection = SeekDirection.FORWARD,
            calendar: ITimeCalendar = TimeCalendar.Default,
        ): DaySeeker {
            return DaySeeker(filter, seekDir, calendar)
        }
    }

    /**
     * [startDay] 부터 [dayCount] 만큼의 날짜를 찾습니다.
     *
     * ```
     * val start = DayRange()
     * val daySeeker = DaySeeker()
     *
     * val day1 = daySeeker.findDay(start, 0)
     * day1 shouldBeEqualTo start
     *
     * val day2 = daySeeker.findDay(start, 1)
     * day2 shouldBeEqualTo start.nextDay()
     *
     * (-10..20).forEach { i ->
     *     val offset = i * 5
     *     val day = daySeeker.findDay(start, offset)
     *     day shouldBeEqualTo start.addDays(offset)
     * }
     * ```
     *
     * @param startDay 시작 날짜
     * @param dayCount 찾을 날짜의 수
     */
    open fun findDay(startDay: DayRange, dayCount: Int): DayRange? {
        log.trace { "find day... startDay=$startDay, dayCount=$dayCount" }

        if (dayCount == 0)
            return startDay

        val context = DaySeekerContext(startDay, dayCount)
        var visitDir = seekDirection

        if (dayCount < 0) {
            visitDir = when {
                visitDir.isForward -> SeekDirection.BACKWARD
                else               -> SeekDirection.FORWARD
            }
        }

        startDayVisit(startDay, context, visitDir)
        val foundDay = context.foundDay

        log.trace { "Success to find day. startDay=$startDay, dayCount=$dayCount, visitDir=$visitDir, foundDay=$foundDay" }
        return foundDay
    }

    override fun enterYears(years: YearRangeCollection, context: DaySeekerContext): Boolean = context.notFinished

    override fun enterMonths(year: YearRange, context: DaySeekerContext): Boolean = context.notFinished

    override fun enterDays(month: MonthRange, context: DaySeekerContext): Boolean = context.notFinished

    override fun enterHours(day: DayRange, context: DaySeekerContext): Boolean = false

    override fun onVisitDay(day: DayRange, context: DaySeekerContext): Boolean {
        return when {
            context.isFinished                 -> false
            day.isSamePeriod(context.startDay) -> true
            !isMatchingDay(day, context)       -> true
            !isLimits(day)                     -> true

            else                               -> {
                context.processDay(day)
                // context 가 찾기를 완료하면 탐색(Visit)를 중단하도록 합니다.
                !context.isFinished
            }
        }
    }
}
