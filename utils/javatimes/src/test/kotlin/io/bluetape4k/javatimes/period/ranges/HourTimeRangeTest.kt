package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.MinutesPerHour
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldHaveSize
import org.junit.jupiter.api.Test

class HourTimeRangeTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `default constructor uses current time with hourCount 1`() {
        val range = HourTimeRange()
        range.hourCount shouldBeEqualTo 1
    }

    @Test
    fun `hourCount property is correct`() {
        val range = HourTimeRange(now, hourCount = 3)
        range.hourCount shouldBeEqualTo 3
    }

    @Test
    fun `minutes returns MinutesPerHour times hourCount elements`() {
        val range = HourTimeRange(now, hourCount = 2)
        range.minutes() shouldHaveSize 2 * MinutesPerHour
    }

    @Test
    fun `minuteSequence returns lazy sequence`() {
        val range = HourTimeRange(now, hourCount = 1)
        range.minuteSequence().count() shouldBeEqualTo MinutesPerHour
    }

    @Test
    fun `hourOfDayOfEnd is correct for single hour`() {
        val range = HourTimeRange(now, hourCount = 1)
        range.hourOfDayOfEnd shouldBeEqualTo range.end.hour
    }

    @Test
    fun `hourCount 3 gives 3 times 60 minutes`() {
        val range = HourTimeRange(now, hourCount = 3)
        range.minutes() shouldHaveSize 3 * MinutesPerHour
    }

    @Test
    fun `start is beginning of the hour`() {
        val range = HourTimeRange(now, hourCount = 1)
        range.start.minute shouldBeEqualTo 0
        range.start.second shouldBeEqualTo 0
    }
}
