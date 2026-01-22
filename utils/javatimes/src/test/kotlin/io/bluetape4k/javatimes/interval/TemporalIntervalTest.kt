package io.bluetape4k.javatimes.interval

import io.bluetape4k.javatimes.dayPeriod
import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.hours
import io.bluetape4k.javatimes.interval.ReadableTemporalInterval.Companion.SEPARATOR
import io.bluetape4k.javatimes.nowOffsetDateTime
import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.toInstant
import io.bluetape4k.junit5.random.RandomValue
import io.bluetape4k.junit5.random.RandomizedTest
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeNull
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.math.absoluteValue

@RandomizedTest
class TemporalIntervalTest {

    companion object: KLogging() {
        private const val REPEAT_SIZE = 5
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `create with milliseconds`(@RandomValue time1: Long, @RandomValue time2: Long) {
        val start = minOf(time1.absoluteValue, time2.absoluteValue)
        val end = maxOf(time1.absoluteValue, time2.absoluteValue)

        with(temporalIntervalOf(start.toInstant(), end.toInstant())) {
            this.startInclusive.toEpochMilli() shouldBeEqualTo start
            this.endExclusive.toEpochMilli() shouldBeEqualTo end
            zoneId shouldBeEqualTo ZoneOffset.UTC
            toDurationMillis() shouldBeEqualTo (end - start)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `create with reverse input`(@RandomValue time1: Long, @RandomValue time2: Long) {
        val start = minOf(time1.absoluteValue, time2.absoluteValue)
        val end = maxOf(time1.absoluteValue, time2.absoluteValue)

        with(temporalIntervalOf(end.toInstant(), start.toInstant())) {
            this.startInclusive.toEpochMilli() shouldBeEqualTo start
            this.endExclusive.toEpochMilli() shouldBeEqualTo end
            zoneId shouldBeEqualTo ZoneOffset.UTC
            toDurationMillis() shouldBeEqualTo (end - start)
        }
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `create with start and period`(@RandomValue n: Int) {
        val days = n.absoluteValue % 365 + 1
        log.trace { "days=$days" }

        val start = nowZonedDateTime(ZoneOffset.UTC)
        val period = Period.ofDays(days)

        val interval = temporalIntervalOf(start, period)

        interval.startInclusive shouldBeEqualTo start
        interval.endExclusive shouldBeEqualTo (start + period)
        interval.zoneId shouldBeEqualTo ZoneOffset.UTC
        interval.toDuration() shouldBeEqualTo Duration.ofDays(days.toLong())
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `create with start and duration`(@RandomValue n: Long) {
        val hours = n.absoluteValue % 100 + 1
        log.trace { "hours=$hours" }

        val start = nowZonedDateTime(ZoneOffset.UTC)
        val duration = Duration.ofHours(hours)

        val interval = temporalIntervalOf(start, duration)

        interval.startInclusive shouldBeEqualTo start
        interval.endExclusive shouldBeEqualTo (start + duration)
        interval.zoneId shouldBeEqualTo ZoneOffset.UTC
        interval.toDuration() shouldBeEqualTo duration
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `create with period and end`(@RandomValue n: Int) {
        val days = n.absoluteValue % 100 + 1
        log.trace { "days=$days" }

        val end = nowZonedDateTime(ZoneOffset.UTC)
        val period = Period.ofDays(days)

        val interval = temporalIntervalOf(period, end)

        interval.startInclusive shouldBeEqualTo end - period
        interval.endExclusive shouldBeEqualTo end
        interval.zoneId shouldBeEqualTo ZoneOffset.UTC
        interval.toDuration() shouldBeEqualTo Duration.ofDays(days.toLong())
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `create with duration and end`(@RandomValue n: Long) {
        val hours = n.absoluteValue % 100 + 1
        log.trace { "hours=$hours" }

        val end = nowZonedDateTime(ZoneOffset.UTC)
        val duration = Duration.ofHours(hours)

        val interval = temporalIntervalOf(duration, end)

        interval.startInclusive shouldBeEqualTo end - duration
        interval.endExclusive shouldBeEqualTo end
        interval.zoneId shouldBeEqualTo ZoneOffset.UTC
        interval.toDuration() shouldBeEqualTo duration
    }

    @Test
    fun `overlap with intervals`() {
        val interval1 = TemporalInterval(0.toInstant(), 100.toInstant())
        val interval2 = TemporalInterval(50.toInstant(), 150.toInstant())
        val interval3 = TemporalInterval(200.toInstant(), 300.toInstant())

        interval1.overlaps(interval2).shouldBeTrue()
        interval1.overlaps(interval3).shouldBeFalse()

        interval1.overlap(interval2) shouldBeEqualTo TemporalInterval(50.toInstant(), 100.toInstant())
        interval1.overlap(interval3).shouldBeNull()
    }

    @Test
    fun `gap with intervals`() {
        val interval1 = TemporalInterval(0.toInstant(), 100.toInstant())
        val interval2 = TemporalInterval(50.toInstant(), 150.toInstant())
        val interval3 = TemporalInterval(200.toInstant(), 300.toInstant())

        interval1.gap(interval2).shouldBeNull()
        interval1.gap(interval3) shouldBeEqualTo TemporalInterval(100.toInstant(), 200.toInstant())
    }

    @Test
    fun `abuts two intervals - 두 Interval이 연속하는지 여부`() {
        val interval1 = InstantInterval(0.toInstant(), 100.toInstant())
        val interval2 = InstantInterval(50.toInstant(), 150.toInstant())
        val interval3 = InstantInterval(200.toInstant(), 300.toInstant())
        val interval4 = InstantInterval(100.toInstant(), 200.toInstant())
        val interval5 = InstantInterval(100.toInstant(), 300.toInstant())

        interval1.abuts(interval2).shouldBeFalse()
        interval1.abuts(interval3).shouldBeFalse()
        interval1.abuts(interval4).shouldBeTrue()
        interval3.abuts(interval4).shouldBeTrue()
        interval4.abuts(interval5).shouldBeFalse()      // 연속하는 것이 아니라 start가 같다
    }

    @Test
    fun `change with start or end`() {
        val interval = TemporalInterval(0.toInstant(), 100.toInstant())

        interval.withStart(50.toInstant()) shouldBeEqualTo temporalIntervalOf(50.toInstant(), 100.toInstant())
        interval.withEnd(200.toInstant()) shouldBeEqualTo temporalIntervalOf(0.toInstant(), 200.toInstant())
        interval.withStart(50.toInstant()).withEnd(200.toInstant()) shouldBeEqualTo temporalIntervalOf(
            50.toInstant(),
            200.toInstant()
        )
    }

    @Test
    fun `change by period`() {
        val start = nowZonedDateTime()
        val interval = TemporalInterval(start, start + 100.days())

        interval.withAmountBeforeEnd(50.dayPeriod()) shouldBeEqualTo temporalIntervalOf(
            50.dayPeriod(),
            start + 100.days()
        )
        interval.withAmountAfterStart(200.dayPeriod()) shouldBeEqualTo temporalIntervalOf(start, 200.days())
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse TemporalInterval instance with zoneId is system default`(@RandomValue zoneId: ZoneId) {

        val start = nowZonedDateTime(zoneId)

        val expected = TemporalInterval(start, start + 100.days())

        val str = expected.toString()
        log.trace { "interval=$str" }
        val actual = TemporalInterval.parse(str)

        actual shouldBeEqualTo expected
    }

    @RepeatedTest(REPEAT_SIZE)
    fun `parse by offset`(@RandomValue zoneId: ZoneId) {
        val start = nowOffsetDateTime(zoneId)
        val end = start + 1.hours()

        val text = "$start $SEPARATOR $end"

        val parsed = TemporalInterval.parseWithOffset(text)

        log.trace { "text=$text, parsed=$parsed" }

        parsed.startInclusive.toOffsetDateTime() shouldBeEqualTo start
        parsed.endExclusive.toOffsetDateTime() shouldBeEqualTo end
    }
}
