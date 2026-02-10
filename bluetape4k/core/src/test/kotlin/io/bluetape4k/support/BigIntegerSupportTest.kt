package io.bluetape4k.support

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BigIntegerSupportTest {

    @Test
    fun `compare BigInteger with Number`() {
        assertTrue { BigInteger.ZERO < 1L }
        assertFalse { BigInteger.ZERO > 1L }

        assertTrue { BigInteger.ONE > 0L }
        assertTrue { BigInteger.TEN > 5 }

        assertFalse { BigInteger.ZERO > 1L }
        assertFalse { BigInteger.ZERO > 1.0 }
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
