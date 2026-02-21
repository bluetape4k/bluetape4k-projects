package io.bluetape4k.javatimes

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Month
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.time.temporal.UnsupportedTemporalTypeException
import kotlin.test.assertFailsWith

class TemporalAccessorSupportTest {

    companion object: KLogging()

    @Test
    fun `format string for Instant`() {
        val instant = localDateTimeOf(
            2020,
            10,
            14,
            6,
            55,
            44,
            123
        ).toInstant(ZoneOffset.UTC)

        instant.toString() shouldBeEqualTo "2020-10-14T06:55:44.123Z"
        instant.toIsoInstantString() shouldBeEqualTo "2020-10-14T06:55:44.123Z"
        instant.toIsoString() shouldBeEqualTo "2020-10-14T06:55:44.123Z"
    }

    @Test
    fun `format string for LocalDateTime`() {
        val localDateTime = localDateTimeOf(
            2020,
            10,
            14,
            6,
            55,
            44,
            123
        )
        with(localDateTime) {
            toIsoInstantString() shouldBeEqualTo "2020-10-14T06:55:44.123Z"
            toIsoString() shouldBeEqualTo "2020-10-14T06:55:44.123"
            toIsoDateString() shouldBeEqualTo "2020-10-14"
            toIsoTimeString() shouldBeEqualTo "06:55:44.123"

            toIsoLocalString() shouldBeEqualTo "2020-10-14T06:55:44.123"
            toIsoLocalDateString() shouldBeEqualTo "2020-10-14"
            toIsoLocalTimeString() shouldBeEqualTo "06:55:44.123"

            assertFailsWith<UnsupportedTemporalTypeException> {
                toIsoOffsetDateTimeString() shouldBeEqualTo "2020-10-14T06:55:44.123+09:00"
            }
            assertFailsWith<UnsupportedTemporalTypeException> {
                toIsoOffsetDateString() shouldBeEqualTo "2020-10-14"
            }
            assertFailsWith<UnsupportedTemporalTypeException> {
                toIsoOffsetTimeString() shouldBeEqualTo "06:55:44.123"
            }
            assertFailsWith<UnsupportedTemporalTypeException> {
                toIsoZonedDateTimeString() shouldBeEqualTo "2020-10-14T06:55:44.000000123+09:00[Asia/Seoul]"
            }
        }
    }

    @Test
    fun `format string for OffsetDateTime`() {
        val offsetDateTime = offsetDateTimeOf(
            2020,
            10,
            14,
            6,
            55,
            44,
            123,
            ZoneOffset.ofHours(9)
        )
        with(offsetDateTime) {
            toIsoInstantString() shouldBeEqualTo "2020-10-13T21:55:44.123Z"
            toIsoString() shouldBeEqualTo "2020-10-14T06:55:44.123+09:00"
            toIsoDateString() shouldBeEqualTo "2020-10-14+09:00"
            toIsoTimeString() shouldBeEqualTo "06:55:44.123+09:00"

            toIsoLocalString() shouldBeEqualTo "2020-10-14T06:55:44.123"
            toIsoLocalDateString() shouldBeEqualTo "2020-10-14"
            toIsoLocalTimeString() shouldBeEqualTo "06:55:44.123"

            toIsoOffsetDateTimeString() shouldBeEqualTo "2020-10-14T06:55:44.123+09:00"
            toIsoOffsetDateString() shouldBeEqualTo "2020-10-14+09:00"
            toIsoOffsetTimeString() shouldBeEqualTo "06:55:44.123+09:00"
            toIsoZonedDateTimeString() shouldBeEqualTo "2020-10-14T06:55:44.123+09:00"
        }
    }

