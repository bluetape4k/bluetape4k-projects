package io.bluetape4k.javatimes.period.calendars

import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.hours
import io.bluetape4k.javatimes.nanos
import io.bluetape4k.javatimes.period.AbstractPeriodTest
import io.bluetape4k.javatimes.period.SeekBoundaryMode
import io.bluetape4k.javatimes.period.TimeRange
import io.bluetape4k.javatimes.todayZonedDateTime
import io.bluetape4k.javatimes.zonedDateTimeOf
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNull
import org.junit.jupiter.api.Test
import java.time.Duration

class DateAddTest: AbstractPeriodTest() {

    companion object: KLogging()

    @Test
    fun `simple add duration`() = runTest {
        val dateAdd = DateAdd()
        val today = todayZonedDateTime()

        dateAdd.add(today, Duration.ZERO) shouldBeEqualTo today
        dateAdd.add(today, 1.days()) shouldBeEqualTo today.plusDays(1)
        dateAdd.add(today, (-1).days()) shouldBeEqualTo today.minusDays(1)

        dateAdd.subtract(today, 1.days()) shouldBeEqualTo today.minusDays(1)
        dateAdd.subtract(today, (-1).days()) shouldBeEqualTo today.plusDays(1)
    }

    @Test
    fun `date add with exclude periods`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 12)
        val period1 = TimeRange(
            zonedDateTimeOf(2011, 4, 20),
            zonedDateTimeOf(2011, 4, 25)
        )
        val period2 = TimeRange(
            zonedDateTimeOf(2011, 4, 30),
            null
        ) // 4월 30일 이후

        // 예외기간을 설정합니다.
        // 4월 20일 ~ 40월 25일, 4월 30일 이후
        val dateAdd = DateAdd().apply {
            excludePeriods.add(period1)
            excludePeriods.add(period2)
        }

        dateAdd.add(start, 1.days()) shouldBeEqualTo start.plusDays(1)

        // 4월 12일에 8일을 더하면 4월 20일이지만, 20~25일까지 제외되므로, 4월 25일이 된다.
        dateAdd.add(start, 8.days()) shouldBeEqualTo period1.end

        // 4월 12일에 20일을 더하면 4월 20~25일을 제외한 후 계산하면 4월 30 이후가 된다. (5월 3일)
        // 하지만 4월 30일 이후는 모두 제외되므로 결과값은 null 이다.
        dateAdd.add(start, 20.days()).shouldBeNull()

        dateAdd.subtract(start, 3.days()) shouldBeEqualTo start.minusDays(3)
    }

    @Test
    fun `date subtract with exclude periods`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 30)
        val period1 = TimeRange(
            zonedDateTimeOf(2011, 4, 20),
            zonedDateTimeOf(2011, 4, 25)
        )
        val period2 = TimeRange(
            null,
            zonedDateTimeOf(2011, 4, 6)
        ) // ~ 4월 6일까지

        val dateAdd = DateAdd().apply {
            excludePeriods.add(period1)
            excludePeriods.add(period2)
        }

        dateAdd.subtract(start, 1.days()) shouldBeEqualTo start.minusDays(1)

        // 4월 30일로부터 5일 전이면 4월 25일이지만, 예외기간이므로 4월 20일이 된다.
        dateAdd.subtract(start, 5.days()) shouldBeEqualTo period1.start

        // 4월 30일로부터 20일 전이면 4월 10일이지만, 예외기간 때문에 4월 5일이 된다. 근데 4월 6일 이전은 모두 제외기간이므로 null 을 반환한다.
        dateAdd.subtract(start, 20.days()).shouldBeNull()
    }

    @Test
    fun `include period with outside max`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 12)
        val period = TimeRange(
            zonedDateTimeOf(2011, 4, 20),
            null
        )

        val dateAdd = DateAdd().apply {
            includePeriods.add(period)
        }

        dateAdd.add(start, 0.days()) shouldBeEqualTo period.start
        dateAdd.add(start, 1.days()) shouldBeEqualTo period.start.plusDays(1)

        dateAdd.subtract(start, 0.days()).shouldBeNull()
        dateAdd.subtract(start, 1.days()).shouldBeNull()
    }

    @Test
    fun `include period with outside min`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 12)
        val period = TimeRange(
            null,
            zonedDateTimeOf(2011, 4, 10)
        )

        val dateAdd = DateAdd().apply {
            includePeriods.add(period)
        }

        dateAdd.add(start, 0.days()).shouldBeNull()
        dateAdd.add(start, 1.days()).shouldBeNull()

        dateAdd.subtract(start, 0.days()) shouldBeEqualTo period.end
        dateAdd.subtract(start, 1.days()) shouldBeEqualTo period.end.minusDays(1)
    }

    @Test
    fun `include period is same with exclude period`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 12)
        val period = TimeRange(
            zonedDateTimeOf(2011, 4, 10),
            zonedDateTimeOf(2011, 4, 20)
        )

        val dateAdd = DateAdd().apply {
            includePeriods.add(period)
            excludePeriods.add(period)
        }

        // include 와 exclude 가 같기 때문에, 유효한 일자는 없다.
        dateAdd.add(start, 0.days()).shouldBeNull()
        dateAdd.add(start, 2000.days()).shouldBeNull()
        dateAdd.subtract(start, 2000.days()).shouldBeNull()
    }

    @Test
    fun `include period is same with two exclude period`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 12)
        val period1 = TimeRange(
            zonedDateTimeOf(2011, 4, 10),
            zonedDateTimeOf(2011, 4, 20)
        )
        val period2 = TimeRange(
            zonedDateTimeOf(2011, 4, 10),
            zonedDateTimeOf(2011, 4, 15)
        )
        val period3 = TimeRange(
            zonedDateTimeOf(2011, 4, 15),
            zonedDateTimeOf(2011, 4, 20)
        )


        val dateAdd = DateAdd().apply {
            includePeriods.add(period1)

            excludePeriods.add(period2)
            excludePeriods.add(period3)
        }

        // include 와 exclude 가 같기 때문에, 유효한 일자는 없다.
        dateAdd.add(start, 0.days()).shouldBeNull()
        dateAdd.add(start, 2000.days()).shouldBeNull()
        dateAdd.subtract(start, 2000.days()).shouldBeNull()
    }

    @Test
    fun `include period is end touching with exclude period`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 12)
        val period1 = TimeRange(
            zonedDateTimeOf(2011, 4, 10),
            zonedDateTimeOf(2011, 4, 20)
        )
        val period2 = TimeRange(
            zonedDateTimeOf(2011, 4, 15),
            zonedDateTimeOf(2011, 4, 20)
        )

        val dateAdd = DateAdd().apply {
            includePeriods.add(period1)
            excludePeriods.add(period2)
        }

        dateAdd.add(start, 0.days()) shouldBeEqualTo start
        dateAdd.add(start, 1.days()) shouldBeEqualTo start.plusDays(1)    // 4-13
        dateAdd.add(start, 2.days()) shouldBeEqualTo start.plusDays(2)    // 4-14
        dateAdd.add(start, 3.days()).shouldBeNull()
    }

    @Test
    fun `start of period is same with start`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 12)
        val period = TimeRange(start)
        val dateAdd = DateAdd()

        dateAdd.add(start, 0.days()) shouldBeEqualTo start

        dateAdd.includePeriods.add(period)
        dateAdd.add(start, 0.days()) shouldBeEqualTo start

        dateAdd.excludePeriods.add(period)
        dateAdd.add(start, 0.days()) shouldBeEqualTo start

        dateAdd.includePeriods.clear()
        dateAdd.add(start, 0.days()) shouldBeEqualTo start

        dateAdd.excludePeriods.clear()
        dateAdd.add(start, 0.days()) shouldBeEqualTo start
    }

    @Test
    fun `only include period`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 12)
        val period = TimeRange(zonedDateTimeOf(2011, 4, 1), MaxPeriodTime)

        val dateAdd = DateAdd().apply {
            includePeriods.add(period)
        }

        dateAdd.add(start, 0.days()) shouldBeEqualTo start
        dateAdd.add(start, 1.days()) shouldBeEqualTo start.plusDays(1)
        dateAdd.add(start, 365.days()) shouldBeEqualTo start.plusDays(365)
        dateAdd.add(start, 99999.days()) shouldBeEqualTo start.plusDays(99999)
    }

    @Test
    fun `two include periods which is split`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 12)
        val period1 = TimeRange(
            zonedDateTimeOf(2011, 4, 1),
            zonedDateTimeOf(2011, 4, 15)
        )
        val period2 = TimeRange(
            zonedDateTimeOf(2011, 4, 20),
            zonedDateTimeOf(2011, 4, 24)
        )

        val dateAdd = DateAdd().apply {
            includePeriods.add(period1)
            includePeriods.add(period2)
        }

        dateAdd.add(start, 0.days()) shouldBeEqualTo start
        dateAdd.add(start, 1.days()) shouldBeEqualTo start.plusDays(1)            // 4-13
        dateAdd.add(start, 3.days()) shouldBeEqualTo period2.start                      // 4-20
        dateAdd.add(start, 5.days()) shouldBeEqualTo period2.start.plusDays(2)   // 4-22
        dateAdd.add(start, 6.days()) shouldBeEqualTo period2.start.plusDays(3)    // 4-23
        dateAdd.add(start, 7.days()).shouldBeNull()

        // NOTE: SeekBoundaryMode 에 따라 결과가 달라집니다.
        dateAdd.add(start, 7.days(), SeekBoundaryMode.FILL) shouldBeEqualTo period2.end
        dateAdd.add(start, 7.days(), SeekBoundaryMode.NEXT).shouldBeNull()
    }

    @Test
    fun `start is before exclude period`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 12)
        val period = TimeRange(
            zonedDateTimeOf(2011, 4, 15),
            zonedDateTimeOf(2011, 4, 20)
        )

        val dateAdd = DateAdd().apply {
            excludePeriods.add(period)
        }

        dateAdd.add(start, 0.days()) shouldBeEqualTo start
        dateAdd.add(start, 1.days()) shouldBeEqualTo start.plusDays(1)  // 4-13
        dateAdd.add(start, 2.days()) shouldBeEqualTo start.plusDays(2)  // 4-14
        dateAdd.add(start, 3.days()) shouldBeEqualTo period.end  // 4-20
        dateAdd.add(start, 3.days() + 1.nanos()) shouldBeEqualTo period.end + 1.nanos()  // 4-20 + 1.nanos()
        dateAdd.add(start, 5.days()) shouldBeEqualTo period.end.plusDays(2)
    }

    @Test
    fun `two exclude period with splitted`() = runTest {
        val start = zonedDateTimeOf(2011, 4, 12)
        val period1 = TimeRange(
            zonedDateTimeOf(2011, 4, 15),
            zonedDateTimeOf(2011, 4, 20)
        )
        val period2 = TimeRange(
            zonedDateTimeOf(2011, 4, 22),
            zonedDateTimeOf(2011, 4, 25)
        )

        val dateAdd = DateAdd().apply {
            excludePeriods.add(period1)
            excludePeriods.add(period2)
        }

        dateAdd.add(start, 0.days()) shouldBeEqualTo start
        dateAdd.add(start, 1.days()) shouldBeEqualTo start.plusDays(1)    // 4-13
        dateAdd.add(start, 2.days()) shouldBeEqualTo start.plusDays(2)    // 4-14
        dateAdd.add(start, 3.days()) shouldBeEqualTo period1.end  // 4-20
        dateAdd.add(start, 4.days()) shouldBeEqualTo period1.end.plusDays(1)  // 4-21
        dateAdd.add(start, 5.days()) shouldBeEqualTo period2.end  // 4-25
        dateAdd.add(start, 6.days()) shouldBeEqualTo period2.end.plusDays(1)  // 4-26
        dateAdd.add(start, 7.days()) shouldBeEqualTo period2.end.plusDays(2)  // 4-27
    }


    @Test
    fun `include period and exclude period is same with start is same with period start`() = runTest {
        val start = zonedDateTimeOf(2011, 3, 5)
        val period1 = TimeRange(
            zonedDateTimeOf(2011, 3, 5),
            zonedDateTimeOf(2011, 3, 10)
        )
        val period2 = TimeRange(
            zonedDateTimeOf(2011, 3, 5),
            zonedDateTimeOf(2011, 3, 10)
        )

        val dateAdd = DateAdd().apply {
            includePeriods.add(period1)
            excludePeriods.add(period2)
        }

        dateAdd.add(start, 0.days()).shouldBeNull()
        dateAdd.add(start, 1.nanos()).shouldBeNull()
        dateAdd.add(start, (-1).nanos()).shouldBeNull()

        dateAdd.subtract(start, 0.days()).shouldBeNull()
        dateAdd.subtract(start, 1.nanos()).shouldBeNull()
        dateAdd.subtract(start, (-1).nanos()).shouldBeNull()
    }

    @Test
    fun `포함기간 내에 여러 예외기간이 있을 때`() = runTest {
        val dateAdd = DateAdd().apply {
            includePeriods.add(TimeRange(zonedDateTimeOf(2011, 3, 17), zonedDateTimeOf(2011, 4, 20)))

            excludePeriods.add(TimeRange(zonedDateTimeOf(2011, 3, 22), zonedDateTimeOf(2011, 3, 25)))
            excludePeriods.add(TimeRange(zonedDateTimeOf(2011, 4, 1), zonedDateTimeOf(2011, 4, 7)))
            excludePeriods.add(TimeRange(zonedDateTimeOf(2011, 4, 15), zonedDateTimeOf(2011, 4, 16)))
        }

        // positive
        val start = zonedDateTimeOf(2011, 3, 19)

        dateAdd.add(start, 1.hours()) shouldBeEqualTo start.plusHours(1)
        dateAdd.add(start, 4.days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 26)
        dateAdd.add(start, 17.days()) shouldBeEqualTo zonedDateTimeOf(2011, 4, 14)
        dateAdd.add(start, 20.days()) shouldBeEqualTo zonedDateTimeOf(2011, 4, 18)

        dateAdd.add(start, 22.days(), SeekBoundaryMode.FILL) shouldBeEqualTo zonedDateTimeOf(2011, 4, 20)
        dateAdd.add(start, 22.days(), SeekBoundaryMode.NEXT).shouldBeNull()
        dateAdd.add(start, 22.days()).shouldBeNull()

        // negative
        val end = zonedDateTimeOf(2011, 4, 18)
        dateAdd.add(end, (-1).hours()) shouldBeEqualTo end.minusHours(1)
        dateAdd.add(end, (-4).days()) shouldBeEqualTo zonedDateTimeOf(2011, 4, 13)
        dateAdd.add(end, (-17).days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 22)
        dateAdd.add(end, (-20).days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 19)

        dateAdd.add(end, (-22).days(), SeekBoundaryMode.FILL) shouldBeEqualTo zonedDateTimeOf(2011, 3, 17)
        dateAdd.add(end, (-22).days(), SeekBoundaryMode.NEXT).shouldBeNull()
        dateAdd.add(end, (-22).days()).shouldBeNull()
    }

    @Test
    fun `포함기간과 예외기간이 연속으로 있는 경우`() = runTest {
        val dateAdd = DateAdd().apply {
            includePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 1),
                    zonedDateTimeOf(2011, 3, 5)
                )
            )
            excludePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 5),
                    zonedDateTimeOf(2011, 3, 10)
                )
            )
            includePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 10),
                    zonedDateTimeOf(2011, 3, 15)
                )
            )
            excludePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 15),
                    zonedDateTimeOf(2011, 3, 20)
                )
            )
            includePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 20),
                    zonedDateTimeOf(2011, 3, 25)
                )
            )
        }

        val start = zonedDateTimeOf(2011, 3, 1)
        val end = zonedDateTimeOf(2011, 3, 25)

        // add from start
        dateAdd.add(start, 0.days()) shouldBeEqualTo start
        dateAdd.add(start, 1.days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 2)
        dateAdd.add(start, 3.days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 4)
        dateAdd.add(start, 4.days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 10)
        dateAdd.add(start, 5.days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 11)
        dateAdd.add(start, 8.days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 14)
        dateAdd.add(start, 9.days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 20)
        dateAdd.add(start, 10.days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 21)

        dateAdd.add(start, 14.days(), SeekBoundaryMode.FILL) shouldBeEqualTo zonedDateTimeOf(2011, 3, 25)
        dateAdd.add(start, 14.days(), SeekBoundaryMode.NEXT).shouldBeNull()
        dateAdd.add(start, 14.days()).shouldBeNull()

        // add from endInclusive
        dateAdd.add(end, 0.days()) shouldBeEqualTo end
        dateAdd.add(end, (-1).days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 24)
        dateAdd.add(end, (-5).days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 15)
        dateAdd.add(end, (-6).days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 14)
        dateAdd.add(end, (-10).days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 5)
        dateAdd.add(end, (-11).days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 4)

        dateAdd.add(end, (-14).days(), SeekBoundaryMode.FILL) shouldBeEqualTo zonedDateTimeOf(2011, 3, 1)
        dateAdd.add(end, (-14).days(), SeekBoundaryMode.NEXT).shouldBeNull()
        dateAdd.add(end, (-14).days()).shouldBeNull()

        // subtract from endInclusive
        dateAdd.subtract(end, 0.days()) shouldBeEqualTo end
        dateAdd.subtract(end, 1.days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 24)
        dateAdd.subtract(end, 5.days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 15)
        dateAdd.subtract(end, 6.days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 14)
        dateAdd.subtract(end, 10.days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 5)
        dateAdd.subtract(end, 11.days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 4)

        dateAdd.subtract(end, 14.days(), SeekBoundaryMode.FILL) shouldBeEqualTo zonedDateTimeOf(2011, 3, 1)
        dateAdd.subtract(end, 14.days(), SeekBoundaryMode.NEXT).shouldBeNull()
        dateAdd.subtract(end, 14.days()).shouldBeNull()

        // subtract from start
        dateAdd.subtract(start, 0.days()) shouldBeEqualTo start
        dateAdd.subtract(start, (-1).days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 2)
        dateAdd.subtract(start, (-3).days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 4)
        dateAdd.subtract(start, (-4).days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 10)
        dateAdd.subtract(start, (-5).days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 11)
        dateAdd.subtract(start, (-8).days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 14)
        dateAdd.subtract(start, (-9).days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 20)
        dateAdd.subtract(start, (-10).days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 21)

        dateAdd.subtract(start, (-14).days(), SeekBoundaryMode.FILL) shouldBeEqualTo zonedDateTimeOf(2011, 3, 25)
        dateAdd.subtract(start, (-14).days(), SeekBoundaryMode.NEXT).shouldBeNull()
        dateAdd.subtract(start, (-14).days()).shouldBeNull()
    }

    @Test
    fun `예외기간과 포함기간이 연속으로 있는 경우 2`() = runTest {
        val dateAdd = DateAdd().apply {
            excludePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 5),
                    zonedDateTimeOf(2011, 3, 10)
                )
            )
            includePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 10),
                    zonedDateTimeOf(2011, 3, 15)
                )
            )
            excludePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 15),
                    zonedDateTimeOf(2011, 3, 20)
                )
            )
        }
        val start = zonedDateTimeOf(2011, 3, 10)

        dateAdd.add(start, 0.days()) shouldBeEqualTo start
        dateAdd.add(start, 1.days()) shouldBeEqualTo start.plusDays(1)
        dateAdd.add(start, 5.days(), SeekBoundaryMode.FILL) shouldBeEqualTo start.plusDays(5)
        dateAdd.add(start, 5.days()).shouldBeNull()
    }

    @Test
    fun `포함기간과 예외기간이 같은 경우`() = runTest {
        val dateAdd = DateAdd().apply {
            includePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 10),
                    zonedDateTimeOf(2011, 3, 20)
                )
            )
            excludePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 10),
                    zonedDateTimeOf(2011, 3, 15)
                )
            )
            excludePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 15),
                    zonedDateTimeOf(2011, 3, 20)
                )
            )
        }

        val start = zonedDateTimeOf(2011, 3, 10)

        dateAdd.add(start, 0.days()).shouldBeNull()
        dateAdd.add(start, 1.days()).shouldBeNull()
        dateAdd.add(start, 5.days()).shouldBeNull()
    }

    @Test
    fun `포함기간이 예외기간 안에 있을 경우`() = runTest {
        val dateAdd = DateAdd().apply {
            includePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 10),
                    zonedDateTimeOf(2011, 3, 20)
                )
            )
            excludePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 5),
                    zonedDateTimeOf(2011, 3, 15)
                )
            )
            excludePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 15),
                    zonedDateTimeOf(2011, 3, 30)
                )
            )
        }

        val start = zonedDateTimeOf(2011, 3, 10)

        dateAdd.add(start, 0.days()).shouldBeNull()
        dateAdd.add(start, 1.days()).shouldBeNull()
        dateAdd.add(start, (-1).days()).shouldBeNull()

        dateAdd.subtract(start, 0.days()).shouldBeNull()
        dateAdd.subtract(start, 1.days()).shouldBeNull()
        dateAdd.subtract(start, (-1).days()).shouldBeNull()
    }

    @Test
    fun `두개의 예외기간 사이에 포함기간이 있을 경우`() = runTest {
        val dateAdd = DateAdd().apply {
            includePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 10),
                    zonedDateTimeOf(2011, 3, 20)
                )
            )
            excludePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 5),
                    zonedDateTimeOf(2011, 3, 12)
                )
            )
            excludePeriods.add(
                TimeRange(
                    zonedDateTimeOf(2011, 3, 18),
                    zonedDateTimeOf(2011, 3, 30)
                )
            )
        }

        val start = zonedDateTimeOf(2011, 3, 10)

        dateAdd.add(start, 0.days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 12)
        dateAdd.add(start, 1.days()) shouldBeEqualTo zonedDateTimeOf(2011, 3, 13)
    }
}
