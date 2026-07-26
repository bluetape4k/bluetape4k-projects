package io.bluetape4k.math.interpolation

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.support.emptyDoubleArray
import org.apache.commons.math3.exception.NoDataException
import org.junit.jupiter.api.Test

class LoessInterpolatorTest: AbstractInterpolationTest() {

    override val interpolator: Interpolator = LoessInterpolator()

    override val TOLERANCE: Double = 1e-1

    @Test
    override fun `empty data for interpolator`() {
        assertFailsWith<NoDataException> {
            interpolator.interpolate(emptyDoubleArray, emptyDoubleArray)
        }
    }

}
