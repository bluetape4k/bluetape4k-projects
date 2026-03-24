package io.bluetape4k.javatimes

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * 밀리초당 나노초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = NANO_PER_MILLIS
 * // value
 * // true
 * ```
 */
const val NANO_PER_MILLIS: Long = 1_000_000L

/**
 * 초당 나노초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = NANO_PER_SECOND
 * // value
 * // true
 * ```
 */
const val NANO_PER_SECOND: Long = 1_000_000_000L

/**
 * 하루의 밀리초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MILLIS_IN_DAY
 * // value
 * // true
 * ```
 */
@Deprecated(
    message = "Use MillisPerDay 등 camelCase 상수를 사용하세요",
    replaceWith = ReplaceWith("MillisPerDay")
)
val MILLIS_IN_DAY: Long = Duration.ofDays(1).toMillis()

/**
 * 한 시간의 밀리초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MILLIS_IN_HOUR
 * // value
 * // true
 * ```
 */
@Deprecated(
    message = "Use MillisPerHour 등 camelCase 상수를 사용하세요",
    replaceWith = ReplaceWith("MillisPerHour")
)
val MILLIS_IN_HOUR = Duration.ofHours(1).toMillis()

/**
 * 1분의 밀리초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MILLIS_IN_MINUTE
 * // value
 * // true
 * ```
 */
@Deprecated(
    message = "Use MillisPerMinute 등 camelCase 상수를 사용하세요",
    replaceWith = ReplaceWith("MillisPerMinute")
)
val MILLIS_IN_MINUTE = Duration.ofMinutes(1).toMillis()

/**
 * 하루의 나노초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = NANOS_IN_DAY
 * // value
 * // true
 * ```
 */
val NANOS_IN_DAY: Long = Duration.ofDays(1).toNanos()

/**
 * 한 시간의 나노초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = NANOS_IN_HOUR
 * // value
 * // true
 * ```
 */
val NANOS_IN_HOUR: Long = Duration.ofHours(1).toNanos()

/**
 * 1분의 나노초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = NANOS_IN_MINUTE
 * // value
 * // true
 * ```
 */
val NANOS_IN_MINUTE = Duration.ofMinutes(1).toNanos()

/**
 * 1초의 나노초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = NANOS_IN_SECOND
 * // value
 * // true
 * ```
 */
val NANOS_IN_SECOND = Duration.ofSeconds(1).toNanos()


/**
 * 기본 날짜/시각 포맷터 (ISO_INSTANT)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = DefaultDateTimeFormatter
 * // value
 * // true
 * ```
 */
val DefaultDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

/**
 * UTC 시간대
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = UtcTimeZone
 * // value
 * // true
 * ```
 */
val UtcTimeZone: TimeZone = TimeZone.getTimeZone(ZoneOffset.UTC)

/**
 * 시스템 기본 시간대
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * JVM 시작 시점의 시스템 타임존을 캐시합니다. 런타임에 TimeZone.setDefault()로 변경해도 이 값은 갱신되지 않습니다.
 *
 * ```kotlin
 * val value = SystemTimeZone
 * // value
 * // true
 * ```
 */
val SystemTimeZone: TimeZone = TimeZone.getDefault()

/**
 * 시스템 기본 ZoneId
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * JVM 시작 시점의 시스템 타임존을 캐시합니다. 런타임에 TimeZone.setDefault()로 변경해도 이 값은 갱신되지 않습니다.
 *
 * ```kotlin
 * val value = SystemZoneId
 * // value
 * // true
 * ```
 */
val SystemZoneId: ZoneId = ZoneId.systemDefault()

/**
 * 시스템 기본 ZoneOffset
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * JVM 시작 시점의 시스템 타임존을 캐시합니다. 런타임에 TimeZone.setDefault()로 변경해도 이 값은 갱신되지 않습니다.
 *
 * ```kotlin
 * val value = SystemOffset
 * // value
 * // true
 * ```
 */
val SystemOffset: ZoneOffset = ZoneOffset.ofTotalSeconds(SystemTimeZone.rawOffset / 1000)


/**
 * 연간 월 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MonthsPerYear
 * // value
 * // true
 * ```
 */
const val MonthsPerYear = 12

/**
 * 연간 반기 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = HalfyearsPerYear
 * // value
 * // true
 * ```
 */
const val HalfyearsPerYear = 2

/**
 * 연간 분기 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = QuartersPerYear
 * // value
 * // true
 * ```
 */
const val QuartersPerYear = 4

