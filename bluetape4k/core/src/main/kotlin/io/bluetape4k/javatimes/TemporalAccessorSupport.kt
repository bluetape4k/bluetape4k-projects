package io.bluetape4k.javatimes

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.TemporalAccessor
import java.time.temporal.TemporalField
import java.time.temporal.TemporalQueries
import java.time.temporal.TemporalUnit

/**
 * ISO 형식의 날짜/시각 포맷터입니다.
 */
private val DefaultIsoInstantFormatter = DateTimeFormatterBuilder()
    .parseCaseInsensitive()
    .appendInstant(3)
    .toFormatter()

/**
 * 날짜를 ISO 형식([DefaultIsoInstantFormatter])의 문자열로 만듭니다.
 *
 * 예:
 * - Instant        : `2020-10-14T06:55:44.123Z`
 * - LocalDateTime  : `2020-10-14T06:55:44.123Z`
 * - OffsetDateTime : `2020-10-14T06:55:44.123+09:00`
 * - ZonedDateTime  : `2020-10-14T06:55:44.000000123+09:00[Asia/Seoul]`
 */
fun TemporalAccessor.toIsoInstantString(): String = DefaultIsoInstantFormatter.format(this)

/**
 * [TemporalAccessor]가
 * - [Instant]인 경우에는 [DefaultIsoInstantFormatter] 포맷으로,
 * - 다른 경우 ISO 형식([DateTimeFormatter.ISO_DATE_TIME])의
 * 문자열로 만듭니다.
 *
 * 예:
 * * Instant        : `2020-10-14T06:55:44.123Z`
 * * LocalDateTime  : `2020-10-14T06:55:44.123`
 * * OffsetDateTime : `2020-10-14T06:55:44.123+09:00`
 * * ZonedDateTime  : `2020-10-14T06:55:44.000000123+09:00[Asia/Seoul]`
 */
fun TemporalAccessor.toIsoString(): String = when (this) {
    is Instant -> DefaultIsoInstantFormatter.format(this)
    else       -> DateTimeFormatter.ISO_DATE_TIME.format(this)
}

/**
 * 일자를 ISO 형식([DateTimeFormatter.ISO_DATE])의 문자열로 만듭니다.
 *
 * 예:
 * * LocalDateTime  : `2020-10-14`
 * * OffsetDateTime : `2020-10-14+09:00`
 * * ZonedDateTime  : `2020-10-14+09:00`
 */
fun TemporalAccessor.toIsoDateString(): String = DateTimeFormatter.ISO_DATE.format(this)

/**
 * 시각을 ISO 형식([DateTimeFormatter.ISO_TIME])의 문자열로 만듭니다.
 *
 * 예:
 * * LocalDateTime  : `06:55:44.123`
 * * OffsetDateTime : `06:55:44.123+09:00`
 * * ZonedDateTime  : `06:55:44.000000123+09:00`
 */
fun TemporalAccessor.toIsoTimeString(): String = DateTimeFormatter.ISO_TIME.format(this)

/**
 * 날짜를 ISO 형식의 [DateTimeFormatter.ISO_LOCAL_DATE_TIME] 형식으로 표현합니다.
 *
 * 예: '2011-12-03T10:15:30'
 */
fun TemporalAccessor.toIsoLocalString(): String = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(this)

/**
 * 일자을 ISO 형식의 [DateTimeFormatter.ISO_LOCAL_DATE] 형식으로 표현합니다.
 *
 * 예: Date: '2011-12-03'
 */
fun TemporalAccessor.toIsoLocalDateString(): String = DateTimeFormatter.ISO_LOCAL_DATE.format(this)

/**
 * 시각을 ISO 형식의 [DateTimeFormatter.ISO_LOCAL_TIME] 형식으로 표현합니다.
 *
 * 예: '10:15', '10:15:30'
 */
fun TemporalAccessor.toIsoLocalTimeString(): String = DateTimeFormatter.ISO_LOCAL_TIME.format(this)

/**
 * 날짜를 ISO 형식의 [DateTimeFormatter.ISO_OFFSET_DATE_TIME] 형식으로 표현합니다.
 *
 * 예: '2011-12-03T10:15:30+01:00'
 */
fun TemporalAccessor.toIsoOffsetDateTimeString(): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(this)

/**
 * 날짜를 ISO 형식의 [DateTimeFormatter.ISO_OFFSET_DATE] 형식으로 표현합니다.
 *
 * 예: '2011-12-03+01:00'
 */
fun TemporalAccessor.toIsoOffsetDateString(): String = DateTimeFormatter.ISO_OFFSET_DATE.format(this)

/**
 * 시각을 ISO 형식의 [DateTimeFormatter.ISO_OFFSET_TIME] 형식으로 표현합니다.
 *
 * 예: '10:15:30+01:00'
 */
fun TemporalAccessor.toIsoOffsetTimeString(): String = DateTimeFormatter.ISO_OFFSET_TIME.format(this)

/**
 * 날짜를 ISO 형식의 [DateTimeFormatter.ISO_ZONED_DATE_TIME] 형식으로 표현합니다.
 *
 * 예: '2011-12-03T10:15:30+01:00[Europe/Paris]'
 */
fun TemporalAccessor.toIsoZonedDateTimeString(): String = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(this)


// Queries

val TemporalAccessor.precision: TemporalUnit? get() = query(TemporalQueries.precision())
val TemporalAccessor.year: Year? get() = query { Year.from(this) }
val TemporalAccessor.yearMonth: YearMonth? get() = query { YearMonth.from(this) }
val TemporalAccessor.month: Month? get() = query { Month.from(this) }
val TemporalAccessor.monthDay: MonthDay? get() = query { MonthDay.from(this) }
val TemporalAccessor.dayOfWeek: DayOfWeek? get() = query { DayOfWeek.from(this) }
val TemporalAccessor.instant: Instant? get() = query { Instant.from(this) }
val TemporalAccessor.localDate: LocalDate? get() = query { LocalDate.from(this) }
val TemporalAccessor.localTime: LocalTime? get() = query { LocalTime.from(this) }
val TemporalAccessor.localDateTime: LocalDateTime? get() = query { LocalDateTime.from(this) }
val TemporalAccessor.zoneOffset: ZoneOffset? get() = query { ZoneOffset.from(this) }
val TemporalAccessor.offsetTime: OffsetTime? get() = query { OffsetTime.from(this) }
val TemporalAccessor.offsetDateTime: OffsetDateTime? get() = query { OffsetDateTime.from(this) }
val TemporalAccessor.zone: ZoneId? get() = query { ZoneId.from(this) }
val TemporalAccessor.zoneId: ZoneId? get() = query { ZoneId.from(this) }
val TemporalAccessor.zonedDateTime: ZonedDateTime? get() = query { ZonedDateTime.from(this) }


/**
 * [TemporalAccessor]가 [TemporalField]를 지원하는지 확인합니다.
 *
 * ```
 * val now = nowInstant()
 * now.supports(ChronoField.INSTANT_SECONDS).shouldBeTrue()
 * ```
 */
infix fun TemporalAccessor.supports(temporalField: TemporalField) = isSupported(temporalField)
