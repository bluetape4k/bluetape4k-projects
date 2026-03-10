package io.bluetape4k.protobuf

import com.google.protobuf.Timestamp
import com.google.protobuf.timestamp
import com.google.protobuf.util.Timestamps
import java.time.Instant
import java.util.*

/**
 * Protobuf Timestamp 최소값 상수입니다.
 *
 * ## 동작/계약
 * - [Timestamps.MIN_VALUE]를 그대로 노출합니다.
 * - 불변 protobuf 인스턴스를 공유하며 새 객체를 만들지 않습니다.
 *
 * ```kotlin
 * val min = PROTO_TIMESTAMP_MIN
 * // min.seconds == -62135596800L
 * ```
 */
@JvmField
val PROTO_TIMESTAMP_MIN: ProtoTimestamp = Timestamps.MIN_VALUE

/**
 * Protobuf Timestamp 최대값 상수입니다.
 *
 * ## 동작/계약
 * - [Timestamps.MAX_VALUE]를 그대로 노출합니다.
 * - 불변 protobuf 인스턴스를 공유하며 새 객체를 만들지 않습니다.
 *
 * ```kotlin
 * val max = PROTO_TIMESTAMP_MAX
 * // max.seconds == 253402300799L
 * ```
 */
@JvmField
val PROTO_TIMESTAMP_MAX: ProtoTimestamp = Timestamps.MAX_VALUE

/**
 * Unix epoch(1970-01-01T00:00:00Z) 상수입니다.
 *
 * ## 동작/계약
 * - [Timestamps.EPOCH]를 그대로 노출합니다.
 * - 불변 protobuf 인스턴스를 공유하며 새 객체를 만들지 않습니다.
 *
 * ```kotlin
 * val epoch = PROTO_TIMESTAMP_EPOCH
 * // epoch.seconds == 0L
 * ```
 */
@JvmField
val PROTO_TIMESTAMP_EPOCH: ProtoTimestamp = Timestamps.EPOCH

/**
 * 두 Protobuf Timestamp를 시점 기준으로 비교합니다.
 *
 * ## 동작/계약
 * - [Timestamps.compare]를 호출해 음수/0/양수를 반환합니다.
 * - 입력 인스턴스는 변경하지 않습니다.
 *
 * ```kotlin
 * val cmp = protoTimestampOfSeconds(1).compareTo(protoTimestampOfSeconds(2))
 * // cmp < 0
 * ```
 */
operator fun ProtoTimestamp.compareTo(other: ProtoTimestamp): Int = Timestamps.compare(this, other)

/** Timestamp 값이 protobuf 규격 범위에 있으면 `true`입니다. */
val ProtoTimestamp.isValid: Boolean get() = Timestamps.isValid(this)

/**
 * Protobuf Timestamp를 RFC3339 문자열로 변환합니다.
 *
 * ## 동작/계약
 * - [Timestamps.toString] 포맷을 그대로 사용합니다.
 * - 유효 범위를 벗어난 값이면 [IllegalArgumentException]이 발생합니다.
 *
 * ```kotlin
 * val text = protoTimestampOfSeconds(0).asString()
 * // text == "1970-01-01T00:00:00Z"
 * ```
 */
fun ProtoTimestamp.asString(): String = Timestamps.toString(this)

/**
 * RFC3339 문자열을 Protobuf Timestamp로 파싱합니다.
 *
 * ## 동작/계약
 * - [Timestamps.parse]를 사용합니다.
 * - 형식이 잘못되거나 범위를 벗어나면 [java.text.ParseException] 또는 [IllegalArgumentException]이 발생합니다.
 *
 * ```kotlin
 * val ts = protoTimestampOf("1970-01-01T00:00:01Z")
 * // ts.seconds == 1L
 * ```
 */
fun protoTimestampOf(value: String): ProtoTimestamp = Timestamps.parse(value)

/**
 * 문자열을 검증 없이 Protobuf Timestamp로 파싱합니다.
 *
 * ## 동작/계약
 * - [Timestamps.parseUnchecked]를 사용합니다.
 * - 입력이 잘못되면 런타임 예외가 발생할 수 있습니다.
 *
 * ```kotlin
 * val ts = protoTimestampOfUnchecked("1970-01-01T00:00:00Z")
 * // ts.seconds == 0L
 * ```
 */
fun protoTimestampOfUnchecked(value: String): ProtoTimestamp = Timestamps.parseUnchecked(value)

/**
 * [Date]를 Protobuf Timestamp로 변환합니다.
 *
 * ## 동작/계약
 * - [Timestamps.fromDate]를 사용합니다.
 * - 표현 범위를 벗어나면 [IllegalArgumentException]이 발생할 수 있습니다.
 *
 * ```kotlin
 * val ts = protoTimestampOf(Date(1_000L))
 * // ts.toMillis() == 1000L
 * ```
 */
