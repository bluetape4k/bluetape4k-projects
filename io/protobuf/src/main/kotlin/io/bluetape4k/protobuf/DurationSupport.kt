package io.bluetape4k.protobuf

import com.google.protobuf.Duration
import com.google.protobuf.util.Durations

/**
 * Protobuf Duration 최소값 상수입니다.
 *
 * ## 동작/계약
 * - [Durations.MIN_VALUE]를 그대로 노출합니다.
 * - 불변 protobuf 인스턴스를 공유하며 새 객체를 만들지 않습니다.
 *
 * ```kotlin
 * val min = PROTO_DURATION_MIN
 * // min.seconds == -315_576_000_000L
 * ```
 */
@JvmField
val PROTO_DURATION_MIN: ProtoDuration = Durations.MIN_VALUE

/**
 * Protobuf Duration 최대값 상수입니다.
 *
 * ## 동작/계약
 * - [Durations.MAX_VALUE]를 그대로 노출합니다.
 * - 불변 protobuf 인스턴스를 공유하며 새 객체를 만들지 않습니다.
 *
 * ```kotlin
 * val max = PROTO_DURATION_MAX
 * // max.seconds == 315_576_000_000L
 * ```
 */
@JvmField
val PROTO_DURATION_MAX: ProtoDuration = Durations.MAX_VALUE

/**
 * Protobuf Duration 0초 상수입니다.
 *
 * ## 동작/계약
 * - [Durations.ZERO]를 그대로 노출합니다.
 * - 불변 protobuf 인스턴스를 공유하며 새 객체를 만들지 않습니다.
 *
 * ```kotlin
 * val zero = PROTO_DURATION_ZERO
 * // zero.seconds == 0L
 * ```
 */
@JvmField
val PROTO_DURATION_ZERO: ProtoDuration = Durations.ZERO

/**
 * 두 Protobuf Duration을 시간 길이 기준으로 비교합니다.
 *
 * ## 동작/계약
 * - [Durations.compare]를 호출해 음수/0/양수를 반환합니다.
 * - 입력 인스턴스는 변경하지 않습니다.
 *
 * ```kotlin
 * val cmp = protoDurationOfSeconds(1).compareTo(protoDurationOfSeconds(2))
 * // cmp < 0
 * ```
 */
operator fun ProtoDuration.compareTo(other: ProtoDuration): Int = Durations.compare(this, other)

/**
 * Duration 값이 protobuf 규격 범위에 있으면 `true`입니다.
 *
 * ```kotlin
 * val valid = protoDurationOfSeconds(1).isValid
 * // valid == true
 * ```
 */
val ProtoDuration.isValid: Boolean get() = Durations.isValid(this)

/**
 * Duration 값이 0보다 크면 `true`입니다.
 *
 * ```kotlin
 * val pos = protoDurationOfSeconds(1).isPositive
 * // pos == true
 * ```
 */
val ProtoDuration.isPositive: Boolean get() = Durations.isPositive(this)

/**
 * Duration 값이 0보다 작으면 `true`입니다.
 *
 * ```kotlin
 * val neg = protoDurationOfSeconds(-1).isNegative
 * // neg == true
 * ```
 */
val ProtoDuration.isNegative: Boolean get() = Durations.isNegative(this)

/**
 * Protobuf Duration을 문자열(`"1.500s"` 형식)로 변환합니다.
 *
 * ## 동작/계약
 * - [Durations.toString] 포맷을 그대로 사용합니다.
 * - 유효 범위를 벗어난 값이면 [IllegalArgumentException]이 발생할 수 있습니다.
 *
 * ```kotlin
 * val text = protoDurationOfMillis(1500).asString()
 * // text == "1.500s"
 * ```
 */
fun ProtoDuration.asString(): String = Durations.toString(this)

/**
 * Duration 문자열을 Protobuf Duration으로 파싱합니다.
 *
 * ## 동작/계약
 * - [Durations.parse]를 사용하며 형식은 `Xs`, `X.Ys`를 따릅니다.
 * - 형식이 잘못되거나 범위를 벗어나면 [java.text.ParseException] 또는 [IllegalArgumentException]이 발생합니다.
 *
 * ```kotlin
 * val duration = protoDurationOf("1.500s")
 * // duration.toMillis() == 1500L
 * ```
 */
