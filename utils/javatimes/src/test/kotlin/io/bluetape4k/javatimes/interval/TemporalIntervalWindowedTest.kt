package io.bluetape4k.javatimes.interval

import io.bluetape4k.javatimes.nowZonedDateTime
import io.bluetape4k.javatimes.startOf
import io.bluetape4k.javatimes.temporalAmount
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeLessOrEqualTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.time.temporal.ChronoUnit
import kotlin.test.assertFailsWith

class TemporalIntervalWindowedTest {

    companion object: KLogging()

    @Nested
    inner class ChunkedTest {

        @ParameterizedTest(name = "chunk interval by {0}")
        @EnumSource(
            ChronoUnit::class,
            names = ["YEARS", "MONTHS", "WEEKS", "DAYS", "HOURS", "MINUTES", "SECONDS", "MILLIS"]
        )
        fun `chunk interval`(chronoUnit: ChronoUnit) {
            val start = nowZonedDateTime().startOf(chronoUnit)
            val interval = temporalIntervalOf(start, 5.temporalAmount(chronoUnit))

            val chunks = interval.chunked(4, chronoUnit).toList()

            chunks.forEachIndexed { index, chunk ->
                log.trace { "chunks[$index] = $chunk" }
                chunk.size shouldBeLessOrEqualTo 4
                Assertions.assertTrue { chunk.first() in interval }
                Assertions.assertTrue { chunk.last() in interval }
            }

            chunks.size shouldBeEqualTo 2
            chunks.first().size shouldBeEqualTo 4
            chunks.last().size shouldBeEqualTo 1

            assertFailsWith<AssertionError> {
                interval.chunked(0, chronoUnit)
            }

            assertFailsWith<AssertionError> {
                interval.chunked(-1, chronoUnit)
            }
        }

        @ParameterizedTest(name = "chunk interval and aggregate by {0}")
        @EnumSource(
            ChronoUnit::class,
            names = ["YEARS", "MONTHS", "WEEKS", "DAYS", "HOURS", "MINUTES", "SECONDS", "MILLIS"]
        )
        fun `chunk interval and aggregate`(chronoUnit: ChronoUnit) {
            val start = nowZonedDateTime().startOf(chronoUnit)
            val interval = temporalIntervalOf(start, 5.temporalAmount(chronoUnit))

            val chunks = interval.chunked(3, chronoUnit)
                .map { years -> temporalIntervalOf(years.first(), years.last()) }
                .toList()


            chunks.size shouldBeEqualTo 2
            chunks.forEach {
                log.trace { "chunk=$it" }
                Assertions.assertTrue { it in interval }
            }
        }
    }

    @Nested
    inner class WindowedTest {

        @ParameterizedTest(name = "windowed interval by {0}")
        @EnumSource(
            ChronoUnit::class,
            names = ["YEARS", "MONTHS", "WEEKS", "DAYS", "HOURS", "MINUTES", "SECONDS", "MILLIS"]
        )
        fun `windowed interval`(unit: ChronoUnit) {

            val start = nowZonedDateTime().startOf(unit)
            val interval = temporalIntervalOf(start, 5.temporalAmount(unit))

            val windowed = interval.windowed(3, 2, unit)
            windowed.forEachIndexed { index, items ->
                log.trace { "index=$index, items=$items" }

                Assertions.assertTrue { items.first() in interval }
                Assertions.assertTrue { items.last() in interval }
            }
            windowed.count() shouldBeEqualTo 3

            assertFailsWith<AssertionError> {
                interval.windowed(-1, 2, unit)
            }
            assertFailsWith<AssertionError> {
                interval.windowed(1, -2, unit)
            }
            assertFailsWith<AssertionError> {
                interval.windowed(0, 2, unit)
            }
            assertFailsWith<AssertionError> {
                interval.windowed(1, 0, unit)
            }
        }
    }

    @Nested
    inner class ZipWithNextTest {
        @ParameterizedTest(name = "zip with next with interval by {0}")
        @EnumSource(
            ChronoUnit::class,
            names = ["YEARS", "MONTHS", "WEEKS", "DAYS", "HOURS", "MINUTES", "SECONDS", "MILLIS"]
        )
        fun `zip with next with interval`(unit: ChronoUnit) {

            val start = nowZonedDateTime().startOf(unit)
            val interval = temporalIntervalOf(start, 5.temporalAmount(unit))

            val zipWithNext = interval.zipWithNext(unit)

            zipWithNext.count() shouldBeEqualTo 4
            zipWithNext.forEach { (current, next) ->
                log.trace { "current=$current, next=$next" }
                kotlin.test.assertTrue { current in interval }
                kotlin.test.assertTrue { next in interval }
                kotlin.test.assertTrue { current < next }
            }
        }
    }
}
