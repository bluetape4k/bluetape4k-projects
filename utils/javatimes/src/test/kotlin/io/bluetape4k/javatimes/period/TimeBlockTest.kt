package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.EmptyDuration
import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinDuration
import io.bluetape4k.javatimes.MinPeriodTime
import io.bluetape4k.javatimes.MinPositiveDuration
import io.bluetape4k.javatimes.durationOfHour
import io.bluetape4k.javatimes.hours
import io.bluetape4k.javatimes.millis
import io.bluetape4k.javatimes.nanos
import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.samples.TimeBlockPeriodRelationTestData
import io.bluetape4k.javatimes.seconds
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.amshove.kluent.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TimeBlockTest: AbstractPeriodTest() {

    companion object: KLogging()

    private val duration = 1.hours()
    private val offset = 1.seconds()

    private val start = nowZonedDateTime()
    private val end = start + duration

    private val testData = TimeBlockPeriodRelationTestData(start, end, offset)

    @Test
    fun `AnyTime variable`() {
        TimeBlock.AnyTime.start shouldBeEqualTo MinPeriodTime
        TimeBlock.AnyTime.end shouldBeEqualTo MaxPeriodTime

        TimeBlock.AnyTime.isAnyTime.shouldBeTrue()
        TimeBlock.AnyTime.readonly.shouldBeTrue()

        TimeBlock.AnyTime.hasPeriod.shouldBeFalse()
        TimeBlock.AnyTime.hasStart.shouldBeFalse()
        TimeBlock.AnyTime.hasEnd.shouldBeFalse()
        TimeBlock.AnyTime.isMoment.shouldBeFalse()
    }

    @Test
    fun `default constructor`() {
        val block = TimeBlock()

        block shouldNotBeEqualTo TimeBlock.AnyTime
        block.relationWith(TimeBlock.AnyTime) shouldBeEqualTo PeriodRelation.ExactMatch

        block.isAnyTime.shouldBeTrue()
        block.readonly.shouldBeFalse()

        block.hasPeriod.shouldBeFalse()
        block.hasStart.shouldBeFalse()
        block.hasEnd.shouldBeFalse()
        block.isMoment.shouldBeFalse()
    }

    @Test
    fun `construct with mement`() {
        val moment = nowZonedDateTime()
        val block = TimeBlock(moment)

        block.hasStart.shouldBeTrue()
        block.hasEnd.shouldBeTrue()
        block.duration shouldBeEqualTo EmptyDuration

        block.isAnyTime.shouldBeFalse()
        block.isMoment.shouldBeTrue()
        block.hasPeriod.shouldBeTrue()
    }

    @Test
    fun `construct with moment and duration`() {
        val block = TimeBlock(nowZonedDateTime(), MinPositiveDuration)

        block.isMoment.shouldBeFalse()
        block.duration shouldBeEqualTo MinPositiveDuration
    }

    @Test
    fun `construct with start only`() {
        // 현재부터 ~
        val block = TimeBlock(nowZonedDateTime(), null, false)

        block.hasStart.shouldBeTrue()
        block.hasEnd.shouldBeFalse()
    }

    @Test
    fun `construct with end only`() {
        // ~ 현재까지
        val block = TimeBlock(null, nowZonedDateTime(), false)

        block.hasStart.shouldBeFalse()
        block.hasEnd.shouldBeTrue()
    }

    @Test
    fun `construct with start and end`() {
        val block = TimeBlock(start, end)

        block.start shouldBeEqualTo start
        block.end shouldBeEqualTo end
        block.duration shouldBeEqualTo duration

        block.hasPeriod.shouldBeTrue()
        block.isAnyTime.shouldBeFalse()
        block.isMoment.shouldBeFalse()
        block.readonly.shouldBeFalse()
    }

    @Test
    fun `construct with reverse range`() {
        val block = TimeBlock(end, start)
        assertBlockCreator(block)
    }

    @Test
    fun `construct with start and duration`() {
        val block = TimeBlock(start, duration)
        assertBlockCreator(block)
    }

    private fun assertBlockCreator(block: TimeBlock) {
        block.start shouldBeEqualTo start
        block.end shouldBeEqualTo end
        block.duration shouldBeEqualTo duration

        block.hasPeriod.shouldBeTrue()
        block.isAnyTime.shouldBeFalse()
        block.isMoment.shouldBeFalse()
        block.readonly.shouldBeFalse()
    }

    @Test
    fun `construct with start and negate duration`() {
        val block = TimeBlock(start, duration.negated())

        block.start shouldBeEqualTo start - duration
        block.end shouldBeEqualTo end - duration
        block.duration shouldBeEqualTo duration
    }

    @Test
    fun `copy constructor`() {
        val source = TimeBlock(start, start + 1.hours(), true)
        val copied = TimeBlock(source)

        copied.start shouldBeEqualTo source.start
        copied.end shouldBeEqualTo source.end
        copied.duration shouldBeEqualTo source.duration
        copied.readonly shouldBeEqualTo source.readonly

        copied.hasPeriod.shouldBeTrue()
        copied.isAnyTime.shouldBeFalse()
        copied.isMoment.shouldBeFalse()
    }

    @Test
    fun `change start value`() {
        val block = TimeBlock(start, start + 1.hours())
        block.start shouldBeEqualTo start
        block.duration shouldBeEqualTo 1.hours()

        val changedStart = start + 1.hours()
        block.start = changedStart

        block.start shouldBeEqualTo changedStart
        block.end shouldBeEqualTo block.start
        block.duration shouldBeEqualTo EmptyDuration
    }

    @Test
    fun `change readonly block`() {
        assertFailsWith<IllegalStateException> {
            val block = TimeBlock(zonedDateTimeOf(), 1.hours(), true)
            block.start -= 1.hours()
        }
    }

    @Test
    fun `change end value`() {
        val block = TimeBlock(end - 1.hours(), end)

        block.end shouldBeEqualTo end

        val changedEnd = end + 1.hours()
        block.end = changedEnd
        block.end shouldBeEqualTo changedEnd
        block.start shouldBeEqualTo end - 1.hours()

        block.duration shouldBeEqualTo 2.hours()
    }

    @Test
    fun `change end with readonly is true`() {
        assertFailsWith<IllegalStateException> {
            val block = TimeBlock(nowZonedDateTime(), 1.hours(), true)
            block.end += 1.hours()
        }
    }

    @Test
    fun `change duration`() {
        val block = TimeBlock(start, duration)

        block.start shouldBeEqualTo start
        block.end shouldBeEqualTo end
        block.duration shouldBeEqualTo duration

        val delta = 1.hours()
        block.duration += delta

        block.start shouldBeEqualTo start
        block.end shouldBeEqualTo end + delta
        block.duration shouldBeEqualTo duration + delta

        block.duration = MinDuration

        block.start shouldBeEqualTo start
        block.end shouldBeEqualTo start
        block.duration shouldBeEqualTo MinDuration
    }

    @Test
    fun `set duration with out of range`() {
        assertFailsWith<AssertionError> {
            val block = TimeBlock(start, duration)
            block.duration = (-1).millis()
        }
    }

    @Test
    fun `set duration from start`() {
        val block = TimeBlock(start, duration)

        block.start shouldBeEqualTo start
        block.end shouldBeEqualTo end
        block.duration shouldBeEqualTo duration

        val delta = 1.hours()
        block.durationFromStart(duration + delta)

        block.start shouldBeEqualTo start
        block.end shouldBeEqualTo start + duration + delta
        block.duration shouldBeEqualTo duration + delta


        block.duration = MinDuration

        block.start shouldBeEqualTo start
        block.end shouldBeEqualTo start
        block.duration shouldBeEqualTo MinDuration
    }

    @Test
    fun `set duration from end`() {
        val block = TimeBlock(start, duration)

        val delta = 1.hours()
        block.durationFromEnd(duration + delta)

        block.start shouldBeEqualTo start - delta
        block.end shouldBeEqualTo end
        block.duration shouldBeEqualTo duration + delta

        block.duration = MinDuration

        block.start shouldBeEqualTo start - delta
        block.end shouldBeEqualTo start - delta
        block.duration shouldBeEqualTo MinDuration
    }

    @Test
    fun `hasInside with datetime`() {
        val block = TimeBlock(start, end)

        block.hasInsideWith(start - duration).shouldBeFalse()
        block.hasInsideWith(start).shouldBeTrue()
        block.hasInsideWith(start + duration).shouldBeTrue()

        block.hasInsideWith(end - duration).shouldBeTrue()
        block.hasInsideWith(end).shouldBeTrue()
        block.hasInsideWith(end + duration).shouldBeFalse()
    }

    @Test
    fun `hasInside with datetime before, after, inside`() {
        val block = TimeBlock(start, end)

        // before
        val before1 = TimeBlock(start - 2.nanos(), start - 1.nanos())
        val before2 = TimeBlock(start - 1.nanos(), end)
        val before3 = TimeBlock(start - 1.nanos(), start)

        block.hasInsideWith(before1).shouldBeFalse()
        block.hasInsideWith(before2).shouldBeFalse()
        block.hasInsideWith(before3).shouldBeFalse()

        // after
        val after1 = TimeBlock(start + 1.nanos(), end + 1.nanos())
        val after2 = TimeBlock(start, end + 1.nanos())
        val after3 = TimeBlock(end, end + 1.nanos())

        block.hasInsideWith(after1).shouldBeFalse()
        block.hasInsideWith(after2).shouldBeFalse()
        block.hasInsideWith(after3).shouldBeFalse()

        // inside
        block.hasInsideWith(block).shouldBeTrue()

        val inside1 = TimeBlock(start + 1.nanos(), end)
        val inside2 = TimeBlock(start + 1.nanos(), end - 1.nanos())
        val inside3 = TimeBlock(start, end - 1.nanos())

        block.hasInsideWith(inside1).shouldBeTrue()
        block.hasInsideWith(inside2).shouldBeTrue()
        block.hasInsideWith(inside3).shouldBeTrue()
    }

    @Test
    fun `copy TimeBlock`() {

        val source = TimeBlock(start, end)

        val noMove = source.copy(Duration.ZERO)
        noMove shouldBeEqualTo source

        val forwardOffset = durationOfHour(2, 30, 15)
        val forward = source.copy(forwardOffset)

        forward.start shouldBeEqualTo start + forwardOffset
        forward.end shouldBeEqualTo end + forwardOffset
        forward.duration shouldBeEqualTo duration

        val backwardOffset = durationOfHour(-1, -10, -30)
        val backward = source.copy(backwardOffset)

        backward.start shouldBeEqualTo start + backwardOffset
        backward.end shouldBeEqualTo end + backwardOffset
        backward.duration shouldBeEqualTo duration
    }

    @Test
    fun `move TimeBlock`() {
        val moveZero = TimeBlock(start, end)
        moveZero.move(Duration.ZERO)
        moveZero shouldBeEqualTo TimeBlock(start, end)

        val forward = TimeBlock(start, end)
        val forwardOffset = durationOfHour(2, 30, 15)
        forward.move(forwardOffset)

        forward.start shouldBeEqualTo start + forwardOffset
        forward.end shouldBeEqualTo end + forwardOffset
        forward.duration shouldBeEqualTo duration

        val backward = TimeBlock(start, end)
        val backwardOffset = durationOfHour(-1, -10, -30)
        backward.move(backwardOffset)

        backward.start shouldBeEqualTo start + backwardOffset
        backward.end shouldBeEqualTo end + backwardOffset
        backward.duration shouldBeEqualTo duration
    }

    @Test
    fun `is same period`() {
        val range1 = TimeBlock(start, end)
        val range2 = TimeBlock(start, end)

        range1.isSamePeriod(range1).shouldBeTrue()
        range2.isSamePeriod(range2).shouldBeTrue()

        range1.isSamePeriod(range2).shouldBeTrue()
        range2.isSamePeriod(range1).shouldBeTrue()

        range1.isSamePeriod(TimeBlock.AnyTime).shouldBeFalse()
        range2.isSamePeriod(TimeBlock.AnyTime).shouldBeFalse()

        range1.move(1.nanos())
        range1.isSamePeriod(range2).shouldBeFalse()
        range2.isSamePeriod(range1).shouldBeFalse()

        range1.move((-1).nanos())
        range1.isSamePeriod(range2).shouldBeTrue()
        range2.isSamePeriod(range1).shouldBeTrue()
    }

    @Test
    fun `hasInsideWith with all ZonedDateTime`() {
        with(testData) {
            assertFalse { reference hasInsideWith before }
            assertFalse { reference hasInsideWith startTouching }
            assertFalse { reference hasInsideWith startInside }
            assertFalse { reference hasInsideWith insideStartTouching }

            assertTrue { reference hasInsideWith enclosingStartTouching }
            assertTrue { reference hasInsideWith enclosing }
            assertTrue { reference hasInsideWith enclosingEndTouching }
            assertTrue { reference hasInsideWith exactMatch }

            assertFalse { reference hasInsideWith inside }
            assertFalse { reference hasInsideWith insideEndTouching }
            assertFalse { reference hasInsideWith endTouching }
            assertFalse { reference hasInsideWith after }
        }
    }

    @Test
    fun `intersectWith with ZonedDateTime`() {

        with(testData) {
            assertFalse { reference intersectWith before }
            assertTrue { reference intersectWith startTouching }
            assertTrue { reference intersectWith startInside }
            assertTrue { reference intersectWith insideStartTouching }

            assertTrue { reference intersectWith enclosingStartTouching }
            assertTrue { reference intersectWith enclosing }
            assertTrue { reference intersectWith enclosingEndTouching }
            assertTrue { reference intersectWith exactMatch }

            assertTrue { reference intersectWith inside }
            assertTrue { reference intersectWith insideEndTouching }
            assertTrue { reference intersectWith endTouching }
            assertFalse { reference intersectWith after }
        }
    }

    @Test
    fun `overlapWith with ZonedDateTime`() {

        with(testData) {
            assertFalse { reference overlapWith before }
            assertFalse { reference overlapWith startTouching }
            assertTrue { reference overlapWith startInside }
            assertTrue { reference overlapWith insideStartTouching }

            assertTrue { reference overlapWith enclosingStartTouching }
            assertTrue { reference overlapWith enclosing }
            assertTrue { reference overlapWith enclosingEndTouching }
            assertTrue { reference overlapWith exactMatch }

            assertTrue { reference overlapWith inside }
            assertTrue { reference overlapWith insideEndTouching }
            assertFalse { reference overlapWith endTouching }
            assertFalse { reference overlapWith after }
        }
    }

    @Test
    fun `intersectWith with various ZonedDateTime`() {
        val block = TimeBlock(start, end)

        // before
        assertFalse { block intersectWith TimeBlock(start - 2.hours(), start - 1.hours()) }
        assertTrue { block intersectWith TimeBlock(start - 1.hours(), start) }
        assertTrue { block intersectWith TimeBlock(start - 1.hours(), start + 1.nanos()) }

        // after
        assertFalse { block intersectWith TimeBlock(end + 1.hours(), end + 2.hours()) }
        assertTrue { block intersectWith TimeBlock(end, end + 1.nanos()) }
        assertTrue { block intersectWith TimeBlock(end - 1.nanos(), end + 1.nanos()) }

        // intersection
        assertTrue { block intersectWith block }
        assertTrue { block intersectWith TimeBlock(start + 1.nanos(), end + 1.nanos()) }
        assertTrue { block intersectWith TimeBlock(start - 1.nanos(), start + 1.nanos()) }
        assertTrue { block intersectWith TimeBlock(end - 1.nanos(), end + 1.nanos()) }
    }

    @Test
    fun `intersection with blocks`() {
        val block = TimeBlock(start, end)

        // before
        block.intersectBlock(TimeBlock(start - 2.nanos(), start - 1.nanos())).shouldBeNull()
        block.intersectBlock(TimeBlock(start - 1.nanos(), start)) shouldBeEqualTo TimeBlock(start)
        block.intersectBlock(TimeBlock(start - 2.nanos(), start + 1.nanos())) shouldBeEqualTo TimeBlock(
            start,
            start + 1.nanos()
        )

        // after
        block.intersectBlock(TimeBlock(end + 1.nanos(), end + 2.nanos())).shouldBeNull()
        block.intersectBlock(TimeBlock(end, end + 1.nanos())) shouldBeEqualTo TimeBlock(end)
        block.intersectBlock(TimeBlock(end - 1.nanos(), end + 1.nanos())) shouldBeEqualTo TimeBlock(
            end - 1.nanos(),
            end
        )


        // intersect
        block.intersectBlock(block) shouldBeEqualTo block
        block.intersectBlock(TimeBlock(start - 1.nanos(), end + 1.nanos())) shouldBeEqualTo block
        block.intersectBlock(TimeBlock(start + 1.nanos(), end - 1.nanos())) shouldBeEqualTo TimeBlock(
            start + 1.nanos(),
            end - 1.nanos()
        )
    }

    @Test
    fun `overlap with blocks`() {
        val block = TimeBlock(start, end)

        block.unionBlock(block) shouldBeEqualTo block
        block.unionBlock(TimeBlock(start - 1.nanos(), start)) shouldBeEqualTo TimeBlock(start - 1.nanos(), end)
        block.unionBlock(TimeBlock(start - 2.nanos(), start + 1.nanos())) shouldBeEqualTo TimeBlock(
            start - 2.nanos(),
            end
        )

        block.unionBlock(TimeBlock(end + 1.nanos(), end + 2.nanos())) shouldBeEqualTo TimeBlock(start, end + 2.nanos())
        block.unionBlock(TimeBlock(end, end + 1.nanos())) shouldBeEqualTo TimeBlock(start, end + 1.nanos())
        block.unionBlock(TimeBlock(end - 1.nanos(), end + 1.nanos())) shouldBeEqualTo TimeBlock(start, end + 1.nanos())

        block.unionBlock(block) shouldBeEqualTo block
        block.unionBlock(TimeBlock(start - 1.nanos(), end + 1.nanos())) shouldBeEqualTo TimeBlock(
            start - 1.nanos(),
            end + 1.nanos()
        )
        block.unionBlock(TimeBlock(start + 1.nanos(), end - 1.nanos())) shouldBeEqualTo block
    }

    @Test
    fun `get relation with two TimeBlock instances`() {
        with(testData) {
            reference relationWith before shouldBeEqualTo PeriodRelation.Before
            reference relationWith startTouching shouldBeEqualTo PeriodRelation.StartTouching
            reference relationWith startInside shouldBeEqualTo PeriodRelation.StartInside
            reference relationWith insideStartTouching shouldBeEqualTo PeriodRelation.InsideStartTouching
            reference relationWith enclosing shouldBeEqualTo PeriodRelation.Enclosing
            reference relationWith exactMatch shouldBeEqualTo PeriodRelation.ExactMatch
            reference relationWith inside shouldBeEqualTo PeriodRelation.Inside
            reference relationWith insideEndTouching shouldBeEqualTo PeriodRelation.InsideEndTouching
            reference relationWith endInside shouldBeEqualTo PeriodRelation.EndInside
            reference relationWith endTouching shouldBeEqualTo PeriodRelation.EndTouching
            reference relationWith after shouldBeEqualTo PeriodRelation.After

            // reference
            reference.start shouldBeEqualTo start
            reference.end shouldBeEqualTo end
            assertTrue { reference.readonly }

            // after
            assertTrue { after.readonly }
            assertTrue { after.start < start }
            assertTrue { after.end < start }

            assertFalse { reference.hasInsideWith(after.start) }
            assertFalse { reference.hasInsideWith(after.end) }

            // start touching
            assertTrue { startTouching.readonly }
            assertTrue { startTouching.start < start }
            assertTrue { startTouching.end == start }

            assertFalse { reference.hasInsideWith(startTouching.start) }
            assertTrue { reference.hasInsideWith(startTouching.end) }

            // start inside
            assertTrue { startInside.readonly }
            assertTrue { startInside.start < start }
            assertTrue { startInside.end < end }

            assertFalse { reference.hasInsideWith(startInside.start) }
            assertTrue { reference.hasInsideWith(startInside.end) }


            // inside start touching
            assertTrue { insideStartTouching.readonly }
            assertTrue { insideStartTouching.start == start }
            assertTrue { insideStartTouching.end > end }

            assertTrue { reference.hasInsideWith(insideStartTouching.start) }
            assertFalse { reference.hasInsideWith(insideStartTouching.end) }

            // enclosing start touching
            assertTrue { enclosingStartTouching.readonly }
            assertTrue { enclosingStartTouching.start == start }
            assertTrue { enclosingStartTouching.end < end }

            assertTrue { reference.hasInsideWith(enclosingStartTouching.start) }
            assertTrue { reference.hasInsideWith(enclosingStartTouching.end) }

            // enclosing
            assertTrue { enclosing.readonly }
            assertTrue { enclosing.start > start }
            assertTrue { enclosing.end < end }

            assertTrue { reference.hasInsideWith(enclosing.start) }
            assertTrue { reference.hasInsideWith(enclosing.end) }

            // enclosing end touching
            assertTrue { enclosingEndTouching.readonly }
            assertTrue { enclosingEndTouching.start > start }
            assertTrue { enclosingEndTouching.end == end }

            assertTrue { reference.hasInsideWith(enclosingEndTouching.start) }
            assertTrue { reference.hasInsideWith(enclosingEndTouching.end) }

            // exact match
            assertTrue { exactMatch.readonly }
            assertTrue { exactMatch.start == start }
            assertTrue { exactMatch.end == end }

            assertTrue { reference.hasInsideWith(exactMatch.start) }
            assertTrue { reference.hasInsideWith(exactMatch.end) }

            // inside
            assertTrue { inside.readonly }
            assertTrue { inside.start < start }
            assertTrue { inside.end > end }

            assertFalse { reference.hasInsideWith(inside.start) }
            assertFalse { reference.hasInsideWith(inside.end) }

            // inside end touching
            assertTrue { insideEndTouching.readonly }
            assertTrue { insideEndTouching.start < start }
            assertTrue { insideEndTouching.end == end }

            assertFalse { reference.hasInsideWith(insideEndTouching.start) }
            assertTrue { reference.hasInsideWith(insideEndTouching.end) }

            // end inside
            assertTrue { endInside.readonly }
            assertTrue { endInside.start in start..end }
            assertTrue { endInside.end > end }

            assertTrue { reference.hasInsideWith(endInside.start) }
            assertFalse { reference.hasInsideWith(endInside.end) }

            // end Touching
            assertTrue { endTouching.readonly }
            assertTrue { endTouching.start == end }
            assertTrue { endTouching.end > end }

            assertTrue { reference.hasInsideWith(endTouching.start) }
            assertFalse { reference.hasInsideWith(endTouching.end) }

            // before
            assertTrue { before.readonly }
            assertTrue { before.start > end }
            assertTrue { before.end > end }

            assertFalse { reference.hasInsideWith(before.start) }
            assertFalse { reference.hasInsideWith(before.end) }
        }
    }

    @Test
    fun `reset TimeBlock`() {
        val block = TimeBlock(start, end)

        block shouldBeEqualTo TimeBlock(start, end)

        block.reset()

        block.start shouldBeEqualTo MinPeriodTime
        block.end shouldBeEqualTo MaxPeriodTime
        block.hasStart.shouldBeFalse()
        block.hasEnd.shouldBeFalse()
        block.hasPeriod.shouldBeFalse()
        block.isMoment.shouldBeFalse()
    }

    @Test
    fun `equals two blocks`() {
        val block1 = TimeBlock(start, end)
        val block2 = TimeBlock(start, end)
        val block3 = TimeBlock(start + 1.nanos(), end + 1.nanos())
        val block4 = TimeBlock(start, end, true)

        block1 shouldBeEqualTo block2

        block1 shouldNotBeEqualTo block3
        block2 shouldNotBeEqualTo block3
        block1 shouldNotBeEqualTo block4
        block2 shouldNotBeEqualTo block4
    }
}
