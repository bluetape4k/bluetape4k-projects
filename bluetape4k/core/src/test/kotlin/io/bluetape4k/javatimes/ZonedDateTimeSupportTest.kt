package io.bluetape4k.javatimes

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * [ZonedDateTimeSupport.kt]에 대한 테스트
 */
class ZonedDateTimeSupportTest {

    companion object: KLogging()

    @Test
    fun `zonedDateTimeOf로 ZonedDateTime 생성 - 전체 파라미터`() {
        val zdt = zonedDateTimeOf(
            year = 2021,
            monthOfYear = 6,
            dayOfMonth = 15,
            hourOfDay = 14,
            minuteOfHour = 30,
            secondOfMinute = 45,
            nanoOfSecond = 123_456_789,
            zoneId = ZoneId.of("Asia/Seoul")
        )

        zdt.shouldNotBeNull()
        zdt.year shouldBeEqualTo 2021
        zdt.monthValue shouldBeEqualTo 6
        zdt.dayOfMonth shouldBeEqualTo 15
        zdt.hour shouldBeEqualTo 14
        zdt.minute shouldBeEqualTo 30
        zdt.second shouldBeEqualTo 45
        zdt.nano shouldBeEqualTo 123_456_789
        zdt.zone shouldBeEqualTo ZoneId.of("Asia/Seoul")
    }

    @Test
    fun `zonedDateTimeOf로 ZonedDateTime 생성 - 기본값 사용`() {
        val zdt = zonedDateTimeOf(year = 2021)

        zdt.shouldNotBeNull()
        zdt.year shouldBeEqualTo 2021
        zdt.monthValue shouldBeEqualTo 1
        zdt.dayOfMonth shouldBeEqualTo 1
        zdt.hour shouldBeEqualTo 0
        zdt.minute shouldBeEqualTo 0
        zdt.second shouldBeEqualTo 0
        zdt.nano shouldBeEqualTo 0
        zdt.zone shouldBeEqualTo ZoneOffset.UTC
    }

    @Test
    fun `zonedDateTimeOf로 ZonedDateTime 생성 - LocalDate와 LocalTime 사용`() {
        val localDate = LocalDate.of(2021, 3, 15)
        val localTime = LocalTime.of(12, 30, 45)
        val zoneId = ZoneId.of("Europe/London")

        val zdt = zonedDateTimeOf(localDate, localTime, zoneId)

        zdt.shouldNotBeNull()
        zdt.toLocalDate() shouldBeEqualTo localDate
        zdt.toLocalTime() shouldBeEqualTo localTime
        zdt.zone shouldBeEqualTo zoneId
    }

    @Test
    fun `week-based 속성 확인`() {
        zonedDateTimeOf(2021, 1, 1).weekyear shouldBeEqualTo 2020

        val zdtMonday = zonedDateTimeOf(2021, 1, 4)
        zdtMonday.weekOfWeekyear shouldBeGreaterThan 0
        log.debug { "weekOfWeekyear: ${zdtMonday.weekOfWeekyear}" }

        val zdtMarch = zonedDateTimeOf(2021, 3, 15)
        zdtMarch.weekOfMonth shouldBeGreaterThan 0
        log.debug { "weekOfMonth: ${zdtMarch.weekOfMonth}" }
    }

    @Test
    fun `day 시간 속성 확인`() {
        zonedDateTimeOf(2021, 1, 1, 12, 30, 45).secondsOfDay shouldBeEqualTo (12 * 3600 + 30 * 60 + 45)
        zonedDateTimeOf(2021, 1, 1, 12, 30, 45, nanoOfSecond = 123_000_000).millisOfDay shouldBeEqualTo (12 * 3600 + 30 * 60 + 45) * 1000 + 123
        zonedDateTimeOf(2021, 1, 1, 1, 0, 0).nanoOfDay shouldBeEqualTo 3600_000_000_000L
    }

