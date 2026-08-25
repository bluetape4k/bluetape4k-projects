package io.bluetape4k.support

import io.bluetape4k.assertions.assertFailsWith
import io.bluetape4k.assertions.shouldBeEqualTo
import io.bluetape4k.assertions.shouldBeFalse
import io.bluetape4k.assertions.shouldBeTrue
import org.junit.jupiter.api.Test
import java.math.BigInteger

class BigIntegerSupportTest {

    @Test
    fun `compare BigInteger with Number`() {
        (BigInteger.ZERO < 1L).shouldBeTrue()
        (BigInteger.ZERO > 1L).shouldBeFalse()

        (BigInteger.ONE > 0L).shouldBeTrue()
        (BigInteger.TEN > 5).shouldBeTrue()

        (BigInteger.ZERO > 1L).shouldBeFalse()
        (BigInteger.ZERO > 1.0).shouldBeFalse()
    }

    @Test
    fun `convert to BigInteger`() {
        0.toBigInt() shouldBeEqualTo BigInteger.ZERO
        1.toBigInt() shouldBeEqualTo BigInteger.ONE
        1L.toBigInt() shouldBeEqualTo BigInteger.ONE
        "1".toBigInt() shouldBeEqualTo BigInteger.ONE

        1.5.toBigInt() shouldBeEqualTo BigInteger("1")
        2.0f.toBigInt() shouldBeEqualTo BigInteger("2")

        10000000000L.toBigInt() shouldBeEqualTo BigInteger("10000000000")
    }

    @Test
    fun `basic operators`() {
        val b = 20.toBigInt()
        val a = 10.toBigInt()

        a + a shouldBeEqualTo b
        b - a shouldBeEqualTo a
        a * 2 shouldBeEqualTo b
        2 * a shouldBeEqualTo b
        b / 2 shouldBeEqualTo a
    }

    @Test
    fun `divide operator throws exception on zero`() {
        val a = 1.toBigInt()

        assertFailsWith<ArithmeticException> {
            a / 0
        }
    }

    @Test
    fun `sum of BigInteger collection`() {
        val numbers = bigIntList(3) { it + 1 }
        numbers.sum() shouldBeEqualTo 6.toBigInt()
    }

    @Test
    fun `sum of BigInteger array`() {
        val numbers = bigIntArray(3) { it + 1 }
        numbers.sum() shouldBeEqualTo 6.toBigInt()
    }

    @Test
    fun `average of BigInteger collection`() {
        val numbers = bigIntList(3) { it + 1 }
        numbers.average() shouldBeEqualTo 2.0
    }


    @Test
    fun `average of BigInteger array`() {
        val numbers = bigIntArray(3) { it + 1 }
        numbers.average() shouldBeEqualTo 2.0
    }
}
