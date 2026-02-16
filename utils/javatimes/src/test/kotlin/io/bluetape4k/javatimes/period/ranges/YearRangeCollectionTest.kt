package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.startOfDay
import io.bluetape4k.javatimes.startOfYear
import io.bluetape4k.javatimes.yearPeriod
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class YearRangeCollectionTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `single years`() {
        val startYear = now.year

        val yrs = YearRangeCollection(startYear, 1)

        yrs.yearCount shouldBeEqualTo 1
        yrs.startYear shouldBeEqualTo startYear
        yrs.endYear shouldBeEqualTo startYear

        val yearSeq = yrs.yearSequence()

        yearSeq.count() shouldBeEqualTo 1
        yearSeq.first() shouldBeEqualTo YearRange(startYear)
    }

    @Test
    fun `multiple years`() {
        val startYear = now.year
        val yearCount = 5

        val yrs = YearRangeCollection(startYear, yearCount, TimeCalendar.EmptyOffset)

        yrs.yearCount shouldBeEqualTo yearCount
        yrs.startYear shouldBeEqualTo startYear
        yrs.endYear shouldBeEqualTo startYear + yearCount
    }

    @Test
    fun `various year count`() = runTest {
        val yearCounts = listOf(1, 6, 48, 180, 365)

        val today = now.startOfDay()

        yearCounts.map { yearCount ->
            launch(Dispatchers.Default) {
                val yrs = YearRangeCollection(now, yearCount)

                val startTime = yrs.calendar.mapStart(today.startOfYear())
                val endTime = yrs.calendar.mapEnd(startTime + yearCount.yearPeriod())

                yrs.start shouldBeEqualTo startTime
                yrs.end shouldBeEqualTo endTime

                val yearSeq = yrs.yearSequence()

                yearSeq.mapIndexed { year, yr ->
                    async(Dispatchers.Default) {
                        yr.start shouldBeEqualTo startTime + year.yearPeriod()
                        yr.end shouldBeEqualTo yr.calendar.mapEnd(startTime.plusYears(year + 1L))

                        yr.unmappedStart shouldBeEqualTo startTime.plusYears(year.toLong())
                        yr.unmappedEnd shouldBeEqualTo startTime.plusYears(year + 1L)

                        yr shouldBeEqualTo YearRange(yrs.start.plusYears(year.toLong()))
                    }
                }
                    .toList()
                    .awaitAll()
            }
        }.joinAll()
    }
}
