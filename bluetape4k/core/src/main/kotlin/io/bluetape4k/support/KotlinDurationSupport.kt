package io.bluetape4k.support

import java.time.Instant
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

/** Kotlin [Duration]이 양수인지 검증하고 원래 값을 반환합니다. */
inline fun Duration.requirePositiveDuration(
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): Duration = apply {
    require(this > Duration.ZERO) {
        lazyMessage?.invoke() ?: "$parameterName must be positive."
    }
}

/** Kotlin [Duration]이 0 이하인지 검증하고 원래 값을 반환합니다. */
inline fun Duration.requireZeroOrNegativeDuration(
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): Duration = apply {
    require(this <= Duration.ZERO) {
        lazyMessage?.invoke() ?: "$parameterName must be zero or negative."
    }
}

/** Kotlin [Duration]이 0 이상인지 검증하고 원래 값을 반환합니다. */
inline fun Duration.requireZeroOrPositiveDuration(
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): Duration = apply {
    require(this >= Duration.ZERO) {
        lazyMessage?.invoke() ?: "$parameterName must be zero or positive."
    }
}

/** Kotlin [Duration]이 음수인지 검증하고 원래 값을 반환합니다. */
inline fun Duration.requireNegativeDuration(
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): Duration = apply {
    require(this < Duration.ZERO) {
        lazyMessage?.invoke() ?: "$parameterName must be negative."
    }
}

/** Kotlin [Duration]이 양수인지 불변 조건으로 검증합니다. */
inline fun Duration.checkPositiveDuration(
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): Duration = apply {
    check(this > Duration.ZERO) {
        lazyMessage?.invoke() ?: "$parameterName must be positive."
    }
}

/** Kotlin [Duration]이 0 이하인지 불변 조건으로 검증합니다. */
inline fun Duration.checkZeroOrNegativeDuration(
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): Duration = apply {
    check(this <= Duration.ZERO) {
        lazyMessage?.invoke() ?: "$parameterName must be zero or negative."
    }
}

/** Kotlin [Duration]이 0 이상인지 불변 조건으로 검증합니다. */
inline fun Duration.checkZeroOrPositiveDuration(
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): Duration = apply {
    check(this >= Duration.ZERO) {
        lazyMessage?.invoke() ?: "$parameterName must be zero or positive."
    }
}

/** Kotlin [Duration]이 음수인지 불변 조건으로 검증합니다. */
inline fun Duration.checkNegativeDuration(
    parameterName: String,
    noinline lazyMessage: (() -> Any)? = null,
): Duration = apply {
    check(this < Duration.ZERO) {
        lazyMessage?.invoke() ?: "$parameterName must be negative."
    }
}

/**
 * [kotlin.time.Duration]에서 milliseconds를 제외한 나노초 값(0..999_999)을 반환합니다.
 *
 * ```kotlin
 * val duration = 1500.milliseconds + 123456.nanoseconds
 * duration.nanosOfMillis  // 123456
 *
 * 2.seconds.nanosOfMillis  // 0
 * ```
 *
 * @return 밀리초 단위를 제외한 나노초 부분 (0..999_999)
 */
val Duration.nanosOfMillis: Int
    get() = abs((inWholeNanoseconds % 1_000_000).toInt())


/**
 * [kotlin.time.Duration] 동안 sleep 합니다.
 *
 * ```kotlin
 * 1.seconds.sleep()
 * ```
 */
fun Duration.sleep() {
    require(!this.isNegative()) { "Duration must not be negative. duration=$this" }
    if (this == Duration.ZERO) return

    val finishInstant = Instant.now().plus(this.toJavaDuration())
    generateSequence(this) {
        java.time.Duration.between(Instant.now(), finishInstant).toKotlinDuration()
            .takeIf { remaining -> !remaining.isNegative() }
    }.forEach { remaining ->
        Thread.sleep(remaining.inWholeMilliseconds, remaining.nanosOfMillis)
    }
}
