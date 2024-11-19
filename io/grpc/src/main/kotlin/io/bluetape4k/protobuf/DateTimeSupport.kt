package io.bluetape4k.protobuf

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * protobuf의 [com.google.type.Date] 수형을 [java.time.LocalDate] 수형으로 변환합니다.
 */
fun ProtoDate.toLocalDate(): LocalDate = LocalDate.of(year, month, day)

/**
 * [java.time.LocalDate]를 protobuf의 [com.google.type.Date] 수형으로 변환합니다.
 */
fun LocalDate.toProtoDate(): ProtoDate =
    ProtoDate.newBuilder()
        .setYear(year)
        .setMonth(monthValue)
        .setDay(dayOfMonth)
        .build()


/**
 * [com.google.type.DateTime] 수형을 [java.time.LocalDateTime] 수형으로 변환합니다.
 */
fun ProtoDateTime.toLocalDateTime(): LocalDateTime =
    LocalDateTime.of(year, month, day, hours, minutes, seconds, nanos)

/**
 * [java.time.LocalDateTime] 수형을 [com.google.type.DateTime] 수형으로 변환합니다.
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
 * [com.google.type.Time] 수형을 [java.time.LocalTime] 수형으로 변환합니다.
 */
fun ProtoTime.toLocalTime(): LocalTime =
    LocalTime.of(hours, minutes, seconds, nanos)

/**
 * [java.time.LocalTime] 수형을 [com.google.type.Time] 수형으로 변환합니다.
 */
fun LocalTime.toProtoTime(): ProtoTime =
    ProtoTime.newBuilder()
        .setHours(hour)
        .setMinutes(minute)
        .setSeconds(second)
        .setNanos(nano)
        .build()
