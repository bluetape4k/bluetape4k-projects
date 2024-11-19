package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.hours
import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.startOfHour
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class HourRangeCollectionTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `construct with single hour`() {
        val startTime = nowZonedDateTime()
        val hours = HourRangeCollection(startTime, 1, TimeCalendar.EmptyOffset)

        hours.hourCount shouldBeEqualTo 1
        hours.start shouldBeEqualTo startTime.startOfHour()

        val hseq = hours.hourSequence()
        hseq.count() shouldBeEqualTo 1
        hseq.first() shouldBeEqualTo HourRange(startTime, TimeCalendar.EmptyOffset)
    }

    @Test
    fun `calendar hours`() {
        val startTime = nowZonedDateTime()
        val startHour = startTime.startOfHour()

        val hourCount = 44
        val hours = HourRangeCollection(startTime, hourCount, TimeCalendar.EmptyOffset)

        hours.hourCount shouldBeEqualTo hourCount
        hours.start shouldBeEqualTo startTime.startOfHour()
        hours.end shouldBeEqualTo startHour + hourCount.hours()

        val hseq = hours.hourSequence()

        hseq.forEachIndexed { i, hr ->
            hr shouldBeEqualTo HourRange(startHour + i.hours(), TimeCalendar.EmptyOffset)
        }
    }

    @Test
    fun `hour sequence`() {
        val hourCounts = listOf(1, 24, 48, 64, 128)
        val now = nowZonedDateTime()

        hourCounts.parallelStream().forEach { hourCount ->
            val hours = HourRangeCollection(now, hourCount)
            val startHour = now.startOfHour() + hours.calendar.startOffset
            val endHour = now.startOfHour() + hourCount.hours() + hours.calendar.endOffset

            hours.start shouldBeEqualTo startHour
            hours.end shouldBeEqualTo endHour
            hours.hourCount shouldBeEqualTo hourCount

            val hseq = hours.hourSequence()
            hseq.count() shouldBeEqualTo hourCount

            hseq.forEachIndexed { i, hr ->
                hr.start shouldBeEqualTo startHour + i.hours()
                hr.end shouldBeEqualTo hours.calendar.mapEnd(startHour + (i + 1).hours())
                hr.unmappedEnd shouldBeEqualTo startHour.plusHours(i + 1L)
            }
        }
    }
}
