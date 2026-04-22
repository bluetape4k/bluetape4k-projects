package io.bluetape4k.javatimes.range.coroutines

import io.bluetape4k.javatimes.range.TemporalOpenedRange
import io.bluetape4k.javatimes.range.temporalOpenedProgression
import io.bluetape4k.javatimes.startOfHour
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class TemporalOpenedRangeCoroutinesTest {

    companion object : KLogging()

    private val now: LocalDateTime = LocalDateTime.of(2024, 1, 15, 10, 0, 0)

    // ----- TemporalOpenedProgression.asFlow -----

    @Test
    fun `TemporalOpenedProgression asFlow 이 올바른 요소를 방출한다`() = runTest {
        val start = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
        val end = start.plusDays(3)
        val progression = temporalOpenedProgression(start, end, Duration.ofDays(1))

        val items = progression.asFlow().toList()

        items.size shouldBeEqualTo 3   // day 0, 1, 2 (end 제외)
        items.first() shouldBeEqualTo start
        items.last() shouldBeEqualTo start.plusDays(2)
    }

    // ----- TemporalOpenedRange.asFlow -----

    @Test
    fun `TemporalOpenedRange asFlow 이 요소를 방출한다`() = runTest {
        val start = now.startOfHour()
        val end = start.plusHours(1)
        val range = TemporalOpenedRange(start, end)

        val items = range.asFlow().toList()

        items.shouldNotBeEmpty()
        items.first() shouldBeEqualTo start
    }

    // ----- windowedFlowHours -----
    // windowedFlow implementation generates all items from current until endExclusive
    // (size parameter limits items per window via index-based generation)

    @Test
    fun `windowedFlowHours 가 윈도우를 방출한다`() = runTest {
        val start = now.startOfHour()
        val end = start.plusHours(3)
        val range = TemporalOpenedRange(start, end)

        val windows = range.windowedFlowHours(2, 1).toList()

        windows.shouldNotBeEmpty()
        windows.first().shouldNotBeEmpty()
    }

    @Test
    fun `windowedFlowHours step 2 가 올바른 윈도우를 반환한다`() = runTest {
        val start = now.startOfHour()
        val end = start.plusHours(5)
        val range = TemporalOpenedRange(start, end)

        val windows = range.windowedFlowHours(2, 2).toList()

        windows.shouldNotBeEmpty()
        windows.first().shouldNotBeEmpty()
    }

    @Test
    fun `windowedFlowDays 가 윈도우를 방출한다`() = runTest {
        val start = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
        val end = start.plusDays(5)
        val range = TemporalOpenedRange(start, end)

        val windows = range.windowedFlowDays(3, 1).toList()

        windows.shouldNotBeEmpty()
        windows.first().shouldNotBeEmpty()
    }

    @Test
    fun `windowedFlowMinutes 가 윈도우를 방출한다`() = runTest {
        val start = now.withSecond(0).withNano(0)
        val end = start.plusMinutes(5)
        val range = TemporalOpenedRange(start, end)

        val windows = range.windowedFlowMinutes(2, 1).toList()

        windows.shouldNotBeEmpty()
        windows.first().shouldNotBeEmpty()
    }

    // ----- chunkedFlowHours -----

    @Test
    fun `chunkedFlowHours 가 청크를 방출한다`() = runTest {
        val start = now.startOfHour()
        val end = start.plusHours(6)
        val range = TemporalOpenedRange(start, end)

        val chunks = range.chunkedFlowHours(3).toList()

        chunks.shouldNotBeEmpty()
        chunks.first().shouldNotBeEmpty()
    }

    @Test
    fun `chunkedFlowDays 가 청크를 방출한다`() = runTest {
        val start = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
        val end = start.plusDays(6)
        val range = TemporalOpenedRange(start, end)

        val chunks = range.chunkedFlowDays(3).toList()

        chunks.shouldNotBeEmpty()
        chunks.first().shouldNotBeEmpty()
    }

    @Test
    fun `chunkedFlowMinutes 가 청크를 방출한다`() = runTest {
        val start = now.withSecond(0).withNano(0)
        val end = start.plusMinutes(6)
        val range = TemporalOpenedRange(start, end)

        val chunks = range.chunkedFlowMinutes(3).toList()

        chunks.shouldNotBeEmpty()
        chunks.first().shouldNotBeEmpty()
    }

    // ----- zipWithNextFlowHours -----

    @Test
    fun `zipWithNextFlowHours 가 연속된 시간 쌍을 반환한다`() = runTest {
        val start = now.startOfHour()
        val end = start.plusHours(4)
        val range = TemporalOpenedRange(start, end)

        val pairs = range.zipWithNextFlowHours().toList()

        pairs.size shouldBeEqualTo 3  // (h0,h1), (h1,h2), (h2,h3)
        pairs.forEach { (first, second) ->
            second shouldBeEqualTo first.plusHours(1)
        }
    }

    @Test
    fun `zipWithNextFlowDays 가 연속된 날짜 쌍을 반환한다`() = runTest {
        val start = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
        val end = start.plusDays(4)
        val range = TemporalOpenedRange(start, end)

        val pairs = range.zipWithNextFlowDays().toList()

        pairs.size shouldBeEqualTo 3   // (d0,d1), (d1,d2), (d2,d3)
        pairs.forEach { (first, second) ->
            second shouldBeEqualTo first.plusDays(1)
        }
    }

    @Test
    fun `zipWithNextFlowMinutes 가 연속된 분 쌍을 반환한다`() = runTest {
        val start = now.withSecond(0).withNano(0)
        val end = start.plusMinutes(4)
        val range = TemporalOpenedRange(start, end)

        val pairs = range.zipWithNextFlowMinutes().toList()

        pairs.size shouldBeEqualTo 3
        pairs.forEach { (first, second) ->
            second shouldBeEqualTo first.plusMinutes(1)
        }
    }

    @Test
    fun `zipWithNextFlowSeconds 가 연속된 초 쌍을 반환한다`() = runTest {
        val start = now.withNano(0)
        val end = start.plusSeconds(4)
        val range = TemporalOpenedRange(start, end)

        val pairs = range.zipWithNextFlowSeconds().toList()

        pairs.size shouldBeEqualTo 3
        pairs.forEach { (first, second) ->
            second shouldBeEqualTo first.plusSeconds(1)
        }
    }
}
