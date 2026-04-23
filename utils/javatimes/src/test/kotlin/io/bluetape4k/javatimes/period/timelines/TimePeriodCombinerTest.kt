package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.TimeRange
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test

class TimePeriodCombinerTest: AbstractPeriodTest() {

    companion object: KLogging()

    private val combiner = TimePeriodCombiner<ITimePeriod>()

    @Test
    fun `empty collection returns empty result`() {
        val result = combiner.combinePeriods(emptyList())
        result.shouldBeEmpty()
    }

    @Test
    fun `single period returns same period`() {
        val start = zonedDateTimeOf(2024, 1, 1)
        val end = zonedDateTimeOf(2024, 1, 5)
        val result = combiner.combinePeriods(listOf(TimeRange(start, end)))

        result shouldHaveSize 1
        result.periods.first().start shouldBeEqualTo start
        result.periods.first().end shouldBeEqualTo end
    }

    @Test
    fun `non-overlapping periods remain separate`() {
        val p1 = TimeRange(zonedDateTimeOf(2024, 1, 1), zonedDateTimeOf(2024, 1, 5))
        val p2 = TimeRange(zonedDateTimeOf(2024, 1, 10), zonedDateTimeOf(2024, 1, 15))

        val result = combiner.combinePeriods(listOf(p1, p2))
        result shouldHaveSize 2
    }

    @Test
    fun `overlapping periods are merged into one`() {
        val p1 = TimeRange(zonedDateTimeOf(2024, 1, 1), zonedDateTimeOf(2024, 1, 10))
        val p2 = TimeRange(zonedDateTimeOf(2024, 1, 5), zonedDateTimeOf(2024, 1, 15))

        val result = combiner.combinePeriods(listOf(p1, p2))
        result shouldHaveSize 1
        result.periods.first().start shouldBeEqualTo zonedDateTimeOf(2024, 1, 1)
        result.periods.first().end shouldBeEqualTo zonedDateTimeOf(2024, 1, 15)
    }

    @Test
    fun `adjacent periods are merged into one`() {
        val p1 = TimeRange(zonedDateTimeOf(2024, 1, 1), zonedDateTimeOf(2024, 1, 5))
        val p2 = TimeRange(zonedDateTimeOf(2024, 1, 5), zonedDateTimeOf(2024, 1, 10))

        val result = combiner.combinePeriods(listOf(p1, p2))
        result shouldHaveSize 1
        result.periods.first().start shouldBeEqualTo zonedDateTimeOf(2024, 1, 1)
        result.periods.first().end shouldBeEqualTo zonedDateTimeOf(2024, 1, 10)
    }

    @Test
    fun `three overlapping periods merge into one`() {
        val p1 = TimeRange(zonedDateTimeOf(2024, 1, 1), zonedDateTimeOf(2024, 1, 10))
        val p2 = TimeRange(zonedDateTimeOf(2024, 1, 5), zonedDateTimeOf(2024, 1, 15))
        val p3 = TimeRange(zonedDateTimeOf(2024, 1, 12), zonedDateTimeOf(2024, 1, 20))

        val result = combiner.combinePeriods(listOf(p1, p2, p3))
        result shouldHaveSize 1
        result.periods.first().start shouldBeEqualTo zonedDateTimeOf(2024, 1, 1)
        result.periods.first().end shouldBeEqualTo zonedDateTimeOf(2024, 1, 20)
    }

    @Test
    fun `contained period does not expand the container`() {
        val outer = TimeRange(zonedDateTimeOf(2024, 1, 1), zonedDateTimeOf(2024, 1, 31))
        val inner = TimeRange(zonedDateTimeOf(2024, 1, 10), zonedDateTimeOf(2024, 1, 20))

        val result = combiner.combinePeriods(listOf(outer, inner))
        result shouldHaveSize 1
        result.periods.first().start shouldBeEqualTo zonedDateTimeOf(2024, 1, 1)
        result.periods.first().end shouldBeEqualTo zonedDateTimeOf(2024, 1, 31)
    }

    @Test
    fun `multiple separate groups remain separate`() {
        val p1 = TimeRange(zonedDateTimeOf(2024, 1, 1), zonedDateTimeOf(2024, 1, 5))
        val p2 = TimeRange(zonedDateTimeOf(2024, 1, 3), zonedDateTimeOf(2024, 1, 8))
        val p3 = TimeRange(zonedDateTimeOf(2024, 2, 1), zonedDateTimeOf(2024, 2, 5))
        val p4 = TimeRange(zonedDateTimeOf(2024, 2, 3), zonedDateTimeOf(2024, 2, 10))

        val result = combiner.combinePeriods(listOf(p1, p2, p3, p4))
        result shouldHaveSize 2
    }
}
