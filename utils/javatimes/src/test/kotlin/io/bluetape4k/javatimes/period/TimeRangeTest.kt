package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinPeriodTime
import io.bluetape4k.javatimes.hours
import io.bluetape4k.javatimes.minutes
import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeGreaterThan
import org.amshove.kluent.shouldBeLessThan
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration

/**
 * [TimeRange]에 대한 테스트
 */
class TimeRangeTest {

    companion object: KLogging()

    @Test
    fun `TimeRange 기본 생성`() {
        val range = TimeRange()

        range.start shouldBeEqualTo MinPeriodTime
        range.end shouldBeEqualTo MaxPeriodTime
        range.readonly.shouldBeFalse()
    }

    @Test
    fun `TimeRange moment 생성`() {
        val moment = nowZonedDateTime()
        val range = TimeRange(moment)

        range.start shouldBeEqualTo moment
        range.end shouldBeEqualTo moment
        range.isMoment.shouldBeTrue()
    }

    @Test
    fun `TimeRange start와 end로 생성`() {
        val start = nowZonedDateTime()
        val end = start + 1.hours()

        val range = TimeRange(start, end)

        range.start shouldBeEqualTo start
        range.end shouldBeEqualTo end
        range.hasStart.shouldBeTrue()
        range.hasEnd.shouldBeTrue()
        range.hasPeriod.shouldBeTrue()
    }

    @Test
    fun `TimeRange duration으로 생성 - start 기준`() {
        val start = nowZonedDateTime()
        val duration = 2.hours()

        val range = TimeRange(start, duration)

        range.start shouldBeEqualTo start
        range.end shouldBeEqualTo start + duration
        range.duration shouldBeEqualTo duration
    }

    @Test
    fun `TimeRange duration으로 생성 - end 기준`() {
        val end = nowZonedDateTime()
        val duration = 2.hours()

        val range = TimeRange(duration, end)

        range.start shouldBeEqualTo end - duration
        range.end shouldBeEqualTo end
        range.duration shouldBeEqualTo duration
    }

    @Test
    fun `TimeRange AnyTime은 readonly`() {
        val anyTime = TimeRange.AnyTime

        anyTime.isAnyTime.shouldBeTrue()
        anyTime.readonly.shouldBeTrue()
        anyTime.hasStart.shouldBeFalse()
        anyTime.hasEnd.shouldBeFalse()
        anyTime.hasPeriod.shouldBeFalse()
    }

    @Test
    fun `TimeRange copy 메서드`() {
        val start = nowZonedDateTime()
        val end = start + 1.hours()
        val range = TimeRange(start, end)

        val offset = 30.minutes()
        val copied = range.copy(offset)

        copied.start shouldBeEqualTo start + offset
        copied.end shouldBeEqualTo end + offset
        copied.duration shouldBeEqualTo range.duration
    }

    @Test
    fun `TimeRange copy with zero offset`() {
        val start = nowZonedDateTime()
        val end = start + 1.hours()
        val range = TimeRange(start, end)

        val copied = range.copy(Duration.ZERO)

        copied.start shouldBeEqualTo start
        copied.end shouldBeEqualTo end
    }

    @Test
    fun `TimeRange move 메서드`() {
        val start = nowZonedDateTime()
        val end = start + 1.hours()
        val range = TimeRange(start, end, readonly = false)

        val offset = 30.minutes()
        range.move(offset)

        range.start shouldBeEqualTo start + offset
        range.end shouldBeEqualTo end + offset
    }

    @Test
    fun `TimeRange move with zero offset`() {
        val start = nowZonedDateTime()
        val end = start + 1.hours()
        val range = TimeRange(start, end, readonly = false)

        val originalStart = range.start
        val originalEnd = range.end

        range.move(Duration.ZERO)

        range.start shouldBeEqualTo originalStart
        range.end shouldBeEqualTo originalEnd
    }

    @Test
    fun `readonly TimeRange는 move 불가`() {
        val range = TimeRange(nowZonedDateTime(), nowZonedDateTime() + 1.hours(), readonly = true)

        assertThrows<IllegalStateException> {
            range.move(30.minutes())
        }
    }

    @Test
    fun `TimeRange expandStartTo`() {
        val start = nowZonedDateTime()
        val end = start + 2.hours()
        val range = TimeRange(start, end, readonly = false)

        val newStart = start - 1.hours()
        range.expandStartTo(newStart)

        range.start shouldBeEqualTo newStart
        range.end shouldBeEqualTo end
    }

    @Test
    fun `TimeRange expandStartTo - start보다 큰 시각은 무시`() {
        val start = nowZonedDateTime()
        val end = start + 2.hours()
        val range = TimeRange(start, end, readonly = false)

        val futureTime = start + 30.minutes()
        range.expandStartTo(futureTime)

        range.start shouldBeEqualTo start // 변경되지 않음
    }

    @Test
    fun `TimeRange expandEndTo`() {
        val start = nowZonedDateTime()
        val end = start + 2.hours()
        val range = TimeRange(start, end, readonly = false)

        val newEnd = end + 1.hours()
        range.expandEndTo(newEnd)

        range.start shouldBeEqualTo start
        range.end shouldBeEqualTo newEnd
    }

