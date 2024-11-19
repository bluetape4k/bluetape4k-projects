@file:Suppress("UNCHECKED_CAST")

package io.bluetape4k.javatimes

import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.time.temporal.Temporal
import java.time.temporal.TemporalAccessor
import java.time.temporal.TemporalAdjusters
import java.time.temporal.TemporalAmount
import java.time.temporal.TemporalUnit


//
// NOTE: [Temporal]에 이미 plus, minus 함수가 있어서 재사용을 못한다
//

fun <T: Temporal> T.add(period: Period): T = this.plus(period) as T
fun <T: Temporal> T.add(duration: Duration): T = this.plus(duration) as T
fun <T: Temporal> T.add(amount: TemporalAmount): T = this.plus(amount) as T

fun <T: Temporal> T.subtract(period: Period): T = this.minus(period) as T
fun <T: Temporal> T.subtract(duration: Duration): T = this.minus(duration) as T
fun <T: Temporal> T.subtract(amount: TemporalAmount): T = this.minus(amount) as T

// Temporal Adjusters

/**
 * 현 시각 기준으로 월 초 시각을 반환합니다.
 */
val <T: Temporal> T.firstOfMonth: T get() = with(TemporalAdjusters.firstDayOfMonth()) as T

/**
 * 현 시각 기준으로 월 말 시각을 반환합니다.
 */
val <T: Temporal> T.lastOfMonth: T get() = with(TemporalAdjusters.lastDayOfMonth()) as T

/**
 * 현 시각 기준으로 다음 월의 시작 시각을 반환합니다.
 */
val <T: Temporal> T.firstOfNextMonth: T get() = with(TemporalAdjusters.firstDayOfNextMonth()) as T

/**
 * 현 시각 기준으로 연초 시각을 반환합니다.
 */
val <T: Temporal> T.firstOfYear: T get() = with(TemporalAdjusters.firstDayOfYear()) as T

/**
 * 현 시각 기준으로 연말 시각을 반환합니다.
 */
val <T: Temporal> T.lastOfYear: T get() = with(TemporalAdjusters.lastDayOfYear()) as T

/**
 * 현 시각 기준으로 다음 년의 시작 시각을 반환합니다.
 */
val <T: Temporal> T.firstOfNextYear: T get() = with(TemporalAdjusters.firstDayOfNextYear()) as T

/**
 * 현 시각 기준으로 월의 [ordinal]번째 [dayOfWeek] 요일의 시각을 반환합니다.
 */
fun <T: Temporal> T.dayOfWeekInMonth(ordinal: Int, dayOfWeek: DayOfWeek): T =
    with(TemporalAdjusters.dayOfWeekInMonth(ordinal, dayOfWeek)) as T

/**
 * 현 시각 기준으로 해당 월의 첫번째 [dayOfWeek] 요일의 시각을 반환합니다.
 */
fun <T: Temporal> T.firstInMonth(dayOfWeek: DayOfWeek): T = with(TemporalAdjusters.firstInMonth(dayOfWeek)) as T

/**
 * 현 시각 기준으로 해당 월의 마지막 [dayOfWeek] 요일의 시각을 반환합니다.
 */
fun <T: Temporal> T.lastInMonth(dayOfWeek: DayOfWeek): T = with(TemporalAdjusters.lastInMonth(dayOfWeek)) as T

/**
 * 현 시각 기준으로 해당 월의 전 주의 [dayOfWeek] 요일의 시각을 반환합니다.
 */
fun <T: Temporal> T.previous(dayOfWeek: DayOfWeek): T = with(TemporalAdjusters.previous(dayOfWeek)) as T

/**
 * 현 시각 기준으로 해당 월의 전 주의 [dayOfWeek] 요일의 시각 또는 같은 주의 [dayOfWeek] 요일의 시각을 반환합니다.
 */
fun <T: Temporal> T.previousOrSame(dayOfWeek: DayOfWeek): T = with(TemporalAdjusters.previousOrSame(dayOfWeek)) as T

/**
 * 현 시각 기준으로 해당 월의 다음 주의 [dayOfWeek] 요일의 시각을 반환합니다.
 */
fun <T: Temporal> T.next(dayOfWeek: DayOfWeek): T = with(TemporalAdjusters.next(dayOfWeek)) as T

/**
 * 현 시각 기준으로 해당 월의 다음 주의 [dayOfWeek] 요일의 시각 또는 같은 주의 [dayOfWeek] 요일의 시각을 반환합니다.
 */
