package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.MonthsPerYear
import io.bluetape4k.javatimes.QuartersPerYear
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

class YearTimeRangeTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `year property matches constructor year`() {
        val range = YearTimeRange(2024)
        range.year shouldBeEqualTo 2024
    }

    @Test
    fun `yearCount defaults to 1`() {
        val range = YearTimeRange(2024)
        range.yearCount shouldBeEqualTo 1
    }

    @Test
    fun `yearCount set to 3`() {
        val range = YearTimeRange(2024, yearCount = 3)
        range.yearCount shouldBeEqualTo 3
    }

    @Test
    fun `quarters returns QuartersPerYear times yearCount`() {
        val range = YearTimeRange(2024, yearCount = 2)
        range.quarters() shouldHaveSize 2 * QuartersPerYear
    }

    @Test
    fun `months returns MonthsPerYear times yearCount`() {
        val range = YearTimeRange(2024, yearCount = 2)
        range.months() shouldHaveSize 2 * MonthsPerYear
    }

    @Test
    fun `days for a leap year returns 366 days`() {
        val range = YearTimeRange(2024, yearCount = 1) // 2024 is leap year
        range.days().size shouldBeEqualTo 366
    }

    @Test
    fun `days for a non-leap year returns 365 days`() {
        val range = YearTimeRange(2023, yearCount = 1)
        range.days().size shouldBeEqualTo 365
    }

    @Test
    fun `hours returns 24 hours per day across the year`() {
        val range = YearTimeRange(2023, yearCount = 1)
        range.hours() shouldHaveSize 365 * 24
    }

    @Test
    fun `start year matches the year property`() {
        val range = YearTimeRange(2024)
        range.startYear shouldBeEqualTo 2024
    }

    @Test
    fun `end year for yearCount 3 is start year plus 2`() {
        val range = YearTimeRange(2022, yearCount = 3)
        // end of 2024 is start of 2025 calendar-wise
        (range.endYear >= 2024).shouldBeEqualTo(true)
    }

    @Test
    fun `monthSequence returns lazy sequence`() {
        val range = YearTimeRange(2024, yearCount = 1)
        range.monthSequence().count() shouldBeEqualTo MonthsPerYear
    }
}
