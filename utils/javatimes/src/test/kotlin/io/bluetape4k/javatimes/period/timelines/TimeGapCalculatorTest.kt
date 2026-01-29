package io.bluetape4k.javatimes.period.timelines

import io.bluetape4k.collections.eclipse.toFastList
import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.hours
import io.bluetape4k.javatimes.isWeekend
import io.bluetape4k.javatimes.nanos
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.ITimePeriod
import io.bluetape4k.javatimes.period.TimeCalendar
import io.bluetape4k.javatimes.period.TimePeriodCollection
import io.bluetape4k.javatimes.period.TimeRange
import io.bluetape4k.javatimes.period.hasInsideWith
import io.bluetape4k.javatimes.period.ranges.CalendarTimeRange
import io.bluetape4k.javatimes.period.ranges.DayRangeCollection
import io.bluetape4k.javatimes.period.ranges.MonthRange
import io.bluetape4k.javatimes.period.samples.SchoolDay
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEmpty
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldHaveSize
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class TimeGapCalculatorTest: AbstractPeriodTest() {

    companion object: KLogging()

    private val startTime = zonedDateTimeOf(2018, 3, 1)
    private val endTime = zonedDateTimeOf(2018, 3, 5)
    private val limits = TimeRange(startTime, endTime)

    private val calculator: TimeGapCalculator<ITimePeriod> = TimeGapCalculator()

    @Test
    fun `no periods`() = runTest {
        val gaps = calculator.gaps(TimePeriodCollection.EMPTY, limits)
        gaps.toFastList() shouldBeEqualTo listOf(limits)
    }

    @Test
    fun `when exclude period is equal to limits`() = runTest {
        val excludePeriod = TimePeriodCollection(limits)

        val gaps = calculator.gaps(excludePeriod, limits)
        gaps.shouldBeEmpty()
    }

    @Test
    fun `when exclude periods larger than limits`() = runTest {
        val excludePeriods = TimePeriodCollection(
            TimeRange(
                zonedDateTimeOf(2018, 2, 1),
                zonedDateTimeOf(2018, 4, 1)
            )
        )

        val gaps = calculator.gaps(excludePeriods, limits)
        gaps.shouldBeEmpty()
    }

    @Test
    fun `when exclude period is outside with limits`() = runTest {
        val excludePeriods = TimePeriodCollection(
            TimeRange(
                zonedDateTimeOf(2018, 2, 1),
                zonedDateTimeOf(2018, 2, 5)
            ),
            TimeRange(
                zonedDateTimeOf(2018, 4, 1),
                zonedDateTimeOf(2018, 4, 5)
            )
        )

        val gaps = calculator.gaps(excludePeriods, limits)
        gaps.toFastList() shouldBeEqualTo listOf(limits)
    }

    @Test
    fun `when exclude periods outside touching limits`() = runTest {
        val limits = MonthRange(2018, 3)
        val excludePeriods = TimePeriodCollection(
            TimeRange(
                zonedDateTimeOf(2018, 2, 1),
                zonedDateTimeOf(2018, 3, 5)
            ),
            TimeRange(
                zonedDateTimeOf(2018, 3, 20),
                zonedDateTimeOf(2018, 4, 15)
            )
        )

        val gaps = calculator.gaps(excludePeriods, limits)

        gaps.size shouldBeEqualTo 1
        gaps.first() shouldBeEqualTo TimeRange(
            zonedDateTimeOf(2018, 3, 5),
            zonedDateTimeOf(2018, 3, 20)
        )
    }

    @Test
    fun `simple gaps`() = runTest {
        val limits = TimeRange(
            zonedDateTimeOf(2018, 3, 1),
            zonedDateTimeOf(2018, 3, 20)
        )

        val excludeRange = TimeRange(
            zonedDateTimeOf(2018, 3, 10),
            zonedDateTimeOf(2018, 3, 15)
        )
        val excludePeriods = TimePeriodCollection(excludeRange)

        val gaps = calculator.gaps(excludePeriods, limits)

        gaps.toFastList() shouldBeEqualTo listOf(
            TimeRange(limits.start, excludeRange.start),
            TimeRange(excludeRange.end, limits.end)
        )
    }

    @Test
    fun `when exclude periods touching start of limits`() = runTest {
        val limits = TimeRange(
            zonedDateTimeOf(2018, 3, 1),
            zonedDateTimeOf(2018, 3, 20)
        )

        val excludePeriods = TimePeriodCollection(
            TimeRange(
                zonedDateTimeOf(2018, 3, 1),
                zonedDateTimeOf(2018, 3, 10)
            )
        )

        val gaps = calculator.gaps(excludePeriods, limits)

        gaps.size shouldBeEqualTo 1
        gaps[0] shouldBeEqualTo TimeRange(
            zonedDateTimeOf(2018, 3, 10),
            zonedDateTimeOf(2018, 3, 20)
        )
    }

    @Test
    fun `when exclude periods touching end of limits`() = runTest {
        val limits = TimeRange(
            zonedDateTimeOf(2018, 3, 1),
            zonedDateTimeOf(2018, 3, 20)
        )

        val excludePeriods = TimePeriodCollection(
            TimeRange(
                zonedDateTimeOf(2018, 3, 10),
                zonedDateTimeOf(2018, 3, 20)
            )
        )

        val gaps = calculator.gaps(excludePeriods, limits)

        gaps shouldHaveSize 1
        gaps.first() shouldBeEqualTo TimeRange(
            zonedDateTimeOf(2018, 3, 1),
            zonedDateTimeOf(2018, 3, 10)
        )
    }

    @Test
    fun `when exclude period is moment`() = runTest {
        val limits = TimeRange(
            zonedDateTimeOf(2018, 3, 1),
            zonedDateTimeOf(2018, 3, 20), true
        )

        val excludePeriods = TimePeriodCollection(
            TimeRange(zonedDateTimeOf(2018, 3, 10))
        )

        // Gap 검사 시에 moment는 제외된다.
        val gaps = calculator.gaps(excludePeriods, limits)
        gaps shouldHaveSize 1
        gaps.first() shouldBeEqualTo limits
    }

    @Test
    fun `all exclude periods touching`() = runTest {
        val limits = TimeRange(
            zonedDateTimeOf(2018, 3, 29),
            zonedDateTimeOf(2018, 4, 1), true
        )

        val excludePeriods = TimePeriodCollection(
            TimeRange(
                zonedDateTimeOf(2018, 3, 30, 0, 0),
                zonedDateTimeOf(2018, 3, 30, 8, 30)
            ),
            TimeRange(
                zonedDateTimeOf(2018, 3, 30, 8, 30),
                zonedDateTimeOf(2018, 3, 30, 12, 0)
            ),
            TimeRange(
                zonedDateTimeOf(2018, 3, 30, 10, 0),
                zonedDateTimeOf(2018, 3, 31, 0, 0)
            )
        )

        val gaps = calculator.gaps(excludePeriods, limits)

        gaps shouldHaveSize 2
        gaps[0] shouldBeEqualTo TimeRange(
            zonedDateTimeOf(2018, 3, 29),
            zonedDateTimeOf(2018, 3, 30)
        )
        gaps[1] shouldBeEqualTo TimeRange(
            zonedDateTimeOf(2018, 3, 31),
            zonedDateTimeOf(2018, 4, 1)
        )
    }

    @Test
    fun `overlapping exclude periods 1`() = runTest {
        val limits = TimeRange(
            zonedDateTimeOf(2018, 3, 29),
            zonedDateTimeOf(2018, 4, 1), true
        )

        val excludePeriods = TimePeriodCollection(
            TimeRange(
                zonedDateTimeOf(2018, 3, 30, 0, 0),
                zonedDateTimeOf(2018, 3, 31, 0, 0)
            ),
            TimeRange(
                zonedDateTimeOf(2018, 3, 30, 0, 0),
                zonedDateTimeOf(2018, 3, 30, 12, 0)
            ),
            TimeRange(
                zonedDateTimeOf(2018, 3, 30, 12, 0),
                zonedDateTimeOf(2018, 3, 31, 0, 0)
            )
        )

        val gaps = calculator.gaps(excludePeriods, limits)

        gaps shouldHaveSize 2
        gaps[0] shouldBeEqualTo TimeRange(
            zonedDateTimeOf(2018, 3, 29),
            zonedDateTimeOf(2018, 3, 30)
        )
        gaps[1] shouldBeEqualTo TimeRange(
            zonedDateTimeOf(2018, 3, 31),
            zonedDateTimeOf(2018, 4, 1)
        )
    }

    @Test
    fun `overlapping exclude periods 2`() = runTest {
        val limits = TimeRange(
            zonedDateTimeOf(2018, 3, 29),
            zonedDateTimeOf(2018, 4, 1), true
        )

        val excludePeriods = TimePeriodCollection(
            TimeRange(
                zonedDateTimeOf(2018, 3, 30, 0, 0),
                zonedDateTimeOf(2018, 3, 31, 0, 0)
            ),
            TimeRange(
                zonedDateTimeOf(2018, 3, 30, 0, 0),
                zonedDateTimeOf(2018, 3, 30, 6, 30)
            ),
            TimeRange(
                zonedDateTimeOf(2018, 3, 30, 8, 30),
                zonedDateTimeOf(2018, 3, 30, 12, 0)
            ),
            TimeRange(
                zonedDateTimeOf(2018, 3, 30, 22, 30),
                zonedDateTimeOf(2018, 3, 31, 0, 0)
            )
        )

        val gaps = calculator.gaps(excludePeriods, limits)

        gaps shouldHaveSize 2
        gaps[0] shouldBeEqualTo TimeRange(
            zonedDateTimeOf(2018, 3, 29),
            zonedDateTimeOf(2018, 3, 30)
        )
        gaps[1] shouldBeEqualTo TimeRange(
            zonedDateTimeOf(2018, 3, 31),
            zonedDateTimeOf(2018, 4, 1)
        )
    }

    @Test
    fun `overlapping exclude periods 3`() = runTest {
        val limits = TimeRange(
            zonedDateTimeOf(2018, 3, 29),
            zonedDateTimeOf(2018, 4, 1), true
        )

        val excludePeriods = TimePeriodCollection(
            TimeRange(
                zonedDateTimeOf(2018, 3, 30),
                zonedDateTimeOf(2018, 3, 31)
            ),
            TimeRange(
                zonedDateTimeOf(2018, 3, 30),
                zonedDateTimeOf(2018, 3, 31)
            )
        )

        val gaps = calculator.gaps(excludePeriods, limits)

        gaps shouldHaveSize 2
        gaps[0] shouldBeEqualTo TimeRange(
            zonedDateTimeOf(2018, 3, 29),
            zonedDateTimeOf(2018, 3, 30)
        )
        gaps[1] shouldBeEqualTo TimeRange(
            zonedDateTimeOf(2018, 3, 31),
            zonedDateTimeOf(2018, 4, 1)
        )
    }

    @Test
    fun `calculate gap`() = runTest {
        val schoolDay = SchoolDay(now)

        val excludePeriods = TimePeriodCollection.ofAll(schoolDay)

        calculator.gaps(excludePeriods).shouldBeEmpty()
        calculator.gaps(excludePeriods, schoolDay).shouldBeEmpty()

        excludePeriods.clear()
        excludePeriods += schoolDay.lesson1
        excludePeriods += schoolDay.lesson2
        excludePeriods += schoolDay.lesson3
        excludePeriods += schoolDay.lesson4

        val gaps2 = calculator.gaps(excludePeriods)

        gaps2.size shouldBeEqualTo 3
        assertTrue { gaps2[0].isSamePeriod(schoolDay.break1) }
        assertTrue { gaps2[1].isSamePeriod(schoolDay.break2) }
        assertTrue { gaps2[2].isSamePeriod(schoolDay.break3) }

        val testRange3 = TimeRange(schoolDay.lesson1.start, schoolDay.lesson4.end)
        val gaps3 = calculator.gaps(excludePeriods, testRange3)

        gaps3.size shouldBeEqualTo 3
        assertTrue { gaps3[0].isSamePeriod(schoolDay.break1) }
        assertTrue { gaps3[1].isSamePeriod(schoolDay.break2) }
        assertTrue { gaps3[2].isSamePeriod(schoolDay.break3) }

        val testRange4 = TimeRange(schoolDay.start - 1.hours(), schoolDay.end + 1.hours())
        val gaps4 = calculator.gaps(excludePeriods, testRange4)

        gaps4.size shouldBeEqualTo 5
        assertTrue { gaps4[0].isSamePeriod(TimeRange(testRange4.start, schoolDay.start)) }
        assertTrue { gaps4[1].isSamePeriod(schoolDay.break1) }
        assertTrue { gaps4[2].isSamePeriod(schoolDay.break2) }
        assertTrue { gaps4[3].isSamePeriod(schoolDay.break3) }
        assertTrue { gaps4[4].isSamePeriod(TimeRange(schoolDay.end, testRange4.end)) }

        excludePeriods.clear()
        excludePeriods += schoolDay.lesson1

        val gaps5 = calculator.gaps(excludePeriods, schoolDay.lesson1)
        gaps5.shouldBeEmpty()

        excludePeriods.clear()
        excludePeriods += schoolDay.lesson1
        val testRange6 = TimeRange(
            schoolDay.lesson1.start - 1.nanos(),
            schoolDay.lesson1.end + 1.nanos()
        )

        val gaps6 = calculator.gaps(excludePeriods, testRange6)

        gaps6 shouldHaveSize 2
        gaps6[0].duration shouldBeEqualTo 1.nanos()
        gaps6[1].duration shouldBeEqualTo 1.nanos()
    }

    @Test
    fun `calculate gap with calendar`() = runTest {
        val calendars = listOf(TimeCalendar.Default, TimeCalendar.EmptyOffset)

        calendars.forEach { calendar ->

            // simulate of same reservations
            val excludePeriods = TimePeriodCollection(
                DayRangeCollection(zonedDateTimeOf(2018, 3, 7), 2, calendar),
                DayRangeCollection(zonedDateTimeOf(2018, 3, 16), 2, calendar)
            )

            // the overall search range
            val limits = CalendarTimeRange(
                zonedDateTimeOf(2018, 3, 4),
                zonedDateTimeOf(2018, 3, 21),
                calendar
            )

            val days = DayRangeCollection(limits.start, limits.duration.toDays().toInt() + 1, calendar)

            // limits의 내부이고, 주말인 경우 제외기간에 추가합니다.
            days.daySequence().forEach { dr ->
                if (limits.hasInsideWith(dr) && dr.dayOfWeek.isWeekend())
                    excludePeriods.add(dr)
            }

            val calculator = TimeGapCalculator<ITimePeriod>(calendar)

            val gaps = calculator.gaps(excludePeriods, limits)
            gaps.forEach {
                log.trace { "gap=$it" }
            }

            gaps shouldHaveSize 4
            assertTrue {
                gaps[0].isSamePeriod(TimeRange(zonedDateTimeOf(2018, 3, 5), 2.days()))
            }
            assertTrue {
                gaps[1].isSamePeriod(TimeRange(zonedDateTimeOf(2018, 3, 9), 1.days()))
            }
            assertTrue {
                gaps[2].isSamePeriod(TimeRange(zonedDateTimeOf(2018, 3, 12), 4.days()))
            }
            assertTrue {
                gaps[3].isSamePeriod(TimeRange(zonedDateTimeOf(2018, 3, 19), 2.days()))
            }
        }
    }
}
