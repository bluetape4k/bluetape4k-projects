package io.bluetape4k.support

import java.time.Duration
import java.time.temporal.Temporal

operator fun Duration.unaryMinus(): Duration = this.negated()

/**
 * [java.time.Duration] 이 양수가 아님을 확인한다.
 *
 * @return 0 보다 작거나 같으면 true
 */
val Duration.isNotPositive: Boolean get() = this <= Duration.ZERO

/**
 * [java.time.Duration] 이 음수가 아님을 확인한다.
 *
 * @return 0 보다 크거나 같으면 true
 */
val Duration.isNotNegative: Boolean get() = this >= Duration.ZERO

/**
 * Duration을 millseconds 로 환산
 */
fun Duration.inMillis(): Long = toMillis()

/**
 * Duration을 nano seconds로 환산
 */
fun Duration.inNanos(): Long = toNanos() // seconds * NANO_PER_SECOND + nano

/**
 * Duration의 milliseconds 까지를 제외한 nonoseconds 부분만의 값
 */
fun Duration.nanosOfMillis(): Int = (toNanos() % 1_000_000).toInt()

/**
 * [startInclusive] ~ [endExclusive] 의 기간을 [Duration]으로 빌드합니다.
 *
 * @param startInclusive 시작 시각
 * @param endExclusive 끝 시각
 * @return Duration
 */
fun durationOf(startInclusive: Temporal, endExclusive: Temporal): Duration =
    Duration.between(startInclusive, endExclusive)
