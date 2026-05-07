package io.bluetape4k.javatimes.period

import io.bluetape4k.javatimes.DefaultEndOffset
import io.bluetape4k.javatimes.DefaultStartOffset
import io.bluetape4k.javatimes.EmptyDuration
import io.bluetape4k.javatimes.FirstDayOfWeek
import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.DayOfWeek
import java.time.Duration
import java.util.Locale

class TimeCalendarConfigTest {

    companion object : KLogging()

    @Test
    fun `Default config has zero startOffset`() {
        val config = TimeCalendarConfig.Default
        config.startOffset shouldBeEqualTo DefaultStartOffset
    }

    @Test
    fun `Default config has negative endOffset`() {
        val config = TimeCalendarConfig.Default
        config.endOffset shouldBeEqualTo DefaultEndOffset
        config.endOffset.isNegative.shouldBeTrue()
    }

    @Test
    fun `Default config firstDayOfWeek is MONDAY`() {
        val config = TimeCalendarConfig.Default
        config.firstDayOfWeek shouldBeEqualTo FirstDayOfWeek
        config.firstDayOfWeek shouldBeEqualTo DayOfWeek.MONDAY
    }

    @Test
    fun `EmptyOffset config has zero startOffset and endOffset`() {
        val config = TimeCalendarConfig.EmptyOffset
        config.startOffset shouldBeEqualTo EmptyDuration
        config.endOffset shouldBeEqualTo EmptyDuration
    }

    @Test
    fun `custom config with specified values`() {
        val customOffset = Duration.ofMinutes(5)
        val config = TimeCalendarConfig(
            locale = Locale.US,
            startOffset = customOffset,
            endOffset = customOffset.negated(),
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )
        config.locale shouldBeEqualTo Locale.US
        config.startOffset shouldBeEqualTo customOffset
        config.endOffset shouldBeEqualTo customOffset.negated()
        config.firstDayOfWeek shouldBeEqualTo DayOfWeek.SUNDAY
    }

    @Test
    fun `equals for same config data`() {
        val a = TimeCalendarConfig.Default
        val b = TimeCalendarConfig()
        a shouldBeEqualTo b
    }

    @Test
    fun `hashCode consistent with equals`() {
        val a = TimeCalendarConfig.Default
        val b = TimeCalendarConfig()
        a.hashCode() shouldBeEqualTo b.hashCode()
    }

    @Test
    fun `EmptyOffset differs from Default`() {
        val default = TimeCalendarConfig.Default
        val empty = TimeCalendarConfig.EmptyOffset
        (default == empty).let { it.not().shouldBeTrue() }
    }

    @Test
    fun `data class copy creates new instance with modified field`() {
        val original = TimeCalendarConfig.Default
        val copy = original.copy(firstDayOfWeek = DayOfWeek.SUNDAY)
        copy.firstDayOfWeek shouldBeEqualTo DayOfWeek.SUNDAY
        copy.startOffset shouldBeEqualTo original.startOffset
    }
}
