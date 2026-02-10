package io.bluetape4k.support

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.minus
import kotlin.plus
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.toBigDecimal

class BigDecimalSupportTest {

    @Test
    fun `compare BigDecimal and Number`() {
        assertTrue { BigDecimal.ONE > 0L }
        assertTrue { BigDecimal.ONE > 0.5 }

        assertFalse { BigDecimal.ZERO > 0L }
        assertFalse { BigDecimal.ZERO > 0.5 }
    }

    @Test
    fun `convert to BigDecimal`() {
        0.toBigDecimal() shouldBeEqualTo BigDecimal.ZERO
        1.toBigDecimal() shouldBeEqualTo BigDecimal.ONE
        1L.toBigDecimal() shouldBeEqualTo BigDecimal.ONE
        "1".toBigDecimal() shouldBeEqualTo BigDecimal.ONE

        1.5.toBigDecimal() shouldBeEqualTo BigDecimal("1.5")
        2.0f.toBigDecimal() shouldBeEqualTo BigDecimal("2.0")

        10000000000L.toBigDecimal() shouldBeEqualTo BigDecimal("10000000000")
    }

    @Test
    fun `basic operators`() {
        val b = 20.toBigDecimal()
        val a = 10.toBigDecimal()

        a + a shouldBeEqualTo b
        b - a shouldBeEqualTo a
        a * 2 shouldBeEqualTo b
        2 * a shouldBeEqualTo b
        b / 2 shouldBeEqualTo a
    }

    @Test
    fun `divide operator throws exception on non-terminating decimal`() {
        val a = 1.toBigDecimal()

        assertFailsWith<ArithmeticException> {
            a / 0
        }

        assertFailsWith<ArithmeticException> {
            a / 3
        }
    }

    @Test
    fun `divideSafe rounds with default scale and rounding mode`() {
        val a = 10.toBigDecimal()

        a.divideSafe(3) shouldBeEqualTo 3.33.toBigDecimal()
        a.divideSafe(3, 4) shouldBeEqualTo 3.3333.toBigDecimal()
        a.divideSafe(3, 4, java.math.RoundingMode.DOWN) shouldBeEqualTo 3.3333.toBigDecimal()
    }

    @Test
    fun `average of BigDecimal collection`() {
        val numbers = bigDecimalList(3) { it + 1 }
        numbers.average() shouldBeEqualTo 2.toBigDecimal()
    }

    @Test
    fun `average of BigDecimal array`() {
        val numbers = bigDecimalArray(3) { it + 1 }
        numbers.average() shouldBeEqualTo 2.toBigDecimal()
    }

    @Test
    fun `average of empty BigDecimal collection returns ZERO`() {
        val numbers = emptyList<BigDecimal>()
        numbers.average() shouldBeEqualTo BigDecimal.ZERO
    }

    @Test
    fun `collection operator of BigDecimal`() {
        val numbers = bigDecimalList(3) { it + 1 }
        numbers.sum() shouldBeEqualTo 6.toBigDecimal()
    }

    @Test
    fun `roud up bigdecimal`() {
        val n = 2.45.toBigDecimal()

        n.roundUp(0) shouldBeEqualTo 2.toBigDecimal()
        n.roundUp(1) shouldBeEqualTo 2.5.toBigDecimal()
        n.roundUp(2) shouldBeEqualTo 2.45.toBigDecimal()

        n.roundUp(-1) shouldBeEqualTo 0.toBigDecimal()
    }
}
