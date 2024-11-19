package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.Quarter
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.startOfQuarter
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test


class QuarterRangeCollectionTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `construct with moment`() {
        val qrs = QuarterRangeCollection(now, 3)

        qrs.quarterCount shouldBeEqualTo 3
        qrs.start shouldBeEqualTo now.startOfQuarter()
    }

    @Test
    fun `single quarter`() {
        val startYear = now.year
        val startQuarter = Quarter.Q2

        val qrs = QuarterRangeCollection(startYear, startQuarter, 1)

        qrs.quarterCount shouldBeEqualTo 1
        qrs.quarterOfStart shouldBeEqualTo startQuarter
        qrs.endYear shouldBeEqualTo startYear
        qrs.quarterOfEnd shouldBeEqualTo startQuarter

        val quarterSeq = qrs.quarterSequence()
        quarterSeq.count() shouldBeEqualTo 1
        quarterSeq.first() shouldBeEqualTo QuarterRange(startYear, Quarter.Q2)
    }

    @Test
    fun `various quarter count`() {
        val quarterCounts = listOf(1, 5, 10, 64, 128)

        val startYear = now.year
        val startQuarter = Quarter.Q2

        quarterCounts.forEach { quarterCount ->
            val qrs = QuarterRangeCollection(startYear, startQuarter, quarterCount)

            qrs.quarterCount shouldBeEqualTo quarterCount
            qrs.quarterOfStart shouldBeEqualTo startQuarter
            qrs.endYear shouldBeEqualTo startYear + quarterCount / 4
            qrs.quarterOfEnd shouldBeEqualTo Quarter.of((2 + quarterCount - 1) % 4)

            val quarters = qrs.quarters()

            quarters.size shouldBeEqualTo quarterCount
            quarters[0] shouldBeEqualTo QuarterRange(startYear, Quarter.Q2)

            if (quarterCount > 4) {
                quarters[1] shouldBeEqualTo QuarterRange(startYear, Quarter.Q3)
                quarters[2] shouldBeEqualTo QuarterRange(startYear, Quarter.Q4)
                quarters[3] shouldBeEqualTo QuarterRange(startYear + 1, Quarter.Q1)
                quarters[4] shouldBeEqualTo QuarterRange(startYear + 1, Quarter.Q2)
            }
        }
    }
}
