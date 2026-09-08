package io.bluetape4k.javatimes.range

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime

class TemporalOpenedProgressionTest {

    @Test
    fun `비정렬된 종료 경계 직전의 마지막 원소를 포함한다`() {
        val start = LocalDateTime.of(2024, 1, 1, 0, 0)
        val endExclusive = LocalDateTime.of(2024, 1, 4, 0, 0)

        temporalOpenedProgression(start, endExclusive, Duration.ofDays(2)).toList() shouldBeEqualTo listOf(
            start,
            start.plusDays(2),
        )
    }

    @Test
    fun `정렬된 종료 경계는 제외한다`() {
        val start = LocalDateTime.of(2024, 1, 1, 0, 0)
        val endExclusive = LocalDateTime.of(2024, 1, 5, 0, 0)

        temporalOpenedProgression(start, endExclusive, Duration.ofDays(2)).toList() shouldBeEqualTo listOf(
            start,
            start.plusDays(2),
        )
    }

    @Test
    fun `음수 step에서도 종료 경계를 제외한다`() {
        val start = LocalDateTime.of(2024, 1, 5, 0, 0)
        val endExclusive = LocalDateTime.of(2024, 1, 1, 0, 0)

        temporalOpenedProgression(start, endExclusive, Duration.ofDays(-2)).toList() shouldBeEqualTo listOf(
            start,
            start.minusDays(2),
        )
    }

    @Test
    fun `시작과 종료가 같으면 열린 progression은 비어있다`() {
        val point = LocalDateTime.of(2024, 1, 1, 0, 0)
        val progression = temporalOpenedProgression(point, point, Duration.ofDays(1))

        progression.isEmpty().shouldBeTrue()
        progression.toList() shouldBeEqualTo emptyList()
    }
}
