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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
 * ```
 */
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
 * println(value)
 * check(true)
 * ```
 */
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
 * println(value)
 * check(true)
 * ```
 */
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * ```kotlin
 * val value = SystemTimeZone
 * println(value)
 * check(true)
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
 * ```kotlin
 * val value = SystemZoneId
 * println(value)
 * check(true)
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
 * ```kotlin
 * val value = SystemOffset
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
 * ```
 */
const val NanosPerMillis: Long = MicrosPerSecond

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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
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
 * println(value)
 * check(true)
 * ```
 */
val DefaultEndOffset: Duration = MinNegativeDuration

/**
 * [DayOfWeek]가 주말인지 여부를 반환합니다.
 *
 * @return 토요일 또는 일요일이면 true, 그렇지 않으면 false
 */
fun DayOfWeek.isWeekend(): Boolean = Weekends.contains(this)
