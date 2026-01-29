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
import io.bluetape4k.logging.debug
import io.bluetape4k.logging.trace
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
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


@Suppress("UNCHECKED_CAST")
abstract class TemporalOpenedRangeTest<T> where T: Temporal, T: Comparable<T> {

    companion object: KLogging()

    abstract val start: T
    open val duration: TemporalAmount = 5.hours()

    private val endExclusive: T by lazy { start.add(duration) }
    private val range: TemporalOpenedRange<T> by lazy { start until endExclusive }

    @Test
    fun `start gerater than endExclusive`() {
        assertFailsWith<AssertionError> {
            endExclusive until start
        }
    }

    @Test
    fun `empty range`() {
        val empty = start until start
        empty.isEmpty()
        empty shouldBeEqualTo TemporalOpenedRange.EMPTY
    }

    @Test
    fun `create by until`() {
        val range1 = range
        val range2 = start until endExclusive

        range1 shouldBeEqualTo range2
    }

    @Test
    fun `windowed with flow`() = runTest {
        val range = start.startOfHour() until (start.startOfHour() + 5.hours()) as T
        log.debug { "range=$range" }

        val windowed = range
            .windowedFlowHours(3, 1)
            .onEach { log.trace { "windowed $it" } }
            .toFastList()

        windowed.size shouldBeEqualTo 5
    }

    @Test
    fun `chunk ranges with flow`() = runTest {
        val range = start.startOfHour() until (start.startOfHour() + 5.hours()) as T
        log.debug { "range=$range" }

        val chunked = range
            .chunkedFlowHours(3)
            .onEach { log.trace { "chunked $it" } }
            .toFastList()

        chunked.size shouldBeEqualTo 2
    }
}

class InstantRangeExclusiveTest: TemporalOpenedRangeTest<Instant>() {
    override val start: Instant = Instant.now()
}

class LocalDateTimeRangeExclusiveTest: TemporalOpenedRangeTest<LocalDateTime>() {
    override val start: LocalDateTime = LocalDateTime.now()
}

class OffsetDateTimeRangeExclusiveTest: TemporalOpenedRangeTest<OffsetDateTime>() {
    override val start: OffsetDateTime = OffsetDateTime.now()
}

class ZonedDateTimeRangeExclusiveTest: TemporalOpenedRangeTest<ZonedDateTime>() {
    override val start: ZonedDateTime = ZonedDateTime.now()
}

@Disabled("LocalTime 은 지원하지 않습니다.")
class LocalTimeRangeExclusiveTest: TemporalOpenedRangeTest<LocalTime>() {
    override val start: LocalTime = localTimeOf(7, 12, 45)
    override val duration: TemporalAmount = 5.seconds()
}

@Disabled("OffsetTime 은 지원하지 않습니다.")
class OffsetTimeRangeExclusiveTest: TemporalOpenedRangeTest<OffsetTime>() {
    override val start = offsetTimeOf(7, 12, 55)
    override val duration: TemporalAmount = 5.seconds()
}

@Disabled("LocalDate 는 지원하지 않습니다.")
class LocalDateRangeExclusiveTest: TemporalOpenedRangeTest<LocalDate>() {
    override val start: LocalDate = LocalDate.now()
    override val duration: TemporalAmount = 5.days()
}
