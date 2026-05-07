package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.TimeRange
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class TimeLineMomentTest: AbstractPeriodTest() {

    companion object: KLogging()

    private val testMoment = zonedDateTimeOf(2024, 6, 1, 9, 0)

    @Test
    fun `moment property returns the ZonedDateTime`() {
        val moment = TimeLineMoment(testMoment)
        moment.moment shouldBeEqualTo testMoment
    }

    @Test
    fun `startCount is zero for empty periods`() {
        val moment = TimeLineMoment(testMoment)
        moment.startCount shouldBeEqualTo 0L
    }

    @Test
    fun `endCount is zero for empty periods`() {
        val moment = TimeLineMoment(testMoment)
        moment.endCount shouldBeEqualTo 0L
    }

    @Test
    fun `startCount counts periods starting at this moment`() {
        val moment = TimeLineMoment(testMoment)
        val end = testMoment.plusDays(1)
        moment.periods.add(TimeRange(testMoment, end))

        moment.startCount shouldBeEqualTo 1L
        moment.endCount shouldBeEqualTo 0L
    }

    @Test
    fun `endCount counts periods ending at this moment`() {
        val moment = TimeLineMoment(testMoment)
        val start = testMoment.minusDays(1)
        moment.periods.add(TimeRange(start, testMoment))

        moment.startCount shouldBeEqualTo 0L
        moment.endCount shouldBeEqualTo 1L
    }

    @Test
    fun `compareTo orders by moment`() {
        val earlier = TimeLineMoment(testMoment.minusHours(1))
        val later = TimeLineMoment(testMoment.plusHours(1))

        (earlier < later).shouldBeTrue()
        (later > earlier).shouldBeTrue()
    }

    @Test
    fun `equals is true for same moment`() {
        val m1 = TimeLineMoment(testMoment)
        val m2 = TimeLineMoment(testMoment)
        m1 shouldBeEqualTo m2
    }

    @Test
    fun `hashCode is same for equal moments`() {
        val m1 = TimeLineMoment(testMoment)
        val m2 = TimeLineMoment(testMoment)
        m1.hashCode() shouldBeEqualTo m2.hashCode()
    }

    @Test
    fun `multiple periods at same moment accumulate startCount`() {
        val moment = TimeLineMoment(testMoment)
        moment.periods.add(TimeRange(testMoment, testMoment.plusHours(1)))
        moment.periods.add(TimeRange(testMoment, testMoment.plusHours(2)))

        moment.startCount shouldBeEqualTo 2L
    }
}
