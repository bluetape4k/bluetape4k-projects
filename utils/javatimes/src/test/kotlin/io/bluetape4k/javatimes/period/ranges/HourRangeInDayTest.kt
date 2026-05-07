package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.LocalTime

class HourRangeInDayTest {

    companion object : KLogging()

    @Test
    fun `default constructor uses LocalTime min and 23 00`() {
        val range = HourRangeInDay()
        range.start shouldBeEqualTo LocalTime.MIN
        range.end shouldBeEqualTo LocalTime.of(23, 0)
    }

    @Test
    fun `constructor with hour integers`() {
        val range = HourRangeInDay(9, 18)
        range.start shouldBeEqualTo LocalTime.of(9, 0)
        range.end shouldBeEqualTo LocalTime.of(18, 0)
    }

    @Test
    fun `single hour constructor`() {
        val range = HourRangeInDay(10)
        range.start shouldBeEqualTo LocalTime.of(10, 0)
        range.end shouldBeEqualTo LocalTime.of(10, 0)
    }

    @Test
    fun `constructor with LocalTime values`() {
        val start = LocalTime.of(8, 30)
        val end = LocalTime.of(17, 45)
        val range = HourRangeInDay(start, end)
        range.start shouldBeEqualTo start
        range.end shouldBeEqualTo end
    }

    @Test
    fun `compareTo orders by start time`() {
        val morning = HourRangeInDay(8, 12)
        val afternoon = HourRangeInDay(13, 18)
        (morning.compareTo(afternoon) < 0).shouldBeTrue()
        (afternoon.compareTo(morning) > 0).shouldBeTrue()
    }

    @Test
    fun `equals for same start and end`() {
        val a = HourRangeInDay(9, 18)
        val b = HourRangeInDay(9, 18)
        a shouldBeEqualTo b
    }

    @Test
    fun `hashCode consistent with equals`() {
        val a = HourRangeInDay(9, 18)
        val b = HourRangeInDay(9, 18)
        a.hashCode() shouldBeEqualTo b.hashCode()
    }

    @Test
    fun `compareTo equal ranges returns zero`() {
        val a = HourRangeInDay(9, 18)
        val b = HourRangeInDay(9, 18)
        a.compareTo(b) shouldBeEqualTo 0
    }

    @Test
    fun `toString contains start and end`() {
        val range = HourRangeInDay(9, 17)
        val str = range.toString()
        (str.contains("09") || str.contains("9")).shouldBeTrue()
    }

    @Test
    fun `boundary hours 0 to 23`() {
        val range = HourRangeInDay(0, 23)
        range.start shouldBeEqualTo LocalTime.of(0, 0)
        range.end shouldBeEqualTo LocalTime.of(23, 0)
    }

    @Test
    fun `afternoon range start is less than end`() {
        val range = HourRangeInDay(14, 20)
        (range.start < range.end).shouldBeTrue()
    }

    @Test
    fun `different ranges are not equal`() {
        val a = HourRangeInDay(9, 17)
        val b = HourRangeInDay(9, 18)
        (a == b).shouldBeFalse()
    }

    @Test
    fun `hashCode differs for different ranges`() {
        val a = HourRangeInDay(9, 17)
        val b = HourRangeInDay(10, 18)
        (a.hashCode() != b.hashCode()).shouldBeTrue()
    }

    @Test
    fun `sorted list of ranges ordered by start`() {
        val ranges = listOf(
            HourRangeInDay(14, 18),
            HourRangeInDay(8, 12),
            HourRangeInDay(9, 17),
        ).sorted()
        ranges[0].start shouldBeEqualTo LocalTime.of(8, 0)
        ranges[1].start shouldBeEqualTo LocalTime.of(9, 0)
        ranges[2].start shouldBeEqualTo LocalTime.of(14, 0)
    }
}
