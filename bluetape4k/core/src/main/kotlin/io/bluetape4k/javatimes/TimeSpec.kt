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

/** 밀리초당 나노초 수 */
const val NANO_PER_MILLIS: Long = 1_000_000L

/** 초당 나노초 수 */
const val NANO_PER_SECOND: Long = 1_000_000_000L

/** 하루의 밀리초 수 */
val MILLIS_IN_DAY: Long = Duration.ofDays(1).toMillis()

/** 한 시간의 밀리초 수 */
val MILLIS_IN_HOUR = Duration.ofHours(1).toMillis()

/** 1분의 밀리초 수 */
val MILLIS_IN_MINUTE = Duration.ofMinutes(1).toMillis()

/** 하루의 나노초 수 */
val NANOS_IN_DAY: Long = Duration.ofDays(1).toNanos()

/** 한 시간의 나노초 수 */
val NANOS_IN_HOUR: Long = Duration.ofHours(1).toNanos()

/** 1분의 나노초 수 */
val NANOS_IN_MINUTE = Duration.ofMinutes(1).toNanos()

/** 1초의 나노초 수 */
val NANOS_IN_SECOND = Duration.ofSeconds(1).toNanos()


/** 기본 날짜/시각 포맷터 (ISO_INSTANT) */
val DefaultDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

/** UTC 시간대 */
val UtcTimeZone: TimeZone = TimeZone.getTimeZone(ZoneOffset.UTC)

/** 시스템 기본 시간대 */
val SystemTimeZone: TimeZone = TimeZone.getDefault()

/** 시스템 기본 ZoneId */
val SystemZoneId: ZoneId = ZoneId.systemDefault()

/** 시스템 기본 ZoneOffset */
val SystemOffset: ZoneOffset = ZoneOffset.ofTotalSeconds(SystemTimeZone.rawOffset / 1000)


/** 연간 월 수 */
const val MonthsPerYear = 12

/** 연간 반기 수 */
const val HalfyearsPerYear = 2

/** 연간 분기 수 */
const val QuartersPerYear = 4

/** 반기당 분기 수 */
const val QuartersPerHalfyear = 2

/** 반기당 월 수 */
const val MonthsPerHalfyear = 6

/** 분기당 월 수 */
const val MonthsPerQuarter = 3

/** 연간 최대 주 수 */
const val MaxWeeksPerYear = 54

/** 월간 최대 일 수 */
const val MaxDaysPerMonth = 31

/** 주당 일 수 */
const val DaysPerWeek = 7

/** 하루의 시간 수 */
const val HoursPerDay = 24

/** 한 시간의 분 수 */
const val MinutesPerHour = 60

/** 1분의 초 수 */
const val SecondsPerMinute = 60

/** 1초의 밀리초 수 */
const val MillisPerSecond = 1000L

/** 1분의 밀리초 수 */
const val MillisPerMinute: Long = MillisPerSecond * SecondsPerMinute

/** 한 시간의 밀리초 수 */
const val MillisPerHour: Long = MillisPerMinute * MinutesPerHour

/** 하루의 밀리초 수 */
const val MillisPerDay: Long = MillisPerHour * HoursPerDay

/** 밀리초당 마이크로초 수 */
const val MicrosPerMillis = 1000L

/** 1초의 마이크로초 수 */
const val MicrosPerSecond = MicrosPerMillis * MillisPerSecond

/** 1분의 마이크로초 수 */
const val MicrosPerMinute: Long = MicrosPerSecond * SecondsPerMinute

/** 한 시간의 마이크로초 수 */
const val MicrosPerHour: Long = MicrosPerMinute * MinutesPerHour

/** 하루의 마이크로초 수 */
const val MicrosPerDay: Long = MicrosPerHour * HoursPerDay

/** 밀리초당 나노초 수 */
const val NanosPerMillis: Long = MicrosPerSecond

/** 1초의 나노초 수 */
const val NanosPerSecond = NanosPerMillis * MillisPerSecond

/** 1분의 나노초 수 */
const val NanosPerMinute: Long = NanosPerSecond * SecondsPerMinute

/** 한 시간의 나노초 수 */
const val NanosPerHour: Long = NanosPerMinute * MinutesPerHour

/** 하루의 나노초 수 */
const val NanosPerDay: Long = NanosPerHour * HoursPerDay

/** 밀리초당 틱 수 */
const val TicksPerMillisecond = 10000L

/** 1초의 틱 수 */
const val TicksPerSecond = TicksPerMillisecond * MillisPerSecond

/** 1분의 틱 수 */
const val TicksPerMinute = TicksPerSecond * SecondsPerMinute

/** 한 시간의 틱 수 */
const val TicksPerHour = TicksPerMinute * MinutesPerHour

/** 하루의 틱 수 */
const val TicksPerDay = TicksPerHour * HoursPerDay


/** 평일 (월요일 ~ 금요일) */
val Weekdays = arrayOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
)

/** 주말 (토요일, 일요일) */
val Weekends = arrayOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

/** 주의 시작 요일 (월요일) */
val FirstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY

/** 상반기 월 배열 (1월 ~ 6월) */
val FirstHalfyearMonths = intArrayOf(1, 2, 3, 4, 5, 6)

/** 하반기 월 배열 (7월 ~ 12월) */
val SecondHalfyearMonths = intArrayOf(7, 8, 9, 10, 11, 12)


/** 1분기 월 배열 (1월 ~ 3월) */
val Q1Months = intArrayOf(1, 2, 3)

/** 2분기 월 배열 (4월 ~ 6월) */
val Q2Months = intArrayOf(4, 5, 6)

/** 3분기 월 배열 (7월 ~ 9월) */
val Q3Months = intArrayOf(7, 8, 9)

/** 4분기 월 배열 (10월 ~ 12월) */
val Q4Months = intArrayOf(10, 11, 12)

/** 빈 Duration (ZERO) */
val EmptyDuration: Duration = Duration.ZERO

/** 최소 Duration (0 나노초) */
val MinDuration: Duration = 0.nanos()

/** 최대 Duration (Long.MAX_VALUE 초) */
val MaxDuration: Duration = Long.MAX_VALUE.seconds()

/** 최소 양수 Duration (1 나노초) */
val MinPositiveDuration: Duration = 1.nanos()

/** 최소 음수 Duration (-1 나노초) */
val MinNegativeDuration: Duration = (-1).nanos()

/** 최소 기간 시각 (LocalDate.MIN의 자정) */
val MinPeriodTime: ZonedDateTime = zonedDateTimeOf(LocalDate.MIN, LocalTime.MIDNIGHT)

/** 최대 기간 시각 (LocalDate.MAX의 자정) */
val MaxPeriodTime: ZonedDateTime = zonedDateTimeOf(LocalDate.MAX, LocalTime.MIDNIGHT)

/** 기본 시작 오프셋 (빈 Duration) */
val DefaultStartOffset: Duration = EmptyDuration

/** 기본 종료 오프셋 (-1 나노초) */
val DefaultEndOffset: Duration = MinNegativeDuration

/**
 * [DayOfWeek]가 주말인지 여부를 반환합니다.
 *
 * @return 토요일 또는 일요일이면 true, 그렇지 않으면 false
 */
fun DayOfWeek.isWeekend(): Boolean = Weekends.contains(this)
