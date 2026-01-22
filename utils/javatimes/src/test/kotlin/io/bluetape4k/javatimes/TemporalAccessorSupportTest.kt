package io.bluetape4k.javatimes

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.DateTimeException
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

        // LocalDateTime 에서 직접적으로 Instant 를 구할 수 없습니다.
        assertFailsWith<DateTimeException> {
            now.instant shouldBeEqualTo now.toInstant()
        }

        now.localDate shouldBeEqualTo now.toLocalDate()
        now.localTime shouldBeEqualTo now.toLocalTime()
        now.localDateTime shouldBeEqualTo now

        assertFailsWith<DateTimeException> {
            now.zoneOffset shouldBeEqualTo ZoneOffset.systemDefault()
        }
        assertFailsWith<DateTimeException> {
            now.offsetTime shouldBeEqualTo now.toOffsetDateTime().toOffsetTime()
        }
        assertFailsWith<DateTimeException> {
            now.offsetDateTime shouldBeEqualTo now.toOffsetDateTime()
        }

        assertFailsWith<DateTimeException> {
            now.zonedDateTime shouldBeEqualTo now.toZonedDateTime()
        }

        val nowOffset = nowOffsetDateTime()
        nowOffset.zoneOffset shouldBeEqualTo nowZonedDateTime().offset
        nowOffset.offsetTime shouldBeEqualTo nowOffset.toOffsetTime()
        nowOffset.offsetDateTime shouldBeEqualTo nowOffset

        val nowZoned = nowZonedDateTime()
        nowZoned.zone shouldBeEqualTo SystemZoneId
        nowZoned.zoneId shouldBeEqualTo SystemZoneId
        nowZoned.zonedDateTime shouldBeEqualTo nowZoned
    }
}
