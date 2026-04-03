package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.period.ranges.YearCalendarTimeRange
import io.bluetape4k.javatimes.zonedDateTimeOf
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TimePeriodSupportTest {

    private val fiscalCalendar = object : TimeCalendar() {
        override val baseMonth: Int = 4
    }

    @Test
    fun `yearOf는 기준 월 이전이면 이전 연도로 계산한다`() {
        yearOf(2025, 3, fiscalCalendar) shouldBeEqualTo 2024
        yearOf(2025, 4, fiscalCalendar) shouldBeEqualTo 2025
    }

    @Test
    fun `custom calendar은 재정의한 baseMonth를 노출한다`() {
        fiscalCalendar.baseMonth shouldBeEqualTo 4
    }

    @Test
    fun `ZonedDateTime yearOf는 전달한 calendar의 기준 월을 따른다`() {
        val march = zonedDateTimeOf(2025, 3, 1)

        march.yearOf(fiscalCalendar) shouldBeEqualTo 2024
    }

    @Test
    fun `YearCalendarTimeRange의 baseYear는 달력 기준 월을 따른다`() {
        val range = YearCalendarTimeRange(
            TimeRange(
                zonedDateTimeOf(2025, 3, 1),
                zonedDateTimeOf(2025, 4, 1),
            ),
            fiscalCalendar,
        )

        range.baseYear shouldBeEqualTo 2024
    }

    @Test
    fun `yearOf는 잘못된 monthOfYear에 대해 예외를 던진다`() {
        assertThrows<IllegalArgumentException> {
            yearOf(2025, 13)
        }
    }
}
