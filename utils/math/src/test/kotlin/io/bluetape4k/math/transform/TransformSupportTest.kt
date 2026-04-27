package io.bluetape4k.math.transform

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.apache.commons.math3.complex.Complex
import org.junit.jupiter.api.Test

class TransformSupportTest {

    companion object : KLogging()

    @Test
    fun `DoubleArray를 스케일링할 수 있다`() {
        val data = doubleArrayOf(1.0, 2.0, 3.0)
        val result = data.scale(2.0)
        result[0].shouldBeNear(2.0, 1e-10)
        result[1].shouldBeNear(4.0, 1e-10)
        result[2].shouldBeNear(6.0, 1e-10)
    }

    @Test
    fun `DoubleArray를 0으로 스케일링하면 모두 0이 된다`() {
        val data = doubleArrayOf(1.0, 2.0, 3.0)
        val result = data.scale(0.0)
        result[0].shouldBeNear(0.0, 1e-10)
        result[1].shouldBeNear(0.0, 1e-10)
        result[2].shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `복소수 배열을 스케일링할 수 있다`() {
        val data = arrayOf(Complex(1.0, 0.0), Complex(0.0, 1.0))
        val result = data.scale(2.0)
        result[0].real.shouldBeNear(2.0, 1e-10)
        result[0].imaginary.shouldBeNear(0.0, 1e-10)
        result[1].real.shouldBeNear(0.0, 1e-10)
        result[1].imaginary.shouldBeNear(2.0, 1e-10)
    }

    @Test
    fun `복소수 배열을 실수부와 허수부 2차원 배열로 변환할 수 있다`() {
        val data = arrayOf(Complex(1.0, 2.0), Complex(3.0, 4.0))
        val result = data.toRealImaginaryArray()
        result[0][0].shouldBeNear(1.0, 1e-10)
        result[0][1].shouldBeNear(3.0, 1e-10)
        result[1][0].shouldBeNear(2.0, 1e-10)
        result[1][1].shouldBeNear(4.0, 1e-10)
    }

    @Test
    fun `2차원 실수 배열을 복소수 배열로 변환할 수 있다`() {
        val data = arrayOf(doubleArrayOf(1.0, 3.0), doubleArrayOf(2.0, 4.0))
        val result = data.toComplexArray()
        result[0].real.shouldBeNear(1.0, 1e-10)
        result[0].imaginary.shouldBeNear(2.0, 1e-10)
        result[1].real.shouldBeNear(3.0, 1e-10)
        result[1].imaginary.shouldBeNear(4.0, 1e-10)
    }

    @Test
    fun `2의 거듭제곱에 대한 log2를 계산할 수 있다`() {
        8.exactLog2().shouldBeEqualTo(3)
        16.exactLog2().shouldBeEqualTo(4)
        1.exactLog2().shouldBeEqualTo(0)
        2.exactLog2().shouldBeEqualTo(1)
    }

    @Test
    fun `toRealImaginaryArray 후 toComplexArray로 왕복 변환된다`() {
        val original = arrayOf(Complex(1.0, 2.0), Complex(3.0, 4.0))
        val roundTrip = original.toRealImaginaryArray().toComplexArray()
        roundTrip[0].real.shouldBeNear(original[0].real, 1e-10)
        roundTrip[0].imaginary.shouldBeNear(original[0].imaginary, 1e-10)
        roundTrip[1].real.shouldBeNear(original[1].real, 1e-10)
        roundTrip[1].imaginary.shouldBeNear(original[1].imaginary, 1e-10)
    }
}
