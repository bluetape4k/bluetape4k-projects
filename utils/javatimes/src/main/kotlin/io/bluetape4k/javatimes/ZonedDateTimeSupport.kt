package io.bluetape4k.javatimes

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

/**
 * [ZonedDateTime] 을 생성합니다.
 */
fun zonedDateTimeOf(
    year: Int,
    monthOfYear: Int = 1,
    dayOfMonth: Int = 1,
    hourOfDay: Int = 0,
    minuteOfHour: Int = 0,
    secondOfMinute: Int = 0,
    nanoOfSecond: Int = 0,
    zoneId: ZoneId = SystemZoneId,
): ZonedDateTime =
    ZonedDateTime.of(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, nanoOfSecond, zoneId)

/**
 * [localDate], [localTime]을 이용하여 [ZonedDateTime] 을 생성합니다.
 */
fun zonedDateTimeOf(
    localDate: LocalDate = LocalDate.ofEpochDay(0),
    localTime: LocalTime = LocalTime.MIDNIGHT,
    zoned: ZoneId = SystemZoneId,
): ZonedDateTime =
    ZonedDateTime.of(localDate, localTime, zoned)

/**
 * ISO 8601 기준으로 해당 년도의 Week의 수를 반환합니다.
 */
val ZonedDateTime.weekyear: Int get() = this[WeekFields.ISO.weekBasedYear()]

/**
 * ISO 8601 기준으로 해당 년도의 몇 번째 Week인지를 반환합니다.
 */
val ZonedDateTime.weekOfWeekyear: Int get() = this[WeekFields.ISO.weekOfWeekBasedYear()]

/**
 * ISO 8601 기준으로 해당 월의 몇 번째 주인지를 반환합니다.
 */
val ZonedDateTime.weekOfMonth: Int get() = this[WeekFields.ISO.weekOfMonth()]

/**
 * [ZonedDateTime]의 Day 이후의 값을 초 단위로 반환합니다. (Hour, Minute, Second)
 */
val ZonedDateTime.seondsOfDay: Int get() = this[ChronoField.SECOND_OF_DAY]

/**
 * [ZonedDateTime]의 Day 이후의 값을 밀리초 단위로 반환합니다. (Hour, Minute, Second, Millis)
 */
val ZonedDateTime.millisOfDay: Int get() = this[ChronoField.MILLI_OF_DAY]

/**
 * [ZonedDateTime]의 Day 이후의 값을 마이크로초 단위로 반환합니다. (Hour, Minute, Second, Millis, Nanos)
 */
val ZonedDateTime.nanoOfDay: Long get() = this.getLong(ChronoField.NANO_OF_DAY)

/**
 * [ZonedDateTime]을 UTC 기준의 [Instant]로 변환합니다.
 */
fun ZonedDateTime.toUtcInstant(): Instant =
    Instant.ofEpochSecond(this.toEpochSecond()) // toInstant().minusSeconds(offset.totalSeconds.toLong())

//fun ZonedDateTime.startOfYear(): ZonedDateTime = zonedDateTimeOf(year, 1, 1)
/**
 * [ZonedDateTime]의 해당 년도의 마지막 시각을 반환합니다. (예: 12월 31일 23:59:59.999999999)
 */
fun ZonedDateTime.endOfYear(): ZonedDateTime = endOfYear(year)

/**
 * [ZonedDateTime]의 해당 분기의 시작 시각을 반환합니다. (예: 4월 1일 00:00:00.000000000)
 */
fun ZonedDateTime.startOfQuarter(): ZonedDateTime = startOfQuarter(year, monthValue)

/**
 * [ZonedDateTime]의 해당 분기의 마지막 시각을 반환합니다. (예: 6월 30일 23:59:59.999999999)
 */
fun ZonedDateTime.endOfQuarter(): ZonedDateTime = endOfQuarter(year, monthValue)

/**
 * [ZonedDateTime]의 해당 월의 시작 시각을 반환합니다. (예: 4월 1일 00:00:00.000000000)
 */
fun ZonedDateTime.startOfMonth(): ZonedDateTime = zonedDateTimeOf(year, monthValue, 1)

/**
 * [ZonedDateTime]의 해당 월의 마지막 시각을 반환합니다. (예: 4월 30일 23:59:59.999999999)
 */
fun ZonedDateTime.endOfMonth(): ZonedDateTime = endOfMonth(year, monthValue)

