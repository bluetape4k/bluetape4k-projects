package io.bluetape4k.javatimes

import java.time.Clock
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * 현 시각을 [Instant]로 반환합니다.
 *
 * ```
 * val now = nowInstant()  // 현재 시각
 * ```
 *
 * @return zoneId 시간대 (기본값: [SystemZoneId])
 */
fun nowInstant(zoneId: ZoneId = SystemZoneId): Instant = Instant.now(Clock.system(zoneId))

/**
 * 현 시각을 [LocalTime]으로 반환합니다.
 *
 * ```
 * val now = nowLocalTime()  // 현재 시각
 * ```
 *
 * @return zoneId 시간대 (기본값: [SystemZoneId])
 */
fun nowLocalTime(zoneId: ZoneId = SystemZoneId): LocalTime = LocalTime.now(zoneId)

/**
 * 현 시각을 [LocalDate]로 반환합니다.
 *
 * ```
 * val now = nowLocalDate()  // 현재 시각
 * ```
 *
 * @return zoneId 시간대 (기본값: [SystemZoneId])
 */
fun nowLocalDate(zoneId: ZoneId = SystemZoneId): LocalDate = LocalDate.now(zoneId)

/**
 * 현 시각을 [LocalDateTime]로 반환합니다.
 *
 * ```
 * val now = nowLocalDateTime()  // 현재 시각
 * ```
 *
 * @return zoneId 시간대 (기본값: [SystemZoneId])
 */
fun nowLocalDateTime(zoneId: ZoneId = SystemZoneId): LocalDateTime = LocalDateTime.now(zoneId)

/**
 * 현 시각을 [OffsetDateTime]로 반환합니다.
 *
 * ```
 * val now = nowOffsetDateTime()  // 현재 시각
 * ```
 *
 * @return zoneId 시간대 (기본값: [SystemZoneId])
 */
fun nowOffsetDateTime(zoneId: ZoneId = SystemZoneId): OffsetDateTime = OffsetDateTime.now(zoneId)

/**
 * 현 시각을 [OffsetTime]으로 반환합니다.
 *
 * ```
 * val now = nowOffsetTime()  // 현재 시각
 * ```
 *
 * @return zoneId 시간대 (기본값: [SystemZoneId])
 */
fun nowOffsetTime(zoneId: ZoneId = SystemZoneId): OffsetTime = OffsetTime.now(zoneId)

/**
 * 현 시각을 [ZonedDateTime]로 반환합니다.
 *
 * ```
 * val now = nowZonedDateTime()  // 현재 시각
 * ```
 *
 * @return zoneId 시간대 (기본값: [SystemZoneId])
 */
fun nowZonedDateTime(zoneId: ZoneId = SystemZoneId): ZonedDateTime = ZonedDateTime.now(zoneId)

/**
 * 오늘을 [Instant]로 반환합니다.
 *
 * ```
 * val today = todayInstant()  // 오늘
 * ```
 *
 * @return zoneId 시간대 (기본값: [SystemZoneId])
 */
fun todayInstant(zonedId: ZoneId = SystemZoneId): Instant = nowInstant(zonedId).truncatedTo(ChronoUnit.DAYS)

/**
 * 오늘을 [LocalDate]으로 로 반환합니다.
 *
 * ```
 * val today = todayLocalDate()  // 오늘
 * ```
 *
 * @return zoneId 시간대 (기본값: [SystemZoneId])
 */
fun todayLocalDate(zoneId: ZoneId = SystemZoneId): LocalDate =
    nowLocalDate(zoneId)


/**
 * 오늘을 [LocalDateTime]으로 로 반환합니다.
 *
 * ```
 * val today = todayLocalDateTime()  // 오늘
 * ```
 *
 * @return zoneId 시간대 (기본값: [SystemZoneId])
 */
fun todayLocalDateTime(zoneId: ZoneId = SystemZoneId): LocalDateTime =
    nowLocalDateTime(zoneId).truncatedTo(ChronoUnit.DAYS)

