package io.bluetape4k.support

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

/**
 * [kotlin.time.Duration]의 Milliseconds 까지를 제외한 nono seconds 값만 반환합니다.
 */
val Duration.nanosOfMillis: Int
    get() = (inWholeNanoseconds % 1_000_000).toInt()


/**
 * [kotlin.time.Duration] 동안 sleep 합니다.
 *
 * ```
 * 1.seconds.sleep()
 * ```
 */
fun Duration.sleep() {
    val finishInstant = Instant.now().plus(this.toJavaDuration())
    var remainingDuration = this
    do {
        Thread.sleep(remainingDuration.inWholeMilliseconds, remainingDuration.nanosOfMillis)
        remainingDuration = java.time.Duration.between(Instant.now(), finishInstant).toKotlinDuration()
    } while (!remainingDuration.isNegative())
}
