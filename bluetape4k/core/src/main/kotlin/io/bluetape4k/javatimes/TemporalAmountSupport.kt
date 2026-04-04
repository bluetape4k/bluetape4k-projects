package io.bluetape4k.javatimes

import io.bluetape4k.support.toIntExact
import java.time.Duration
import java.time.Period
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAmount


// NOTE: ChronoUnit.DAYS 아래만 가능합니다. (Period 는 정확한 계산할 수 없습니다)
//

/**
 * [TemporalAmount]를 [Duration]으로 정확하게 변환합니다.
 *
 * [Period]는 `years`, `months`가 0이어야만 변환할 수 있습니다.
 * 지원되지 않는 단위가 있으면 [IllegalArgumentException]을 발생시킵니다.
 *
 * ```kotlin
 * val d: Duration = Duration.ofSeconds(30).toDurationExact()  // Duration.ofSeconds(30)
 * val d2: Duration = Period.ofDays(2).toDurationExact()       // Duration.ofDays(2)
 * ```
 *
 * @throws IllegalArgumentException [Period]에 years 또는 months가 포함된 경우
 */
fun TemporalAmount.toDurationExact(): Duration = when (this) {
    is Duration -> this
    is Period   -> {
        require(months == 0 && years == 0) {
            "Period with years or months cannot be converted to Duration accurately."
        }
        Duration.ofDays(days.toLong())
    }
    else        -> {
        units.fold(Duration.ZERO) { acc, unit ->
            val unitDuration = try {
                Duration.of(get(unit), unit)
            } catch (e: Exception) {
                throw IllegalArgumentException("TemporalAmount unit cannot be converted to Duration. unit=$unit", e)
            }
            acc.plus(unitDuration)
        }
    }
}

/**
 * [TemporalAmount]를 [Duration]으로 변환 시도하고, 실패하면 `null`을 반환합니다.
 *
 * ```kotlin
 * val d = Period.ofDays(1).toDurationOrNull()   // Duration.ofDays(1)
 * val n = Period.ofMonths(1).toDurationOrNull() // null (월 포함 변환 불가)
 * ```
 */
fun TemporalAmount.toDurationOrNull(): Duration? =
    runCatching { toDurationExact() }.getOrNull()

/**
 * [TemporalAmount]의 값을 나노초 정밀도의 [Long]으로 반환합니다.
 *
 * ```kotlin
 * val ns = Duration.ofMillis(1).nanosLong  // 1_000_000L
 * ```
 */
val TemporalAmount.nanosLong: Long
    get() = toDurationExact().toNanos()

/**
 * [TemporalAmount]의 값을 나노초 정밀도의 [Double]로 반환합니다.
 *
 * ```kotlin
 * val ns = Duration.ofMillis(1).nanos  // 1_000_000.0
 * ```
 */
val TemporalAmount.nanos: Double
    get() = nanosLong.toDouble()

// NOTE: ChronoUnit.DAYS 아래만 가능합니다. (Period 는 정확한 계산할 수 없습니다)
//

/**
 * [TemporalAmount]의 모든 [ChronoUnit] 단위의 값들을 milliseconds로 변환하여 합산합니다.
 *
 * ```kotlin
 * val ms = Duration.ofSeconds(2).millis  // 2_000L
 * ```
 */
val TemporalAmount.millis: Long
    get() = toDurationExact().toMillis()

/**
 * [TemporalAmount]의 부호를 반환합니다. (음수: -1, 0: 0, 양수: 1)
 *
 * ```kotlin
 * Duration.ofSeconds(5).sign   // 1
 * Duration.ZERO.sign           // 0
 * Duration.ofSeconds(-3).sign  // -1
 * ```
 */
val TemporalAmount.sign: Int
    get() = toDurationExact().compareTo(Duration.ZERO)

/**
 * [TemporalAmount]가 0인지 여부를 반환합니다.
 *
 * ```kotlin
 * Duration.ZERO.isZero           // true
 * Duration.ofSeconds(1).isZero   // false
 * ```
 */
val TemporalAmount.isZero: Boolean
    get() = sign == 0

/**
 * [TemporalAmount]가 양수인지 여부를 반환합니다.
 *
 * ```kotlin
 * Duration.ofSeconds(5).isPositive   // true
 * Duration.ofSeconds(-1).isPositive  // false
 * ```
 */
val TemporalAmount.isPositive: Boolean
    get() = sign > 0

/**
 * [TemporalAmount]가 음수인지 여부를 반환합니다.
 *
 * ```kotlin
 * Duration.ofSeconds(-3).isNegative  // true
 * Duration.ofSeconds(1).isNegative   // false
 * ```
 */
val TemporalAmount.isNegative: Boolean
    get() = sign < 0

/**
 * [TemporalAmount]가 양수가 아닌지 여부를 반환합니다. (0 이하)
 *
 * ```kotlin
 * Duration.ZERO.isNotPositive           // true
 * Duration.ofSeconds(-1).isNotPositive  // true
 * Duration.ofSeconds(1).isNotPositive   // false
 * ```
 */
val TemporalAmount.isNotPositive: Boolean
    get() = sign <= 0

/**
 * [TemporalAmount]가 음수가 아닌지 여부를 반환합니다. (0 이상)
 *
 * ```kotlin
 * Duration.ZERO.isNotNegative          // true
 * Duration.ofSeconds(5).isNotNegative  // true
 * Duration.ofSeconds(-1).isNotNegative // false
 * ```
 */
val TemporalAmount.isNotNegative: Boolean
    get() = sign >= 0

/**
 * [chronoUnit]의 숫자를 [TemporalAmount]로 변환합니다.
 *
 * ```
 * 1.temporalAmount(ChronoUnit.DAYS) // Duration.ofDays(1)
 * 2.temporalAmount(ChronoUnit.HOURS) // Duration.ofHours(2)
 * ```
 */
fun Int.temporalAmount(chronoUnit: ChronoUnit): TemporalAmount =
    toLong().temporalAmount(chronoUnit)

/**
 * [chronoUnit]의 숫자를 [TemporalAmount]로 변환합니다.
 *
 * ```
 * 1L.temporalAmount(ChronoUnit.DAYS) // Duration.ofDays(1)
 * 2L.temporalAmount(ChronoUnit.HOURS) // Duration.ofHours(2)
 * ```
 */
fun Long.temporalAmount(chronoUnit: ChronoUnit): TemporalAmount = when (chronoUnit) {
    ChronoUnit.YEARS  -> Period.ofYears(toIntExact())
    ChronoUnit.MONTHS -> Period.ofMonths(toIntExact())
    ChronoUnit.WEEKS  -> Period.ofWeeks(toIntExact())
    ChronoUnit.DAYS    -> Duration.ofDays(this)
    ChronoUnit.HOURS   -> Duration.ofHours(this)
    ChronoUnit.MINUTES -> Duration.ofMinutes(this)
    ChronoUnit.SECONDS -> Duration.ofSeconds(this)
    ChronoUnit.MILLIS  -> Duration.ofMillis(this)
    ChronoUnit.MICROS  -> Duration.ofNanos(this * 1000L)
    ChronoUnit.NANOS   -> Duration.ofNanos(this)
    else               -> throw IllegalArgumentException("Not supported ChronoUnit. chronounit=$chronoUnit")
}
