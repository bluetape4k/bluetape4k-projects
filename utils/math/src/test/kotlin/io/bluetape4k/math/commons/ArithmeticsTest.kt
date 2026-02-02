package io.bluetape4k.math.commons

import io.bluetape4k.collections.eclipse.fastList
import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

class ArithmeticsTest {

    companion object: KLogging()

    private val values1 = listOf(1.0, -1.1, 2.2, -4.5)
    private val values2 = listOf(2.0, 2.0, -3.1, 4.2)

    @Test
    fun `different size`() {
        val larges = fastList(values1.size * 2) { Random.nextDouble(-100.0, 100.0) }
        val added = values1 + larges
        added.count() shouldBeEqualTo values1.size
    }

    @Test
    fun `plus for two sequences`() {
        val negatives = values1.map { -it }
        val added = values1 + negatives
        added.all { it == 0.0 }.shouldBeTrue()
    }

    @Test
    fun `minus for two sequences`() {
        val minuses = values1 - values1
        minuses.all { it == 0.0 }.shouldBeTrue()
    }

    @Test
    fun `times for two sequences`() {
        val times = values1 * values1
        times.all { it >= 0.0 }.shouldBeTrue()
    }

    @Test
    fun `div for two sequences`() {
        val divs = values1 / values1
        divs.all { it == 1.0 }.shouldBeTrue()
    }
}
