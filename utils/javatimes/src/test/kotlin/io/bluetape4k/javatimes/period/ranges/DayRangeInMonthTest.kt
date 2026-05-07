package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.MaxDaysPerMonth
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DayRangeInMonthTest {

    companion object : KLogging()

    @Test
    fun `default constructor uses 1 to MaxDaysPerMonth`() {
        val range = DayRangeInMonth()
        range.startDayOfMonth shouldBeEqualTo 1
        range.endDayOfMonth shouldBeEqualTo MaxDaysPerMonth
    }

    @Test
    fun `constructor with valid range`() {
        val range = DayRangeInMonth(5, 21)
        range.startDayOfMonth shouldBeEqualTo 5
        range.endDayOfMonth shouldBeEqualTo 21
    }

    @Test
    fun `single day range`() {
        val range = DayRangeInMonth(15, 15)
        range.isSingleDay.shouldBeTrue()
        range.startDayOfMonth shouldBeEqualTo 15
        range.endDayOfMonth shouldBeEqualTo 15
    }

    @Test
    fun `multi-day range is not single day`() {
        val range = DayRangeInMonth(1, 10)
        range.isSingleDay.shouldBeFalse()
    }

    @Test
    fun `hasInside returns true for day within range`() {
        val range = DayRangeInMonth(5, 25)
        range.hasInside(10).shouldBeTrue()
        range.hasInside(5).shouldBeTrue()
        range.hasInside(25).shouldBeTrue()
    }

    @Test
    fun `hasInside returns false for day outside range`() {
        val range = DayRangeInMonth(5, 25)
        range.hasInside(4).shouldBeFalse()
        range.hasInside(26).shouldBeFalse()
    }

    @Test
    fun `invalid start day throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            DayRangeInMonth(0, 10)
        }
    }

    @Test
    fun `invalid end day over max throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            DayRangeInMonth(1, 32)
        }
    }

    @Test
    fun `start greater than end throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            DayRangeInMonth(20, 5)
        }
    }

    @Test
    fun `equals for same range`() {
        val a = DayRangeInMonth(1, 15)
        val b = DayRangeInMonth(1, 15)
        a shouldBeEqualTo b
    }

    @Test
    fun `not equal for different range`() {
        val a = DayRangeInMonth(1, 15)
        val b = DayRangeInMonth(1, 20)
        (a == b).shouldBeFalse()
    }

    @Test
    fun `hashCode consistent with equals`() {
        val a = DayRangeInMonth(5, 20)
        val b = DayRangeInMonth(5, 20)
        a.hashCode() shouldBeEqualTo b.hashCode()
    }

    @Test
    fun `compareTo orders by startDayOfMonth`() {
        val early = DayRangeInMonth(1, 10)
        val late = DayRangeInMonth(15, 28)
        (early.compareTo(late) < 0).shouldBeTrue()
    }

    @Test
    fun `boundary range 1 to 31`() {
        val range = DayRangeInMonth(1, 31)
        range.startDayOfMonth shouldBeEqualTo 1
        range.endDayOfMonth shouldBeEqualTo 31
    }

    @Test
    fun `sorted list of ranges ordered correctly`() {
        val ranges = listOf(
            DayRangeInMonth(20, 28),
            DayRangeInMonth(1, 5),
            DayRangeInMonth(10, 15),
        ).sorted()
        ranges[0].startDayOfMonth shouldBeEqualTo 1
        ranges[1].startDayOfMonth shouldBeEqualTo 10
        ranges[2].startDayOfMonth shouldBeEqualTo 20
    }
}
