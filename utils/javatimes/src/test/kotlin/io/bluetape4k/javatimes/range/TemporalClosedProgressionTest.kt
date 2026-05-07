package io.bluetape4k.javatimes.range

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class TemporalClosedProgressionTest {

    companion object : KLogging()

    private val start = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
    private val end = LocalDateTime.of(2024, 1, 5, 0, 0, 0)
    private val stepOneDay = Duration.ofDays(1)

    @Test
    fun `fromClosedRange creates progression with correct first and last`() {
        val progression = TemporalClosedProgression.fromClosedRange(start, end, stepOneDay)
        progression.first shouldBeEqualTo start
    }

    @Test
    fun `temporalClosedProgressionOf factory function works`() {
        val progression = temporalClosedProgressionOf(start, end, stepOneDay)
        progression.first shouldBeEqualTo start
    }

    @Test
    fun `iteration over days yields all 5 days inclusive`() {
        val progression = TemporalClosedProgression.fromClosedRange(start, end, stepOneDay)
        val items = progression.toList()
        items.size shouldBeEqualTo 5
        items[0] shouldBeEqualTo LocalDateTime.of(2024, 1, 1, 0, 0)
        items[4] shouldBeEqualTo LocalDateTime.of(2024, 1, 5, 0, 0)
    }

    @Test
    fun `progression is not empty for valid range`() {
        val progression = TemporalClosedProgression.fromClosedRange(start, end, stepOneDay)
        progression.isEmpty().shouldBeFalse()
    }

    @Test
    fun `single element progression when start equals end`() {
        val progression = TemporalClosedProgression.fromClosedRange(start, start, stepOneDay)
        val items = progression.toList()
        items.size shouldBeEqualTo 1
        items[0] shouldBeEqualTo start
    }

    @Test
    fun `step is accessible`() {
        val progression = TemporalClosedProgression.fromClosedRange(start, end, stepOneDay)
        progression.step shouldBeEqualTo stepOneDay
    }

    @Test
    fun `equals for identical progressions`() {
        val a = TemporalClosedProgression.fromClosedRange(start, end, stepOneDay)
        val b = TemporalClosedProgression.fromClosedRange(start, end, stepOneDay)
        a shouldBeEqualTo b
    }

    @Test
    fun `hashCode consistent with equals`() {
        val a = TemporalClosedProgression.fromClosedRange(start, end, stepOneDay)
        val b = TemporalClosedProgression.fromClosedRange(start, end, stepOneDay)
        a.hashCode() shouldBeEqualTo b.hashCode()
    }

    @Test
    fun `toString contains first and last`() {
        val progression = TemporalClosedProgression.fromClosedRange(start, end, stepOneDay)
        val str = progression.toString()
        str.isNotEmpty().shouldBeTrue()
    }

    @Test
    fun `hourly progression iterates correctly`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 1, 1, 4, 0)
        val step = Duration.ofHours(1)
        val progression = TemporalClosedProgression.fromClosedRange(s, e, step)
        val items = progression.toList()
        items.size shouldBeEqualTo 5
        items[0] shouldBeEqualTo s
        items[4] shouldBeEqualTo e
    }

    @Test
    fun `minute progression iterates correctly`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 1, 1, 0, 3)
        val step = Duration.ofMinutes(1)
        val progression = TemporalClosedProgression.fromClosedRange(s, e, step)
        val items = progression.toList()
        items.size shouldBeEqualTo 4
    }

    @Test
    fun `two-day step produces fewer elements`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 1, 9, 0, 0)
        val step = Duration.ofDays(2)
        val progression = TemporalClosedProgression.fromClosedRange(s, e, step)
        val items = progression.toList()
        // 1,3,5,7,9 = 5 items
        items.size shouldBeEqualTo 5
    }
}
