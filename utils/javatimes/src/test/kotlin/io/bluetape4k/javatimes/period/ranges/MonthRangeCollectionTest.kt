package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.monthPeriod
import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.startOfMonth
import io.bluetape4k.javatimes.todayZonedDateTime
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeGreaterThan
import org.junit.jupiter.api.Test
import java.time.YearMonth


class MonthRangeCollectionTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `single month collection`() {
        val now = nowZonedDateTime()
        val startYear = now.year
        val startMonth = now.monthValue

        val mrs = MonthRangeCollection(startYear, startMonth, 1)
        mrs.monthCount shouldBeEqualTo 1

        mrs.startYear shouldBeEqualTo startYear
        mrs.endYear shouldBeEqualTo startYear
        mrs.startMonthOfYear shouldBeEqualTo startMonth
        mrs.endMonthOfYear shouldBeEqualTo startMonth


        val months = mrs.monthSequence()
        months.count() shouldBeEqualTo 1
        months.first() shouldBeEqualTo MonthRange(startYear, startMonth)
    }

    @Test
    fun `month collection with calendar`() {
        val now = nowZonedDateTime()
        val startYear = now.year
        val startMonth = now.monthValue
        val monthCount = 15

        val mrs = MonthRangeCollection(startYear, startMonth, monthCount)

        mrs.monthCount shouldBeEqualTo monthCount
        mrs.startYear shouldBeEqualTo startYear
        mrs.startMonthOfYear shouldBeEqualTo startMonth
        mrs.endYear shouldBeGreaterThan startYear
    }

    @Test
    fun `various month count`() {
        val monthCounts = listOf(1, 6, 48, 180, 365)

        val now = nowZonedDateTime()
        val today = todayZonedDateTime()

        monthCounts.parallelStream().forEach { monthCount ->

            val mrs = MonthRangeCollection(now, monthCount)

            val startTime = mrs.calendar.mapStart(today.startOfMonth())
            val endTime = mrs.calendar.mapEnd(startTime.plusMonths(monthCount.toLong()))

            mrs.start shouldBeEqualTo startTime
            mrs.end shouldBeEqualTo endTime

            val monthSeq = mrs.monthSequence()

            monthSeq.forEachIndexed { i, mr ->
                mr.start shouldBeEqualTo startTime + i.monthPeriod()
                mr.end shouldBeEqualTo mr.calendar.mapEnd(startTime.plusMonths(i + 1L))

                mr.unmappedStart shouldBeEqualTo startTime + i.monthPeriod()
                mr.unmappedEnd shouldBeEqualTo startTime + (i + 1).monthPeriod()

                mr shouldBeEqualTo MonthRange(mrs.start + i.monthPeriod())

                val ym = YearMonth.of(now.year, now.monthValue).plusMonths(i.toLong())
                mr shouldBeEqualTo MonthRange(ym, mrs.start.zone)
            }
        }
    }
}
