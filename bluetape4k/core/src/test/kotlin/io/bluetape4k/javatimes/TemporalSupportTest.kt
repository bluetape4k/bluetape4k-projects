package io.bluetape4k.javatimes

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TemporalSupportTest {

    companion object: KLogging()

    @Test
    fun `get startOfYear from Temporal`() {
        nowInstant().startOfYear().toZonedDateTime().dayOfYear shouldBeEqualTo 1
        nowLocalDate().startOfYear().dayOfYear shouldBeEqualTo 1
        nowLocalDateTime().startOfYear().dayOfYear shouldBeEqualTo 1
        nowOffsetDateTime().startOfYear().dayOfYear shouldBeEqualTo 1
        nowZonedDateTime().startOfYear().dayOfYear shouldBeEqualTo 1
    }

    @Test
    fun `get startOfMonth from Temporal`() {
        nowInstant().startOfMonth().toZonedDateTime().dayOfMonth shouldBeEqualTo 1
        nowLocalDate().startOfMonth().dayOfMonth shouldBeEqualTo 1
        nowLocalDateTime().startOfMonth().dayOfMonth shouldBeEqualTo 1
        nowOffsetDateTime().startOfMonth().dayOfMonth shouldBeEqualTo 1
        nowZonedDateTime().startOfMonth().dayOfMonth shouldBeEqualTo 1
    }

    @Test
    fun `get startOfDay from Temporal`() {

        // NOTE : Time zone에 따른 시간을 고려해야 합니다
        nowInstant().startOfDay().toZonedDateTime(ZoneOffset.UTC).hour shouldBeEqualTo 0

        nowLocalDateTime().startOfDay().hour shouldBeEqualTo 0
        nowOffsetDateTime().startOfDay().hour shouldBeEqualTo 0
        nowZonedDateTime().startOfDay().hour shouldBeEqualTo 0
    }

    @Test
    fun `get startOfHour from Temporal`() {

        nowInstant().startOfHour().toZonedDateTime(ZoneOffset.UTC).minute shouldBeEqualTo 0

        nowLocalDateTime().startOfHour().minute shouldBeEqualTo 0
        nowOffsetDateTime().startOfHour().minute shouldBeEqualTo 0
        nowZonedDateTime().startOfHour().minute shouldBeEqualTo 0

        nowLocalTime().startOfHour().minute shouldBeEqualTo 0
        nowOffsetTime().startOfHour().minute shouldBeEqualTo 0
    }

    @Test
    fun `get startOfMinute from Temporal`() {

        nowInstant().startOfMinute().toZonedDateTime(ZoneOffset.UTC).second shouldBeEqualTo 0

        nowLocalDateTime().startOfMinute().second shouldBeEqualTo 0
        nowOffsetDateTime().startOfMinute().second shouldBeEqualTo 0
        nowZonedDateTime().startOfMinute().second shouldBeEqualTo 0

        nowLocalTime().startOfMinute().second shouldBeEqualTo 0
        nowOffsetTime().startOfMinute().second shouldBeEqualTo 0
    }

    @Test
    fun `get startOfSecond from Temporal`() {

        nowInstant().startOfSecond().toZonedDateTime(ZoneOffset.UTC).nano shouldBeEqualTo 0

        nowLocalDateTime().startOfSecond().nano shouldBeEqualTo 0
        nowOffsetDateTime().startOfSecond().nano shouldBeEqualTo 0
        nowZonedDateTime().startOfSecond().nano shouldBeEqualTo 0

        nowLocalTime().startOfSecond().nano shouldBeEqualTo 0
        nowOffsetTime().startOfSecond().nano shouldBeEqualTo 0
    }

    @Test
    fun `temporal adjuster operators`() {
        val now = localDateTimeOf(2020, 10, 14, 6, 55, 44)

        now.firstOfMonth shouldBeEqualTo localDateTimeOf(2020, 10, 1, 6, 55, 44)
        now.lastOfMonth shouldBeEqualTo localDateTimeOf(2020, 10, 31, 6, 55, 44)
        now.firstOfNextMonth shouldBeEqualTo localDateTimeOf(2020, 11, 1, 6, 55, 44)

        now.firstOfYear shouldBeEqualTo localDateTimeOf(2020, 1, 1, 6, 55, 44)
        now.lastOfYear shouldBeEqualTo localDateTimeOf(2020, 12, 31, 6, 55, 44)
        now.firstOfNextYear shouldBeEqualTo localDateTimeOf(2021, 1, 1, 6, 55, 44)
    }

    @Test
    fun `temporal adjuster by with operator`() {
        val now = localDateTimeOf(2020, 10, 14, 6, 55, 44)

        now.dayOfWeekInMonth(1, DayOfWeek.MONDAY) shouldBeEqualTo localDateTimeOf(2020, 10, 5, 6, 55, 44)
        now.dayOfWeekInMonth(2, DayOfWeek.MONDAY) shouldBeEqualTo localDateTimeOf(2020, 10, 12, 6, 55, 44)
        now.dayOfWeekInMonth(5, DayOfWeek.MONDAY) shouldBeEqualTo localDateTimeOf(2020, 11, 2, 6, 55, 44)

        now.firstInMonth(DayOfWeek.MONDAY) shouldBeEqualTo localDateTimeOf(2020, 10, 5, 6, 55, 44)
        now.firstInMonth(DayOfWeek.TUESDAY) shouldBeEqualTo localDateTimeOf(2020, 10, 6, 6, 55, 44)
        now.firstInMonth(DayOfWeek.SUNDAY) shouldBeEqualTo localDateTimeOf(2020, 10, 4, 6, 55, 44)

        now.lastInMonth(DayOfWeek.MONDAY) shouldBeEqualTo localDateTimeOf(2020, 10, 26, 6, 55, 44)
        now.lastInMonth(DayOfWeek.FRIDAY) shouldBeEqualTo localDateTimeOf(2020, 10, 30, 6, 55, 44)
        now.lastInMonth(DayOfWeek.SUNDAY) shouldBeEqualTo localDateTimeOf(2020, 10, 25, 6, 55, 44)

        now.previous(DayOfWeek.MONDAY) shouldBeEqualTo localDateTimeOf(2020, 10, 12, 6, 55, 44)
        now.previousOrSame(DayOfWeek.MONDAY) shouldBeEqualTo localDateTimeOf(2020, 10, 12, 6, 55, 44)
        now.previousOrSame(DayOfWeek.WEDNESDAY) shouldBeEqualTo localDateTimeOf(2020, 10, 14, 6, 55, 44)

        now.next(DayOfWeek.MONDAY) shouldBeEqualTo localDateTimeOf(2020, 10, 19, 6, 55, 44)
        now.nextOrSame(DayOfWeek.MONDAY) shouldBeEqualTo localDateTimeOf(2020, 10, 19, 6, 55, 44)
        now.nextOrSame(DayOfWeek.WEDNESDAY) shouldBeEqualTo localDateTimeOf(2020, 10, 14, 6, 55, 44)
    }

    @Test
    fun `Temporal supports operator`() {
        assertFalse { nowInstant() supports ChronoUnit.ERAS }
        assertFalse { nowInstant() supports ChronoUnit.CENTURIES }
        assertFalse { nowInstant() supports ChronoUnit.YEARS }
        assertFalse { nowInstant() supports ChronoUnit.MONTHS }

        assertTrue { nowInstant() supports ChronoUnit.DAYS }
        assertTrue { nowInstant() supports ChronoUnit.HALF_DAYS }
        assertTrue { nowInstant() supports ChronoUnit.HOURS }

        assertTrue { nowZonedDateTime() supports ChronoUnit.ERAS }
        assertTrue { nowZonedDateTime() supports ChronoUnit.CENTURIES }
        assertTrue { nowZonedDateTime() supports ChronoUnit.YEARS }
        assertTrue { nowZonedDateTime() supports ChronoUnit.DAYS }
        assertTrue { nowZonedDateTime() supports ChronoUnit.HALF_DAYS }
        assertTrue { nowZonedDateTime() supports ChronoUnit.HOURS }

        assertTrue { nowLocalDate() supports ChronoUnit.ERAS }
        assertTrue { nowLocalDate() supports ChronoUnit.CENTURIES }
        assertTrue { nowLocalDate() supports ChronoUnit.YEARS }
        assertTrue { nowLocalDate() supports ChronoUnit.DAYS }
        assertFalse { nowLocalDate() supports ChronoUnit.HALF_DAYS }
        assertFalse { nowLocalDate() supports ChronoUnit.HOURS }

        assertTrue { nowLocalDateTime() supports ChronoUnit.ERAS }
        assertTrue { nowLocalDateTime() supports ChronoUnit.CENTURIES }
        assertTrue { nowLocalDateTime() supports ChronoUnit.YEARS }
        assertTrue { nowLocalDateTime() supports ChronoUnit.DAYS }
        assertTrue { nowLocalDateTime() supports ChronoUnit.HALF_DAYS }
        assertTrue { nowLocalDateTime() supports ChronoUnit.HOURS }
    }

    @Test
    fun `startOfWeek for Instant 는 UTC 월요일 00시 Instant를 반환한다`() {
        val instant = Instant.parse("2025-11-05T10:15:30Z") // Wednesday
        val start = instant.startOfWeek()

        start.toZonedDateTime(ZoneOffset.UTC).dayOfWeek shouldBeEqualTo DayOfWeek.MONDAY
        start.toZonedDateTime(ZoneOffset.UTC).hour shouldBeEqualTo 0
        start.toZonedDateTime(ZoneOffset.UTC).minute shouldBeEqualTo 0
        start.toZonedDateTime(ZoneOffset.UTC).second shouldBeEqualTo 0
    }

    @Test
    fun `startOfYear와 startOfMonth 는 기존 zone offset을 보존한다`() {
        val offset = ZoneOffset.ofHours(9)
        val offsetDateTime = offsetDateTimeOf(2025, 8, 17, 11, 22, 33, 0, offset)
        val startOfYear = offsetDateTime.startOfYear()
        val startOfMonth = offsetDateTime.startOfMonth()

        startOfYear.offset shouldBeEqualTo offset
        startOfYear.monthValue shouldBeEqualTo 1
        startOfYear.dayOfMonth shouldBeEqualTo 1
        startOfYear.hour shouldBeEqualTo 0

        startOfMonth.offset shouldBeEqualTo offset
        startOfMonth.monthValue shouldBeEqualTo 8
        startOfMonth.dayOfMonth shouldBeEqualTo 1
        startOfMonth.hour shouldBeEqualTo 0

        val zone = ZoneId.of("Asia/Seoul")
        val zonedDateTime = zonedDateTimeOf(2025, 8, 17, 11, 22, 33, 0, zone)
        zonedDateTime.startOfYear().zone shouldBeEqualTo zone
        zonedDateTime.startOfMonth().zone shouldBeEqualTo zone
    }

    @Test
    fun `LocalDate toEpochMillis 는 zoneId 인자에 따라 계산된다`() {
        val date = LocalDate.of(1970, 1, 1)

        date.toEpochMillis(ZoneOffset.UTC) shouldBeEqualTo 0L
        date.toEpochMillis(ZoneOffset.ofHours(9)) shouldBeEqualTo -9L * 60L * 60L * 1000L
    }
}