/**
 * 오늘을 [OffsetDateTime]으로 반환합니다.
 *
 * ```
 * val today = todayOffsetDateTime()  // 오늘
 * ```
 *
 * @return zoneId 시간대 (기본값: [SystemZoneId])
 */
fun todayOffsetDateTime(zoneId: ZoneId = SystemZoneId): OffsetDateTime = nowOffsetDateTime(zoneId).truncatedTo(
    ChronoUnit.DAYS
)

/**
 * 오늘을 [ZonedDateTime]으로 반환합니다.
 *
 * ```
 * val today = todayZonedDateTime()  // 오늘
 * ```
 *
 * @return zoneId 시간대 (기본값: [SystemZoneId])
 */
fun todayZonedDateTime(zoneId: ZoneId = SystemZoneId): ZonedDateTime =
    nowZonedDateTime(zoneId).truncatedTo(ChronoUnit.DAYS)

/**
 * [Int] 값을 나노초 단위의 [Duration]으로 변환합니다.
 */
fun Int.nanos(): Duration = Duration.ofNanos(this.toLong())

/**
 * [Int] 값을 마이크로초 단위의 [Duration]으로 변환합니다.
 */
fun Int.micros(): Duration = Duration.ofNanos(this.toLong() * 1000L)

/**
 * [Int] 값을 밀리초 단위의 [Duration]으로 변환합니다.
 */
fun Int.millis(): Duration = Duration.ofMillis(this.toLong())

/**
 * [Int] 값을 초 단위의 [Duration]으로 변환합니다.
 */
fun Int.seconds(): Duration = Duration.ofSeconds(this.toLong())

/**
 * [Int] 값을 분 단위의 [Duration]으로 변환합니다.
 */
fun Int.minutes(): Duration = Duration.ofMinutes(this.toLong())

/**
 * [Int] 값을 시간 단위의 [Duration]으로 변환합니다.
 */
fun Int.hours(): Duration = Duration.ofHours(this.toLong())

/**
 * [Int] 값을 일 단위의 [Duration]으로 변환합니다.
 */
fun Int.days(): Duration = Duration.ofDays(this.toLong())

/**
 * [Int] 값을 주 단위의 [Duration]으로 변환합니다.
 */
fun Int.weeks(): Duration = (this * DaysPerWeek).days()

/**
 * [Int] 값을 일 단위의 [Period]로 변환합니다.
 */
fun Int.dayPeriod(): Period = Period.ofDays(this)

/**
 * [Int] 값을 주 단위의 [Period]로 변환합니다.
 */
fun Int.weekPeriod(): Period = Period.ofWeeks(this)

/**
 * [Int] 값을 월 단위의 [Period]로 변환합니다.
 */
fun Int.monthPeriod(): Period = Period.ofMonths(this)

/**
 * [Int] 값을 분기 단위의 [Period]로 변환합니다.
 */
fun Int.quarterPeriod(): Period = Period.ofMonths(this * MonthsPerQuarter)

/**
 * [Int] 값을 년 단위의 [Period]로 변환합니다.
 */
fun Int.yearPeriod(): Period = Period.ofYears(this)

/**
 * 밀리초를 나노초로 변환합니다.
 */
fun Int.millisToNanos(): Int = (this * 1e6).toInt()

/**
 * 해당 연도가 윤년인지 여부를 반환합니다.
 */
fun Int.isLeapYear(): Boolean = Year.of(this).isLeap

/**
 * [Int]와 [Duration]을 곱합니다.
 */
operator fun Int.times(duration: Duration): Duration = duration.multipliedBy(this.toLong())

/**
 * [Int]와 [Period]를 곱합니다.
 */
operator fun Int.times(period: Period): Period = period.multipliedBy(this)

/**
 * [Duration]에 정수를 곱합니다.
 */
operator fun Duration.times(scalar: Int): Duration = multipliedBy(scalar.toLong())

/**
 * [Period]에 정수를 곱합니다.
 */
operator fun Period.times(scalar: Int): Period = multipliedBy(scalar)

/**
 * [Duration]을 정수로 나눕니다.
 */
