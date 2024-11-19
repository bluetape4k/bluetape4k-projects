package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.MonthsPerYear
import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.lengthOfMonth
import io.bluetape4k.javatimes.monthPeriod
import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.startOfMonth
import io.bluetape4k.javatimes.startOfYear
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test


class MonthRangeTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `construct with now`() {
        val now = nowZonedDateTime()
        val firstMonth = now.startOfMonth()
        val secondMonth = firstMonth.plusMonths(1)

        log.debug { "firstMonth=$firstMonth, secondMonth=$secondMonth" }

        val mr = MonthRange(now, TimeCalendar.EmptyOffset)

        mr.start shouldBeEqualTo firstMonth
        mr.end shouldBeEqualTo secondMonth
    }

    @Test
    fun `default constructor`() = runTest {
        val yearStart = nowZonedDateTime().startOfYear()
        log.debug { "yearStart=$yearStart" }


        val tasks = (0 until MonthsPerYear).map { i ->
            async {
                val mr = MonthRange(yearStart + i.monthPeriod())

                log.debug { "i=$i, mr=$mr" }

                mr.year shouldBeEqualTo yearStart.year
                mr.monthOfYear shouldBeEqualTo i + 1

                mr.unmappedStart shouldBeEqualTo yearStart + i.monthPeriod()
                mr.unmappedEnd shouldBeEqualTo yearStart + (i + 1).monthPeriod()

                mr.year shouldBeEqualTo yearStart.year
                mr.monthOfYear shouldBeEqualTo i + 1

                mr.unmappedStart shouldBeEqualTo yearStart + i.monthPeriod()
                mr.unmappedEnd shouldBeEqualTo yearStart + (i + 1).monthPeriod()
            }
        }
        tasks.awaitAll()
    }

    @Test
    fun `day sequence`() {
        val now = nowZonedDateTime()
        val mr = MonthRange()
        val days = mr.daySequence()

        days.forEachIndexed { index, dr ->
            dr.start shouldBeEqualTo mr.start + index.days()
            dr.end shouldBeEqualTo dr.start + 1.days() + dr.calendar.endOffset
        }

        lengthOfMonth(now.year, now.monthValue) shouldBeEqualTo days.count()
    }

    @Test
    fun `add months`() {
        val now = nowZonedDateTime()
        val startMonth = now.startOfMonth()
        val mr = MonthRange(now)

        mr.addMonths(0) shouldBeEqualTo mr
        mr.prevMonth().start shouldBeEqualTo startMonth - 1.monthPeriod()
        mr.nextMonth().start shouldBeEqualTo startMonth + 1.monthPeriod()

        (-60..120).toList().parallelStream().forEach { m ->
            mr.addMonths(m).start shouldBeEqualTo startMonth + m.monthPeriod()
        }

        runBlocking(Dispatchers.Default) {
            val tasks = (-60..120).map { m ->
                async {
                    mr.addMonths(m).start shouldBeEqualTo startMonth + m.monthPeriod()
                }
            }
            tasks.awaitAll()
        }
    }
}
