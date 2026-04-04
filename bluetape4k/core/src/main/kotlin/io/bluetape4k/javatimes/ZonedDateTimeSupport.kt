package io.bluetape4k.javatimes

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

/**
 * [ZonedDateTime] 을 생성합니다.
 *
 * 기본 [zoneId]는 UTC입니다.
 *
 * ```kotlin
 * // UTC 기준 특정 시각 생성
 * val utc = zonedDateTimeOf(2024, 3, 15, 9, 30, 0)
 * // 2024-03-15T09:30:00Z
 *
 * // 서울 시간대로 생성
 * val seoul = zonedDateTimeOf(2024, 3, 15, 9, 30, 0, zoneId = ZoneId.of("Asia/Seoul"))
 * // 2024-03-15T09:30:00+09:00[Asia/Seoul]
 * ```
 *
 * @param year 년도
 * @param monthOfYear 월 (1..12, 기본값 1)
 * @param dayOfMonth 일 (1..31, 기본값 1)
 * @param hourOfDay 시 (0..23, 기본값 0)
 * @param minuteOfHour 분 (0..59, 기본값 0)
 * @param secondOfMinute 초 (0..59, 기본값 0)
 * @param nanoOfSecond 나노초 (0..999999999, 기본값 0)
 * @param zoneId 시간대 (기본값 UTC)
 * @return 생성된 [ZonedDateTime]
 */
fun zonedDateTimeOf(
    year: Int,
    monthOfYear: Int = 1,
    dayOfMonth: Int = 1,
    hourOfDay: Int = 0,
    minuteOfHour: Int = 0,
    secondOfMinute: Int = 0,
    nanoOfSecond: Int = 0,
    zoneId: ZoneId = ZoneOffset.UTC,
): ZonedDateTime =
    ZonedDateTime.of(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, nanoOfSecond, zoneId)

/**
 * [localDate], [localTime]을 이용하여 [ZonedDateTime] 을 생성합니다.
 *
 * 기본 zone은 UTC입니다.
 *
 * ```kotlin
 * val date = LocalDate.of(2024, 6, 1)
 * val time = LocalTime.of(12, 0, 0)
 *
 * val zdt = zonedDateTimeOf(date, time)
 * // 2024-06-01T12:00:00Z
 *
 * val zdtSeoul = zonedDateTimeOf(date, time, ZoneId.of("Asia/Seoul"))
 * // 2024-06-01T12:00:00+09:00[Asia/Seoul]
 * ```
 *
 * @param localDate 날짜 (기본값 Unix epoch 시작일)
 * @param localTime 시각 (기본값 자정)
 * @param zoned 시간대 (기본값 UTC)
 * @return 생성된 [ZonedDateTime]
 */
fun zonedDateTimeOf(
    localDate: LocalDate = LocalDate.ofEpochDay(0),
    localTime: LocalTime = LocalTime.MIDNIGHT,
    zoned: ZoneId = ZoneOffset.UTC,
): ZonedDateTime =
    ZonedDateTime.of(localDate, localTime, zoned)

/**
 * ISO 8601 기준으로 해당 년도의 Week의 수를 반환합니다.
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 1, 1)
 * zdt.weekyear // 2024
 * ```
 */
val ZonedDateTime.weekyear: Int get() = this[WeekFields.ISO.weekBasedYear()]

/**
 * ISO 8601 기준으로 해당 년도의 몇 번째 Week인지를 반환합니다.
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 1, 8)
 * zdt.weekOfWeekyear // 2 (해당 년도의 2번째 주)
 * ```
 */
val ZonedDateTime.weekOfWeekyear: Int get() = this[WeekFields.ISO.weekOfWeekBasedYear()]

/**
 * ISO 8601 기준으로 해당 월의 몇 번째 주인지를 반환합니다.
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 1, 8)
 * zdt.weekOfMonth // 2 (해당 월의 2번째 주)
 * ```
 */
val ZonedDateTime.weekOfMonth: Int get() = this[WeekFields.ISO.weekOfMonth()]

/**
 * [ZonedDateTime]의 Day 이후의 값을 초 단위로 반환합니다. (Hour, Minute, Second)
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 1, 1, 1, 30, 45)
 * zdt.secondsOfDay // 1 * 3600 + 30 * 60 + 45 = 5445
 * ```
 */
val ZonedDateTime.secondsOfDay: Int get() = this[ChronoField.SECOND_OF_DAY]

/**
 * [ZonedDateTime]의 Day 이후의 값을 밀리초 단위로 반환합니다. (Hour, Minute, Second, Millis)
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 1, 1, 0, 0, 1)
 * zdt.millisOfDay // 1000
 * ```
 */
val ZonedDateTime.millisOfDay: Int get() = this[ChronoField.MILLI_OF_DAY]

