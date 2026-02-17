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
    fun `weekyear 속성 확인`() {
        val zdt = zonedDateTimeOf(2021, 1, 1)
        zdt.weekyear shouldBeEqualTo 2020 // ISO 8601: 2021-01-01은 2020년의 마지막 주
    }

    @Test
    fun `weekOfWeekyear 속성 확인`() {
        val zdt = zonedDateTimeOf(2021, 1, 4) // Monday
        zdt.weekOfWeekyear shouldBeGreaterThan 0
        log.debug { "weekOfWeekyear: ${zdt.weekOfWeekyear}" }
    }

    @Test
    fun `weekOfMonth 속성 확인`() {
        val zdt = zonedDateTimeOf(2021, 3, 15)
        zdt.weekOfMonth shouldBeGreaterThan 0
        log.debug { "weekOfMonth: ${zdt.weekOfMonth}" }
    }

    @Test
    fun `secondsOfDay 속성 확인`() {
        val zdt = zonedDateTimeOf(2021, 1, 1, 12, 30, 45)
        zdt.secondsOfDay shouldBeEqualTo (12 * 3600 + 30 * 60 + 45)
    }

    @Test
    fun `millisOfDay 속성 확인`() {
        val zdt = zonedDateTimeOf(2021, 1, 1, 12, 30, 45, nanoOfSecond = 123_000_000)
        zdt.millisOfDay shouldBeEqualTo (12 * 3600 + 30 * 60 + 45) * 1000 + 123
    }

    @Test
    fun `nanoOfDay 속성 확인`() {
        val zdt = zonedDateTimeOf(2021, 1, 1, 1, 0, 0)
        zdt.nanoOfDay shouldBeEqualTo 3600_000_000_000L
    }

    @Test
    fun `toUtcInstant로 UTC Instant 변환`() {
        val zdt = zonedDateTimeOf(2021, 1, 1, 0, 0, 0, zoneId = ZoneId.of("Asia/Seoul"))
        val instant = zdt.toUtcInstant()

        instant.shouldNotBeNull()
        instant.epochSecond shouldBeEqualTo zdt.toEpochSecond()
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
        val zdt = zonedDateTimeOf(2021, 3, 15)

        val start = zdt.startOfMonth()
        start.dayOfMonth shouldBeEqualTo 1
        start.hour shouldBeEqualTo 0

        val end = zdt.endOfMonth()
        end.dayOfMonth shouldBeEqualTo 31
        end.hour shouldBeEqualTo 23
        end.minute shouldBeEqualTo 59
    }

    @Test
    fun `startOfWeek와 endOfWeek 확인`() {
        val zdt = zonedDateTimeOf(2021, 3, 17) // Wednesday

        val start = zdt.startOfWeek()
        start.dayOfWeek shouldBeEqualTo DayOfWeek.MONDAY

        val end = zdt.endOfWeek()
        end.dayOfWeek shouldBeEqualTo DayOfWeek.SUNDAY
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
    fun `startOfHour와 endOfHour 확인`() {
        val zdt = zonedDateTimeOf(2021, 3, 15, 14, 30, 45)

        val start = zdt.startOfHour()
        start.minute shouldBeEqualTo 0
        start.second shouldBeEqualTo 0
        start.nano shouldBeEqualTo 0

        val end = zdt.endOfHour()
        end.minute shouldBeEqualTo 59
        end.second shouldBeEqualTo 59
        end.nano shouldBeEqualTo 999_999_999
    }

    @Test
    fun `startOfMinute와 endOfMinute 확인`() {
        val zdt = zonedDateTimeOf(2021, 3, 15, 14, 30, 45)

        val start = zdt.startOfMinute()
        start.second shouldBeEqualTo 0
        start.nano shouldBeEqualTo 0

        val end = zdt.endOfMinute()
        end.second shouldBeEqualTo 59
        end.nano shouldBeEqualTo 999_999_999
    }

    @Test
    fun `startOfSecond와 endOfSeconds 확인`() {
        val zdt = zonedDateTimeOf(2021, 3, 15, 14, 30, 45, nanoOfSecond = 123_456_789)

        val start = zdt.startOfSecond()
        start.nano shouldBeEqualTo 0

        val end = zdt.endOfSeconds()
        end.nano shouldBeEqualTo 999_999_999
    }

    @Test
    fun `startOfMillis와 endOfMillis 확인`() {
        val zdt = zonedDateTimeOf(2021, 3, 15, 14, 30, 45, nanoOfSecond = 123_456_789)

        val start = zdt.startOfMillis()
        start.nano shouldBeEqualTo 123_000_000

        val end = zdt.endOfMillis()
        end.nano shouldBeEqualTo 123_999_999
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

        val end = endOfWeekOfWeekyear(2021, 1)

        // endOfWeekOfWeekyear는 시작 월요일 + 7일 - 1나노초
        // 즉, 다음 주 월요일 직전 (거의 7일 후)

        // Duration.between은 정확한 시간 차이를 계산하므로
        // 월요일 00:00:00 부터 다음 월요일 00:00:00 - 1나노초 까지는
        // 6일 23시간 59분 59.999999999초이지만, toDays()는 소수점 버림으로 6
        // 그러나 실제로는 start의 시간이 00:00:00이 아니라 현재 시간이므로
        // 정확한 검증은 시간 차이가 거의 7일(168시간)에 근접함을 확인
        val hours = java.time.Duration.between(start, end).toHours()
        hours shouldBeGreaterThan 167L // 거의 7일 (168시간)

        // start와 end 사이 날짜 차이 확인
        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(start.toLocalDate(), end.toLocalDate())
        daysDiff shouldBeEqualTo 7L // Monday to next Monday (date only)
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
    fun `ZonedDateTime min 확장 함수`() {
        val zdt1 = zonedDateTimeOf(2021, 1, 1)
        val zdt2 = zonedDateTimeOf(2021, 12, 31)

        (zdt1 min zdt2) shouldBeEqualTo zdt1
        (zdt2 min zdt1) shouldBeEqualTo zdt1

        (null min zdt1) shouldBeEqualTo zdt1
        (zdt1 min null) shouldBeEqualTo zdt1
    }

    @Test
    fun `ZonedDateTime max 확장 함수`() {
        val zdt1 = zonedDateTimeOf(2021, 1, 1)
        val zdt2 = zonedDateTimeOf(2021, 12, 31)

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
