@file:Suppress("NOTHING_TO_INLINE")

package io.bluetape4k.javatimes

import java.time.DateTimeException
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
 * - OffsetDateTime : `2020-10-13T21:55:44.123Z`
 * - ZonedDateTime  : `2020-10-13T21:55:44.000Z`
 */
fun TemporalAccessor.toIsoInstantString(): String = when (this) {
    is Instant        -> DefaultIsoInstantFormatter.format(this)
    is LocalDateTime  -> DefaultIsoInstantFormatter.format(this.toInstant())
    is OffsetDateTime -> DefaultIsoInstantFormatter.format(this.toInstant())
    is ZonedDateTime  -> DefaultIsoInstantFormatter.format(this.toInstant())
    is LocalDate      -> DefaultIsoInstantFormatter.format(this.atStartOfDay(ZoneOffset.UTC).toInstant())
    else              -> DefaultIsoInstantFormatter.format(Instant.from(this))
}

/**
 * [toIsoInstantString]의 nullable 안전 버전입니다.
 */
fun TemporalAccessor.toIsoInstantStringOrNull(): String? =
    runCatching { toIsoInstantString() }.getOrNull()

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
 * [toIsoString]의 nullable 안전 버전입니다.
 */
fun TemporalAccessor.toIsoStringOrNull(): String? =
    formatOrNull(DateTimeFormatter.ISO_DATE_TIME)

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
 * [toIsoDateString]의 nullable 안전 버전입니다.
 */
fun TemporalAccessor.toIsoDateStringOrNull(): String? =
    formatOrNull(DateTimeFormatter.ISO_DATE)

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
 * [toIsoTimeString]의 nullable 안전 버전입니다.
 */
fun TemporalAccessor.toIsoTimeStringOrNull(): String? =
    formatOrNull(DateTimeFormatter.ISO_TIME)

/**
 * 날짜를 ISO 형식의 [DateTimeFormatter.ISO_LOCAL_DATE_TIME] 형식으로 표현합니다.
 *
 * 예: '2011-12-03T10:15:30'
 */
fun TemporalAccessor.toIsoLocalString(): String = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(this)

/**
 * [toIsoLocalString]의 nullable 안전 버전입니다.
 */
