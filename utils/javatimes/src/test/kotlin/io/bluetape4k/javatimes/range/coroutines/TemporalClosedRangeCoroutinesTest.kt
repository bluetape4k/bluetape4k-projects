package io.bluetape4k.javatimes.range.coroutines

import io.bluetape4k.javatimes.range.temporalClosedProgressionOf
import io.bluetape4k.javatimes.range.temporalClosedRangeOf
import io.bluetape4k.javatimes.startOfHour
import io.bluetape4k.logging.KLogging
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeLessOrEqualTo
import io.bluetape4k.assertions.shouldNotBeEmpty
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class TemporalClosedRangeCoroutinesTest {

    companion object : KLogging()

    private val now: LocalDateTime = LocalDateTime.of(2024, 1, 15, 10, 0, 0)

    // ----- TemporalClosedProgression.asFlow -----

    @Test
    fun `TemporalClosedProgression asFlow 이 올바른 요소를 방출한다`() = runTest {
        val start = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
        val end = start.plusDays(2)
        val progression = temporalClosedProgressionOf(start, end, Duration.ofDays(1))

        val items = progression.asFlow().toList()

        items.size shouldBeEqualTo 3   // day 0, 1, 2 (closed)
        items.first() shouldBeEqualTo start
        items.last() shouldBeEqualTo end
    }

    // ----- TemporalClosedRange.asFlow -----

    @Test
    fun `TemporalClosedRange asFlow 이 요소를 방출한다`() = runTest {
        val start = now.startOfHour()
        val end = start.plusHours(1)
        val range = temporalClosedRangeOf(start, end)

        val items = range.asFlow().toList()

        items.shouldNotBeEmpty()
        items.first() shouldBeEqualTo start
    }

    // ----- windowedFlowHours -----
    // windowedFlow uses takeWhile so trailing windows may be smaller than size

    @Test
    fun `windowedFlowHours 가 윈도우를 올바른 수만큼 반환한다`() = runTest {
        val start = now.startOfHour()
        val end = start.plusHours(5)
        val range = temporalClosedRangeOf(start, end)

        val windows = range.windowedFlowHours(3, 1).toList()

        windows.size shouldBeEqualTo 6  // 시간 0..5: 윈도우 6개
        windows.first().size shouldBeEqualTo 3
        windows.forEach { window ->
            window.size shouldBeLessOrEqualTo 3
            window.shouldNotBeEmpty()
        }
    }

    @Test
    fun `windowedFlowHours step 2 가 올바른 윈도우를 반환한다`() = runTest {
        val start = now.startOfHour()
        val end = start.plusHours(5)
        val range = temporalClosedRangeOf(start, end)

        val windows = range.windowedFlowHours(2, 2).toList()

        windows.shouldNotBeEmpty()
        windows.first().shouldNotBeEmpty()
    }

    // ----- windowedFlowDays -----

    @Test
    fun `windowedFlowDays 가 올바른 크기의 윈도우를 반환한다`() = runTest {
        val start = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
        val end = start.plusDays(4)
        val range = temporalClosedRangeOf(start, end)

        val windows = range.windowedFlowDays(3, 1).toList()

        windows.shouldNotBeEmpty()
        windows.first().size shouldBeEqualTo 3
        windows.forEach { window ->
            window.size shouldBeLessOrEqualTo 3
        }
    }

    // ----- windowedFlowMinutes -----

    @Test
    fun `windowedFlowMinutes 가 올바른 크기의 윈도우를 반환한다`() = runTest {
        val start = now.withSecond(0).withNano(0)
        val end = start.plusMinutes(4)
        val range = temporalClosedRangeOf(start, end)

        val windows = range.windowedFlowMinutes(2, 1).toList()

        windows.shouldNotBeEmpty()
        windows.first().size shouldBeEqualTo 2
        windows.forEach { window ->
            window.size shouldBeLessOrEqualTo 2
        }
    }

    // ----- chunkedFlowHours -----

    @Test
    fun `chunkedFlowHours 가 올바른 청크를 반환한다`() = runTest {
        val start = now.startOfHour()
        val end = start.plusHours(5)
        val range = temporalClosedRangeOf(start, end)

        val chunks = range.chunkedFlowHours(3).toList()

        chunks.size shouldBeEqualTo 2  // [h0,h1,h2] 와 [h3,h4,h5]
        chunks.first().size shouldBeEqualTo 3
    }

    @Test
    fun `chunkedFlowDays 가 올바른 청크를 반환한다`() = runTest {
        val start = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
        val end = start.plusDays(5)
        val range = temporalClosedRangeOf(start, end)

        val chunks = range.chunkedFlowDays(3).toList()

        chunks.shouldNotBeEmpty()
        chunks.first().size shouldBeEqualTo 3
    }

    @Test
    fun `chunkedFlowMinutes 가 올바른 청크를 반환한다`() = runTest {
        val start = now.withSecond(0).withNano(0)
        val end = start.plusMinutes(5)
        val range = temporalClosedRangeOf(start, end)

        val chunks = range.chunkedFlowMinutes(3).toList()

        chunks.shouldNotBeEmpty()
        chunks.first().size shouldBeEqualTo 3
    }

    // ----- zipWithNextFlowHours -----

    @Test
    fun `zipWithNextFlowHours 가 연속된 시간 쌍을 반환한다`() = runTest {
        val start = now.startOfHour()
        val end = start.plusHours(3)
        val range = temporalClosedRangeOf(start, end)

        val pairs = range.zipWithNextFlowHours().toList()

        pairs.size shouldBeEqualTo 3  // (h0,h1), (h1,h2), (h2,h3)
        pairs.forEach { (first, second) ->
            second shouldBeEqualTo first.plusHours(1)
        }
    }

    @Test
    fun `zipWithNextFlowDays 가 연속된 날짜 쌍을 반환한다`() = runTest {
        val start = now.withHour(0).withMinute(0).withSecond(0).withNano(0)
        val end = start.plusDays(3)
        val range = temporalClosedRangeOf(start, end)

        val pairs = range.zipWithNextFlowDays().toList()

        pairs.size shouldBeEqualTo 3   // (d0,d1), (d1,d2), (d2,d3)
        pairs.forEach { (first, second) ->
            second shouldBeEqualTo first.plusDays(1)
        }
    }

    @Test
    fun `zipWithNextFlowMinutes 가 연속된 분 쌍을 반환한다`() = runTest {
        val start = now.withSecond(0).withNano(0)
        val end = start.plusMinutes(3)
        val range = temporalClosedRangeOf(start, end)

        val pairs = range.zipWithNextFlowMinutes().toList()

        pairs.size shouldBeEqualTo 3
        pairs.forEach { (first, second) ->
            second shouldBeEqualTo first.plusMinutes(1)
        }
    }

    // ----- windowedFlowSeconds -----

    @Test
    fun `windowedFlowSeconds 가 올바른 크기의 윈도우를 반환한다`() = runTest {
        val start = now.withNano(0)
        val end = start.plusSeconds(4)
        val range = temporalClosedRangeOf(start, end)

        val windows = range.windowedFlowSeconds(2, 1).toList()

        windows.shouldNotBeEmpty()
        windows.first().size shouldBeEqualTo 2
        windows.forEach { window ->
            window.size shouldBeLessOrEqualTo 2
        }
    }

    // ----- chunkedFlowSeconds -----

    @Test
    fun `chunkedFlowSeconds 가 올바른 청크를 반환한다`() = runTest {
        val start = now.withNano(0)
        val end = start.plusSeconds(5)
        val range = temporalClosedRangeOf(start, end)

        val chunks = range.chunkedFlowSeconds(3).toList()

        chunks.shouldNotBeEmpty()
        chunks.first().size shouldBeEqualTo 3
    }

    // ----- zipWithNextFlowSeconds -----

    @Test
    fun `zipWithNextFlowSeconds 가 연속된 초 쌍을 반환한다`() = runTest {
        val start = now.withNano(0)
        val end = start.plusSeconds(3)
        val range = temporalClosedRangeOf(start, end)

        val pairs = range.zipWithNextFlowSeconds().toList()

        pairs.size shouldBeEqualTo 3
        pairs.forEach { (first, second) ->
            second shouldBeEqualTo first.plusSeconds(1)
        }
    }
}
