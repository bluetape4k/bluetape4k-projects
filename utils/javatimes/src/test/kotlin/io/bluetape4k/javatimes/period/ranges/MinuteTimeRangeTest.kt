package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class MinuteTimeRangeTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `default constructor uses current time with minuteCount 1`() {
        val range = MinuteTimeRange()
        range.minuteCount shouldBeEqualTo 1
    }

    @Test
    fun `minuteCount property is correct`() {
        val range = MinuteTimeRange(now, minuteCount = 30)
        range.minuteCount shouldBeEqualTo 30
    }

    @Test
    fun `minuteOfHourOfEnd reflects end minute`() {
        val range = MinuteTimeRange(now, minuteCount = 1)
        range.minuteOfHourOfEnd shouldBeEqualTo range.end.minute
    }

    @Test
    fun `start is beginning of minute`() {
        val range = MinuteTimeRange(now, minuteCount = 1)
        range.start.second shouldBeEqualTo 0
    }

    @Test
    fun `end is start plus minuteCount minutes`() {
        val range = MinuteTimeRange(now, minuteCount = 30)
        // Default calendar applies -1ns endOffset so add 1ns to get true duration boundary
        val durationMinutes = java.time.Duration.between(range.start, range.end.plusNanos(1)).toMinutes()
        durationMinutes shouldBeEqualTo 30L
    }

    @Test
    fun `minuteCount 60 spans one hour`() {
        val range = MinuteTimeRange(now, minuteCount = 60)
        range.minuteCount shouldBeEqualTo 60
        // Default calendar applies -1ns endOffset so add 1ns to get true duration boundary
        val durationHours = java.time.Duration.between(range.start, range.end.plusNanos(1)).toHours()
        durationHours shouldBeEqualTo 1L
    }
}