fun protoTimestampOf(date: Date): ProtoTimestamp = Timestamps.fromDate(date)

/**
 * [Instant]를 Protobuf Timestamp로 변환합니다.
 *
 * ## 동작/계약
 * - `epochSecond/nano` 필드를 그대로 복사합니다.
 * - 입력 객체는 변경하지 않고 새 protobuf 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val ts = protoTimestampOf(Instant.ofEpochSecond(2, 3))
 * // ts.seconds == 2L
 * ```
 */
fun protoTimestampOf(instant: Instant): ProtoTimestamp =
    timestamp {
        seconds = instant.epochSecond
        nanos = instant.nano
    }

/** Epoch 기준 초 값을 Protobuf Timestamp로 생성합니다. */
fun protoTimestampOfSeconds(seconds: Long): ProtoTimestamp = Timestamps.fromSeconds(seconds)

/** Epoch 기준 밀리초 값을 Protobuf Timestamp로 생성합니다. */
fun protoTimestampOfMillis(millis: Long): ProtoTimestamp = Timestamps.fromMillis(millis)

/** Epoch 기준 마이크로초 값을 Protobuf Timestamp로 생성합니다. */
fun protoTimestampOfMicros(micros: Long): ProtoTimestamp = Timestamps.fromMicros(micros)

/** Epoch 기준 나노초 값을 Protobuf Timestamp로 생성합니다. */
fun protoTimestampOfNanos(nanos: Long): ProtoTimestamp = Timestamps.fromNanos(nanos)

/**
 * Protobuf Timestamp를 [Instant]로 변환합니다.
 *
 * ## 동작/계약
 * - `seconds/nanos`를 그대로 사용합니다.
 * - 입력 객체는 변경하지 않고 새 [Instant]를 반환합니다.
 *
 * ```kotlin
 * val instant = protoTimestampOfSeconds(3).toInstant()
 * // instant.epochSecond == 3L
 * ```
 */
fun ProtoTimestamp.toInstant(): Instant = Instant.ofEpochSecond(seconds, nanos.toLong())

/**
 * Protobuf Timestamp를 epoch 초로 변환합니다.
 *
 * ## 동작/계약
 * - [Timestamps.toSeconds]를 사용하며 소수점 이하 초는 버림 처리됩니다.
 * - 입력 객체는 변경하지 않습니다.
 *
 * ```kotlin
 * val seconds = protoTimestampOfMillis(1999).toSeconds()
 * // seconds == 1L
 * ```
 */
fun ProtoTimestamp.toSeconds(): Long = Timestamps.toSeconds(this)

/** Protobuf Timestamp를 epoch 밀리초로 변환합니다. */
fun ProtoTimestamp.toMillis(): Long = Timestamps.toMillis(this)

/** Protobuf Timestamp를 epoch 마이크로초로 변환합니다. */
fun ProtoTimestamp.toMicros(): Long = Timestamps.toMicros(this)

/** Protobuf Timestamp를 epoch 나노초로 변환합니다. */
fun ProtoTimestamp.toNanos(): Long = Timestamps.toNanos(this)

/**
 * 두 Timestamp 사이의 차이를 Protobuf Duration으로 계산합니다.
 *
 * ## 동작/계약
 * - [from]부터 [to]까지의 길이를 [Timestamps.between]으로 계산합니다.
 * - 두 입력은 변경하지 않고 새 Duration을 반환합니다.
 *
 * ```kotlin
 * val d = protoDurationOf(protoTimestampOfSeconds(1), protoTimestampOfSeconds(3))
 * // d.seconds == 2L
 * ```
 */
fun protoDurationOf(from: Timestamp, to: Timestamp): ProtoDuration = Timestamps.between(from, to)

/**
 * Timestamp에 Duration을 더한 새 Timestamp를 반환합니다.
 *
 * ## 동작/계약
 * - [Timestamps.add]를 사용합니다.
 * - 수신 객체와 인자는 변경하지 않습니다.
 *
 * ```kotlin
 * val ts = protoTimestampOfSeconds(1) + protoDurationOfSeconds(2)
 * // ts.seconds == 3L
 * ```
 */
operator fun ProtoTimestamp.plus(length: ProtoDuration): ProtoTimestamp = Timestamps.add(this, length)

/**
 * Timestamp에서 Duration을 뺀 새 Timestamp를 반환합니다.
 *
 * ## 동작/계약
 * - [Timestamps.subtract]를 사용합니다.
 * - 수신 객체와 인자는 변경하지 않습니다.
 *
 * ```kotlin
 * val ts = protoTimestampOfSeconds(3) - protoDurationOfSeconds(2)
 * // ts.seconds == 1L
 * ```
 */
operator fun ProtoTimestamp.minus(length: ProtoDuration): ProtoTimestamp = Timestamps.subtract(this, length)
