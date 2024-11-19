package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.startOfYear
import io.bluetape4k.javatimes.yearPeriod
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class YearRangeTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `construct with now`() {
        val thisYear = now.startOfYear()
        val nextYear = thisYear + 1.yearPeriod()

        val yr = YearRange(now, TimeCalendar.EmptyOffset)

        yr.start shouldBeEqualTo thisYear
        yr.end shouldBeEqualTo nextYear
    }

    @Test
    fun `construct with year value`() {
        val nowYear = now.year

        val yr = YearRange(nowYear, TimeCalendar.EmptyOffset)

        yr.readonly.shouldBeTrue()
        yr.start shouldBeEqualTo zonedDateTimeOf(nowYear)
        yr.end shouldBeEqualTo zonedDateTimeOf(nowYear + 1)
    }

    @Test
    fun `add years`() {
        val startYear = now.startOfYear()

        val yr = YearRange(now)
        log.trace { "year range=$yr, prev year=${startYear - 1.yearPeriod()}" }

        yr.prevYear().start shouldBeEqualTo startYear - 1.yearPeriod()
        yr.nextYear().start shouldBeEqualTo startYear + 1.yearPeriod()

        runBlocking(Dispatchers.Default) {
            (-120..120).map { year ->
                async {
                    yr.addYears(year).start shouldBeEqualTo startYear + year.yearPeriod()
                }
            }.awaitAll()
        }
    }
}
