package io.bluetape4k.javatimes.range

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.ZonedDateTime

class TemporalOpenedProgressionTest {

    private val start = LocalDateTime.of(2024, 1, 1, 0, 0)

    @Test
    fun `iteration and contains use the exclusive boundary`() {
        val endExclusive = start.plusDays(4)
        val progression = TemporalOpenedProgression.fromOpendRange(
            start,
            endExclusive,
            Duration.ofDays(2)
        )

        progression.toList() shouldBeEqualTo listOf(start, start.plusDays(2))
        progression.contains(start.plusDays(2)).shouldBeTrue()
        progression.contains(endExclusive).shouldBeFalse()
    }

    @Test
    fun `equal start and exclusive end create an empty progression`() {
        val progression = TemporalOpenedProgression.fromOpendRange(start, start, Duration.ofDays(1))

        progression.isEmpty().shouldBeTrue()
        progression.toList() shouldBeEqualTo emptyList<LocalDateTime>()
    }

    @Test
    fun `different exclusive endpoints are not equal when last is the same`() {
        val firstEndExclusive = start.plusDays(3)
        val secondEndExclusive = start.plusDays(3).plusHours(12)
        val first = TemporalOpenedProgression.fromOpendRange(
            start,
            firstEndExclusive,
            Duration.ofDays(2)
        )
        val second = TemporalOpenedProgression.fromOpendRange(
            start,
            secondEndExclusive,
            Duration.ofDays(2)
        )

        first.last shouldBeEqualTo second.last
        (first == second).shouldBeFalse()
        (first.hashCode() == second.hashCode()).shouldBeFalse()
    }

    @Test
    fun `same exclusive boundary and step have equal hashes`() {
        val endExclusive = start.plusDays(4)
        val first = TemporalOpenedProgression.fromOpendRange(start, endExclusive, Duration.ofDays(2))
        val second = TemporalOpenedProgression.fromOpendRange(start, endExclusive, Duration.ofDays(2))

        first shouldBeEqualTo second
        first.hashCode() shouldBeEqualTo second.hashCode()
        setOf(first, second).size shouldBeEqualTo 1
    }

    @Test
    fun `reverse progression includes values before the exclusive endpoint`() {
        val reverseStart = LocalDateTime.of(2024, 1, 5, 0, 0)
        val endExclusive = LocalDateTime.of(2024, 1, 1, 12, 0)
        val progression = TemporalOpenedProgression.fromOpendRange(
            reverseStart,
            endExclusive,
            Duration.ofDays(-2)
        )

        progression.toList() shouldBeEqualTo listOf(
            reverseStart,
            reverseStart.minusDays(2)
        )
        progression.contains(endExclusive).shouldBeFalse()
    }

    @Test
    fun `period steps preserve the open boundary for date values`() {
        val dateStart = LocalDate.of(2024, 1, 1)
        val endExclusive = LocalDate.of(2024, 1, 6)
        val progression = TemporalOpenedProgression.fromOpendRange(
            dateStart,
            endExclusive,
            Period.ofDays(2)
        )

        progression.toList() shouldBeEqualTo listOf(
            dateStart,
            dateStart.plusDays(2),
            dateStart.plusDays(4)
        )
    }

    @Test
    fun `zoned progression equality includes the chronology boundary`() {
        val zone = ZoneId.of("Asia/Seoul")
        val endExclusive = ZonedDateTime.of(2024, 1, 4, 0, 0, 0, 0, zone)
        val start = endExclusive.minusDays(3)
        val progression = TemporalOpenedProgression.fromOpendRange(start, endExclusive, Duration.ofDays(1))
        val equivalent = TemporalOpenedProgression.fromOpendRange(start, endExclusive, Duration.ofDays(1))

        progression shouldBeEqualTo equivalent
        progression.hashCode() shouldBeEqualTo equivalent.hashCode()
    }

    @Test
    fun `opened range retains a submillisecond exclusive boundary`() {
        val rangeStart = Instant.EPOCH
        val endExclusive = rangeStart.plusNanos(1_500_000)
        val range = TemporalOpenedRange.fromOpenedRange(rangeStart, endExclusive)

        range.endExclusive shouldBeEqualTo endExclusive
        range.contains(rangeStart.plusMillis(1)).shouldBeTrue()
        range.contains(endExclusive).shouldBeFalse()
    }
}
