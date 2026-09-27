package io.bluetape4k.javatimes.range

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessThan
import io.bluetape4k.assertions.shouldHaveSize
import io.bluetape4k.assertions.shouldNotBeEmpty
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TemporalClosedRangeSupportTest {

    companion object: KLogging()

    private val start: LocalDateTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
    private val end: LocalDateTime = LocalDateTime.of(2024, 12, 31, 0, 0, 0)

    @Test
    fun `temporalClosedRangeOf creates range with correct start and end`() {
        val range = temporalClosedRangeOf(start, end)
        range.start shouldBeEqualTo start
        range.endInclusive shouldBeEqualTo end
    }

    @Test
    fun `rangeTo operator creates closed range`() {
        val range = start..end
        range.start shouldBeEqualTo start
        range.endInclusive shouldBeEqualTo end
    }

    @Test
    fun `windowed years produces windows`() {
        val s = LocalDateTime.of(2020, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 1, 1, 0, 0)
        val range = s..e
        val windows = range.windowedYears(2, 1).toList()
        windows.shouldNotBeEmpty()
        windows[0] shouldHaveSize 2
    }

    @Test
    fun `windowed months produces windows`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 6, 1, 0, 0)
        val range = s..e
        val windows = range.windowedMonths(2, 1).toList()
        windows.shouldNotBeEmpty()
    }

    @Test
    fun `windowed days produces windows`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 1, 10, 0, 0)
        val range = s..e
        val windows = range.windowedDays(3, 1).toList()
        windows.shouldNotBeEmpty()
        windows[0] shouldHaveSize 3
    }

    @Test
    fun `windowed hours produces windows`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 1, 1, 5, 0)
        val range = s..e
        val windows = range.windowedHours(3, 1).toList()
        windows.shouldNotBeEmpty()
    }

    @Test
    fun `windowed minutes produces windows`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 1, 1, 0, 30)
        val range = s..e
        val windows = range.windowedMinutes(5, 1).toList()
        windows.shouldNotBeEmpty()
    }

    @Test
    fun `windowed seconds produces windows`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
        val e = LocalDateTime.of(2024, 1, 1, 0, 0, 10)
        val range = s..e
        val windows = range.windowedSeconds(3, 1).toList()
        windows.shouldNotBeEmpty()
    }

    @Test
    fun `chunked years produces chunks`() {
        val s = LocalDateTime.of(2020, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 1, 1, 0, 0)
        val range = s..e
        val chunks = range.chunkedYears(2).toList()
        chunks.shouldNotBeEmpty()
        chunks[0] shouldHaveSize 2
    }

    @Test
    fun `chunked months produces chunks`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 6, 1, 0, 0)
        val range = s..e
        val chunks = range.chunkedMonths(2).toList()
        chunks.shouldNotBeEmpty()
    }

    @Test
    fun `chunked days produces chunks`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 1, 10, 0, 0)
        val range = s..e
        val chunks = range.chunkedDays(3).toList()
        chunks.shouldNotBeEmpty()
    }

    @Test
    fun `chunked hours produces chunks`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 1, 1, 6, 0)
        val range = s..e
        val chunks = range.chunkedHours(2).toList()
        chunks.shouldNotBeEmpty()
    }

    @Test
    fun `chunked minutes produces chunks`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 1, 1, 0, 30)
        val range = s..e
        val chunks = range.chunkedMinutes(5).toList()
        chunks.shouldNotBeEmpty()
    }

    @Test
    fun `chunked seconds produces chunks`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
        val e = LocalDateTime.of(2024, 1, 1, 0, 0, 15)
        val range = s..e
        val chunks = range.chunkedSeconds(3).toList()
        chunks.shouldNotBeEmpty()
    }

    @Test
    fun `zipWithNextYear produces pairs`() {
        val s = LocalDateTime.of(2020, 1, 1, 0, 0)
        val e = LocalDateTime.of(2023, 1, 1, 0, 0)
        val range = s..e
        val pairs = range.zipWithNextYear().toList()
        pairs.shouldNotBeEmpty()
        pairs[0].first shouldBeLessThan pairs[0].second
    }

    @Test
    fun `zipWithNextMonth produces pairs`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 4, 1, 0, 0)
        val range = s..e
        val pairs = range.zipWithNextMonth().toList()
        pairs.shouldNotBeEmpty()
    }

    @Test
    fun `zipWithNextDay produces pairs`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 1, 5, 0, 0)
        val range = s..e
        val pairs = range.zipWithNextDay().toList()
        pairs.shouldNotBeEmpty()
        pairs[0].first shouldBeLessThan pairs[0].second
    }

    @Test
    fun `zipWithNextHour produces pairs`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 1, 1, 4, 0)
        val range = s..e
        val pairs = range.zipWithNextHour().toList()
        pairs.shouldNotBeEmpty()
    }

    @Test
    fun `zipWithNextMinute produces pairs`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0)
        val e = LocalDateTime.of(2024, 1, 1, 0, 4)
        val range = s..e
        val pairs = range.zipWithNextMinute().toList()
        pairs.shouldNotBeEmpty()
    }

    @Test
    fun `zipWithNextSecond produces pairs`() {
        val s = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
        val e = LocalDateTime.of(2024, 1, 1, 0, 0, 4)
        val range = s..e
        val pairs = range.zipWithNextSecond().toList()
        pairs.shouldNotBeEmpty()
    }
}
