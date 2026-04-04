package io.bluetape4k.javatimes

import java.sql.Timestamp
import java.time.Duration
import java.time.Period
import java.util.*

/**
 * [Date]를 생성합니다.
 *
 * ```
 * val date = dateOf()
 * val date = dateOf(1619827200000)
 * ```
 *
 * @param epochMillis 생성할 [Date]의 Epoch Millis (default: [System.currentTimeMillis])
 * @return [Date]
 */
fun dateOf(epochMillis: Long = System.currentTimeMillis()): Date = Date(epochMillis)

/**
 * 두 [Date]의 시간 값을 더합니다.
 *
 * ```kotlin
 * val base = dateOf(1000L)
 * val other = dateOf(500L)
 * val result = base + other  // Date(1500L)
 * ```
 *
 * @param that 더할 [Date]
 * @return 두 시간을 합한 새로운 [Date]
 */
@Deprecated(
    message = "두 Date 값을 더하는 것은 의미론적으로 올바르지 않습니다. Duration을 사용하세요.",
    level = DeprecationLevel.WARNING
)
operator fun Date.plus(that: Date): Date = Date(this.time + that.time)

/**
 * [Date]에 밀리초를 더합니다.
 *
 * ```kotlin
 * val base = dateOf(1_000L)
 * val result = base + 500L  // Date(1_500L)
 * ```
 *
 * @param millis 더할 밀리초
 * @return 밀리초를 더한 새로운 [Date]
 */
operator fun Date.plus(millis: Long): Date = Date(this.time + millis)

/**
 * [Date]에 [Duration]을 더합니다.
 *
 * ```kotlin
 * val base = dateOf(0L)
 * val result = base + Duration.ofHours(1)  // Date(3_600_000L)
 * ```
 *
 * @param duration 더할 [Duration]
 * @return [Duration]을 더한 새로운 [Date]
 */
operator fun Date.plus(duration: Duration): Date = Date(this.time + duration.toMillis())

/**
 * [Date]에 [Period]의 일(days) 수를 더합니다.
 *
 * ```kotlin
 * val base = dateOf(0L)
 * val result = base + Period.ofDays(1)  // Date(86_400_000L)
 * ```
 *
 * @param period 더할 [Period] (years, months는 0이어야 합니다)
 * @return [Period]의 일 수를 더한 새로운 [Date]
 */
operator fun Date.plus(period: Period): Date = Date(this.time + period.inWholeDaysUtc() * MillisPerDay)

/**
 * 두 [Date]의 시간 값을 뺍니다.
 *
 * ```kotlin
 * val base = dateOf(1_500L)
 * val other = dateOf(500L)
 * val result = base - other  // Date(1_000L)
 * ```
 *
 * @param that 뺄 [Date]
 * @return 두 시간의 차이를 나타내는 새로운 [Date]
 */
operator fun Date.minus(that: Date): Date = Date(this.time - that.time)

/**
 * [Date]에서 밀리초를 뺍니다.
 *
 * ```kotlin
 * val base = dateOf(1_500L)
 * val result = base - 500L  // Date(1_000L)
 * ```
 *
 * @param millis 뺄 밀리초
 * @return 밀리초를 뺀 새로운 [Date]
 */
operator fun Date.minus(millis: Long): Date = Date(this.time - millis)

/**
 * [Date]에서 [Duration]을 뺍니다.
 *
 * ```kotlin
 * val base = dateOf(3_600_000L)
 * val result = base - Duration.ofHours(1)  // Date(0L)
 * ```
 *
 * @param duration 뺄 [Duration]
 * @return [Duration]을 뺀 새로운 [Date]
 */
operator fun Date.minus(duration: Duration): Date = Date(this.time - duration.toMillis())

/**
 * [Date]에서 [Period]의 일(days) 수를 뺍니다.
 *
 * ```kotlin
 * val base = dateOf(86_400_000L)
 * val result = base - Period.ofDays(1)  // Date(0L)
 * ```
 *
 * @param period 뺄 [Period] (years, months는 0이어야 합니다)
 * @return [Period]의 일 수를 뺀 새로운 [Date]
 */
operator fun Date.minus(period: Period): Date = Date(this.time - period.inWholeDaysUtc() * MillisPerDay)

