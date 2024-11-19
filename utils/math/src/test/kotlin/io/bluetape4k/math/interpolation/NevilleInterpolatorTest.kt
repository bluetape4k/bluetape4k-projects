package io.bluetape4k.math.interpolation

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class NevilleInterpolatorTest: AbstractInterpolationTest() {

    override val interpolator: Interpolator = NevilleInterpolator()

    /**
     * 정렬이 안된 데이터를 넣어도 [NevilleInterpolator]는 정렬을 해서 내삽을 한다.
     */
    @Test
    override fun `un-sorted values for interceptor`() {
        val xs = doubleArrayOf(0.0, 1.0, 0.5, 7.0, 3.5, 2.2, 8.0)
        val ys = DoubleArray(7) { it.toDouble() }
        val func = interpolator.interpolate(xs, ys)

        func(0.0) shouldBeEqualTo 0.0
    }
}
