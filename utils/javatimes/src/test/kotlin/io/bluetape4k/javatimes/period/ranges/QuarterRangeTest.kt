package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.MonthsPerQuarter
import io.bluetape4k.javatimes.MonthsPerYear
import io.bluetape4k.javatimes.Quarter
import io.bluetape4k.javatimes.monthPeriod
import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.startOfQuarter
import io.bluetape4k.javatimes.startOfYear
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.junit.jupiter.api.Test
import kotlin.math.absoluteValue
import kotlin.test.assertTrue

class QuarterRangeTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `simple construct`() {
        val now = nowZonedDateTime()
        val firstQuarter = startOfQuarter(now.year, Quarter.Q1)
        val secondQuarter = startOfQuarter(now.year, Quarter.Q2)

        val qr = QuarterRange(now.year, Quarter.Q1, TimeCalendar.EmptyOffset)

        qr.start shouldBeEqualTo firstQuarter
        qr.end shouldBeEqualTo secondQuarter
    }

    @Test
    fun `default constructor`() {
        val yearStart = nowZonedDateTime().startOfYear()

        Quarter.entries.forEach { quarter ->
            val offset = quarter.number - 1L
            val qr = QuarterRange(yearStart.plusMonths(offset * MonthsPerQuarter))

            qr.unmappedStart shouldBeEqualTo yearStart + (offset * MonthsPerQuarter).monthPeriod()
            qr.unmappedEnd shouldBeEqualTo yearStart + ((offset + 1) * MonthsPerQuarter).monthPeriod()
        }
    }

    @Test
    fun `check quater property by construct with moment`() {
        val now = nowZonedDateTime()
        val nowYear = now.year

        QuarterRange(zonedDateTimeOf(nowYear, 1, 1)).quarter shouldBeEqualTo Quarter.Q1
        QuarterRange(zonedDateTimeOf(nowYear, 3, 31)).quarter shouldBeEqualTo Quarter.Q1

        QuarterRange(zonedDateTimeOf(nowYear, 4, 1)).quarter shouldBeEqualTo Quarter.Q2
        QuarterRange(zonedDateTimeOf(nowYear, 6, 30)).quarter shouldBeEqualTo Quarter.Q2

        QuarterRange(zonedDateTimeOf(nowYear, 7, 1)).quarter shouldBeEqualTo Quarter.Q3
        QuarterRange(zonedDateTimeOf(nowYear, 9, 30)).quarter shouldBeEqualTo Quarter.Q3

        QuarterRange(zonedDateTimeOf(nowYear, 10, 1)).quarter shouldBeEqualTo Quarter.Q4
        QuarterRange(zonedDateTimeOf(nowYear, 12, 31)).quarter shouldBeEqualTo Quarter.Q4
    }

    @Test
    fun `check startMonth property`() {
        val now = nowZonedDateTime()
        val nowYear = now.year

        QuarterRange(nowYear, Quarter.Q1).startMonthOfYear shouldBeEqualTo Quarter.Q1.startMonth
        QuarterRange(nowYear, Quarter.Q2).startMonthOfYear shouldBeEqualTo Quarter.Q2.startMonth
        QuarterRange(nowYear, Quarter.Q3).startMonthOfYear shouldBeEqualTo Quarter.Q3.startMonth
        QuarterRange(nowYear, Quarter.Q4).startMonthOfYear shouldBeEqualTo Quarter.Q4.startMonth
    }

    @Test
    fun `is multiple calendar year`() {
        val now = nowZonedDateTime()

        QuarterRange(now.year, Quarter.Q1).isMultipleCalendarYears.shouldBeFalse()
    }

    @Test
    fun `is multiple calendar quarter`() {
        val now = nowZonedDateTime()

        QuarterRange(now.year, Quarter.Q1).isMultipleCalendarQuarters.shouldBeFalse()
    }

    @Test
    fun `calendar quarter`() {
        val now = nowZonedDateTime()
        val nowYear = now.year
        val calendar = TimeCalendar.EmptyOffset

        fun checkQuarters(qr: QuarterRange, quarter: Quarter) {
            assertTrue(qr.readonly)
            qr.quarter shouldBeEqualTo quarter
            qr.start shouldBeEqualTo zonedDateTimeOf(nowYear, quarter.startMonth)

            val endYear = if (quarter == Quarter.Q4) nowYear + 1 else nowYear
            qr.end shouldBeEqualTo zonedDateTimeOf(endYear, (quarter.endMonth + 1) % MonthsPerYear)
        }

        Quarter.entries.forEach { quarter ->
            val qr = QuarterRange(nowYear, quarter, calendar)
            checkQuarters(qr, quarter)
        }
    }

    @Test
    fun `month sequence`() {
        val now = nowZonedDateTime()
        val nowYear = now.year
        val calendar = TimeCalendar.EmptyOffset

        val qr = QuarterRange(nowYear, Quarter.Q1, calendar)

        val monthSeq = qr.monthSequence()

        monthSeq.count() shouldBeEqualTo MonthsPerQuarter
        monthSeq.forEachIndexed { index, mr ->
            mr.unmappedStart shouldBeEqualTo qr.start.plusMonths(index.toLong())
            mr.unmappedEnd shouldBeEqualTo qr.start.plusMonths(index + 1L)
        }
    }

    @Test
    fun `add quarters`() {
        val now = nowZonedDateTime()
        val nowYear = now.year
        val calendar = TimeCalendar.EmptyOffset

        val q1 = QuarterRange(nowYear, Quarter.Q1, calendar)

        (-8..8).forEach { q ->
            val qr = q1.addQuarters(q)
            val qv = when {
                q < 0 -> 4 + (q + 1) % 4
                else  -> q % 4 + 1
            }
            log.trace { "q=$q, qv=$qv" }
            val quarter = Quarter.of(qv.absoluteValue)
            val startTime = q1.start + (q * MonthsPerQuarter).monthPeriod()
            val endTime = startTime + 3.monthPeriod()

            log.trace { "add quarter q = $q, quarter=$quarter" }

            qr.quarter shouldBeEqualTo quarter
            qr.unmappedStart shouldBeEqualTo startTime
            qr.unmappedEnd shouldBeEqualTo endTime
        }
    }
}
