package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import io.bluetape4k.logging.trace
import org.amshove.kluent.shouldBeEqualTo
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
}
