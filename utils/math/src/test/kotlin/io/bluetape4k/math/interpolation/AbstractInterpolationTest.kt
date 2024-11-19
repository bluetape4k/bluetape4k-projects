package io.bluetape4k.math.interpolation

import io.bluetape4k.logging.KLogging
import io.bluetape4k.math.commons.approximateEqual
import io.bluetape4k.support.emptyDoubleArray
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeTrue
import org.apache.commons.math3.exception.DimensionMismatchException
import org.apache.commons.math3.exception.NonMonotonicSequenceException
import org.apache.commons.math3.exception.NumberIsTooSmallException
import org.apache.commons.math3.exception.OutOfRangeException
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.assertFailsWith

abstract class AbstractInterpolationTest {

    companion object: KLogging()

    protected abstract val interpolator: Interpolator
    protected open val TOLERANCE: Double = 1e-5

    @Test
    open fun `empty data for interpolator`() {
        assertFailsWith<NumberIsTooSmallException> {
            interpolator.interpolate(emptyDoubleArray, emptyDoubleArray)
        }
    }

    @Test
    open fun `diffent size for interpolator`() {
        assertFailsWith<DimensionMismatchException> {
            val xs = DoubleArray(5) { it.toDouble() }
            val ys = DoubleArray(6) { it.toDouble() }
            interpolator.interpolate(xs, ys)
        }
    }

    @Test
    open fun `un-sorted values for interceptor`() {
        assertFailsWith<NonMonotonicSequenceException> {
            val xs = doubleArrayOf(0.0, 1.0, 0.5, 7.0, 3.5, 2.2, 8.0)
            val ys = DoubleArray(7) { it.toDouble() }
            interpolator.interpolate(xs, ys)
        }
    }

    @Test
    fun `interpolate linear values`() {
        val xs = DoubleArray(10) { it.toDouble() }
        val ys = DoubleArray(10) { it.toDouble() }

        val func = interpolator.interpolate(xs, ys)

        repeat(10) {
            func(it.toDouble()) shouldBeEqualTo it.toDouble()
        }

        // 실제 내삽한 데이터
        func(0.5) shouldBeEqualTo 0.5
    }

    @Test
    fun `extrapolate linear values raise OutOfRangeException`() {
        // NevilleInterpolator만 외삽을 지원합니다.
        Assumptions.assumeTrue(interpolator !is NevilleInterpolator)

        val xs = DoubleArray(10) { it.toDouble() }
        val ys = DoubleArray(10) { it.toDouble() }

        val func = interpolator.interpolate(xs, ys)

        repeat(10) {
            func(it.toDouble()) shouldBeEqualTo it.toDouble()
        }

        // Range 밖은 interpolator (내삽)가 아닌 extrapolator (외삽)
        assertFailsWith<OutOfRangeException> {
            func(10.0) shouldBeEqualTo 10.0
        }
    }

    @Test
    fun `interpolate sin func`() {
        val xs = DoubleArray(361) { it.toDouble() / 360.0 }
        val ys = DoubleArray(361) { sin(it.toDouble() * PI / 360.0) }

        val sinfunc = interpolator.interpolate(xs, ys)

        sinfunc(0.0).approximateEqual(0.0, TOLERANCE).shouldBeTrue()
        sinfunc(0.5).approximateEqual(1.0, TOLERANCE).shouldBeTrue()
        sinfunc(1.0).approximateEqual(0.0, TOLERANCE).shouldBeTrue()
    }
}