/**
 * 반기당 분기 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = QuartersPerHalfyear
 * // value
 * // true
 * ```
 */
const val QuartersPerHalfyear = 2

/**
 * 반기당 월 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MonthsPerHalfyear
 * // value
 * // true
 * ```
 */
const val MonthsPerHalfyear = 6

/**
 * 분기당 월 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MonthsPerQuarter
 * // value
 * // true
 * ```
 */
const val MonthsPerQuarter = 3

/**
 * 연간 최대 주 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MaxWeeksPerYear
 * // value
 * // true
 * ```
 */
const val MaxWeeksPerYear = 54

/**
 * 월간 최대 일 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MaxDaysPerMonth
 * // value
 * // true
 * ```
 */
const val MaxDaysPerMonth = 31

/**
 * 주당 일 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = DaysPerWeek
 * // value
 * // true
 * ```
 */
const val DaysPerWeek = 7

/**
 * 하루의 시간 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = HoursPerDay
 * // value
 * // true
 * ```
 */
const val HoursPerDay = 24

/**
 * 한 시간의 분 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MinutesPerHour
 * // value
 * // true
 * ```
 */
const val MinutesPerHour = 60

/**
 * 1분의 초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = SecondsPerMinute
 * // value
 * // true
 * ```
 */
const val SecondsPerMinute = 60

/**
 * 1초의 밀리초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MillisPerSecond
 * // value
 * // true
 * ```
 */
const val MillisPerSecond = 1000L

/**
 * 1분의 밀리초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MillisPerMinute
 * // value
 * // true
 * ```
 */
const val MillisPerMinute: Long = MillisPerSecond * SecondsPerMinute

/**
 * 한 시간의 밀리초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MillisPerHour
 * // value
 * // true
 * ```
 */
const val MillisPerHour: Long = MillisPerMinute * MinutesPerHour

/**
 * 하루의 밀리초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MillisPerDay
 * // value
 * // true
 * ```
 */
const val MillisPerDay: Long = MillisPerHour * HoursPerDay

/**
 * 밀리초당 마이크로초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MicrosPerMillis
 * // value
 * // true
 * ```
 */
const val MicrosPerMillis = 1000L

/**
 * 1초의 마이크로초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MicrosPerSecond
 * // value
 * // true
 * ```
 */
const val MicrosPerSecond = MicrosPerMillis * MillisPerSecond

/**
 * 1분의 마이크로초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MicrosPerMinute
 * // value
 * // true
 * ```
 */
const val MicrosPerMinute: Long = MicrosPerSecond * SecondsPerMinute

/**
 * 한 시간의 마이크로초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MicrosPerHour
 * // value
 * // true
 * ```
 */
const val MicrosPerHour: Long = MicrosPerMinute * MinutesPerHour

/**
 * 하루의 마이크로초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MicrosPerDay
 * // value
 * // true
 * ```
 */
const val MicrosPerDay: Long = MicrosPerHour * HoursPerDay

/**
 * 밀리초당 나노초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = NanosPerMillis
 * // value
 * // true
 * ```
 */
const val NanosPerMillis: Long = 1_000_000L

/**
 * 1초의 나노초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = NanosPerSecond
 * // value
 * // true
 * ```
 */
const val NanosPerSecond = NanosPerMillis * MillisPerSecond

/**
 * 1분의 나노초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = NanosPerMinute
 * // value
 * // true
 * ```
 */
const val NanosPerMinute: Long = NanosPerSecond * SecondsPerMinute

/**
 * 한 시간의 나노초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = NanosPerHour
 * // value
 * // true
 * ```
 */
const val NanosPerHour: Long = NanosPerMinute * MinutesPerHour

/**
 * 하루의 나노초 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = NanosPerDay
 * // value
 * // true
 * ```
 */
const val NanosPerDay: Long = NanosPerHour * HoursPerDay

/**
 * 밀리초당 틱 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = TicksPerMillisecond
 * // value
 * // true
 * ```
 */
const val TicksPerMillisecond = 10000L

/**
 * 1초의 틱 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = TicksPerSecond
 * // value
 * // true
 * ```
 */
const val TicksPerSecond = TicksPerMillisecond * MillisPerSecond

/**
 * 1분의 틱 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = TicksPerMinute
 * // value
 * // true
 * ```
 */
const val TicksPerMinute = TicksPerSecond * SecondsPerMinute

/**
 * 한 시간의 틱 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = TicksPerHour
 * // value
 * // true
 * ```
 */