    @Test
    fun `TimeRange expandEndTo - end보다 작은 시각은 무시`() {
        val start = nowZonedDateTime()
        val end = start + 2.hours()
        val range = TimeRange(start, end, readonly = false)

        val pastTime = end - 30.minutes()
        range.expandEndTo(pastTime)

        range.end shouldBeEqualTo end // 변경되지 않음
    }

    @Test
    fun `TimeRange shrinkStartTo`() {
        val start = nowZonedDateTime()
        val end = start + 2.hours()
        val range = TimeRange(start, end, readonly = false)

        val newStart = start + 30.minutes()
        range.shrinkStartTo(newStart)

        range.start shouldBeEqualTo newStart
        range.end shouldBeEqualTo end
    }

    @Test
    fun `TimeRange shrinkStartTo - 범위 밖의 시각은 무시`() {
        val start = nowZonedDateTime()
        val end = start + 2.hours()
        val range = TimeRange(start, end, readonly = false)

        val outsideTime = end + 1.hours()
        range.shrinkStartTo(outsideTime)

        range.start shouldBeEqualTo start // 변경되지 않음
    }

    @Test
    fun `TimeRange shrinkEndTo`() {
        val start = nowZonedDateTime()
        val end = start + 2.hours()
        val range = TimeRange(start, end, readonly = false)

        val newEnd = end - 30.minutes()
        range.shrinkEndTo(newEnd)

        range.start shouldBeEqualTo start
        range.end shouldBeEqualTo newEnd
    }

    @Test
    fun `TimeRange shrinkEndTo - 범위 밖의 시각은 무시`() {
        val start = nowZonedDateTime()
        val end = start + 2.hours()
        val range = TimeRange(start, end, readonly = false)

        val outsideTime = start - 1.hours()
        range.shrinkEndTo(outsideTime)

        range.end shouldBeEqualTo end // 변경되지 않음
    }

    @Test
    fun `TimeRange isSamePeriod`() {
        val start = nowZonedDateTime()
        val end = start + 1.hours()

        val range1 = TimeRange(start, end)
        val range2 = TimeRange(start, end)

        range1.isSamePeriod(range2).shouldBeTrue()
    }

    @Test
    fun `TimeRange isSamePeriod - 다른 기간`() {
        val start = nowZonedDateTime()

        val range1 = TimeRange(start, start + 1.hours())
        val range2 = TimeRange(start, start + 2.hours())

        range1.isSamePeriod(range2).shouldBeFalse()
    }

    @Test
    fun `TimeRange hasInside`() {
        val start = nowZonedDateTime()
        val end = start + 2.hours()
        val range = TimeRange(start, end)

        val inside = start + 1.hours()
        range.hasInsideWith(inside).shouldBeTrue()
    }

    @Test
    fun `TimeRange overlaps`() {
        val start1 = nowZonedDateTime()
        val range1 = TimeRange(start1, start1 + 2.hours())

        val start2 = start1 + 1.hours()
        val range2 = TimeRange(start2, start2 + 2.hours())

        range1.overlapWith(range2).shouldBeTrue()
    }

    @Test
    fun `readonly TimeRange 확인`() {
        val range = TimeRange(nowZonedDateTime(), nowZonedDateTime() + 1.hours(), readonly = true)

        range.readonly.shouldBeTrue()

        assertThrows<IllegalStateException> {
            range.start = nowZonedDateTime()
        }

        assertThrows<IllegalStateException> {
            range.end = nowZonedDateTime()
        }
    }

    @Test
    fun `TimeRange compareTo - start 기준`() {
        val start1 = nowZonedDateTime()
        val start2 = start1 + 1.hours()

        val range1 = TimeRange(start1, start1 + 2.hours())
        val range2 = TimeRange(start2, start2 + 2.hours())

        range1.compareTo(range2) shouldBeLessThan 0
        range2.compareTo(range1) shouldBeGreaterThan 0
    }

    @Test
    fun `TimeRange compareTo - start가 같으면 end 기준`() {
        val start = nowZonedDateTime()

        val range1 = TimeRange(start, start + 1.hours())
        val range2 = TimeRange(start, start + 2.hours())

        range1.compareTo(range2) shouldBeLessThan 0
        range2.compareTo(range1) shouldBeGreaterThan 0
    }

    @Test
    fun `TimeRange compareTo - 완전히 같은 경우`() {
        val start = nowZonedDateTime()
        val end = start + 1.hours()

        val range1 = TimeRange(start, end)
        val range2 = TimeRange(start, end)

        range1.compareTo(range2) shouldBeEqualTo 0
    }

    @Test
    fun `ITimePeriod로부터 TimeRange 생성`() {
        val start = nowZonedDateTime()
        val end = start + 1.hours()
        val period = TimePeriod(start, end)

        val range = TimeRange(period)

        range.start shouldBeEqualTo period.start
        range.end shouldBeEqualTo period.end
        range.readonly shouldBeEqualTo period.readonly
    }

    @Test
    fun `TimeRange duration 속성`() {
        val start = nowZonedDateTime()
        val duration = 3.hours()
        val range = TimeRange(start, start + duration)

        range.duration shouldBeEqualTo duration
    }

    @Test
    fun `TimeRange with null start and end`() {
        val range = TimeRange(null, null)

        range.start shouldBeEqualTo MinPeriodTime
        range.end shouldBeEqualTo MaxPeriodTime
        range.isAnyTime.shouldBeTrue()
    }
}
