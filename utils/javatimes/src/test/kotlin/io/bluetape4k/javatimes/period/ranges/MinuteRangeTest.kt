package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.MinNegativeDuration
import io.bluetape4k.javatimes.MinutesPerHour
import io.bluetape4k.javatimes.minutes
import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.startOfMinute
import io.bluetape4k.javatimes.todayZonedDateTime
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class MinuteRangeTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `init with now`() {
        val now = nowZonedDateTime()
        val firstMin = now.startOfMinute()
        val secondMin = firstMin + 1.minutes()

        val mr = MinuteRange(now, TimeCalendar.EmptyOffset)

        mr.start shouldBeEqualTo firstMin
        mr.unmappedEnd shouldBeEqualTo secondMin
    }

    @Test
    fun `default constructor`() {
        val today = todayZonedDateTime()

        repeat(MinutesPerHour) {
            val mr = MinuteRange(today + it.minutes())

            mr.start shouldBeEqualTo today + it.minutes()
            mr.unmappedEnd shouldBeEqualTo today.plusMinutes(it + 1L)
        }
    }

    @Test
    fun `construct with now`() {
        val now = nowZonedDateTime()
        val mr = MinuteRange(now)

        mr.start shouldBeEqualTo now.startOfMinute()
        mr.unmappedEnd shouldBeEqualTo now.startOfMinute() + 1.minutes()
    }

    @Test
    fun `add minutes`() {

        val mr = MinuteRange()

        mr.prevMinute() shouldBeEqualTo MinuteRange(mr.start - 1.minutes())
        mr.nextMinute() shouldBeEqualTo MinuteRange(mr.start + 1.minutes())

        mr.addMinutes(0) shouldBeEqualTo mr

        val prev = mr.prevMinute()
        prev.start shouldBeEqualTo mr.start - 1.minutes()
        prev.unmappedEnd shouldBeEqualTo mr.unmappedEnd - 1.minutes()

        val next = mr.nextMinute()
        next.start shouldBeEqualTo mr.start + 1.minutes()
        next.unmappedEnd shouldBeEqualTo mr.unmappedEnd + 1.minutes()
    }

    @Test
    fun `add various hours`() {
        val mr = MinuteRange()

        (-100..100).toList()
            .parallelStream()
            .forEach { m ->
                val next = mr.addMinutes(m)

                next.start shouldBeEqualTo mr.start + m.minutes()
                next.unmappedEnd shouldBeEqualTo mr.start.plusMinutes(m + 1L)
            }
    }

    @Test
    fun `minute sequence`() {
        val hourRange = HourRange()
        val minutes = hourRange.minuteSequence()

        minutes.count() shouldBeEqualTo MinutesPerHour

        minutes.forEachIndexed { i, mr ->
            mr.start shouldBeEqualTo hourRange.start + i.minutes()
            mr.end shouldBeEqualTo hourRange.start + (i + 1).minutes() + MinNegativeDuration

            mr.unmappedStart shouldBeEqualTo hourRange.start + i.minutes()
            mr.unmappedEnd shouldBeEqualTo hourRange.start + (i + 1).minutes()
        }
    }
}
