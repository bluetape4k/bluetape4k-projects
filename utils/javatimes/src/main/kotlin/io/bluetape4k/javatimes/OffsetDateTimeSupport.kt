package io.bluetape4k.javatimes

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneOffset

/**
 * [OffsetDateTime]을 생성합니다.
 *
 * ```
 * val offsetDateTime = offsetDateTimeOf(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
 * ```
 */
fun offsetDateTimeOf(
    year: Int,
    monthOfYear: Int = 1,
    dayOfMonth: Int = 1,
    hourOfDay: Int = 0,
    minuteOfHour: Int = 0,
    secondOfMinute: Int = 0,
    milliOfSecond: Int = 0,
    offset: ZoneOffset = SystemOffset,
): OffsetDateTime =
    OffsetDateTime.of(
        year,
        monthOfYear,
        dayOfMonth,
        hourOfDay,
        minuteOfHour,
        secondOfMinute,
        milliOfSecond.millisToNanos(),
        offset
    )

/**
 * [OffsetDateTime]을 생성합니다.
 *
 * ```
 * val localDate = LocalDate.of(2021, 1, 1)
 * val localTime = LocalTime.of(0, 0, 0, 0)
 * val offset = ZoneOffset.UTC
 * val offsetDateTime = offsetDateTimeOf(localDate, localTime, offset)
 * ```
 *
 * @param localDate 날짜 정보
 * @param localTime 시간 정보
 * @param offset 시간대 정보
 */
fun offsetDateTimeOf(
    localDate: LocalDate = LocalDate.ofEpochDay(0),
    localTime: LocalTime = LocalTime.ofSecondOfDay(0),
    offset: ZoneOffset = SystemOffset,
): OffsetDateTime =
    OffsetDateTime.of(localDate, localTime, offset)

/**
 * [OffsetTime]을 생성합니다.
 *
 * ```
 * val offsetTime = offsetTimeOf(12, 59, 59, 100000, ZoneOffset.UTC)
 * ```
 */
fun offsetTimeOf(
    hourOfDay: Int,
    minuteOfHour: Int = 0,
    secondOfMinute: Int = 0,
    nanoOfSeconds: Int = 0,
    offset: ZoneOffset = ZoneOffset.UTC,
): OffsetTime =
    OffsetTime.of(hourOfDay, minuteOfHour, secondOfMinute, nanoOfSeconds, offset)

/**
 * [OffsetTime]을 [Instant]로 변환합니다.
 *
 * ```
 * val offsetTime = offsetTimeOf(12, 59, 59, 100000, ZoneOffset.UTC)
 * val instant = offsetTime.toInstant()
 * ```
 */
fun OffsetTime.toInstant(): Instant = Instant.from(this)
