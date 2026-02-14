package io.bluetape4k.protobuf

import com.google.protobuf.Timestamp
import com.google.protobuf.timestamp
import com.google.protobuf.util.Timestamps
import java.time.Instant
import java.util.*

@JvmField
val PROTO_TIMESTAMP_MIN: ProtoTimestamp = Timestamps.MIN_VALUE

@JvmField
val PROTO_TIMESTAMP_MAX: ProtoTimestamp = Timestamps.MAX_VALUE

@JvmField
val PROTO_TIMESTAMP_EPOCH: ProtoTimestamp = Timestamps.EPOCH

/**
 * gRPC/Protobuf 처리에서 `compareTo` 함수를 제공합니다.
 */
operator fun ProtoTimestamp.compareTo(other: ProtoTimestamp): Int = Timestamps.compare(this, other)

val ProtoTimestamp.isValid: Boolean get() = Timestamps.isValid(this)

/**
 * Convert Timestamp to RFC 3339 date string format. The output will always be Z-normalized and
 * uses 3, 6 or 9 fractional digits as required to represent the exact value. Note that Timestamp
 * can only represent time from 0001-01-01T00:00:00Z to 9999-12-31T23:59:59.999999999Z. See
 * https://www.ietf.org/rfc/rfc3339.txt
 *
 * <p>Example of generated format: "1972-01-01T10:00:20.021Z"
 *
 * @return The string representation of the given timestamp.
 * @throws IllegalArgumentException if the given timestamp is not in the valid range.
 */
fun ProtoTimestamp.asString(): String = Timestamps.toString(this)

/**
 * gRPC/Protobuf 처리에서 `protoTimestampOf` 함수를 제공합니다.
 */
fun protoTimestampOf(value: String): ProtoTimestamp = Timestamps.parse(value)

/**
 * gRPC/Protobuf 처리에서 `protoTimestampOfUnchecked` 함수를 제공합니다.
 */
fun protoTimestampOfUnchecked(value: String): ProtoTimestamp = Timestamps.parseUnchecked(value)

/**
 * Create a Timestamp from a java.util.Date. If the java.util.Date is a java.sql.Timestamp,
 * full nanonsecond precision is retained.
 *
 * @throws IllegalArgumentException if the year is before 1 CE or after 9999 CE
 */
fun protoTimestampOf(date: Date): ProtoTimestamp = Timestamps.fromDate(date)

/**
 * gRPC/Protobuf 처리에서 `protoTimestampOf` 함수를 제공합니다.
 */
fun protoTimestampOf(instant: Instant): ProtoTimestamp =
    timestamp {
        seconds = instant.epochSecond
        nanos = instant.nano
    }

/** Create a Timestamp from the number of seconds elapsed from the epoch. */
fun protoTimestampOfSeconds(seconds: Long): ProtoTimestamp = Timestamps.fromSeconds(seconds)

/** Create a Timestamp from the number of milliseconds elapsed from the epoch. */
fun protoTimestampOfMillis(millis: Long): ProtoTimestamp = Timestamps.fromMillis(millis)

/** Create a Timestamp from the number of microseconds elapsed from the epoch. */
fun protoTimestampOfMicros(micros: Long): ProtoTimestamp = Timestamps.fromMicros(micros)

/** Create a Timestamp from the number of nanoseconds elapsed from the epoch. */
fun protoTimestampOfNanos(nanos: Long): ProtoTimestamp = Timestamps.fromNanos(nanos)

/**
 * gRPC/Protobuf 처리 타입 변환을 위한 `toInstant` 함수를 제공합니다.
 */
fun ProtoTimestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanos.toLong())

/**
 * Convert a Timestamp to the number of seconds elapsed from the epoch.
 *
 * ```
 * The result will be rounded down to the nearest second. E.g., if the timestamp represents
 * "1969-12-31T23:59:59.999999999Z", it will be rounded to -1 second.
 * ```
 */
fun ProtoTimestamp.toSeconds(): Long = Timestamps.toSeconds(this)

/**
 * gRPC/Protobuf 처리 타입 변환을 위한 `toMillis` 함수를 제공합니다.
 */
fun ProtoTimestamp.toMillis(): Long = Timestamps.toMillis(this)

/**
 * gRPC/Protobuf 처리 타입 변환을 위한 `toMicros` 함수를 제공합니다.
 */
fun ProtoTimestamp.toMicros(): Long = Timestamps.toMicros(this)

/**
 * gRPC/Protobuf 처리 타입 변환을 위한 `toNanos` 함수를 제공합니다.
 */
fun ProtoTimestamp.toNanos(): Long = Timestamps.toNanos(this)

/**
 * gRPC/Protobuf 처리에서 `protoDurationOf` 함수를 제공합니다.
 */
fun protoDurationOf(from: Timestamp, to: Timestamp): ProtoDuration = Timestamps.between(from, to)

/**
 * gRPC/Protobuf 처리에서 `plus` 함수를 제공합니다.
 */
operator fun ProtoTimestamp.plus(length: ProtoDuration): ProtoTimestamp = Timestamps.add(this, length)

/**
 * gRPC/Protobuf 처리에서 `minus` 함수를 제공합니다.
 */
operator fun ProtoTimestamp.minus(length: ProtoDuration): ProtoTimestamp = Timestamps.subtract(this, length)
