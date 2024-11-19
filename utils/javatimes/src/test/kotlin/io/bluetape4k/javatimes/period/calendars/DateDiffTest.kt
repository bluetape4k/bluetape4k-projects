package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.javatimes.HoursPerDay
import io.bluetape4k.javatimes.MinutesPerHour
import io.bluetape4k.javatimes.MonthsPerQuarter
import io.bluetape4k.javatimes.MonthsPerYear
import io.bluetape4k.javatimes.QuartersPerYear
import io.bluetape4k.javatimes.SecondsPerMinute
import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.hours
import io.bluetape4k.javatimes.minutes
import io.bluetape4k.javatimes.monthPeriod
import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.quarterPeriod
import io.bluetape4k.javatimes.seconds
import io.bluetape4k.javatimes.weekPeriod
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.Duration

class DateDiffTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `empty duration`() {
        val now = nowZonedDateTime()
        val dateDiff = DateDiff(now, now)

        dateDiff.isEmpty.shouldBeTrue()
        dateDiff.difference shouldBeEqualTo Duration.ZERO

        with(dateDiff) {
            years shouldBeEqualTo 0
            quarters shouldBeEqualTo 0
            months shouldBeEqualTo 0
            weeks shouldBeEqualTo 0
            days shouldBeEqualTo 0
            hours shouldBeEqualTo 0
            minutes shouldBeEqualTo 0
            seconds shouldBeEqualTo 0

            elapsedYears shouldBeEqualTo 0
            elapsedQuarters shouldBeEqualTo 0
            elapsedMonths shouldBeEqualTo 0
            elapsedDays shouldBeEqualTo 0
            elapsedHours shouldBeEqualTo 0
            elapsedMinutes shouldBeEqualTo 0
            elapsedSeconds shouldBeEqualTo 0
        }
    }

    @Test
    fun `different two instant`() {
        val date1 = zonedDateTimeOf(2008, 10, 12, 15, 32, 44, 234)
        val date2 = zonedDateTimeOf(2010, 1, 3, 23, 22, 9, 345)

        val dateDiff = DateDiff(date1, date2)

        dateDiff.difference shouldBeEqualTo Duration.between(date1, date2)
        log.debug { "difference=${dateDiff.difference}" }
    }

    @Test
    fun `datediff - years`() {
        val years = intArrayOf(1, 3, 15)

        years.forEach { year ->
            val date1 = nowZonedDateTime()
            val date2 = date1.plusYears(year.toLong())
            val date3 = date1.minusYears(year.toLong())

            log.debug { "date1=$date1, date2=$date2, date3=$date3" }

            val dateDiff12 = DateDiff(date1, date2)

            with(dateDiff12) {
                elapsedYears shouldBeEqualTo year.toLong()
                elapsedMonths shouldBeEqualTo 0
                elapsedDays shouldBeEqualTo 0
                elapsedHours shouldBeEqualTo 0
                elapsedMinutes shouldBeEqualTo 0
                elapsedSeconds shouldBeEqualTo 0
            }

            dateDiff12.years shouldBeEqualTo year.toLong()
            dateDiff12.quarters shouldBeEqualTo year.toLong() * QuartersPerYear
            dateDiff12.months shouldBeEqualTo year.toLong() * MonthsPerYear

            val date12Days = Duration.between(date1, date2).toDays()

            dateDiff12.days shouldBeEqualTo date12Days
            dateDiff12.hours shouldBeEqualTo date12Days * HoursPerDay
            dateDiff12.minutes shouldBeEqualTo date12Days * HoursPerDay * MinutesPerHour
            dateDiff12.seconds shouldBeEqualTo date12Days * HoursPerDay * MinutesPerHour * SecondsPerMinute

            val dateDiff13 = DateDiff(date1, date3)

            dateDiff13.elapsedYears shouldBeEqualTo -year.toLong()
            dateDiff13.elapsedMonths shouldBeEqualTo 0
            dateDiff13.elapsedDays shouldBeEqualTo 0
            dateDiff13.elapsedHours shouldBeEqualTo 0
            dateDiff13.elapsedMinutes shouldBeEqualTo 0
            dateDiff13.elapsedSeconds shouldBeEqualTo 0

            dateDiff13.years shouldBeEqualTo -year.toLong()
            dateDiff13.quarters shouldBeEqualTo -year.toLong() * QuartersPerYear
            dateDiff13.months shouldBeEqualTo -year.toLong() * MonthsPerYear

            val date13Days = Duration.between(date1, date3).toDays()

            dateDiff13.days shouldBeEqualTo date13Days
            dateDiff13.hours shouldBeEqualTo date13Days * HoursPerDay
            dateDiff13.minutes shouldBeEqualTo date13Days * HoursPerDay * MinutesPerHour
            dateDiff13.seconds shouldBeEqualTo date13Days * HoursPerDay * MinutesPerHour * SecondsPerMinute
        }
    }


    @Test
    fun `datediff - quarter`() {
        val date1 = zonedDateTimeOf(2011, 5, 15, 15, 32, 44, 245)
        val date2 = date1 + 1.quarterPeriod()
        val date3 = date1 - 1.quarterPeriod()

        val dateDiff12 = DateDiff(date1, date2)
        val days12 = Duration.between(date1, date2).toDays()

        dateDiff12.elapsedYears shouldBeEqualTo 0
        dateDiff12.elapsedMonths shouldBeEqualTo MonthsPerQuarter.toLong()
        dateDiff12.elapsedDays shouldBeEqualTo 0
        dateDiff12.elapsedHours shouldBeEqualTo 0
        dateDiff12.elapsedMinutes shouldBeEqualTo 0
        dateDiff12.elapsedSeconds shouldBeEqualTo 0

        dateDiff12.years shouldBeEqualTo 0
        dateDiff12.quarters shouldBeEqualTo 1
        dateDiff12.months shouldBeEqualTo MonthsPerQuarter.toLong()
        dateDiff12.weeks shouldBeEqualTo 14
        dateDiff12.days shouldBeEqualTo days12
        dateDiff12.hours shouldBeEqualTo days12 * HoursPerDay
        dateDiff12.minutes shouldBeEqualTo days12 * HoursPerDay * MinutesPerHour
        dateDiff12.seconds shouldBeEqualTo days12 * HoursPerDay * MinutesPerHour * SecondsPerMinute

        val dateDiff13 = DateDiff(date1, date3)
        val days13 = Duration.between(date1, date3).toDays()

        dateDiff13.elapsedYears shouldBeEqualTo 0
        dateDiff13.elapsedMonths shouldBeEqualTo -MonthsPerQuarter.toLong()
        dateDiff13.elapsedDays shouldBeEqualTo 0
        dateDiff13.elapsedHours shouldBeEqualTo 0
        dateDiff13.elapsedMinutes shouldBeEqualTo 0
        dateDiff13.elapsedSeconds shouldBeEqualTo 0

        dateDiff13.years shouldBeEqualTo 0
        dateDiff13.quarters shouldBeEqualTo -1
        dateDiff13.months shouldBeEqualTo -MonthsPerQuarter.toLong()
        dateDiff13.weeks shouldBeEqualTo -12
        dateDiff13.days shouldBeEqualTo days13
        dateDiff13.hours shouldBeEqualTo days13 * HoursPerDay
        dateDiff13.minutes shouldBeEqualTo days13 * HoursPerDay * MinutesPerHour
        dateDiff13.seconds shouldBeEqualTo days13 * HoursPerDay * MinutesPerHour * SecondsPerMinute
    }

    @Test
    fun `datediff - months`() {
        val date1 = zonedDateTimeOf(2011, 5, 15, 15, 32, 44, 245)
        val date2 = date1 + 1.monthPeriod()
        val date3 = date1 - 1.monthPeriod()

        val dateDiff12 = DateDiff(date1, date2)
        val days12 = Duration.between(date1, date2).toDays()

        dateDiff12.elapsedYears shouldBeEqualTo 0
        dateDiff12.elapsedMonths shouldBeEqualTo 1
        dateDiff12.elapsedDays shouldBeEqualTo 0
        dateDiff12.elapsedHours shouldBeEqualTo 0
        dateDiff12.elapsedMinutes shouldBeEqualTo 0
        dateDiff12.elapsedSeconds shouldBeEqualTo 0

        dateDiff12.years shouldBeEqualTo 0
        dateDiff12.quarters shouldBeEqualTo 0
        dateDiff12.months shouldBeEqualTo 1
        dateDiff12.weeks shouldBeEqualTo 5
        dateDiff12.days shouldBeEqualTo days12
        dateDiff12.hours shouldBeEqualTo days12 * HoursPerDay
        dateDiff12.minutes shouldBeEqualTo days12 * HoursPerDay * MinutesPerHour
        dateDiff12.seconds shouldBeEqualTo days12 * HoursPerDay * MinutesPerHour * SecondsPerMinute

        val dateDiff13 = DateDiff(date1, date3)
        val days13 = Duration.between(date1, date3).toDays()

        dateDiff13.elapsedYears shouldBeEqualTo 0
        dateDiff13.elapsedMonths shouldBeEqualTo -1
        dateDiff13.elapsedDays shouldBeEqualTo 0
        dateDiff13.elapsedHours shouldBeEqualTo 0
        dateDiff13.elapsedMinutes shouldBeEqualTo 0
        dateDiff13.elapsedSeconds shouldBeEqualTo 0

        dateDiff13.years shouldBeEqualTo 0
        dateDiff13.quarters shouldBeEqualTo 0
        dateDiff13.months shouldBeEqualTo -1
        dateDiff13.weeks shouldBeEqualTo -4
        dateDiff13.days shouldBeEqualTo days13
        dateDiff13.hours shouldBeEqualTo days13 * HoursPerDay
        dateDiff13.minutes shouldBeEqualTo days13 * HoursPerDay * MinutesPerHour
        dateDiff13.seconds shouldBeEqualTo days13 * HoursPerDay * MinutesPerHour * SecondsPerMinute
    }

    @Test
    fun `datediff - weeks`() {
        val date1 = zonedDateTimeOf(2011, 5, 15, 15, 32, 44, 245)
        val date2 = date1 + 1.weekPeriod()
        val date3 = date1 - 1.weekPeriod()

        val dateDiff12 = DateDiff(date1, date2)
        val days12 = Duration.between(date1, date2).toDays()

        dateDiff12.years shouldBeEqualTo 0
        dateDiff12.quarters shouldBeEqualTo 0
        dateDiff12.months shouldBeEqualTo 0
        dateDiff12.weeks shouldBeEqualTo 1
        dateDiff12.days shouldBeEqualTo days12
        dateDiff12.hours shouldBeEqualTo days12 * HoursPerDay
        dateDiff12.minutes shouldBeEqualTo days12 * HoursPerDay * MinutesPerHour
        dateDiff12.seconds shouldBeEqualTo days12 * HoursPerDay * MinutesPerHour * SecondsPerMinute

        val dateDiff13 = DateDiff(date1, date3)
        val days13 = Duration.between(date1, date3).toDays()

        dateDiff13.years shouldBeEqualTo 0
        dateDiff13.quarters shouldBeEqualTo 0
        dateDiff13.months shouldBeEqualTo 0
        dateDiff13.weeks shouldBeEqualTo -1
        dateDiff13.days shouldBeEqualTo days13
        dateDiff13.hours shouldBeEqualTo days13 * HoursPerDay
        dateDiff13.minutes shouldBeEqualTo days13 * HoursPerDay * MinutesPerHour
        dateDiff13.seconds shouldBeEqualTo days13 * HoursPerDay * MinutesPerHour * SecondsPerMinute
    }

    @Test
    fun `datediff - days`() {

        val days = listOf(1, 3)

        days.forEach { day ->

            val date1 = zonedDateTimeOf(2011, 5, 19, 15, 32, 44, 245)
            val date2 = date1 + day.days()
            val date3 = date1 - day.days()

            val dateDiff12 = DateDiff(date1, date2)
            val days12 = Duration.between(date1, date2).toDays()

            dateDiff12.elapsedYears shouldBeEqualTo 0
            dateDiff12.elapsedMonths shouldBeEqualTo 0
            dateDiff12.elapsedDays shouldBeEqualTo day.toLong()
            dateDiff12.elapsedHours shouldBeEqualTo 0
            dateDiff12.elapsedMinutes shouldBeEqualTo 0
            dateDiff12.elapsedSeconds shouldBeEqualTo 0

            dateDiff12.years shouldBeEqualTo 0
            dateDiff12.quarters shouldBeEqualTo 0
            dateDiff12.months shouldBeEqualTo 0
            dateDiff12.weeks shouldBeEqualTo 0
            dateDiff12.days shouldBeEqualTo days12
            dateDiff12.hours shouldBeEqualTo days12 * HoursPerDay
            dateDiff12.minutes shouldBeEqualTo days12 * HoursPerDay * MinutesPerHour
            dateDiff12.seconds shouldBeEqualTo days12 * HoursPerDay * MinutesPerHour * SecondsPerMinute

            val dateDiff13 = DateDiff(date1, date3)
            val days13 = Duration.between(date1, date3).toDays()

            dateDiff13.elapsedYears shouldBeEqualTo 0
            dateDiff13.elapsedMonths shouldBeEqualTo 0
            dateDiff13.elapsedDays shouldBeEqualTo -day.toLong()
            dateDiff13.elapsedHours shouldBeEqualTo 0
            dateDiff13.elapsedMinutes shouldBeEqualTo 0
            dateDiff13.elapsedSeconds shouldBeEqualTo 0

            dateDiff13.years shouldBeEqualTo 0
            dateDiff13.quarters shouldBeEqualTo 0
            dateDiff13.months shouldBeEqualTo 0
            dateDiff13.weeks shouldBeEqualTo 0
            dateDiff13.days shouldBeEqualTo days13
            dateDiff13.hours shouldBeEqualTo days13 * HoursPerDay
            dateDiff13.minutes shouldBeEqualTo days13 * HoursPerDay * MinutesPerHour
            dateDiff13.seconds shouldBeEqualTo days13 * HoursPerDay * MinutesPerHour * SecondsPerMinute
        }
    }

    @Test
    fun `datediff - hours`() {

        val hours = listOf(1, 3, 5)

        hours.forEach { hour ->

            val date1 = zonedDateTimeOf(2011, 5, 19, 15, 32, 44, 245)
            val date2 = date1 + hour.hours()
            val date3 = date1 - hour.hours()

            val dateDiff12 = DateDiff(date1, date2)
            val hours12 = Duration.between(date1, date2).toHours()

            dateDiff12.elapsedYears shouldBeEqualTo 0
            dateDiff12.elapsedMonths shouldBeEqualTo 0
            dateDiff12.elapsedDays shouldBeEqualTo 0
            dateDiff12.elapsedHours shouldBeEqualTo hour.toLong()
            dateDiff12.elapsedMinutes shouldBeEqualTo 0
            dateDiff12.elapsedSeconds shouldBeEqualTo 0

            dateDiff12.years shouldBeEqualTo 0
            dateDiff12.quarters shouldBeEqualTo 0
            dateDiff12.months shouldBeEqualTo 0
            dateDiff12.weeks shouldBeEqualTo 0
            dateDiff12.days shouldBeEqualTo 0
            dateDiff12.hours shouldBeEqualTo hours12
            dateDiff12.minutes shouldBeEqualTo hours12 * MinutesPerHour
            dateDiff12.seconds shouldBeEqualTo hours12 * MinutesPerHour * SecondsPerMinute

            val dateDiff13 = DateDiff(date1, date3)
            val hours13 = Duration.between(date1, date3).toHours()

            dateDiff13.elapsedYears shouldBeEqualTo 0
            dateDiff13.elapsedMonths shouldBeEqualTo 0
            dateDiff13.elapsedDays shouldBeEqualTo 0
            dateDiff13.elapsedHours shouldBeEqualTo -hour.toLong()
            dateDiff13.elapsedMinutes shouldBeEqualTo 0
            dateDiff13.elapsedSeconds shouldBeEqualTo 0

            dateDiff13.years shouldBeEqualTo 0
            dateDiff13.quarters shouldBeEqualTo 0
            dateDiff13.months shouldBeEqualTo 0
            dateDiff13.weeks shouldBeEqualTo 0
            dateDiff13.days shouldBeEqualTo 0
            dateDiff13.hours shouldBeEqualTo hours13
            dateDiff13.minutes shouldBeEqualTo hours13 * MinutesPerHour
            dateDiff13.seconds shouldBeEqualTo hours13 * MinutesPerHour * SecondsPerMinute
        }
    }

    @Test
    fun `datediff - minutes`() {

        val minutes = listOf(1, 3, 5, 7)

        minutes.forEach { minute ->

            val date1 = zonedDateTimeOf(2011, 5, 19, 15, 32, 44, 245)
            val date2 = date1 + minute.minutes()
            val date3 = date1 - minute.minutes()

            val dateDiff12 = DateDiff(date1, date2)
            val minutes12 = Duration.between(date1, date2).toMinutes()

            dateDiff12.elapsedYears shouldBeEqualTo 0
            dateDiff12.elapsedMonths shouldBeEqualTo 0
            dateDiff12.elapsedDays shouldBeEqualTo 0
            dateDiff12.elapsedHours shouldBeEqualTo 0
            dateDiff12.elapsedMinutes shouldBeEqualTo minute.toLong()
            dateDiff12.elapsedSeconds shouldBeEqualTo 0

            dateDiff12.years shouldBeEqualTo 0
            dateDiff12.quarters shouldBeEqualTo 0
            dateDiff12.months shouldBeEqualTo 0
            dateDiff12.weeks shouldBeEqualTo 0
            dateDiff12.days shouldBeEqualTo 0
            dateDiff12.hours shouldBeEqualTo 0
            dateDiff12.minutes shouldBeEqualTo minutes12
            dateDiff12.seconds shouldBeEqualTo minutes12 * SecondsPerMinute

            val dateDiff13 = DateDiff(date1, date3)
            val minutes13 = Duration.between(date1, date3).toMinutes()

            dateDiff13.elapsedYears shouldBeEqualTo 0
            dateDiff13.elapsedMonths shouldBeEqualTo 0
            dateDiff13.elapsedDays shouldBeEqualTo 0
            dateDiff13.elapsedHours shouldBeEqualTo 0
            dateDiff13.elapsedMinutes shouldBeEqualTo -minute.toLong()
            dateDiff13.elapsedSeconds shouldBeEqualTo 0

            dateDiff13.years shouldBeEqualTo 0
            dateDiff13.quarters shouldBeEqualTo 0
            dateDiff13.months shouldBeEqualTo 0
            dateDiff13.weeks shouldBeEqualTo 0
            dateDiff13.days shouldBeEqualTo 0
            dateDiff13.hours shouldBeEqualTo 0
            dateDiff13.minutes shouldBeEqualTo minutes13
            dateDiff13.seconds shouldBeEqualTo minutes13 * SecondsPerMinute
        }
    }

    @Test
    fun `datediff - seconds`() {

        val seconds = listOf(1, 3, 5, 7)

        seconds.forEach { second ->

            val date1 = zonedDateTimeOf(2011, 5, 19, 15, 32, 44, 245)
            val date2 = date1 + second.seconds()
            val date3 = date1 - second.seconds()

            val dateDiff12 = DateDiff(date1, date2)
            val seconds12 = Duration.between(date1, date2).seconds

            dateDiff12.elapsedYears shouldBeEqualTo 0
            dateDiff12.elapsedMonths shouldBeEqualTo 0
            dateDiff12.elapsedDays shouldBeEqualTo 0
            dateDiff12.elapsedHours shouldBeEqualTo 0
            dateDiff12.elapsedMinutes shouldBeEqualTo 0
            dateDiff12.elapsedSeconds shouldBeEqualTo second.toLong()

            dateDiff12.years shouldBeEqualTo 0
            dateDiff12.quarters shouldBeEqualTo 0
            dateDiff12.months shouldBeEqualTo 0
            dateDiff12.weeks shouldBeEqualTo 0
            dateDiff12.days shouldBeEqualTo 0
            dateDiff12.hours shouldBeEqualTo 0
            dateDiff12.minutes shouldBeEqualTo 0
            dateDiff12.seconds shouldBeEqualTo seconds12

            val dateDiff13 = DateDiff(date1, date3)
            val seconds13 = Duration.between(date1, date3).seconds

            dateDiff13.elapsedYears shouldBeEqualTo 0
            dateDiff13.elapsedMonths shouldBeEqualTo 0
            dateDiff13.elapsedDays shouldBeEqualTo 0
            dateDiff13.elapsedHours shouldBeEqualTo 0
            dateDiff13.elapsedMinutes shouldBeEqualTo 0
            dateDiff13.elapsedSeconds shouldBeEqualTo -second.toLong()

            dateDiff13.years shouldBeEqualTo 0
            dateDiff13.quarters shouldBeEqualTo 0
            dateDiff13.months shouldBeEqualTo 0
            dateDiff13.weeks shouldBeEqualTo 0
            dateDiff13.days shouldBeEqualTo 0
            dateDiff13.hours shouldBeEqualTo 0
            dateDiff13.minutes shouldBeEqualTo 0
            dateDiff13.seconds shouldBeEqualTo seconds13
        }
    }

    @Test
    fun `positive duration`() {
        val diffs = longArrayOf(1, 3, 5)

        diffs.forEach { diff ->
            val date1 = zonedDateTimeOf(2017, 10, 14)
            val date2 = date1.plusYears(diff).plusMonths(diff).plusDays(diff).plusHours(diff).plusMinutes(diff)
                .plusSeconds(diff)
            val date3 = date1.minusYears(diff).minusMonths(diff).minusDays(diff).minusHours(diff).minusMinutes(diff)
                .minusSeconds(diff)

            val dateDiff12 = DateDiff(date1, date2)

            dateDiff12.elapsedYears shouldBeEqualTo diff
            dateDiff12.elapsedMonths shouldBeEqualTo diff
            dateDiff12.elapsedDays shouldBeEqualTo diff
            dateDiff12.elapsedHours shouldBeEqualTo diff
            dateDiff12.elapsedMinutes shouldBeEqualTo diff
            dateDiff12.elapsedSeconds shouldBeEqualTo diff

            val dateDiff13 = DateDiff(date1, date3)

            dateDiff13.elapsedYears shouldBeEqualTo -diff
            dateDiff13.elapsedMonths shouldBeEqualTo -diff
            dateDiff13.elapsedDays shouldBeEqualTo -diff
            dateDiff13.elapsedHours shouldBeEqualTo -diff
            dateDiff13.elapsedMinutes shouldBeEqualTo -diff
            dateDiff13.elapsedSeconds shouldBeEqualTo -diff
        }
    }
}
