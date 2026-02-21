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
 * @param that 더할 [Date]
 * @return 두 시간을 합한 새로운 [Date]
 */
operator fun Date.plus(that: Date): Date = Date(this.time + that.time)

/**
 * [Date]에 밀리초를 더합니다.
 *
 * @param millis 더할 밀리초
 * @return 밀리초를 더한 새로운 [Date]
 */
operator fun Date.plus(millis: Long): Date = Date(this.time + millis)

/**
 * [Date]에 [Duration]을 더합니다.
 *
 * @param duration 더할 [Duration]
 * @return [Duration]을 더한 새로운 [Date]
 */
operator fun Date.plus(duration: Duration): Date = Date(this.time + duration.toMillis())

/**
 * [Date]에 [Period]의 일(days) 수를 더합니다.
 *
 * @param period 더할 [Period]
 * @return [Period]의 일 수를 더한 새로운 [Date]
 */
operator fun Date.plus(period: Period): Date = Date(this.time + period.inWholeDaysUtc() * MILLIS_IN_DAY)

/**
 * 두 [Date]의 시간 값을 뺍니다.
 *
 * @param that 뺄 [Date]
 * @return 두 시간의 차이를 나타내는 새로운 [Date]
 */
operator fun Date.minus(that: Date): Date = Date(this.time - that.time)

/**
 * [Date]에서 밀리초를 뺍니다.
 *
 * @param millis 뺄 밀리초
 * @return 밀리초를 뺀 새로운 [Date]
 */
operator fun Date.minus(millis: Long): Date = Date(this.time - millis)

/**
 * [Date]에서 [Duration]을 뺍니다.
 *
 * @param duration 뺄 [Duration]
 * @return [Duration]을 뺀 새로운 [Date]
 */
operator fun Date.minus(duration: Duration): Date = Date(this.time - duration.toMillis())

/**
 * [Date]에서 [Period]의 일(days) 수를 뺍니다.
 *
 * @param period 뺄 [Period]
 * @return [Period]의 일 수를 뺀 새로운 [Date]
 */
operator fun Date.minus(period: Period): Date = Date(this.time - period.inWholeDaysUtc() * MILLIS_IN_DAY)

/**
 * 두 [Timestamp]의 시간 값을 더합니다.
 *
 * @param that 더할 [Timestamp]
 * @return 두 시간을 합한 새로운 [Timestamp]
 */
operator fun Timestamp.plus(that: Timestamp): Timestamp = Timestamp(this.time + that.time)

/**
 * [Timestamp]에 밀리초를 더합니다.
 *
 * @param millis 더할 밀리초
 * @return 밀리초를 더한 새로운 [Timestamp]
 */
operator fun Timestamp.plus(millis: Long): Timestamp = Timestamp(this.time + millis)

/**
 * [Timestamp]에 [Duration]을 더합니다.
 *
 * @param duration 더할 [Duration]
 * @return [Duration]을 더한 새로운 [Timestamp]
 */
operator fun Timestamp.plus(duration: Duration): Timestamp = Timestamp(this.time + duration.toMillis())

/**
 * [Timestamp]에 [Period]의 일(days) 수를 더합니다.
 *
 * @param period 더할 [Period]
 * @return [Period]의 일 수를 더한 새로운 [Timestamp]
 */
operator fun Timestamp.plus(period: Period): Timestamp = Timestamp(this.time + period.inWholeDaysUtc() * MILLIS_IN_DAY)

/**
 * 두 [Timestamp]의 시간 값을 뺍니다.
 *
 * @param that 뺄 [Timestamp]
 * @return 두 시간의 차이를 나타내는 새로운 [Timestamp]
 */
operator fun Timestamp.minus(that: Timestamp): Timestamp = Timestamp(this.time - that.time)

/**
 * [Timestamp]에서 밀리초를 뺍니다.
 *
 * @param millis 뺄 밀리초
 * @return 밀리초를 뺀 새로운 [Timestamp]
 */
operator fun Timestamp.minus(millis: Long): Timestamp = Timestamp(this.time - millis)

/**
 * [Timestamp]에서 [Duration]을 뺍니다.
 *
 * @param duration 뺄 [Duration]
 * @return [Duration]을 뺀 새로운 [Timestamp]
 */
operator fun Timestamp.minus(duration: Duration): Timestamp = Timestamp(this.time - duration.toMillis())

/**
 * [Timestamp]에서 [Period]의 일(days) 수를 뺍니다.
 *
 * @param period 뺄 [Period]
 * @return [Period]의 일 수를 뺀 새로운 [Timestamp]
 */
operator fun Timestamp.minus(period: Period): Timestamp = Timestamp(this.time - period.inWholeDaysUtc() * MILLIS_IN_DAY)

private fun Period.inWholeDaysUtc(): Long {
    require(years == 0 && months == 0) {
        "Period with years or months cannot be converted to UTC day-based milliseconds accurately."
    }
    return days.toLong()
}
