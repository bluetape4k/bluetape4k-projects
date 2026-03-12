package io.bluetape4k.math.commons

import io.bluetape4k.logging.KLogging
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeNear
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

class SquareTest {

    companion object: KLogging()

    @Test
    fun `Double square 가 동작한다`() {
        3.0.square().shouldBeNear(9.0, 1e-10)
        (-4.0).square().shouldBeNear(16.0, 1e-10)
        0.0.square().shouldBeNear(0.0, 1e-10)
    }

    @Test
    fun `Float square 가 동작한다`() {
        3.0f.square().shouldBeNear(9.0f, 1e-5f)
        (-4.0f).square().shouldBeNear(16.0f, 1e-5f)
    }

    @Test
    fun `Long square 가 동작한다`() {
        3L.square() shouldBeEqualTo 9L
        (-4L).square() shouldBeEqualTo 16L
        0L.square() shouldBeEqualTo 0L
    }

    @Test
    fun `Int square 가 동작한다`() {
        3.square() shouldBeEqualTo 9
        (-4).square() shouldBeEqualTo 16
        0.square() shouldBeEqualTo 0
    }

    @Test
    fun `BigDecimal square 가 동작한다`() {
        BigDecimal("3.5").square() shouldBeEqualTo BigDecimal("12.25")
        BigDecimal("-2.0").square() shouldBeEqualTo BigDecimal("4.00")
    }

    @Test
    fun `BigInteger square 가 동작한다`() {
        BigInteger("5").square() shouldBeEqualTo BigInteger("25")
        BigInteger("-3").square() shouldBeEqualTo BigInteger("9")
    }

    @Test
    fun `square 는 x 곱하기 x 와 같다`() {
        val x = 7.0
        x.square() shouldBeEqualTo x * x
    }
}