fun <T: Temporal> T.nextOrSame(dayOfWeek: DayOfWeek): T = with(TemporalAdjusters.nextOrSame(dayOfWeek)) as T

/**
 * 현 [Temporal] 이 [temporalUnit]을 지원하는지 여부
 */
infix fun <T: Temporal> T.supports(temporalUnit: TemporalUnit): Boolean = isSupported(temporalUnit)

/**
 * 현 [TemporalAccessor]를 [Instant]로 변환합니다.
 */
fun <T: TemporalAccessor> T.toInstant(): Instant = Instant.from(this)

/**
 * [Temporal] 을 Epoch 이후의 milli seconds 단위로 표현한 값 (기존 Date#time, Timestamp 와 같은 값을 나타낸다)
 *
 * [Instant], [LocalDate], [LocalDateTime], [OffsetDateTime], [ZonedDateTime]는 `toEpochMilli()` 함수를 제공한다.
 * 나머지 [Temporal] 은 [ChronoField.EPOCH_DAY] 와 [ChronoField.MILLI_OF_DAY] 를 지원하는 경우에만 사용 가능하다.
 *
 * ```
 * val instant = Instant.now()
 * val epochMillis = instant.toEpochMillis()
 * ```
 */
fun <T: Temporal> T.toEpochMillis(): Long = when (this) {
    is Instant        -> toEpochMilli()
    is LocalDate      -> zonedDateTimeOf(year, monthValue, dayOfMonth).toEpochMillis()
    is LocalDateTime  -> toZonedDateTime(ZoneOffset.UTC).toEpochMillis()
    is OffsetDateTime -> toInstant().toEpochMilli()
    is ZonedDateTime  -> toInstant().toEpochMilli()
    else              ->
        if (isSupported(ChronoField.EPOCH_DAY) && isSupported(ChronoField.MILLI_OF_DAY)) {
            val days = getLong(ChronoField.EPOCH_DAY)
            val millis = getLong(ChronoField.MILLI_OF_DAY)
            days * MILLIS_IN_DAY + millis
        } else {
            error("Not supported class [${this.javaClass}]")
        }
}

/**
 * [Temporal] 을 Epoch 이후의 day 단위로 표현한 값 (기존 Date#time, Timestamp 와 같은 값을 나타낸다)
 *
 * [Instant], [LocalDate], [LocalDateTime], [OffsetDateTime], [ZonedDateTime]는 `toEpochDay()` 함수를 제공한다.
 * 나머지 [Temporal] 은 [ChronoField.EPOCH_DAY] 를 지원하는 경우에만 사용 가능하다.
 *
 * ```
 * val instant = Instant.now()
 * val epochMillis = instant.toEpochDay()
 * ```
 */
fun <T: Temporal> T.toEpochDay(): Long {
    return when (this) {
        is Instant        -> toEpochMilli() / MILLIS_IN_DAY
        is LocalDate      -> toEpochDay()
        is LocalDateTime  -> toLocalDate().toEpochDay()
        is OffsetDateTime -> toLocalDate().toEpochDay()
        is ZonedDateTime  -> toLocalDate().toEpochDay()
        else              ->
            if (isSupported(ChronoField.EPOCH_DAY)) {
                getLong(ChronoField.EPOCH_DAY)
            } else {
                error("Not supported class [${this.javaClass}]")
            }
    }
    // return toEpochMillis() / (24 * 60 * 60 * 1000)
}

/**
 * [Temporal] 을 [zoneId] 에 맞춰 변환합니다.
 *
 * ```
 * val offsetDateTime = OffsetDateTime.now()
 * val ZoneOffset.UTCDateTime = offsetDateTime.asTemporal(ZoneOffset.UTC)
 *
 * @param zoneId 변환할 [ZoneId] (기본값: [SystemZoneId])
 */
fun <T: Temporal> T.asTemporal(zoneId: ZoneId = SystemZoneId): T = when (this) {
    is Instant        -> Instant.ofEpochMilli(this.toEpochMillis()) as T
    is LocalDate      -> Instant.ofEpochMilli(this.toEpochMillis()).toLocalDate() as T
    is LocalDateTime  -> Instant.ofEpochMilli(this.toEpochMillis()).toLocalDateTime() as T
    is OffsetDateTime -> Instant.ofEpochMilli(this.toEpochMillis()).toOffsetDateTime(zoneId) as T
    is ZonedDateTime  -> Instant.ofEpochMilli(this.toEpochMillis()).toZonedDateTime(zoneId) as T
    else              -> error("Not supported class [${this.javaClass}]")
}

