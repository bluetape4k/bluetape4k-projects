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
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeNull
import io.bluetape4k.assertions.shouldBeTrue
import io.bluetape4k.assertions.shouldNotBeEqualTo
import org.junit.jupiter.api.Test
import java.time.Duration
import io.bluetape4k.assertions.assertFailsWith

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
        assertFailsWith<IllegalArgumentException> {
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
            (reference hasInsideWith before).shouldBeFalse()
            (reference hasInsideWith startTouching).shouldBeFalse()
            (reference hasInsideWith startInside).shouldBeFalse()
            (reference hasInsideWith insideStartTouching).shouldBeFalse()

            (reference hasInsideWith enclosingStartTouching).shouldBeTrue()
            (reference hasInsideWith enclosing).shouldBeTrue()
            (reference hasInsideWith enclosingEndTouching).shouldBeTrue()
            (reference hasInsideWith exactMatch).shouldBeTrue()

            (reference hasInsideWith inside).shouldBeFalse()
            (reference hasInsideWith insideEndTouching).shouldBeFalse()
            (reference hasInsideWith endTouching).shouldBeFalse()
            (reference hasInsideWith after).shouldBeFalse()
        }
    }

    @Test
    fun `intersectWith with ZonedDateTime`() {

        with(testData) {
            (reference intersectWith before).shouldBeFalse()
            (reference intersectWith startTouching).shouldBeTrue()
            (reference intersectWith startInside).shouldBeTrue()
            (reference intersectWith insideStartTouching).shouldBeTrue()

            (reference intersectWith enclosingStartTouching).shouldBeTrue()
            (reference intersectWith enclosing).shouldBeTrue()
            (reference intersectWith enclosingEndTouching).shouldBeTrue()
            (reference intersectWith exactMatch).shouldBeTrue()

            (reference intersectWith inside).shouldBeTrue()
            (reference intersectWith insideEndTouching).shouldBeTrue()
            (reference intersectWith endTouching).shouldBeTrue()
            (reference intersectWith after).shouldBeFalse()
        }
    }

    @Test
    fun `overlapWith with ZonedDateTime`() {

        with(testData) {
            (reference overlapWith before).shouldBeFalse()
            (reference overlapWith startTouching).shouldBeFalse()
            (reference overlapWith startInside).shouldBeTrue()
            (reference overlapWith insideStartTouching).shouldBeTrue()

            (reference overlapWith enclosingStartTouching).shouldBeTrue()
            (reference overlapWith enclosing).shouldBeTrue()
            (reference overlapWith enclosingEndTouching).shouldBeTrue()
            (reference overlapWith exactMatch).shouldBeTrue()

            (reference overlapWith inside).shouldBeTrue()
            (reference overlapWith insideEndTouching).shouldBeTrue()
            (reference overlapWith endTouching).shouldBeFalse()
            (reference overlapWith after).shouldBeFalse()
        }
    }

    @Test
    fun `intersectWith with various ZonedDateTime`() {
        val block = TimeBlock(start, end)

        // before
        (block intersectWith TimeBlock(start - 2.hours(), start - 1.hours())).shouldBeFalse()
        (block intersectWith TimeBlock(start - 1.hours(), start)).shouldBeTrue()
        (block intersectWith TimeBlock(start - 1.hours(), start + 1.nanos())).shouldBeTrue()

        // after
        (block intersectWith TimeBlock(end + 1.hours(), end + 2.hours())).shouldBeFalse()
        (block intersectWith TimeBlock(end, end + 1.nanos())).shouldBeTrue()
        (block intersectWith TimeBlock(end - 1.nanos(), end + 1.nanos())).shouldBeTrue()

        // intersection
        (block intersectWith block).shouldBeTrue()
        (block intersectWith TimeBlock(start + 1.nanos(), end + 1.nanos())).shouldBeTrue()
        (block intersectWith TimeBlock(start - 1.nanos(), start + 1.nanos())).shouldBeTrue()
        (block intersectWith TimeBlock(end - 1.nanos(), end + 1.nanos())).shouldBeTrue()
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
            (reference.readonly).shouldBeTrue()

            // after
            (after.readonly).shouldBeTrue()
            (after.start < start).shouldBeTrue()
            (after.end < start).shouldBeTrue()

            (reference.hasInsideWith(after.start)).shouldBeFalse()
            (reference.hasInsideWith(after.end)).shouldBeFalse()

            // start touching
            (startTouching.readonly).shouldBeTrue()
            (startTouching.start < start).shouldBeTrue()
            (startTouching.end == start).shouldBeTrue()

            (reference.hasInsideWith(startTouching.start)).shouldBeFalse()
            (reference.hasInsideWith(startTouching.end)).shouldBeTrue()

            // start inside
            (startInside.readonly).shouldBeTrue()
            (startInside.start < start).shouldBeTrue()
            (startInside.end < end).shouldBeTrue()

            (reference.hasInsideWith(startInside.start)).shouldBeFalse()
            (reference.hasInsideWith(startInside.end)).shouldBeTrue()


            // inside start touching
            (insideStartTouching.readonly).shouldBeTrue()
            (insideStartTouching.start == start).shouldBeTrue()
            (insideStartTouching.end > end).shouldBeTrue()

            (reference.hasInsideWith(insideStartTouching.start)).shouldBeTrue()
            (reference.hasInsideWith(insideStartTouching.end)).shouldBeFalse()

            // enclosing start touching
            (enclosingStartTouching.readonly).shouldBeTrue()
            (enclosingStartTouching.start == start).shouldBeTrue()
            (enclosingStartTouching.end < end).shouldBeTrue()

            (reference.hasInsideWith(enclosingStartTouching.start)).shouldBeTrue()
            (reference.hasInsideWith(enclosingStartTouching.end)).shouldBeTrue()

            // enclosing
            (enclosing.readonly).shouldBeTrue()
            (enclosing.start > start).shouldBeTrue()
            (enclosing.end < end).shouldBeTrue()

            (reference.hasInsideWith(enclosing.start)).shouldBeTrue()
            (reference.hasInsideWith(enclosing.end)).shouldBeTrue()

            // enclosing end touching
            (enclosingEndTouching.readonly).shouldBeTrue()
            (enclosingEndTouching.start > start).shouldBeTrue()
            (enclosingEndTouching.end == end).shouldBeTrue()

            (reference.hasInsideWith(enclosingEndTouching.start)).shouldBeTrue()
            (reference.hasInsideWith(enclosingEndTouching.end)).shouldBeTrue()

            // exact match
            (exactMatch.readonly).shouldBeTrue()
            (exactMatch.start == start).shouldBeTrue()
            (exactMatch.end == end).shouldBeTrue()

            (reference.hasInsideWith(exactMatch.start)).shouldBeTrue()
            (reference.hasInsideWith(exactMatch.end)).shouldBeTrue()

            // inside
            (inside.readonly).shouldBeTrue()
            (inside.start < start).shouldBeTrue()
            (inside.end > end).shouldBeTrue()

            (reference.hasInsideWith(inside.start)).shouldBeFalse()
            (reference.hasInsideWith(inside.end)).shouldBeFalse()

            // inside end touching
            (insideEndTouching.readonly).shouldBeTrue()
            (insideEndTouching.start < start).shouldBeTrue()
            (insideEndTouching.end == end).shouldBeTrue()

            (reference.hasInsideWith(insideEndTouching.start)).shouldBeFalse()
            (reference.hasInsideWith(insideEndTouching.end)).shouldBeTrue()

            // end inside
            (endInside.readonly).shouldBeTrue()
            (endInside.start in start..end).shouldBeTrue()
            (endInside.end > end).shouldBeTrue()

            (reference.hasInsideWith(endInside.start)).shouldBeTrue()
            (reference.hasInsideWith(endInside.end)).shouldBeFalse()

            // end Touching
            (endTouching.readonly).shouldBeTrue()
            (endTouching.start == end).shouldBeTrue()
            (endTouching.end > end).shouldBeTrue()

            (reference.hasInsideWith(endTouching.start)).shouldBeTrue()
            (reference.hasInsideWith(endTouching.end)).shouldBeFalse()

            // before
            (before.readonly).shouldBeTrue()
            (before.start > end).shouldBeTrue()
            (before.end > end).shouldBeTrue()

            (reference.hasInsideWith(before.start)).shouldBeFalse()
            (reference.hasInsideWith(before.end)).shouldBeFalse()
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
