package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeNear
import org.junit.jupiter.api.Test

class ProbabilityTest {

    companion object: KLogging()

    data class Item(val id: Int, val name: String)

    @Test
    fun `probability of normal distribution`() {
        val prob = 1.0.normalDensity(1.0, 2.0)
        log.trace { "prob=$prob" }
        prob shouldBeEqualTo 0.19947114020071635
    }

    @Test
    fun `frequency of items`() {
        val values = listOf(
            Item(1, "A"),
            Item(2, "B"),
            Item(2, "B"),
            Item(3, "C"),
        )

        val freq = values.frequency()
        log.trace { "freq=$freq" }

        freq[values[0]] shouldBeEqualTo 1
        freq[values[1]] shouldBeEqualTo 2
        freq[values[2]] shouldBeEqualTo 2
        freq[values[3]] shouldBeEqualTo 1
    }

    @Test
    fun `frequency of items by value selector`() {
        val values = listOf(
            Item(1, "A"),
            Item(2, "B"),
            Item(2, "B"),
            Item(2, "B"),
            Item(3, "C"),
            Item(3, "C"),
        )

        val freq = values.frequency { it.id }
        log.trace { "freq=$freq" }

        freq[values.first()] shouldBeEqualTo 1
        freq[values[2]] shouldBeEqualTo 3
        freq[values.last()] shouldBeEqualTo 2
    }

    @Test
    fun `probability of items`() {
        val values = listOf(
            Item(1, "A"),
            Item(2, "B"),
            Item(2, "B"),
            Item(2, "B"),
            Item(3, "C"),
            Item(3, "C"),
        )

        val prob = values.probability(Item(2, "B")) { a, b -> a.id == b.id }
        log.trace { "prob=$prob" }
        prob shouldBeEqualTo 3.0 / values.size
    }

    // ----- Sequence.frequency -----

    @Test
    fun `Sequence frequency 가 각 요소의 빈도를 반환한다`() {
        val values = sequenceOf("a", "b", "a", "c", "a")

        val freq = values.frequency()
        log.trace { "freq=$freq" }

        freq["a"] shouldBeEqualTo 3
        freq["b"] shouldBeEqualTo 1
        freq["c"] shouldBeEqualTo 1
    }

    @Test
    fun `Sequence frequency by selector 가 올바른 빈도를 반환한다`() {
        val values = sequenceOf(
            Item(1, "A"),
            Item(2, "B"),
            Item(1, "A"),
            Item(3, "C"),
        )

        val freq = values.frequency { it.id }
        log.trace { "freq=$freq" }

        freq[Item(1, "A")] shouldBeEqualTo 2
        freq[Item(2, "B")] shouldBeEqualTo 1
    }

    // ----- Sequence.probability -----

    @Test
    fun `Sequence probability 가 올바른 확률을 반환한다`() {
        val values = sequenceOf(1, 2, 1, 3, 1)

        val prob = values.probability(1) { a, b -> a == b }
        log.trace { "prob=$prob" }
        prob.shouldBeNear(3.0 / 5.0, 1e-10)
    }

    // ----- normalDensity (Sequence) -----

    @Test
    fun `Sequence normalDensity 가 각 요소의 정규분포 확률을 반환한다`() {
        val xs = sequenceOf(-1.0, 0.0, 1.0)
        val probs = xs.normalDensity(0.0, 1.0).toList()

        log.trace { "probs=$probs" }
        // 표준정규분포 f(0) = 1/sqrt(2π) ≈ 0.3989
        probs[1].shouldBeNear(0.3989422804014327, 1e-10)
        // 대칭: f(-1) == f(1)
        probs[0].shouldBeNear(probs[2], 1e-10)
    }
}
