package io.bluetape4k.math.linear

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.apache.commons.math3.linear.ArrayRealVector
import org.junit.jupiter.api.Test

class RealVectorSupportTest {

    companion object: KLogging()

    private fun vectorOf(vararg values: Double) = ArrayRealVector(values)

    @Test
    fun `get 연산자로 원소를 접근한다`() {
        val v = vectorOf(1.0, 2.0, 3.0)
        v[0] shouldBeEqualTo 1.0
        v[1] shouldBeEqualTo 2.0
        v[2] shouldBeEqualTo 3.0
    }

    @Test
    fun `set 연산자로 원소를 설정한다`() {
        val v = vectorOf(1.0, 2.0, 3.0)
        v[1] = 99.0
        v[1] shouldBeEqualTo 99.0
    }

    @Test
    fun `벡터 덧셈이 동작한다`() {
        val a = vectorOf(1.0, 2.0, 3.0)
        val b = vectorOf(4.0, 5.0, 6.0)
        val result = a + b

        result[0] shouldBeEqualTo 5.0
        result[1] shouldBeEqualTo 7.0
        result[2] shouldBeEqualTo 9.0
    }

    @Test
    fun `스칼라 덧셈이 동작한다`() {
        val a = vectorOf(1.0, 2.0, 3.0)
        // <N: Number> plus(scalar: Number) - 타입 파라미터 명시
        val result = a.plus<Double>(10.0)

        result[0] shouldBeEqualTo 11.0
        result[1] shouldBeEqualTo 12.0
        result[2] shouldBeEqualTo 13.0
    }

    @Test
    fun `벡터 뺄셈이 동작한다`() {
        val a = vectorOf(4.0, 5.0, 6.0)
        val b = vectorOf(1.0, 2.0, 3.0)
        val result = a - b

        result[0] shouldBeEqualTo 3.0
        result[1] shouldBeEqualTo 3.0
        result[2] shouldBeEqualTo 3.0
    }

    @Test
    fun `스칼라 뺄셈이 동작한다`() {
        val a = vectorOf(5.0, 6.0, 7.0)
        val result = a.minus<Double>(2.0)

        result[0] shouldBeEqualTo 3.0
        result[1] shouldBeEqualTo 4.0
        result[2] shouldBeEqualTo 5.0
    }

    @Test
    fun `벡터 원소별 곱셈이 동작한다`() {
        val a = vectorOf(2.0, 3.0, 4.0)
        val b = vectorOf(5.0, 6.0, 7.0)
        val result = a * b

        result[0] shouldBeEqualTo 10.0
        result[1] shouldBeEqualTo 18.0
        result[2] shouldBeEqualTo 28.0
    }

    @Test
    fun `스칼라 곱셈이 동작한다`() {
        val a = vectorOf(1.0, 2.0, 3.0)
        val result = a.times<Double>(3.0)

        result[0] shouldBeEqualTo 3.0
        result[1] shouldBeEqualTo 6.0
        result[2] shouldBeEqualTo 9.0
    }

    @Test
    fun `벡터 원소별 나눗셈이 동작한다`() {
        val a = vectorOf(10.0, 12.0, 15.0)
        val b = vectorOf(2.0, 4.0, 5.0)
        val result = a / b

        result[0] shouldBeEqualTo 5.0
        result[1] shouldBeEqualTo 3.0
        result[2] shouldBeEqualTo 3.0
    }

    @Test
    fun `스칼라 나눗셈이 동작한다`() {
        val a = vectorOf(6.0, 9.0, 12.0)
        val result = a.div<Double>(3.0)

        result[0] shouldBeEqualTo 2.0
        result[1] shouldBeEqualTo 3.0
        result[2] shouldBeEqualTo 4.0
    }

    @Test
    fun `toArrayRealVector 가 동작한다`() {
        val a = vectorOf(1.0, 2.0, 3.0)
        val result = a.toArrayRealVector()

        result[0] shouldBeEqualTo 1.0
        result[1] shouldBeEqualTo 2.0
        result[2] shouldBeEqualTo 3.0
    }
}
