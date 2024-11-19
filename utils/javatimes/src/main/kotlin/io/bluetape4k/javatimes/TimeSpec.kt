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

const val NANO_PER_MILLIS: Long = 1_000_000L
const val NANO_PER_SECOND: Long = 1_000_000_000L

val MILLIS_IN_DAY: Long = Duration.ofDays(1).toMillis()
val MILLIS_IN_HOUR = Duration.ofHours(1).toMillis()
val MILLIS_IN_MINUTE = Duration.ofMinutes(1).toMillis()
val NANOS_IN_DAY: Long = Duration.ofDays(1).toNanos()
val NANOS_IN_HOUR: Long = Duration.ofHours(1).toNanos()
val NANOS_IN_MINUTER = Duration.ofMinutes(1).toNanos()
val NANOS_IN_SECOND = Duration.ofSeconds(1).toNanos()


val DefaultDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

val UtcTimeZone: TimeZone = TimeZone.getTimeZone(ZoneOffset.UTC)

val SystemTimeZone: TimeZone = TimeZone.getDefault()

val SystemZoneId: ZoneId = ZoneId.systemDefault()

val SystemOffset: ZoneOffset = ZoneOffset.ofTotalSeconds(SystemTimeZone.rawOffset / 1000)


const val MonthsPerYear = 12
const val HalfyearsPerYear = 2
const val QuartersPerYear = 4
const val QuartersPerHalfyear = 2
const val MonthsPerHalfyear = 6
const val MonthsPerQuarter = 3
const val MaxWeeksPerYear = 54
const val MaxDaysPerMonth = 31
const val DaysPerWeek = 7
const val HoursPerDay = 24
const val MinutesPerHour = 60
const val SecondsPerMinute = 60

const val MillisPerSecond = 1000L
const val MillisPerMinute: Long = MillisPerSecond * SecondsPerMinute
const val MillisPerHour: Long = MillisPerMinute * MinutesPerHour
const val MillisPerDay: Long = MillisPerHour * HoursPerDay

const val MicrosPerMillis = 1000L
const val MicrosPerSecond = MicrosPerMillis * MillisPerSecond
const val MicrosPerMinute: Long = MicrosPerSecond * SecondsPerMinute
const val MicrosPerHour: Long = MicrosPerMinute * MinutesPerHour
const val MicrosPerDay: Long = MicrosPerHour * HoursPerDay

const val NanosPerMillis: Long = MicrosPerSecond
const val NanosPerSecond = NanosPerMillis * MillisPerSecond
const val NanosPerMinute: Long = NanosPerSecond * SecondsPerMinute
const val NanosPerHour: Long = NanosPerMinute * MinutesPerHour
const val NanosPerDay: Long = NanosPerHour * HoursPerDay

const val TicksPerMillisecond = 10000L
const val TicksPerSecond = TicksPerMillisecond * MillisPerSecond
const val TicksPerMinute = TicksPerSecond * SecondsPerMinute
const val TicksPerHour = TicksPerMinute * MinutesPerHour
const val TicksPerDay = TicksPerHour * HoursPerDay


val Weekdays = arrayOf(
    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
)

val Weekends = arrayOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

val FirstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY

val FirstHalfyearMonths = intArrayOf(1, 2, 3, 4, 5, 6)

val SecondHalfyearMonths = intArrayOf(7, 8, 9, 10, 11, 12)


val Q1Months = intArrayOf(1, 2, 3)
val Q2Months = intArrayOf(4, 5, 6)
val Q3Months = intArrayOf(7, 8, 9)
val Q4Months = intArrayOf(10, 11, 12)

val EmptyDuration: Duration = Duration.ZERO
val MinDuration: Duration = 0.nanos()
val MaxDuration: Duration = Long.MAX_VALUE.seconds()
val MinPositiveDuration: Duration = 1.nanos()
val MinNegativeDuration: Duration = (-1).nanos()

val MinPeriodTime: ZonedDateTime = zonedDateTimeOf(LocalDate.MIN, LocalTime.MIDNIGHT)
val MaxPeriodTime: ZonedDateTime = zonedDateTimeOf(LocalDate.MAX, LocalTime.MIDNIGHT)

val DefaultStartOffset: Duration = EmptyDuration
val DefaultEndOffset: Duration = MinNegativeDuration

fun DayOfWeek.isWeekend(): Boolean = Weekends.contains(this)
