package io.bluetape4k.javatimes

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeNull
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * [OffsetDateTimeSupport.kt]에 대한 테스트
 */
class OffsetDateTimeSupportTest {

    companion object: KLogging()

    @Test
    fun `offsetDateTimeOf로 OffsetDateTime 생성 - 전체 파라미터`() {
        val odt = offsetDateTimeOf(
            year = 2021,
            monthOfYear = 3,
            dayOfMonth = 15,
            hourOfDay = 14,
            minuteOfHour = 30,
            secondOfMinute = 45,
            milliOfSecond = 123,
            offset = ZoneOffset.UTC
        )

        odt.shouldNotBeNull()
        odt.year shouldBeEqualTo 2021
        odt.monthValue shouldBeEqualTo 3
        odt.dayOfMonth shouldBeEqualTo 15
        odt.hour shouldBeEqualTo 14
        odt.minute shouldBeEqualTo 30
        odt.second shouldBeEqualTo 45
        odt.nano shouldBeEqualTo 123_000_000
        odt.offset shouldBeEqualTo ZoneOffset.UTC
    }

    @Test
    fun `offsetDateTimeOf로 OffsetDateTime 생성 - 기본값 사용`() {
        val odt = offsetDateTimeOf(year = 2021)

        odt.shouldNotBeNull()
        odt.year shouldBeEqualTo 2021
        odt.monthValue shouldBeEqualTo 1
        odt.dayOfMonth shouldBeEqualTo 1
        odt.hour shouldBeEqualTo 0
        odt.minute shouldBeEqualTo 0
        odt.second shouldBeEqualTo 0
        odt.nano shouldBeEqualTo 0
    }

    @Test
    fun `offsetDateTimeOf로 OffsetDateTime 생성 - LocalDate와 LocalTime 사용`() {
        val localDate = LocalDate.of(2021, 6, 1)
        val localTime = LocalTime.of(12, 30, 45)
        val offset = ZoneOffset.ofHours(9)

        val odt = offsetDateTimeOf(localDate, localTime, offset)

        odt.shouldNotBeNull()
        odt.toLocalDate() shouldBeEqualTo localDate
        odt.toLocalTime() shouldBeEqualTo localTime
        odt.offset shouldBeEqualTo offset
    }

    @Test
    fun `offsetDateTimeOf로 OffsetDateTime 생성 - 기본 LocalDate와 LocalTime`() {
        val odt = offsetDateTimeOf()

        odt.shouldNotBeNull()
        odt.toLocalDate() shouldBeEqualTo LocalDate.ofEpochDay(0)
        odt.toLocalTime() shouldBeEqualTo LocalTime.ofSecondOfDay(0)
    }

    @Test
    fun `offsetTimeOf로 OffsetTime 생성 - 전체 파라미터`() {
        val ot = offsetTimeOf(
            hourOfDay = 14,
            minuteOfHour = 30,
            secondOfMinute = 45,
            nanoOfSeconds = 123_456_789,
            offset = ZoneOffset.ofHours(9)
        )

        ot.shouldNotBeNull()
        ot.hour shouldBeEqualTo 14
        ot.minute shouldBeEqualTo 30
        ot.second shouldBeEqualTo 45
        ot.nano shouldBeEqualTo 123_456_789
        ot.offset shouldBeEqualTo ZoneOffset.ofHours(9)
    }

    @Test
    fun `offsetTimeOf로 OffsetTime 생성 - 기본값 사용`() {
        val ot = offsetTimeOf(hourOfDay = 12)

        ot.shouldNotBeNull()
        ot.hour shouldBeEqualTo 12
        ot.minute shouldBeEqualTo 0
        ot.second shouldBeEqualTo 0
        ot.nano shouldBeEqualTo 0
        ot.offset shouldBeEqualTo ZoneOffset.UTC
    }

    @Test
    fun `OffsetTime을 Instant로 변환`() {
        val ot = offsetTimeOf(
            hourOfDay = 12,
            minuteOfHour = 0,
            secondOfMinute = 0,
            nanoOfSeconds = 0,
            offset = ZoneOffset.UTC
        )

        // OffsetTime은 날짜 정보가 없어서 직접 Instant로 변환 불가
        // 대신 현재 날짜와 결합하여 Instant 생성
        val instant = ot.atDate(LocalDate.now()).toInstant()

        instant.shouldNotBeNull()
    }

    @Test
    fun `OffsetTime 확장 toInstant는 epoch date를 기본으로 사용한다`() {
        val ot = offsetTimeOf(12, 0, 0, 0, ZoneOffset.UTC)
        ot.toInstant().toEpochMilli() shouldBeEqualTo 12L * 60L * 60L * 1000L
    }

    @Test
    fun `다양한 ZoneOffset으로 OffsetDateTime 생성`() {
        val year = 2021
        val month = 12
        val day = 25

        val utc = offsetDateTimeOf(year, month, day, offset = ZoneOffset.UTC)
        val plusNine = offsetDateTimeOf(year, month, day, offset = ZoneOffset.ofHours(9))
        val minusFive = offsetDateTimeOf(year, month, day, offset = ZoneOffset.ofHours(-5))

        utc.offset shouldBeEqualTo ZoneOffset.UTC
        plusNine.offset shouldBeEqualTo ZoneOffset.ofHours(9)
        minusFive.offset shouldBeEqualTo ZoneOffset.ofHours(-5)

        // 동일한 시각이지만 offset이 다름
        utc.toLocalDateTime() shouldBeEqualTo plusNine.toLocalDateTime()
        utc.toLocalDateTime() shouldBeEqualTo minusFive.toLocalDateTime()
    }

    @Test
    fun `millisToNanos 확장 함수 동작 확인`() {
        val millis = 123
        val nanos = millis.millisToNanos()

        nanos shouldBeEqualTo 123_000_000
    }

    @Test
    fun `SystemOffset 사용하여 OffsetDateTime 생성`() {
        val odt = offsetDateTimeOf(2021, 1, 1)

        odt.shouldNotBeNull()
        // SystemOffset이 기본값으로 사용됨
        odt.offset shouldBeEqualTo SystemOffset
    }
}