/**
 * [Temporal] 을 [chronoUnit] 단위의 시작 시각으로 변환합니다.
 *
 * ```
 * val localDateTime = LocalDateTime.now()
 * val startOfDay = localDateTime.startOf(ChronoUnit.DAYS)  // 2024-10-14:00:00:00.000Z
 * ```
 */
fun <T: Temporal> T.startOf(chronoUnit: ChronoUnit): T = when (chronoUnit) {
    ChronoUnit.YEARS   -> startOfYear()
    ChronoUnit.MONTHS  -> startOfMonth()
    ChronoUnit.WEEKS   -> previousOrSame(DayOfWeek.MONDAY)
    ChronoUnit.DAYS    -> startOfDay()
    ChronoUnit.HOURS   -> startOfHour()
    ChronoUnit.MINUTES -> startOfMinute()
    ChronoUnit.SECONDS -> startOfSecond()
    ChronoUnit.MILLIS  -> startOfMillis()
    else               -> throw IllegalArgumentException("Unsupported ChronoUnit. chronoUnit=$chronoUnit")
}


/**
 * [Temporal]의 년의 시작 시각을 반환합니다.
 *
 * ```
 * val localDateTime = LocalDateTime.now()
 * val startOfYear = localDateTime.startOfYear()  // 2024-01-01:00:00:00.000Z
 */
fun <T: Temporal> T.startOfYear(): T = when (this) {
    is Instant        -> (startOfDay() as Instant).toZonedDateTime(ZoneOffset.UTC).withDayOfYear(1).toInstant() as T
    is LocalDate      -> withDayOfYear(1).startOfDay() as T
    is LocalDateTime  -> withDayOfYear(1).startOfDay() as T
    is OffsetDateTime -> offsetDateTimeOf(year, 1, 1) as T
    is ZonedDateTime  -> zonedDateTimeOf(year, 1, 1) as T
    else              -> error("Not supported class [${this.javaClass}]")
}

/**
 * [Temporal]의 월의 시작 시각을 반환합니다.
 *
 * ```
 * val localDateTime = LocalDateTime.now()
 * val startOfMonth = localDateTime.startOfMonth()  // 2024-10-01:00:00:00.000Z
 * ```
 */
fun <T: Temporal> T.startOfMonth(): T = when (this) {
    is Instant        -> (startOfDay() as Instant).toZonedDateTime(ZoneOffset.UTC).withDayOfMonth(1).toInstant() as T
    is LocalDate      -> withDayOfMonth(1).startOfDay() as T
    is LocalDateTime  -> withDayOfMonth(1).startOfDay() as T
    is OffsetDateTime -> offsetDateTimeOf(year, monthValue, 1) as T
    is ZonedDateTime  -> zonedDateTimeOf(year, monthValue, 1) as T
    else              -> error("Not supported class [${this.javaClass}]")
}

/**
 * [Temporal]의 주의 시작 시각을 반환합니다.
 *
 * ```
 * val localDateTime = LocalDateTime.now()
 * val startOfWeek = localDateTime.startOfWeek()  // 2024-10-08:00:00:00.000Z
 * ```
 */
fun <T: Temporal> T.startOfWeek(): T = when (this) {
    is Instant        -> (startOfDay() as Instant).toZonedDateTime(ZoneOffset.UTC).startOfWeek() as T
    is LocalDate      -> (startOfDay() - (dayOfWeek.value - DayOfWeek.MONDAY.value).days()) as T
    is LocalDateTime  -> (startOfDay() - (dayOfWeek.value - DayOfWeek.MONDAY.value).days()) as T
    is OffsetDateTime -> (startOfDay() - (dayOfWeek.value - DayOfWeek.MONDAY.value).days()) as T
    is ZonedDateTime  -> (startOfDay() - (dayOfWeek.value - DayOfWeek.MONDAY.value).days()) as T
    else              -> error("Not supported class [${this.javaClass}]")
}

/**
 * [Temporal]의 일의 시작 시각을 반환합니다.
 *
 * ```
 * val localDateTime = LocalDateTime.now()
 * val startOfDay = localDateTime.startOfDay()  // 2024-10-14:00:00:00.000Z
 * ```
 */
fun <T: Temporal> T.startOfDay(): T = when (this) {
    is Instant        -> truncatedTo(ChronoUnit.DAYS) as T
    is LocalDate      -> this
    is LocalTime      -> this
    is LocalDateTime  -> truncatedTo(ChronoUnit.DAYS) as T
    is OffsetDateTime -> truncatedTo(ChronoUnit.DAYS) as T
    is ZonedDateTime  -> truncatedTo(ChronoUnit.DAYS) as T
    else              -> error("Not supported class [${this.javaClass}]")
}

