package io.bluetape4k.javatimes.interval

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.temporal.ChronoUnit

class TemporalIntervalSupportTest {

    companion object: KLogging()

    @Test
    fun `toPeriod - 기본 Period 변환이 올바르게 동작한다`() {
        val start = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2024, 4, 10)
        val interval = temporalIntervalOf(start, end)

        val period = interval.toPeriod()
        period.months shouldBeEqualTo 3
        period.days shouldBeEqualTo 9
    }

    @Test
    fun `toPeriod with DAYS - 일 단위 Period 를 올바르게 반환한다`() {
        val start = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2024, 1, 10)
        val interval = temporalIntervalOf(start, end)

        val period = interval.toPeriod(ChronoUnit.DAYS)
        period shouldBeEqualTo Period.ofDays(9)
    }

    @Test
    fun `toPeriod with WEEKS - 주 단위 Period 를 올바르게 반환한다`() {
        val start = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2024, 1, 22)
        val interval = temporalIntervalOf(start, end)

        val period = interval.toPeriod(ChronoUnit.WEEKS)
        period shouldBeEqualTo Period.ofWeeks(3)
    }

    @Test
    fun `toPeriod with MONTHS - 월 단위 Period 를 올바르게 반환한다`() {
        val start = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2024, 4, 1)
        val interval = temporalIntervalOf(start, end)

        val period = interval.toPeriod(ChronoUnit.MONTHS)
        period shouldBeEqualTo Period.ofMonths(3)
    }

    @Test
    fun `toPeriod with YEARS - 년 단위 Period 를 올바르게 반환한다`() {
        val start = LocalDate.of(2020, 1, 1)
        val end = LocalDate.of(2024, 1, 1)
        val interval = temporalIntervalOf(start, end)

        val period = interval.toPeriod(ChronoUnit.YEARS)
        period shouldBeEqualTo Period.ofYears(4)
    }

    @Test
    fun `toDurationMillis - Duration 밀리초 변환이 올바르다`() {
        val start = java.time.Instant.ofEpochSecond(0)
        val end = java.time.Instant.ofEpochSecond(3600)
        val interval = temporalIntervalOf(start, end)

        interval.toDurationMillis() shouldBeEqualTo 3_600_000L
    }

    @Test
    fun `toInterval - MutableInterval 을 불변 Interval 로 변환한다`() {
        val start = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2024, 12, 31)
        val mutable = mutableTemporalIntervalOf(start, end)

        val immutable = mutable.toInterval()
        immutable.startInclusive shouldBeEqualTo start
        immutable.endExclusive shouldBeEqualTo end
    }

    @Test
    fun `toMutableInterval - 불변 Interval 을 MutableInterval 로 변환한다`() {
        val start = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2024, 12, 31)
        val interval = temporalIntervalOf(start, end)

        val mutable = interval.toMutableInterval()
        mutable.startInclusive shouldBeEqualTo start
        mutable.endExclusive shouldBeEqualTo end
    }

    @Test
    fun `sequence - days 단위로 올바르게 열거한다`() {
        val start = LocalDateTime.of(2024, 1, 1, 0, 0)
        val end = LocalDateTime.of(2024, 1, 6, 0, 0)
        val interval = temporalIntervalOf(start, end)

        val days = interval.days().toList()
        days.size shouldBeEqualTo 5
        days.first() shouldBeEqualTo start
        days.last() shouldBeEqualTo LocalDateTime.of(2024, 1, 5, 0, 0)
    }

    @Test
    fun `sequence - step 2 로 격일 열거한다`() {
        val start = LocalDateTime.of(2024, 1, 1, 0, 0)
        val end = LocalDateTime.of(2024, 1, 10, 0, 0)
        val interval = temporalIntervalOf(start, end)

        val days = interval.sequence(2, ChronoUnit.DAYS).toList()
        days.size shouldBeEqualTo 5
        days[0] shouldBeEqualTo LocalDateTime.of(2024, 1, 1, 0, 0)
        days[1] shouldBeEqualTo LocalDateTime.of(2024, 1, 3, 0, 0)
        days[2] shouldBeEqualTo LocalDateTime.of(2024, 1, 5, 0, 0)
    }

    @Test
    fun `months - 월 단위 열거가 올바르게 동작한다`() {
        val start = LocalDate.of(2024, 1, 1)
        val end = LocalDate.of(2024, 7, 1)
        val interval = temporalIntervalOf(start, end)

        val months = interval.months().toList()
        months.size shouldBeEqualTo 6
        months.first() shouldBeEqualTo LocalDate.of(2024, 1, 1)
        months.last() shouldBeEqualTo LocalDate.of(2024, 6, 1)
    }
}