    @Test
    fun `toUtcInstant 변환 확인`() {
        val zdt = zonedDateTimeOf(2021, 1, 1, 0, 0, 0, zoneId = ZoneId.of("Asia/Seoul"))
        val instant = zdt.toUtcInstant()
        instant.shouldNotBeNull()
        instant.epochSecond shouldBeEqualTo zdt.toEpochSecond()

        val zdtNano = zonedDateTimeOf(2021, 1, 1, 0, 0, 0, nanoOfSecond = 123_456_789, zoneId = ZoneId.of("Asia/Seoul"))
        zdtNano.toUtcInstant().nano shouldBeEqualTo 123_456_789
    }

    @Test
    fun `endOfYear 확인`() {
        val zdt = zonedDateTimeOf(2021, 6, 15)
        val end = zdt.endOfYear()

        end.year shouldBeEqualTo 2021
        end.monthValue shouldBeEqualTo 12
        end.dayOfMonth shouldBeEqualTo 31
        end.hour shouldBeEqualTo 23
        end.minute shouldBeEqualTo 59
        end.second shouldBeEqualTo 59
        end.nano shouldBeEqualTo 999_999_999
    }

    @Test
    fun `startOfQuarter와 endOfQuarter 확인`() {
        val zdt = zonedDateTimeOf(2021, 5, 15) // Q2

        val start = zdt.startOfQuarter()
        start.monthValue shouldBeEqualTo 4
        start.dayOfMonth shouldBeEqualTo 1
        start.hour shouldBeEqualTo 0

        val end = zdt.endOfQuarter()
        end.monthValue shouldBeEqualTo 6
        end.dayOfMonth shouldBeEqualTo 30
        end.hour shouldBeEqualTo 23
        end.minute shouldBeEqualTo 59
    }

    @Test
    fun `startOfMonth와 endOfMonth 확인`() {
        val zone = ZoneId.of("Asia/Seoul")
        val zdt = zonedDateTimeOf(2021, 3, 15, zoneId = zone)

        val start = zdt.startOfMonth()
        start.dayOfMonth shouldBeEqualTo 1
        start.hour shouldBeEqualTo 0
        start.zone shouldBeEqualTo zone

        val end = zdt.endOfMonth()
        end.dayOfMonth shouldBeEqualTo 31
        end.hour shouldBeEqualTo 23
        end.minute shouldBeEqualTo 59
        end.zone shouldBeEqualTo zone
    }

    @Test
    fun `startOfWeek와 endOfWeek 확인`() {
        val zone = ZoneId.of("Asia/Seoul")
        val zdt = zonedDateTimeOf(2021, 3, 17, zoneId = zone) // Wednesday

        val start = zdt.startOfWeek()
        start.dayOfWeek shouldBeEqualTo DayOfWeek.MONDAY
        start.zone shouldBeEqualTo zone

        val end = zdt.endOfWeek()
        end.dayOfWeek shouldBeEqualTo DayOfWeek.SUNDAY
        end.zone shouldBeEqualTo zone
    }

    @Test
    fun `startOfYear와 endOfYear는 기존 zone을 보존한다`() {
        val zone = ZoneId.of("Asia/Seoul")
        val zdt = zonedDateTimeOf(2021, 6, 15, 12, 34, 56, zoneId = zone)

        zdt.startOfYear().zone shouldBeEqualTo zone
        zdt.endOfYear().zone shouldBeEqualTo zone
    }

    @Test
    fun `startOfDay와 endOfDay 확인`() {
        val zdt = zonedDateTimeOf(2021, 3, 15, 14, 30, 45)

        val start = zdt.startOfDay()
        start.hour shouldBeEqualTo 0
        start.minute shouldBeEqualTo 0
        start.second shouldBeEqualTo 0
        start.nano shouldBeEqualTo 0

        val end = zdt.endOfDay()
        end.hour shouldBeEqualTo 23
        end.minute shouldBeEqualTo 59
        end.second shouldBeEqualTo 59
        end.nano shouldBeEqualTo 999_999_999
    }

