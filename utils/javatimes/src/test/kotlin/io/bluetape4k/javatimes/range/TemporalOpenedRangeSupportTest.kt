package io.bluetape4k.javatimes.range

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TemporalOpenedRangeSupportTest {

    companion object : KLogging()

    @Test
    fun `until operator creates opened range with correct start and end`() {
        val start = LocalDateTime.of(2024, 1, 1, 0, 0)
        val end = LocalDateTime.of(2024, 1, 10, 0, 0)
        val range = start until end
        range.start shouldBeEqualTo start
        range.endExclusive shouldBeEqualTo end
    }

    @Test
    fun `opened range contains start`() {
        val start = LocalDateTime.of(2024, 1, 1, 0, 0)
        val end = LocalDateTime.of(2024, 1, 10, 0, 0)
        val range = start until end
        range.contains(start).shouldBeTrue()
    }

    @Test
    fun `opened range does not contain end`() {
        val start = LocalDateTime.of(2024, 1, 1, 0, 0)
        val end = LocalDateTime.of(2024, 1, 10, 0, 0)
        val range = start until end
        range.contains(end).shouldBeFalse()
    }

    @Test
    fun `opened range contains midpoint`() {
        val start = LocalDateTime.of(2024, 1, 1, 0, 0)
        val end = LocalDateTime.of(2024, 1, 10, 0, 0)
        val mid = LocalDateTime.of(2024, 1, 5, 0, 0)
        val range = start until end
        range.contains(mid).shouldBeTrue()
    }

    @Test
    fun `opened range does not contain value before start`() {
        val start = LocalDateTime.of(2024, 1, 5, 0, 0)
        val end = LocalDateTime.of(2024, 1, 10, 0, 0)
        val before = LocalDateTime.of(2024, 1, 1, 0, 0)
        val range = start until end
        range.contains(before).shouldBeFalse()
    }

    @Test
    fun `opened range does not contain value after end`() {
        val start = LocalDateTime.of(2024, 1, 1, 0, 0)
        val end = LocalDateTime.of(2024, 1, 10, 0, 0)
        val after = LocalDateTime.of(2024, 1, 15, 0, 0)
        val range = start until end
        range.contains(after).shouldBeFalse()
    }

    @Test
    fun `isEmpty returns false for non-empty range`() {
        val start = LocalDateTime.of(2024, 1, 1, 0, 0)
        val end = LocalDateTime.of(2024, 1, 10, 0, 0)
        val range = start until end
        range.isEmpty().shouldBeFalse()
    }

    @Test
    fun `until with hour precision`() {
        val start = LocalDateTime.of(2024, 1, 1, 8, 0)
        val end = LocalDateTime.of(2024, 1, 1, 18, 0)
        val range = start until end
        range.contains(LocalDateTime.of(2024, 1, 1, 12, 0)).shouldBeTrue()
        range.contains(LocalDateTime.of(2024, 1, 1, 18, 0)).shouldBeFalse()
    }

    @Test
    fun `until with minute precision`() {
        val start = LocalDateTime.of(2024, 1, 1, 0, 0)
        val end = LocalDateTime.of(2024, 1, 1, 0, 30)
        val range = start until end
        range.contains(LocalDateTime.of(2024, 1, 1, 0, 15)).shouldBeTrue()
        range.contains(LocalDateTime.of(2024, 1, 1, 0, 30)).shouldBeFalse()
    }

    @Test
    fun `toString contains start and endExclusive`() {
        val start = LocalDateTime.of(2024, 1, 1, 0, 0)
        val end = LocalDateTime.of(2024, 1, 10, 0, 0)
        val range = start until end
        val str = range.toString()
        str.isNotEmpty().shouldBeTrue()
    }
}
