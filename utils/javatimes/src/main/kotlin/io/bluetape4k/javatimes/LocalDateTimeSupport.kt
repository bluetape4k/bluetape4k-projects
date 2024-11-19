package io.bluetape4k.javatimes

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.Period
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

/**
 * [LocalDateTime]을 빌드합니다.
 *
 * ```
 * val localDateTime1 = localDateTimeOf(2021, 3, 1) // 2021-03-01T00:00:00
 * val localDateTime2 = localDateTimeOf(2021, 3, 1, 12, 30) // 2021-03-01T12:30:00
 * ```
 */
fun localDateTimeOf(
    year: Int,
    monthOfYear: Int = 1,
    dayOfMonth: Int = 1,
    hourOfDay: Int = 0,
    minuteOfHour: Int = 0,
    secondOfMinute: Int = 0,
    milliOfSecond: Int = 0,
): LocalDateTime =
    LocalDateTime.of(
        year,
        monthOfYear,
        dayOfMonth,
        hourOfDay,
        minuteOfHour,
        secondOfMinute,
        milliOfSecond.millisToNanos()
    )

/**
 * [LocalDateTime]을 [Date]로 변환합니다.
 *
 * ```
 * val localDateTime = localDateTimeOf(2021, 3, 1) // 2021-03-01T00:00:00
 * val date = localDateTime.toDate() // 2021-03-01T00:00:00
 * ```
 */
fun LocalDateTime.toDate(): Date = Date.from(toInstant())

/**
 * [LocalDateTime]을 [Instant]로 변환합니다.
 *
 * ```
 * val localDateTime = localDateTimeOf(2021, 3, 1) // 2021-03-01T00:00:00
 * val instant = localDateTime.toInstant() // 2021-03-01T00:00:00
 * ```
 */
fun LocalDateTime.toInstant(): Instant = toZonedDateTime(ZoneOffset.UTC).toInstant()


/**
 * [LocalDateTime]을 [OffsetDateTime]로 변환합니다.
 *
 * ```
 * val localDateTime = localDateTimeOf(2021, 3, 1) // 2021-03-01T00:00:00
 * val offsetDateTime = localDateTime.toOffsetDateTime() // 2021-03-01T00:00:00+00:00
 * ```
 *
 * @param offset 변환할 zone offset (기본 값: [SystemOffset])
 */
fun LocalDateTime.toOffsetDateTime(offset: ZoneOffset = SystemOffset) = OffsetDateTime.of(this, offset)

/**
 * [LocalDateTime]을 [ZonedDateTime]로 변환합니다.
 *
 * ```
 * val localDateTime = localDateTimeOf(2021, 3, 1) // 2021-03-01T00:00:00
 * val zonedDateTime = localDateTime.toZonedDateTime() // 2021-03-01T00:00:00+00:00
 * ```
 *
 * @param offset 변환할 zone offset (기본 값: [SystemOffset])
 */
fun LocalDateTime.toZonedDateTime(offset: ZoneOffset = SystemOffset) = ZonedDateTime.of(this, offset)

//fun LocalDateTime.startOfYear(): LocalDateTime = this.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS)
//fun LocalDateTime.startOfMonth(): LocalDateTime = this.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)
//fun LocalDateTime.startOfWeek(): LocalDateTime = startOfDay() - (dayOfWeek.value - DayOfWeek.MONDAY.value).days()
//fun LocalDateTime.startOfDay(): LocalDateTime = this.truncatedTo(ChronoUnit.DAYS)


/**
 * [LocalDate]을 빌드합니다.
 *
 * ```
 * val localDate1 = localDateOf(2021, 3, 1) // 2021-03-01
 * val localDate2 = localDateOf(2021, 3) // 2021-03-01
 * val localDate3 = localDateOf(2021) // 2021-01-01
 * ```
 *
 * @param year 년도
 * @param monthOfYear 월 (기본 값: 1)
 * @param dayOfMonth 일 (기본 값: 1)
 */
fun localDateOf(
    year: Int,
    monthOfYear: Int = 1,
    dayOfMonth: Int = 1,
): LocalDate = LocalDate.of(year, monthOfYear, dayOfMonth)

/**
 * [LocalDate]을 [Date]로 변환합니다.
 *
 * ```
 * val localDate = localDateOf(2021, 3, 1) // 2021-03-01
 * val date = localDate.toDate() // 2021-03-01
 * ```
 */
fun LocalDate.toDate(): Date = Date.from(toInstant())

/**
 * [LocalDate]을 [Instant]로 변환합니다.
 *
 * ```
 * val localDate = localDateOf(2021, 3, 1) // 2021-03-01
 * val instant = localDate.toInstant() // 2021-03-01
 * ```
 */
fun LocalDate.toInstant(): Instant = Instant.ofEpochMilli(toEpochMillis())

//fun LocalDate.startOfMonth(): LocalDate = withDayOfMonth(1)
// fun LocalDate.startOfWeek(): LocalDate = this - (dayOfWeek.value - DayOfWeek.MONDAY.value).days()


/**
 * 두 [LocalDate]의 기간을 계산합니다.
 *
 * ```
 * val start = localDateOf(2021, 3, 1) // 2021-03-01
 * val end = localDateOf(2021, 3, 31) // 2021-03-31
 * val period = start.between(end) // P30D
 * ```
 */
fun LocalDate.between(endExclusive: LocalDate): Period = Period.between(this, endExclusive)

/**
 * [LocalTime]을 빌드합니다.
 *
 * ```
 * val localTime1 = localTimeOf(0, 0) // 00:00:00
 * val localTime2 = localTimeOf(12, 30) // 12:30:00
 * ```
 *
 * @param hourOfDay 시간
 * @param minuteOfHour 분 (기본 값: 0)
 * @param secondOfMinute 초 (기본 값: 0)
 * @param milliOfSecond 밀리초 (기본 값: 0)
 */
fun localTimeOf(
    hourOfDay: Int,
    minuteOfHour: Int = 0,
    secondOfMinute: Int = 0,
    milliOfSecond: Int = 0,
): LocalTime =
    LocalTime.of(hourOfDay, minuteOfHour, secondOfMinute, milliOfSecond.millisToNanos())

/**
 * [LocalTime]을 [Instant]로 변환합니다.
 *
 * ```
 * val localTime = localTimeOf(0, 0) // 00:00:00
 * val instant = localTime.toInstant() // 00:00:00
 * ```
 */
fun LocalTime.toInstant(): Instant = Instant.from(this)
