package io.bluetape4k.math.commons

import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.logging.KLogging
import org.junit.jupiter.api.Test

class MinMaxTest {

    companion object: KLogging()

    private val values = sequenceOf(-2.0, -1.0, 2.0, 4.0, Double.NaN)
    private val specialValues =
        sequenceOf(-2.0, -1.0, 2.0, 4.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)

    @Test
    fun `get max value when all values are non-positive`() {
        sequenceOf(-3.0, -2.0, -1.0).minMax() shouldBeEqualTo (-3.0 to -1.0)
        sequenceOf(0.0).minMax() shouldBeEqualTo (0.0 to 0.0)
        sequenceOf(-3.0, -2.0, -1.0).absMinMax() shouldBeEqualTo (1.0 to 3.0)
        sequenceOf(0.0).absMinMax() shouldBeEqualTo (0.0 to 0.0)
    }

    @Test
    fun `get min max value of sequence`() {
        val (min, max) = values.minMax()
        min shouldBeEqualTo -2.0
        max shouldBeEqualTo 4.0

        val (min2, max2) = specialValues.minMax()
        min2 shouldBeEqualTo Double.NEGATIVE_INFINITY
        max2 shouldBeEqualTo Double.POSITIVE_INFINITY
    }

    @Test
    fun `get absolute min max value of sequence`() {
        val (min, max) = values.absMinMax()
        min shouldBeEqualTo 1.0
        max shouldBeEqualTo 4.0

        val (min2, max2) = specialValues.absMinMax()
        min2 shouldBeEqualTo 1.0
        max2 shouldBeEqualTo Double.POSITIVE_INFINITY
    }
}
