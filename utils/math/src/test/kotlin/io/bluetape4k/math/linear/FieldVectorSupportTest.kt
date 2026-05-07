package io.bluetape4k.math.linear

import io.bluetape4k.logging.KLogging
import io.bluetape4k.assertions.shouldBeEqualTo
import org.apache.commons.math3.fraction.Fraction
import org.apache.commons.math3.linear.ArrayFieldVector
import org.junit.jupiter.api.Test

class FieldVectorSupportTest {

    companion object : KLogging()

    private fun fractionVector(vararg values: Fraction) = ArrayFieldVector(values)
    private fun f(n: Int, d: Int = 1) = Fraction(n, d)

    @Test
    fun `인덱스로 필드 벡터의 원소를 가져올 수 있다`() {
        val v = fractionVector(f(1), f(2), f(3))
        v[0].shouldBeEqualTo(f(1))
        v[2].shouldBeEqualTo(f(3))
    }

    @Test
    fun `인덱스로 필드 벡터의 원소를 설정할 수 있다`() {
        val v = fractionVector(f(1), f(2), f(3))
        v[1] = f(10)
        v[1].shouldBeEqualTo(f(10))
    }

    @Test
    fun `두 필드 벡터를 더할 수 있다`() {
        val v1 = fractionVector(f(1), f(2), f(3))
        val v2 = fractionVector(f(4), f(5), f(6))
        val result = v1 + v2
        result[0].shouldBeEqualTo(f(5))
        result[1].shouldBeEqualTo(f(7))
        result[2].shouldBeEqualTo(f(9))
    }

    @Test
    fun `필드 벡터에 스칼라를 더할 수 있다`() {
        val v = fractionVector(f(1), f(2), f(3))
        val result = v + f(10)
        result[0].shouldBeEqualTo(f(11))
        result[1].shouldBeEqualTo(f(12))
        result[2].shouldBeEqualTo(f(13))
    }

    @Test
    fun `두 필드 벡터를 뺄 수 있다`() {
        val v1 = fractionVector(f(4), f(5), f(6))
        val v2 = fractionVector(f(1), f(2), f(3))
        val result = v1 - v2
        result[0].shouldBeEqualTo(f(3))
        result[1].shouldBeEqualTo(f(3))
        result[2].shouldBeEqualTo(f(3))
    }

    @Test
    fun `필드 벡터에서 스칼라를 뺄 수 있다`() {
        val v = fractionVector(f(4), f(5), f(6))
        val result = v - f(1)
        result[0].shouldBeEqualTo(f(3))
        result[1].shouldBeEqualTo(f(4))
        result[2].shouldBeEqualTo(f(5))
    }

    @Test
    fun `두 필드 벡터를 원소별로 곱할 수 있다`() {
        val v1 = fractionVector(f(1), f(2), f(3))
        val v2 = fractionVector(f(2), f(3), f(4))
        val result = v1 * v2
        result[0].shouldBeEqualTo(f(2))
        result[1].shouldBeEqualTo(f(6))
        result[2].shouldBeEqualTo(f(12))
    }

    @Test
    fun `필드 벡터에 스칼라를 곱할 수 있다`() {
        val v = fractionVector(f(1), f(2), f(3))
        val result = v * f(3)
        result[0].shouldBeEqualTo(f(3))
        result[1].shouldBeEqualTo(f(6))
        result[2].shouldBeEqualTo(f(9))
    }

    @Test
    fun `두 필드 벡터를 원소별로 나눌 수 있다`() {
        val v1 = fractionVector(f(6), f(8), f(10))
        val v2 = fractionVector(f(2), f(4), f(5))
        val result = v1 / v2
        result[0].shouldBeEqualTo(f(3))
        result[1].shouldBeEqualTo(f(2))
        result[2].shouldBeEqualTo(f(2))
    }

    @Test
    fun `필드 벡터를 스칼라로 나눌 수 있다`() {
        val v = fractionVector(f(4), f(6), f(8))
        val result = v / f(2)
        result[0].shouldBeEqualTo(f(2))
        result[1].shouldBeEqualTo(f(3))
        result[2].shouldBeEqualTo(f(4))
    }
}
