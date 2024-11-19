package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.javatimes.DaysPerWeek
import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.startOfWeek
import io.bluetape4k.javatimes.startOfYear
import io.bluetape4k.javatimes.weekOfWeekyear
import io.bluetape4k.javatimes.weekPeriod
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class WeekRangeTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `construct with now`() {
        val now = nowZonedDateTime()
        val firstWeek = now.startOfWeek()
        val secondWeek = firstWeek + 1.weekPeriod()

        val wr = WeekRange(now, TimeCalendar.EmptyOffset)

        wr.start shouldBeEqualTo firstWeek
        wr.end shouldBeEqualTo secondWeek
    }

    @Test
    fun `default construct`() {
        val yearStart = nowZonedDateTime().startOfYear()
        val weekStart = yearStart.startOfWeek()

        (1L until 50L).forEach { w ->
            val wr = WeekRange(yearStart + w.weekPeriod())

            wr.year shouldBeEqualTo yearStart.year
            wr.weekOfWeekyear shouldBeEqualTo yearStart.plusWeeks(w).weekOfWeekyear

            wr.unmappedStart shouldBeEqualTo weekStart.plusWeeks(w)
            wr.unmappedEnd shouldBeEqualTo weekStart.plusWeeks(w + 1)
        }
    }

    @Test
    fun `day sequence`() {
        val wr = WeekRange()
        val days = wr.daySequence()

        days.count() shouldBeEqualTo DaysPerWeek

        days.forEachIndexed { index, dr ->
            dr.start shouldBeEqualTo wr.start + index.days()
            dr.end shouldBeEqualTo dr.calendar.mapEnd(dr.start.plusDays(1L))
        }
    }

    @Test
    fun `add weeks`() {
        val now = nowZonedDateTime()
        val startWeek = now.startOfWeek()
        val wr = WeekRange(now)

        log.trace { "startWeek=$startWeek, weekRange=$wr" }

        wr.prevWeek().start shouldBeEqualTo startWeek.minusWeeks(1)
        wr.nextWeek().start shouldBeEqualTo startWeek.plusWeeks(1)

        wr.addWeeks(0) shouldBeEqualTo wr

        runBlocking(Dispatchers.Default) {
            val tasks = (-60L..120L).map { m ->
                async {
                    wr.addWeeks(m).start shouldBeEqualTo startWeek.plusWeeks(m)
                }
            }
            tasks.awaitAll()
        }
    }
}