fun protoDurationOf(value: String): ProtoDuration = Durations.parse(value)

/**
 * Duration 문자열을 검증 없이 Protobuf Duration으로 파싱합니다.
 *
 * ## 동작/계약
 * - [Durations.parseUnchecked]를 사용합니다.
 * - 입력이 잘못되면 런타임 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val duration = protoDurationOfUnchecked("2s")
 * // duration.seconds == 2L
 * ```
 */
fun protoDurationOfUnchecked(value: String): ProtoDuration = Durations.parseUnchecked(value)

/**
 * Java Duration을 Protobuf Duration으로 변환합니다.
 *
 * ## 동작/계약
 * - `seconds/nano` 필드를 그대로 복사합니다.
 * - 입력 객체는 변경하지 않고 새 protobuf 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val proto = protoDurationOf(java.time.Duration.ofMillis(1200))
 * // proto.toMillis() == 1200L
 * ```
 */
fun protoDurationOf(duration: java.time.Duration): ProtoDuration = com.google.protobuf.duration {
    this.seconds = duration.seconds
    this.nanos = duration.nano
}

/**
 * 일(day) 단위를 Protobuf Duration으로 생성합니다.
 *
 * ```kotlin
 * val d = protoDurationOfDays(2)
 * // d.toSeconds() == 172_800L
 * ```
 */
fun protoDurationOfDays(days: Long): ProtoDuration = Durations.fromDays(days)

/**
 * 시(hour) 단위를 Protobuf Duration으로 생성합니다.
 *
 * ```kotlin
 * val d = protoDurationOfHours(3)
 * // d.toSeconds() == 10_800L
 * ```
 */
fun protoDurationOfHours(hours: Long): ProtoDuration = Durations.fromHours(hours)

/**
 * 분(minute) 단위를 Protobuf Duration으로 생성합니다.
 *
 * ```kotlin
 * val d = protoDurationOfMinutes(5)
 * // d.toSeconds() == 300L
 * ```
 */
fun protoDurationOfMinutes(minutes: Long): ProtoDuration = Durations.fromMinutes(minutes)

/**
 * 초(second) 단위를 Protobuf Duration으로 생성합니다.
 *
 * ```kotlin
 * val d = protoDurationOfSeconds(30)
 * // d.toSeconds() == 30L
 * ```
 */
fun protoDurationOfSeconds(seconds: Long): ProtoDuration = Durations.fromSeconds(seconds)

/**
 * 밀리초를 Protobuf Duration으로 생성합니다.
 *
 * ```kotlin
 * val d = protoDurationOfMillis(1500)
 * // d.toMillis() == 1500L
 * ```
 */
fun protoDurationOfMillis(millis: Long): ProtoDuration = Durations.fromMillis(millis)

/**
 * 마이크로초를 Protobuf Duration으로 생성합니다.
 *
 * ```kotlin
 * val d = protoDurationOfMicros(2_000_000)
 * // d.toMicros() == 2_000_000L
 * ```
 */
fun protoDurationOfMicros(micros: Long): ProtoDuration = Durations.fromMicros(micros)

/**
 * 나노초를 Protobuf Duration으로 생성합니다.
 *
 * ```kotlin
 * val d = protoDurationOfNanos(1_000_000_000)
 * // d.toNanos() == 1_000_000_000L
 * ```
 */
fun protoDurationOfNanos(nanos: Long): ProtoDuration = Durations.fromNanos(nanos)

/**
 * Protobuf Duration을 Java Duration으로 변환합니다.
 *
 * ## 동작/계약
 * - `seconds/nanos`를 그대로 사용해 [java.time.Duration.ofSeconds]를 호출합니다.
 * - 입력 객체는 변경하지 않고 새 Java Duration을 반환합니다.
 *
 * ```kotlin
 * val javaDuration = protoDurationOfMillis(1500).toJavaDuration()
 * // javaDuration.toMillis() == 1500L
 * ```
 */
fun ProtoDuration.toJavaDuration(): java.time.Duration = java.time.Duration.ofSeconds(seconds, nanos.toLong())