    @Test
    fun `format string for ZonedDateTime`() {
        val zonedDateTime = zonedDateTimeOf(
            2020,
            10,
            14,
            6,
            55,
            44,
            123,
            ZoneId.of("Asia/Seoul")
        )
        with(zonedDateTime) {
            toIsoInstantString() shouldBeEqualTo "2020-10-13T21:55:44.000Z"
            toIsoString() shouldBeEqualTo "2020-10-14T06:55:44.000000123+09:00[Asia/Seoul]"
            toIsoDateString() shouldBeEqualTo "2020-10-14+09:00"
            toIsoTimeString() shouldBeEqualTo "06:55:44.000000123+09:00"

            toIsoLocalString() shouldBeEqualTo "2020-10-14T06:55:44.000000123"
            toIsoLocalDateString() shouldBeEqualTo "2020-10-14"
            toIsoLocalTimeString() shouldBeEqualTo "06:55:44.000000123"

            toIsoOffsetDateTimeString() shouldBeEqualTo "2020-10-14T06:55:44.000000123+09:00"
            toIsoOffsetDateString() shouldBeEqualTo "2020-10-14+09:00"
            toIsoOffsetTimeString() shouldBeEqualTo "06:55:44.000000123+09:00"
            toIsoZonedDateTimeString() shouldBeEqualTo "2020-10-14T06:55:44.000000123+09:00[Asia/Seoul]"
        }
    }

    @Test
    fun `TemporalAccessor query operator`() {
        val now = localDateTimeOf(
            2020,
            10,
            14,
            6,
            55,
            44
        )

        now.precision shouldBeEqualTo ChronoUnit.NANOS

        now.year shouldBeEqualTo 2020
        now.yearMonth shouldBeEqualTo yearMonthOf(2020, 10)

        now.month shouldBeEqualTo Month.OCTOBER
        now.monthDay shouldBeEqualTo monthDayOf(10, 14)

        now.dayOfWeek shouldBeEqualTo DayOfWeek.WEDNESDAY

        // LocalDateTime 에서는 직접적인 timezone 정보가 없으므로 null을 반환합니다.
        now.instant.shouldBeNull()

        now.localDate shouldBeEqualTo now.toLocalDate()
        now.localTime shouldBeEqualTo now.toLocalTime()
        now.localDateTime shouldBeEqualTo now

        now.zoneOffset.shouldBeNull()
        now.offsetTime.shouldBeNull()
        now.offsetDateTime.shouldBeNull()
        now.zonedDateTime.shouldBeNull()

        val nowOffset = nowOffsetDateTime()
        nowOffset.zoneOffset shouldBeEqualTo nowZonedDateTime().offset
        nowOffset.offsetTime shouldBeEqualTo nowOffset.toOffsetTime()
        nowOffset.offsetDateTime shouldBeEqualTo nowOffset

        val nowZoned = nowZonedDateTime()
        nowZoned.zone shouldBeEqualTo SystemZoneId
        nowZoned.zoneId shouldBeEqualTo SystemZoneId
        nowZoned.zonedDateTime shouldBeEqualTo nowZoned
    }

    @Test
    fun `OrNull 포맷 함수는 지원 불가 필드에서 null을 반환한다`() {
        val localDateTime = localDateTimeOf(2020, 10, 14, 6, 55, 44, 123)

        localDateTime.toIsoStringOrNull() shouldBeEqualTo "2020-10-14T06:55:44.123"
        localDateTime.toIsoDateStringOrNull() shouldBeEqualTo "2020-10-14"
        localDateTime.toIsoTimeStringOrNull() shouldBeEqualTo "06:55:44.123"
        localDateTime.toIsoLocalStringOrNull() shouldBeEqualTo "2020-10-14T06:55:44.123"
        localDateTime.toIsoLocalDateStringOrNull() shouldBeEqualTo "2020-10-14"
        localDateTime.toIsoLocalTimeStringOrNull() shouldBeEqualTo "06:55:44.123"

        localDateTime.toIsoOffsetDateTimeStringOrNull().shouldBeNull()
        localDateTime.toIsoOffsetDateStringOrNull().shouldBeNull()
        localDateTime.toIsoOffsetTimeStringOrNull().shouldBeNull()
        localDateTime.toIsoZonedDateTimeStringOrNull().shouldBeNull()
        localDateTime.toIsoInstantStringOrNull().shouldNotBeNull()
    }
}
