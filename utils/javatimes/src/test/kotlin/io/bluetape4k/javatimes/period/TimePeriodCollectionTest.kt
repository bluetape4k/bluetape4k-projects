package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class TimePeriodCollectionTest {

    companion object : KLogging()

    private fun makeRange(offsetDays: Long, durationDays: Long): TimeRange {
        val now = nowZonedDateTime()
        return TimeRange(now.plusDays(offsetDays), now.plusDays(offsetDays + durationDays))
    }

    @Test
    fun `EMPTY is an empty collection`() {
        TimePeriodCollection.EMPTY.periods.isEmpty().shouldBeTrue()
    }

    @Test
    fun `invoke with single element`() {
        val r = makeRange(0, 3)
        val collection = TimePeriodCollection(r)
        collection.periods.size shouldBeEqualTo 1
    }

    @Test
    fun `invoke with multiple elements`() {
        val r1 = makeRange(0, 3)
        val r2 = makeRange(1, 5)
        val collection = TimePeriodCollection(r1, r2)
        collection.periods.size shouldBeEqualTo 2
    }

    @Test
    fun `ofAll creates collection from list`() {
        val periods = listOf(makeRange(0, 2), makeRange(3, 2), makeRange(6, 2))
        val collection = TimePeriodCollection.ofAll(periods)
        collection.periods.size shouldBeEqualTo 3
    }

    @Test
    fun `hasInsidePeriods returns true when collection period fits inside target`() {
        val now = nowZonedDateTime()
        // small is inside big — hasInsideWith(big) means "small fits inside big"
        val big = TimeRange(now, now.plusDays(10))
        val small = TimeRange(now.plusDays(2), now.plusDays(4))
        val collection = TimePeriodCollection(big)
        collection.hasInsidePeriods(small).shouldBeTrue()
    }

    @Test
    fun `hasInsidePeriods returns false when no period fits inside target`() {
        val now = nowZonedDateTime()
        val target = TimeRange(now.plusDays(20), now.plusDays(25))
        val r = makeRange(0, 5)
        val collection = TimePeriodCollection(r)
        collection.hasInsidePeriods(target).shouldBeFalse()
    }

    @Test
    fun `hasOverlapPeriods returns true when overlap exists`() {
        val now = nowZonedDateTime()
        val r = TimeRange(now, now.plusDays(5))
        val target = TimeRange(now.plusDays(3), now.plusDays(8))
        val collection = TimePeriodCollection(r)
        collection.hasOverlapPeriods(target).shouldBeTrue()
    }

    @Test
    fun `hasOverlapPeriods returns false when no overlap`() {
        val now = nowZonedDateTime()
        val r = TimeRange(now, now.plusDays(3))
        val target = TimeRange(now.plusDays(10), now.plusDays(15))
        val collection = TimePeriodCollection(r)
        collection.hasOverlapPeriods(target).shouldBeFalse()
    }

    @Test
    fun `hasIntersectionPeriods with moment returns true when moment is inside a period`() {
        val now = nowZonedDateTime()
        val r = TimeRange(now, now.plusDays(5))
        val collection = TimePeriodCollection(r)
        collection.hasIntersectionPeriods(now.plusDays(2)).shouldBeTrue()
    }

    @Test
    fun `hasIntersectionPeriods with moment returns false when moment is outside all periods`() {
        val now = nowZonedDateTime()
        val r = TimeRange(now, now.plusDays(5))
        val collection = TimePeriodCollection(r)
        collection.hasIntersectionPeriods(now.plusDays(10)).shouldBeFalse()
    }

    @Test
    fun `insidePeriods returns matching periods`() {
        val now = nowZonedDateTime()
        // big contains small — big.hasInsideWith(small) means "small fits inside big"
        val big = TimeRange(now, now.plusDays(10))
        val small = TimeRange(now.plusDays(2), now.plusDays(4))
        val collection = TimePeriodCollection(big)
        val result = collection.insidePeriods(small)
        result.size shouldBeEqualTo 1
    }

    @Test
    fun `overlapPeriods returns overlapping periods`() {
        val now = nowZonedDateTime()
        val r1 = TimeRange(now, now.plusDays(5))
        val r2 = TimeRange(now.plusDays(10), now.plusDays(15))
        val target = TimeRange(now.plusDays(3), now.plusDays(12))
        val collection = TimePeriodCollection(r1, r2)
        val result = collection.overlapPeriods(target)
        result.size shouldBeEqualTo 2
    }

    @Test
    fun `relationPeriods filters by given relations`() {
        val now = nowZonedDateTime()
        val r1 = TimeRange(now.minusDays(5), now.minusDays(1))  // Before target
        val r2 = TimeRange(now.plusDays(10), now.plusDays(15))  // After target
        val collection = TimePeriodCollection(r1, r2)
        val target = TimeRange(now, now.plusDays(5))
        val result = collection.relationPeriods(target, PeriodRelation.Before, PeriodRelation.After)
        result.isNotEmpty().shouldBeTrue()
    }
}
