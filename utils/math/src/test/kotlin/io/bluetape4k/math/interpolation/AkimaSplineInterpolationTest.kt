package io.bluetape4k.math.interpolation

import io.bluetape4k.assertions.assertFailsWith
import org.apache.commons.math3.exception.NumberIsTooSmallException
import org.junit.jupiter.api.Test

class AkimaSplineInterpolationTest: AbstractInterpolationTest() {

    override val interpolator: Interpolator = AkimaSplineInterpolator()

    @Test
    fun `number is small exception`() {
        assertFailsWith<NumberIsTooSmallException> {
            val xs = DoubleArray(4) { it.toDouble() }
            val ys = DoubleArray(4) { it.toDouble() }

            interpolator.interpolate(xs, ys)
        }
    }
}
