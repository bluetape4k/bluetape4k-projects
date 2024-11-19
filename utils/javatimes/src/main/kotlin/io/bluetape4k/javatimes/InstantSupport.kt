package io.bluetape4k.javatimes

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.util.*

/**
 * Epoch 시작 [Instant]
 */
@JvmField
val EPOCH: Instant = Instant.EPOCH

/**
 * [epochMillis]를 가지는 [Instant]를 빌드힙니다.
 *
 * ```
 * val instant = instantOf() // 현재 시간
 * val instant = instantOf(1614556800000) // 2021-03-01T00:00:00Z
 * ```
 *
 * @param epochMillis epoch millis
 * @return Instant
 */
fun instantOf(epochMillis: Long = System.currentTimeMillis()): Instant =
    Instant.ofEpochMilli(epochMillis)

/**
 * [Instant]를 [LocalDate]로 변환합니다.
 *
 * ```
 * val instant = instantOf(1614556800000) // 2021-03-01T00:00:00Z
 * val localDate = instant.toLocalDate() // 2021-03-01
 * ```
 */
fun Instant.toLocalDate(): LocalDate =
    LocalDate.ofEpochDay(this.epochSecond / (24 * 3600))

/**
 * [Instant]를 [LocalDateTime]으로 변환합니다.
 *
 * ```
 * val instant = instantOf(1614556800000) // 2021-03-01T00:00:00Z
 * val localDateTime = instant.toLocalDateTime() // 2021-03-01T00:00:00
 * ```
 *
 * @receiver Instant 대상 [Instant]
 * @param zoneId Local에 해당하는 zone (기본 값: [ZoneId.systemDefault()])
 * @return LocalDateTime
 */
fun Instant.toLocalDateTime(zoneId: ZoneId = SystemZoneId): LocalDateTime =
    LocalDateTime.ofInstant(this, zoneId)

/**
 * [Instant]를 [OffsetDateTime]으로 변환합니다.
 *
 * ```
 * val instant = instantOf(1614556800000) // 2021-03-01T00:00:00Z
 * val offsetDateTime = instant.toOffsetDateTime() // 2021-03-01T00:00:00Z
 * ```
 *
 * @receiver Instant 대상 [Instant]
 * @param zoneId Local에 해당하는 zone (기본 값: [ZoneId.systemDefault()])
 * @return OffsetDateTime
 */
fun Instant.toOffsetDateTime(zoneId: ZoneId = SystemZoneId): OffsetDateTime =
    OffsetDateTime.ofInstant(this, zoneId)

/**
 * [Instant]를 [ZonedDateTime]으로 변환합니다.
 *
 * ```
 * val instant = instantOf(1614556800000) // 2021-03-01T00:00:00Z
 * val zonedDateTime = instant.toZonedDateTime() // 2021-03-01T00:00:00Z
 * ```
 *
 * @receiver Instant 대상 [Instant]
 * @param zoneId Local에 해당하는 zone (기본 값: [ZoneId.systemDefault()])
 * @return OffsetDateTime
 */
fun Instant.toZonedDateTime(zoneId: ZoneId = SystemZoneId): ZonedDateTime =
    ZonedDateTime.ofInstant(this, zoneId)

/**
 * [Instant]를 [Date]로 변환합니다.
 *
 * ```
 * val instant = instantOf(1614556800000) // 2021-03-01T00:00:00Z
 * val date = instant.toDate() // 2021-03-01T00:00:00Z
 * ```
 */
fun Instant.toDate(): Date = Date.from(this)

/**
 * [Instant]를 [Calendar]로 변환합니다.
 *
 * ```
 * val instant = instantOf(1614556800000) // 2021-03-01T00:00:00Z
 * val calendar = instant.toCalendar() // 2021-03-01T00:00:00Z
 * ```
 *
 * @receiver Instant
 * @param timeZone Local에 해당하는 time zone (기본 값: [TimeZone.getDefault()])
 * @return Calendar
 */
fun Instant.toCalendar(timeZone: TimeZone = TimeZone.getDefault()): Calendar =
    Calendar.Builder()
        .setInstant(this.toEpochMilli())
        .setTimeZone(timeZone)
        .build()

/**
 * [Instant]에 설정값을 지정하여 새로운 Instant를 생성합니다.
 *
 * ```
 * val instant = instantOf(1614556800000) // 2021-03-01T00:00:00Z
 * val newInstant = instant.with(year = 2022, monthOfYear = 2, dayOfMonth = 1) // 2022-02-01T00:00:00Z
 * ```
 *
 * @receiver Instant
 * @param year Int
 * @param monthOfYear Int
 * @param dayOfMonth Int
 * @param hourOfDay Int
 * @param minuteOfHour Int
 * @param secondOfMinute Int
 * @param millisOfSecond Int
 * @param zoneOffset [ZoneOffset] 인스턴스 (기본: ZoneOffset.UTC)
 * @return Instant
 */
fun Instant.with(
    year: Int,
    monthOfYear: Int = 1,
    dayOfMonth: Int = 1,
    hourOfDay: Int = 0,
    minuteOfHour: Int = 0,
    secondOfMinute: Int = 0,
    millisOfSecond: Int = 0,
    zoneOffset: ZoneOffset = ZoneOffset.UTC,
): Instant =
    LocalDateTime.ofInstant(this, zoneOffset)
        .withYear(year)
        .withMonth(monthOfYear)
        .withDayOfMonth(dayOfMonth)
        .withHour(hourOfDay)
        .withMinute(minuteOfHour)
        .withSecond(secondOfMinute)
        .with(ChronoField.MILLI_OF_SECOND, millisOfSecond.toLong())
        .toInstant(zoneOffset)

/**
 * 두 [Instant] 중 작은 값을 반환한다. 둘 중 null이 있으면 null이 아닌 값을 반환한다
 *
 * ```
 * val instant1 = instantOf(1614556800000) // 2021-03-01T00:00:00Z
 * val instant2 = instantOf(1614556800059) // 2021-03-01T00:00:59Z
 * val minInstant = instant1 min instant2 // 2021-03-01T00:00:00Z
 * ```
 *
 * @receiver Instant?
 * @param that Instant?
 * @return Instant?
 */
infix fun Instant?.min(that: Instant?): Instant? = when {
    this == null -> that
    that == null -> this
    this > that  -> that
    else         -> this
}

/**
 * 두 [Instant] 중 큰 값을 반환한다. 둘 중 null이 있으면 null이 아닌 값을 반환한다
 *
 * ```
 * val instant1 = instantOf(1614556800000) // 2021-03-01T00:00:00Z
 * val instant2 = instantOf(1614556800059) // 2021-03-01T00:00:59Z
 * val maxInstant = instant1 max instant2 // 2021-03-01T00:00:59Z
 * ```
 *
 * @receiver Instant?
 * @param that Instant?
 * @return Instant?
 */
infix fun Instant?.max(that: Instant?): Instant? = when {
    this == null -> that
    that == null -> this
    this < that  -> that
    else         -> this
}
