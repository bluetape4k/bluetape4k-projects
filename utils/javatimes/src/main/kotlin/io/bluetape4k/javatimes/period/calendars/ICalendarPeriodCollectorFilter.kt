package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.javatimes.period.ranges.DayOfWeekHourRange
import io.bluetape4k.javatimes.period.ranges.DayRangeInMonth
import io.bluetape4k.javatimes.period.ranges.HourRangeInDay
import io.bluetape4k.javatimes.period.ranges.MonthRangeInYear

/**
 * 달력 기간 수집 필터 인터페이스
 *
 * 특정 월, 일, 시간, 요일-시간 범위를 수집하기 위한 필터
 */
interface ICalendarPeriodCollectorFilter: ICalendarVisitorFilter {

    /** 수집할 월 범위 목록 */
    val collectingMonths: MutableList<MonthRangeInYear>

    /** 수집할 일 범위 목록 */
    val collectingDays: MutableList<DayRangeInMonth>

    /** 수집할 시간 범위 목록 */
    val collectingHours: MutableList<HourRangeInDay>

    /** 수집할 요일-시간 범위 목록 */
    val collectingDayOfWeekHours: MutableList<DayOfWeekHourRange>
}
