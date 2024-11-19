package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

class TimePeriodContainerTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `construct with TimeRange`() {
        val start1 = zonedDateTimeOf(2018, 4, 15)
        val start2 = zonedDateTimeOf(2018, 4, 22)
        val period1 = TimeRange(start1)
        val period2 = TimeRange(start2)

        val container = TimePeriodContainer(period1, period2)
        container shouldHaveSize 2

        val container2 = TimePeriodContainer(period1, period2, container)
        container2 shouldHaveSize 2
    }

    @Test
    fun `add period`() {
        val start1 = zonedDateTimeOf(2018, 4, 15)
        val start2 = zonedDateTimeOf(2018, 4, 22)
        val period1 = TimeRange(start1)
        val period2 = TimeRange(start2)

        val container = TimePeriodContainer(period1, period2)

        val container2 = TimePeriodContainer(period1, period2, container)
        container2.size shouldBeEqualTo 2

        container2.add(period1)
        container2 shouldHaveSize 2

        container2.add(TimeBlock(start1, start2))
        container2 shouldHaveSize 3
    }
}
