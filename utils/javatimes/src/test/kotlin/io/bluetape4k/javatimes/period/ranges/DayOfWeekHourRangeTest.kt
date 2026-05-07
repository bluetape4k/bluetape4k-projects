package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.DayOfWeek
import java.time.LocalTime

class DayOfWeekHourRangeTest {

    companion object : KLogging()

    @Test
    fun `default hours for Monday`() {
        val range = DayOfWeekHourRange(DayOfWeek.MONDAY)
        range.dayOfWeek shouldBeEqualTo DayOfWeek.MONDAY
        range.start shouldBeEqualTo LocalTime.of(0, 0)
        range.end shouldBeEqualTo LocalTime.of(23, 0)
    }

    @Test
    fun `business hours for Wednesday`() {
        val range = DayOfWeekHourRange(DayOfWeek.WEDNESDAY, 9, 18)
        range.dayOfWeek shouldBeEqualTo DayOfWeek.WEDNESDAY
        range.start shouldBeEqualTo LocalTime.of(9, 0)
        range.end shouldBeEqualTo LocalTime.of(18, 0)
    }

    @Test
    fun `single hour range`() {
        val range = DayOfWeekHourRange(DayOfWeek.FRIDAY, 10, 10)
        range.start shouldBeEqualTo LocalTime.of(10, 0)
        range.end shouldBeEqualTo LocalTime.of(10, 0)
    }

    @Test
    fun `invalid startHourOfDay throws exception`() {
        assertThrows<Exception> {
            DayOfWeekHourRange(DayOfWeek.MONDAY, -1, 18)
        }
    }

    @Test
    fun `invalid endHourOfDay throws exception`() {
        assertThrows<Exception> {
            DayOfWeekHourRange(DayOfWeek.MONDAY, 0, 24)
        }
    }

    @Test
    fun `start greater than end throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            DayOfWeekHourRange(DayOfWeek.MONDAY, 18, 9)
        }
    }

    @Test
    fun `same hours are considered equal via parent class`() {
        val a = DayOfWeekHourRange(DayOfWeek.TUESDAY, 9, 17)
        val b = DayOfWeekHourRange(DayOfWeek.TUESDAY, 9, 17)
        // start and end are the same so they are equal via HourRangeInDay.equalProperties
        a shouldBeEqualTo b
    }

    @Test
    fun `dayOfWeek property differs between instances`() {
        val a = DayOfWeekHourRange(DayOfWeek.MONDAY, 9, 17)
        val b = DayOfWeekHourRange(DayOfWeek.TUESDAY, 9, 17)
        // equals is inherited from HourRangeInDay which only compares start/end time
        // dayOfWeek must be compared explicitly
        (a.dayOfWeek != b.dayOfWeek).shouldBeTrue()
    }

    @Test
    fun `hashCode consistent with equals`() {
        val a = DayOfWeekHourRange(DayOfWeek.THURSDAY, 8, 16)
        val b = DayOfWeekHourRange(DayOfWeek.THURSDAY, 8, 16)
        a.hashCode() shouldBeEqualTo b.hashCode()
    }

    @Test
    fun `hashCode differs for different dayOfWeek`() {
        val a = DayOfWeekHourRange(DayOfWeek.MONDAY, 9, 17)
        val b = DayOfWeekHourRange(DayOfWeek.FRIDAY, 9, 17)
        (a.hashCode() != b.hashCode()).shouldBeTrue()
    }

    @Test
    fun `boundary full day range 0 to 23`() {
        val range = DayOfWeekHourRange(DayOfWeek.SATURDAY, 0, 23)
        range.start shouldBeEqualTo LocalTime.of(0, 0)
        range.end shouldBeEqualTo LocalTime.of(23, 0)
    }

    @Test
    fun `toString contains dayOfWeek`() {
        val range = DayOfWeekHourRange(DayOfWeek.SUNDAY, 6, 14)
        range.toString().contains("SUNDAY").shouldBeTrue()
    }

    @Test
    fun `all days of week can be created`() {
        DayOfWeek.entries.forEach { dow ->
            val range = DayOfWeekHourRange(dow, 9, 17)
            range.dayOfWeek shouldBeEqualTo dow
        }
    }
}
