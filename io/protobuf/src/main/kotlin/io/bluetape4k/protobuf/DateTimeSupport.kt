package io.bluetape4k.protobuf

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Protobuf [ProtoDate]를 [LocalDate]로 변환합니다.
 *
 * ## 동작/계약
 * - `year/month/day` 필드를 그대로 사용해 [LocalDate.of]를 호출합니다.
 * - 값 범위가 잘못되면 [java.time.DateTimeException]이 발생할 수 있습니다.
 *
 * ```kotlin
 * val date = protoDate.toLocalDate()
 * // date.year == protoDate.year
 * ```
 */
fun ProtoDate.toLocalDate(): LocalDate = LocalDate.of(year, month, day)

/**
 * [LocalDate]를 [ProtoDate]로 변환합니다.
 *
 * ## 동작/계약
 * - 날짜 필드를 그대로 protobuf builder에 설정합니다.
 * - 입력 객체는 변경하지 않고 새 protobuf 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val proto = LocalDate.of(2024, 1, 2).toProtoDate()
 * // proto.year == 2024
 * ```
 */
fun LocalDate.toProtoDate(): ProtoDate =
    ProtoDate.newBuilder()
        .setYear(year)
        .setMonth(monthValue)
        .setDay(dayOfMonth)
        .build()

/**
 * Protobuf [ProtoDateTime]을 [LocalDateTime]으로 변환합니다.
 *
 * ## 동작/계약
 * - `year..nanos` 필드를 그대로 사용합니다.
 * - 타임존 오프셋 정보는 반영하지 않습니다.
 *
 * ```kotlin
 * val dt = protoDateTime.toLocalDateTime()
 * // dt.second == protoDateTime.seconds
 * ```
 */
fun ProtoDateTime.toLocalDateTime(): LocalDateTime =
    LocalDateTime.of(year, month, day, hours, minutes, seconds, nanos)

/**
 * [LocalDateTime]을 [ProtoDateTime]으로 변환합니다.
 *
 * ## 동작/계약
 * - 날짜/시간 필드를 그대로 protobuf builder에 설정합니다.
 * - 오프셋/타임존 필드는 설정하지 않습니다.
 *
 * ```kotlin
 * val proto = LocalDateTime.of(2024, 1, 2, 3, 4, 5).toProtoDateTime()
 * // proto.hours == 3
 * ```
 */
fun LocalDateTime.toProtoDateTime(): ProtoDateTime =
    ProtoDateTime.newBuilder()
        .setYear(year)
        .setMonth(monthValue)
        .setDay(dayOfMonth)
        .setHours(hour)
        .setMinutes(minute)
        .setSeconds(second)
        .setNanos(nano)
        .build()

/**
 * Protobuf [ProtoTime]을 [LocalTime]으로 변환합니다.
 *
 * ## 동작/계약
 * - `hours/minutes/seconds/nanos`를 그대로 사용합니다.
 * - 값 범위가 잘못되면 [java.time.DateTimeException]이 발생할 수 있습니다.
 *
 * ```kotlin
 * val time = protoTime.toLocalTime()
 * // time.minute == protoTime.minutes
 * ```
 */
fun ProtoTime.toLocalTime(): LocalTime =
    LocalTime.of(hours, minutes, seconds, nanos)

/**
 * [LocalTime]을 [ProtoTime]으로 변환합니다.
 *
 * ## 동작/계약
 * - 시간 필드를 그대로 protobuf builder에 설정합니다.
 * - 입력 객체는 변경하지 않고 새 protobuf 인스턴스를 반환합니다.
 *
 * ```kotlin
 * val proto = LocalTime.of(10, 11, 12).toProtoTime()
 * // proto.hours == 10
 * ```
 */
fun LocalTime.toProtoTime(): ProtoTime =
    ProtoTime.newBuilder()
        .setHours(hour)
        .setMinutes(minute)
        .setSeconds(second)
        .setNanos(nano)
        .build()
