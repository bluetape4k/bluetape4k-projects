package io.bluetape4k.support

import io.bluetape4k.collections.eclipse.fastList
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigInteger

class BigIntegerSupportTest {

    @Test
    fun `compare BigInteger with Number`() {
        Assertions.assertTrue { BigInteger.ZERO < 1L }
        Assertions.assertFalse { BigInteger.ZERO > 1L }

        Assertions.assertTrue { BigInteger.ONE > 0L }
        Assertions.assertTrue { BigInteger.TEN > 5 }

        Assertions.assertFalse { BigInteger.ZERO > 1L }
        Assertions.assertFalse { BigInteger.ZERO > 1.0 }
    }

    @Test
    fun `basic operators`() {
        val b = 20.toBigInt()
        val a = 10.toBigInt()

        a + a shouldBeEqualTo b
        b - a shouldBeEqualTo a
        a * 2 shouldBeEqualTo b
        b / 2 shouldBeEqualTo a
    }

    @Test
    fun `collection operator of BigInteger`() {
        val numbers = fastList(100) { it.toBigInt() }
        numbers.sum() shouldBeEqualTo (0 until 100).sum().toBigInt()
    }
}
