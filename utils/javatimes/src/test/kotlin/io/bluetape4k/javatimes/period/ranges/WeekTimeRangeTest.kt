package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.DaysPerWeek
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

class WeekTimeRangeTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `default constructor uses current time with weekCount 1`() {
        val range = WeekTimeRange()
        range.weekCount shouldBeEqualTo 1
    }

    @Test
    fun `weekCount property is correct`() {
        val range = WeekTimeRange(now, weekCount = 2)
        range.weekCount shouldBeEqualTo 2
    }

    @Test
    fun `days returns DaysPerWeek times weekCount elements`() {
        val range = WeekTimeRange(now, weekCount = 2)
        range.days() shouldHaveSize 2 * DaysPerWeek
    }

    @Test
    fun `daySequence returns lazy sequence`() {
        val range = WeekTimeRange(now, weekCount = 1)
        range.daySequence().count() shouldBeEqualTo DaysPerWeek
    }

    @Test
    fun `year property returns the week year`() {
        val range = WeekTimeRange(now, weekCount = 1)
        range.year shouldBeEqualTo range.start.year
    }

    @Test
    fun `weekCount 4 spans 4 weeks`() {
        val range = WeekTimeRange(now, weekCount = 4)
        range.days() shouldHaveSize 4 * DaysPerWeek
    }

    @Test
    fun `weekyear and weekOfWeekyear are accessible`() {
        val range = WeekTimeRange(now, weekCount = 1)
        // Just verify they can be accessed without throwing
        val weekyear = range.weekyear
        val weekOfWeekyear = range.weekOfWeekyear
        (weekyear > 0).shouldBeEqualTo(true)
        (weekOfWeekyear in 1..53).shouldBeEqualTo(true)
    }
}
