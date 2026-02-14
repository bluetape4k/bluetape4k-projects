package io.bluetape4k.javatimes.period.ranges

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.startOfWeek
import io.bluetape4k.javatimes.startOfWeekOfWeekyear
import io.bluetape4k.javatimes.todayZonedDateTime
import io.bluetape4k.javatimes.weekOfWeekyear
import io.bluetape4k.javatimes.weekyear
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.junit5.coroutines.SuspendedJobTester
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class WeekRangeCollectionTest: AbstractPeriodTest() {

    companion object: KLogging()

    @ParameterizedTest(name = "single week collection. day={0}")
    @ValueSource(ints = [1, 15, 31])
    fun `single week collection`(day: Int) {
        val now = zonedDateTimeOf(2019, 12, day)
        val startYear = now.weekyear
        val startWeek = now.weekOfWeekyear
        val start = startOfWeekOfWeekyear(now.weekyear, now.weekOfWeekyear)
        log.trace { "weekyear=$startYear, weekOfWeekyear=$startWeek, start=$start" }

        val wrs = WeekRangeCollection(startYear, startWeek, 1)
        log.debug { "wrs=$wrs" }

        wrs.weekCount shouldBeEqualTo 1
        if (now.year == startYear) {
            wrs.startYear shouldBeEqualTo startYear
        } else {
            wrs.startYear shouldBeEqualTo startYear - 1
        }
        wrs.endYear shouldBeEqualTo startYear
        wrs.startWeekOfWeekyear shouldBeEqualTo startWeek
        wrs.endWeekOfWeekyear shouldBeEqualTo startWeek

        val weekSeq = wrs.weekSequence()
        weekSeq.count() shouldBeEqualTo 1
        weekSeq.first() shouldBeEqualTo WeekRange(startYear, startWeek)
    }

    @Test
    fun `week range collection with calendar`() {
        val startYear = 2018
        val startWeek = 22
        val weekCount = 5

        val wrs = WeekRangeCollection(startYear, startWeek, weekCount)

        wrs.weekCount shouldBeEqualTo weekCount
        wrs.startYear shouldBeEqualTo startYear
        wrs.startWeekOfWeekyear shouldBeEqualTo startWeek
        wrs.endYear shouldBeEqualTo startYear
        wrs.endWeekOfWeekyear shouldBeEqualTo startWeek + weekCount - 1
    }

    @ParameterizedTest(name = "various weekCount. weekCount={0}")
    @ValueSource(ints = [1, 6, 48, 180, 365])
    fun `various weekCount`(weekCount: Int) {
        val now = nowZonedDateTime()
        val today = todayZonedDateTime()

        val wrs = WeekRangeCollection(now, weekCount)

        val startTime = wrs.calendar.mapStart(today.startOfWeek())
        val endTime = wrs.calendar.mapEnd(startTime.plusWeeks(weekCount.toLong()))

        wrs.start shouldBeEqualTo startTime
        wrs.end shouldBeEqualTo endTime

        val wrSeq = wrs.weekSequence()
        wrSeq.count() shouldBeEqualTo weekCount

        runBlocking(Dispatchers.Default) {
            val tasks = wrSeq.mapIndexed { w, wr ->
                async {
                    wr.start shouldBeEqualTo startTime.plusWeeks(w.toLong())
                    wr.end shouldBeEqualTo wr.calendar.mapEnd(startTime.plusWeeks(w + 1L))

                    wr.unmappedStart shouldBeEqualTo startTime.plusWeeks(w.toLong())
                    wr.unmappedEnd shouldBeEqualTo startTime.plusWeeks(w + 1L)

                    wr shouldBeEqualTo WeekRange(wrs.start.plusWeeks(w.toLong()))
                    val afterWeek = now.startOfWeek().plusWeeks(w.toLong())
                    wr shouldBeEqualTo WeekRange(afterWeek)
                }
            }.toFastList()
            
            tasks.awaitAll()
        }
    }

    @Test
    fun `various weekCount in coroutines`() = runTest {
        val weekCounts = listOf(1, 6, 48, 180, 365)

        val now = nowZonedDateTime()
        val today = todayZonedDateTime()

        SuspendedJobTester()
            .workers(8)
            .rounds(5 * 8)
            .add {
                weekCounts.forEach { weekCount ->
                    val wrs = WeekRangeCollection(now, weekCount)

                    val startTime = wrs.calendar.mapStart(today.startOfWeek())
                    val endTime = wrs.calendar.mapEnd(startTime.plusWeeks(weekCount.toLong()))

                    wrs.start shouldBeEqualTo startTime
                    wrs.end shouldBeEqualTo endTime

                    val wrSeq = wrs.weekSequence()
                    wrSeq.count() shouldBeEqualTo weekCount

                    val tasks = wrSeq.mapIndexed { w, wr ->
                        async(Dispatchers.Default) {
                            wr.start shouldBeEqualTo startTime.plusWeeks(w.toLong())
                            wr.end shouldBeEqualTo wr.calendar.mapEnd(startTime.plusWeeks(w + 1L))

                            wr.unmappedStart shouldBeEqualTo startTime.plusWeeks(w.toLong())
                            wr.unmappedEnd shouldBeEqualTo startTime.plusWeeks(w + 1L)

                            wr shouldBeEqualTo WeekRange(wrs.start.plusWeeks(w.toLong()))
                            val afterWeek = now.startOfWeek().plusWeeks(w.toLong())
                            wr shouldBeEqualTo WeekRange(afterWeek)
                        }
                    }.toFastList()
                    tasks.awaitAll()
                }
            }
            .run()
    }
}
