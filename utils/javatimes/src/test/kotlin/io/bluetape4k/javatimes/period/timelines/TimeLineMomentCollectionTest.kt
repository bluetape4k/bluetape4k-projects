package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.TimeRange
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeNull
import org.junit.jupiter.api.Test

class TimeLineMomentCollectionTest: AbstractPeriodTest() {

    companion object: KLogging()

    private val start = zonedDateTimeOf(2024, 1, 1)
    private val end = zonedDateTimeOf(2024, 1, 10)

    @Test
    fun `empty collection has size zero`() {
        val collection = TimeLineMomentCollection()
        collection.size shouldBeEqualTo 0
    }

    @Test
    fun `add period creates two moments start and end`() {
        val collection = TimeLineMomentCollection()
        val period = TimeRange(start, end)
        collection.add(period)

        collection shouldHaveSize 2
        collection.find(start).shouldNotBeNull()
        collection.find(end).shouldNotBeNull()
    }

    @Test
    fun `find returns moment at the given time`() {
        val collection = TimeLineMomentCollection()
        collection.add(TimeRange(start, end))

        collection.find(start)?.moment shouldBeEqualTo start
        collection.find(end)?.moment shouldBeEqualTo end
    }

    @Test
    fun `find returns null for non-existent moment`() {
        val collection = TimeLineMomentCollection()
        collection.add(TimeRange(start, end))
        collection.find(start.plusDays(3)).shouldBeNull()
    }

    @Test
    fun `contains returns true for existing moment`() {
        val collection = TimeLineMomentCollection()
        collection.add(TimeRange(start, end))
        collection.contains(start).shouldBeTrue()
    }

    @Test
    fun `minOrNull returns earliest moment`() {
        val collection = TimeLineMomentCollection()
        collection.add(TimeRange(start, end))
        collection.minOrNull()?.moment shouldBeEqualTo start
    }

    @Test
    fun `maxOrNull returns latest moment`() {
        val collection = TimeLineMomentCollection()
        collection.add(TimeRange(start, end))
        collection.maxOrNull()?.moment shouldBeEqualTo end
    }

    @Test
    fun `remove period removes associated moments`() {
        val collection = TimeLineMomentCollection()
        val period = TimeRange(start, end)
        collection.add(period)
        collection.remove(period)

        // After removal, moments with no periods should be removed
        collection.size shouldBeEqualTo 0
    }

    @Test
    fun `addAll from multiple periods creates all moments`() {
        val collection = TimeLineMomentCollection()
        val p1 = TimeRange(start, end)
        val p2 = TimeRange(end, end.plusDays(5))
        collection.addAll(listOf(p1, p2))

        // start, end (shared), end+5d → 3 distinct moment times
        collection.find(start).shouldNotBeNull()
        collection.find(end).shouldNotBeNull()
        collection.find(end.plusDays(5)).shouldNotBeNull()
    }

    @Test
    fun `startCount from moment reflects period count`() {
        val collection = TimeLineMomentCollection()
        val p1 = TimeRange(start, end)
        val p2 = TimeRange(start, end.plusDays(5))
        collection.add(p1)
        collection.add(p2)

        collection.find(start)?.startCount shouldBeEqualTo 2L
    }
}