/**
 * [ZonedDateTime]의 Day 이후의 값을 마이크로초 단위로 반환합니다. (Hour, Minute, Second, Millis, Nanos)
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 1, 1, 0, 0, 1)
 * zdt.nanoOfDay // 1_000_000_000L
 * ```
 */
val ZonedDateTime.nanoOfDay: Long get() = this.getLong(ChronoField.NANO_OF_DAY)

/**
 * [ZonedDateTime]을 UTC 기준의 [Instant]로 변환합니다.
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 1, 1, 0, 0, 0, zoneId = ZoneId.of("Asia/Seoul"))
 * val instant = zdt.toUtcInstant()
 * // 2023-12-31T15:00:00Z (UTC 기준 -9시간)
 * ```
 *
 * @return UTC 기준 [Instant]
 */
fun ZonedDateTime.toUtcInstant(): Instant =
    toInstant()

//fun ZonedDateTime.startOfYear(): ZonedDateTime = zonedDateTimeOf(year, 1, 1)
/**
 * [ZonedDateTime]의 해당 년도의 마지막 시각을 반환합니다. (예: 12월 31일 23:59:59.999999999)
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 6, 15)
 * zdt.endOfYear() // 2024-12-31T23:59:59.999999999Z
 * ```
 *
 * @return 해당 년도의 마지막 나노초 시각
 */
fun ZonedDateTime.endOfYear(): ZonedDateTime = endOfYear(year, zone)

/**
 * [ZonedDateTime]의 해당 분기의 시작 시각을 반환합니다. (예: 4월 1일 00:00:00.000000000)
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 5, 20)   // 5월 → Q2
 * zdt.startOfQuarter() // 2024-04-01T00:00:00Z
 * ```
 *
 * @return 해당 분기 첫날 자정 시각
 */
fun ZonedDateTime.startOfQuarter(): ZonedDateTime = startOfQuarter(year, monthValue, zone)

/**
 * [ZonedDateTime]의 해당 분기의 마지막 시각을 반환합니다. (예: 6월 30일 23:59:59.999999999)
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 5, 20)   // 5월 → Q2
 * zdt.endOfQuarter() // 2024-06-30T23:59:59.999999999Z
 * ```
 *
 * @return 해당 분기 마지막 나노초 시각
 */
fun ZonedDateTime.endOfQuarter(): ZonedDateTime = endOfQuarter(year, monthValue, zone)

/**
 * [ZonedDateTime]의 해당 월의 시작 시각을 반환합니다. (예: 4월 1일 00:00:00.000000000)
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 4, 15, 10, 30)
 * zdt.startOfMonth() // 2024-04-01T00:00:00Z
 * ```
 *
 * @return 해당 월 첫날 자정 시각
 */
fun ZonedDateTime.startOfMonth(): ZonedDateTime = withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)

/**
 * [ZonedDateTime]의 해당 월의 마지막 시각을 반환합니다. (예: 4월 30일 23:59:59.999999999)
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 4, 15, 10, 30)
 * zdt.endOfMonth() // 2024-04-30T23:59:59.999999999Z
 * ```
 *
 * @return 해당 월 마지막 나노초 시각
 */
fun ZonedDateTime.endOfMonth(): ZonedDateTime = startOfMonth().plusMonths(1).minusNanos(1)

/**
 * [ZonedDateTime]의 해당 주의 시작 시각을 반환합니다. (예: 4월 3일 00:00:00.000000000)
 *
 * ISO 8601 기준 주 시작은 월요일(MONDAY)입니다.
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 4, 4) // 목요일
 * zdt.startOfWeek() // 2024-04-01T00:00:00Z (월요일)
 * ```
 *
 * @return 해당 주 월요일 자정 시각
 */
fun ZonedDateTime.startOfWeek(): ZonedDateTime =
    startOfDay().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * [ZonedDateTime]의 해당 주의 마지막 시각을 반환합니다. (예: 4월 10일 23:59:59.999999999)
 *
 * ISO 8601 기준 주 마지막은 일요일(SUNDAY)입니다.
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 4, 4) // 목요일
 * zdt.endOfWeek() // 2024-04-07T23:59:59.999999999Z (일요일)
 * ```
 *
 * @return 해당 주 일요일 마지막 나노초 시각
 */
fun ZonedDateTime.endOfWeek(): ZonedDateTime = startOfWeek().plusDays(DaysPerWeek.toLong()).minusNanos(1)

/**
 * [ZonedDateTime]의 해당 일의 시작 시각을 반환합니다. (예: 4월 3일 00:00:00.000000000)
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 4, 3, 15, 30, 45)
 * zdt.startOfDay() // 2024-04-03T00:00:00Z
 * ```
 *
 * @return 해당 일 자정 시각
 */
