package io.bluetape4k.support

import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.minus
import kotlin.plus
import kotlin.toBigDecimal

class BigDecimalSupportTest {

    @Test
    fun `compare BigDecimal and Number`() {
        Assertions.assertTrue { BigDecimal.ONE > 0L }
        Assertions.assertTrue { BigDecimal.ONE > 0.5 }

        Assertions.assertFalse { BigDecimal.ZERO > 0L }
        Assertions.assertFalse { BigDecimal.ZERO > 0.5 }
    }

    @Test
    fun `basic operators`() {
        val b = 20.toBigDecimal()
        val a = 10.toBigDecimal()

        a + a shouldBeEqualTo b
        b - a shouldBeEqualTo a
        a * 2 shouldBeEqualTo b
        b / 2 shouldBeEqualTo a
    }

    @Test
    fun `collection operator of BigDecimal`() {
        val numbers = List(100) { it.toBigDecimal() }
        numbers.sum() shouldBeEqualTo (0 until 100).sum().toBigDecimal()
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