/**
 * [ZonedDateTime]의 해당 주의 시작 시각을 반환합니다. (예: 4월 3일 00:00:00.000000000)
 */
fun ZonedDateTime.startOfWeek(): ZonedDateTime = startOfWeek(year, monthValue, dayOfMonth)

/**
 * [ZonedDateTime]의 해당 주의 마지막 시각을 반환합니다. (예: 4월 10일 23:59:59.999999999)
 */
fun ZonedDateTime.endOfWeek(): ZonedDateTime = endOfWeek(year, monthValue, dayOfMonth)

/**
 * [ZonedDateTime]의 해당 일의 시작 시각을 반환합니다. (예: 4월 3일 00:00:00.000000000)
 */
fun ZonedDateTime.startOfDay(): ZonedDateTime = truncatedTo(ChronoUnit.DAYS)

/**
 * [ZonedDateTime]의 해당 일의 마지막 시각을 반환합니다. (예: 4월 3일 23:59:59.999999999)
 */
fun ZonedDateTime.endOfDay(): ZonedDateTime = startOfDay().plusDays(1).minusNanos(1)

/**
 * [ZonedDateTime]의 해당 시의 시작 시각을 반환합니다. (예: 4월 3일 13:00:00.000000000)
 */
fun ZonedDateTime.startOfHour(): ZonedDateTime = truncatedTo(ChronoUnit.HOURS)

/**
 * [ZonedDateTime]의 해당 시의 마지막 시각을 반환합니다. (예: 4월 3일 13:59:59.999999999)
 */
fun ZonedDateTime.endOfHour(): ZonedDateTime = startOfHour().plusHours(1L).minusNanos(1)

/**
 * [ZonedDateTime]의 해당 분의 시작 시각을 반환합니다. (예: 4월 3일 13:19:.000000000)
 */
fun ZonedDateTime.startOfMinute(): ZonedDateTime = truncatedTo(ChronoUnit.MINUTES)

/**
 * [ZonedDateTime]의 해당 분의 마지막 시각을 반환합니다. (예: 4월 3일 13:19:59.999999999)
 */
fun ZonedDateTime.endOfMinute(): ZonedDateTime = startOfMinute().plusMinutes(1).minusNanos(1)

/**
 * [ZonedDateTime]의 해당 초의 시작 시각을 반환합니다. (예: 4월 3일 13:19:23.000000000)
 */
fun ZonedDateTime.startOfSecond(): ZonedDateTime = truncatedTo(ChronoUnit.SECONDS)

/**
 * [ZonedDateTime]의 해당 초의 마지막 시각을 반환합니다. (예: 4월 3일 13:19:23.999999999)
 */
fun ZonedDateTime.endOfSeconds(): ZonedDateTime = startOfSecond().plusSeconds(1).minusNanos(1)

/**
 * [ZonedDateTime]의 해당 밀리초의 시작 시각을 반환합니다. (예: 4월 3일 13:19:23.123000000)
 */
fun ZonedDateTime.startOfMillis(): ZonedDateTime = truncatedTo(ChronoUnit.MILLIS)

/**
 * [ZonedDateTime]의 해당 밀리초의 마지막 시각을 반환합니다. (예: 4월 3일 13:19:23.123999999)
 */
fun ZonedDateTime.endOfMillis(): ZonedDateTime = startOfMillis().plusNanos(1_000_000L - 1L)

/**
 * [year]의 시작 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun startOfYear(year: Int): ZonedDateTime =
    zonedDateTimeOf(year, 1, 1)

/**
 * [year]의 마지막 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun endOfYear(year: Int): ZonedDateTime =
    startOfYear(year).plusYears(1).minusNanos(1)

/**
 * [year],[monthOfYear]의 분기 시작 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun startOfQuarter(year: Int, monthOfYear: Int): ZonedDateTime =
    startOfQuarter(year, Quarter.ofMonth(monthOfYear))

/**
 * [year],[quarter]의 분기 시작 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun startOfQuarter(year: Int, quarter: Quarter): ZonedDateTime =
    zonedDateTimeOf(year, quarter.startMonth, 1)

/**
 * [year], [monthOfYear]의 분기 마지막 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun endOfQuarter(year: Int, monthOfYear: Int): ZonedDateTime =
    endOfQuarter(year, Quarter.ofMonth(monthOfYear))

/**
 * [year], [quarter]의 분기 마지막 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun endOfQuarter(year: Int, quarter: Quarter): ZonedDateTime =
    zonedDateTimeOf(year, quarter.endMonth, 1).plusMonths(1).minusNanos(1)

/**
 * [year], [monthOfYear]의 월 시작 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun startOfMonth(year: Int, monthOfYear: Int): ZonedDateTime =
    zonedDateTimeOf(year, monthOfYear, 1)

/**
 * [year], [monthOfYear]의 월 마지막 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun endOfMonth(year: Int, monthOfYear: Int): ZonedDateTime =
    startOfMonth(year, monthOfYear).plusMonths(1).minusNanos(1)

/**
 * [year], [monthOfYear]의 월의 날짜 수를 반환합니다. (예: 31일, 30일, 28일, 29일)
 */
