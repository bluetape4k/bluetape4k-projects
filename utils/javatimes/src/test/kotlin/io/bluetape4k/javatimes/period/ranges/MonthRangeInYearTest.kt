package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Month

class MonthRangeInYearTest {

    companion object : KLogging()

    @Test
    fun `default constructor uses JANUARY to DECEMBER`() {
        val range = MonthRangeInYear()
        range.startMonth shouldBeEqualTo Month.JANUARY
        range.endMonth shouldBeEqualTo Month.DECEMBER
    }

    @Test
    fun `constructor with Month values`() {
        val range = MonthRangeInYear(Month.MARCH, Month.SEPTEMBER)
        range.startMonth shouldBeEqualTo Month.MARCH
        range.endMonth shouldBeEqualTo Month.SEPTEMBER
    }

    @Test
    fun `companion invoke with int month values`() {
        val range = MonthRangeInYear(3, 9)
        range.startMonthOfYear shouldBeEqualTo 3
        range.endMonthOfYear shouldBeEqualTo 9
    }

    @Test
    fun `single month range`() {
        val range = MonthRangeInYear(Month.JUNE, Month.JUNE)
        range.isSingleMonth.shouldBeTrue()
    }

    @Test
    fun `multi month range is not single month`() {
        val range = MonthRangeInYear(Month.JANUARY, Month.JUNE)
        range.isSingleMonth.shouldBeFalse()
    }

    @Test
    fun `startMonthOfYear and endMonthOfYear are correct`() {
        val range = MonthRangeInYear(Month.APRIL, Month.OCTOBER)
        range.startMonthOfYear shouldBeEqualTo 4
        range.endMonthOfYear shouldBeEqualTo 10
    }

    @Test
    fun `hasInside returns true for month within range`() {
        val range = MonthRangeInYear(Month.MARCH, Month.SEPTEMBER)
        range.hasInside(Month.MARCH).shouldBeTrue()
        range.hasInside(Month.JUNE).shouldBeTrue()
        range.hasInside(Month.SEPTEMBER).shouldBeTrue()
    }

    @Test
    fun `hasInside returns false for month outside range`() {
        val range = MonthRangeInYear(Month.MARCH, Month.SEPTEMBER)
        range.hasInside(Month.FEBRUARY).shouldBeFalse()
        range.hasInside(Month.OCTOBER).shouldBeFalse()
    }

    @Test
    fun `start greater than end throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            MonthRangeInYear(Month.DECEMBER, Month.JANUARY)
        }
    }

    @Test
    fun `equals for same range`() {
        val a = MonthRangeInYear(Month.JANUARY, Month.JUNE)
        val b = MonthRangeInYear(Month.JANUARY, Month.JUNE)
        a shouldBeEqualTo b
    }

    @Test
    fun `not equal for different range`() {
        val a = MonthRangeInYear(Month.JANUARY, Month.JUNE)
        val b = MonthRangeInYear(Month.JANUARY, Month.DECEMBER)
        (a == b).shouldBeFalse()
    }

    @Test
    fun `hashCode consistent with equals`() {
        val a = MonthRangeInYear(Month.APRIL, Month.OCTOBER)
        val b = MonthRangeInYear(Month.APRIL, Month.OCTOBER)
        a.hashCode() shouldBeEqualTo b.hashCode()
    }

    @Test
    fun `compareTo orders by startMonth`() {
        val q1 = MonthRangeInYear(Month.JANUARY, Month.MARCH)
        val q3 = MonthRangeInYear(Month.JULY, Month.SEPTEMBER)
        (q1.compareTo(q3) < 0).shouldBeTrue()
    }

    @Test
    fun `sorted list of ranges ordered by startMonth`() {
        val ranges = listOf(
            MonthRangeInYear(Month.OCTOBER, Month.DECEMBER),
            MonthRangeInYear(Month.JANUARY, Month.MARCH),
            MonthRangeInYear(Month.APRIL, Month.JUNE),
        ).sorted()
        ranges[0].startMonth shouldBeEqualTo Month.JANUARY
        ranges[1].startMonth shouldBeEqualTo Month.APRIL
        ranges[2].startMonth shouldBeEqualTo Month.OCTOBER
    }
}
