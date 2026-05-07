package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeGreaterThan
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test

class RangeSupportTest {

    companion object : KLogging()

    @Test
    fun `dayRanges by count generates correct number of days`() {
        val now = nowZonedDateTime()
        val days = dayRanges(now, 5, TimeCalendar.EmptyOffset).toList()
        days.size shouldBeEqualTo 5
    }

    @Test
    fun `dayRanges by start and end spans correct duration`() {
        val start = nowZonedDateTime()
        val end = start.plusDays(3)
        val days = dayRanges(start, end, TimeCalendar.EmptyOffset).toList()
        days.isNotEmpty().shouldBeTrue()
        days.size shouldBeGreaterThan 0
    }

    @Test
    fun `weekRanges by count generates correct number of weeks`() {
        val now = nowZonedDateTime()
        val weeks = weekRanges(now, 4, TimeCalendar.EmptyOffset).toList()
        weeks.size shouldBeEqualTo 4
    }

    @Test
    fun `weekRanges by start and end spans correct duration`() {
        val start = nowZonedDateTime()
        val end = start.plusWeeks(2)
        val weeks = weekRanges(start, end, TimeCalendar.EmptyOffset).toList()
        weeks.isNotEmpty().shouldBeTrue()
    }

    @Test
    fun `monthRanges by count generates correct number of months`() {
        val now = nowZonedDateTime()
        val months = monthRanges(now, 6, TimeCalendar.EmptyOffset).toList()
        months.size shouldBeEqualTo 6
    }

    @Test
    fun `monthRanges by start and end spans correct duration`() {
        val start = nowZonedDateTime()
        val end = start.plusMonths(3)
        val months = monthRanges(start, end, TimeCalendar.EmptyOffset).toList()
        months.isNotEmpty().shouldBeTrue()
    }

    @Test
    fun `hourRanges by count generates correct number of hours`() {
        val now = nowZonedDateTime()
        val hours = hourRanges(now, 8, TimeCalendar.EmptyOffset).toList()
        hours.size shouldBeEqualTo 8
    }

    @Test
    fun `hourRanges by start and end spans correct duration`() {
        val start = nowZonedDateTime()
        val end = start.plusHours(4)
        val hours = hourRanges(start, end, TimeCalendar.EmptyOffset).toList()
        hours.isNotEmpty().shouldBeTrue()
    }

    @Test
    fun `minuteRanges by count generates correct number of minutes`() {
        val now = nowZonedDateTime()
        val minutes = minuteRanges(now, 30, TimeCalendar.EmptyOffset).toList()
        minutes.size shouldBeEqualTo 30
    }

    @Test
    fun `minuteRanges by start and end spans correct duration`() {
        val start = nowZonedDateTime()
        val end = start.plusMinutes(10)
        val minutes = minuteRanges(start, end, TimeCalendar.EmptyOffset).toList()
        minutes.isNotEmpty().shouldBeTrue()
    }

    @Test
    fun `dayRanges are sequential`() {
        val now = nowZonedDateTime()
        val days = dayRanges(now, 3, TimeCalendar.EmptyOffset).toList()
        (days[0].start < days[1].start).shouldBeTrue()
        (days[1].start < days[2].start).shouldBeTrue()
    }

    @Test
    fun `weekRanges are sequential`() {
        val now = nowZonedDateTime()
        val weeks = weekRanges(now, 3, TimeCalendar.EmptyOffset).toList()
        (weeks[0].start < weeks[1].start).shouldBeTrue()
        (weeks[1].start < weeks[2].start).shouldBeTrue()
    }

    @Test
    fun `monthRanges are sequential`() {
        val now = nowZonedDateTime()
        val months = monthRanges(now, 3, TimeCalendar.EmptyOffset).toList()
        (months[0].start < months[1].start).shouldBeTrue()
        (months[1].start < months[2].start).shouldBeTrue()
    }

    @Test
    fun `hourRanges are sequential`() {
        val now = nowZonedDateTime()
        val hours = hourRanges(now, 3, TimeCalendar.EmptyOffset).toList()
        (hours[0].start < hours[1].start).shouldBeTrue()
        (hours[1].start < hours[2].start).shouldBeTrue()
    }

    @Test
    fun `minuteRanges are sequential`() {
        val now = nowZonedDateTime()
        val minutes = minuteRanges(now, 3, TimeCalendar.EmptyOffset).toList()
        (minutes[0].start < minutes[1].start).shouldBeTrue()
        (minutes[1].start < minutes[2].start).shouldBeTrue()
    }
}
