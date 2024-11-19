package io.bluetape4k.protobuf

import com.google.protobuf.Duration
import com.google.protobuf.util.Durations


@JvmField
val PROTO_DURATION_MIN: ProtoDuration = Durations.MIN_VALUE

@JvmField
val PROTO_DURATION_MAX: ProtoDuration = Durations.MAX_VALUE

@JvmField
val PROTO_DURATION_ZERO: ProtoDuration = Durations.ZERO

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

fun protoDurationOf(value: String): ProtoDuration = Durations.parse(value)
fun protoDurationOfUnchecked(value: String): ProtoDuration = Durations.parseUnchecked(value)

fun protoDurationOf(duration: java.time.Duration): ProtoDuration = com.google.protobuf.duration {
    this.seconds = duration.seconds
    this.nanos = duration.nano
}

fun protoDurationOfDays(days: Long): ProtoDuration = Durations.fromDays(days)
fun protoDurationOfHours(hours: Long): ProtoDuration = Durations.fromHours(hours)
fun protoDurationOfMinutes(minutes: Long): ProtoDuration = Durations.fromMinutes(minutes)
fun protoDurationOfSeconds(seconds: Long): ProtoDuration = Durations.fromSeconds(seconds)
fun protoDurationOfMillis(millis: Long): ProtoDuration = Durations.fromMillis(millis)
fun protoDurationOfMicros(micros: Long): ProtoDuration = Durations.fromMicros(micros)
fun protoDurationOfNanos(nanos: Long): ProtoDuration = Durations.fromNanos(nanos)

fun ProtoDuration.toJavaDuration(): java.time.Duration = java.time.Duration.ofSeconds(seconds, nanos.toLong())

/**
 * [java.time.Duration]을 protobuf의 [com.google.protobuf.Duration] 수형으로 변환합니다.
 */
fun java.time.Duration.toProtoDuration(): Duration =
    Duration.newBuilder()
        .setSeconds(this@toProtoDuration.seconds)
        .setNanos(this@toProtoDuration.nano)
        .build()

fun ProtoDuration.toDays(): Long = Durations.toDays(this)
fun ProtoDuration.toHours(): Long = Durations.toHours(this)
fun ProtoDuration.toMinutes(): Long = Durations.toMinutes(this)
fun ProtoDuration.toSeconds(): Long = Durations.toSeconds(this)
fun ProtoDuration.toSecondsAsDouble(): Double = Durations.toSecondsAsDouble(this)
fun ProtoDuration.toMillis(): Long = Durations.toMillis(this)
fun ProtoDuration.toMicros(): Long = Durations.toMicros(this)
fun ProtoDuration.toNanos(): Long = Durations.toNanos(this)

operator fun ProtoDuration.plus(other: ProtoDuration): ProtoDuration =
    (this.toJavaDuration() + other.toJavaDuration()).toProtoDuration()

operator fun ProtoDuration.minus(other: ProtoDuration): ProtoDuration =
    (this.toJavaDuration() - other.toJavaDuration()).toProtoDuration()