    @Test
    fun `sub-period boundaries - hour, minute, second, millis`() {
        val zdt = zonedDateTimeOf(2021, 3, 15, 14, 30, 45, nanoOfSecond = 123_456_789)

        zdt.startOfHour().run { minute shouldBeEqualTo 0; second shouldBeEqualTo 0; nano shouldBeEqualTo 0 }
        zdt.endOfHour().run { minute shouldBeEqualTo 59; second shouldBeEqualTo 59; nano shouldBeEqualTo 999_999_999 }

        zdt.startOfMinute().run { second shouldBeEqualTo 0; nano shouldBeEqualTo 0 }
        zdt.endOfMinute().run { second shouldBeEqualTo 59; nano shouldBeEqualTo 999_999_999 }

        zdt.startOfSecond().nano shouldBeEqualTo 0
        zdt.endOfSeconds().nano shouldBeEqualTo 999_999_999

        zdt.startOfMillis().nano shouldBeEqualTo 123_000_000
        zdt.endOfMillis().nano shouldBeEqualTo 123_999_999
    }

    @Test
    fun `startOfYear 함수로 ZonedDateTime 생성`() {
        val zdt = startOfYear(2021)

        zdt.year shouldBeEqualTo 2021
        zdt.monthValue shouldBeEqualTo 1
        zdt.dayOfMonth shouldBeEqualTo 1
        zdt.hour shouldBeEqualTo 0
    }

    @Test
    fun `endOfYear 함수로 ZonedDateTime 생성`() {
        val zdt = endOfYear(2021)

        zdt.year shouldBeEqualTo 2021
        zdt.monthValue shouldBeEqualTo 12
        zdt.dayOfMonth shouldBeEqualTo 31
        zdt.hour shouldBeEqualTo 23
        zdt.minute shouldBeEqualTo 59
    }

    @Test
    fun `startOfQuarter와 endOfQuarter 함수 확인`() {
        val start = startOfQuarter(2021, 5) // Q2
        start.monthValue shouldBeEqualTo 4

        val end = endOfQuarter(2021, 5)
        end.monthValue shouldBeEqualTo 6

        val startQ3 = startOfQuarter(2021, Quarter.Q3)
        startQ3.monthValue shouldBeEqualTo 7

        val endQ3 = endOfQuarter(2021, Quarter.Q3)
        endQ3.monthValue shouldBeEqualTo 9
    }

    @Test
    fun `startOfMonth와 endOfMonth 함수 확인`() {
        val start = startOfMonth(2021, 3)
        start.dayOfMonth shouldBeEqualTo 1

        val end = endOfMonth(2021, 3)
        end.dayOfMonth shouldBeEqualTo 31
    }

    @Test
    fun `lengthOfMonth 함수 확인`() {
        lengthOfMonth(2021, 2) shouldBeEqualTo 28
        lengthOfMonth(2020, 2) shouldBeEqualTo 29 // 윤년
        lengthOfMonth(2021, 1) shouldBeEqualTo 31
        lengthOfMonth(2021, 4) shouldBeEqualTo 30
    }

    @Test
    fun `startOfWeek와 endOfWeek 함수 확인`() {
        val start = startOfWeek(2021, 3, 17) // Wednesday
        start.dayOfWeek shouldBeEqualTo DayOfWeek.MONDAY

        val end = endOfWeek(2021, 3, 17)
        end.dayOfWeek shouldBeEqualTo DayOfWeek.SUNDAY
    }