fun TemporalAccessor.toIsoLocalStringOrNull(): String? =
    formatOrNull(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

/**
 * 일자을 ISO 형식의 [DateTimeFormatter.ISO_LOCAL_DATE] 형식으로 표현합니다.
 *
 * 예: Date: '2011-12-03'
 */
fun TemporalAccessor.toIsoLocalDateString(): String = DateTimeFormatter.ISO_LOCAL_DATE.format(this)

/**
 * [toIsoLocalDateString]의 nullable 안전 버전입니다.
 */
fun TemporalAccessor.toIsoLocalDateStringOrNull(): String? =
    formatOrNull(DateTimeFormatter.ISO_LOCAL_DATE)

/**
 * 시각을 ISO 형식의 [DateTimeFormatter.ISO_LOCAL_TIME] 형식으로 표현합니다.
 *
 * 예: '10:15', '10:15:30'
 */
fun TemporalAccessor.toIsoLocalTimeString(): String = DateTimeFormatter.ISO_LOCAL_TIME.format(this)

/**
 * [toIsoLocalTimeString]의 nullable 안전 버전입니다.
 */
fun TemporalAccessor.toIsoLocalTimeStringOrNull(): String? =
    formatOrNull(DateTimeFormatter.ISO_LOCAL_TIME)

/**
 * 날짜를 ISO 형식의 [DateTimeFormatter.ISO_OFFSET_DATE_TIME] 형식으로 표현합니다.
 *
 * 예: '2011-12-03T10:15:30+01:00'
 */
fun TemporalAccessor.toIsoOffsetDateTimeString(): String = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(this)

/**
 * [toIsoOffsetDateTimeString]의 nullable 안전 버전입니다.
 */
fun TemporalAccessor.toIsoOffsetDateTimeStringOrNull(): String? =
    formatOrNull(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

/**
 * 날짜를 ISO 형식의 [DateTimeFormatter.ISO_OFFSET_DATE] 형식으로 표현합니다.
 *
 * 예: '2011-12-03+01:00'
 */
fun TemporalAccessor.toIsoOffsetDateString(): String = DateTimeFormatter.ISO_OFFSET_DATE.format(this)

/**
 * [toIsoOffsetDateString]의 nullable 안전 버전입니다.
 */
fun TemporalAccessor.toIsoOffsetDateStringOrNull(): String? =
    formatOrNull(DateTimeFormatter.ISO_OFFSET_DATE)

/**
 * 시각을 ISO 형식의 [DateTimeFormatter.ISO_OFFSET_TIME] 형식으로 표현합니다.
 *
 * 예: '10:15:30+01:00'
 */
fun TemporalAccessor.toIsoOffsetTimeString(): String = DateTimeFormatter.ISO_OFFSET_TIME.format(this)

/**
 * [toIsoOffsetTimeString]의 nullable 안전 버전입니다.
 */
fun TemporalAccessor.toIsoOffsetTimeStringOrNull(): String? =
    formatOrNull(DateTimeFormatter.ISO_OFFSET_TIME)

/**
 * 날짜를 ISO 형식의 [DateTimeFormatter.ISO_ZONED_DATE_TIME] 형식으로 표현합니다.
 *
 * 예: '2011-12-03T10:15:30+01:00[Europe/Paris]'
 */
fun TemporalAccessor.toIsoZonedDateTimeString(): String = DateTimeFormatter.ISO_ZONED_DATE_TIME.format(this)

/**
 * [toIsoZonedDateTimeString]의 nullable 안전 버전입니다.
 */
fun TemporalAccessor.toIsoZonedDateTimeStringOrNull(): String? =
    formatOrNull(DateTimeFormatter.ISO_ZONED_DATE_TIME)


// Queries

/**
 * [TemporalAccessor]가 제공하는 최소 정밀도 단위를 조회합니다.
 *
 * ## 동작/계약
 * - [TemporalQueries.precision] 질의를 수행합니다.
 * - 질의 결과가 없으면 null을 반환할 수 있습니다.
 *
 * ```kotlin
 * val p = Instant.now().precision
 * // p != null
 * ```
 */
val TemporalAccessor.precision: TemporalUnit? get() = query(TemporalQueries.precision())

/**
 * [TemporalAccessor]에서 [Year]를 안전하게 추출합니다.
 *
 * ## 동작/계약
 * - 변환 불가 시 예외를 던지지 않고 null을 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val y = LocalDate.now().year
 * // y != null
 * ```
 */
val TemporalAccessor.year: Year? get() = queryOrNull { Year.from(this) }

/**
 * [TemporalAccessor]에서 [YearMonth]를 안전하게 추출합니다.
 *
 * ## 동작/계약
 * - 변환 불가 시 예외를 던지지 않고 null을 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val ym = LocalDate.now().yearMonth
 * // ym != null
 * ```
 */
val TemporalAccessor.yearMonth: YearMonth? get() = queryOrNull { YearMonth.from(this) }

/**
 * [TemporalAccessor]에서 [Month]를 안전하게 추출합니다.
 *
 * ## 동작/계약
 * - 변환 불가 시 예외를 던지지 않고 null을 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val m = LocalDate.now().month
 * // m != null
 * ```
 */
val TemporalAccessor.month: Month? get() = queryOrNull { Month.from(this) }

/**
 * [TemporalAccessor]에서 [MonthDay]를 안전하게 추출합니다.
 *
 * ## 동작/계약
 * - 변환 불가 시 예외를 던지지 않고 null을 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val md = LocalDate.now().monthDay
 * // md != null
 * ```
 */
val TemporalAccessor.monthDay: MonthDay? get() = queryOrNull { MonthDay.from(this) }

/**
 * [TemporalAccessor]에서 [DayOfWeek]를 안전하게 추출합니다.
 *
 * ## 동작/계약
 * - 변환 불가 시 예외를 던지지 않고 null을 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val dow = LocalDate.now().dayOfWeek
 * // dow != null
 * ```
 */
val TemporalAccessor.dayOfWeek: DayOfWeek? get() = queryOrNull { DayOfWeek.from(this) }

/**
 * [TemporalAccessor]에서 [Instant]를 안전하게 추출합니다.
 *
 * ## 동작/계약
 * - 변환 불가 시 예외를 던지지 않고 null을 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val instant = Instant.now().instant
 * // instant != null
 * ```
 */
val TemporalAccessor.instant: Instant? get() = queryOrNull { Instant.from(this) }

/**
 * [TemporalAccessor]에서 [LocalDate]를 안전하게 추출합니다.
 *
 * ## 동작/계약
 * - 변환 불가 시 예외를 던지지 않고 null을 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val date = LocalDate.now().localDate
 * // date != null
 * ```
 */
val TemporalAccessor.localDate: LocalDate? get() = queryOrNull { LocalDate.from(this) }

/**
 * [TemporalAccessor]에서 [LocalTime]을 안전하게 추출합니다.
 *
 * ## 동작/계약
 * - 변환 불가 시 예외를 던지지 않고 null을 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val time = LocalTime.now().localTime
 * // time != null
 * ```
 */
val TemporalAccessor.localTime: LocalTime? get() = queryOrNull { LocalTime.from(this) }

/**
 * [TemporalAccessor]에서 [LocalDateTime]을 안전하게 추출합니다.
 *
 * ## 동작/계약
 * - 변환 불가 시 예외를 던지지 않고 null을 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val ldt = LocalDateTime.now().localDateTime
 * // ldt != null
 * ```
 */
val TemporalAccessor.localDateTime: LocalDateTime? get() = queryOrNull { LocalDateTime.from(this) }

/**
 * [TemporalAccessor]에서 [ZoneOffset]을 안전하게 추출합니다.
 *
 * ## 동작/계약
 * - 변환 불가 시 예외를 던지지 않고 null을 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val offset = OffsetDateTime.now().zoneOffset
 * // offset != null
 * ```
 */
val TemporalAccessor.zoneOffset: ZoneOffset? get() = queryOrNull { ZoneOffset.from(this) }

/**
 * [TemporalAccessor]에서 [OffsetTime]을 안전하게 추출합니다.
 *
 * ## 동작/계약
 * - 변환 불가 시 예외를 던지지 않고 null을 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val ot = OffsetTime.now().offsetTime
 * // ot != null
 * ```
 */
val TemporalAccessor.offsetTime: OffsetTime? get() = queryOrNull { OffsetTime.from(this) }

/**
 * [TemporalAccessor]에서 [OffsetDateTime]을 안전하게 추출합니다.
 *
 * ## 동작/계약
 * - 변환 불가 시 예외를 던지지 않고 null을 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val odt = OffsetDateTime.now().offsetDateTime
 * // odt != null
 * ```
 */
val TemporalAccessor.offsetDateTime: OffsetDateTime? get() = queryOrNull { OffsetDateTime.from(this) }

/**
 * [TemporalAccessor]에서 [ZoneId]를 안전하게 추출합니다.
 *
 * ## 동작/계약
 * - 변환 불가 시 예외를 던지지 않고 null을 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val zone = ZonedDateTime.now().zone
 * // zone != null
 * ```
 */
val TemporalAccessor.zone: ZoneId? get() = queryOrNull { ZoneId.from(this) }

/**
 * [TemporalAccessor]에서 [ZoneId]를 안전하게 추출합니다. ([zone]의 별칭)
 *
 * ## 동작/계약
 * - 변환 불가 시 예외를 던지지 않고 null을 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val zoneId = ZonedDateTime.now().zoneId
 * // zoneId != null
 * ```
 */
val TemporalAccessor.zoneId: ZoneId? get() = queryOrNull { ZoneId.from(this) }

/**
 * [TemporalAccessor]에서 [ZonedDateTime]을 안전하게 추출합니다.
 *
 * ## 동작/계약
 * - 변환 불가 시 예외를 던지지 않고 null을 반환합니다.
 * - 수신 객체를 변경하지 않습니다.
 *
 * ```kotlin
 * val zdt = ZonedDateTime.now().zonedDateTime
 * // zdt != null
 * ```
 */
val TemporalAccessor.zonedDateTime: ZonedDateTime? get() = queryOrNull { ZonedDateTime.from(this) }


/**
 * [TemporalAccessor]가 [TemporalField]를 지원하는지 확인합니다.
 *
 * ```
 * val now = nowInstant()
 * now.supports(ChronoField.INSTANT_SECONDS).shouldBeTrue()
 * ```
 */
infix fun TemporalAccessor.supports(temporalField: TemporalField) = isSupported(temporalField)

private inline fun <T> TemporalAccessor.queryOrNull(crossinline block: TemporalAccessor.() -> T): T? =
    try {
        block(this)
    } catch (_: DateTimeException) {
        null
    }

/**
 * formatOrNull 기능을 제공합니다.
 *
 * ## 동작/계약
 * - null 입력 허용 여부는 시그니처의 nullable 표기를 따릅니다.
 * - 수신 객체 mutate 여부는 구현을 따르며, 별도 명시가 없으면 값을 반환합니다.
 * - 사전조건 위반 시 IllegalArgumentException 또는 구현 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val result = java.time.Instant.EPOCH.formatOrNull(java.time.format.DateTimeFormatter.ISO_INSTANT)
 * // result == "1970-01-01T00:00:00Z"
 * ```
 */
inline fun TemporalAccessor.formatOrNull(formatter: DateTimeFormatter): String? =
    try {
        formatter.format(this)
    } catch (_: DateTimeException) {
        null
    }
