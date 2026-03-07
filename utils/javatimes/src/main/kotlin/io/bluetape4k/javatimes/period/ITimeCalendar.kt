package io.bluetape4k.javatimes.period

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAccessor
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

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
     * 기본값은 1월입니다. `yearOf(...)`, `ZonedDateTime.yearOf(calendar)`, `YearCalendarTimeRange.baseYear`
     * 같은 helper 성 API에서 사용됩니다.
     */
    val baseMonth: Int get() = 1

    fun year(moment: ZonedDateTime): Int = moment.year
    fun monthOfYear(moment: ZonedDateTime): Int = moment.monthValue
    fun weekFields(): WeekFields = WeekFields.of(firstDayOfWeek, 4)

    fun weekOfyear(moment: TemporalAccessor): WeekyearWeek = WeekyearWeek(moment, weekFields())

    fun startOfWeek(moment: ZonedDateTime): ZonedDateTime =
        moment.truncatedTo(ChronoUnit.DAYS).with(TemporalAdjusters.previousOrSame(firstDayOfWeek))

    fun startOfYearWeek(moment: ZonedDateTime): ZonedDateTime =
        weekOfyear(moment).run { startOfYearWeek(weekyear, weekOfWeekyear, moment.zone) }

    fun startOfYearWeek(weekyear: Int, weekOfWeekyear: Int, zoneId: ZoneId = ZoneId.systemDefault()): ZonedDateTime {
        val weekFields = weekFields()
        val localDate = LocalDate.of(weekyear, 1, 4)
            .with(weekFields.weekBasedYear(), weekyear.toLong())
            .with(weekFields.weekOfWeekBasedYear(), weekOfWeekyear.toLong())
            .with(weekFields.dayOfWeek(), 1L)
        return ZonedDateTime.of(localDate, LocalTime.MIDNIGHT, zoneId)
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
