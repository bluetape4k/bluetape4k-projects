package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.EmptyDuration
import io.bluetape4k.javatimes.MaxPeriodTime
import io.bluetape4k.javatimes.MinPeriodTime
import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Duration

class TimeCalendarTest {

    companion object : KLogging()

    @Test
    fun `Default calendar has correct offsets`() {
        val calendar = TimeCalendar.Default
        calendar.startOffset shouldBeEqualTo Duration.ZERO
        (calendar.endOffset.isNegative).shouldBeTrue()
    }

    @Test
    fun `EmptyOffset calendar has zero offsets`() {
        val calendar = TimeCalendar.EmptyOffset
        calendar.startOffset shouldBeEqualTo EmptyDuration
        calendar.endOffset shouldBeEqualTo EmptyDuration
    }

    @Test
    fun `Default calendar firstDayOfWeek is MONDAY`() {
        val calendar = TimeCalendar.Default
        calendar.firstDayOfWeek shouldBeEqualTo DayOfWeek.MONDAY
    }

    @Test
    fun `mapStart adds startOffset to moment`() {
        val calendar = TimeCalendar.Default
        val now = nowZonedDateTime()
        val mapped = calendar.mapStart(now)
        mapped shouldBeEqualTo now + calendar.startOffset
    }

    @Test
    fun `mapEnd adds endOffset to moment`() {
        val calendar = TimeCalendar.Default
        val now = nowZonedDateTime()
        val mapped = calendar.mapEnd(now)
        mapped shouldBeEqualTo now + calendar.endOffset
    }

    @Test
    fun `mapStart with MinPeriodTime returns unchanged`() {
        val calendar = TimeCalendar.Default
        val mapped = calendar.mapStart(MinPeriodTime)
        mapped shouldBeEqualTo MinPeriodTime
    }

    @Test
    fun `mapEnd with MaxPeriodTime returns unchanged`() {
        val calendar = TimeCalendar.Default
        val mapped = calendar.mapEnd(MaxPeriodTime)
        mapped shouldBeEqualTo MaxPeriodTime
    }

    @Test
    fun `unmapStart reverses mapStart`() {
        val calendar = TimeCalendar.Default
        val now = nowZonedDateTime()
        val mapped = calendar.mapStart(now)
        val unmapped = calendar.unmapStart(mapped)
        unmapped shouldBeEqualTo now
    }

    @Test
    fun `unmapEnd reverses mapEnd`() {
        val calendar = TimeCalendar.Default
        val now = nowZonedDateTime()
        val mapped = calendar.mapEnd(now)
        val unmapped = calendar.unmapEnd(mapped)
        unmapped shouldBeEqualTo now
    }

    @Test
    fun `equals for same config`() {
        val a = TimeCalendar(TimeCalendarConfig.Default)
        val b = TimeCalendar(TimeCalendarConfig.Default)
        a shouldBeEqualTo b
    }

    @Test
    fun `hashCode consistent with equals`() {
        val a = TimeCalendar(TimeCalendarConfig.Default)
        val b = TimeCalendar(TimeCalendarConfig.Default)
        a.hashCode() shouldBeEqualTo b.hashCode()
    }

    @Test
    fun `factory method invoke creates same as constructor`() {
        val a = TimeCalendar(TimeCalendarConfig.Default)
        val b = TimeCalendar.invoke(TimeCalendarConfig.Default)
        a shouldBeEqualTo b
    }

    @Test
    fun `factory method of creates same as constructor`() {
        val a = TimeCalendar(TimeCalendarConfig.Default)
        val b = TimeCalendar.of(TimeCalendarConfig.Default)
        a shouldBeEqualTo b
    }

    @Test
    fun `EmptyOffset mapStart does not shift moment`() {
        val calendar = TimeCalendar.EmptyOffset
        val now = nowZonedDateTime()
        calendar.mapStart(now) shouldBeEqualTo now
    }

    @Test
    fun `EmptyOffset mapEnd does not shift moment`() {
        val calendar = TimeCalendar.EmptyOffset
        val now = nowZonedDateTime()
        calendar.mapEnd(now) shouldBeEqualTo now
    }
}
