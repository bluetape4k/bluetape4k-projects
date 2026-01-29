package io.bluetape4k.javatimes.range

import io.bluetape4k.coroutines.flow.extensions.toFastList
import io.bluetape4k.javatimes.add
import io.bluetape4k.javatimes.days
import io.bluetape4k.javatimes.hours
import io.bluetape4k.javatimes.localTimeOf
import io.bluetape4k.javatimes.offsetTimeOf
import io.bluetape4k.javatimes.range.coroutines.chunkedFlowHours
import io.bluetape4k.javatimes.range.coroutines.windowedFlowHours
import io.bluetape4k.javatimes.seconds
import io.bluetape4k.javatimes.startOfHour
import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.time.temporal.Temporal
import java.time.temporal.TemporalAmount
import kotlin.test.assertFailsWith

abstract class TemporalClosedRangeTest<T> where T: Temporal, T: Comparable<T> {

    companion object: KLogging()

    abstract val start: T
    open val duration: TemporalAmount = 5.hours()

    private val endInclusive: T by lazy { start.add(duration) }
    private val range: TemporalClosedRange<T> by lazy { start..endInclusive }

    @Test
    fun `simple constructor`() {
        val range = temporalClosedRangeOf(start, endInclusive)

        range.start shouldBeEqualTo start
        range.endInclusive shouldBeEqualTo endInclusive
        range.first shouldBeEqualTo start
        range.last shouldBeEqualTo endInclusive
    }

    @Test
    fun `start greater than endInclusive`() {
        assertFailsWith<AssertionError> {
            temporalClosedRangeOf(endInclusive, start)
        }
    }

    @Test
    fun `emtpy range`() {
        val empty = temporalClosedRangeOf(start, start)
        empty.isEmpty().shouldBeTrue()
        empty shouldBeEqualTo TemporalClosedRange.EMPTY
    }

    @Test
    fun `create by ragneTo`() {
        val range1 = range
        val range2 = temporalClosedRangeOf(start, endInclusive)
        val range3 = start..endInclusive

        range1 shouldBeEqualTo range2
        range2 shouldBeEqualTo range3
        range3 shouldBeEqualTo range1
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `windowed with flow`() = runTest {
        val range = start.startOfHour()..(start.startOfHour() + 5.hours()) as T
        val windowed = range
            .windowedFlowHours(3, 1)
            .onEach { log.trace { "windowed $it" } }
            .toFastList()

        windowed.size shouldBeEqualTo 6
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `chunk ranges with flow`() = runTest {
        val range = start.startOfHour()..(start.startOfHour() + 5.hours()) as T
        val chunked = range
            .chunkedFlowHours(3)
            .onEach { log.trace { "chunked $it" } }
            .toFastList()
        chunked.size shouldBeEqualTo 2
    }
}

class InstantRangeTest: TemporalClosedRangeTest<Instant>() {
    override val start: Instant = Instant.now()
}

class LocalDateTimeRangeTest: TemporalClosedRangeTest<LocalDateTime>() {
    override val start: LocalDateTime = LocalDateTime.now()
}

class OffsetDateTimeRangeTest: TemporalClosedRangeTest<OffsetDateTime>() {
    override val start: OffsetDateTime = OffsetDateTime.now()
}

class ZonedDateTimeRangeTest: TemporalClosedRangeTest<ZonedDateTime>() {
    override val start: ZonedDateTime = ZonedDateTime.now()
}

@Disabled("LocalTime 은 지원하지 않습니다.")
class LocalTimeRangeTest: TemporalClosedRangeTest<LocalTime>() {
    override val start: LocalTime = localTimeOf(7, 12, 45)
    override val duration: TemporalAmount = 5.seconds()
}

@Disabled("OffsetTime 은 지원하지 않습니다.")
class OffsetTimeRangeTest: TemporalClosedRangeTest<OffsetTime>() {
    override val start = offsetTimeOf(7, 12, 55)
    override val duration: TemporalAmount = 5.seconds()
}

@Disabled("LocalDate 는 지원하지 않습니다.")
class LocalDateRangeTest: TemporalClosedRangeTest<LocalDate>() {
    override val start: LocalDate = LocalDate.now()
    override val duration: TemporalAmount = 5.days()
}
