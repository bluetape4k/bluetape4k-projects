package io.bluetape4k.javatimes.period

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAccessor

/**
 * 시간 단위의 달력을 나타내는 인터페이스
 */
interface ITimeCalendar: ITimePeriodMapper {

    /** 기간 시작을 매핑할 때 적용할 offset */
    val startOffset: Duration

    /** 기간 종료를 매핑할 때 적용할 offset */
    val endOffset: Duration

    /** 주 계산의 시작 요일 */
    val firstDayOfWeek: DayOfWeek

    /**
     * 연도 계산의 기준 월입니다.
     *
     * 기본값은 1월이며, 회계연도처럼 4월 시작 연도를 사용하려면 4를 반환하도록 구현합니다.
     */
    val baseMonth: Int get() = 1

    fun year(moment: ZonedDateTime): Int = moment.year
    fun monthOfYear(moment: ZonedDateTime): Int = moment.monthValue

    fun weekOfyear(moment: TemporalAccessor): WeekyearWeek = WeekyearWeek(moment)

    fun startOfYearWeek(moment: ZonedDateTime): ZonedDateTime =
        weekOfyear(moment).run { startOfYearWeek(weekyear, weekOfWeekyear, moment.zone) }

    fun startOfYearWeek(weekyear: Int, weekOfWeekyear: Int, zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
        val localDate = LocalDate.ofYearDay(Year.of(weekyear).value, weekOfWeekyear * 7)
        return ZonedDateTime.of(localDate, LocalTime.ofSecondOfDay(0L), zoneId)
    }

    fun dayOfMonth(moment: ZonedDateTime): Int = moment.dayOfMonth
    fun dayOfYear(moment: ZonedDateTime): Int = moment.dayOfYear
    fun dayOfWeek(moment: ZonedDateTime): DayOfWeek = moment.dayOfWeek

    fun daysInMonth(moment: TemporalAccessor): Int = YearMonth.from(moment).lengthOfMonth()
    fun daysInMonth(yearMonth: YearMonth): Int = yearMonth.lengthOfMonth()
    fun daysInMonth(year: Int, monthOfYear: Int): Int = YearMonth.of(year, monthOfYear).lengthOfMonth()

    fun hourOfDay(moment: ZonedDateTime): Int = moment.hour
    fun minuteOfHour(moment: ZonedDateTime): Int = moment.minute

}