/**
 * 두 [Timestamp]의 시간 값을 더합니다.
 *
 * ```kotlin
 * val base = Timestamp(1_000L)
 * val other = Timestamp(500L)
 * val result = base + other  // Timestamp(1_500L)
 * ```
 *
 * @param that 더할 [Timestamp]
 * @return 두 시간을 합한 새로운 [Timestamp]
 */
@Deprecated(
    message = "두 Timestamp 값을 더하는 것은 의미론적으로 올바르지 않습니다. Duration을 사용하세요.",
    level = DeprecationLevel.WARNING
)
operator fun Timestamp.plus(that: Timestamp): Timestamp = Timestamp(this.time + that.time)

/**
 * [Timestamp]에 밀리초를 더합니다.
 *
 * ```kotlin
 * val base = Timestamp(1_000L)
 * val result = base + 500L  // Timestamp(1_500L)
 * ```
 *
 * @param millis 더할 밀리초
 * @return 밀리초를 더한 새로운 [Timestamp]
 */
operator fun Timestamp.plus(millis: Long): Timestamp = Timestamp(this.time + millis)

/**
 * [Timestamp]에 [Duration]을 더합니다.
 *
 * ```kotlin
 * val base = Timestamp(0L)
 * val result = base + Duration.ofSeconds(1)  // Timestamp(1_000L)
 * ```
 *
 * @param duration 더할 [Duration]
 * @return [Duration]을 더한 새로운 [Timestamp]
 */
operator fun Timestamp.plus(duration: Duration): Timestamp = Timestamp(this.time + duration.toMillis())

/**
 * [Timestamp]에 [Period]의 일(days) 수를 더합니다.
 *
 * ```kotlin
 * val base = Timestamp(0L)
 * val result = base + Period.ofDays(1)  // Timestamp(86_400_000L)
 * ```
 *
 * @param period 더할 [Period] (years, months는 0이어야 합니다)
 * @return [Period]의 일 수를 더한 새로운 [Timestamp]
 */
operator fun Timestamp.plus(period: Period): Timestamp = Timestamp(this.time + period.inWholeDaysUtc() * MillisPerDay)

/**
 * 두 [Timestamp]의 시간 값을 뺍니다.
 *
 * ```kotlin
 * val base = Timestamp(1_500L)
 * val other = Timestamp(500L)
 * val result = base - other  // Timestamp(1_000L)
 * ```
 *
 * @param that 뺄 [Timestamp]
 * @return 두 시간의 차이를 나타내는 새로운 [Timestamp]
 */
operator fun Timestamp.minus(that: Timestamp): Timestamp = Timestamp(this.time - that.time)

/**
 * [Timestamp]에서 밀리초를 뺍니다.
 *
 * ```kotlin
 * val base = Timestamp(1_500L)
 * val result = base - 500L  // Timestamp(1_000L)
 * ```
 *
 * @param millis 뺄 밀리초
 * @return 밀리초를 뺀 새로운 [Timestamp]
 */
operator fun Timestamp.minus(millis: Long): Timestamp = Timestamp(this.time - millis)

/**
 * [Timestamp]에서 [Duration]을 뺍니다.
 *
 * ```kotlin
 * val base = Timestamp(1_000L)
 * val result = base - Duration.ofSeconds(1)  // Timestamp(0L)
 * ```
 *
 * @param duration 뺄 [Duration]
 * @return [Duration]을 뺀 새로운 [Timestamp]
 */
operator fun Timestamp.minus(duration: Duration): Timestamp = Timestamp(this.time - duration.toMillis())

/**
 * [Timestamp]에서 [Period]의 일(days) 수를 뺍니다.
 *
 * ```kotlin
 * val base = Timestamp(86_400_000L)
 * val result = base - Period.ofDays(1)  // Timestamp(0L)
 * ```
 *
 * @param period 뺄 [Period] (years, months는 0이어야 합니다)
 * @return [Period]의 일 수를 뺀 새로운 [Timestamp]
 */
operator fun Timestamp.minus(period: Period): Timestamp = Timestamp(this.time - period.inWholeDaysUtc() * MillisPerDay)

private fun Period.inWholeDaysUtc(): Long {
    require(years == 0 && months == 0) {
        "Period with years or months cannot be converted to UTC day-based milliseconds accurately."
    }
    return days.toLong()
}
