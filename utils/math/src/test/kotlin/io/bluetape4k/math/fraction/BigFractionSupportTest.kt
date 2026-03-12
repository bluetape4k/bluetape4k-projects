package io.bluetape4k.math.fraction

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.apache.commons.math3.fraction.BigFraction
import org.junit.jupiter.api.Test
import java.math.BigInteger

class BigFractionSupportTest {

    companion object: KLogging()

    // ----- bigFractionOf 팩토리 -----

    @Test
    fun `bigFractionOf 분자만으로 생성한다`() {
        val f = bigFractionOf(3)
        f.numerator shouldBeEqualTo BigInteger.valueOf(3)
        f.denominator shouldBeEqualTo BigInteger.ONE
    }

    @Test
    fun `bigFractionOf 분자와 분모로 생성한다`() {
        val f = bigFractionOf(1, 2)
        f.numerator shouldBeEqualTo BigInteger.ONE
        f.denominator shouldBeEqualTo BigInteger.valueOf(2)
    }

    @Test
    fun `bigFractionOf Double 로 생성한다`() {
        val f = bigFractionOf(0.5)
        f.toDouble().shouldBeNear(0.5, 1e-10)
    }

    @Test
    fun `reducedBigFractionOf 기약분수를 반환한다`() {
        val f = reducedBigFractionOf(4, 8)
        f.numerator shouldBeEqualTo BigInteger.ONE
        f.denominator shouldBeEqualTo BigInteger.valueOf(2)
    }

    // ----- 사칙연산 연산자 -----

    @Test
    fun `BigFraction 덧셈이 동작한다`() {
        val a = bigFractionOf(1, 2)
        val b = bigFractionOf(1, 3)
        val result = a + b
        result shouldBeEqualTo bigFractionOf(5, 6)
    }

    @Test
    fun `BigFraction 스칼라 덧셈이 동작한다`() {
        val a = bigFractionOf(1, 2)
        val result = a + 1
        result shouldBeEqualTo bigFractionOf(3, 2)
    }

    @Test
    fun `BigFraction 뺄셈이 동작한다`() {
        val a = bigFractionOf(3, 4)
        val b = bigFractionOf(1, 4)
        val result = a - b
        result shouldBeEqualTo bigFractionOf(1, 2)
    }

    @Test
    fun `BigFraction 곱셈이 동작한다`() {
        val a = bigFractionOf(2, 3)
        val b = bigFractionOf(3, 4)
        val result = a * b
        result shouldBeEqualTo bigFractionOf(1, 2)
    }

    @Test
    fun `BigFraction 스칼라 곱셈이 동작한다`() {
        val a = bigFractionOf(1, 3)
        val result = a * 3L
        result shouldBeEqualTo BigFraction.ONE
    }

    @Test
    fun `BigFraction 나눗셈이 동작한다`() {
        val a = bigFractionOf(3, 4)
        val b = bigFractionOf(3, 8)
        val result = a / b
        result shouldBeEqualTo bigFractionOf(2, 1)
    }

    @Test
    fun `BigFraction 스칼라 나눗셈이 동작한다`() {
        val a = bigFractionOf(6, 1)
        val result = a / 2L
        result shouldBeEqualTo bigFractionOf(3, 1)
    }
}
