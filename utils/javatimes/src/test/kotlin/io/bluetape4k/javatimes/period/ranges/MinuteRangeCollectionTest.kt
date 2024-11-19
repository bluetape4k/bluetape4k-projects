package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.minutes
import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.startOfMinute
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class MinuteRangeCollectionTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `single minute`() {
        val now = nowZonedDateTime()
        val minutes = MinuteRangeCollection(now, 1, TimeCalendar.EmptyOffset)

        val startTime = now.startOfMinute()
        val endTime = startTime + 1.minutes()

        minutes.minuteCount shouldBeEqualTo 1
        minutes.start shouldBeEqualTo startTime
        minutes.end shouldBeEqualTo endTime

        val minSeq = minutes.minuteSequence()
        minSeq.count() shouldBeEqualTo 1
        minSeq.first() shouldBeEqualTo MinuteRange(startTime, TimeCalendar.EmptyOffset)
    }

    @Test
    fun `various minute count`() {
        val minCounts = listOf(1, 24, 48, 64, 128)
        val now = nowZonedDateTime()

        minCounts.parallelStream().forEach { minCount ->
            val minutes = MinuteRangeCollection(now, minCount)

            val startTime = now.startOfMinute()
            val endTime = startTime + minCount.minutes() + minutes.calendar.endOffset

            minutes.minuteCount shouldBeEqualTo minCount
            minutes.start shouldBeEqualTo startTime
            minutes.end shouldBeEqualTo endTime

            val minSeq = minutes.minuteSequence()

            minSeq.count() shouldBeEqualTo minCount

            minSeq.forEachIndexed { index, mr ->
                mr.start shouldBeEqualTo startTime + index.minutes()
                mr.unmappedStart shouldBeEqualTo startTime + index.minutes()
                mr.end shouldBeEqualTo startTime + (index + 1).minutes() + minutes.calendar.endOffset
                mr.unmappedEnd shouldBeEqualTo startTime + (index + 1).minutes()
            }
        }
    }
}