fun lengthOfMonth(year: Int, monthOfYear: Int): Int =
    YearMonth.of(year, monthOfYear).lengthOfMonth()

/**
 * [year], [monthOfYear], [dayOfMonth]의 주(week) 단위 시작 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun startOfWeek(year: Int, monthOfYear: Int, dayOfMonth: Int): ZonedDateTime {
    val date = zonedDateTimeOf(year, monthOfYear, dayOfMonth)
    return date - (date.dayOfWeek.value - DayOfWeek.MONDAY.value).days()
}

/**
 * [year], [monthOfYear], [dayOfMonth]의 주(week) 단위 마지막 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun endOfWeek(year: Int, monthOfYear: Int, dayOfMonth: Int): ZonedDateTime =
    startOfWeek(year, monthOfYear, dayOfMonth).plusDays(DaysPerWeek.toLong()).minusNanos(1)

/**
 * [year], 주차([weekOfWeekyear])의 시작 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun startOfWeekOfWeekyear(weekyear: Int, weekOfWeekyear: Int): ZonedDateTime =
    ZonedDateTime.now()
        .with(IsoFields.WEEK_BASED_YEAR, weekyear.toLong())
        .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, weekOfWeekyear.toLong())
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * [year], 주차([weekOfWeekyear])의 마지막 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun endOfWeekOfWeekyear(weekyear: Int, weekOfWeekyear: Int): ZonedDateTime =
    startOfWeekOfWeekyear(weekyear, weekOfWeekyear)
        .plusDays(DaysPerWeek.toLong())
        .minusNanos(1)

/**
 * 현 시각의 다음 주 같은 요일의 시각을 반환한다.
 */
fun ZonedDateTime.nextDayOfWeek(): ZonedDateTime = this.plusWeeks(1)

/**
 * 현 시각의 전 주 같은 요일의 시각을 반환한다.
 */
fun ZonedDateTime.prevDayOfWeek(): ZonedDateTime = this.minusWeeks(1)

/**
 * 두 개의 [ZonedDateTime] 중 더 작은 값을 반환합니다
 */
infix fun ZonedDateTime?.min(that: ZonedDateTime?): ZonedDateTime? = when {
    this == null -> that
    that == null -> this
    this < that -> this
    else -> that
}

/**
 * 두 개의 [ZonedDateTime] 중 더 큰 값을 반환합니다
 */
infix fun ZonedDateTime?.max(that: ZonedDateTime?): ZonedDateTime? = when {
    this == null -> that
    that == null -> this
    this > that -> this
    else -> that
}

/**
 * [ZonedDateTime]가 [that]과 같은지 여부를 반환합니다.
 */
fun ZonedDateTime.equalTo(that: OffsetDateTime): Boolean =
    this.toOffsetDateTime().isEqual(that)

/**
 * [ZonedDateTime]가 [that]과 초 단위까지 같은지 여부를 반환합니다.
 */
fun ZonedDateTime?.equalToSeconds(that: ZonedDateTime?): Boolean = when {
    (this == null || that == null) -> false
    else -> this.truncatedTo(ChronoUnit.SECONDS).isEqual(that.truncatedTo(ChronoUnit.SECONDS))
}

/**
 * [ZonedDateTime]가 [that]과 밀리초 단위까지 같은지 여부를 반환합니다.
 */
fun ZonedDateTime?.equalToMillis(that: ZonedDateTime?): Boolean = when {
    (this == null || that == null) -> false
    else -> this.truncatedTo(ChronoUnit.MILLIS).isEqual(that.truncatedTo(ChronoUnit.MILLIS))
}