const val TicksPerHour = TicksPerMinute * MinutesPerHour

/**
 * 하루의 틱 수
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = TicksPerDay
 * // value
 * // true
 * ```
 */
const val TicksPerDay = TicksPerHour * HoursPerDay


/**
 * 평일 (월요일 ~ 금요일)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = Weekdays
 * // value
 * // true
 * ```
 */
val Weekdays = arrayOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
)

/**
 * 주말 (토요일, 일요일)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = Weekends
 * // value
 * // true
 * ```
 */
val Weekends = arrayOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

/**
 * 주의 시작 요일 (월요일)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = FirstDayOfWeek
 * // value
 * // true
 * ```
 */
val FirstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY

/**
 * 상반기 월 배열 (1월 ~ 6월)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = FirstHalfyearMonths
 * // value
 * // true
 * ```
 */
val FirstHalfyearMonths = intArrayOf(1, 2, 3, 4, 5, 6)

/**
 * 하반기 월 배열 (7월 ~ 12월)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = SecondHalfyearMonths
 * // value
 * // true
 * ```
 */
val SecondHalfyearMonths = intArrayOf(7, 8, 9, 10, 11, 12)


/**
 * 1분기 월 배열 (1월 ~ 3월)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = Q1Months
 * // value
 * // true
 * ```
 */
val Q1Months = intArrayOf(1, 2, 3)

/**
 * 2분기 월 배열 (4월 ~ 6월)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = Q2Months
 * // value
 * // true
 * ```
 */
val Q2Months = intArrayOf(4, 5, 6)

/**
 * 3분기 월 배열 (7월 ~ 9월)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = Q3Months
 * // value
 * // true
 * ```
 */
val Q3Months = intArrayOf(7, 8, 9)

/**
 * 4분기 월 배열 (10월 ~ 12월)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = Q4Months
 * // value
 * // true
 * ```
 */
val Q4Months = intArrayOf(10, 11, 12)

/**
 * 빈 Duration (ZERO)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = EmptyDuration
 * // value
 * // true
 * ```
 */
val EmptyDuration: Duration = Duration.ZERO

/**
 * 최소 Duration (0 나노초)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MinDuration
 * // value
 * // true
 * ```
 */
val MinDuration: Duration = 0.nanos()

/**
 * 최대 Duration (Long.MAX_VALUE 초)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MaxDuration
 * // value
 * // true
 * ```
 */
val MaxDuration: Duration = Long.MAX_VALUE.seconds()

/**
 * 최소 양수 Duration (1 나노초)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MinPositiveDuration
 * // value
 * // true
 * ```
 */
val MinPositiveDuration: Duration = 1.nanos()

/**
 * 최소 음수 Duration (-1 나노초)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MinNegativeDuration
 * // value
 * // true
 * ```
 */
val MinNegativeDuration: Duration = (-1).nanos()

/**
 * 최소 기간 시각 (LocalDate.MIN의 자정)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MinPeriodTime
 * // value
 * // true
 * ```
 */
val MinPeriodTime: ZonedDateTime = zonedDateTimeOf(LocalDate.MIN, LocalTime.MIDNIGHT)

/**
 * 최대 기간 시각 (LocalDate.MAX의 자정)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = MaxPeriodTime
 * // value
 * // true
 * ```
 */
val MaxPeriodTime: ZonedDateTime = zonedDateTimeOf(LocalDate.MAX, LocalTime.MIDNIGHT)

/**
 * 기본 시작 오프셋 (빈 Duration)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = DefaultStartOffset
 * // value
 * // true
 * ```
 */
val DefaultStartOffset: Duration = EmptyDuration

/**
 * 기본 종료 오프셋 (-1 나노초)
 *
 * ## 동작/계약
 * - null 입력이 없으며 상수/프로퍼티 값을 제공합니다.
 * - 수신 객체 mutate 없이 값을 조회하거나 계산해 반환합니다.
 * - 시간/타임존 의존 값은 실행 환경(JVM 기본 설정)에 따라 달라질 수 있습니다.
 *
 * ```kotlin
 * val value = DefaultEndOffset
 * // value
 * // true
 * ```
 */
val DefaultEndOffset: Duration = MinNegativeDuration

/**
 * [DayOfWeek]가 주말인지 여부를 반환합니다.
 *
 * @return 토요일 또는 일요일이면 true, 그렇지 않으면 false
 */
fun DayOfWeek.isWeekend(): Boolean = Weekends.contains(this)