fun ZonedDateTime.startOfDay(): ZonedDateTime = truncatedTo(ChronoUnit.DAYS)

/**
 * [ZonedDateTime]의 해당 일의 마지막 시각을 반환합니다. (예: 4월 3일 23:59:59.999999999)
 *
 * ```kotlin
 * val zdt = zonedDateTimeOf(2024, 4, 3, 15, 30, 45)
 * zdt.endOfDay() // 2024-04-03T23:59:59.999999999Z
 * ```
 *
 * @return 해당 일 마지막 나노초 시각
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
 * [ZonedDateTime]의 해당 초의 마지막 시각을 반환합니다. (예: 4월 3일 13:19:23.999999999)
 *
 * [endOfSeconds]의 단수형 별칭입니다.
 */
fun ZonedDateTime.endOfSecond(): ZonedDateTime = endOfSeconds()

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
fun startOfYear(year: Int, zoneId: ZoneId = ZoneOffset.UTC): ZonedDateTime =
    zonedDateTimeOf(year, 1, 1, zoneId = zoneId)

/**
 * [year]의 마지막 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun endOfYear(year: Int, zoneId: ZoneId = ZoneOffset.UTC): ZonedDateTime =
    startOfYear(year, zoneId).plusYears(1).minusNanos(1)

/**
 * [year],[monthOfYear]의 분기 시작 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun startOfQuarter(year: Int, monthOfYear: Int, zoneId: ZoneId = ZoneOffset.UTC): ZonedDateTime =
    startOfQuarter(year, Quarter.ofMonth(monthOfYear), zoneId)

/**
 * [year],[quarter]의 분기 시작 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun startOfQuarter(year: Int, quarter: Quarter, zoneId: ZoneId = ZoneOffset.UTC): ZonedDateTime =
    zonedDateTimeOf(year, quarter.startMonth, 1, zoneId = zoneId)

/**
 * [year], [monthOfYear]의 분기 마지막 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun endOfQuarter(year: Int, monthOfYear: Int, zoneId: ZoneId = ZoneOffset.UTC): ZonedDateTime =
    endOfQuarter(year, Quarter.ofMonth(monthOfYear), zoneId)

/**
 * [year], [quarter]의 분기 마지막 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun endOfQuarter(year: Int, quarter: Quarter, zoneId: ZoneId = ZoneOffset.UTC): ZonedDateTime =
    zonedDateTimeOf(year, quarter.endMonth, 1, zoneId = zoneId).plusMonths(1).minusNanos(1)

/**
 * [year], [monthOfYear]의 월 시작 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun startOfMonth(year: Int, monthOfYear: Int, zoneId: ZoneId = ZoneOffset.UTC): ZonedDateTime =
    zonedDateTimeOf(year, monthOfYear, 1, zoneId = zoneId)

/**
 * [year], [monthOfYear]의 월 마지막 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun endOfMonth(year: Int, monthOfYear: Int, zoneId: ZoneId = ZoneOffset.UTC): ZonedDateTime =
    startOfMonth(year, monthOfYear, zoneId).plusMonths(1).minusNanos(1)

/**
 * [year], [monthOfYear]의 월의 날짜 수를 반환합니다. (예: 31일, 30일, 28일, 29일)
 */
fun lengthOfMonth(year: Int, monthOfYear: Int): Int =
    YearMonth.of(year, monthOfYear).lengthOfMonth()

/**
 * [year], [monthOfYear], [dayOfMonth]의 주(week) 단위 시작 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun startOfWeek(year: Int, monthOfYear: Int, dayOfMonth: Int, zoneId: ZoneId = ZoneOffset.UTC): ZonedDateTime {
    val date = zonedDateTimeOf(year, monthOfYear, dayOfMonth, zoneId = zoneId)
    return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

/**
 * [year], [monthOfYear], [dayOfMonth]의 주(week) 단위 마지막 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun endOfWeek(year: Int, monthOfYear: Int, dayOfMonth: Int, zoneId: ZoneId = ZoneOffset.UTC): ZonedDateTime =
    startOfWeek(year, monthOfYear, dayOfMonth, zoneId).plusDays(DaysPerWeek.toLong()).minusNanos(1)

/**
 * [year], 주차([weekOfWeekyear])의 시작 시각을 [ZonedDateTime]으로 반환합니다.
 *
 * ISO-8601 규칙(주 시작: MONDAY)을 따릅니다.
 */
