package io.bluetape4k.math.fraction

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.apache.commons.math3.fraction.Fraction
import org.junit.jupiter.api.Test

class FractionSupportTest {

    companion object: KLogging()

    // ----- fractionOf 팩토리 -----

    @Test
    fun `fractionOf 분자만으로 생성한다`() {
        val f = fractionOf(3)
        f.numerator shouldBeEqualTo 3
        f.denominator shouldBeEqualTo 1
    }

    @Test
    fun `fractionOf 분자와 분모로 생성한다`() {
        val f = fractionOf(1, 2)
        f.numerator shouldBeEqualTo 1
        f.denominator shouldBeEqualTo 2
    }

    @Test
    fun `fractionOf Double 로 생성한다`() {
        val f = fractionOf(0.5)
        f.toDouble().shouldBeNear(0.5, 1e-10)
    }

    @Test
    fun `reducedFractionOf 기약분수를 반환한다`() {
        val f = reducedFractionOf(4, 8)
        f.numerator shouldBeEqualTo 1
        f.denominator shouldBeEqualTo 2
    }

    // ----- 사칙연산 연산자 -----

    @Test
    fun `Fraction 덧셈이 동작한다`() {
        val a = fractionOf(1, 2)
        val b = fractionOf(1, 3)
        val result = a + b
        result shouldBeEqualTo fractionOf(5, 6)
    }

    @Test
    fun `Fraction 스칼라 덧셈이 동작한다`() {
        val a = fractionOf(1, 2)
        val result = a + 1
        result shouldBeEqualTo fractionOf(3, 2)
    }

    @Test
    fun `Fraction 뺄셈이 동작한다`() {
        val a = fractionOf(3, 4)
        val b = fractionOf(1, 4)
        val result = a - b
        result shouldBeEqualTo fractionOf(1, 2)
    }

    @Test
    fun `Fraction 스칼라 뺄셈이 동작한다`() {
        val a = fractionOf(5, 2)
        val result = a - 1
        result shouldBeEqualTo fractionOf(3, 2)
    }

    @Test
    fun `Fraction 곱셈이 동작한다`() {
        val a = fractionOf(2, 3)
        val b = fractionOf(3, 4)
        val result = a * b
        result shouldBeEqualTo fractionOf(1, 2)
    }

    @Test
    fun `Fraction 스칼라 곱셈이 동작한다`() {
        val a = fractionOf(1, 3)
        val result = a * 3
        result shouldBeEqualTo Fraction.ONE
    }

    @Test
    fun `Fraction 나눗셈이 동작한다`() {
        val a = fractionOf(3, 4)
        val b = fractionOf(3, 8)
        val result = a / b
        result shouldBeEqualTo fractionOf(2, 1)
    }

    @Test
    fun `Fraction 스칼라 나눗셈이 동작한다`() {
        val a = fractionOf(6, 1)
        val result = a / 2
        result shouldBeEqualTo fractionOf(3, 1)
    }
}