operator fun Duration.div(scalar: Int): Duration = dividedBy(scalar.toLong())

/**
 * [Period]를 정수로 나눕니다.
 */
operator fun Period.div(scalar: Int): Period = Period.of(years / scalar, months / scalar, days / scalar)

/**
 * [Int] 값을 epoch 밀리초로 간주하여 [Instant]를 생성합니다.
 */
fun Int.toInstant(): Instant = Instant.ofEpochMilli(this.toLong())


/**
 * [Long] 값을 나노초 단위의 [Duration]으로 변환합니다.
 */
fun Long.nanos(): Duration = Duration.ofNanos(this)

/**
 * [Long] 값을 마이크로초 단위의 [Duration]으로 변환합니다.
 */
fun Long.micros(): Duration = Duration.ofNanos(this * 1000L)

/**
 * [Long] 값을 밀리초 단위의 [Duration]으로 변환합니다.
 */
fun Long.millis(): Duration = Duration.ofMillis(this)

/**
 * [Long] 값을 초 단위의 [Duration]으로 변환합니다.
 */
fun Long.seconds(): Duration = Duration.ofSeconds(this)

/**
 * [Long] 값을 분 단위의 [Duration]으로 변환합니다.
 */
fun Long.minutes(): Duration = Duration.ofMinutes(this)

/**
 * [Long] 값을 시간 단위의 [Duration]으로 변환합니다.
 */
fun Long.hours(): Duration = Duration.ofHours(this)

/**
 * [Long] 값을 일 단위의 [Duration]으로 변환합니다.
 */
fun Long.days(): Duration = Duration.ofDays(this)

/**
 * [Long] 값을 주 단위의 [Duration]으로 변환합니다.
 */
fun Long.weeks(): Duration = (this * DaysPerWeek).days()

/**
 * [Long] 값을 일 단위의 [Period]로 변환합니다.
 */
fun Long.dayPeriod(): Period = Period.ofDays(this.toInt())

/**
 * [Long] 값을 주 단위의 [Period]로 변환합니다.
 */
fun Long.weekPeriod(): Period = Period.ofWeeks(this.toInt())

/**
 * [Long] 값을 월 단위의 [Period]로 변환합니다.
 */
fun Long.monthPeriod(): Period = Period.ofMonths(this.toInt())

/**
 * [Long] 값을 분기 단위의 [Period]로 변환합니다.
 */
fun Long.quarterPeriod(): Period = Period.ofMonths(this.toInt() * MonthsPerQuarter)

/**
 * [Long] 값을 년 단위의 [Period]로 변환합니다.
 */
fun Long.yearPeriod(): Period = Period.ofYears(this.toInt())

/**
 * 밀리초를 나노초로 변환합니다.
 */
fun Long.millisToNanos(): Int = (this * NanosPerMillis).toInt()

/**
 * 해당 연도가 윤년인지 여부를 반환합니다.
 */
fun Long.isLeapYear(): Boolean = Year.of(this.toInt()).isLeap

/**
 * [Long]과 [Duration]을 곱합니다.
 */
operator fun Long.times(duration: Duration): Duration = duration.multipliedBy(this)

/**
 * [Long]과 [Period]를 곱합니다.
 */
operator fun Long.times(period: Period): Period = period.multipliedBy(this.toInt())

/**
 * [Duration]에 정수를 곱합니다.
 */
operator fun Duration.times(scalar: Long): Duration = multipliedBy(scalar)

/**
 * [Period]에 정수를 곱합니다.
 */
operator fun Period.times(scalar: Long): Period = multipliedBy(scalar.toInt())

/**
 * [Duration]을 정수로 나눕니다.
 */
operator fun Duration.div(scalar: Long): Duration = dividedBy(scalar)

/**
 * [Period]를 정수로 나눕니다.
 */
operator fun Period.div(scalar: Long): Period =
    Period.of(years / scalar.toInt(), months / scalar.toInt(), days / scalar.toInt())

/**
 * [Long] 값을 epoch 밀리초로 간주하여 [Instant]를 생성합니다.
 */
fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)

/**
 * 연도와 월로 [YearMonth]를 생성합니다.
 */
fun yearMonthOf(year: Int, month: Month): YearMonth = YearMonth.of(year, month)

/**
 * 연도와 월 번호로 [YearMonth]를 생성합니다.
 */
fun yearMonthOf(year: Int, monthOfYear: Int): YearMonth = YearMonth.of(year, monthOfYear)

/**
 * 월과 일로 [MonthDay]를 생성합니다.
 */
fun monthDayOf(monthOfYear: Int, dayOfMonth: Int): MonthDay = MonthDay.of(monthOfYear, dayOfMonth)

/**
 * 월과 일로 [MonthDay]를 생성합니다.
 */
fun monthDayOf(month: Month, dayOfMonth: Int): MonthDay = MonthDay.of(month, dayOfMonth)

/**
 * 현재 시각의 [LocalDate]
 */
val NowLocalDate: LocalDate get() = LocalDate.now()

/**
 * 현재 시각의 [LocalTime]
 */
val NowLocalTime: LocalTime get() = LocalTime.now()

/**
 * 현재 시각의 [LocalDateTime]
 */
val NowLocalDateTime: LocalDateTime get() = LocalDateTime.now()

/**
 * [Duration]의 초 부분을 구조 분해 선언으로 가져옵니다.
 */
operator fun Duration.component1(): Long = this.seconds

/**
 * [Duration]의 나노초 부분을 구조 분해 선언으로 가져옵니다.
 */
operator fun Duration.component2(): Int = this.nano

/**
 * [Period]의 년 부분을 구조 분해 선언으로 가져옵니다.
 */
operator fun Period.component1(): Int = this.years

/**
 * [Period]의 월 부분을 구조 분해 선언으로 가져옵니다.
 */
operator fun Period.component2(): Int = this.months

/**
 * [Period]의 일 부분을 구조 분해 선언으로 가져옵니다.
 */
operator fun Period.component3(): Int = this.days

/**
 * [Year]에 [Month]를 더하여 [YearMonth]를 생성합니다.
 */
operator fun Year.plus(month: Month): YearMonth = atMonth(month)

/**
 * [Year]에 [MonthDay]를 더하여 [LocalDate]를 생성합니다.
 */
operator fun Year.plus(monthDay: MonthDay): LocalDate = atMonthDay(monthDay)

/**
 * [Year]에 년수를 더합니다.
 */
operator fun Year.plus(years: Int): Year = plusYears(years.toLong())

/**
 * [Year]에서 년수를 뺍니다.
 */
operator fun Year.minus(years: Int): Year = minusYears(years.toLong())

/**
 * [Month]에 월수를 더합니다.
 */
operator fun Month.plus(months: Int): Month = plus(months.toLong())

/**
 * [Month]에서 월수를 뺍니다.
 */
operator fun Month.minus(months: Int): Month = minus(months.toLong())

/**
 * [YearMonth]에 [Year]를 더합니다.
 */
operator fun YearMonth.plus(year: Year): YearMonth = plusYears(year.value.toLong())

/**
 * [YearMonth]에 [Month]를 더합니다.
 */
operator fun YearMonth.plus(month: Month): YearMonth = plusMonths(month.value.toLong())

/**
 * [Year]를 1년 증가시킵니다.
 */
operator fun Year.inc(): Year = plusYears(1L)

/**
 * [Year]를 1년 감소시킵니다.
 */
operator fun Year.dec(): Year = minusYears(1L)

/**
 * [Month]를 1개월 증가시킵니다.
 */
operator fun Month.inc(): Month = plus(1L)

/**
 * [Month]를 1개월 감소시킵니다.
 */
operator fun Month.dec(): Month = minus(1L)

/**
 * [DayOfWeek]를 하루 증가시킵니다.
 */
operator fun DayOfWeek.inc(): DayOfWeek = plus(1L)

/**
 * [DayOfWeek]를 하루 감소시킵니다.
 */
operator fun DayOfWeek.dec(): DayOfWeek = minus(1L)
