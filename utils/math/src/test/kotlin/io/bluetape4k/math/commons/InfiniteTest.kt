package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeFalse
import org.amshove.kluent.shouldBeTrue
import org.junit.jupiter.api.Test

class InfiniteTest {

    companion object: KLogging()

    @Test
    fun `value is special case`() {
        val one = 1.0
        one.isSpecialCase().shouldBeFalse()

        val pInf = Double.POSITIVE_INFINITY
        pInf.isSpecialCase().shouldBeTrue()

        val nInf = Double.NEGATIVE_INFINITY
        nInf.isSpecialCase().shouldBeTrue()

        val nan = Double.NaN
        nan.isSpecialCase().shouldBeTrue()
    }

    @Test
    fun `value is infinite`() {
        val one = 1.0
        one.isPositiveInfinite().shouldBeFalse()
        one.isNegativeInfinite().shouldBeFalse()

        val pInf = Double.POSITIVE_INFINITY
        pInf.isPositiveInfinite().shouldBeTrue()
        pInf.isNegativeInfinite().shouldBeFalse()

        val nInf = Double.NEGATIVE_INFINITY
        nInf.isPositiveInfinite().shouldBeFalse()
        nInf.isNegativeInfinite().shouldBeTrue()
    }

    @Test
    fun `value is max or min`() {
        val one = 1.0
        one.isMaxValue().shouldBeFalse()
        one.isMinValue().shouldBeFalse()

        val maxValue = Double.MAX_VALUE
        maxValue.isMaxValue().shouldBeTrue()
        maxValue.isMinValue().shouldBeFalse()

        val minValue = Double.MIN_VALUE
        minValue.isMaxValue().shouldBeFalse()
        minValue.isMinValue().shouldBeTrue()
    }
}
