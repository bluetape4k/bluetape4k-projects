package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.HoursPerDay
import io.bluetape4k.javatimes.MinNegativeDuration
import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.hours
import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.startOfDay
import io.bluetape4k.javatimes.todayZonedDateTime
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class DayRangeCollectionTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `single day`() {
        val start = todayZonedDateTime()
        val days = DayRangeCollection(start, 1)

        days.dayCount shouldBeEqualTo 1
        days.start shouldBeEqualTo start
        days.end shouldBeEqualTo start.plusDays(1) + MinNegativeDuration

        val daySeq = days.daySequence()
        daySeq.count() shouldBeEqualTo 1
        daySeq.first() shouldBeEqualTo DayRange(start)
    }

    @Test
    fun `multiple days`() {
        val dayCount = 5
        val start = todayZonedDateTime()

        val days = DayRangeCollection(start, dayCount)

        days.start shouldBeEqualTo start
        days.end shouldBeEqualTo start + dayCount.days() + MinNegativeDuration

        val daySeq = days.daySequence()
        daySeq.count() shouldBeEqualTo dayCount

        daySeq.forEachIndexed { index, dr ->
            assertTrue { dr.isSamePeriod(DayRange(start + index.days())) }
        }
    }

    @Test
    fun `calendar hours`() {
        val dayCounts = listOf(1, 6, 48, 180, 480)

        dayCounts.parallelStream().forEach { dayCount ->

            val now = nowZonedDateTime()
            val days = DayRangeCollection(now, dayCount)

            val startTime = now.startOfDay() + days.calendar.startOffset
            val endTime = startTime + dayCount.days() + days.calendar.endOffset

            days.start shouldBeEqualTo startTime
            days.end shouldBeEqualTo endTime

            days.dayCount shouldBeEqualTo dayCount

            val hourSeq = days.hourSequence()

            hourSeq.count() shouldBeEqualTo dayCount * HoursPerDay
            hourSeq.forEachIndexed { i, hour ->
                hour.start shouldBeEqualTo startTime.plusHours(i.toLong())
                hour.end shouldBeEqualTo days.calendar.mapEnd(startTime.plusHours(i + 1L))
                hour.isSamePeriod(HourRange(days.start + i.hours())).shouldBeTrue()
            }
        }
    }
}
