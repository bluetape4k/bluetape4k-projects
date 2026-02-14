package io.bluetape4k.protobuf

import com.google.protobuf.Duration
import com.google.protobuf.util.Durations

@JvmField
val PROTO_DURATION_MIN: ProtoDuration = Durations.MIN_VALUE

@JvmField
val PROTO_DURATION_MAX: ProtoDuration = Durations.MAX_VALUE

@JvmField
val PROTO_DURATION_ZERO: ProtoDuration = Durations.ZERO

/**
 * gRPC/Protobuf 처리에서 `compareTo` 함수를 제공합니다.
 */
operator fun ProtoDuration.compareTo(other: ProtoDuration): Int = Durations.compare(this, other)

val ProtoDuration.isValid: Boolean get() = Durations.isValid(this)
val ProtoDuration.isPositive: Boolean get() = Durations.isPositive(this)
val ProtoDuration.isNegative: Boolean get() = Durations.isNegative(this)

/**
 * Convert Duration to string format. The string format will contains 3, 6, or 9 fractional digits
 * depending on the precision required to represent the exact Duration value. For example: "1s",
 * "1.010s", "1.000000100s", "-3.100s" The range that can be represented by Duration is from
 * -315,576,000,000 to +315,576,000,000 inclusive (in seconds).
 *
 * @return The string representation of the given duration.
 */
fun ProtoDuration.asString(): String = Durations.toString(this)

/**
 * gRPC/Protobuf 처리에서 `protoDurationOf` 함수를 제공합니다.
 */
fun protoDurationOf(value: String): ProtoDuration = Durations.parse(value)

/**
 * gRPC/Protobuf 처리에서 `protoDurationOfUnchecked` 함수를 제공합니다.
 */
fun protoDurationOfUnchecked(value: String): ProtoDuration = Durations.parseUnchecked(value)

/**
 * gRPC/Protobuf 처리에서 `protoDurationOf` 함수를 제공합니다.
 */
fun protoDurationOf(duration: java.time.Duration): ProtoDuration = com.google.protobuf.duration {
    this.seconds = duration.seconds
    this.nanos = duration.nano
}

/**
 * gRPC/Protobuf 처리에서 `protoDurationOfDays` 함수를 제공합니다.
 */
fun protoDurationOfDays(days: Long): ProtoDuration = Durations.fromDays(days)

/**
 * gRPC/Protobuf 처리에서 `protoDurationOfHours` 함수를 제공합니다.
 */
fun protoDurationOfHours(hours: Long): ProtoDuration = Durations.fromHours(hours)

/**
 * gRPC/Protobuf 처리에서 `protoDurationOfMinutes` 함수를 제공합니다.
 */
fun protoDurationOfMinutes(minutes: Long): ProtoDuration = Durations.fromMinutes(minutes)

/**
 * gRPC/Protobuf 처리에서 `protoDurationOfSeconds` 함수를 제공합니다.
 */
fun protoDurationOfSeconds(seconds: Long): ProtoDuration = Durations.fromSeconds(seconds)

/**
 * gRPC/Protobuf 처리에서 `protoDurationOfMillis` 함수를 제공합니다.
 */
fun protoDurationOfMillis(millis: Long): ProtoDuration = Durations.fromMillis(millis)

/**
 * gRPC/Protobuf 처리에서 `protoDurationOfMicros` 함수를 제공합니다.
 */
fun protoDurationOfMicros(micros: Long): ProtoDuration = Durations.fromMicros(micros)

/**
 * gRPC/Protobuf 처리에서 `protoDurationOfNanos` 함수를 제공합니다.
 */
fun protoDurationOfNanos(nanos: Long): ProtoDuration = Durations.fromNanos(nanos)

/**
 * gRPC/Protobuf 처리 타입 변환을 위한 `toJavaDuration` 함수를 제공합니다.
 */
fun ProtoDuration.toJavaDuration(): java.time.Duration = java.time.Duration.ofSeconds(seconds, nanos.toLong())

/**
 * [java.time.Duration]을 protobuf의 [com.google.protobuf.Duration] 수형으로 변환합니다.
 */
fun java.time.Duration.toProtoDuration(): Duration =
    Duration.newBuilder()
        .setSeconds(this@toProtoDuration.seconds)
        .setNanos(this@toProtoDuration.nano)
        .build()

/**
 * gRPC/Protobuf 처리 타입 변환을 위한 `toDays` 함수를 제공합니다.
 */
fun ProtoDuration.toDays(): Long = Durations.toDays(this)

/**
 * gRPC/Protobuf 처리 타입 변환을 위한 `toHours` 함수를 제공합니다.
 */
fun ProtoDuration.toHours(): Long = Durations.toHours(this)

/**
 * gRPC/Protobuf 처리 타입 변환을 위한 `toMinutes` 함수를 제공합니다.
 */
fun ProtoDuration.toMinutes(): Long = Durations.toMinutes(this)

/**
 * gRPC/Protobuf 처리 타입 변환을 위한 `toSeconds` 함수를 제공합니다.
 */
fun ProtoDuration.toSeconds(): Long = Durations.toSeconds(this)

/**
 * gRPC/Protobuf 처리 타입 변환을 위한 `toSecondsAsDouble` 함수를 제공합니다.
 */
fun ProtoDuration.toSecondsAsDouble(): Double = Durations.toSecondsAsDouble(this)

/**
 * gRPC/Protobuf 처리 타입 변환을 위한 `toMillis` 함수를 제공합니다.
 */
fun ProtoDuration.toMillis(): Long = Durations.toMillis(this)

/**
 * gRPC/Protobuf 처리 타입 변환을 위한 `toMicros` 함수를 제공합니다.
 */
fun ProtoDuration.toMicros(): Long = Durations.toMicros(this)

/**
 * gRPC/Protobuf 처리 타입 변환을 위한 `toNanos` 함수를 제공합니다.
 */
fun ProtoDuration.toNanos(): Long = Durations.toNanos(this)

/**
 * gRPC/Protobuf 처리에서 `plus` 함수를 제공합니다.
 */
operator fun ProtoDuration.plus(other: ProtoDuration): ProtoDuration =
    (this.toJavaDuration() + other.toJavaDuration()).toProtoDuration()

/**
 * gRPC/Protobuf 처리에서 `minus` 함수를 제공합니다.
 */
operator fun ProtoDuration.minus(other: ProtoDuration): ProtoDuration =
    (this.toJavaDuration() - other.toJavaDuration()).toProtoDuration()