    @Test
    fun `startOfWeekOfWeekyear와 endOfWeekOfWeekyear 확인`() {
        val start = startOfWeekOfWeekyear(2021, 1)
        start.dayOfWeek shouldBeEqualTo DayOfWeek.MONDAY
        start.hour shouldBeEqualTo 0
        start.minute shouldBeEqualTo 0
        start.second shouldBeEqualTo 0
        start.nano shouldBeEqualTo 0

        val end = endOfWeekOfWeekyear(2021, 1)

        // endOfWeekOfWeekyear는 시작 월요일 + 7일 - 1나노초
        // Duration.toHours()는 버림이므로 167시간이 됩니다.
        val hours = java.time.Duration.between(start, end).toHours()
        hours shouldBeEqualTo 167L

        // start와 end 사이 날짜 차이 확인
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate())
        daysDiff shouldBeEqualTo 6L // Monday to Sunday (date only)
    }

    @Test
    fun `nextDayOfWeek와 prevDayOfWeek 확인`() {
        val zdt = zonedDateTimeOf(2021, 3, 15) // Monday

        val next = zdt.nextDayOfWeek()
        next.dayOfWeek shouldBeEqualTo zdt.dayOfWeek
        java.time.Duration.between(zdt, next).toDays() shouldBeEqualTo 7L

        val prev = zdt.prevDayOfWeek()
        prev.dayOfWeek shouldBeEqualTo zdt.dayOfWeek
        java.time.Duration.between(prev, zdt).toDays() shouldBeEqualTo 7L
    }

    @Test
    fun `ZonedDateTime min max 확장 함수`() {
        val zdt1 = zonedDateTimeOf(2021, 1, 1)
        val zdt2 = zonedDateTimeOf(2021, 12, 31)

        (zdt1 min zdt2) shouldBeEqualTo zdt1
        (zdt2 min zdt1) shouldBeEqualTo zdt1
        (null min zdt1) shouldBeEqualTo zdt1
        (zdt1 min null) shouldBeEqualTo zdt1

        (zdt1 max zdt2) shouldBeEqualTo zdt2
        (zdt2 max zdt1) shouldBeEqualTo zdt2
        (null max zdt1) shouldBeEqualTo zdt1
        (zdt1 max null) shouldBeEqualTo zdt1
    }

    @Test
    fun `equalToSeconds 확장 함수`() {
        val zdt1 = zonedDateTimeOf(2021, 1, 1, 12, 30, 45, nanoOfSecond = 123_456_789)
        val zdt2 = zonedDateTimeOf(2021, 1, 1, 12, 30, 45, nanoOfSecond = 987_654_321)

        zdt1.equalToSeconds(zdt2).shouldBeTrue()

        val zdt3 = zonedDateTimeOf(2021, 1, 1, 12, 30, 46)
        zdt1.equalToSeconds(zdt3).shouldBeFalse()

        zdt1.equalToSeconds(null).shouldBeFalse()
        (null as ZonedDateTime?).equalToSeconds(zdt2).shouldBeFalse()
    }

    @Test
    fun `equalToMillis 확장 함수`() {
        val zdt1 = zonedDateTimeOf(2021, 1, 1, 12, 30, 45, nanoOfSecond = 123_456_789)
        val zdt2 = zonedDateTimeOf(2021, 1, 1, 12, 30, 45, nanoOfSecond = 123_999_999)

        zdt1.equalToMillis(zdt2).shouldBeTrue()

        val zdt3 = zonedDateTimeOf(2021, 1, 1, 12, 30, 45, nanoOfSecond = 124_000_000)
        zdt1.equalToMillis(zdt3).shouldBeFalse()

        zdt1.equalToMillis(null).shouldBeFalse()
        (null as ZonedDateTime?).equalToMillis(zdt2).shouldBeFalse()
    }

    @Test
    fun `equalTo with OffsetDateTime 확인`() {
        val zdt = zonedDateTimeOf(2021, 1, 1, 0, 0, 0, zoneId = ZoneId.of("UTC"))
        val odt = offsetDateTimeOf(2021, 1, 1, 0, 0, 0, offset = java.time.ZoneOffset.UTC)

        zdt.equalTo(odt).shouldBeTrue()
    }
}