/**
 * [Temporal]의 시간의 시작 시각을 반환합니다.
 *
 * ```
 * val localDateTime = LocalDateTime.now()
 * val startOfHour = localDateTime.startOfHour()  // 2024-10-14:15:00:00.000Z
 * ```
 */
fun <T: Temporal> T.startOfHour(): T = when (this) {
    is Instant        -> truncatedTo(ChronoUnit.HOURS) as T
    is LocalDateTime  -> truncatedTo(ChronoUnit.HOURS) as T
    is OffsetDateTime -> truncatedTo(ChronoUnit.HOURS) as T
    is ZonedDateTime  -> truncatedTo(ChronoUnit.HOURS) as T
    is LocalTime      -> truncatedTo(ChronoUnit.HOURS) as T
    is OffsetTime     -> truncatedTo(ChronoUnit.HOURS) as T
    else              -> error("Not supported class [${this.javaClass}]")
}

/**
 * [Temporal]의 분의 시작 시각을 반환합니다.
 *
 * ```
 * val localDateTime = LocalDateTime.now()
 * val startOfMinute = localDateTime.startOfMinute()  // 2024-10-14:15:30:00.000Z
 * ```
 */
fun <T: Temporal> T.startOfMinute(): T = when (this) {
    is Instant        -> truncatedTo(ChronoUnit.MINUTES) as T
    is LocalDateTime  -> truncatedTo(ChronoUnit.MINUTES) as T
    is OffsetDateTime -> truncatedTo(ChronoUnit.MINUTES) as T
    is ZonedDateTime  -> truncatedTo(ChronoUnit.MINUTES) as T
    is LocalTime      -> truncatedTo(ChronoUnit.MINUTES) as T
    is OffsetTime     -> truncatedTo(ChronoUnit.MINUTES) as T
    else              -> error("Not supported class [${this.javaClass}]")
}

/**
 * [Temporal]의 초의 시작 시각을 반환합니다.
 *
 * ```
 * val localDateTime = LocalDateTime.now()
 * val startOfSecond = localDateTime.startOfSecond()  // 2024-10-14:15:30:45.000Z
 * ```
 */
fun <T: Temporal> T.startOfSecond(): T = when (this) {
    is Instant        -> truncatedTo(ChronoUnit.SECONDS) as T
    is LocalDateTime  -> truncatedTo(ChronoUnit.SECONDS) as T
    is OffsetDateTime -> truncatedTo(ChronoUnit.SECONDS) as T
    is ZonedDateTime  -> truncatedTo(ChronoUnit.SECONDS) as T
    is LocalTime      -> truncatedTo(ChronoUnit.SECONDS) as T
    is OffsetTime     -> truncatedTo(ChronoUnit.SECONDS) as T
    else              -> error("Not supported class [${this.javaClass}]")
}

/**
 * [Temporal]의 밀리초의 시작 시각을 반환합니다.
 *
 * ```
 * val localDateTime = LocalDateTime.now()
 * val startOfMillis = localDateTime.startOfMillis()  // 2024-10-14:15:30:45.123Z
 * ```
 */
fun <T: Temporal> T.startOfMillis(): T = when (this) {
    is Instant        -> truncatedTo(ChronoUnit.MILLIS) as T
    is LocalDateTime  -> truncatedTo(ChronoUnit.MILLIS) as T
    is OffsetDateTime -> truncatedTo(ChronoUnit.MILLIS) as T
    is ZonedDateTime  -> truncatedTo(ChronoUnit.MILLIS) as T
    is LocalTime      -> truncatedTo(ChronoUnit.MILLIS) as T
    is OffsetTime     -> truncatedTo(ChronoUnit.MILLIS) as T
    else              -> error("Not supported class [${this.javaClass}]")
}

/**
 * 두 [Temporal] 중 작은 값을 반환합니다. null 값은 무시합니다.
 */
infix fun <T> T?.min(that: T?): T? where T: Temporal, T: Comparable<T> = when {
    this == null -> that
    that == null -> this
    this < that  -> this
    else         -> that
}

/**
 * 두 [Temporal] 중 큰 값을 반환합니다. null 값은 무시합니다.
 */
infix fun <T> T?.max(that: T?): T? where T: Temporal, T: Comparable<T> = when {
    this == null -> that
    that == null -> this
    this > that  -> this
    else         -> that
}
