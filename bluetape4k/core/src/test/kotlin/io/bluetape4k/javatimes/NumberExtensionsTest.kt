package io.bluetape4k.javatimes

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.Month
import java.time.Period
import java.time.Year
import java.time.ZoneOffset

/**
 * [NumberExtensions.kt]에 대한 테스트
 */
class NumberExtensionsTest {

    companion object: KLogging()

    @Test
    fun `UTC now 함수들은 UTC 기준으로 동작한다`() {
        nowOffsetDateTimeUtc().offset shouldBeEqualTo ZoneOffset.UTC
        nowOffsetTimeUtc().offset shouldBeEqualTo ZoneOffset.UTC
        nowZonedDateTimeUtc().zone shouldBeEqualTo ZoneOffset.UTC

        nowLocalDateUtc() shouldBeEqualTo nowLocalDate(ZoneOffset.UTC)
        nowLocalDateTimeUtc().toLocalDate() shouldBeEqualTo nowLocalDate(ZoneOffset.UTC)

        val diff = Duration.between(nowInstant(ZoneOffset.UTC), nowInstantUtc()).abs()
        (diff < Duration.ofSeconds(1)).shouldBeTrue()
    }

    @Test
    fun `UTC today 함수들은 UTC 자정 기준으로 동작한다`() {
        todayInstantUtc() shouldBeEqualTo todayInstant(ZoneOffset.UTC)
        todayLocalDateUtc() shouldBeEqualTo todayLocalDate(ZoneOffset.UTC)
        todayLocalDateTimeUtc() shouldBeEqualTo todayLocalDateTime(ZoneOffset.UTC)
        todayOffsetDateTimeUtc() shouldBeEqualTo todayOffsetDateTime(ZoneOffset.UTC)
        todayZonedDateTimeUtc() shouldBeEqualTo todayZonedDateTime(ZoneOffset.UTC)

        todayLocalDateTimeUtc().toLocalTime() shouldBeEqualTo java.time.LocalTime.MIDNIGHT
        todayOffsetDateTimeUtc().toLocalTime() shouldBeEqualTo java.time.LocalTime.MIDNIGHT
        todayZonedDateTimeUtc().toLocalTime() shouldBeEqualTo java.time.LocalTime.MIDNIGHT
    }

    @Test
    fun `Int를 Duration으로 변환`() {
        1.nanos() shouldBeEqualTo Duration.ofNanos(1)
        10.micros() shouldBeEqualTo Duration.ofNanos(10_000)
        100.millis() shouldBeEqualTo Duration.ofMillis(100)
        5.seconds() shouldBeEqualTo Duration.ofSeconds(5)
        3.minutes() shouldBeEqualTo Duration.ofMinutes(3)
        2.hours() shouldBeEqualTo Duration.ofHours(2)
        1.days() shouldBeEqualTo Duration.ofDays(1)
        2.weeks() shouldBeEqualTo Duration.ofDays(14)
    }

    @Test
    fun `Long을 Duration으로 변환`() {
        1L.nanos() shouldBeEqualTo Duration.ofNanos(1)
        10L.micros() shouldBeEqualTo Duration.ofNanos(10_000)
        100L.millis() shouldBeEqualTo Duration.ofMillis(100)
        5L.seconds() shouldBeEqualTo Duration.ofSeconds(5)
        3L.minutes() shouldBeEqualTo Duration.ofMinutes(3)
        2L.hours() shouldBeEqualTo Duration.ofHours(2)
        1L.days() shouldBeEqualTo Duration.ofDays(1)
        2L.weeks() shouldBeEqualTo Duration.ofDays(14)
    }

    @Test
    fun `Int를 Period로 변환`() {
        1.dayPeriod() shouldBeEqualTo Period.ofDays(1)
        2.weekPeriod() shouldBeEqualTo Period.ofWeeks(2)
        3.monthPeriod() shouldBeEqualTo Period.ofMonths(3)
        1.quarterPeriod() shouldBeEqualTo Period.ofMonths(3)
        2.quarterPeriod() shouldBeEqualTo Period.ofMonths(6)
        1.yearPeriod() shouldBeEqualTo Period.ofYears(1)
    }

    @Test
    fun `Long을 Period로 변환`() {
        1L.dayPeriod() shouldBeEqualTo Period.ofDays(1)
        2L.weekPeriod() shouldBeEqualTo Period.ofWeeks(2)
        3L.monthPeriod() shouldBeEqualTo Period.ofMonths(3)
        1L.quarterPeriod() shouldBeEqualTo Period.ofMonths(3)
        2L.quarterPeriod() shouldBeEqualTo Period.ofMonths(6)
        1L.yearPeriod() shouldBeEqualTo Period.ofYears(1)
    }

    @Test
    fun `millisToNanos 변환`() {
        1.millisToNanos() shouldBeEqualTo 1_000_000
        10.millisToNanos() shouldBeEqualTo 10_000_000
        1L.millisToNanos() shouldBeEqualTo 1_000_000
        10L.millisToNanos() shouldBeEqualTo 10_000_000
    }

    @Test
    fun `윤년 여부 확인`() {
        2020.isLeapYear().shouldBeTrue()
        2024.isLeapYear().shouldBeTrue()
        2000.isLeapYear().shouldBeTrue()

        2019.isLeapYear().shouldBeFalse()
        2021.isLeapYear().shouldBeFalse()
        1900.isLeapYear().shouldBeFalse()

        2020L.isLeapYear().shouldBeTrue()
        2021L.isLeapYear().shouldBeFalse()
    }