fun startOfWeekOfWeekyear(
    weekyear: Int,
    weekOfWeekyear: Int,
    zoneId: ZoneId = ZoneOffset.UTC,
): ZonedDateTime =
    ZonedDateTime.of(LocalDate.of(weekyear, 1, 4), LocalTime.MIDNIGHT, zoneId)
        .with(IsoFields.WEEK_BASED_YEAR, weekyear.toLong())
        .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, weekOfWeekyear.toLong())
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/**
 * [year], 주차([weekOfWeekyear])의 마지막 시각을 [ZonedDateTime]으로 반환합니다.
 */
fun endOfWeekOfWeekyear(
    weekyear: Int,
    weekOfWeekyear: Int,
    zoneId: ZoneId = ZoneOffset.UTC,
): ZonedDateTime =
    startOfWeekOfWeekyear(weekyear, weekOfWeekyear, zoneId)
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
 *
 * ```kotlin
 * val a = zonedDateTimeOf(2024, 1, 1)
 * val b = zonedDateTimeOf(2024, 6, 1)
 * (a min b) // 2024-01-01T00:00:00Z
 * (null min b) // b
 * ```
 *
 * @return 더 작은 [ZonedDateTime], 둘 다 null이면 null
 */
infix fun ZonedDateTime?.min(that: ZonedDateTime?): ZonedDateTime? = when {
    this == null -> that
    that == null -> this
    this < that  -> this
    else         -> that
}

/**
 * 두 개의 [ZonedDateTime] 중 더 큰 값을 반환합니다
 *
 * ```kotlin
 * val a = zonedDateTimeOf(2024, 1, 1)
 * val b = zonedDateTimeOf(2024, 6, 1)
 * (a max b) // 2024-06-01T00:00:00Z
 * (null max a) // a
 * ```
 *
 * @return 더 큰 [ZonedDateTime], 둘 다 null이면 null
 */
infix fun ZonedDateTime?.max(that: ZonedDateTime?): ZonedDateTime? = when {
    this == null -> that
    that == null -> this
    this > that  -> this
    else         -> that
}

/**
 * [ZonedDateTime]가 [that]과 같은지 여부를 반환합니다.
 *
 * 시간대가 다르더라도 동일한 시각을 가리키면 `true`를 반환합니다.
 *
 * ```kotlin
 * val utc   = zonedDateTimeOf(2024, 1, 1, 0, 0, 0, zoneId = ZoneOffset.UTC)
 * val odt   = OffsetDateTime.of(2024, 1, 1, 9, 0, 0, 0, ZoneOffset.ofHours(9))
 * utc.equalTo(odt) // true (UTC 00:00 == KST 09:00)
 * ```
 *
 * @param that 비교할 [OffsetDateTime]
 * @return 동일한 시각이면 `true`
 */
fun ZonedDateTime.equalTo(that: OffsetDateTime): Boolean =
    this.toOffsetDateTime().isEqual(that)

/**
 * [ZonedDateTime]가 [that]과 초 단위까지 같은지 여부를 반환합니다.
 *
 * 나노초 이하 차이를 무시하고 초 단위까지 동일하면 `true`를 반환합니다.
 *
 * ```kotlin
 * val a = zonedDateTimeOf(2024, 1, 1, 12, 0, 30, 100)
 * val b = zonedDateTimeOf(2024, 1, 1, 12, 0, 30, 999)
 * a.equalToSeconds(b) // true (나노초 차이 무시)
 * ```
 *
 * @param that 비교할 [ZonedDateTime]
 * @return 초 단위까지 동일하면 `true`, 어느 한쪽이 null이면 `false`
 */
fun ZonedDateTime?.equalToSeconds(that: ZonedDateTime?): Boolean = when {
    (this == null || that == null) -> false
    else                           -> this.truncatedTo(ChronoUnit.SECONDS).isEqual(that.truncatedTo(ChronoUnit.SECONDS))
}

/**
 * [ZonedDateTime]가 [that]과 밀리초 단위까지 같은지 여부를 반환합니다.
 *
 * 밀리초 이하(마이크로초·나노초) 차이를 무시하고 밀리초 단위까지 동일하면 `true`를 반환합니다.
 *
 * ```kotlin
 * val a = zonedDateTimeOf(2024, 1, 1, 12, 0, 30, 123_000_000)
 * val b = zonedDateTimeOf(2024, 1, 1, 12, 0, 30, 123_999_999)
 * a.equalToMillis(b) // true (마이크로초·나노초 차이 무시)
 * ```
 *
 * @param that 비교할 [ZonedDateTime]
 * @return 밀리초 단위까지 동일하면 `true`, 어느 한쪽이 null이면 `false`
 */
fun ZonedDateTime?.equalToMillis(that: ZonedDateTime?): Boolean = when {
    (this == null || that == null) -> false
    else                           -> this.truncatedTo(ChronoUnit.MILLIS).isEqual(that.truncatedTo(ChronoUnit.MILLIS))
}