/**
 * Java Duration을 Protobuf Duration으로 변환합니다.
 *
 * ## 동작/계약
 * - `seconds/nano` 값을 protobuf builder에 그대로 설정합니다.
 * - 입력 객체는 변경하지 않고 새 protobuf 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val proto = java.time.Duration.ofSeconds(3).toProtoDuration()
 * // proto.seconds == 3L
 * ```
 */
fun java.time.Duration.toProtoDuration(): Duration =
    Duration.newBuilder()
        .setSeconds(this@toProtoDuration.seconds)
        .setNanos(this@toProtoDuration.nano)
        .build()

/**
 * Protobuf Duration을 일(day) 단위로 내림 변환합니다.
 *
 * ```kotlin
 * val days = protoDurationOfHours(25).toDays()
 * // days == 1L
 * ```
 */
fun ProtoDuration.toDays(): Long = Durations.toDays(this)

/**
 * Protobuf Duration을 시(hour) 단위로 내림 변환합니다.
 *
 * ```kotlin
 * val hours = protoDurationOfMinutes(90).toHours()
 * // hours == 1L
 * ```
 */
fun ProtoDuration.toHours(): Long = Durations.toHours(this)

/**
 * Protobuf Duration을 분(minute) 단위로 내림 변환합니다.
 *
 * ```kotlin
 * val minutes = protoDurationOfSeconds(90).toMinutes()
 * // minutes == 1L
 * ```
 */
fun ProtoDuration.toMinutes(): Long = Durations.toMinutes(this)

/**
 * Protobuf Duration을 초(second) 단위로 변환합니다.
 *
 * ```kotlin
 * val seconds = protoDurationOfMillis(2500).toSeconds()
 * // seconds == 2L
 * ```
 */
fun ProtoDuration.toSeconds(): Long = Durations.toSeconds(this)

/**
 * Protobuf Duration을 소수점 초(Double)로 변환합니다.
 *
 * ```kotlin
 * val secs = protoDurationOfMillis(1500).toSecondsAsDouble()
 * // secs == 1.5
 * ```
 */
fun ProtoDuration.toSecondsAsDouble(): Double = Durations.toSecondsAsDouble(this)

/**
 * Protobuf Duration을 밀리초로 변환합니다.
 *
 * ```kotlin
 * val ms = protoDurationOfSeconds(2).toMillis()
 * // ms == 2000L
 * ```
 */
fun ProtoDuration.toMillis(): Long = Durations.toMillis(this)

/**
 * Protobuf Duration을 마이크로초로 변환합니다.
 *
 * ```kotlin
 * val us = protoDurationOfMillis(1).toMicros()
 * // us == 1_000L
 * ```
 */
fun ProtoDuration.toMicros(): Long = Durations.toMicros(this)

/**
 * Protobuf Duration을 나노초로 변환합니다.
 *
 * ```kotlin
 * val ns = protoDurationOfMillis(1).toNanos()
 * // ns == 1_000_000L
 * ```
 */
fun ProtoDuration.toNanos(): Long = Durations.toNanos(this)

/**
 * 두 Protobuf Duration을 더한 새 Duration을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 Java Duration으로 변환 후 덧셈합니다.
 * - 수신 객체와 인자는 변경하지 않습니다.
 *
 * ```kotlin
 * val sum = protoDurationOfSeconds(1) + protoDurationOfMillis(500)
 * // sum.toMillis() == 1500L
 * ```
 */
operator fun ProtoDuration.plus(other: ProtoDuration): ProtoDuration =
    (this.toJavaDuration() + other.toJavaDuration()).toProtoDuration()

/**
 * 다른 Protobuf Duration을 뺀 새 Duration을 반환합니다.
 *
 * ## 동작/계약
 * - 내부적으로 Java Duration으로 변환 후 뺄셈합니다.
 * - 수신 객체와 인자는 변경하지 않습니다.
 *
 * ```kotlin
 * val diff = protoDurationOfSeconds(2) - protoDurationOfMillis(500)
 * // diff.toMillis() == 1500L
 * ```
 */
operator fun ProtoDuration.minus(other: ProtoDuration): ProtoDuration =
    (this.toJavaDuration() - other.toJavaDuration()).toProtoDuration()