    @Test
    fun `Int와 Duration 연산자`() {
        val duration = 5.seconds()

        // Int * Duration
        (2 * duration) shouldBeEqualTo 10.seconds()

        // Duration * Int
        (duration * 3) shouldBeEqualTo 15.seconds()

        // Duration div Int
        (duration / 5) shouldBeEqualTo 1.seconds()
    }

    @Test
    fun `Long과 Duration 연산자`() {
        val duration = 5.seconds()

        // Long * Duration
        (2L * duration) shouldBeEqualTo 10.seconds()

        // Duration * Long
        (duration * 3L) shouldBeEqualTo 15.seconds()

        // Duration div Long
        (duration / 5L) shouldBeEqualTo 1.seconds()
    }

    @Test
    fun `Int와 Period 연산자`() {
        val period = Period.of(1, 2, 3)

        // Int * Period
        (2 * period) shouldBeEqualTo Period.of(2, 4, 6)

        // Period * Int
        (period * 3) shouldBeEqualTo Period.of(3, 6, 9)

        // Period div Int
        (period / 1) shouldBeEqualTo Period.of(1, 2, 3)
        val divided = Period.of(6, 9, 12) / 3
        divided shouldBeEqualTo Period.of(2, 3, 4)
    }

    @Test
    fun `Long과 Period 연산자`() {
        val period = Period.of(1, 2, 3)

        // Long * Period
        (2L * period) shouldBeEqualTo Period.of(2, 4, 6)

        // Period * Long
        (period * 3L) shouldBeEqualTo Period.of(3, 6, 9)

        // Period div Long
        val divided = Period.of(6, 9, 12) / 3L
        divided shouldBeEqualTo Period.of(2, 3, 4)
    }

    @Test
    fun `Duration 구조 분해`() {
        val duration = Duration.ofSeconds(123, 456_789_012)
        val (seconds, nanos) = duration

        seconds shouldBeEqualTo 123L
        nanos shouldBeEqualTo 456_789_012
    }

    @Test
    fun `Period 구조 분해`() {
        val period = Period.of(1, 2, 3)
        val (years, months, days) = period

        years shouldBeEqualTo 1
        months shouldBeEqualTo 2
        days shouldBeEqualTo 3
    }

    @Test
    fun `Year 연산자`() {
        val year2020 = Year.of(2020)

        // Year + Month
        (year2020 + Month.JANUARY).year shouldBeEqualTo 2020
        (year2020 + Month.JANUARY).month shouldBeEqualTo Month.JANUARY

        // Year + MonthDay
        val monthDay = monthDayOf(3, 15)
        (year2020 + monthDay).year shouldBeEqualTo 2020
        (year2020 + monthDay).monthValue shouldBeEqualTo 3
        (year2020 + monthDay).dayOfMonth shouldBeEqualTo 15

        // Year + Int
        (year2020 + 1).value shouldBeEqualTo 2021

        // Year - Int
        (year2020 - 1).value shouldBeEqualTo 2019

        // Year.inc()
        var year = year2020
        (++year).value shouldBeEqualTo 2021

        // Year.dec()
        (--year).value shouldBeEqualTo 2020
    }

    @Test
    fun `Month 연산자`() {
        // Month + Int
        (Month.JANUARY + 1) shouldBeEqualTo Month.FEBRUARY
        (Month.DECEMBER + 1) shouldBeEqualTo Month.JANUARY

        // Month - Int
        (Month.FEBRUARY - 1) shouldBeEqualTo Month.JANUARY
        (Month.JANUARY - 1) shouldBeEqualTo Month.DECEMBER

        // Month.inc()
        var month = Month.JANUARY
        (++month) shouldBeEqualTo Month.FEBRUARY

        // Month.dec()
        (--month) shouldBeEqualTo Month.JANUARY
    }

    @Test
    fun `YearMonth 연산자`() {
        val ym = yearMonthOf(2020, Month.MARCH)

        // YearMonth + Year
        (ym + Year.of(2)).year shouldBeEqualTo 2022

        // YearMonth + Month
        (ym + Month.APRIL).monthValue shouldBeEqualTo 7
    }

    @Test
    fun `DayOfWeek 연산자`() {
        // DayOfWeek.inc()
        var day = DayOfWeek.MONDAY
        (++day) shouldBeEqualTo DayOfWeek.TUESDAY

        // DayOfWeek.dec()
        (--day) shouldBeEqualTo DayOfWeek.MONDAY
    }

    @Test
    fun `yearMonthOf 생성`() {
        yearMonthOf(2020, Month.JANUARY).year shouldBeEqualTo 2020
        yearMonthOf(2020, Month.JANUARY).month shouldBeEqualTo Month.JANUARY

        yearMonthOf(2020, 3).year shouldBeEqualTo 2020
        yearMonthOf(2020, 3).monthValue shouldBeEqualTo 3
    }

    @Test
    fun `monthDayOf 생성`() {
        monthDayOf(3, 15).monthValue shouldBeEqualTo 3
        monthDayOf(3, 15).dayOfMonth shouldBeEqualTo 15

        monthDayOf(Month.MARCH, 15).month shouldBeEqualTo Month.MARCH
        monthDayOf(Month.MARCH, 15).dayOfMonth shouldBeEqualTo 15
    }

    @Test
    fun `Int와 Long을 Instant로 변환`() {
        val epochMillis = 1609459200000L // 2021-01-01T00:00:00Z

        // Int는 범위 제한이 있으므로 작은 값 사용
        val smallMillis = 123456789L
        smallMillis.toInt().toInstant().toEpochMilli() shouldBeEqualTo smallMillis
        epochMillis.toInstant().toEpochMilli() shouldBeEqualTo epochMillis
    }
}
