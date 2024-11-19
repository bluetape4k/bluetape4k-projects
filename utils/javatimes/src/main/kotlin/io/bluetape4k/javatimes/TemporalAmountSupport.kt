package io.bluetape4k.javatimes

import java.time.Duration
import java.time.Period
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAmount


// NOTE: ChronoUnit.DAYS 아래만 가능합니다. (Period 는 정확한 계산할 수 없습니다)
//

/**
 * [TemporalAmount]의 모든 [ChronoUnit] 단위의 값들을 nanoseconds로 변환하여 합산합니다.
 */
val TemporalAmount.nanos: Double
    get() = units.fold(0.0) { acc, it -> acc + Duration.of(get(it), it).toNanos().toDouble() }

// NOTE: ChronoUnit.DAYS 아래만 가능합니다. (Period 는 정확한 계산할 수 없습니다)
//

/**
 * [TemporalAmount]의 모든 [ChronoUnit] 단위의 값들을 milliseconds로 변환하여 합산합니다.
 */
val TemporalAmount.millis: Long
    get() = units.fold(0L) { acc, unit -> acc + Duration.of(get(unit), unit).toMillis() }

/**
 * [TemporalAmount]가 milliseconds 정밀도로 0인지 여부를 반환합니다.
 */
val TemporalAmount.isZero: Boolean
    get() = millis == 0L

/**
 * [TemporalAmount]가 milliseconds 정밀도로 양수인지 여부를 반환합니다.
 */
val TemporalAmount.isPositive: Boolean
    get() = millis > 0L

/**
 * [TemporalAmount]가 milliseconds 정밀도로 음수인지 여부를 반환합니다.
 */
val TemporalAmount.isNegative: Boolean
    get() = millis < 0L

/**
 * [chronoUnit]의 숫자를 [TemporalAmount]로 변환합니다.
 *
 * ```
 * 1.temporalAmount(ChronoUnit.DAYS) // Period.ofDays(1)
 * 2.temporalAmount(ChronoUnit.HOURS) // Duration.ofHours(2)
 * ```
 */
fun Int.temporalAmount(chronoUnit: ChronoUnit): TemporalAmount = toLong().temporalAmount(chronoUnit)

/**
 * [chronoUnit]의 숫자를 [TemporalAmount]로 변환합니다.
 *
 * ```
 * 1L.temporalAmount(ChronoUnit.DAYS) // Period.ofDays(1)
 * 2L.temporalAmount(ChronoUnit.HOURS) // Duration.ofHours(2)
 * ```
 */
fun Long.temporalAmount(chronoUnit: ChronoUnit): TemporalAmount = when (chronoUnit) {
    ChronoUnit.YEARS   -> Period.ofYears(this.toInt())
    ChronoUnit.MONTHS  -> Period.ofMonths(this.toInt())
    ChronoUnit.WEEKS   -> Period.ofWeeks(this.toInt())
    ChronoUnit.DAYS    -> Duration.ofDays(this)
    ChronoUnit.HOURS   -> Duration.ofHours(this)
    ChronoUnit.MINUTES -> Duration.ofMinutes(this)
    ChronoUnit.SECONDS -> Duration.ofSeconds(this)
    ChronoUnit.MILLIS  -> Duration.ofMillis(this)
    ChronoUnit.MICROS  -> Duration.ofNanos(this * 1000L)
    ChronoUnit.NANOS   -> Duration.ofNanos(this)
    else               -> throw IllegalArgumentException("Not supported ChronoUnit. chronounit=$chronoUnit")
}
